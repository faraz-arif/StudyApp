package dev.study.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val workRequest = OneTimeWorkRequestBuilder<ScheduleAlarmsWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueue(workRequest)
        }
    }
}
