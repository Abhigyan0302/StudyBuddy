package com.example.myapplication.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.json.JSONObject

data class Flashcard(val front: String, val back: String)

@Composable
fun InteractiveFlashcards(jsonContent: String) {
    var cards      by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var idx        by remember { mutableStateOf(0) }
    var isFlipped  by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf(false) }

    LaunchedEffect(jsonContent) {
        runCatching {
            val arr = JSONObject(jsonContent).getJSONArray("flashcards")
            cards = (0 until arr.length()).map { i ->
                val c = arr.getJSONObject(i)
                Flashcard(c.getString("front"), c.getString("back"))
            }
        }.onFailure { parseError = true }
    }

    if (parseError) {

        return
    }
    if (cards.isEmpty()) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "${idx + 1} / ${cards.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { (idx + 1f) / cards.size },
                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Text(
            "Tap card to reveal answer",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        FlipCard(
            front = cards[idx].front,
            back = cards[idx].back,
            isFlipped = isFlipped,
            onFlip = { isFlipped = !isFlipped }
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (idx > 0) { idx--; isFlipped = false } },
                enabled = idx > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
            }

            TextButton(onClick = {
                val newIdx = (cards.indices - idx).random()
                idx = newIdx
                isFlipped = false
            }) {
                Text("Shuffle")
            }

            IconButton(
                onClick = { if (idx < cards.size - 1) { idx++; isFlipped = false } },
                enabled = idx < cards.size - 1
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
            }
        }
    }
}

@Composable
fun FlipCard(front: String, back: String, isFlipped: Boolean, onFlip: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "flip"
    )
    val showFront = rotation <= 90f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (showFront) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showFront) {
                Text(
                    front,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    back,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                )
            }
        }
    }
}
