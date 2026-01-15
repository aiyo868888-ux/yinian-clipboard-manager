package com.yinian.clipboard.floatingwindow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * FloatingWindowManager 单元测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FloatingWindowManagerTest {

    private lateinit var context: Context
    private lateinit var manager: FloatingWindowManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        manager = FloatingWindowManager(context)
    }

    @Test
    fun `hasPermission returns false by default in Robolectric`() {
        // Robolectric environment doesn't have overlay permission by default
        val hasPermission = manager.hasPermission()
        assertThat(hasPermission).isFalse()
    }

    @Test
    fun `requestPermission returns correct intent`() {
        val intent = manager.requestPermission()

        assertThat(intent).isNotNull()
        assertThat(intent.action).isEqualTo(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        assertThat(intent.data).isEqualTo(
            Uri.parse("package:${context.packageName}")
        )
    }

    @Test
    fun `showFloatingWindow throws exception without permission`() {
        try {
            manager.showFloatingWindow()
            // Should have thrown exception
            assertThat(true).isFalse()
        } catch (e: SecurityException) {
            assertThat(e.message).contains("权限")
        }
    }

    @Test
    fun `isShowing returns false initially`() {
        val isShowing = manager.isShowing()
        assertThat(isShowing).isFalse()
    }

    @Test
    fun `toggleFloatingWindow without permission does nothing`() {
        val initialShowing = manager.isShowing()
        manager.toggleFloatingWindow()
        val afterToggle = manager.isShowing()

        // Should remain false since no permission
        assertThat(initialShowing).isFalse()
        assertThat(afterToggle).isFalse()
    }
}
