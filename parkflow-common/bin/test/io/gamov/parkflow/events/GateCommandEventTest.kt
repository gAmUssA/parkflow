package io.gamov.parkflow.events

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class GateCommandEventTest : FunSpec({
    test("create and serialize GateCommandEvent") {
        val event = GateCommandEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setGateId("GATE-1")
            .setCommand(GateCommand.OPEN)
            .setReason("Valid payment received")
            .setLicensePlate("ABC123")
            .setOperatorId(null)
            .setSource(CommandSource.AUTOMATIC)
            .build()

        val serialized = event.toByteArray()
        val deserialized = event.fromByteArray(serialized)

        deserialized shouldNotBe null
        deserialized.command shouldBe GateCommand.OPEN
        deserialized.source shouldBe CommandSource.AUTOMATIC
        deserialized.licensePlate shouldBe "ABC123"
        deserialized.operatorId shouldBe null
    }

    test("create manual gate command") {
        val operatorId = "OP-" + UUID.randomUUID().toString()
        val event = GateCommandEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setGateId("GATE-2")
            .setCommand(GateCommand.DENY)
            .setReason("Suspicious activity")
            .setLicensePlate("XYZ789")
            .setOperatorId(operatorId)
            .setSource(CommandSource.MANUAL)
            .build()

        val serialized = event.toByteArray()
        val deserialized = event.fromByteArray(serialized)

        deserialized shouldNotBe null
        deserialized.command shouldBe GateCommand.DENY
        deserialized.source shouldBe CommandSource.MANUAL
        deserialized.operatorId shouldBe operatorId
    }
})
