package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.FirebaseConnectionState
import com.example.data.firebase.FirebaseService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseServiceTest {

    private lateinit var context: Context
    private lateinit var firebaseService: FirebaseService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        firebaseService = FirebaseService.getInstance(context)
    }

    @Test
    fun testFirebaseServiceSingletonAndDefaultProject() {
        assertNotNull(firebaseService)
        val projectId = firebaseService.getProjectId()
        assertTrue(projectId.isNotEmpty())
    }

    @Test
    fun testFirebaseAuthFlowAndSignOut() = runBlocking {
        assertNotNull(firebaseService.currentUserFlow)
        firebaseService.signOut()
        assertEquals(null, firebaseService.currentUserFlow.value)
    }

    @Test
    fun testFirebaseConfigUpdate() = runBlocking {
        val testProject = "test-printer-kiosk"
        val testApiKey = "AIzaSyTestApiKey123"
        val testAppId = "1:123456789:android:testkiosk"

        val result = firebaseService.updateFirebaseConfig(testProject, testApiKey, testAppId)
        assertTrue(result.isSuccess)
        assertEquals(testProject, firebaseService.getProjectId())
        assertEquals(testApiKey, firebaseService.getApiKey())
        assertEquals(testAppId, firebaseService.getAppId())

        val state = firebaseService.connectionState.value
        assertTrue(state is FirebaseConnectionState.Connected)
        if (state is FirebaseConnectionState.Connected) {
            assertEquals(testProject, state.projectId)
        }
    }
}
