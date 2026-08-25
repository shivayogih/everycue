package com.everycue.feature.pack

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun PackingCategory.displayName(): String = stringResource(
    when (this) {
        PackingCategory.DOCUMENTS -> R.string.category_documents
        PackingCategory.CLOTHING -> R.string.category_clothing
        PackingCategory.TOILETRIES -> R.string.category_toiletries
        PackingCategory.TECH -> R.string.category_tech
        PackingCategory.HEALTH -> R.string.category_health
        PackingCategory.ESSENTIALS -> R.string.category_essentials
        PackingCategory.EXTRAS -> R.string.category_extras
    },
)
