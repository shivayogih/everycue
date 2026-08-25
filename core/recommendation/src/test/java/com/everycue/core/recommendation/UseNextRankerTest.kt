package com.everycue.core.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UseNextRankerTest {
    private val today = 20_000L

    @Test
    fun `orders today before soon before later and excludes expired`() {
        val result = UseNextRanker.rank(
            listOf(item("expired", -1), item("later", 20), item("soon", 3), item("today", 0)),
            today,
        )

        assertEquals(listOf("today", "soon", "later"), result.map(UseNextScore::itemId))
        assertTrue(result.none { it.itemId == "expired" })
    }

    @Test
    fun `assigns transparent reason codes at every expiry boundary`() {
        val result = UseNextRanker.rank(
            listOf(item("expired", -1), item("today", 0), item("soon", 7), item("fresh", 8)),
            today,
        ).associateBy(UseNextScore::itemId)

        assertEquals(UseNextReasonCode.EXPIRES_TODAY, result.getValue("today").reasonCodes.single())
        assertEquals(UseNextReasonCode.EXPIRING_SOON, result.getValue("soon").reasonCodes.single())
        assertEquals(UseNextReasonCode.EXPIRY_UPCOMING, result.getValue("fresh").reasonCodes.single())
        assertTrue("expired" !in result)
    }

    @Test
    fun `ranking is stable for the same input`() {
        val inputs = listOf(item("b", 2, "Banana"), item("a", 2, "Apple"))

        assertEquals(UseNextRanker.rank(inputs, today), UseNextRanker.rank(inputs.reversed(), today))
    }

    @Test
    fun `handles large inventories while excluding expired entries`() {
        val inputs = (0 until 10_000).map { index ->
            item(
                id = "item-$index",
                remaining = (index % 401 - 50).toLong(),
                name = "Item $index",
            )
        }

        val result = UseNextRanker.rank(inputs, today)

        assertEquals(inputs.count { it.expiryEpochDay >= today }, result.size)
        assertTrue(result.zipWithNext().all { (first, second) -> first.score >= second.score })
    }

    private fun item(id: String, remaining: Long, name: String = id) = UseNextInput(
        itemId = id,
        displayName = name,
        category = "GROCERY",
        expiryEpochDay = today + remaining,
        warningDays = 7,
    )
}
