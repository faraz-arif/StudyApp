package dev.study.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.study.app.data.local.AppDatabase

class ScheduleAlarmsWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val subjects = database.subjectDao().getAllSubjectsRaw().map { it.toDomain() }
            val assessments = database.assessmentDao().getAllAssessmentsRaw().map { it.toDomain() }
            
            val alarmScheduler = AlarmScheduler(applicationContext)
            alarmScheduler.scheduleAlarms(subjects, assessments)
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
