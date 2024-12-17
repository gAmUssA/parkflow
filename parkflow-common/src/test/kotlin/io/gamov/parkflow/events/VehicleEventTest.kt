package io.gamov.parkflow.events

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class VehicleEventTest : FunSpec({
    test("create and serialize VehicleEntryEvent") {
        val event = VehicleEntryEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setLicensePlate("ABC123")
            .setGateId("GATE-1")
            .setLaneId("LANE-1")
            .setConfidence(0.95)
            .setVehicleType(VehicleType.CAR)
            .build()

        val serialized = event.toByteArray()
        val deserialized = event.fromByteArray(serialized)

        deserialized shouldNotBe null
        deserialized.licensePlate shouldBe "ABC123"
        deserialized.confidence shouldBe 0.95
        deserialized.vehicleType shouldBe VehicleType.CAR
    }

    test("create and serialize VehicleExitEvent") {
        val entryEventId = UUID.randomUUID().toString()
        val event = VehicleExitEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setLicensePlate("ABC123")
            .setGateId("GATE-2")
            .setLaneId("LANE-1")
            .setConfidence(0.92)
            .setEntryEventId(entryEventId)
            .build()

        val serialized = event.toByteArray()
        val deserialized = event.fromByteArray(serialized)

        deserialized shouldNotBe null
        deserialized.licensePlate shouldBe "ABC123"
        deserialized.entryEventId shouldBe entryEventId
    }
})
