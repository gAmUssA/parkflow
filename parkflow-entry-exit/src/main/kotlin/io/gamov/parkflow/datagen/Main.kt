package io.gamov.parkflow.datagen

import io.gamov.parkflow.datagen.generators.ParkingEventGenerator
import io.gamov.parkflow.events.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    val generator = ParkingEventGenerator()
    val producer = createProducer()

    generator.generateEvents().collect { event ->
        when (event) {
            is VehicleEntryEvent -> {
                producer.send(ProducerRecord("parkflow.events.entry", event.licensePlate, event))
                logger.info { "Entry event generated for ${event.licensePlate}" }
            }
            is PaymentEvent -> {
                producer.send(ProducerRecord("parkflow.events.payment", event.licensePlate, event))
                logger.info { "Payment event generated for ${event.licensePlate}" }
            }
            is VehicleExitEvent -> {
                producer.send(ProducerRecord("parkflow.events.exit", event.licensePlate, event))
                logger.info { "Exit event generated for ${event.licensePlate}" }
            }
        }
    }
}

private fun createProducer(): KafkaProducer<String, Any> {
    val props = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer")
        put("schema.registry.url", "http://localhost:8081")
    }
    return KafkaProducer(props)
}
