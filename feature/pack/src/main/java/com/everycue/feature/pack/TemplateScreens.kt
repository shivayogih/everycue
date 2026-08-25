package com.everycue.feature.pack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everycue.feature.pack.PackingCategory
import com.everycue.feature.pack.PackingTemplate
import com.everycue.feature.pack.TemplateCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(onTemplateClick: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.templates), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.templates_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(TemplateCatalog.all, key = PackingTemplate::id) { template ->
                ElevatedCard(
                    onClick = { onTemplateClick(template.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(template.emoji, style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(template.titleResource), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(template.subtitleResource), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(if (template.suggestedDays == 1) R.string.template_summary_one_day else R.string.template_summary_days, template.items.size, template.suggestedDays),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Icon(Icons.Default.Checklist, contentDescription = null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    templateId: String,
    onBack: () -> Unit,
    onUseTemplate: () -> Unit,
) {
    val template = TemplateCatalog.find(templateId)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(template?.let { stringResource(it.titleResource) } ?: stringResource(R.string.template)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            if (template != null) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = onUseTemplate,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    ) {
                        Text(stringResource(R.string.use_this_template))
                    }
                }
            }
        },
    ) { padding ->
        if (template == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.template_not_found))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(template.emoji, style = MaterialTheme.typography.displayLarge)
                Text(stringResource(template.subtitleResource), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
            }
            PackingCategory.entries.forEach { category ->
                val categoryItems = template.items.filter { it.category == category }
                if (categoryItems.isNotEmpty()) {
                    item {
                        Text(
                            "${category.emoji} ${category.displayName()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(categoryItems, key = { "${category.name}-${it.nameResource}" }) { item ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(item.nameResource), modifier = Modifier.weight(1f))
                                if (item.quantity > 1) Text(stringResource(R.string.quantity_multiplier, item.quantity))
                            }
                        }
                    }
                }
            }
        }
    }
}
