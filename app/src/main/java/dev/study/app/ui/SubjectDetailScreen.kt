package dev.study.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.study.app.domain.model.ScheduleBlock
import dev.study.app.domain.model.Subject
import dev.study.app.ui.theme.StudyShapes
import dev.study.app.ui.theme.StudySpacing
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SubjectDetailScreen(
    subjectId: String?,
    subjects: List<Subject>,
    onSaveSubject: (Subject) -> Unit,
    onArchiveSubject: (String) -> Unit,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val existingSubject = subjects.find { it.id == subjectId }
    val isNew           = existingSubject == null

    var name             by remember { mutableStateOf(existingSubject?.name ?: "") }
    var colorHex         by remember { mutableStateOf(existingSubject?.colorHex ?: "#00668B") }
    var focusModeEnabled by remember { mutableStateOf(existingSubject?.focusModeEnabled ?: false) }
    val classBlocks = remember {
        mutableStateListOf<ScheduleBlock>().apply { addAll(existingSubject?.classBlocks ?: emptyList()) }
    }
    val studyBlocks = remember {
        mutableStateListOf<ScheduleBlock>().apply { addAll(existingSubject?.studyBlocks ?: emptyList()) }
    }
    var errorMsg         by remember { mutableStateOf<String?>(null) }
    var showArchiveDialog by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#00668B", "#2D6A4F", "#D04848", "#7209B7",
        "#FFB400", "#06D6A0", "#4A5568", "#E76F51"
    )

    // Archive confirmation
    if (showArchiveDialog && existingSubject != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            shape            = StudyShapes.large,
            title            = { Text("Archive subject?") },
            text             = {
                Text("\"${existingSubject.name}\" will be archived. All notes and tasks are preserved.")
            },
            confirmButton = {
                Button(
                    onClick = { onArchiveSubject(existingSubject.id); onNavigateBack() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape   = StudyShapes.medium
                ) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isNew) "New Subject" else "Edit Subject", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew) {
                        TextButton(onClick = { showArchiveDialog = true }) {
                            Text("Archive", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        with(sharedTransitionScope) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .then(
                        if (existingSubject != null) {
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "subject_card_bg_${existingSubject.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        } else Modifier
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(StudySpacing.space16),
                verticalArrangement = Arrangement.spacedBy(StudySpacing.space16)
            ) {
                // Error banner
                AnimatedVisibility(visible = errorMsg != null, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        color    = MaterialTheme.colorScheme.errorContainer,
                        shape    = StudyShapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            errorMsg ?: "",
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            style    = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(StudySpacing.space12)
                        )
                    }
                }

                // Subject name field
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; errorMsg = null },
                    label         = { Text("Subject Name") },
                    placeholder   = { Text("e.g. Calculus, Biology…") },
                    modifier      = Modifier.fillMaxWidth().then(
                        if (existingSubject != null) {
                            Modifier.sharedBounds(
                                rememberSharedContentState(key = "subject_card_name_${existingSubject.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        } else Modifier
                    ),
                    singleLine    = true,
                    shape         = StudyShapes.medium
                )

            // Color picker
            SubjectSection(title = "Color Tag") {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(StudySpacing.space12)
                ) {
                    presetColors.forEach { hex ->
                        val c          = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary)
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint     = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Focus Mode toggle
            SubjectSection(title = "Quiet Time") {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Automatically silence phone",
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Your phone goes quiet during this subject's class and study sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(StudySpacing.space12))
                    Switch(checked = focusModeEnabled, onCheckedChange = { focusModeEnabled = it })
                }
            }

            // Class schedule
            ScheduleBlockSection(
                title     = "Class Schedule",
                subtitle  = "When do your classes meet?",
                blocks    = classBlocks,
                onAddBlock = {
                    classBlocks.add(
                        ScheduleBlock(id = UUID.randomUUID().toString(), dayOfWeek = 1,
                            startTime = "09:00", endTime = "10:00", label = null)
                    )
                },
                onUpdateBlock = { index, updated -> classBlocks[index] = updated },
                onRemoveBlock = { block -> classBlocks.remove(block) }
            )

            // Study sessions
            ScheduleBlockSection(
                title     = "Study Sessions",
                subtitle  = "Plan your personal study time",
                blocks    = studyBlocks,
                onAddBlock = {
                    studyBlocks.add(
                        ScheduleBlock(id = UUID.randomUUID().toString(), dayOfWeek = 1,
                            startTime = "14:00", endTime = "15:00", label = null)
                    )
                },
                onUpdateBlock = { index, updated -> studyBlocks[index] = updated },
                onRemoveBlock = { block -> studyBlocks.remove(block) }
            )

            // Save button
            Button(
                onClick = {
                    when {
                        name.isBlank() ->
                            errorMsg = "Please enter a subject name."
                        classBlocks.isEmpty() && studyBlocks.isEmpty() ->
                            errorMsg = "Add at least one class schedule or study session."
                        else -> {
                            onSaveSubject(
                                Subject(
                                    id               = existingSubject?.id ?: UUID.randomUUID().toString(),
                                    name             = name.trim(),
                                    colorHex         = colorHex,
                                    classBlocks      = classBlocks.toList(),
                                    studyBlocks      = studyBlocks.toList(),
                                    focusModeEnabled = focusModeEnabled,
                                    archived         = false,
                                    createdAt        = existingSubject?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = StudyShapes.medium
            ) {
                Text(
                    if (isNew) "Create Subject" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(StudySpacing.space8))
        }
        }
    }
}

// ─── Section container ────────────────────────────────────────────────────────
@Composable
private fun SubjectSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        tonalElevation = 2.dp,
        shape          = StudyShapes.large,
        modifier       = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(StudySpacing.space16),
            verticalArrangement = Arrangement.spacedBy(StudySpacing.space12)
        ) {
            Text(
                title,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

// ─── Schedule block section ───────────────────────────────────────────────────
@Composable
private fun ScheduleBlockSection(
    title:        String,
    subtitle:     String,
    blocks:       List<ScheduleBlock>,
    onAddBlock:   () -> Unit,
    onUpdateBlock:(Int, ScheduleBlock) -> Unit,
    onRemoveBlock:(ScheduleBlock) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape          = StudyShapes.large,
        modifier       = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(StudySpacing.space16),
            verticalArrangement = Arrangement.spacedBy(StudySpacing.space12)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = onAddBlock) {
                    Icon(Icons.Default.Add, contentDescription = "Add session")
                }
            }

            if (blocks.isEmpty()) {
                Text(
                    "No sessions yet. Tap + to add one.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            blocks.forEachIndexed { index, block ->
                BlockRow(
                    block    = block,
                    onUpdate = { updated -> onUpdateBlock(index, updated) },
                    onRemove = { onRemoveBlock(block) }
                )
                if (index < blocks.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

// ─── Single schedule block row ────────────────────────────────────────────────
@Composable
private fun BlockRow(
    block:    ScheduleBlock,
    onUpdate: (ScheduleBlock) -> Unit,
    onRemove: () -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(verticalArrangement = Arrangement.spacedBy(StudySpacing.space8)) {
        // Horizontally scrollable day chips — never wrap on narrow screens
        Row(
            modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(StudySpacing.space4),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            days.forEachIndexed { idx, dayLabel ->
                val dayNum     = idx + 1
                val isSelected = block.dayOfWeek == dayNum
                FilterChip(
                    selected = isSelected,
                    onClick  = { onUpdate(block.copy(dayOfWeek = dayNum)) },
                    label    = { Text(dayLabel, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }
        // Time fields + delete
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StudySpacing.space8),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = block.startTime,
                onValueChange = { onUpdate(block.copy(startTime = it)) },
                modifier      = Modifier.weight(1f),
                label         = { Text("Start (HH:mm)") },
                singleLine    = true,
                shape         = StudyShapes.small
            )
            OutlinedTextField(
                value         = block.endTime,
                onValueChange = { onUpdate(block.copy(endTime = it)) },
                modifier      = Modifier.weight(1f),
                label         = { Text("End (HH:mm)") },
                singleLine    = true,
                shape         = StudyShapes.small
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
