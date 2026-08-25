package com.everycue.feature.pack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripLinkTest {
    private val trip = Trip(id = 7, name = "Test trip")

    @Test
    fun `link overrides update immediately without duplicating records`() {
        val key = TripLinkKey(7, TripLinkEntityType.TRACK, "track-1")
        val linked = PackData(trips = listOf(trip)).withLinkOverrides(mapOf(key to true))
        val repeated = linked.withLinkOverrides(mapOf(key to true))

        assertEquals(listOf(TripLink(7, TripLinkEntityType.TRACK, "track-1")), repeated.tripLinks)
        assertTrue(repeated.tripLinks.single().relevanceType == TripRelevanceType.TRAVEL_CONSUMABLE)
    }

    @Test
    fun `unlink override removes only its exact cross feature link`() {
        val track = TripLink(7, TripLinkEntityType.TRACK, "same-id")
        val renewal = TripLink(7, TripLinkEntityType.RENEW, "same-id")
        val result = PackData(listOf(trip), listOf(track, renewal)).withLinkOverrides(
            mapOf(track.key() to false),
        )

        assertFalse(result.tripLinks.contains(track))
        assertEquals(listOf(renewal), result.tripLinks)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `link to missing trip is rejected by backup validation`() {
        PackData(
            trips = listOf(trip),
            tripLinks = listOf(TripLink(99, TripLinkEntityType.RENEW, "renew-1")),
        ).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate link is rejected by backup validation`() {
        val link = TripLink(7, TripLinkEntityType.RENEW, "renew-1")
        PackData(trips = listOf(trip), tripLinks = listOf(link, link)).validate()
    }
}
