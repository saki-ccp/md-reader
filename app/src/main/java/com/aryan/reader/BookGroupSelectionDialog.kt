package com.aryan.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun BookGroupSelectionDialog(
    shelves: List<Shelf>,
    isSaving: Boolean,
    error: String?,
    onSelect: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val trimmedName = name.trim()
    val duplicate = trimmedName.equals(stringResource(R.string.group_ungrouped), ignoreCase = true) ||
        shelves.any { it.name.equals(trimmedName, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.group_add_documents)) },
        text = {
            Column {
                Text(stringResource(R.string.group_selection_hint))
                if (error != null) Text(error, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(shelves, key = { it.id }) { shelf ->
                        TextButton(
                            onClick = { onSelect(shelf.id) },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Text(shelf.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.shelf_name_hint)) },
                    singleLine = true,
                    enabled = !isSaving,
                    isError = duplicate,
                    supportingText = { if (duplicate) Text(stringResource(R.string.error_shelf_exists)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(trimmedName) },
                enabled = trimmedName.isNotEmpty() && !duplicate && !isSaving
            ) {
                Text(stringResource(R.string.group_create_and_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
