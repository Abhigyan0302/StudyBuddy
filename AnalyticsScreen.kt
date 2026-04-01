package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.QuizResult
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    chapterName: String,
    results: List<QuizResult>,
    onBack: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val avgPct = if (results.isEmpty()) 0f
    else results.map { it.score.toFloat() / it.total }.average().toFloat() * 100

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050510),
                        Color(0xFF0A0520),
                        Color(0xFF050510)
                    )
                )
            )
    ) {
        // Ambient glows
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-40).dp)
                .blur(110.dp)
                .background(Color(0x1ABB86FC), RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(90.dp)
                .background(Color(0x1203DAC6), RoundedCornerShape(50))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                tint = Color(0xAAFFFFFF)
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                "Performance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                chapterName,
                                fontSize = 11.sp,
                                color = Color(0xFFBB86FC)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0x800D0D1A)
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Average score card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x1AFFFFFF))
                            .border(
                                0.5.dp,
                                Brush.linearGradient(
                                    listOf(Color(0x44BB86FC), Color(0x11FFFFFF))
                                ),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Average Score",
                                fontSize = 12.sp,
                                color = Color(0x88FFFFFF),
                                letterSpacing = 1.sp
                            )
                            Text(
                                "${avgPct.toInt()}%",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    avgPct >= 80 -> Color(0xFF03DAC6)
                                    avgPct >= 60 -> Color(0xFFBB86FC)
                                    else         -> Color(0xFFCF6679)
                                }
                            )
                            Text(
                                "over ${results.size} quiz attempt${if (results.size != 1) "s" else ""}",
                                fontSize = 12.sp,
                                color = Color(0x66FFFFFF)
                            )
                            Spacer(Modifier.height(4.dp))

                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x22FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(avgPct / 100f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.linearGradient(
                                                when {
                                                    avgPct >= 80 -> listOf(
                                                        Color(0xFF03DAC6), Color(0xFF018786)
                                                    )
                                                    avgPct >= 60 -> listOf(
                                                        Color(0xFFBB86FC), Color(0xFF7C4DFF)
                                                    )
                                                    else -> listOf(
                                                        Color(0xFFCF6679), Color(0xFFB00020)
                                                    )
                                                }
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                // Empty state
                if (results.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🧠", fontSize = 40.sp)
                                Text(
                                    "No quiz attempts yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xAAFFFFFF)
                                )
                                Text(
                                    "Take a quiz to see your performance here.",
                                    fontSize = 13.sp,
                                    color = Color(0x55FFFFFF)
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "ATTEMPT HISTORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0x66FFFFFF),
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }

                    items(results) { result ->
                        val pct = (result.score.toFloat() / result.total * 100).toInt()
                        val scoreColor = when {
                            pct >= 80 -> Color(0xFF03DAC6)
                            pct >= 60 -> Color(0xFFBB86FC)
                            else      -> Color(0xFFCF6679)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(
                                    0.5.dp,
                                    Color(0x22FFFFFF),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        "${result.score} / ${result.total} correct",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        fmt.format(Date(result.timestamp)),
                                        fontSize = 11.sp,
                                        color = Color(0x66FFFFFF)
                                    )
                                }

                                // Score badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(scoreColor.copy(alpha = 0.15f))
                                        .border(
                                            0.5.dp,
                                            scoreColor.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "$pct%",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColor
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
