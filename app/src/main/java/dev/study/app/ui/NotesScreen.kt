package dev.study.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.study.app.domain.model.BlockType
import dev.study.app.domain.model.Note
import dev.study.app.domain.model.RichTextBlock
import dev.study.app.domain.model.RichTextDocument
import dev.study.app.export.PdfExporter
import dev.study.app.ui.theme.StudyShapes
import dev.study.app.ui.theme.StudySpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NotesScreen(
    subjectId: String,
    subjectName: String,
    subjectColorHex: String = "#00668B",
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onLoadNotesPage: suspend (offset: Int, limit: Int) -> List<Note>
) {
    var activeNote by remember { mutableStateOf<Note?>(null) }
    val context = LocalContext.current

    val loadedNotes = remember { mutableStateListOf<Note>() }
    var offset by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(subjectId) {
        loadedNotes.clear()
        offset = 0
        hasMore = true
        isLoading = true
        val initial = onLoadNotesPage(0, 15)
        loadedNotes.addAll(initial)
        offset = initial.size
        hasMore = initial.size >= 15
        isLoading = false
    }

    val loadMore = {
        if (!isLoading && hasMore) {
            isLoading = true
            scope.launch {
                val next = onLoadNotesPage(offset, 15)
                loadedNotes.addAll(next)
                offset += next.size
                hasMore = next.size >= 15
                isLoading = false
            }
            Unit
        }
    }

    if (activeNote == null) {
        NoteListScreen(
            subjectId       = subjectId,
            subjectName     = subjectName,
            subjectColorHex = subjectColorHex,
            notes           = loadedNotes,
            onOpen          = { activeNote = it },
            onDelete        = { id ->
                onDeleteNote(id)
                loadedNotes.removeAll { it.id == id }
                offset = maxOf(0, offset - 1)
            },
            onNew           = {
                activeNote = Note(
                    id          = UUID.randomUUID().toString(),
                    subjectId   = subjectId,
                    title       = "",
                    contentJson = Json.encodeToString(RichTextDocument(listOf(RichTextBlock(BlockType.PARAGRAPH, "")))),
                    updatedAt   = System.currentTimeMillis()
                )
            },
            onNavigateBack  = onNavigateBack,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onLoadMore      = loadMore
        )
    } else {
        NoteEditorScreen(
            note    = activeNote!!,
            context = context,
            onBack  = { updated ->
                if (updated.title.isNotBlank() || extractPreview(updated.contentJson).isNotBlank()) {
                    onSaveNote(updated)
                    val idx = loadedNotes.indexOfFirst { it.id == updated.id }
                    if (idx != -1) {
                        loadedNotes[idx] = updated
                    } else {
                        loadedNotes.add(0, updated)
                        offset++
                    }
                }
                activeNote = null
            }
        )
    }
}

