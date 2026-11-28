package com.mimo.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph
import com.mimo.companion.domain.custom.CustomPhrase
import com.mimo.companion.domain.custom.PhraseEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPhrasesScreen(onBack: () -> Unit) {
    var phrases by remember { mutableStateOf(Graph.customPhrases.all()) }
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
            Text("العبارات المخصصة", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showAdd = true }) { Text("+", fontSize = 24.sp) }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(phrases, key = { it.id }) { phrase ->
                PhraseRow(
                    phrase = phrase,
                    onToggle = {
                        Graph.customPhrases.toggle(phrase.id)
                        phrases = Graph.customPhrases.all()
                    },
                    onDelete = {
                        Graph.customPhrases.remove(phrase.id)
                        phrases = Graph.customPhrases.all()
                    }
                )
            }
            if (phrases.isEmpty()) {
                item { Text("لا توجد عبارات مخصصة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    if (showAdd) {
        AddPhraseDialog(
            onAdd = { phrase ->
                Graph.customPhrases.add(phrase)
                phrases = Graph.customPhrases.all()
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun PhraseRow(phrase: CustomPhrase, onToggle: () -> Unit, onDelete: () -> Unit) {
    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(phrase.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(phrase.event.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = phrase.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) { Text("🗑", fontSize = 16.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPhraseDialog(onAdd: (CustomPhrase) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var event by remember { mutableStateOf(PhraseEvent.TAP) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عبارة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("نص العبارة") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = event.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الحدث") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        PhraseEvent.values().forEach { e ->
                            DropdownMenuItem(
                                text = { Text(e.name) },
                                onClick = { event = e; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onAdd(CustomPhrase(text = text, event = event)) }) {
                Text("إضافة")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
