package com.yinian.clipboard.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import com.yinian.clipboard.R
import timber.log.Timber
import android.os.Handler
import android.os.Looper

/**
 * 空输入法服务 - 用于获取剪贴板访问权限
 *
 * 原理：Android 系统允许输入法随时访问剪贴板
 * 我们创建一个空输入法，激活后立即切换回上一个输入法，实现并行使用
 */
class ClipboardInputMethodService : InputMethodService() {

    companion object {
        // 剪贴板内容回调
        var onClipboardContent: ((String) -> Unit)? = null

        // 单例实例
        private var instance: ClipboardInputMethodService? = null

        // 最后缓存的剪贴板内容
        var cachedText: String? = null
            private set

        fun getInstance(): ClipboardInputMethodService? = instance

        // 设置剪贴板监听
        fun setClipboardListener(listener: (String) -> Unit) {
            onClipboardContent = listener
        }

        // 主动读取剪贴板（供外部调用）
        fun readClipboardNow(): String? {
            // 优先尝试从实例读取
            val text = instance?.readClipboard(forceRefresh = true)
            if (text != null) {
                return text
            }
            // 如果实例已被销毁，返回缓存的内容
            Timber.w("⚠️ 输入法实例已销毁，返回缓存内容: ${cachedText?.take(30)}")
            return cachedText
        }

        // 显示输入法选择器（供外部调用）
        fun showInputPicker(context: Context) {
            try {
                val imeManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imeManager.showInputMethodPicker()
                Timber.i("✅ 已显示输入法选择器")
            } catch (e: Exception) {
                Timber.e(e, "❌ 显示输入法选择器失败")
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val clipboardRunnable = object : Runnable {
        override fun run() {
            readClipboard()
            // 每秒检查一次
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.i("✅ 输入法服务已创建")

        // 启动定时监听
        handler.post(clipboardRunnable)
    }

    private var keyboardView: View? = null
    private var tvClipboardPreview: TextView? = null

    override fun onCreateInputView(): View {
        keyboardView = LayoutInflater.from(this).inflate(R.layout.clipboard_keyboard, null)

        // 切换输入法按钮
        keyboardView?.findViewById<Button>(R.id.btn_switch_ime)?.setOnClickListener {
            Timber.i("🔄 用户点击切换输入法按钮")
            switchToPreviousIme()
        }

        // 剪贴板预览
        tvClipboardPreview = keyboardView?.findViewById(R.id.tv_clipboard_preview)
        updateClipboardPreview()

        Timber.i("✅ 键盘视图已创建")
        return keyboardView!!
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Timber.i("✅ 输入法已激活")

        // 读取剪贴板并缓存
        val clipboardText = readClipboard(forceRefresh = true)
        Timber.i("📋 已读取并缓存剪贴板: ${clipboardText?.take(30)}")

        // 更新预览
        updateClipboardPreview()

        // 显示Toast提示用户
        android.widget.Toast.makeText(this, "✅ 一念剪贴板输入法已激活\n点击悬浮窗即可保存剪贴板", android.widget.Toast.LENGTH_LONG).show()

        // 保持激活状态，不自动切换
        // 用户可以点击键盘上的"切换输入法"按钮来切换回原来的输入法
        Timber.i("ℹ️ 输入法保持激活状态，用户可手动切换")
    }

    /**
     * 读取剪贴板并回调
     * @param forceRefresh 是否强制刷新（不检查内容变化）
     */
    private fun readClipboard(forceRefresh: Boolean = false): String? {
        try {
            val clipboardManager = getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            val clipData = clipboardManager.primaryClip

            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrEmpty()) {
                    // 强制刷新或检查内容变化
                    if (forceRefresh || cachedText != text) {
                        cachedText = text
                        onClipboardContent?.invoke(text)
                        // 更新预览
                        updateClipboardPreview()
                    }
                    return text
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ 输入法读取剪贴板失败")
        }
        return null
    }

    /**
     * 更新剪贴板预览
     */
    private fun updateClipboardPreview() {
        tvClipboardPreview?.post {
            if (cachedText.isNullOrEmpty()) {
                tvClipboardPreview?.text = "剪贴板为空"
                tvClipboardPreview?.setTextColor(android.graphics.Color.GRAY)
            } else {
                tvClipboardPreview?.text = "📋 ${cachedText}"
                tvClipboardPreview?.setTextColor(android.graphics.Color.BLACK)
            }
        }
    }

    /**
     * 尝试自动切换回上一个输入法
     * @return true=成功, false=失败
     */
    private fun switchToPreviousIme(): Boolean {
        return try {
            // 直接调用 InputMethodService 官方API
            val result = switchToPreviousInputMethod()

            if (result) {
                Timber.i("✅ 自动切换到上一个输入法成功")
            } else {
                Timber.w("⚠️ switchToPreviousInputMethod 返回 false（没有上一个输入法）")
            }
            result
        } catch (e: Exception) {
            Timber.w(e, "❌ 切换输入法失败")
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clipboardRunnable)
        instance = null
        Timber.i("⚠️ 输入法服务被销毁，保留 cachedText: ${cachedText?.take(30)}")
    }

    /**
     * 不处理任何按键，直接返回 false
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent): Boolean {
        return false
    }
}

