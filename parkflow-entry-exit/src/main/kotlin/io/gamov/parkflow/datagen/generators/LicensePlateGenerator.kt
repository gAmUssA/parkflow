package io.gamov.parkflow.datagen.generators

import net.datafaker.Faker
import java.util.concurrent.ConcurrentHashMap

class LicensePlateGenerator(
    private val regularCustomerRatio: Double = 0.3,
    private val regularPlatesPoolSize: Int = 50
) {
    private val faker = Faker()
    private val regularPlates = ConcurrentHashMap<String, Unit>()

    init {
        // Pre-generate regular customer plates
        repeat(regularPlatesPoolSize) {
            regularPlates[generateNewPlate()] = Unit
        }
    }

    fun nextPlate(): String {
        return if (faker.random().nextDouble() < regularCustomerRatio) {
            // Return a plate from the regular customers pool
            regularPlates.keys.random()
        } else {
            // Generate a new plate for a one-time customer
            generateNewPlate()
        }
    }

    private fun generateNewPlate(): String {
        // Generate plate in format: XX-NNXX or XX-XXNN where X is letter and N is number
        val prefix = faker.letterify("??").uppercase()
        val suffix = if (faker.random().nextBoolean()) {
            "${faker.numerify("##")}${faker.letterify("??").uppercase()}"
        } else {
            "${faker.letterify("??").uppercase()}${faker.numerify("##")}"
        }
        return "$prefix-$suffix"
    }
}