// ─── Note list ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun NoteListScreen(
    subjectId: String,
    subjectName: String,
    subjectColorHex: String,
    notes: List<Note>,
    onOpen: (Note) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onLoadMore: () -> Unit
) {
    val accentColor = remember(subjectColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(subjectColorHex)) }.getOrDefault(Color(0xFF00668B))
    }

    with(sharedTransitionScope) {
        Scaffold(
            modifier = Modifier.sharedBounds(
                rememberSharedContentState(key = "subject_card_bg_${subjectId}"),
                animatedVisibilityScope = animatedVisibilityScope
            ),
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.sharedBounds(
                                rememberSharedContentState(key = "subject_card_name_${subjectId}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        ) {
                            Text(subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onNew, shape = StudyShapes.xLarge) {
                    Icon(Icons.Default.Add, contentDescription = "New Note")
                }
            }
        ) { padding ->
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val shouldLoadMore = remember {
                derivedStateOf {
                    val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                    lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
                }
            }
            LaunchedEffect(shouldLoadMore.value) {
                if (shouldLoadMore.value) {
                    onLoadMore()
                }
            }

            if (notes.isEmpty()) {
                NotesEmptyState(modifier = Modifier.fillMaxSize().padding(padding), onNew = onNew)
            } else {
                LazyColumn(
                    state           = listState,
                    modifier        = Modifier.fillMaxSize().padding(padding),
                    contentPadding  = PaddingValues(StudySpacing.space16),
                    verticalArrangement = Arrangement.spacedBy(StudySpacing.space8)
                ) {
                    items(notes, key = { it.id }) { note ->
                        SwipeToDeleteNoteCard(
                            note        = note,
                            accentColor = accentColor,
                            onClick     = { onOpen(note) },
                            onDelete    = { onDelete(note.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteNoteCard(
    note: Note,
    accentColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true }
            else false
        }
    )

    SwipeToDismissBox(
        state            = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, StudyShapes.large)
                    .padding(end = StudySpacing.space16),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        NoteCard(note = note, accentColor = accentColor, onClick = onClick)
    }
}

@Composable
private fun NoteCard(note: Note, accentColor: Color, onClick: () -> Unit) {
    val preview = remember(note.contentJson) { extractPreview(note.contentJson) }

    Card(
        onClick    = onClick,
        shape      = StudyShapes.large,
        elevation  = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier   = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left accent bar matching subject color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(
                modifier = Modifier.weight(1f).padding(StudySpacing.space12),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    note.title.ifBlank { "Untitled Note" },
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (preview.isNotBlank()) {
                    Text(
                        preview,
                        style   = MaterialTheme.typography.bodySmall,
                        color   = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    relativeTime(note.updatedAt),
                    style  = MaterialTheme.typography.labelSmall,
                    color  = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun NotesEmptyState(modifier: Modifier, onNew: () -> Unit) {
    Column(
        modifier             = modifier,
        horizontalAlignment  = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
    ) {
        // Canvas-drawn page icon using Box/shapes
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("📝", style = MaterialTheme.typography.displaySmall)
        }
        Spacer(Modifier.height(StudySpacing.space16))
        Text("No notes yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(StudySpacing.space8))
        Text(
            "Tap the button below to write your first note for this subject.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        Spacer(Modifier.height(StudySpacing.space20))
        Button(onClick = onNew, shape = StudyShapes.medium) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Write a Note", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Note editor ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorScreen(
    note: Note,
    context: Context,
    onBack: (Note) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    val blocks = remember {
        mutableStateListOf<RichTextBlock>().apply {
            addAll(
                runCatching { Json.decodeFromString<RichTextDocument>(note.contentJson).blocks }
                    .getOrDefault(listOf(RichTextBlock(BlockType.PARAGRAPH, "")))
            )
        }
    }

    var savedVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Autosave indicator: trigger "Saved" label 800ms after last change
    fun onContentChange() {
        scope.launch {
            delay(800)
            savedVisible = true
            delay(1500)
            savedVisible = false
        }
    }

    fun buildNote() = note.copy(
        title       = title.ifBlank { "Untitled Note" },
        contentJson = Json.encodeToString(RichTextDocument(blocks.toList())),
        updatedAt   = System.currentTimeMillis()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = savedVisible, enter = fadeIn(tween(300)), exit = fadeOut(tween(600))) {
                        Text(
                            "Saved ✓",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBack(buildNote()) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Save and go back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportPdfAndShare(context, buildNote()) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export as PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Sticky formatting toolbar
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = StudySpacing.space8, vertical = StudySpacing.space4),
                    horizontalArrangement = Arrangement.spacedBy(StudySpacing.space4)
                ) {
                    FormatButton("¶ Text") {
                        blocks.add(RichTextBlock(BlockType.PARAGRAPH, ""))
                        onContentChange()
                    }
                    FormatButton("H Head") {
                        blocks.add(RichTextBlock(BlockType.HEADING, ""))
                        onContentChange()
                    }
                    FormatButton("• List") {
                        blocks.add(RichTextBlock(BlockType.BULLET_ITEM, ""))
                        onContentChange()
                    }
                    FormatButton("☐ Task") {
                        blocks.add(RichTextBlock(BlockType.CHECKBOX_ITEM, "", isChecked = false))
                        onContentChange()
                    }
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(48.dp)) // keyboard nav height reserve
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = StudySpacing.space16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(StudySpacing.space8)
        ) {
            Spacer(Modifier.height(StudySpacing.space12))

            // Title field — large, no border, full visual weight
            BasicTextField(
                value       = title,
                onValueChange = { title = it; onContentChange() },
                textStyle   = TextStyle(
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine  = true,
                modifier    = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text(
                            "Note title…",
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    inner()
                }
            )

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier  = Modifier.padding(vertical = StudySpacing.space4)
            )

            // Blocks
            blocks.forEachIndexed { index, block ->
                BlockEditorRow(
                    block    = block,
                    onUpdate = { updated ->
                        blocks[index] = updated
                        onContentChange()
                    },
                    onDelete = {
                        blocks.removeAt(index)
                        onContentChange()
                    }
                )
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun FormatButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick       = onClick,
        shape         = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier      = Modifier.height(36.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BlockEditorRow(
    block:    RichTextBlock,
    onUpdate: (RichTextBlock) -> Unit,
    onDelete: () -> Unit
) {
    val isHeading   = block.type == BlockType.HEADING
    val isBullet    = block.type == BlockType.BULLET_ITEM
    val isCheckbox  = block.type == BlockType.CHECKBOX_ITEM
    val isChecked   = block.isChecked == true

    val textStyle = when {
        isHeading  -> TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground)
        else       -> TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onBackground,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else null)
    }

    val containerColor by animateColorAsState(
        targetValue   = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(200),
        label         = "blockBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(8.dp))
            .padding(vertical = 2.dp),
        verticalAlignment = if (isHeading) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(StudySpacing.space8)
    ) {
        // Leading element per block type
        when {
            isCheckbox -> Checkbox(
                checked         = isChecked,
                onCheckedChange = { onUpdate(block.copy(isChecked = it)) },
                modifier        = Modifier.size(24.dp)
            )
            isBullet   -> Text("•", style = MaterialTheme.typography.bodyLarge,
                               color = MaterialTheme.colorScheme.primary,
                               modifier = Modifier.padding(top = 2.dp, start = 4.dp))
            isHeading  -> Box(
                modifier = Modifier
                    .width(3.dp).height(22.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            else -> Spacer(Modifier.width(4.dp))
        }

        // Text field
        BasicTextField(
            value         = block.text,
            onValueChange = { onUpdate(block.copy(text = it)) },
            textStyle     = textStyle,
            cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
            modifier      = Modifier.weight(1f).padding(vertical = 6.dp),
            decorationBox = { inner ->
                if (block.text.isEmpty()) {
                    Text(
                        text  = when {
                            isHeading  -> "Heading…"
                            isBullet   -> "List item…"
                            isCheckbox -> "Task…"
                            else       -> "Start typing…"
                        },
                        style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
                inner()
            }
        )

        // Delete block (small, low-emphasis)
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove block",
                tint     = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun extractPreview(contentJson: String): String =
    runCatching {
        Json.decodeFromString<RichTextDocument>(contentJson).blocks
            .filter { it.text.isNotBlank() }
            .joinToString(" · ") { it.text.trim() }
            .take(100)
    }.getOrDefault("")

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000       -> "Just now"
        diff < 3_600_000    -> "${diff / 60_000}m ago"
        diff < 86_400_000   -> "${diff / 3_600_000}h ago"
        diff < 172_800_000  -> "Yesterday"
        else                -> "${diff / 86_400_000}d ago"
    }
}

private fun exportPdfAndShare(context: Context, note: Note) {
    runCatching {
        val file   = File(context.cacheDir, "${note.title.replace(" ", "_").ifBlank { "note" }}.pdf")
        val result = PdfExporter().exportNoteToPdf(note, FileOutputStream(file))
        if (result.isSuccess) {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share Note as PDF"
                )
            )
        }
    }
}
