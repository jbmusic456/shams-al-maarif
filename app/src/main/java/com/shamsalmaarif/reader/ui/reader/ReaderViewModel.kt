package com.shamsalmaarif.reader.ui.reader

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shamsalmaarif.reader.data.database.entities.ReadEntity
import com.shamsalmaarif.reader.data.repository.ReadsRepository
import com.shamsalmaarif.reader.player.PlayerService
import com.shamsalmaarif.reader.tts.TtsEngine
import com.shamsalmaarif.reader.tts.TtsLanguage
import com.shamsalmaarif.reader.tts.TtsState
import com.shamsalmaarif.reader.tts.TtsWord
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: ReadsRepository,
    private val ttsEngine: TtsEngine
) : ViewModel() {

    private val _read = MutableStateFlow<ReadEntity?>(null)
    val read: StateFlow<ReadEntity?> = _read

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    private val _currentWordIndex = MutableStateFlow(-1)
    val currentWordIndex: StateFlow<Int> = _currentWordIndex

    private val _words = MutableStateFlow<List<TtsWord>>(emptyList())
    val words: StateFlow<List<TtsWord>> = _words

    val ttsState: StateFlow<TtsState> = ttsEngine.state

    private val _language = MutableStateFlow(TtsLanguage.ENGLISH)
    val language: StateFlow<TtsLanguage> = _language

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed

    private var playJob: Job? = null

    fun loadRead(readId: String) {
        viewModelScope.launch {
            val entity = repo.getReadById(readId) ?: return@launch
            _read.value = entity
            _language.value = TtsLanguage.fromCode(entity.lastUsedLanguage)
            _speed.value = entity.lastUsedSpeed
        }
    }

    fun setText(fullText: String) {
        _text.value = fullText
        _words.value = ttsEngine.buildWordList(fullText)
    }

    fun play() {
        val readEntity = _read.value ?: return
        val fullText = _text.value.ifBlank { return }
        val lang = _language.value
        val speed = _speed.value

        val serviceIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_TEXT, fullText)
            putExtra(PlayerService.EXTRA_LANG, lang.code)
            putExtra(PlayerService.EXTRA_SPEED, speed)
            putExtra(PlayerService.EXTRA_READ_ID, readEntity.readId)
            putExtra(PlayerService.EXTRA_TITLE, readEntity.title ?: "شمس المعارف")
        }
        context.startForegroundService(serviceIntent)

        playJob?.cancel()
        playJob = viewModelScope.launch {
            ttsEngine.speak(
                text = fullText,
                language = lang,
                speed = speed,
                onWordChanged = { idx ->
                    _currentWordIndex.value = idx
                },
                onChunkDone = { offset ->
                    viewModelScope.launch {
                        readEntity.let { repo.updateProgress(it.readId, offset) }
                    }
                }
            )
        }
    }

    fun pause() {
        ttsEngine.pause()
        val serviceIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PAUSE
        }
        context.startService(serviceIntent)
    }

    fun stop() {
        ttsEngine.stop()
        playJob?.cancel()
        val serviceIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }

    fun setLanguage(lang: TtsLanguage) {
        _language.value = lang
        _read.value?.let { entity ->
            viewModelScope.launch { repo.updateLanguage(entity.readId, lang.code) }
        }
    }

    fun setSpeed(speed: Float) {
        _speed.value = speed
        _read.value?.let { entity ->
            viewModelScope.launch { repo.updateSpeed(entity.readId, speed) }
        }
    }

    override fun onCleared() {
        ttsEngine.stop()
        super.onCleared()
    }
}
