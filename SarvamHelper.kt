package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SarvamHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val apiKey  = "API_KEY"
    private val baseUrl = "https://api.sarvam.ai/v1/chat/completions"
    private val model   = "sarvam-m"

    private val maxNotesChars        = 20000
    private val maxNotesCharsForChat =  6000

    private fun truncateNotes(notes: String, limit: Int = maxNotesChars): String {
        if (notes.length <= limit) return notes
        return notes.take(limit) +
                "\n\n[Content truncated — split into smaller chapters for best results.]"
    }

    private fun stripThinkTags(text: String): String {
        return text
            .replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun stripMarkdownFences(text: String): String {
        return text
            .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("```\\s*"), "")
            .trim()
    }

    private fun sanitizeHistory(history: List<Pair<String, String>>): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        for (msg in history) {
            if (result.isEmpty()) {
                if (msg.first == "user") result.add(msg)
            } else {
                if (msg.first != result.last().first) result.add(msg)
            }
        }
        return result
    }

    private fun truncateHistory(history: List<Pair<String, String>>): List<Pair<String, String>> {
        return history
            .takeLast(6)
            .filter { (_, content) ->
                !content.trimStart().startsWith("{") &&
                        !content.trimStart().startsWith("[")
            }
    }

    private suspend fun call(
        systemPrompt: String,
        history: List<Pair<String, String>>,
        temperature: Double = 0.3
    ): String = withContext(Dispatchers.IO) {
        try {
            val sanitized = sanitizeHistory(history)

            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
            sanitized.forEach { (role, content) ->
                messages.put(JSONObject().put("role", role).put("content", content))
            }

            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("max_tokens", 1500)
                .put("temperature", temperature)
                .toString()

            val request = Request.Builder()
                .url(baseUrl)
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "Error ${response.code}: ${response.body?.string()}"
            }

            val raw = JSONObject(response.body?.string() ?: return@withContext "Empty response")
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")

            stripThinkTags(raw)

        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun summarizeNotes(notes: String) = call(
        systemPrompt = "You are a study assistant. Create clear, well-structured summaries " +
                "using bullet points and markdown. Do not include any thinking or reasoning " +
                "in your response — only the final summary.",
        history = listOf(
            "user" to "Summarize these notes concisely with a brief intro and bullet points:\n\n${truncateNotes(notes)}"
        )
    )

    suspend fun generateQuiz(notes: String) = call(
        systemPrompt = """You are a quiz generator. Return ONLY valid JSON — no markdown fences, no thinking tags, no extra text whatsoever.
Schema: {"questions":[{"type":"mcq","question":"...","options":["A","B","C","D"],"answer":"A"},{"type":"truefalse","question":"...","answer":"true"},{"type":"shortanswer","question":"...","answer":"..."}]}
Generate exactly 6 questions: 3 MCQ, 2 True/False, 1 Short Answer.""",
        history = listOf("user" to "Generate a quiz from:\n\n${truncateNotes(notes)}"),
        temperature = 0.1
    ).let { stripMarkdownFences(it) }

    suspend fun generateFlashcards(notes: String) = call(
        systemPrompt = """You are a flashcard generator. Return ONLY valid JSON — no markdown fences.
Schema: {"flashcards":[{"front":"term or question","back":"definition or answer"}]}
Generate exactly 8 flashcards.""",
        history = listOf("user" to "Create flashcards from:\n\n${truncateNotes(notes)}"),
        temperature = 0.1
    ).let { stripMarkdownFences(it) }

    suspend fun chatWithNotes(
        notes: String,
        userMessage: String,
        history: List<Pair<String, String>>
    ) = call(
        systemPrompt = "You are a helpful study assistant. Answer questions based on the " +
                "provided notes concisely and clearly. If the answer isn't in the notes, " +
                "say so. Do not output thinking tags. Make sure the answers are as concise as possible.\n\n" +
                "STUDY NOTES:\n${truncateNotes(notes, maxNotesCharsForChat)}",
        history = truncateHistory(history) + ("user" to userMessage)
    )
}
