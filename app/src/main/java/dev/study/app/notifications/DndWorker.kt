package dev.study.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.study.app.data.datastore.PreferencesManager
import kotlinx.coroutines.flow.first

class DndWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_ENABLE = "enable"
        const val ACTION_DISABLE = "disable"
        
        fun createInputData(enable: Boolean): androidx.work.Data {
            return workDataOf(
                KEY_ACTION to if (enable) ACTION_ENABLE else ACTION_DISABLE
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val action = inputData.getString(KEY_ACTION) ?: return Result.failure()
            val dndController = DndController(applicationContext)
            val prefs = PreferencesManager(applicationContext)
            
            if (action == ACTION_ENABLE) {
                val filter = prefs.dndFilterFlow.first()
                dndController.enableDnd(filter)
            } else {
                dndController.disableDnd()
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
