package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ActionType
import com.example.data.security.SecureStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches JARVIS AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS AI", appName)
    }

    @Test
    fun `verify secure storage masking`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = SecureStorage(context)
        val masked = storage.maskApiKey("AIzaSyB1234567890XYZ")
        assertTrue(masked.startsWith("AIzaSy"))
        assertTrue(masked.endsWith("0XYZ"))
        assertTrue(masked.contains("••••"))
    }

    @Test
    fun `verify action types exist`() {
        assertNotNull(ActionType.OPEN_APP)
        assertNotNull(ActionType.START_CALL)
        assertNotNull(ActionType.PREPARE_MESSAGE)
        assertNotNull(ActionType.READ_VISIBLE_SCREEN)
        assertNotNull(ActionType.STOP_TASK)
    }
}
