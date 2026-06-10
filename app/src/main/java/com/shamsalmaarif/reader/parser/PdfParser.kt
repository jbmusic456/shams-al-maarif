package com.shamsalmaarif.reader.parser

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class PdfParser @Inject constructor() {

    suspend fun extractText(context: Context, file: File): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                page.close()
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            sb.append("[PDF parsing error: ${e.message}]")
        }
        // Android PdfRenderer doesn't expose text — use iText or MuPDF for real text extraction.
        // For now, return placeholder until a proper PDF library is integrated.
        sb.append("PDF text extraction requires additional library. File: ${file.name}")
        sb.toString()
    }
}
