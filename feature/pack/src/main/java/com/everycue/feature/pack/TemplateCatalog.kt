package com.everycue.feature.pack

data class PackingTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val suggestedDays: Int,
    val items: List<TemplateItem>,
)

data class TemplateItem(
    val name: String,
    val category: PackingCategory,
    val quantity: Int = 1,
)

object TemplateCatalog {
    val all: List<PackingTemplate> = listOf(
        PackingTemplate(
            id = "weekend",
            title = "Weekend getaway",
            subtitle = "A light two-day checklist",
            emoji = "🚗",
            suggestedDays = 2,
            items = listOf(
                TemplateItem("Wallet and ID", PackingCategory.DOCUMENTS),
                TemplateItem("T-shirts", PackingCategory.CLOTHING, 2),
                TemplateItem("Change of clothes", PackingCategory.CLOTHING),
                TemplateItem("Toothbrush", PackingCategory.TOILETRIES),
                TemplateItem("Phone charger", PackingCategory.TECH),
                TemplateItem("Water bottle", PackingCategory.ESSENTIALS),
            ),
        ),
        PackingTemplate(
            id = "business",
            title = "Business trip",
            subtitle = "Work-ready without last-minute panic",
            emoji = "💼",
            suggestedDays = 3,
            items = listOf(
                TemplateItem("Government ID", PackingCategory.DOCUMENTS),
                TemplateItem("Travel tickets", PackingCategory.DOCUMENTS),
                TemplateItem("Formal shirts", PackingCategory.CLOTHING, 2),
                TemplateItem("Laptop", PackingCategory.TECH),
                TemplateItem("Laptop charger", PackingCategory.TECH),
                TemplateItem("Presentation backup", PackingCategory.TECH),
                TemplateItem("Business cards", PackingCategory.EXTRAS),
            ),
        ),
        PackingTemplate(
            id = "beach",
            title = "Beach holiday",
            subtitle = "Sun, swim and relaxed travel",
            emoji = "🏖️",
            suggestedDays = 4,
            items = listOf(
                TemplateItem("Swimwear", PackingCategory.CLOTHING, 2),
                TemplateItem("Sunscreen", PackingCategory.HEALTH),
                TemplateItem("Sunglasses", PackingCategory.ESSENTIALS),
                TemplateItem("Sandals", PackingCategory.CLOTHING),
                TemplateItem("Power bank", PackingCategory.TECH),
                TemplateItem("Reusable water bottle", PackingCategory.ESSENTIALS),
            ),
        ),
        PackingTemplate(
            id = "hiking",
            title = "Hiking day",
            subtitle = "Trail essentials and safety basics",
            emoji = "🥾",
            suggestedDays = 1,
            items = listOf(
                TemplateItem("Trail shoes", PackingCategory.CLOTHING),
                TemplateItem("Rain jacket", PackingCategory.CLOTHING),
                TemplateItem("First-aid kit", PackingCategory.HEALTH),
                TemplateItem("Offline map", PackingCategory.TECH),
                TemplateItem("Snacks", PackingCategory.ESSENTIALS, 3),
                TemplateItem("Water", PackingCategory.ESSENTIALS, 2),
                TemplateItem("Torch", PackingCategory.TECH),
            ),
        ),
    )

    fun find(id: String?): PackingTemplate? = all.firstOrNull { it.id == id }
}
