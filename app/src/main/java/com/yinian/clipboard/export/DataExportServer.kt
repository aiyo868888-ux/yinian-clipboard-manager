package com.yinian.clipboard.export

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yinian.clipboard.data.ClipboardEntity
import com.yinian.clipboard.repository.ClipboardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据导出HTTP服务器
 * 使用NanoHTTPD提供轻量级HTTP服务
 */
@Singleton
class DataExportServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ClipboardRepository
) {

    private var server: ExportHttpServer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val DEFAULT_PORT = 8080
    }

    /**
     * 启动服务器
     */
    fun startServer(port: Int = DEFAULT_PORT): Boolean {
        return try {
            if (server?.isAlive == true) {
                Timber.w("服务器已在运行")
                return true
            }

            server = ExportHttpServer(port, repository)
            server?.start()
            Timber.d("HTTP服务器已启动，端口: $port")
            true
        } catch (e: Exception) {
            Timber.e(e, "启动HTTP服务器失败")
            false
        }
    }

    /**
     * 停止服务器
     */
    fun stopServer() {
        server?.stop()
        server = null
        Timber.d("HTTP服务器已停止")
    }

    /**
     * 检查服务器是否运行
     */
    fun isRunning(): Boolean {
        return server?.isAlive == true
    }

    /**
     * 内部HTTP服务器实现
     */
    inner class ExportHttpServer(
        private val port: Int,
        private val repository: ClipboardRepository
    ) : NanoHTTPD(port) {

        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method

            Timber.d("收到请求: $method $uri")

            return when {
                // CORS预检请求
                method == Method.OPTIONS -> handleCORSPreflight()

                // 获取剪贴板数据（JSON）
                uri == "/api/clipboard" && method == Method.GET -> handleGetClipboard()

                // 导出CSV
                uri == "/api/clipboard/export/csv" && method == Method.GET -> handleExportCsv()

                // 健康检查
                uri == "/api/health" && method == Method.GET -> handleHealthCheck()

                // 404
                else -> newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "application/json",
                    "{\"error\":\"Not found\"}"
                )
            }
        }

        /**
         * 处理CORS预检请求
         */
        private fun handleCORSPreflight(): Response {
            return newFixedLengthResponse(
                Response.Status.OK,
                "",
                ""
            ).apply {
                addCORSHeaders(this)
            }
        }

        /**
         * 获取所有剪贴板数据（JSON格式）
         */
        private fun handleGetClipboard(): Response {
            return try {
                // 异步获取数据
                var clipboards: List<ClipboardEntity> = emptyList()
                var error: Exception? = null

                val job = scope.launch {
                    try {
                        clipboards = repository.getAllClipboards().first()
                    } catch (e: Exception) {
                        error = e
                    }
                }

                // 等待完成（最多5秒）
                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.withTimeout(5000L) {
                        job.join()
                    }
                }

                if (error != null) {
                    throw error ?: Exception("未知错误")
                }

                val json = Gson().toJson(clipboards)
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    json
                ).apply {
                    addCORSHeaders(this)
                }
            } catch (e: Exception) {
                Timber.e(e, "获取剪贴板数据失败")
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    "{\"error\":\"${e.message}\"}"
                ).apply {
                    addCORSHeaders(this)
                }
            }
        }

        /**
         * 导出CSV格式
         */
        private fun handleExportCsv(): Response {
            return try {
                var clipboards: List<ClipboardEntity> = emptyList()
                var error: Exception? = null

                val job = scope.launch {
                    try {
                        clipboards = repository.getAllClipboards().first()
                    } catch (e: Exception) {
                        error = e
                    }
                }

                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.withTimeout(5000L) {
                        job.join()
                    }
                }

                if (error != null) {
                    throw error ?: Exception("未知错误")
                }

                val csv = convertToCsv(clipboards)
                newFixedLengthResponse(
                    Response.Status.OK,
                    "text/csv",
                    csv
                ).apply {
                    addHeader("Content-Disposition", "attachment; filename=\"clipboard_export.csv\"")
                    addCORSHeaders(this)
                }
            } catch (e: Exception) {
                Timber.e(e, "导出CSV失败")
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    "{\"error\":\"${e.message}\"}"
                ).apply {
                    addCORSHeaders(this)
                }
            }
        }

        /**
         * 健康检查
         */
        private fun handleHealthCheck(): Response {
            val status = mapOf(
                "status" to "ok",
                "server" to "Yinian Clipboard Export Server",
                "version" to "1.0.0",
                "timestamp" to System.currentTimeMillis()
            )
            return newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                Gson().toJson(status)
            ).apply {
                addCORSHeaders(this)
            }
        }

        /**
         * 转换为CSV格式
         */
        private fun convertToCsv(clipboards: List<ClipboardEntity>): String {
            val sb = StringBuilder()
            // CSV头部
            sb.appendLine("ID,Type,TextContent,ImageUri,IsFavorite,CreatedAt,UpdatedAt")

            // 数据行
            clipboards.forEach { entity ->
                sb.appendLine(
                    listOf(
                        entity.id,
                        entity.type.name,
                        escapeCsv(entity.textContent),
                        escapeCsv(entity.imageUri),
                        entity.isFavorite,
                        entity.createdAt,
                        entity.updatedAt
                    ).joinToString(",")
                )
            }

            return sb.toString()
        }

        /**
         * CSV字段转义
         */
        private fun escapeCsv(value: String?): String {
            if (value == null) return ""
            val escaped = value.replace("\"", "\"\"")
            return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
                "\"$escaped\""
            } else {
                escaped
            }
        }

        /**
         * 添加CORS头
         */
        private fun addCORSHeaders(response: Response) {
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Content-Type")
        }
    }
}
