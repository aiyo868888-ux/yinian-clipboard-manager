package com.yinian.clipboard.export

import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * 二维码生成器，生成包含配对信息的二维码
 */
class QrCodeGenerator {

    companion object {
        private const val QR_SIZE = 512
        private const val SERVER_PORT = 8080
    }

    /**
     * 配对信息数据类
     */
    data class PairingInfo(
        val deviceId: String,
        val deviceName: String,
        val ipAddress: String,
        val port: Int,
        val timestamp: Long,
        val version: String = "1.0"
    )

    /**
     * 生成配对二维码
     */
    fun generatePairingQrCode(deviceName: String): Bitmap? {
        try {
            val ipAddress = getLocalIpAddress()
            val pairingInfo = PairingInfo(
                deviceId = getDeviceId(),
                deviceName = deviceName,
                ipAddress = ipAddress,
                port = SERVER_PORT,
                timestamp = System.currentTimeMillis()
            )

            val jsonData = Gson().toJson(pairingInfo)
            return generateQrCode(jsonData)
        } catch (e: Exception) {
            Timber.e(e, "生成二维码失败")
            return null
        }
    }

    /**
     * 生成二维码Bitmap
     */
    private fun generateQrCode(content: String): Bitmap? {
        val qrCodeWriter = QRCodeWriter()
        try {
            val bitMatrix = qrCodeWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                QR_SIZE,
                QR_SIZE,
                mapOf(
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
                )
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }

            return bitmap
        } catch (e: WriterException) {
            Timber.e(e, "QR码编码失败")
            return null
        }
    }

    /**
     * 获取本地IP地址
     */
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address.hostAddress?.contains(':') == false) {
                        return address.hostAddress ?: ""
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "获取本地IP失败")
        }
        return "0.0.0.0"
    }

    /**
     * 获取设备ID
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.ANDROID_ID
    }

    /**
     * 解析配对信息
     */
    fun parsePairingInfo(jsonData: String): PairingInfo? {
        return try {
            Gson().fromJson(jsonData, PairingInfo::class.java)
        } catch (e: Exception) {
            Timber.e(e, "解析配对信息失败")
            null
        }
    }
}
