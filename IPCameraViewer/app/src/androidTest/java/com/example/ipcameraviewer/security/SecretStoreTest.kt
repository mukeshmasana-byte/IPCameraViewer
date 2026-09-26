package com.example.ipcameraviewer.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.example.ipcameraviewer.database.CameraSecrets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecretStoreTest {
    @Test fun roundTripsSecretsAndStoresOnlyCiphertextInPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SecretStore(context)
        val id = "test-${System.nanoTime()}"
        val expected = CameraSecrets("rtsp://viewer:secret@camera.local/live", null, "http://camera.local/onvif", "viewer", "secret")
        store.put(id, expected)
        val persisted = context.getSharedPreferences("camera_secrets", Context.MODE_PRIVATE).getString("$id.data", "")
        assertFalse(persisted.orEmpty().contains("secret"))
        assertEquals(expected, store.get(id))
        store.remove(id)
    }
}
