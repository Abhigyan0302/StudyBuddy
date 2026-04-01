package com.example.myapplication

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.data.StudyDatabase
import com.example.myapplication.data.StudyRepository
import com.example.myapplication.ui.AnalyticsScreen
import com.example.myapplication.ui.ChapterScreen
import com.example.myapplication.ui.ChapterViewModel
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.HomeViewModel
import com.example.myapplication.ui.theme.StudyBuddyTheme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        val repository = StudyRepository(StudyDatabase.getInstance(this))

        setContent {
            StudyBuddyTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {

                    // ── Home ──────────────────────────────────────────────────
                    composable("home") {
                        val vm: HomeViewModel = viewModel(
                            factory = HomeViewModel.Factory(repository)
                        )
                        val chapters by vm.chapters.collectAsState()

                        HomeScreen(
                            chapters        = chapters,
                            onChapterClick  = { id -> navController.navigate("chapter/$id") },
                            onCreateChapter = { name, subject ->
                                vm.createChapter(name, subject) { id ->
                                    navController.navigate("chapter/$id")
                                }
                            },
                            onDeleteChapter = { chapter -> vm.deleteChapter(chapter) }
                        )
                    }

                    // ── Chapter chat ──────────────────────────────────────────
                    composable(
                        route = "chapter/{chapterId}",
                        arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
                    ) { backStack ->
                        val id = backStack.arguments!!.getLong("chapterId")
                        val vm: ChapterViewModel = viewModel(
                            factory = ChapterViewModel.Factory(id, repository)
                        )

                        ChapterScreen(
                            viewModel     = vm,
                            tts           = tts,
                            navController = navController,
                            onBack        = { navController.popBackStack() }
                        )
                    }

                    // ── Analytics ─────────────────────────────────────────────
                    composable(
                        route = "analytics/{chapterId}",
                        arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
                    ) { backStack ->
                        val id = backStack.arguments!!.getLong("chapterId")
                        val vm: ChapterViewModel = viewModel(
                            factory = ChapterViewModel.Factory(id, repository)
                        )
                        val uiState  by vm.uiState.collectAsState()
                        val results  by vm.quizResults.collectAsState()

                        AnalyticsScreen(
                            chapterName = uiState.chapter?.name ?: "Chapter",
                            results     = results,
                            onBack      = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
