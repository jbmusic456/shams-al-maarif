package com.shamsalmaarif.reader.ui.importer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shamsalmaarif.reader.data.database.entities.ReadEntity
import com.shamsalmaarif.reader.data.repository.ReadsRepository
import com.shamsalmaarif.reader.parser.EpubParser
import com.shamsalmaarif.reader.parser.PdfParser
import com.shamsalmaarif.reader.parser.UrlParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val readId: String, val text: String) : ImportState()
    data class Error(val message: String) : ImportState()
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: ReadsRepository,
    private val pdfParser: PdfParser,
    private val epubParser: EpubParser,
    private val urlParser: UrlParser
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    fun importFromUri(uri: Uri) {
        _state.value = ImportState.Loading
        viewModelScope.launch {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val fileName = getFileName(uri)
                val ext = fileName.substringAfterLast('.', "").lowercase()

                val file = copyToCache(uri, fileName)
                val text: String
                val title: String
                val fileType: String

                when {
                    ext == "pdf" || mimeType.contains("pdf") -> {
                        text = pdfParser.extractText(context, file)
                        title = fileName.removeSuffix(".pdf")
                        fileType = "pdf"
                    }
                    ext == "epub" || mimeType.contains("epub") -> {
                        text = epubParser.extractText(file)
                        title = fileName.removeSuffix(".epub")
                        fileType = "epub"
                    }
                    ext == "txt" || mimeType.startsWith("text/plain") -> {
                        text = file.readText()
                        title = fileName.removeSuffix(".txt")
                        fileType = "txt"
                    }
                    else -> {
                        text = file.readText()
                        title = fileName
                        fileType = ext
                    }
                }

                val readId = UUID.randomUUID().toString()
                val entity = ReadEntity(
                    readId = readId,
                    title = title,
                    author = null,
                    description = null,
                    url = null,
                    source = "local",
                    originalFileType = fileType,
                    contentType = "text",
                    charCount = text.length,
                    wordCount = text.split("\\s+".toRegex()).size,
                    fromUserImport = true
                )
                repo.insertRead(entity)
                _state.value = ImportState.Success(readId, text)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun importFromUrl(url: String) {
        _state.value = ImportState.Loading
        viewModelScope.launch {
            try {
                val article = urlParser.fetchArticle(url)
                val readId = UUID.randomUUID().toString()
                val entity = ReadEntity(
                    readId = readId,
                    title = article.title,
                    author = article.author,
                    description = article.description,
                    url = url,
                    source = "web",
                    originalFileType = "url",
                    contentType = "article",
                    charCount = article.text.length,
                    wordCount = article.text.split("\\s+".toRegex()).size,
                    articleImageUrl = article.imageUrl,
                    fromUserImport = false
                )
                repo.insertRead(entity)
                _state.value = ImportState.Success(readId, article.text)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Failed to fetch URL")
            }
        }
    }

    fun importFromText(text: String, title: String) {
        _state.value = ImportState.Loading
        viewModelScope.launch {
            try {
                val readId = UUID.randomUUID().toString()
                val entity = ReadEntity(
                    readId = readId,
                    title = title.ifBlank { "Pasted Text" },
                    author = null,
                    description = null,
                    url = null,
                    source = "paste",
                    originalFileType = "txt",
                    contentType = "text",
                    charCount = text.length,
                    wordCount = text.split("\\s+".toRegex()).size,
                    fromUserImport = true
                )
                repo.insertRead(entity)
                _state.value = ImportState.Success(readId, text)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Failed to save text")
            }
        }
    }

    fun resetState() { _state.value = ImportState.Idle }

    private fun getFileName(uri: Uri): String {
        var name = "imported_file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) {
                name = cursor.getString(idx) ?: name
            }
        }
        return name
    }

    private suspend fun copyToCache(uri: Uri, fileName: String): File {
        val dest = File(context.cacheDir, "import_${UUID.randomUUID()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        return dest
    }
}
