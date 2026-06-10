package com.shamsalmaarif.reader.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class TtsLanguage(val code: String, val gtCode: String, val locale: Locale, val label: String) {
    ENGLISH("en", "en", Locale.ENGLISH, "English"),
    URDU("ur", "ur", Locale("ur"), "اردو"),
    HINDI("hi", "hi", Locale("hi"), "हिंदी"),
    ARABIC("ar", "ar", Locale("ar"), "العربية");

    companion object {
        fun fromCode(code: String) = entries.find { it.code == code } ?: ENGLISH
    }
}

data class TtsWord(val word: String, val startIndex: Int, val endIndex: Int)

sealed class TtsState {
    object Idle : TtsState()
    object Loading : TtsState()
    data class Playing(val wordIndex: Int, val words: List<TtsWord>) : TtsState()
    object Paused : TtsState()
    object Finished : TtsState()
    data class Error(val message: String) : TtsState()
}

@Singleton
class TtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state

    private var androidTts: TextToSpeech? = null
    private var androidTtsReady = false
    private var currentWords: List<TtsWord> = emptyList()
    private var currentWordIndex = 0
    private var currentChunks: List<Pair<String, IntRange>> = emptyList()
    private var currentChunkIndex = 0
    private var isPaused = false
    private var cacheDir: File = context.cacheDir

    private val gtBaseUrl = "https://translate.google.com/translate_tts"

    init {
        initAndroidTts()
    }

    private fun initAndroidTts() {
        androidTts = TextToSpeech(context) { status ->
            androidTtsReady = status == TextToSpeech.SUCCESS
        }
    }

    fun buildWordList(text: String): List<TtsWord> {
        val words = mutableListOf<TtsWord>()
        var i = 0
        while (i < text.length) {
            while (i < text.length && text[i].isWhitespace()) i++
            if (i >= text.length) break
            val start = i
            while (i < text.length && !text[i].isWhitespace()) i++
            words.add(TtsWord(text.substring(start, i), start, i))
        }
        return words
    }

    private fun chunkText(text: String, maxLen: Int = 180): List<Pair<String, IntRange>> {
        val chunks = mutableListOf<Pair<String, IntRange>>()
        var pos = 0
        while (pos < text.length) {
            var end = minOf(pos + maxLen, text.length)
            if (end < text.length) {
                val lastSpace = text.lastIndexOf(' ', end)
                if (lastSpace > pos) end = lastSpace
            }
            val chunk = text.substring(pos, end).trim()
            if (chunk.isNotEmpty()) chunks.add(Pair(chunk, pos until end))
            pos = end
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }
        return chunks
    }

    suspend fun speak(
        text: String,
        language: TtsLanguage = TtsLanguage.ENGLISH,
        speed: Float = 1.0f,
        onWordChanged: (Int) -> Unit = {},
        onChunkDone: (charOffset: Int) -> Unit = {}
    ) {
        _state.value = TtsState.Loading
        isPaused = false
        currentWords = buildWordList(text)
        currentChunks = chunkText(text)
        currentChunkIndex = 0
        currentWordIndex = 0

        playChunks(language, speed, onWordChanged, onChunkDone)
    }

    private suspend fun playChunks(
        language: TtsLanguage,
        speed: Float,
        onWordChanged: (Int) -> Unit,
        onChunkDone: (Int) -> Unit
    ) {
        for (i in currentChunkIndex until currentChunks.size) {
            if (isPaused) break
            currentChunkIndex = i
            val (chunk, range) = currentChunks[i]

            val audioFile = fetchGoogleTts(chunk, language.gtCode)

            if (audioFile != null) {
                playAudioFileWithHighlight(audioFile, chunk, range.first, onWordChanged)
            } else {
                playWithAndroidTts(chunk, language.locale, speed, range.first, onWordChanged)
            }

            onChunkDone(range.last)
        }
        if (!isPaused) _state.value = TtsState.Finished
    }

    private suspend fun fetchGoogleTts(text: String, langCode: String): File? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "$gtBaseUrl?ie=UTF-8&q=$encoded&tl=$langCode&client=tw-ob&ttsspeed=1"
            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val file = File(cacheDir, "tts_${UUID.randomUUID()}.mp3")
                file.writeBytes(response.body!!.bytes())
                file
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun playAudioFileWithHighlight(
        file: File,
        chunk: String,
        charOffset: Int,
        onWordChanged: (Int) -> Unit
    ) {
        val chunkWords = buildWordList(chunk)
        val msPerWord = if (chunkWords.isEmpty()) 0L else (estimateDurationMs(chunk) / chunkWords.size)

        withContext(Dispatchers.Main) {
            _state.value = TtsState.Playing(currentWordIndex, currentWords)
        }

        val player = android.media.MediaPlayer()
        val done = kotlinx.coroutines.CompletableDeferred<Unit>()
        try {
            player.setDataSource(file.path)
            player.prepare()
            player.setOnCompletionListener { done.complete(Unit) }
            player.start()

            for (wi in chunkWords.indices) {
                if (isPaused) { player.pause(); break }
                val globalIdx = findGlobalWordIndex(charOffset + chunkWords[wi].startIndex)
                currentWordIndex = globalIdx
                withContext(Dispatchers.Main) {
                    _state.value = TtsState.Playing(globalIdx, currentWords)
                    onWordChanged(globalIdx)
                }
                kotlinx.coroutines.delay(msPerWord)
            }
            done.await()
        } finally {
            player.release()
            file.delete()
        }
    }

    private fun estimateDurationMs(text: String): Long {
        val wordsPerMin = 150f
        val words = text.split("\\s+".toRegex()).size
        return ((words / wordsPerMin) * 60_000).toLong()
    }

    private fun findGlobalWordIndex(charPos: Int): Int {
        return currentWords.indexOfFirst { it.startIndex >= charPos }.takeIf { it >= 0 }
            ?: currentWords.lastIndex
    }

    private suspend fun playWithAndroidTts(
        text: String,
        locale: Locale,
        speed: Float,
        charOffset: Int,
        onWordChanged: (Int) -> Unit
    ) {
        if (!androidTtsReady) return
        val tts = androidTts ?: return

        tts.language = locale
        tts.setSpeechRate(speed)

        val words = buildWordList(text)
        val done = kotlinx.coroutines.CompletableDeferred<Unit>()

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                    _state.value = TtsState.Playing(currentWordIndex, currentWords)
                }
            }
            override fun onDone(utteranceId: String?) { done.complete(Unit) }
            override fun onError(utteranceId: String?) { done.complete(Unit) }
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val globalIdx = findGlobalWordIndex(charOffset + start)
                currentWordIndex = globalIdx
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                    _state.value = TtsState.Playing(globalIdx, currentWords)
                    onWordChanged(globalIdx)
                }
            }
        })

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        done.await()
    }

    fun pause() {
        isPaused = true
        androidTts?.stop()
        _state.value = TtsState.Paused
    }

    fun resume() {
        if (_state.value is TtsState.Paused) {
            isPaused = false
        }
    }

    fun stop() {
        isPaused = true
        androidTts?.stop()
        currentChunkIndex = 0
        currentWordIndex = 0
        _state.value = TtsState.Idle
    }

    fun shutdown() {
        androidTts?.shutdown()
        androidTts = null
    }
}
