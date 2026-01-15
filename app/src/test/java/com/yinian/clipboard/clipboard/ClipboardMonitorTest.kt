package com.yinian.clipboard.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * ClipboardMonitor 单元测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ClipboardMonitorTest {

    private lateinit var clipboardMonitor: ClipboardMonitor

    @Mock
    private lateinit var mockClipboardManager: ClipboardManager

    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        // 由于ClipboardMonitor依赖系统服务，这里使用实际context进行集成测试
        clipboardMonitor = ClipboardMonitor(context)
    }

    @Test
    fun `copyToClipboard with text updates system clipboard`() {
        // Given
        val data = ClipboardData(
            type = ClipboardDataType.TEXT,
            textContent = "Test content"
        )

        // When
        clipboardMonitor.copyToClipboard(data)

        // Then
        val current = clipboardMonitor.getCurrentClip()
        assertThat(current).isNotNull()
        assertThat(current?.textContent).isEqualTo("Test content")
    }

    @Test
    fun `getCurrentClip returns null when clipboard is empty`() {
        // Given - empty clipboard

        // When
        val current = clipboardMonitor.getCurrentClip()

        // Then
        assertThat(current).isNull()
    }

    @Test
    fun `getCurrentClip returns text when text is copied`() {
        // Given
        val data = ClipboardData(
            type = ClipboardDataType.TEXT,
            textContent = "Hello World"
        )
        clipboardMonitor.copyToClipboard(data)

        // When
        val current = clipboardMonitor.getCurrentClip()

        // Then
        assertThat(current).isNotNull()
        assertThat(current?.type).isEqualTo(ClipboardDataType.TEXT)
        assertThat(current?.textContent).isEqualTo("Hello World")
    }

    @Test
    fun `getCurrentClip handles HTML content`() {
        // Given
        val data = ClipboardData(
            type = ClipboardDataType.HTML,
            textContent = "Plain text",
            htmlContent = "<b>Bold text</b>"
        )
        clipboardMonitor.copyToClipboard(data)

        // When
        val current = clipboardMonitor.getCurrentClip()

        // Then
        assertThat(current).isNotNull()
        assertThat(current?.type).isEqualTo(ClipboardDataType.HTML)
        assertThat(current?.textContent).isEqualTo("Plain text")
    }

    @Test
    fun `watchClipboard emits data when clipboard changes`() = runTest {
        // Given
        val data = ClipboardData(
            type = ClipboardDataType.TEXT,
            textContent = "New content"
        )

        val collected = mutableListOf<ClipboardData>()
        val job = clipboardMonitor.watchClipboard()
            .take(1)
            .collect { collected.add(it) }

        // When
        clipboardMonitor.copyToClipboard(data)
        advanceUntilIdle()

        // Then
        assertThat(collected).isNotEmpty()
        assertThat(collected[0].textContent).isEqualTo("New content")
    }
}
