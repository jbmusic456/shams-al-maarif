package com.shamsalmaarif.reader.ui.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onImported: (readId: String, text: String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var urlText by remember { mutableStateOf("") }
    var pastedText by remember { mutableStateOf("") }
    var pastedTitle by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(state) {
        if (state is ImportState.Success) {
            val s = state as ImportState.Success
            onImported(s.readId, s.text)
            viewModel.resetState()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFromUri(it) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Add Content", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("File") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("URL") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    text = { Text("Paste") })
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> FileTab(
                    onPickFile = { filePicker.launch(arrayOf("application/pdf", "application/epub+zip", "text/plain", "*/*")) }
                )
                1 -> UrlTab(
                    urlText = urlText,
                    onUrlChange = { urlText = it },
                    onFetch = { viewModel.importFromUrl(urlText) },
                    enabled = state !is ImportState.Loading
                )
                2 -> PasteTab(
                    title = pastedTitle,
                    text = pastedText,
                    onTitleChange = { pastedTitle = it },
                    onTextChange = { pastedText = it },
                    onSave = { viewModel.importFromText(pastedText, pastedTitle) },
                    enabled = state !is ImportState.Loading
                )
            }

            if (state is ImportState.Loading) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Processing...")
                }
            }

            if (state is ImportState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    (state as ImportState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FileTab(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onPickFile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Browse Files (PDF, EPUB, TXT)")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Supported formats: PDF, EPUB, TXT",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UrlTab(
    urlText: String,
    onUrlChange: (String) -> Unit,
    onFetch: () -> Unit,
    enabled: Boolean
) {
    Column {
        OutlinedTextField(
            value = urlText,
            onValueChange = onUrlChange,
            label = { Text("Article URL") },
            placeholder = { Text("https://...") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onFetch,
            enabled = enabled && urlText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch Article")
        }
    }
}

@Composable
private fun PasteTab(
    title: String,
    text: String,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    enabled: Boolean
) {
    Column {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text("Paste your text here") },
            modifier = Modifier.fillMaxWidth().height(160.dp),
            maxLines = 8
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSave,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Read")
        }
    }
}
