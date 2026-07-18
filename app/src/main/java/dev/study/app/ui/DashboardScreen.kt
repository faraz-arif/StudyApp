package dev.study.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.study.app.MainActivity
import dev.study.app.domain.model.Assessment
import dev.study.app.domain.model.AssessmentType
import dev.study.app.domain.model.Subject
import dev.study.app.ui.theme.StudyShapes
import dev.study.app.ui.theme.StudySpacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    subjects: List<Subject>,
    assessments: List<Assessment>,
    onNavigateToSubjectDetail: (String) -> Unit,
    onNavigateToNotes: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val activeSubjId   by MainActivity.activeSessionSubjectId.collectAsState()
    val activeSubjName by MainActivity.activeSessionSubjectName.collectAsState()
    val activeIsStudy  by MainActivity.activeSessionIsStudy.collectAsState()
    var showDndBanner  by remember { mutableStateOf(true) }

    LaunchedEffect(activeSubjId) { if (activeSubjId != null) showDndBanner = true }

    val activeSubjects      = subjects.filter { !it.archived }
    val pendingAssessments  = assessments.filter { !it.completed }
    val overdueCount        = pendingAssessments.count { it.dueAt < System.currentTimeMillis() }
    val upcomingAssessments = pendingAssessments.sortedBy { it.dueAt }.take(5)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = StudySpacing.space16),
        contentPadding = PaddingValues(top = StudySpacing.space20, bottom = StudySpacing.space24),
        verticalArrangement = Arrangement.spacedBy(StudySpacing.space20)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Study Planner",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Your classes, sessions & upcoming tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Focus Mode banner ─────────────────────────────────────────────────
        item {
            AnimatedVisibility(
                visible = activeSubjId != null && showDndBanner,
                enter   = fadeIn() + slideInVertically { -it / 2 },
                exit    = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Surface(
                    color    = MaterialTheme.colorScheme.tertiaryContainer,
                    shape    = StudyShapes.large,
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Row(
                        modifier  = Modifier.padding(StudySpacing.space16),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StudySpacing.space12)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Focus Mode Active",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "$activeSubjName · ${if (activeIsStudy) "Study Session" else "Class"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        TextButton(onClick = {
                            MainActivity.activeSessionSubjectId.value   = null
                            MainActivity.activeSessionSubjectName.value = null
                            showDndBanner = false
                        }) {
                            Text("End", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Stat cards ────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudySpacing.space12)
            ) {
                StatCard(
                    modifier   = Modifier.weight(1f),
                    value      = "${activeSubjects.size}",
                    label      = "Active Subjects",
                    icon       = Icons.Default.Edit,
                    color      = MaterialTheme.colorScheme.primaryContainer,
                    textColor  = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatCard(
                    modifier   = Modifier.weight(1f),
                    value      = "${pendingAssessments.size}",
                    label      = if (overdueCount > 0) "$overdueCount overdue!" else "Pending Tasks",
                    icon       = if (overdueCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                    color      = if (overdueCount > 0) MaterialTheme.colorScheme.errorContainer
                                 else MaterialTheme.colorScheme.secondaryContainer,
                    textColor  = if (overdueCount > 0) MaterialTheme.colorScheme.onErrorContainer
                                 else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // ── Subjects ──────────────────────────────────────────────────────────
        item {
            SectionHeader(title = "Subjects", hint = "Tap to edit")
        }

        if (activeSubjects.isEmpty()) {
            item {
                DashboardEmptyState(
                    icon    = Icons.Default.Edit,
                    message = "No subjects yet.\nTap  +  at the bottom to add your first subject."
                )
            }
        } else {
            items(activeSubjects, key = { it.id }) { subject ->
                SubjectCard(
                    subject = subject,
                    onEdit  = { onNavigateToSubjectDetail(subject.id) },
                    onNotes = { onNavigateToNotes(subject.id) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }

        // ── Upcoming assessments ───────────────────────────────────────────────
        item {
            SectionHeader(title = "Upcoming Tasks", hint = null)
        }

        if (upcomingAssessments.isEmpty()) {
            item {
                DashboardEmptyState(
                    icon    = Icons.Default.CheckCircle,
                    message = "All caught up!\nNo upcoming tasks right now."
                )
            }
        } else {
            items(upcomingAssessments, key = { it.id }) { assessment ->
                val subject = subjects.find { it.id == assessment.subjectId }
                AssessmentMiniCard(assessment = assessment, subjectName = subject?.name)
            }
        }
    }
}

// ─── Stat card ────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(
    modifier:  Modifier = Modifier,
    value:     String,
    label:     String,
    icon:      ImageVector,
    color:     Color,
    textColor: Color
) {
    Surface(
        modifier = modifier,
        color    = color,
        shape    = StudyShapes.large,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(StudySpacing.space16)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    value,
                    style      = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color      = textColor
                )
                Icon(icon, contentDescription = null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.8f))
        }
    }
}

// ─── Section header ───────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, hint: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────
@Composable
private fun DashboardEmptyState(icon: ImageVector, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = StudyShapes.large,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(StudySpacing.space32),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StudySpacing.space8)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            }
            Text(
                message,
                style   = MaterialTheme.typography.bodyMedium,
                color   = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Subject card ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubjectCard(
    subject: Subject,
    onEdit:  () -> Unit,
    onNotes: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val color = remember(subject.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(subject.colorHex)) }
            .getOrDefault(Color(0xFF00668B))
    }

    with(sharedTransitionScope) {
        Surface(
            onClick    = onEdit,
            shape      = StudyShapes.large,
            tonalElevation = 2.dp,
            modifier   = Modifier
                .fillMaxWidth()
                .sharedBounds(
                    rememberSharedContentState(key = "subject_card_bg_${subject.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(color)
                )
                // Color dot
                Box(
                    modifier = Modifier
                        .padding(start = StudySpacing.space12)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = StudySpacing.space12, vertical = StudySpacing.space12),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.sharedBounds(
                            rememberSharedContentState(key = "subject_card_name_${subject.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                    val detail = buildString {
                        if (subject.classBlocks.isNotEmpty())
                            append("${subject.classBlocks.size} class${if (subject.classBlocks.size > 1) "es" else ""}")
                        if (subject.classBlocks.isNotEmpty() && subject.studyBlocks.isNotEmpty()) append(" · ")
                        if (subject.studyBlocks.isNotEmpty())
                            append("${subject.studyBlocks.size} session${if (subject.studyBlocks.size > 1) "s" else ""}")
                    }
                    if (detail.isNotEmpty()) {
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (subject.focusModeEnabled) {
                        Text("🔕 Quiet Time ON", style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
                // Notes icon button
                IconButton(onClick = onNotes) {
                    Icon(Icons.Default.Edit, contentDescription = "Open Notes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                // Chevron for edit
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Edit subject",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(end = StudySpacing.space8).size(20.dp)
                )
            }
        }
    }
}

// ─── Assessment mini card ─────────────────────────────────────────────────────
@Composable
private fun AssessmentMiniCard(assessment: Assessment, subjectName: String?) {
    val isOverdue = assessment.dueAt < System.currentTimeMillis()
    val typeColor = when (assessment.type) {
        AssessmentType.EXAM       -> MaterialTheme.colorScheme.error
        AssessmentType.QUIZ       -> MaterialTheme.colorScheme.tertiary
        AssessmentType.PROJECT    -> MaterialTheme.colorScheme.secondary
        AssessmentType.ASSIGNMENT -> MaterialTheme.colorScheme.primary
        AssessmentType.OTHER      -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape         = StudyShapes.large,
        tonalElevation = if (isOverdue) 0.dp else 2.dp,
        color         = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
        modifier      = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left type-color accent bar
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(typeColor))

            Row(
                modifier = Modifier.weight(1f).padding(StudySpacing.space12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StudySpacing.space12)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        assessment.title,
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (subjectName != null) {
                        Text(subjectName, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        assessment.type.name,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = typeColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isOverdue) "OVERDUE" else formatDate(assessment.dueAt),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun formatDate(time: Long): String =
    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(time))
