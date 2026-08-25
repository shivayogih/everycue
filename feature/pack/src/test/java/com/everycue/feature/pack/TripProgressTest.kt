package com.everycue.feature.pack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TripProgressTest {
    @Test
    fun progress_isZero_whenTripHasNoItems() {
        assertEquals(0f, Trip(id = 1, name = "Empty").progress)
    }

    @Test
    fun progress_reflectsPackedItems() {
        val trip = Trip(
            id = 2,
            name = "Test",
            items = listOf(
                PackingItem(id = 1, name = "One", isPacked = true),
                PackingItem(id = 2, name = "Two", isPacked = false),
                PackingItem(id = 3, name = "Three", isPacked = true),
                PackingItem(id = 4, name = "Four", isPacked = false),
            ),
        )
        assertEquals(0.5f, trip.progress)
        assertEquals(2, trip.packedCount)
        assertEquals(4, trip.totalCount)
    }

    @Test
    fun packedOverrides_updateOnlyRequestedItem() {
        val original = PackData(
            trips = listOf(
                Trip(
                    id = 1,
                    name = "Trip",
                    items = listOf(
                        PackingItem(id = 10, name = "One"),
                        PackingItem(id = 11, name = "Two", isPacked = true),
                    ),
                ),
            ),
        )

        assertSame(original, original.withPackedOverrides(emptyMap()))
        val updated = original.withPackedOverrides(mapOf(10L to true, 11L to false))
        assertTrue(updated.trips.single().items[0].isPacked)
        assertFalse(updated.trips.single().items[1].isPacked)
    }

    @Test
    fun packingSections_filterAndOrderMoreThanOneHundredItems() {
        val items = (0 until 250).map { index ->
            PackingItem(
                id = index.toLong(),
                name = if (index % 25 == 0) "Match $index" else "Item $index",
                category = PackingCategory.entries[index % PackingCategory.entries.size],
                isPacked = index % 3 == 0,
                position = 249 - index,
            )
        }

        val sections = buildPackingSections(items, "match")
        val visibleItems = sections.flatMap(PackingSection::items)

        assertEquals(10, visibleItems.size)
        assertTrue(visibleItems.all { it.name.startsWith("Match") })
        sections.forEach { section ->
            assertEquals(
                section.items.sortedWith(compareBy<PackingItem>(PackingItem::isPacked).thenBy(PackingItem::position)),
                section.items,
            )
        }
    }

    @Test(expected = PackValidationException::class)
    fun packingItemRejectsDummyName() {
        validatePackingItem("@@@", 1)
    }

    @Test(expected = PackValidationException::class)
    fun packingItemRejectsOutOfRangeQuantity() {
        validatePackingItem("Passport", 100)
    }
}
