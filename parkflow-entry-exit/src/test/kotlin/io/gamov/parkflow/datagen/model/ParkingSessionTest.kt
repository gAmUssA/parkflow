package io.gamov.parkflow.datagen.model

import io.gamov.parkflow.events.PaymentEvent
import io.gamov.parkflow.events.PaymentMethod
import io.gamov.parkflow.events.PaymentStatus
import io.gamov.parkflow.events.VehicleExitEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class ParkingSessionTest : FunSpec({
    test("new session should be incomplete and unpaid") {
        val session = ParkingSession.create(
            licensePlate = "AB-12-CD",
            entryGateId = "ENTRY_NORTH",
            stayDuration = 60
        )

        session.isCompleted shouldBe false
        session.isPaid shouldBe false
        session.entryEvent.licensePlate shouldBe "AB-12-CD"
        session.entryEvent.gateId shouldBe "ENTRY_NORTH"
    }

    test("free period session should be considered paid") {
        val session = ParkingSession.create(
            licensePlate = "AB-12-CD",
            entryGateId = "ENTRY_NORTH",
            stayDuration = 4,
            shouldPay = false
        )

        session.isPaid shouldBe true
    }

    test("session should be complete after exit event") {
        val session = ParkingSession.create(
            licensePlate = "AB-12-CD",
            entryGateId = "ENTRY_NORTH",
            stayDuration = 60
        )

        val exitEvent = VehicleExitEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setLicensePlate(session.licensePlate)
            .setGateId("EXIT_NORTH")
            .setLaneId("LANE-1")
            .setConfidence(0.95)
            .setEntryEventId(session.entryEvent.eventId)
            .build()

        session.exitEvent = exitEvent
        session.isCompleted shouldBe true
    }

    test("session should be paid after payment event") {
        val session = ParkingSession.create(
            licensePlate = "AB-12-CD",
            entryGateId = "ENTRY_NORTH",
            stayDuration = 60
        )

        val paymentEvent = PaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setTransactionId("TXN-${UUID.randomUUID()}")
            .setLicensePlate(session.licensePlate)
            .setAmount(10.0)
            .setCurrency("USD")
            .setPaymentMethod(PaymentMethod.CREDIT_CARD)
            .setStatus(PaymentStatus.COMPLETED)
            .setParkingDuration(60L)
            .build()

        session.paymentEvent = paymentEvent
        session.isPaid shouldBe true
    }
})
