package com.shamsalmaarif.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shamsalmaarif.reader.ui.importer.ImportScreen
import com.shamsalmaarif.reader.ui.library.LibraryScreen
import com.shamsalmaarif.reader.ui.reader.ReaderScreen
import com.shamsalmaarif.reader.ui.theme.ShamsTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShamsTheme {
                ShamsNavGraph(intent)
            }
        }
    }
}

@Composable
private fun ShamsNavGraph(launchIntent: Intent?) {
    val navController = rememberNavController()
    var showImport by remember { mutableStateOf(false) }
    var readerText by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    NavHost(navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                onReadClick = { readId ->
                    val text = readerText[readId] ?: ""
                    val encoded = URLEncoder.encode(text, "UTF-8")
                    navController.navigate("reader/$readId/$encoded")
                },
                onAddClick = { showImport = true }
            )
        }
        composable(
            "reader/{readId}/{text}",
            arguments = listOf(
                navArgument("readId") { type = NavType.StringType },
                navArgument("text") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val readId = backStackEntry.arguments?.getString("readId") ?: return@composable
            val encoded = backStackEntry.arguments?.getString("text") ?: ""
            val text = try { URLDecoder.decode(encoded, "UTF-8") } catch (e: Exception) { encoded }
            ReaderScreen(
                readId = readId,
                fullText = text,
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showImport) {
        ImportScreen(
            onImported = { readId, text ->
                readerText = readerText + (readId to text)
                showImport = false
                val encoded = URLEncoder.encode(text, "UTF-8")
                navController.navigate("reader/$readId/$encoded")
            },
            onDismiss = { showImport = false }
        )
    }
}
