package com.everycue.feature.pack

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable

@Serializable
data class PackData(
    val trips: List<Trip> = emptyList(),
) {
    fun validate() {
        require(trips.distinctBy(Trip::id).size == trips.size) { "Duplicate trip IDs." }
        trips.forEach { trip ->
            TripDraft(trip.name, trip.destination, trip.startDateMillis, trip.endDateMillis).validate()
            require(trip.items.distinctBy(PackingItem::id).size == trip.items.size) { "Duplicate packing item IDs." }
            trip.items.forEach { item -> validatePackingItem(item.name, item.quantity) }
        }
    }
}

data class PackUiState(
    val data: PackData = PackData(),
    val isBusy: Boolean = false,
)

/** Applies short-lived UI overrides while encrypted persistence catches up. */
internal fun PackData.withPackedOverrides(overrides: Map<Long, Boolean>): PackData {
    if (overrides.isEmpty()) return this
    var dataChanged = false
    val updatedTrips = trips.map { trip ->
        var tripChanged = false
        val updatedItems = trip.items.map { item ->
            val packed = overrides[item.id]
            if (packed != null && packed != item.isPacked) {
                tripChanged = true
                item.copy(isPacked = packed)
            } else {
                item
            }
        }
        if (tripChanged) {
            dataChanged = true
            trip.copy(items = updatedItems)
        } else {
            trip
        }
    }
    return if (dataChanged) copy(trips = updatedTrips) else this
}

@Serializable
data class Trip(
    val id: Long,
    val name: String,
    val destination: String = "",
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val items: List<PackingItem> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val packedCount: Int get() = items.count(PackingItem::isPacked)
    val totalCount: Int get() = items.size
    val progress: Float get() = if (items.isEmpty()) 0f else packedCount.toFloat() / items.size
}

@Serializable
data class PackingItem(
    val id: Long,
    val name: String,
    val category: PackingCategory = PackingCategory.ESSENTIALS,
    val quantity: Int = 1,
    val isPacked: Boolean = false,
    val position: Int = 0,
)

@Serializable
enum class PackingCategory(val emoji: String) {
    DOCUMENTS("🪪"), CLOTHING("👕"), TOILETRIES("🧴"), TECH("🔌"), HEALTH("🩹"), ESSENTIALS("🎒"), EXTRAS("✨"),
}

data class TripDraft(
    val name: String,
    val destination: String,
    val startDateMillis: Long?,
    val endDateMillis: Long?,
    val templateId: String? = null,
) {
    fun validationErrors(): TripValidationErrors = TripValidationErrors(
        name = if (name.trim().length in 2..80 && name.any(Char::isLetterOrDigit)) null else R.string.error_trip_name,
        destination = if (destination.length <= 100 && (destination.isBlank() || destination.any(Char::isLetterOrDigit))) null else R.string.error_destination,
        dates = if (startDateMillis == null || endDateMillis == null || endDateMillis >= startDateMillis) null else R.string.end_date_error,
    )

    fun validate() {
        validationErrors().firstError()?.let { throw PackValidationException(it) }
    }
}

data class TripValidationErrors(
    @StringRes val name: Int? = null,
    @StringRes val destination: Int? = null,
    @StringRes val dates: Int? = null,
) {
    val isValid: Boolean get() = firstError() == null
    fun firstError(): Int? = name ?: destination ?: dates
}

fun validatePackingItem(name: String, quantity: Int) {
    val error = when {
        name.trim().length !in 2..80 || name.none(Char::isLetterOrDigit) -> R.string.error_item_name
        quantity !in 1..99 -> R.string.error_item_quantity
        else -> null
    }
    error?.let { throw PackValidationException(it) }
}

class PackValidationException(@StringRes val messageResource: Int) : IllegalArgumentException()
