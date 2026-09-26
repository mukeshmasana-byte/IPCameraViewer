package com.example.ipcameraviewer.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.ipcameraviewer.database.CameraSecrets
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES/GCM-encrypted camera stream secrets, with the key held in Android Keystore. */
class SecretStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("camera_secrets", Context.MODE_PRIVATE)

    @Synchronized
    fun put(cameraId: String, secrets: CameraSecrets) {
        val payload = JSONObject()
            .put("streamUrl", secrets.streamUrl)
            .put("httpUrl", secrets.httpUrl)
            .put("onvifEndpoint", secrets.onvifEndpoint)
            .put("ptzEndpoint", secrets.ptzEndpoint)
            .put("username", secrets.username)
            .put("password", secrets.password)
            .put("subStreamUrl", secrets.subStreamUrl)
            .toString().toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        val encrypted = cipher.doFinal(payload)
        val saved = preferences.edit()
            .putString("$cameraId.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("$cameraId.data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .commit()
        check(saved) { "Could not save encrypted camera details." }
    }

    @Synchronized
    fun get(cameraId: String): CameraSecrets? = runCatching {
        val iv = preferences.getString("$cameraId.iv", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val encrypted = preferences.getString("$cameraId.data", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        }
        val json = JSONObject(String(cipher.doFinal(encrypted), StandardCharsets.UTF_8))
        CameraSecrets(
            streamUrl = json.optString("streamUrl"),
            httpUrl = json.optNullableString("httpUrl"),
            onvifEndpoint = json.optNullableString("onvifEndpoint"),
            ptzEndpoint = json.optNullableString("ptzEndpoint"),
            username = json.optNullableString("username"),
            password = json.optNullableString("password"),
            subStreamUrl = json.optNullableString("subStreamUrl"),
        )
    }.getOrNull()

    @Synchronized
    fun remove(cameraId: String) {
        preferences.edit().remove("$cameraId.iv").remove("$cameraId.data").commit()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? java.security.KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
            generateKey()
        }
    }

    private fun JSONObject.optNullableString(key: String): String? = if (isNull(key)) null else optString(key).takeIf(String::isNotEmpty)

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "ip_camera_viewer_secrets_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
