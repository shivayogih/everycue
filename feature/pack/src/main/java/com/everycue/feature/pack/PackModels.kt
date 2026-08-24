package com.everycue.feature.pack

import kotlinx.serialization.Serializable

@Serializable
data class PackData(
    val trips: List<Trip> = emptyList(),
)

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
enum class PackingCategory(
    val label: String,
    val emoji: String,
) {
    DOCUMENTS("Documents", "🪪"),
    CLOTHING("Clothing", "👕"),
    TOILETRIES("Toiletries", "🧴"),
    TECH("Tech", "🔌"),
    HEALTH("Health", "🩹"),
    ESSENTIALS("Essentials", "🎒"),
    EXTRAS("Extras", "✨"),
}

data class TripDraft(
    val name: String,
    val destination: String,
    val startDateMillis: Long?,
    val endDateMillis: Long?,
    val templateId: String? = null,
)
