package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

data class QuizQuestion(
    val type: String,
    val question: String,
    val options: List<String> = emptyList(),
    val answer: String
)

@Composable
fun InteractiveQuiz(
    jsonContent: String,
    onQuizComplete: (score: Int, total: Int) -> Unit = { _, _ -> }
) {
    var questions  by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentIdx by remember { mutableStateOf(0) }
    var score      by remember { mutableStateOf(0) }
    var finished   by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf(false) }

    LaunchedEffect(jsonContent) {
        runCatching {
            val arr = JSONObject(jsonContent).getJSONArray("questions")
            questions = (0 until arr.length()).map { i ->
                val q = arr.getJSONObject(i)
                QuizQuestion(
                    type     = q.getString("type"),
                    question = q.getString("question"),
                    options  = if (q.has("options")) {
                        val o = q.getJSONArray("options")
                        (0 until o.length()).map { o.getString(it) }
                    } else emptyList(),
                    answer   = q.getString("answer")
                )
            }
        }.onFailure { parseError = true }
    }

    if (parseError) {

        return
    }
    if (questions.isEmpty()) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        return
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (finished) {
                QuizResult(score, questions.size) {
                    currentIdx = 0; score = 0; finished = false
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${currentIdx + 1}/${questions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { (currentIdx + 1f) / questions.size },
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                QuestionView(questions[currentIdx]) { correct ->
                    if (correct) score++
                    if (currentIdx < questions.size - 1) {
                        currentIdx++
                    } else {
                        finished = true
                        onQuizComplete(score, questions.size)
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionView(q: QuizQuestion, onAnswered: (Boolean) -> Unit) {
    var selected   by remember(q) { mutableStateOf<String?>(null) }
    var shortInput by remember(q) { mutableStateOf("") }
    var revealed   by remember(q) { mutableStateOf(false) }

    Text(q.question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

    when (q.type) {
        "mcq" -> q.options.forEach { opt ->
            val isSelected = selected == opt
            val isCorrect  = opt == q.answer
            val bg = when {
                !revealed                -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                isCorrect                -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                isSelected && !isCorrect -> Color(0xFFC62828).copy(alpha = 0.2f)
                else                     -> Color.Transparent
            }
            OutlinedCard(
                onClick = {
                    if (!revealed) {
                        selected = opt
                        revealed = true
                        onAnswered(opt == q.answer)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                ),
                colors = CardDefaults.outlinedCardColors(containerColor = bg)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(opt, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (revealed && isCorrect)
                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                    if (revealed && isSelected && !isCorrect)
                        Icon(Icons.Default.Close, null, tint = Color(0xFFF44336))
                }
            }
        }

        "truefalse" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("true", "false").forEach { opt ->
                val isSelected = selected == opt
                val isCorrect  = opt == q.answer.lowercase()
                val bg = when {
                    !revealed  -> if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                    isCorrect  -> Color(0xFF2E7D32).copy(alpha = 0.3f)
                    isSelected -> Color(0xFFC62828).copy(alpha = 0.3f)
                    else       -> MaterialTheme.colorScheme.surface
                }
                Button(
                    onClick = {
                        if (!revealed) {
                            selected = opt
                            revealed = true
                            onAnswered(opt == q.answer.lowercase())
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bg,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(opt.replaceFirstChar { it.uppercase() })
                }
            }
        }

        "shortanswer" -> {
            OutlinedTextField(
                shortInput, { if (!revealed) shortInput = it },
                label = { Text("Your answer") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = revealed
            )
            if (!revealed) {
                Button({ revealed = true; onAnswered(true) }, Modifier.fillMaxWidth()) {
                    Text("Check Answer")
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1B5E20).copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Sample Answer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(q.answer)
                    }
                }
                Button({ onAnswered(true) }, Modifier.fillMaxWidth()) { Text("Next") }
            }
        }
    }
}

@Composable
fun QuizResult(score: Int, total: Int, onRetry: () -> Unit) {
    val pct = (score.toFloat() / total * 100).toInt()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            when { pct >= 80 -> "🎉"; pct >= 60 -> "👍"; else -> "📚" },
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            "$score / $total",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            when { pct >= 80 -> "Excellent!"; pct >= 60 -> "Good job!"; else -> "Keep studying!" },
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedButton(onClick = onRetry) { Text("Retry Quiz") }
    }
}
