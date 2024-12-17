package io.gamov.parkflow.datagen.generators

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

class LicensePlateGeneratorTest : FunSpec({
    test("generated license plates should match required format") {
        val generator = LicensePlateGenerator()
        val plates = List(100) { generator.nextPlate() }

        plates.forEach { plate ->
            plate shouldMatch """^[A-Z]{2}-(?:\d{2}[A-Z]{2}|[A-Z]{2}\d{2})$"""
        }
    }

    test("regular customers should have repeated plates") {
        val generator = LicensePlateGenerator(regularCustomerRatio = 1.0, regularPlatesPoolSize = 10)
        val plates = List(100) { generator.nextPlate() }
        val uniquePlates = plates.toSet()

        // With 100% regular customer ratio and pool size of 10, we should have exactly 10 unique plates
        uniquePlates.size shouldBe 10
    }

    test("non-regular customers should have unique plates") {
        val generator = LicensePlateGenerator(regularCustomerRatio = 0.0)
        val plates = List(100) { generator.nextPlate() }
        val uniquePlates = plates.toSet()

        // With 0% regular customer ratio, almost all plates should be unique
        uniquePlates.size shouldBe plates.size
    }

    test("mixed customer types should have expected uniqueness ratio") {
        val generator = LicensePlateGenerator(regularCustomerRatio = 0.3, regularPlatesPoolSize = 50)
        val plates = List(1000) { generator.nextPlate() }
        val uniquePlates = plates.toSet()

        // With 30% regular customers and pool size of 50, uniqueness should be around 70%
        val uniquenessRatio = uniquePlates.size.toDouble() / plates.size
        uniquenessRatio shouldBe (0.7 plusOrMinus 0.1)
    }
})
