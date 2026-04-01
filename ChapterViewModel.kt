package com.example.myapplication.ui

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.*
import com.example.myapplication.OcrHelper
import com.example.myapplication.PdfHelper
import com.example.myapplication.PptHelper
import com.example.myapplication.SarvamHelper
import com.example.myapplication.data.Chapter
import com.example.myapplication.data.Message
import com.example.myapplication.data.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

data class ChapterUiState(
    val chapter: Chapter? = null,
    val isLoading: Boolean = false,
    val loadingMessage: String = "Thinking..."
)

class ChapterViewModel(
    private val chapterId: Long,
    private val repository: StudyRepository,
    private val sarvam: SarvamHelper = SarvamHelper(),
    private val ocrHelper: OcrHelper = OcrHelper()
) : ViewModel() {

    val messages: StateFlow<List<Message>> = repository.getMessages(chapterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _ui = MutableStateFlow(ChapterUiState())
    val uiState: StateFlow<ChapterUiState> = _ui.asStateFlow()

    val quizResults = repository.getQuizResultsForChapter(chapterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { loadChapter() }

    private fun loadChapter() = viewModelScope.launch {
        _ui.update { it.copy(chapter = repository.getChapter(chapterId)) }
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    fun sendMessage(text: String) = viewModelScope.launch {
        val chapter = _ui.value.chapter ?: return@launch
        repository.addUserMessage(chapterId, text)
        _ui.update { it.copy(isLoading = true, loadingMessage = "Thinking...") }

        // Only include plain TEXT messages in chat history
        // QUIZ, FLASHCARD, SUMMARY, FILE_UPLOAD type messages confuse the model
        val history = messages.value
            .filter { it.type == "TEXT" }
            .filter { it.content.isNotBlank() }
            .dropLast(1)
            .takeLast(10)
            .map { it.role to it.content }

        val reply = sarvam.chatWithNotes(chapter.notesContext, text, history)

        repository.addAssistantMessage(chapterId, reply, "TEXT")
        _ui.update { it.copy(isLoading = false) }
    }

    // ── AI Actions ────────────────────────────────────────────────────────────

    fun summarize() = aiAction("Summarize my notes", "Summarizing...", "SUMMARY") { notes ->
        sarvam.summarizeNotes(notes)
    }

    fun generateQuiz() = aiAction("Generate a quiz", "Generating quiz...", "QUIZ") { notes ->
        sarvam.generateQuiz(notes)
    }

    fun generateFlashcards() = aiAction("Generate flashcards", "Creating flashcards...", "FLASHCARD") { notes ->
        sarvam.generateFlashcards(notes)
    }

    private fun aiAction(
        userMsg: String,
        loadMsg: String,
        type: String,
        block: suspend (String) -> String
    ) = viewModelScope.launch {
        val chapter = _ui.value.chapter ?: return@launch
        if (chapter.notesContext.isBlank()) {
            repository.addAssistantMessage(
                chapterId,
                "Please upload some study material first! Tap the attach icon to add files.",
                "TEXT"
            )
            return@launch
        }
        repository.addUserMessage(chapterId, userMsg, type)  // ← type instead of default TEXT
        _ui.update { it.copy(isLoading = true, loadingMessage = loadMsg) }
        val result = block(chapter.notesContext)
        repository.addAssistantMessage(chapterId, result, type)
        _ui.update { it.copy(isLoading = false) }
    }

    // ── Quiz Results ──────────────────────────────────────────────────────────

    fun saveQuizResult(score: Int, total: Int) = viewModelScope.launch {
        repository.saveQuizResult(chapterId, score, total)
    }

    // ── File Upload ───────────────────────────────────────────────────────────

    fun uploadFile(context: Context, uri: Uri, fileName: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, loadingMessage = "Reading $fileName...") }
        repository.addUserMessage(chapterId, "Uploaded: $fileName", "FILE_UPLOAD")

        val extension = fileName.substringAfterLast('.', "").lowercase()
        val extracted = when (extension) {
            "pdf" -> PdfHelper(context).extractFromPdf(uri, ocrHelper) { progress ->
                _ui.update { it.copy(loadingMessage = progress) }
            }
            "pptx", "ppt" -> PptHelper().extractFromPptx(context, uri)
            else -> {
                suspendCancellableCoroutine { continuation ->
                    ocrHelper.extractTextFromUri(
                        context   = context,
                        uri       = uri,
                        onSuccess = { text ->
                            if (continuation.isActive) continuation.resume(text) {}
                        },
                        onError   = { error ->
                            if (continuation.isActive)
                                continuation.resume("Error reading image: ${error.message}") {}
                        }
                    )
                }
            }
        }

        repository.appendNotesContext(chapterId, "=== $fileName ===\n$extracted")
        loadChapter()
        repository.addAssistantMessage(
            chapterId,
            "**$fileName** added to your notes! You can now Summarize, Generate a Quiz, " +
                    "create Flashcards, or just chat.",
            "TEXT"
        )
        _ui.update { it.copy(isLoading = false) }
    }

    fun readAloud(text: String, tts: TextToSpeech) =
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)

    class Factory(private val chapterId: Long, private val repository: StudyRepository) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ChapterViewModel(chapterId, repository) as T
    }
}
