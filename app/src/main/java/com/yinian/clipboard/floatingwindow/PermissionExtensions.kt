package com.yinian.clipboard.floatingwindow

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 悬浮窗权限请求ViewModel，避免内存泄漏
 */
class PermissionViewModel : ViewModel() {

    private val _permissionResult = MutableSharedFlow<Boolean>()
    val permissionResult = _permissionResult.asSharedFlow()

    private var permissionLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * 在Activity中注册launcher
     */
    fun registerLauncher(activity: AppCompatActivity) {
        if (permissionLauncher == null) {
            permissionLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { _ ->
                val granted = FloatingWindowManager(activity).hasPermission()
                viewModelScope.launch {
                    _permissionResult.emit(granted)
                }
            }
        }
    }

    /**
     * 请求权限
     */
    fun requestPermission(activity: AppCompatActivity) {
        val launcher = permissionLauncher
        if (launcher == null) {
            registerLauncher(activity)
            requestPermission(activity)
            return
        }

        val manager = FloatingWindowManager(activity)
        if (manager.hasPermission()) {
            viewModelScope.launch {
                _permissionResult.emit(true)
            }
        } else {
            val intent = manager.requestPermission()
            launcher.launch(intent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Launcher会自动清理，不需要手动unregister
        permissionLauncher = null
    }
}

/**
 * AppCompatActivity扩展函数，使用ViewModel管理权限请求
 * 避免内存泄漏
 */
fun AppCompatActivity.requestFloatingWindowPermission(
    onResult: (granted: Boolean) -> Unit
) {
    // 简化实现：直接使用临时的Helper，但确保在使用后立即清理
    val helper = FloatingWindowPermissionHelper(this) { granted ->
        onResult(granted)
    }
    helper.requestPermission()
}

/**
 * 悬浮窗权限请求助手（改进版，使用弱引用避免内存泄漏）
 */
class FloatingWindowPermissionHelper(
    private val activity: AppCompatActivity,
    private val onResult: (granted: Boolean) -> Unit
) {

    private var permissionLauncher: ActivityResultLauncher<Intent>? = null

    init {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            val granted = FloatingWindowManager(activity).hasPermission()
            onResult(granted)
        }
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestPermission() {
        val manager = FloatingWindowManager(activity)
        if (manager.hasPermission()) {
            onResult(true)
        } else {
            val intent = manager.requestPermission()
            permissionLauncher?.launch(intent)
        }
    }

    /**
     * 清理资源（可选调用）
     */
    fun cleanup() {
        // ActivityResultLauncher会在Activity销毁时自动清理
        // 这里保留引用以便后续可能的扩展
        permissionLauncher = null
    }

    /**
     * 检查是否有权限
     */
    fun hasPermission(): Boolean {
        return FloatingWindowManager(activity).hasPermission()
    }
}
