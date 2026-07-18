package dev.study.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.study.app.MainActivity
import dev.study.app.data.datastore.PreferencesManager

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_START_CLASS = "dev.study.app.ACTION_START_CLASS"
        const val ACTION_END_CLASS = "dev.study.app.ACTION_END_CLASS"
        const val ACTION_START_STUDY = "dev.study.app.ACTION_START_STUDY"
        const val ACTION_END_STUDY = "dev.study.app.ACTION_END_STUDY"
        const val ACTION_REMINDER_ASSESSMENT = "dev.study.app.ACTION_REMINDER_ASSESSMENT"

        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_SUBJECT_COLOR = "extra_subject_color"
        const val EXTRA_BLOCK_ID = "extra_block_id"
        const val EXTRA_FOCUS_MODE = "extra_focus_mode"

        const val EXTRA_ASSESSMENT_ID = "extra_assessment_id"
        const val EXTRA_ASSESSMENT_TITLE = "extra_assessment_title"
        const val EXTRA_ASSESSMENT_TYPE = "extra_assessment_type"
        const val EXTRA_ASSESSMENT_DUE = "extra_assessment_due"

        private const val CHANNEL_CLASS = "class_reminders"
        private const val CHANNEL_STUDY = "study_reminders"
        private const val CHANNEL_ASSESSMENT = "assessment_reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        createNotificationChannels(context)

        when (action) {
            ACTION_START_CLASS -> {
                val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: ""
                val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Subject"
                val focusMode = intent.getBooleanExtra(EXTRA_FOCUS_MODE, false)

                // Track active session globally
                MainActivity.activeSessionSubjectId.value = subjectId
                MainActivity.activeSessionSubjectName.value = subjectName
                MainActivity.activeSessionIsStudy.value = false

                if (focusMode) {
                    val workRequest = OneTimeWorkRequestBuilder<DndWorker>()
                        .setInputData(DndWorker.createInputData(enable = true))
                        .build()
                    WorkManager.getInstance(context.applicationContext).enqueue(workRequest)
                }

                showNotification(
                    context,
                    CHANNEL_CLASS,
                    101,
                    "Class Started: $subjectName",
                    "Your class session has begun."
                )
            }
            ACTION_END_CLASS -> {
                val focusMode = intent.getBooleanExtra(EXTRA_FOCUS_MODE, false)
                
                MainActivity.activeSessionSubjectId.value = null
                MainActivity.activeSessionSubjectName.value = null

                if (focusMode) {
                    val workRequest = OneTimeWorkRequestBuilder<DndWorker>()
                        .setInputData(DndWorker.createInputData(enable = false))
                        .build()
                    WorkManager.getInstance(context.applicationContext).enqueue(workRequest)
                }

                showNotification(
                    context,
                    CHANNEL_CLASS,
                    102,
                    "Class Ended",
                    "Your class session has finished."
                )
            }
            ACTION_START_STUDY -> {
                val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: ""
                val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Subject"
                val focusMode = intent.getBooleanExtra(EXTRA_FOCUS_MODE, false)

                MainActivity.activeSessionSubjectId.value = subjectId
                MainActivity.activeSessionSubjectName.value = subjectName
                MainActivity.activeSessionIsStudy.value = true

                if (focusMode) {
                    val workRequest = OneTimeWorkRequestBuilder<DndWorker>()
                        .setInputData(DndWorker.createInputData(enable = true))
                        .build()
                    WorkManager.getInstance(context.applicationContext).enqueue(workRequest)
                }

                showNotification(
                    context,
                    CHANNEL_STUDY,
                    201,
                    "Study Session Started: $subjectName",
                    "Time to focus on studying!"
                )
            }
            ACTION_END_STUDY -> {
                val focusMode = intent.getBooleanExtra(EXTRA_FOCUS_MODE, false)
                
                MainActivity.activeSessionSubjectId.value = null
                MainActivity.activeSessionSubjectName.value = null

                if (focusMode) {
                    val workRequest = OneTimeWorkRequestBuilder<DndWorker>()
                        .setInputData(DndWorker.createInputData(enable = false))
                        .build()
                    WorkManager.getInstance(context.applicationContext).enqueue(workRequest)
                }

                showNotification(
                    context,
                    CHANNEL_STUDY,
                    202,
                    "Study Session Ended",
                    "Well done! Your study session is complete."
                )
            }
            ACTION_REMINDER_ASSESSMENT -> {
                val title = intent.getStringExtra(EXTRA_ASSESSMENT_TITLE) ?: "Assessment"
                val type = intent.getStringExtra(EXTRA_ASSESSMENT_TYPE) ?: "Task"
                showNotification(
                    context,
                    CHANNEL_ASSESSMENT,
                    title.hashCode(),
                    "Upcoming $type Reminder",
                    "\"$title\" is due soon!"
                )
            }
        }
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        content: String
    ) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val classChannel = NotificationChannel(
                CHANNEL_CLASS,
                "Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when classes start/end"
            }

            val studyChannel = NotificationChannel(
                CHANNEL_STUDY,
                "Study Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when study blocks start/end"
            }

            val assessmentChannel = NotificationChannel(
                CHANNEL_ASSESSMENT,
                "Assessment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you of upcoming exams and quizzes"
            }

            notificationManager.createNotificationChannel(classChannel)
            notificationManager.createNotificationChannel(studyChannel)
            notificationManager.createNotificationChannel(assessmentChannel)
        }
    }
}
