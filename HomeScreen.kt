package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Chapter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    chapters: List<Chapter>,
    onChapterClick: (Long) -> Unit,
    onCreateChapter: (String, String) -> Unit,
    onDeleteChapter: (Chapter) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

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
        // Ambient glow blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .blur(120.dp)
                .background(Color(0x22BB86FC), RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(100.dp)
                .background(Color(0x1503DAC6), RoundedCornerShape(50))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Study Buddy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                            Text(
                                "AI-Powered Learning",
                                fontSize = 11.sp,
                                color = Color(0xFFBB86FC)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("New Chapter", fontWeight = FontWeight.SemiBold) },
                    containerColor = Color(0xFFBB86FC),
                    contentColor = Color(0xFF0D0D1A)
                )
            }
        ) { padding ->
            if (chapters.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(
                                    0.5.dp,
                                    Color(0x33FFFFFF),
                                    RoundedCornerShape(28.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.MenuBook, null,
                                modifier = Modifier.size(44.dp),
                                tint = Color(0x66FFFFFF)
                            )
                        }
                        Text(
                            "No chapters yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xAAFFFFFF)
                        )
                        Text(
                            "Tap + to create your first chapter",
                            fontSize = 13.sp,
                            color = Color(0x66FFFFFF)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chapters, key = { it.id }) { chapter ->
                        GlassChapterCard(
                            chapter = chapter,
                            onClick = { onChapterClick(chapter.id) },
                            onDelete = { onDeleteChapter(chapter) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showDialog) {
        GlassCreateChapterDialog(
            onConfirm = { name, subject ->
                onCreateChapter(name, subject)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassChapterCard(chapter: Chapter, onClick: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1AFFFFFF))
            .border(
                0.5.dp,
                Brush.linearGradient(
                    listOf(Color(0x44FFFFFF), Color(0x11FFFFFF))
                ),
                RoundedCornerShape(20.dp)
            )
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x22BB86FC))
                        .border(0.5.dp, Color(0x44BB86FC), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AutoStories, null,
                        tint = Color(0xFFBB86FC),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        chapter.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (chapter.subject.isNotBlank()) {
                        Text(
                            chapter.subject,
                            fontSize = 12.sp,
                            color = Color(0xFFBB86FC)
                        )
                    }
                    Text(
                        fmt.format(Date(chapter.updatedAt)),
                        fontSize = 11.sp,
                        color = Color(0x66FFFFFF)
                    )
                }
                IconButton(onClick = { showConfirm = true }) {
                    Icon(
                        Icons.Default.DeleteOutline, "Delete",
                        tint = Color(0x66FFFFFF)
                    )
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = Color(0xFF1A1A2E),
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete Chapter?", color = Color.White) },
            text = {
                Text(
                    "All messages in \"${chapter.name}\" will be permanently deleted.",
                    color = Color(0xAAFFFFFF)
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("Delete", color = Color(0xFFCF6679))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = Color(0xFFBB86FC))
                }
            }
        )
    }
}

@Composable
fun GlassCreateChapterDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name    by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D0D1A),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                "New Chapter",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Chapter Name *",
                    placeholder = "e.g., Chapter 3: Cell Biology"
                )
                GlassTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = "Subject (optional)",
                    placeholder = "e.g., Biology"
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (name.isNotBlank())
                            Brush.linearGradient(listOf(Color(0xFFBB86FC), Color(0xFF7C4DFF)))
                        else
                            Brush.linearGradient(listOf(Color(0x44FFFFFF), Color(0x44FFFFFF)))
                    )
            ) {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) onConfirm(name.trim(), subject.trim())
                    }
                ) {
                    Text(
                        "Create",
                        color = if (name.isNotBlank()) Color.White else Color(0x44FFFFFF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0x88FFFFFF))
            }
        }
    )
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, color = Color(0x88FFFFFF))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1AFFFFFF))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(placeholder, color = Color(0x44FFFFFF), fontSize = 14.sp)
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = Color(0xFFBB86FC)
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
