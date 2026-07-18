package dev.study.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.study.app.data.backup.BackupRestoreManager
import dev.study.app.data.datastore.PreferencesManager
import dev.study.app.data.local.AppDatabase
import dev.study.app.data.local.AssessmentEntity
import dev.study.app.data.local.NoteEntity
import dev.study.app.data.local.SubjectEntity
import dev.study.app.notifications.AlarmScheduler
import dev.study.app.ui.AssessmentsScreen
import dev.study.app.ui.DashboardScreen
import dev.study.app.ui.NotesScreen
import dev.study.app.ui.SettingsScreen
import dev.study.app.ui.SubjectDetailScreen
import dev.study.app.ui.theme.ColorPreset
import dev.study.app.ui.theme.getThemeColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        val activeSessionSubjectId = MutableStateFlow<String?>(null)
        val activeSessionSubjectName = MutableStateFlow<String?>(null)
        val activeSessionIsStudy = MutableStateFlow(false)
    }

    private lateinit var database: AppDatabase
    private lateinit var prefsManager: PreferencesManager
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var backupRestoreManager: BackupRestoreManager

    private fun triggerAlarmScheduling() {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<dev.study.app.notifications.ScheduleAlarmsWorker>().build()
        androidx.work.WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set highest available refresh rate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val highestRate = display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: 0f
                if (highestRate > 0f) {
                    val lp = window.attributes
                    lp.javaClass.getField("preferredMaxDisplayRefreshRate").set(lp, highestRate)
                    lp.javaClass.getField("preferredMinDisplayRefreshRate").set(lp, highestRate)
                    window.attributes = lp
                }
            } catch (_: Exception) { }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val highestMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                if (highestMode != null) {
                    window.attributes = window.attributes.also {
                        it.preferredDisplayModeId = highestMode.modeId
                        it.preferredRefreshRate = highestMode.refreshRate
                    }
                }
            } catch (_: Exception) { }
        }

        database = AppDatabase.getDatabase(this)
        prefsManager = PreferencesManager(this)
        alarmScheduler = AlarmScheduler(this)
        backupRestoreManager = BackupRestoreManager(this, database)

        setContent {
            val themeMode by prefsManager.themeModeFlow.collectAsState(initial = "SYSTEM")
            val colorPresetStr by prefsManager.colorPresetFlow.collectAsState(initial = "OCEAN")
            val fontFamilyStr by prefsManager.fontFamilyFlow.collectAsState(initial = "DEFAULT")

            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            val preset = try {
                ColorPreset.valueOf(colorPresetStr)
            } catch (_: Exception) {
                ColorPreset.OCEAN
            }

            MaterialTheme(
                colorScheme = getThemeColorScheme(preset, isDarkTheme),
                typography = dev.study.app.ui.theme.getThemeTypography(fontFamilyStr)
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StudyAppNavigation()
                }
            }
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    fun StudyAppNavigation() {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val subjects by database.subjectDao().getAllSubjects().collectAsState(initial = emptyList())
        val assessments by database.assessmentDao().getAllAssessmentsFlow().collectAsState(initial = emptyList())

        val exportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let {
                scope.launch {
                    contentResolver.openOutputStream(it)?.let { stream ->
                        backupRestoreManager.exportData(stream)
                    }
                }
            }
        }

        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                scope.launch {
                    contentResolver.openInputStream(it)?.let { stream ->
                        val result = backupRestoreManager.restoreData(stream)
                        if (result.isSuccess) {
                            triggerAlarmScheduling()
                        }
                    }
                }
            }
        }

        val topLevelRoutes = listOf("dashboard", "assessments", "settings")
        val showBottomBar = currentRoute in topLevelRoutes
        val showFab = currentRoute == "dashboard"

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == "dashboard",
                            onClick = {
                                navController.navigate("dashboard") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "assessments",
                            onClick = {
                                navController.navigate("assessments") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "Assessments") },
                            label = { Text("Tasks") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                navController.navigate("settings") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (showFab) {
                    FloatingActionButton(
                        onClick = { navController.navigate("subject_detail/new") }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subject")
                    }
                }
            }
        ) { paddingValues ->
            SharedTransitionLayout {
                NavHost(
                    navController    = navController,
                    startDestination = "dashboard",
                    modifier         = Modifier.padding(paddingValues),
                    enterTransition  = { fadeIn() + slideInVertically { it / 6 } },
                    exitTransition   = { fadeOut() + slideOutVertically { -it / 6 } },
                    popEnterTransition  = { fadeIn() + slideInVertically { -it / 6 } },
                    popExitTransition   = { fadeOut() + slideOutVertically { it / 6 } }
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                            subjects = subjects.map { it.toDomain() },
                            assessments = assessments.map { it.toDomain() },
                            onNavigateToSubjectDetail = { id -> navController.navigate("subject_detail/$id") },
                            onNavigateToNotes = { subjectId -> navController.navigate("notes/$subjectId") },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }

                    composable("subject_detail/{subjectId}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("subjectId")
                        val cleanId = if (id == "new") null else id
                        SubjectDetailScreen(
                            subjectId = cleanId,
                            subjects = subjects.map { it.toDomain() },
                            onSaveSubject = { subject ->
                                scope.launch {
                                    database.subjectDao().insertSubject(SubjectEntity.fromDomain(subject))
                                    triggerAlarmScheduling()
                                }
                            },
                            onArchiveSubject = { subjectId ->
                                scope.launch {
                                    database.subjectDao().archiveSubject(subjectId)
                                    triggerAlarmScheduling()
                                }
                            },
                            onNavigateBack = { navController.popBackStack() },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }

                    composable("assessments") {
                        AssessmentsScreen(
                            subjects = subjects.map { it.toDomain() },
                            onSaveAssessment = { assessment ->
                                scope.launch {
                                    database.assessmentDao().insertAssessment(AssessmentEntity.fromDomain(assessment))
                                    triggerAlarmScheduling()
                                }
                            },
                            onDeleteAssessment = { id ->
                                scope.launch {
                                    database.assessmentDao().deleteAssessment(id)
                                    triggerAlarmScheduling()
                                }
                            },
                            onLoadAssessmentsPage = { offset, limit ->
                                database.assessmentDao().getAllAssessmentsPaged(limit, offset).map { it.toDomain() }
                            }
                        )
                    }

                    composable("notes/{subjectId}") { backStackEntry ->
                        val subjectId    = backStackEntry.arguments?.getString("subjectId") ?: ""
                        val subject      = subjects.find { it.id == subjectId }

                        NotesScreen(
                            subjectId       = subjectId,
                            subjectName     = subject?.name ?: "Notes",
                            subjectColorHex = subject?.colorHex ?: "#00668B",
                            onSaveNote      = { note ->
                                scope.launch { database.noteDao().insertNote(NoteEntity.fromDomain(note)) }
                            },
                            onDeleteNote    = { id ->
                                scope.launch { database.noteDao().deleteNote(id) }
                            },
                            onNavigateBack  = { navController.popBackStack() },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable,
                            onLoadNotesPage = { offset, limit ->
                                database.noteDao().getNotesForSubjectPaged(subjectId, limit, offset).map { it.toDomain() }
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            prefs = prefsManager,
                            onNavigateBack = { navController.popBackStack() },
                            onExportBackup = {
                                exportLauncher.launch("study_backup_${System.currentTimeMillis()}.json")
                            },
                            onImportBackup = {
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                    }
                }
            }
        }
    }
}
