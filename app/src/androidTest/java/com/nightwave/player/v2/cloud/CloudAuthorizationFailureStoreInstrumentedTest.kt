package com.nightwave.player.v2.cloud

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Dedicated, suffixed preference namespace; installed account is untouched. */
@RunWith(AndroidJUnit4::class)
class CloudAuthorizationFailureStoreInstrumentedTest {
    private class Sandbox(base: Context, private val suffix: String) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences(name + suffix, mode)
    }

    @Test fun onlyNumericCodeAndPublicStageAreStoredAndReported() {
        val base: Context = ApplicationProvider.getApplicationContext()
        val sandbox = Sandbox(base, "_nw45_" + UUID.randomUUID())
        val preferences = sandbox.getSharedPreferences(
            "nightwave_cloud_auth_diagnostic_v1", Context.MODE_PRIVATE)
        try {
            val store = CloudAuthorizationFailureStore(sandbox)
            assertNull(store.read())
            assertTrue(store.record(CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER, 8))
            val failure = store.read()!!
            assertEquals(8, failure.code)
            assertEquals(CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER, failure.stage)
            assertEquals(setOf("stage", "status_code"), preferences.all.keys)
            val publicReport = InstalledApkOAuthIdentity.report(sandbox)
            assertTrue(publicReport.contains("Last failure status: 8"))
            assertTrue(publicReport.contains("After Google account picker"))
            assertFalse(publicReport.contains("Bearer "))
            assertTrue(store.clear())
            assertNull(store.read())
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test fun invalidCodesAreNotSavedAsTrustedGoogleStatuses() {
        val base: Context = ApplicationProvider.getApplicationContext()
        val sandbox = Sandbox(base, "_nw45_" + UUID.randomUUID())
        val preferences = sandbox.getSharedPreferences(
            "nightwave_cloud_auth_diagnostic_v1", Context.MODE_PRIVATE)
        try {
            val store = CloudAuthorizationFailureStore(sandbox)
            store.record(CloudAuthorizationFailurePolicy.Stage.REQUEST, -100)
            assertNull(store.read()!!.code)
            store.record(CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK, 1234567)
            assertNull(store.read()!!.code)
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
