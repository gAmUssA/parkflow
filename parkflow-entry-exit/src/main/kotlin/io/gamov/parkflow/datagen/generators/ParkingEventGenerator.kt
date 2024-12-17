package io.gamov.parkflow.datagen.generators

import io.gamov.parkflow.datagen.model.ParkingSession
import io.gamov.parkflow.events.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

class ParkingEventGenerator(
    private val licensePlateGenerator: LicensePlateGenerator = LicensePlateGenerator(),
    private val continuous: Boolean = true
) {
    private val random = Random.Default
    private val activeSessions = mutableMapOf<String, ParkingSession>()

    fun generateEvents(): Flow<Any> = flow {
        var eventsEmitted = 0
        while (continuous || eventsEmitted < 1000) { // Limit for test mode
            val currentTime = ZonedDateTime.now().toLocalTime()
            val delay = if (continuous) calculateDelay(currentTime) else 1L // Fast for tests
            
            delay(delay)

            // Generate new entry if capacity allows
            if (shouldGenerateEntry(currentTime)) {
                val session = generateEntry()
                activeSessions[session.licensePlate] = session
                emit(session.entryEvent)
                eventsEmitted++
            }

            // Process active sessions
            activeSessions.values.toList().forEach { session ->
                processSession(session)?.let { event ->
                    emit(event)
                    eventsEmitted++
                    if (event is VehicleExitEvent) {
                        activeSessions.remove(session.licensePlate)
                    }
                }
            }
        }
    }

    private fun calculateDelay(currentTime: LocalTime): Long {
        return when {
            isPeakHour(currentTime) -> random.nextLong(6000, 12000) // 6-12 seconds
            isQuietPeriod(currentTime) -> random.nextLong(600000, 900000) // 10-15 minutes
            else -> random.nextLong(20000, 60000) // 20-60 seconds
        }
    }

    private fun shouldGenerateEntry(currentTime: LocalTime): Boolean {
        return when {
            isPeakHour(currentTime) -> random.nextDouble() < 0.8
            isQuietPeriod(currentTime) -> random.nextDouble() < 0.1
            else -> random.nextDouble() < 0.3
        }
    }

    private fun generateEntry(): ParkingSession {
        val licensePlate = licensePlateGenerator.nextPlate()
        val entryGate = if (random.nextDouble() < 0.6) "ENTRY_NORTH" else "ENTRY_SOUTH"
        val stayDuration = when (random.nextDouble()) {
            in 0.0..0.1 -> random.nextLong(5, 15) // Quick stops
            in 0.1..0.7 -> random.nextLong(60, 180) // Shopping
            else -> random.nextLong(480, 600) // Work
        }
        val shouldPay = random.nextDouble() > 0.15 // 15% within free period

        return ParkingSession.create(
            licensePlate = licensePlate,
            entryGateId = entryGate,
            stayDuration = stayDuration,
            shouldPay = shouldPay
        )
    }

    private fun processSession(session: ParkingSession): Any? {
        if (session.isCompleted) return null

        val entryTime = session.entryEvent.timestamp
        val currentTime = System.currentTimeMillis()
        val elapsedMinutes = if (continuous) {
            (currentTime - entryTime) / 60000
        } else {
            // In test mode, simulate time passing faster
            (currentTime - entryTime) / 10 // 10ms = 1 minute in test mode
        }

        // First generate payment if needed
        if (!session.isPaid && session.shouldPay && elapsedMinutes >= 5) {
            return generatePayment(session)
        }

        // Then generate exit if payment is complete (or no payment needed) and duration exceeded
        if ((session.isPaid || !session.shouldPay) && elapsedMinutes >= session.stayDuration) {
            return generateExit(session)
        }

        return null
    }

    private fun generatePayment(session: ParkingSession): PaymentEvent {
        val amount = calculateAmount(session.stayDuration)
        val paymentMethod = when (random.nextDouble()) {
            in 0.0..0.7 -> PaymentMethod.CREDIT_CARD
            in 0.7..0.95 -> PaymentMethod.DEBIT_CARD
            else -> PaymentMethod.CASH
        }
        
        return PaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setTransactionId("TXN-${UUID.randomUUID()}")
            .setLicensePlate(session.licensePlate)
            .setAmount(amount)
            .setCurrency("USD")
            .setPaymentMethod(paymentMethod)
            .setStatus(PaymentStatus.COMPLETED)
            .setParkingDuration(session.stayDuration)
            .build()
            .also { session.paymentEvent = it }
    }

    private fun generateExit(session: ParkingSession): VehicleExitEvent {
        val exitGate = if (random.nextDouble() < 0.6) "EXIT_NORTH" else "EXIT_SOUTH"
        
        return VehicleExitEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setLicensePlate(session.licensePlate)
            .setGateId(exitGate)
            .setLaneId("LANE-1")
            .setConfidence(0.95)
            .setEntryEventId(session.entryEvent.eventId)
            .build()
            .also { session.exitEvent = it }
    }

    private fun calculateAmount(durationMinutes: Long): Double {
        val hours = durationMinutes / 60.0
        return (2.0 + (hours * 3.0)).coerceIn(2.0, 25.0)
    }

    private fun isPeakHour(time: LocalTime): Boolean {
        return (time.hour in 8..9) || (time.hour in 16..17)
    }

    private fun isQuietPeriod(time: LocalTime): Boolean {
        return time.hour in 23..23 || time.hour in 0..5
    }
}
