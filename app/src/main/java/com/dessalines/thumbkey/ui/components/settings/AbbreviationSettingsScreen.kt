package com.dessalines.thumbkey.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dessalines.thumbkey.R
import com.dessalines.thumbkey.db.AbbreviationsUpdate
import com.dessalines.thumbkey.db.AppSettingsViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbbreviationSettingsScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val settings by appSettingsViewModel.appSettings.observeAsState()
    val abbreviationsJson = settings?.abbreviations ?: "{}"
    val abbreviations: Map<String, String> = remember(abbreviationsJson) {
        try {
            Json.decodeFromString(abbreviationsJson)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<String?>(null) }
    var keyInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.abbreviations)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingKey = null
                keyInput = ""
                valueInput = ""
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    ) { padding ->
        if (abbreviations.isEmpty()) {
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.no_abbreviations))
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(abbreviations.toList()) { (key, value) ->
                    AbbreviationItem(
                        key = key,
                        value = value,
                        onEdit = {
                            editingKey = key
                            keyInput = key
                            valueInput = value
                            showDialog = true
                        },
                        onDelete = {
                            val newMap = abbreviations.toMutableMap()
                            newMap.remove(key)
                            val update = AbbreviationsUpdate(settings?.id ?: 1, Json.encodeToString(newMap))
                            appSettingsViewModel.updateAbbreviations(update)
                        },
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(stringResource(if (editingKey == null) R.string.add_abbreviation else R.string.edit_abbreviation))
            },
            text = {
                Column {
                    TextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text(stringResource(R.string.abbreviation_key)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    TextField(
                        value = valueInput,
                        onValueChange = { valueInput = it },
                        label = { Text(stringResource(R.string.abbreviation_value)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newMap = abbreviations.toMutableMap()
                    if (editingKey != null) {
                        newMap.remove(editingKey)
                    }
                    if (keyInput.isNotBlank() && valueInput.isNotBlank()) {
                        newMap[keyInput] = valueInput
                    }
                    val update = AbbreviationsUpdate(settings?.id ?: 1, Json.encodeToString(newMap))
                    appSettingsViewModel.updateAbbreviations(update)
                    showDialog = false
                }) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun AbbreviationItem(
    key: String,
    value: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = key, style = MaterialTheme.typography.titleMedium)
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }
    }
}
