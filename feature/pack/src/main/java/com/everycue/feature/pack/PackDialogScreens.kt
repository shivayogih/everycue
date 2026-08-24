package com.everycue.feature.pack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everycue.feature.pack.PackingCategory

@Composable
fun AddItemDialogScreen(
    onDismiss: () -> Unit,
    onAdd: (String, PackingCategory, Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf(PackingCategory.ESSENTIALS.name) }
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    var categoryMenuOpen by rememberSaveable { mutableStateOf(false) }
    val category = PackingCategory.valueOf(categoryName)

    DialogSurface {
        Text(stringResource(R.string.add_packing_item), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.item_name)) },
            placeholder = { Text(stringResource(R.string.item_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box {
            OutlinedButton(
                onClick = { categoryMenuOpen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${category.emoji} ${category.displayName()}")
            }
            DropdownMenu(
                expanded = categoryMenuOpen,
                onDismissRequest = { categoryMenuOpen = false },
            ) {
                PackingCategory.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("${option.emoji} ${option.displayName()}") },
                        onClick = {
                            categoryName = option.name
                            categoryMenuOpen = false
                        },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.quantity), fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { quantity = (quantity - 1).coerceAtLeast(1) }) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease_quantity))
                }
                Text(quantity.toString(), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { quantity = (quantity + 1).coerceAtMost(99) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase_quantity))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            Button(onClick = { onAdd(name, category, quantity) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.add))
            }
        }
    }
}

@Composable
fun DeleteTripDialogScreen(
    tripName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.delete_trip_title),
        message = stringResource(R.string.delete_trip_message, tripName),
        confirmLabel = stringResource(R.string.delete),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun ResetDataDialogScreen(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.delete_all_title),
        message = stringResource(R.string.delete_all_message),
        confirmLabel = stringResource(R.string.delete_all),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    DialogSurface {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            Button(onClick = onConfirm) { Text(confirmLabel) }
        }
    }
}

@Composable
private fun DialogSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .padding(20.dp)
            .widthIn(min = 300.dp, max = 520.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

