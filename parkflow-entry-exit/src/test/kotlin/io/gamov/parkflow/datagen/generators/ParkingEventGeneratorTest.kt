package io.gamov.parkflow.datagen.generators

import io.gamov.parkflow.events.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

class ParkingEventGeneratorTest : FunSpec({
    test("should generate events in correct sequence") {
        val generator = ParkingEventGenerator(continuous = false)
        val events = runBlocking { 
            withTimeout(10.seconds) {
                generator.generateEvents()
                    .take(10)
                    .toList()
            }
        }

        // First event should be an entry
        events.first().shouldBeInstanceOf<VehicleEntryEvent>()

        // Track sessions to verify sequence
        val sessions = mutableMapOf<String, MutableList<Any>>()
        events.forEach { event ->
            val licensePlate = when (event) {
                is VehicleEntryEvent -> event.licensePlate
                is PaymentEvent -> event.licensePlate
                is VehicleExitEvent -> event.licensePlate
                else -> throw IllegalStateException("Unexpected event type")
            }
            sessions.getOrPut(licensePlate) { mutableListOf() }.add(event)
        }

        // Verify sequence for each session
        sessions.values.forEach { sequence ->
            sequence.forEachIndexed { index, event ->
                when (index) {
                    0 -> event.shouldBeInstanceOf<VehicleEntryEvent>()
                    sequence.size - 1 -> event.shouldBeInstanceOf<VehicleExitEvent>()
                    else -> event.shouldBeInstanceOf<PaymentEvent>()
                }
            }
        }
    }

    test("payment amounts should be within specified range") {
        val generator = ParkingEventGenerator(continuous = false)
        val events = runBlocking {
            withTimeout(10.seconds) {
                generator.generateEvents()
                    .take(50)
                    .toList()
                    .filterIsInstance<PaymentEvent>()
            }
        }

        events.forEach { event ->
            (event.amount in 2.0..25.0) shouldBe true
        }
    }

    test("payment methods should follow specified distribution") {
        val generator = ParkingEventGenerator(continuous = false)
        val events = runBlocking {
            withTimeout(10.seconds) {
                generator.generateEvents()
                    .take(1000) // Increase sample size for more reliable distribution
                    .toList()
                    .filterIsInstance<PaymentEvent>()
            }
        }

        val methodCounts = events.groupBy { it.paymentMethod }
        val total = events.size.toDouble()

        // Credit card should be around 70%
        (methodCounts[PaymentMethod.CREDIT_CARD]?.size ?: 0).toDouble() / total shouldBe (0.7 plusOrMinus 0.1)
        
        // Debit card should be around 25%
        (methodCounts[PaymentMethod.DEBIT_CARD]?.size ?: 0).toDouble() / total shouldBe (0.25 plusOrMinus 0.1)
        
        // Cash should be around 5%
        (methodCounts[PaymentMethod.CASH]?.size ?: 0).toDouble() / total shouldBe (0.05 plusOrMinus 0.05)
    }

    test("entry gates should follow specified distribution") {
        val generator = ParkingEventGenerator(continuous = false)
        val events = runBlocking {
            withTimeout(10.seconds) {
                generator.generateEvents()
                    .take(1000) // Increase sample size for more reliable distribution
                    .toList()
                    .filterIsInstance<VehicleEntryEvent>()
            }
        }

        val gateCounts = events.groupBy { it.gateId }
        val total = events.size.toDouble()

        // ENTRY_NORTH should be around 60%
        (gateCounts["ENTRY_NORTH"]?.size ?: 0).toDouble() / total shouldBe (0.6 plusOrMinus 0.15)
        
        // ENTRY_SOUTH should be around 40%
        (gateCounts["ENTRY_SOUTH"]?.size ?: 0).toDouble() / total shouldBe (0.4 plusOrMinus 0.15)
    }
})
