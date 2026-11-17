package com.example

import com.example.data.local.ProductEntity
import com.example.model.calculateInclusiveVatUgx
import com.example.model.formatUgx
import com.example.util.KioskAudioManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun vatAndCurrencyFormatting_isAccurate() {
        assertEquals("UGX 5,000", formatUgx(5000))
        assertEquals(763, calculateInclusiveVatUgx(5000))
        assertEquals(1525, calculateInclusiveVatUgx(10000))
    }

    @Test
    fun voiceOrderParser_extractsMultipleFlavorsAndQuantities() {
        val catalog = listOf(
            ProductEntity("p1", "A1", "Strawberry Yogurt", "330ml", 5000, 190, 12, 15, 20, 5, "Popular", "Strawberry", "strawberry"),
            ProductEntity("p2", "A2", "Vanilla Yogurt", "330ml", 5000, 180, 15, 14, 20, 5, "Classic", "Vanilla", "vanilla"),
            ProductEntity("p3", "A3", "Chocolate Yogurt", "330ml", 5500, 220, 10, 12, 20, 5, "Gourmet", "Chocolate", "chocolate")
        )
        val matches = KioskAudioManager.parseVoiceOrder("Two strawberry yogurts and one vanilla", catalog)
        assertEquals(2, matches.size)
        assertEquals("p1", matches[0].product.id)
        assertEquals(2, matches[0].quantity)
        assertEquals("p2", matches[1].product.id)
        assertEquals(1, matches[1].quantity)
    }
}
