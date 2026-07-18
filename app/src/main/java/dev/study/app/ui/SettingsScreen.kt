package dev.study.app.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.study.app.data.datastore.PreferencesManager
import dev.study.app.ui.theme.ColorPreset
import dev.study.app.ui.theme.PresetSwatchColors
import dev.study.app.ui.theme.StudyShapes
import dev.study.app.ui.theme.StudySpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: PreferencesManager,
    onNavigateBack: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme  by prefs.themeModeFlow.collectAsState(initial = "SYSTEM")
    val currentPreset by prefs.colorPresetFlow.collectAsState(initial = "OCEAN")
    val currentDnd    by prefs.dndFilterFlow.collectAsState(initial = "PRIORITY")
    val currentFont   by prefs.fontFamilyFlow.collectAsState(initial = "DEFAULT")
    val scope = rememberCoroutineScope()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val dndGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        notificationManager.isNotificationPolicyAccessGranted
    else true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StudySpacing.space16)
            .padding(top = StudySpacing.space20, bottom = StudySpacing.space32),
        verticalArrangement = Arrangement.spacedBy(StudySpacing.space16)
    ) {
        // Page title
        Text(
            "App Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        // ── Appearance ────────────────────────────────────────────────────────
        SettingsSection(title = "Appearance", icon = Icons.Default.Edit) {

            // Theme — segmented control
            SettingsLabel("Theme")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = currentTheme == "LIGHT",
                    onClick  = { scope.launch { prefs.setThemeMode("LIGHT") } },
                    shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text("☀ Light", style = MaterialTheme.typography.labelLarge) }

                SegmentedButton(
                    selected = currentTheme == "SYSTEM",
                    onClick  = { scope.launch { prefs.setThemeMode("SYSTEM") } },
                    shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text("⚙ Auto", style = MaterialTheme.typography.labelLarge) }

                SegmentedButton(
                    selected = currentTheme == "DARK",
                    onClick  = { scope.launch { prefs.setThemeMode("DARK") } },
                    shape    = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text("🌙 Dark", style = MaterialTheme.typography.labelLarge) }
            }

            Spacer(Modifier.height(StudySpacing.space8))

            // Color palette — circular animated swatches
            SettingsLabel("Color Theme")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(StudySpacing.space16)
            ) {
                val presetLabels = mapOf(
                    ColorPreset.OCEAN      to "Ocean",
                    ColorPreset.FOREST     to "Forest",
                    ColorPreset.SUNSET     to "Sunset",
                    ColorPreset.MONOCHROME to "Mono",
                    ColorPreset.SLATE      to "Slate"
                )
                ColorPreset.entries.forEach { preset ->
                    val swatchColor = PresetSwatchColors[preset] ?: MaterialTheme.colorScheme.primary
                    val label       = presetLabels[preset] ?: preset.name
                    ColorSwatch(
                        color    = swatchColor,
                        label    = label,
                        selected = currentPreset == preset.name,
                        onClick  = { scope.launch { prefs.setColorPreset(preset.name) } }
                    )
                }
            }

            Spacer(Modifier.height(StudySpacing.space8))

            // Font Family — segmented control
            SettingsLabel("App Typography")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val fonts = listOf("DEFAULT" to "System", "INTER" to "Inter", "OUTFIT" to "Outfit", "ROBOTO" to "Roboto")
                fonts.forEachIndexed { idx, pair ->
                    SegmentedButton(
                        selected = currentFont == pair.first,
                        onClick  = { scope.launch { prefs.setFontFamily(pair.first) } },
                        shape    = SegmentedButtonDefaults.itemShape(index = idx, count = fonts.size)
                    ) { Text(pair.second, style = MaterialTheme.typography.labelLarge) }
                }
            }
        }

        // ── Quiet Time ────────────────────────────────────────────────────────
        SettingsSection(title = "Quiet Time", icon = Icons.Default.Notifications) {
            if (dndGranted) {
                Surface(
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    shape    = StudyShapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier  = Modifier.padding(StudySpacing.space12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StudySpacing.space8)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Permission granted. Your phone will go quiet during class and study sessions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Surface(
                    color    = MaterialTheme.colorScheme.errorContainer,
                    shape    = StudyShapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(StudySpacing.space12),
                        verticalArrangement = Arrangement.spacedBy(StudySpacing.space8)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(StudySpacing.space8)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp)
                            )
                            Text(
                                "Quiet Time needs permission to temporarily silence your phone's notifications.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    )
                                }
                            },
                            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Allow Access") }
                    }
                }
            }

            Spacer(Modifier.height(StudySpacing.space4))
            SettingsLabel("Block level during study")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudySpacing.space8)
            ) {
                FilterChip(
                    selected = currentDnd == "PRIORITY",
                    onClick  = { scope.launch { prefs.setDndFilter("PRIORITY") } },
                    label    = { Text("Allow Important Calls") },
                    shape    = StudyShapes.medium,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = currentDnd == "SILENCE",
                    onClick  = { scope.launch { prefs.setDndFilter("SILENCE") } },
                    label    = { Text("Silence Everything") },
                    shape    = StudyShapes.medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Save & Recover ────────────────────────────────────────────────────
        SettingsSection(title = "Save & Recover Data", icon = Icons.Default.Settings) {
            Text(
                "Save all your subjects, notes, and tasks to a backup file. Restore anytime if you change phones.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(StudySpacing.space4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StudySpacing.space12)
            ) {
                FilledTonalButton(
                    onClick  = onExportBackup,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = StudyShapes.medium
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Backup", fontWeight = FontWeight.SemiBold)
                }
                FilledTonalButton(
                    onClick  = onImportBackup,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = StudyShapes.medium
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restore", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Footer
        Text(
            "Study Planner · v1.0.0 · Completely Offline",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StudySpacing.space8)
            ) {
                Surface(
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    shape    = StudyShapes.small,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            content()
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text,
        style      = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ColorSwatch(
    color:    Color,
    label:    String,
    selected: Boolean,
    onClick:  () -> Unit
) {
    val ringWidth by animateDpAsState(
        targetValue   = if (selected) 3.dp else 0.dp,
        animationSpec = tween(200),
        label         = "swatchRing"
    )
    val ringColor by animateColorAsState(
        targetValue   = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
        animationSpec = tween(200),
        label         = "swatchRingColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color)
                .border(ringWidth, ringColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint     = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color      = if (selected) MaterialTheme.colorScheme.onBackground
                         else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
