package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffectManager
import com.example.data.CanvasNodeEntity
import com.example.ui.localization.Strings
import com.example.ui.viewmodel.StoryViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullDocumentEditorScreen(
    viewModel: StoryViewModel,
    node: CanvasNodeEntity,
    onBackToCanvas: () -> Unit
) {
    val lang = viewModel.language
    val clipboardManager = LocalClipboardManager.current

    // Document text state & undo/redo history
    var docText by remember { mutableStateOf(node.documentNote.ifEmpty { node.content }) }
    val undoHistory = remember { mutableStateListOf<String>() }
    val redoHistory = remember { mutableStateListOf<String>() }

    var isAutoSaving by remember { mutableStateOf(false) }

    var fontSizeSp by remember { mutableFloatStateOf(16f) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showTemplateMenu by remember { mutableStateOf(false) }

    // Focus
    val focusRequester = remember { FocusRequester() }

    // Auto-save logic
    LaunchedEffect(docText) {
        if (docText != node.documentNote) {
            isAutoSaving = true
            delay(500) // Debounce auto-save
            viewModel.updateNodeDetails(node.copy(documentNote = docText, updatedAt = System.currentTimeMillis()))
            isAutoSaving = false
            SoundEffectManager.playSave()
        }
    }

    // Helper function to mutate document text with undo history tracking
    fun updateDoc(newText: String) {
        if (newText != docText) {
            undoHistory.add(docText)
            if (undoHistory.size > 30) undoHistory.removeAt(0)
            redoHistory.clear()
            docText = newText
        }
    }

    fun undo() {
        if (undoHistory.isNotEmpty()) {
            val prev = undoHistory.removeAt(undoHistory.size - 1)
            redoHistory.add(docText)
            docText = prev
            SoundEffectManager.playClick()
        }
    }

    fun redo() {
        if (redoHistory.isNotEmpty()) {
            val next = redoHistory.removeAt(redoHistory.size - 1)
            undoHistory.add(docText)
            docText = next
            SoundEffectManager.playClick()
        }
    }

    // Word & Character stats
    val wordCount = remember(docText) {
        if (docText.isBlank()) 0 else docText.trim().split("\\s+".toRegex()).size
    }
    val charCount = remember(docText) { docText.length }
    val readTimeMinutes = remember(wordCount) {
        (wordCount / 200).coerceAtLeast(1)
    }

    val nodeColor = try {
        Color(android.graphics.Color.parseColor(node.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(nodeColor)
                        )
                        Column {
                            Text(
                                text = node.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${Strings.translateNodeType(node.nodeType, lang)} ${Strings.get("doc_subtitle", lang)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isAutoSaving) Strings.get("saving", lang) else Strings.get("saved", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAutoSaving) MaterialTheme.colorScheme.tertiary else Color.Gray
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        SoundEffectManager.playCloseNote()
                        viewModel.updateNodeDetails(node.copy(documentNote = docText))
                        onBackToCanvas()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Strings.get("back", lang)
                        )
                    }
                },
                actions = {
                    // Search in Document
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        showSearchBar = !showSearchBar
                    }) {
                        Icon(Icons.Default.Search, contentDescription = Strings.get("search_in_doc", lang))
                    }

                    // Story Templates Menu
                    Box {
                        IconButton(onClick = {
                            SoundEffectManager.playClick()
                            showTemplateMenu = true
                        }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = Strings.get("story_templates", lang), tint = MaterialTheme.colorScheme.primary)
                        }

                        DropdownMenu(
                            expanded = showTemplateMenu,
                            onDismissRequest = { showTemplateMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(Strings.get("template_character", lang)) },
                                onClick = {
                                    showTemplateMenu = false
                                    updateDoc(docText + if (lang == "ar") "\n\n# السيرة الذاتية للشخصية\n- **الاسم الكامل**: \n- **الدور**: بطل / خصم\n- **الهدف الرئيسي**: \n- **نقطة الضعف**: \n- **القدرة الخاصة**: \n- **السر الغامض**: \n" else "\n\n# Character Bio\n- **Full Name**: \n- **Role**: Protagonist / Antagonist\n- **Goal**: \n- **Flaw**: \n- **Ability**: \n- **Secret**: \n")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("template_scene", lang)) },
                                onClick = {
                                    showTemplateMenu = false
                                    updateDoc(docText + if (lang == "ar") "\n\n# مخطط المشهد\n## 1. بداية المشهد والحدث المشوق\n- المكان والمزاج العام:\n- الصراع الرئيسي:\n\n## 2. ذروة المشهد والنقطة الحاسمة\n- تحول الأحداث:\n- النتيجة والتأثير:\n" else "\n\n# Scene Outline\n## 1. Opening Hook\n- Setting & Mood:\n- Conflict:\n\n## 2. Climax & Twist\n- Turn of events:\n- Outcome:\n")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("template_world", lang)) },
                                onClick = {
                                    showTemplateMenu = false
                                    updateDoc(docText + if (lang == "ar") "\n\n# معلومات المملكة والتاريخ\n- **الإقليم**: \n- **نظام الحكم**: \n- **قواعد السحر والقوة**: \n- **المحرمات والخطوط الحمراء**: \n" else "\n\n# Kingdom & Lore\n- **Region**: \n- **Governing Body**: \n- **Magic / Power Rules**: \n- **Forbidden Taboo**: \n")
                                }
                            )
                        }
                    }

                    // Copy All
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        clipboardManager.setText(AnnotatedString(docText))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = Strings.get("copy_text", lang))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // In-Document Find / Search Bar
            AnimatedVisibility(visible = showSearchBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(Strings.get("search_in_doc", lang)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    val count = docText.split(searchQuery, ignoreCase = true).size - 1
                                    Text("$count ${Strings.get("matches", lang)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
                                }
                            }
                        )
                        IconButton(onClick = { showSearchBar = false }) {
                            Icon(Icons.Default.Close, contentDescription = Strings.get("close_search", lang))
                        }
                    }
                }
            }

            // Rich Document Formatting Ribbon
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo
                    IconButton(
                        onClick = { undo() },
                        enabled = undoHistory.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }

                    // Redo
                    IconButton(
                        onClick = { redo() },
                        enabled = redoHistory.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo")
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp))

                    // Heading 1 (# )
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        updateDoc(docText + "\n# ")
                    }) {
                        Text("H1", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }

                    // Heading 2 (## )
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        updateDoc(docText + "\n## ")
                    }) {
                        Text("H2", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Heading 3 (### )
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        updateDoc(docText + "\n### ")
                    }) {
                        Text("H3", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp))

                    // Bold (**text**)
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        updateDoc(docText + "**عريض**")
                    }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                    }

                    // Italic (*text*)
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        updateDoc(docText + "*مائل*")
                    }) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                    }

                    // Bullet List
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        updateDoc(docText + "\n- ")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bullet List")
                    }

                    // Numbered List
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        updateDoc(docText + "\n1. ")
                    }) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List")
                    }

                    // Quote block
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        updateDoc(docText + "\n> ")
                    }) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Quote Block")
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp))

                    // Font Size Minus
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(12f)
                    }) {
                        Text("A-", fontWeight = FontWeight.Bold)
                    }

                    Text("${fontSizeSp.toInt()}sp", fontSize = 12.sp)

                    // Font Size Plus
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        fontSizeSp = (fontSizeSp + 2f).coerceAtMost(32f)
                    }) {
                        Text("A+", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Distraction-Free Long Form Writing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                val textColor = MaterialTheme.colorScheme.onSurface
                BasicTextField(
                    value = docText,
                    onValueChange = { updateDoc(it) },
                    textStyle = TextStyle(
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.5f).sp,
                        fontFamily = FontFamily.Serif,
                        color = textColor
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .verticalScroll(rememberScrollState()),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (docText.isEmpty()) {
                                Text(
                                    text = Strings.get("write_placeholder", lang),
                                    style = TextStyle(
                                        fontSize = fontSizeSp.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = textColor.copy(alpha = 0.4f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // Document Statistics Footer
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount ${Strings.get("words", lang)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$charCount ${Strings.get("chars", lang)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "~$readTimeMinutes ${Strings.get("min_read", lang)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(onClick = {
                        SoundEffectManager.playCloseNote()
                        onBackToCanvas()
                    }) {
                        Text(Strings.get("done_writing", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
