package com.shamsalmaarif.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shamsalmaarif.reader.tts.TtsLanguage
import com.shamsalmaarif.reader.tts.TtsState
import com.shamsalmaarif.reader.tts.TtsWord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    readId: String,
    fullText: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val read by viewModel.read.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val currentWordIndex by viewModel.currentWordIndex.collectAsState()
    val words by viewModel.words.collectAsState()
    val language by viewModel.language.collectAsState()
    val speed by viewModel.speed.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(readId) {
        viewModel.loadRead(readId)
        viewModel.setText(fullText)
    }

    val isPlaying = ttsState is TtsState.Playing
    val isPaused = ttsState is TtsState.Paused
    val isLoading = ttsState is TtsState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(read?.title ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.stop(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            PlayerControls(
                isPlaying = isPlaying,
                isPaused = isPaused,
                isLoading = isLoading,
                onPlay = { viewModel.play() },
                onPause = { viewModel.pause() },
                onStop = { viewModel.stop() }
            )
        }
    ) { padding ->
        TextDisplay(
            words = words,
            currentWordIndex = currentWordIndex,
            modifier = Modifier.padding(padding)
        )
    }

    if (showSettings) {
        ReaderSettingsSheet(
            language = language,
            speed = speed,
            onLanguageChange = { viewModel.setLanguage(it) },
            onSpeedChange = { viewModel.setSpeed(it) },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun TextDisplay(
    words: List<TtsWord>,
    currentWordIndex: Int,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentWordIndex) {
        if (currentWordIndex >= 0 && words.isNotEmpty()) {
            val lineIndex = currentWordIndex / 12
            listState.animateScrollToItem(lineIndex.coerceAtMost((words.size / 12).coerceAtLeast(0)))
        }
    }

    if (words.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val linesOfWords = words.chunked(12)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(linesOfWords.size) { lineIdx ->
            val lineWords = linesOfWords[lineIdx]
            val lineStartIndex = lineIdx * 12

            val annotated = buildAnnotatedString {
                lineWords.forEachIndexed { wi, word ->
                    val globalIdx = lineStartIndex + wi
                    if (globalIdx == currentWordIndex) {
                        withStyle(SpanStyle(
                            background = Color(0xFFFFD700),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )) {
                            append(word.word)
                        }
                    } else {
                        append(word.word)
                    }
                    if (wi < lineWords.size - 1) append(" ")
                }
            }

            Text(
                text = annotated,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    isPaused: Boolean,
    isLoading: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    BottomAppBar {
        Spacer(Modifier.weight(1f))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
        } else {
            IconButton(onClick = onStop, enabled = isPlaying || isPaused) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(24.dp))
            FilledIconButton(
                onClick = if (isPlaying) onPause else onPlay,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    language: TtsLanguage,
    speed: Float,
    onLanguageChange: (TtsLanguage) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)
        ) {
            Text("Voice Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            Text("Language", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TtsLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = language == lang,
                        onClick = { onLanguageChange(lang) },
                        label = { Text(lang.label) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Speed: ${String.format("%.1f", speed)}x",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..2.0f,
                steps = 5
            )
        }
    }
}
