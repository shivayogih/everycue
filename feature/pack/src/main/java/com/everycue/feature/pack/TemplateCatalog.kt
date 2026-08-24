package com.everycue.feature.pack

import androidx.annotation.StringRes

data class PackingTemplate(
    val id: String,
    @StringRes val titleResource: Int,
    @StringRes val subtitleResource: Int,
    val emoji: String,
    val suggestedDays: Int,
    val items: List<TemplateItem>,
)

data class TemplateItem(
    @StringRes val nameResource: Int,
    val category: PackingCategory,
    val quantity: Int = 1,
)

object TemplateCatalog {
    val all: List<PackingTemplate> = listOf(
        PackingTemplate(
            id = "weekend",
            titleResource = R.string.template_weekend_title,
            subtitleResource = R.string.template_weekend_subtitle,
            emoji = "🚗",
            suggestedDays = 2,
            items = listOf(
                TemplateItem(R.string.item_wallet_id, PackingCategory.DOCUMENTS),
                TemplateItem(R.string.item_tshirts, PackingCategory.CLOTHING, 2),
                TemplateItem(R.string.item_change_clothes, PackingCategory.CLOTHING),
                TemplateItem(R.string.item_toothbrush, PackingCategory.TOILETRIES),
                TemplateItem(R.string.item_phone_charger, PackingCategory.TECH),
                TemplateItem(R.string.item_water_bottle, PackingCategory.ESSENTIALS),
            ),
        ),
        PackingTemplate(
            id = "business",
            titleResource = R.string.template_business_title,
            subtitleResource = R.string.template_business_subtitle,
            emoji = "💼",
            suggestedDays = 3,
            items = listOf(
                TemplateItem(R.string.item_government_id, PackingCategory.DOCUMENTS), TemplateItem(R.string.item_travel_tickets, PackingCategory.DOCUMENTS), TemplateItem(R.string.item_formal_shirts, PackingCategory.CLOTHING, 2), TemplateItem(R.string.item_laptop, PackingCategory.TECH), TemplateItem(R.string.item_laptop_charger, PackingCategory.TECH), TemplateItem(R.string.item_presentation_backup, PackingCategory.TECH), TemplateItem(R.string.item_business_cards, PackingCategory.EXTRAS),
            ),
        ),
        PackingTemplate(
            id = "beach",
            titleResource = R.string.template_beach_title,
            subtitleResource = R.string.template_beach_subtitle,
            emoji = "🏖️",
            suggestedDays = 4,
            items = listOf(
                TemplateItem(R.string.item_swimwear, PackingCategory.CLOTHING, 2), TemplateItem(R.string.item_sunscreen, PackingCategory.HEALTH), TemplateItem(R.string.item_sunglasses, PackingCategory.ESSENTIALS), TemplateItem(R.string.item_sandals, PackingCategory.CLOTHING), TemplateItem(R.string.item_power_bank, PackingCategory.TECH), TemplateItem(R.string.item_reusable_water, PackingCategory.ESSENTIALS),
            ),
        ),
        PackingTemplate(
            id = "hiking",
            titleResource = R.string.template_hiking_title,
            subtitleResource = R.string.template_hiking_subtitle,
            emoji = "🥾",
            suggestedDays = 1,
            items = listOf(
                TemplateItem(R.string.item_trail_shoes, PackingCategory.CLOTHING), TemplateItem(R.string.item_rain_jacket, PackingCategory.CLOTHING), TemplateItem(R.string.item_first_aid, PackingCategory.HEALTH), TemplateItem(R.string.item_offline_map, PackingCategory.TECH), TemplateItem(R.string.item_snacks, PackingCategory.ESSENTIALS, 3), TemplateItem(R.string.item_water, PackingCategory.ESSENTIALS, 2), TemplateItem(R.string.item_torch, PackingCategory.TECH),
            ),
        ),
    )

    fun find(id: String?): PackingTemplate? = all.firstOrNull { it.id == id }
}

