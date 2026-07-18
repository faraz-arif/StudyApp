package dev.study.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.study.app.data.security.SecurityUtils
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        SubjectEntity::class,
        AssessmentEntity::class,
        NoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbFile = context.getDatabasePath("study_database")
                if (dbFile.exists()) {
                    try {
                        val header = java.io.FileInputStream(dbFile).use { input ->
                            val bytes = ByteArray(16)
                            input.read(bytes)
                            String(bytes, java.nio.charset.StandardCharsets.US_ASCII)
                        }
                        if (header.startsWith("SQLite format 3")) {
                            context.deleteDatabase("study_database")
                        }
                    } catch (_: Exception) {}
                }

                System.loadLibrary("sqlcipher")
                val passphrase = SecurityUtils.getDatabasePassphrase(context)
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_database"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
