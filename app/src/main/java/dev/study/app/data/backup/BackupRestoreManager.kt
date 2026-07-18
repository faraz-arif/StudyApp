package dev.study.app.data.backup

import android.content.Context
import dev.study.app.data.local.AppDatabase
import dev.study.app.data.local.AssessmentEntity
import dev.study.app.data.local.NoteEntity
import dev.study.app.data.local.SubjectEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val subjects: List<SubjectEntity>,
    val assessments: List<AssessmentEntity>,
    val notes: List<NoteEntity>
)

object BackupCrypto {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    
    // Salt and passphrase for backup encryption key derivation
    private val SALT = byteArrayOf(
        0x53, 0x74, 0x75, 0x64, 0x79, 0x50, 0x6c, 0x61,
        0x6e, 0x6e, 0x65, 0x72, 0x53, 0x65, 0x63, 0x75
    )
    private const val PASSPHRASE = "StudyAppBackupEncryptionPassphraseSecuredSalt"
    
    private fun getSecretKey(): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(PASSPHRASE.toCharArray(), SALT, 65536, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, ALGORITHM)
    }

    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun decrypt(data: ByteArray): ByteArray {
        if (data.size < 12) throw IllegalArgumentException("Invalid backup file: Payload too small.")
        val iv = data.sliceArray(0 until 12)
        val encrypted = data.sliceArray(12 until data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(encrypted)
    }
}

class BackupRestoreManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun exportData(outputStream: OutputStream): Result<Unit> {
        return try {
            val subjects = database.subjectDao().getAllSubjectsRaw()
            val assessments = database.assessmentDao().getAllAssessmentsRaw()
            val notes = database.noteDao().getAllNotesRaw()

            val payload = BackupPayload(
                schemaVersion = 1,
                subjects = subjects,
                assessments = assessments,
                notes = notes
            )
            val jsonString = json.encodeToString(payload)
            val encryptedBytes = BackupCrypto.encrypt(jsonString.toByteArray(Charsets.UTF_8))
            outputStream.write(encryptedBytes)
            outputStream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { outputStream.close() } catch (ignored: Exception) {}
        }
    }

    suspend fun restoreData(inputStream: InputStream): Result<Unit> {
        return try {
            val encryptedBytes = inputStream.readBytes()
            val decryptedBytes = BackupCrypto.decrypt(encryptedBytes)
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val payload = json.decodeFromString<BackupPayload>(jsonString)

            if (payload.schemaVersion < 1) {
                return Result.failure(Exception("Unsupported backup schema version: ${payload.schemaVersion}"))
            }

            // Transaction restoration
            database.runInTransaction {
                // Clear existing
                database.clearAllTables()

                // Insert restored
                payload.subjects.forEach {
                    database.openHelper.writableDatabase.execSQL(
                        "INSERT OR REPLACE INTO subjects (id, name, colorHex, classBlocks, studyBlocks, focusModeEnabled, archived, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(it.id, it.name, it.colorHex, json.encodeToString(it.classBlocks), json.encodeToString(it.studyBlocks), if (it.focusModeEnabled) 1 else 0, if (it.archived) 1 else 0, it.createdAt)
                    )
                }

                payload.assessments.forEach {
                    database.openHelper.writableDatabase.execSQL(
                        "INSERT OR REPLACE INTO assessments (id, subjectId, type, title, dueAt, notes, reminderOffsetsMinutes, completed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(it.id, it.subjectId, it.type.name, it.title, it.dueAt, it.notes, json.encodeToString(it.reminderOffsetsMinutes), if (it.completed) 1 else 0)
                    )
                }

                payload.notes.forEach {
                    database.openHelper.writableDatabase.execSQL(
                        "INSERT OR REPLACE INTO notes (id, subjectId, title, contentJson, updatedAt) VALUES (?, ?, ?, ?, ?)",
                        arrayOf(it.id, it.subjectId, it.title, it.contentJson, it.updatedAt)
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { inputStream.close() } catch (ignored: Exception) {}
        }
    }
}
