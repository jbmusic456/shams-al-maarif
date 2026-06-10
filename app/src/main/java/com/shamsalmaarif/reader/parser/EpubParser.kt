package com.shamsalmaarif.reader.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject

class EpubParser @Inject constructor() {

    suspend fun extractText(file: File): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        try {
            val zip = ZipFile(file)
            val entries = zip.entries().toList()
                .filter { it.name.endsWith(".html") || it.name.endsWith(".xhtml") || it.name.endsWith(".htm") }
                .sortedBy { it.name }

            for (entry in entries) {
                val content = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                val doc = Jsoup.parse(content)
                val text = doc.body()?.text() ?: ""
                if (text.isNotBlank()) {
                    sb.append(text)
                    sb.append("\n\n")
                }
            }
            zip.close()
        } catch (e: Exception) {
            sb.append("[EPUB parsing error: ${e.message}]")
        }
        sb.toString().trim()
    }

    suspend fun extractChapters(file: File): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<Pair<String, String>>()
        try {
            val zip = ZipFile(file)
            val entries = zip.entries().toList()
                .filter { it.name.endsWith(".html") || it.name.endsWith(".xhtml") || it.name.endsWith(".htm") }
                .sortedBy { it.name }

            for (entry in entries) {
                val content = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                val doc = Jsoup.parse(content)
                val title = doc.title().ifBlank { entry.name }
                val text = doc.body()?.text() ?: ""
                if (text.isNotBlank()) chapters.add(Pair(title, text))
            }
            zip.close()
        } catch (e: Exception) {
            // ignore
        }
        chapters
    }
}
