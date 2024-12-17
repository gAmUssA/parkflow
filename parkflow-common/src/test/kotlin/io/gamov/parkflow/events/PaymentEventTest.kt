package io.gamov.parkflow.events

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class PaymentEventTest : FunSpec({
    test("create and serialize PaymentEvent") {
        val event = PaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setTransactionId("TXN-" + UUID.randomUUID().toString())
            .setLicensePlate("ABC123")
            .setAmount(15.50)
            .setCurrency("USD")
            .setPaymentMethod(PaymentMethod.CREDIT_CARD)
            .setStatus(PaymentStatus.COMPLETED)
            .setParkingDuration(120L) // 2 hours in minutes
            .build()

        val serialized = event.toByteArray()
        val deserialized = event.fromByteArray(serialized)

        deserialized shouldNotBe null
        deserialized.amount shouldBe 15.50
        deserialized.paymentMethod shouldBe PaymentMethod.CREDIT_CARD
        deserialized.status shouldBe PaymentStatus.COMPLETED
        deserialized.parkingDuration shouldBe 120L
    }
})
