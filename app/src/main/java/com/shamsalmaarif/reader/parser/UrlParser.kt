package com.shamsalmaarif.reader.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject

class UrlParser @Inject constructor(private val httpClient: OkHttpClient) {

    data class ArticleResult(
        val title: String,
        val author: String?,
        val description: String?,
        val imageUrl: String?,
        val text: String
    )

    suspend fun fetchArticle(url: String): ArticleResult = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            .build()
        val response = httpClient.newCall(request).execute()
        val html = response.body?.string() ?: ""
        val doc = Jsoup.parse(html, url)

        val title = doc.select("meta[property=og:title]").attr("content")
            .ifBlank { doc.title() }

        val author = doc.select("meta[name=author]").attr("content").ifBlank { null }

        val description = doc.select("meta[property=og:description], meta[name=description]")
            .firstOrNull()?.attr("content")?.ifBlank { null }

        val imageUrl = doc.select("meta[property=og:image]").attr("content").ifBlank { null }

        // Remove nav/footer/ads — keep article body
        doc.select("nav, footer, header, aside, script, style, [class*=ad], [id*=ad]").remove()
        val text = doc.select("article, main, [role=main], .content, .post-content, .entry-content")
            .firstOrNull()?.text()
            ?: doc.body()?.text()
            ?: ""

        ArticleResult(title, author, description, imageUrl, text.trim())
    }
}
