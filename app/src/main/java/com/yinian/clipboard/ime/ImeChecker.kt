package com.yinian.clipboard.ime

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.yinian.clipboard.R

/**
 * 检查输入法是否已启用
 */
fun isImeEnabled(context: Context): Boolean {
    val imeId = context.getString(R.string.ime_method_id)
    val enabledImes = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_INPUT_METHODS
    )
    return enabledImes != null && enabledImes.contains(imeId)
}

/**
 * 检查输入法是否是当前选中的输入法
 */
fun isImeCurrent(context: Context): Boolean {
    val imeId = context.getString(R.string.ime_method_id)
    val currentImeId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )
    return currentImeId == imeId
}

/**
 * 显示输入法选择对话框（供用户切换到我们的输入法）
 */
fun showInputMethodPicker(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showInputMethodPicker()
}
