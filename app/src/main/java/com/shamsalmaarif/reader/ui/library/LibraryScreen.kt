package com.shamsalmaarif.reader.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shamsalmaarif.reader.data.database.entities.ReadEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onReadClick: (String) -> Unit,
    onAddClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val reads by viewModel.reads.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    var contextMenuRead by remember { mutableStateOf<ReadEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("شمس المعارف") },
                actions = {
                    IconButton(onClick = { viewModel.toggleArchived() }) {
                        Icon(
                            if (showArchived) Icons.Default.Inbox else Icons.Default.Archive,
                            contentDescription = if (showArchived) "Library" else "Archive"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearch(it) },
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (reads.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (showArchived) "No archived items" else "Add your first book or article",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(reads, key = { it.readId }) { read ->
                        ReadListItem(
                            read = read,
                            onClick = { onReadClick(read.readId) },
                            onLongClick = { contextMenuRead = read }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }

    contextMenuRead?.let { read ->
        ReadContextMenu(
            read = read,
            onDismiss = { contextMenuRead = null },
            onArchive = {
                if (showArchived) viewModel.unarchive(read.readId)
                else viewModel.archive(read.readId)
                contextMenuRead = null
            },
            onDelete = {
                viewModel.delete(read.readId)
                contextMenuRead = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadListItem(
    read: ReadEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val progress = if (read.charCount > 0) read.lastListenedCharOffset.toFloat() / read.charCount else 0f

    ListItem(
        headlineContent = {
            Text(
                read.title ?: "Untitled",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                if (!read.author.isNullOrBlank()) {
                    Text(read.author, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (progress > 0f) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp)
                    )
                }
            }
        },
        leadingContent = {
            Box(
                Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (read.originalFileType?.lowercase()) {
                        "pdf" -> Icons.Default.PictureAsPdf
                        "epub" -> Icons.Default.Book
                        "url" -> Icons.Default.Language
                        else -> Icons.Default.Article
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}

@Composable
private fun ReadContextMenu(
    read: ReadEntity,
    onDismiss: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; onDismiss() },
            title = { Text("Delete?") },
            text = { Text("Delete \"${read.title ?: "this item"}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDismiss() }) { Text("Cancel") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(read.title ?: "Options", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    TextButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                        Text(if (read.isArchived) "Move to Library" else "Archive")
                    }
                    TextButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }
}
