package io.gamov.parkflow.datagen.model

import io.gamov.parkflow.events.*
import java.time.Instant
import java.util.UUID

data class ParkingSession(
    val licensePlate: String,
    val entryEvent: VehicleEntryEvent,
    var paymentEvent: PaymentEvent? = null,
    var exitEvent: VehicleExitEvent? = null,
    val stayDuration: Long, // in minutes
    val shouldPay: Boolean = true
) {
    val isCompleted: Boolean
        get() = exitEvent != null

    val isPaid: Boolean
        get() = paymentEvent != null || !shouldPay

    companion object {
        fun create(
            licensePlate: String,
            entryGateId: String,
            stayDuration: Long,
            shouldPay: Boolean = true
        ): ParkingSession {
            val entryEvent = VehicleEntryEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now().toEpochMilli())
                .setLicensePlate(licensePlate)
                .setGateId(entryGateId)
                .setLaneId("LANE-1")
                .setConfidence(0.95)
                .setVehicleType(VehicleType.CAR)
                .build()

            return ParkingSession(
                licensePlate = licensePlate,
                entryEvent = entryEvent,
                stayDuration = stayDuration,
                shouldPay = shouldPay
            )
        }
    }
}
