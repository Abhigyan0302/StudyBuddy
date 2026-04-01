package com.example.myapplication.ui

import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.myapplication.data.Message
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(
    viewModel: ChapterViewModel,
    tts: TextToSpeech,
    navController: NavController,
    onBack: () -> Unit
) {
    val messages  by viewModel.messages.collectAsState()
    val uiState   by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()
    val context   = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val chapterId = uiState.chapter?.id ?: 0L

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val name = context.contentResolver.query(
                uri, null, null, null, null
            )?.use { cur ->
                val col = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cur.moveToFirst()
                cur.getString(col)
            } ?: "file"
            viewModel.uploadFile(context, uri, name)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty())
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050510), Color(0xFF0A0520), Color(0xFF050510))
                )
            )
    ) {
        // Ambient glows
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-40).dp)
                .blur(110.dp)
                .background(Color(0x1ABB86FC), RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 60.dp)
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
                                "Back",
                                tint = Color(0xAAFFFFFF)
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                uiState.chapter?.name ?: "Chapter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            uiState.chapter?.subject?.takeIf { it.isNotBlank() }?.let {
                                Text(it, fontSize = 11.sp, color = Color(0xFFBB86FC))
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { filePicker.launch("*/*") }) {
                            Icon(
                                Icons.Outlined.AttachFile,
                                "Upload",
                                tint = Color(0xAAFFFFFF)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0x800D0D1A)
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(Color(0xCC050510))
                        .navigationBarsPadding()
                ) {
                    // Action chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassChip("Summarize")  { viewModel.summarize() }
                        GlassChip("Quiz")       { viewModel.generateQuiz() }
                        GlassChip("Flashcards") { viewModel.generateFlashcards() }
                        GlassChip("Stats")      { navController.navigate("analytics/$chapterId") }
                        GlassChip("Upload")     { filePicker.launch("*/*") }
                    }

                    // Input row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        ) {
                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        "Ask about your notes...",
                                        color = Color(0x55FFFFFF),
                                        fontSize = 14.sp
                                    )
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
                                maxLines = 4
                            )
                        }

                        // Send button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !uiState.isLoading)
                                        Brush.linearGradient(
                                            listOf(Color(0xFFBB86FC), Color(0xFF7C4DFF))
                                        )
                                    else
                                        Brush.linearGradient(
                                            listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))
                                        )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank() && !uiState.isLoading) {
                                        viewModel.sendMessage(inputText.trim())
                                        inputText = ""
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(Color(0x1AFFFFFF))
                                        .border(
                                            0.5.dp,
                                            Color(0x33FFFFFF),
                                            RoundedCornerShape(26.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📚", fontSize = 36.sp)
                                }
                                Text(
                                    "Upload your notes to begin",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xAAFFFFFF)
                                )
                                Text(
                                    "Supports PDF, PPTX, and images",
                                    fontSize = 12.sp,
                                    color = Color(0x55FFFFFF)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFBB86FC), Color(0xFF7C4DFF))
                                            )
                                        )
                                ) {
                                    TextButton(onClick = { filePicker.launch("*/*") }) {
                                        Icon(
                                            Icons.Outlined.Upload, null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Upload Notes",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    GlassMessageBubble(msg, tts, viewModel)
                }

                if (uiState.isLoading) {
                    item { GlassLoadingBubble(uiState.loadingMessage) }
                }
            }
        }
    }
}

@Composable
fun GlassChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1AFFFFFF))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(label, color = Color(0xCCFFFFFF), fontSize = 13.sp)
        }
    }
}

@Composable
fun GlassMessageBubble(message: Message, tts: TextToSpeech, viewModel: ChapterViewModel) {
    val isUser  = message.role == "user"
    val context = LocalContext.current

    when (message.type) {
        "QUIZ" -> InteractiveQuiz(
            jsonContent    = message.content,
            onQuizComplete = { score, total -> viewModel.saveQuizResult(score, total) }
        )
        "FLASHCARD" -> InteractiveFlashcards(message.content)
        "FILE_UPLOAD" -> {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22BB86FC))
                        .border(0.5.dp, Color(0x44BB86FC), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AttachFile, null,
                            tint = Color(0xFFBB86FC),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(message.content, color = Color(0xCCFFFFFF), fontSize = 13.sp)
                    }
                }
            }
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                if (!isUser) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFBB86FC), Color(0xFF7C4DFF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "AI",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Column(modifier = Modifier.widthIn(max = 280.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart    = 18.dp,
                                    topEnd      = 18.dp,
                                    bottomStart = if (isUser) 18.dp else 4.dp,
                                    bottomEnd   = if (isUser) 4.dp  else 18.dp
                                )
                            )
                            .background(
                                if (isUser)
                                    Brush.linearGradient(
                                        listOf(Color(0xFFBB86FC), Color(0xFF7C4DFF))
                                    )
                                else
                                    Brush.linearGradient(
                                        listOf(Color(0x1AFFFFFF), Color(0x0DFFFFFF))
                                    )
                            )
                            .border(
                                0.5.dp,
                                if (isUser) Color(0x44FFFFFF) else Color(0x22FFFFFF),
                                RoundedCornerShape(
                                    topStart    = 18.dp,
                                    topEnd      = 18.dp,
                                    bottomStart = if (isUser) 18.dp else 4.dp,
                                    bottomEnd   = if (isUser) 4.dp  else 18.dp
                                )
                            )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            if (isUser) {
                                Text(
                                    text       = message.content,
                                    color      = Color.White,
                                    fontSize   = 14.sp,
                                    lineHeight = 20.sp
                                )
                            } else {
                                // Markdown rendering for AI responses
                                AndroidView(
                                    factory = { ctx ->
                                        TextView(ctx).apply {
                                            setTextColor(
                                                android.graphics.Color.parseColor("#DDFFFFFF")
                                            )
                                            textSize = 14f
                                            setLineSpacing(4f, 1f)
                                        }
                                    },
                                    update = { tv ->
                                        val markwon = Markwon.create(context)
                                        markwon.setMarkdown(tv, message.content)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (!isUser) {
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x11FFFFFF))
                                ) {
                                    TextButton(
                                        onClick = {
                                            tts.speak(
                                                message.content,
                                                TextToSpeech.QUEUE_FLUSH, null, null
                                            )
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp, vertical = 2.dp
                                        )
                                    ) {
                                        Icon(
                                            Icons.Outlined.VolumeUp, null,
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0x88FFFFFF)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Read aloud",
                                            fontSize = 11.sp,
                                            color = Color(0x88FFFFFF)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassLoadingBubble(msg: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFBB86FC), Color(0xFF7C4DFF)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "AI",
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1AFFFFFF))
                .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color       = Color(0xFFBB86FC)
                )
                Text(msg, fontSize = 13.sp, color = Color(0xAAFFFFFF))
            }
        }
    }
}
