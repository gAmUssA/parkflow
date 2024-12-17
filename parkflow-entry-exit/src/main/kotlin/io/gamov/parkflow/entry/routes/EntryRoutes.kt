package io.gamov.parkflow.entry.routes

import io.gamov.parkflow.entry.EntrySimulator
import io.gamov.parkflow.entry.SimulationConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch

fun Route.entryRoutes(simulator: EntrySimulator) {
    route("/api/v1/entry") {
        post("/event") {
            val gateId = call.parameters["gateId"] ?: "GATE_1"
            val laneId = call.parameters["laneId"] ?: "LANE_1"
            val event = simulator.generateEvent(gateId, laneId)
            simulator.sendEvent(event)
            call.respond(HttpStatusCode.Created, mapOf("eventId" to event.eventId))
        }

        post("/simulate") {
            val config = call.receive<SimulationConfig>()
            launch {
                simulator.runSimulation(config)
            }
            call.respond(HttpStatusCode.Accepted, mapOf("message" to "Simulation started"))
        }
    }
}
