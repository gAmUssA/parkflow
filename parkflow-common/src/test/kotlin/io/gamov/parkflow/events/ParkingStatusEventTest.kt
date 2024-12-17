package io.gamov.parkflow.events

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class ParkingStatusEventTest : FunSpec({
    test("create and serialize active parking session") {
        val entryTime = System.currentTimeMillis()
        val event = ParkingStatusEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setLicensePlate("ABC123")
            .setEntryTimestamp(entryTime)
            .setParkingDuration(60L)
            .setAmountDue(7.50)
            .setPaymentStatus(SessionPaymentStatus.UNPAID)
            .setLastPaymentId(null)
            .setEntryGateId("GATE-1")
            .setVehicleType("CAR")
            .setStatus(SessionStatus.ACTIVE)
            .build()

        val serialized = event.toByteArray()
        val deserialized = event.fromByteArray(serialized)

        deserialized shouldNotBe null
        deserialized.licensePlate shouldBe "ABC123"
        deserialized.entryTimestamp shouldBe entryTime
        deserialized.parkingDuration shouldBe 60L
        deserialized.amountDue shouldBe 7.50
        deserialized.paymentStatus shouldBe SessionPaymentStatus.UNPAID
        deserialized.status shouldBe SessionStatus.ACTIVE
    }

    test("create completed parking session") {
        val paymentId = UUID.randomUUID().toString()
        val event = ParkingStatusEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setLicensePlate("XYZ789")
            .setEntryTimestamp(System.currentTimeMillis() - 7200000) // 2 hours ago
            .setParkingDuration(120L)
            .setAmountDue(15.00)
            .setPaymentStatus(SessionPaymentStatus.PAID)
            .setLastPaymentId(paymentId)
            .setEntryGateId("GATE-2")
            .setVehicleType("CAR")
            .setStatus(SessionStatus.COMPLETED)
            .build()

        val serialized = event.toByteArray()
        val deserialized = event.fromByteArray(serialized)

        deserialized shouldNotBe null
        deserialized.paymentStatus shouldBe SessionPaymentStatus.PAID
        deserialized.lastPaymentId shouldBe paymentId
        deserialized.status shouldBe SessionStatus.COMPLETED
    }
})
