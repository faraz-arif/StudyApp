package dev.study.app.ui

import dev.study.app.ui.components.ConfettiCelebration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.study.app.domain.model.Assessment
import dev.study.app.domain.model.AssessmentType
import dev.study.app.domain.model.Subject
import dev.study.app.ui.theme.StudyShapes
import dev.study.app.ui.theme.StudySpacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentsScreen(
    subjects: List<Subject>,
    onSaveAssessment: (Assessment) -> Unit,
    onDeleteAssessment: (String) -> Unit,
    onLoadAssessmentsPage: suspend (offset: Int, limit: Int) -> List<Assessment>
) {
    var showAddSheet    by remember { mutableStateOf(false) }
    var filterCompleted by remember { mutableStateOf(false) }
    var triggerConfetti by remember { mutableStateOf(false) }

    val loadedAssessments = remember { mutableStateListOf<Assessment>() }
    var offset by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadPage(clear: Boolean) {
        if (isLoading) return
        isLoading = true
        scope.launch {
            val currentOffset = if (clear) 0 else offset
            val nextPage = onLoadAssessmentsPage(currentOffset, 20)
            if (clear) {
                loadedAssessments.clear()
            }
            loadedAssessments.addAll(nextPage)
            offset = currentOffset + nextPage.size
            hasMore = nextPage.size >= 20
            isLoading = false
        }
    }

    LaunchedEffect(filterCompleted) {
        loadPage(clear = true)
    }

    val loadMore = {
        if (!isLoading && hasMore) {
            loadPage(clear = false)
        }
    }

    val filtered = if (filterCompleted) loadedAssessments else loadedAssessments.filter { !it.completed }
    val sorted   = filtered.sortedWith(compareBy({ it.completed }, { it.dueAt }))

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title  = { Text("Assessments", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    actions = {
                        FilterChip(
                            selected  = filterCompleted,
                            onClick   = { filterCompleted = !filterCompleted },
                            label     = { Text(if (filterCompleted) "Showing All" else "Active Only") },
                            modifier  = Modifier.padding(end = StudySpacing.space8)
                        )
                    }
                )
            },
            floatingActionButton = {
                if (subjects.isNotEmpty()) {
                    FloatingActionButton(
                        onClick        = { showAddSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape          = StudyShapes.xLarge
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Assessment",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
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
                    loadMore()
                }
            }

            if (sorted.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(StudySpacing.space8)
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp))
                        }
                        Text("All clear!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (subjects.isEmpty()) "Add a subject first, then tap + to schedule a task."
                            else "Tap + to add an exam, quiz, or assignment.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier  = Modifier.padding(horizontal = 32.dp)
                        )
                        if (subjects.isNotEmpty()) {
                            Button(onClick = { showAddSheet = true }, shape = StudyShapes.medium) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Task", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state           = listState,
                    modifier        = Modifier.fillMaxSize().padding(padding),
                    contentPadding  = PaddingValues(StudySpacing.space16),
                    verticalArrangement = Arrangement.spacedBy(StudySpacing.space8)
                ) {
                    items(sorted, key = { it.id }) { assessment ->
                        val subject = subjects.find { it.id == assessment.subjectId }
                        AssessmentCard(
                            assessment       = assessment,
                            subjectName      = subject?.name,
                            subjectColorHex  = subject?.colorHex,
                            onToggleComplete = {
                                val nextComplete = !assessment.completed
                                if (nextComplete) {
                                    triggerConfetti = true
                                }
                                onSaveAssessment(assessment.copy(completed = nextComplete))
                                
                                val idx = loadedAssessments.indexOfFirst { it.id == assessment.id }
                                if (idx != -1) {
                                    loadedAssessments[idx] = assessment.copy(completed = nextComplete)
                                } else {
                                    loadedAssessments.add(assessment.copy(completed = nextComplete))
                                    offset++
                                }
                            },
                            onDelete         = {
                                onDeleteAssessment(assessment.id)
                                loadedAssessments.removeAll { it.id == assessment.id }
                                offset = maxOf(0, offset - 1)
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        ConfettiCelebration(trigger = triggerConfetti) {
            triggerConfetti = false
        }
    }

    // ── Add Assessment BottomSheet ────────────────────────────────────────────
    if (showAddSheet && subjects.isNotEmpty()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState       = sheetState,
            shape            = StudyShapes.xLarge
        ) {
            AddAssessmentForm(
                subjects  = subjects,
                onDismiss = { showAddSheet = false },
                onSave    = { assessment ->
                    onSaveAssessment(assessment)
                    loadedAssessments.add(0, assessment)
                    offset++
                    showAddSheet = false
                }
            )
        }
    }
}

// ─── Assessment card ──────────────────────────────────────────────────────────
@Composable
private fun AssessmentCard(
    assessment:      Assessment,
    subjectName:     String?,
    subjectColorHex: String?,
    onToggleComplete: () -> Unit,
    onDelete:        () -> Unit
) {
    val isOverdue  = !assessment.completed && assessment.dueAt < System.currentTimeMillis()
    val typeColor  = assessmentTypeColor(assessment.type)
    val subjectColor = remember(subjectColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(subjectColorHex ?: "#4A5568")) }
            .getOrDefault(Color(0xFF4A5568))
    }

    val containerColor by animateColorAsState(
        targetValue   = when {
            assessment.completed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            isOverdue            -> MaterialTheme.colorScheme.errorContainer
            else                 -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label         = "cardColor"
    )

    Surface(
        shape    = StudyShapes.large,
        color    = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar = assessment type color
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(typeColor))

            Row(
                modifier = Modifier.weight(1f).padding(StudySpacing.space12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StudySpacing.space12)
            ) {
                // Type icon
                Text(assessmentTypeEmoji(assessment.type), style = MaterialTheme.typography.titleMedium)

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        assessment.title,
                        style           = MaterialTheme.typography.titleMedium,
                        fontWeight      = FontWeight.SemiBold,
                        textDecoration  = if (assessment.completed) TextDecoration.LineThrough else null,
                        color           = if (assessment.completed) MaterialTheme.colorScheme.onSurfaceVariant
                                          else MaterialTheme.colorScheme.onSurface
                    )
                    if (subjectName != null) {
                        Text(subjectName, style = MaterialTheme.typography.bodySmall,
                            color = subjectColor, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        text  = when {
                            isOverdue            -> "⚠ OVERDUE · Due ${formatAssessmentDate(assessment.dueAt)}"
                            assessment.completed -> "✓ Completed"
                            else                 -> "Due ${formatAssessmentDate(assessment.dueAt)}"
                        },
                        style      = MaterialTheme.typography.bodySmall,
                        color      = when {
                            isOverdue            -> MaterialTheme.colorScheme.error
                            assessment.completed -> MaterialTheme.colorScheme.onSurfaceVariant
                            else                 -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = onToggleComplete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Toggle complete",
                            tint     = if (assessment.completed) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ─── Add form inside BottomSheet ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAssessmentForm(
    subjects:  List<Subject>,
    onDismiss: () -> Unit,
    onSave:    (Assessment) -> Unit
) {
    var title               by remember { mutableStateOf("") }
    var selectedSubjectIdx  by remember { mutableStateOf(0) }
    var selectedType        by remember { mutableStateOf(AssessmentType.EXAM) }
    var subjectExpanded     by remember { mutableStateOf(false) }
    var typeExpanded        by remember { mutableStateOf(false) }
    var showDatePicker      by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 86_400_000L
    )
    val selectedDateMs  = datePickerState.selectedDateMillis ?: (System.currentTimeMillis() + 86_400_000L)
    val formattedDate   = remember(selectedDateMs) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMs))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StudySpacing.space20)
            .padding(bottom = StudySpacing.space32),
        verticalArrangement = Arrangement.spacedBy(StudySpacing.space16)
    ) {
        Text("New Assessment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Title
        OutlinedTextField(
            value         = title,
            onValueChange = { title = it },
            label         = { Text("Title  (e.g. Midterm Exam)") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            shape         = StudyShapes.medium
        )

        // Subject dropdown
        ExposedDropdownMenuBox(expanded = subjectExpanded, onExpandedChange = { subjectExpanded = it }) {
            OutlinedTextField(
                value         = subjects[selectedSubjectIdx].name,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Subject") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                shape         = StudyShapes.medium
            )
            ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                subjects.forEachIndexed { idx, subject ->
                    DropdownMenuItem(
                        text    = { Text(subject.name) },
                        onClick = { selectedSubjectIdx = idx; subjectExpanded = false }
                    )
                }
            }
        }

        // Type dropdown
        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
            OutlinedTextField(
                value         = "${assessmentTypeEmoji(selectedType)} ${selectedType.name}",
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Type") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                shape         = StudyShapes.medium
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                AssessmentType.entries.forEach { type ->
                    DropdownMenuItem(
                        text    = { Text("${assessmentTypeEmoji(type)} ${type.name}") },
                        onClick = { selectedType = type; typeExpanded = false }
                    )
                }
            }
        }

        // Due date — tappable field opening DatePickerDialog
        OutlinedTextField(
            value         = formattedDate,
            onValueChange = {},
            readOnly      = true,
            label         = { Text("Due Date") },
            trailingIcon  = { Icon(Icons.Default.DateRange, contentDescription = null) },
            modifier      = Modifier.fillMaxWidth(),
            shape         = StudyShapes.medium,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                .also { src ->
                    LaunchedEffect(src) {
                        src.interactions.collect { interaction ->
                            if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                showDatePicker = true
                            }
                        }
                    }
                }
        )

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StudySpacing.space12)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = StudyShapes.medium) {
                Text("Cancel")
            }
            Button(
                onClick  = {
                    if (title.isNotBlank()) {
                        onSave(Assessment(
                            id                     = UUID.randomUUID().toString(),
                            subjectId              = subjects[selectedSubjectIdx].id,
                            type                   = selectedType,
                            title                  = title.trim(),
                            dueAt                  = selectedDateMs,
                            reminderOffsetsMinutes = listOf(1440, 60),
                            completed              = false
                        ))
                    }
                },
                enabled  = title.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape    = StudyShapes.medium
            ) { Text("Add Task", fontWeight = FontWeight.Bold) }
        }
    }

    // DatePickerDialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton    = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
@Composable
private fun assessmentTypeColor(type: AssessmentType): Color = when (type) {
    AssessmentType.EXAM       -> MaterialTheme.colorScheme.error
    AssessmentType.QUIZ       -> MaterialTheme.colorScheme.tertiary
    AssessmentType.PROJECT    -> MaterialTheme.colorScheme.secondary
    AssessmentType.ASSIGNMENT -> MaterialTheme.colorScheme.primary
    AssessmentType.OTHER      -> MaterialTheme.colorScheme.outline
}

private fun assessmentTypeEmoji(type: AssessmentType) = when (type) {
    AssessmentType.EXAM       -> "📝"
    AssessmentType.QUIZ       -> "❓"
    AssessmentType.PROJECT    -> "📁"
    AssessmentType.ASSIGNMENT -> "📌"
    AssessmentType.OTHER      -> "📋"
}

private fun formatAssessmentDate(time: Long): String =
    SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(time))
