package net.battaglini.fantaf1appbackend.configuration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class PricingPropertiesTest {

    @Autowired
    lateinit var pricingProperties: PricingProperties

    @Test
    fun `should load pricing properties`() {
        assertTrue(pricingProperties.enable)
        assertEquals(false, pricingProperties.dryRun)
        assertEquals(3, pricingProperties.rollingWindowSize)
        assertEquals(0.7, pricingProperties.driverWeight)
        assertEquals(0.2, pricingProperties.teamWeight)
        assertEquals(20.0, pricingProperties.priceFloor)
        assertEquals(85.0, pricingProperties.priceCeiling)
        assertEquals(50.0, pricingProperties.targetAvgPrice)
        assertEquals(52.0, pricingProperties.maxAvgPriceThreshold)
    }
}
