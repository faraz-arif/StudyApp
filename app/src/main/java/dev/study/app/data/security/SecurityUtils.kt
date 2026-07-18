package dev.study.app.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.SecureRandom

object SecurityUtils {
    private const val SECURE_PREFS_FILE = "secure_study_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    @Synchronized
    fun getDatabasePassphrase(context: Context): ByteArray {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            SECURE_PREFS_FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var passphrase = sharedPreferences.getString(KEY_DB_PASSPHRASE, null)
        if (passphrase == null) {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            passphrase = android.util.Base64.encodeToString(randomBytes, android.util.Base64.NO_WRAP)
            sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
        }
        return android.util.Base64.decode(passphrase, android.util.Base64.NO_WRAP)
    }
}
