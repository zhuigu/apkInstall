package com.tv.upload

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.tv.upload.databinding.ActivityMainBinding
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var httpServer: HttpServer
    private val serverPort = 8080
    private var isServerRunning = false

    // 创建一个 ActivityResultLauncher
    private val installActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val tempApkFile = File(filesDir, "temp.apk")
        // 处理安装操作的结果
        if (tempApkFile.exists()) {
            showDeleteDialog(this, tempApkFile)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // Inflate the layout
        setContentView(binding.root) // Set content view using binding.root

        httpServer = HttpServer(this, serverPort) { file ->
            // 在这里处理回调
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.fileprovider",
                    file
                )
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            installActivityResultLauncher.launch(intent)
        }

        binding.toggleButton.setOnClickListener {
            if (isServerRunning) {
                stopServer()
            } else {
                startServer()
            }
        }
        updateUi()
        // 检查是否已授予安装未知应用权限
        checkInstallPermission()
        // 返回退出APP提示
        setupBackPressCallback()
    }

    private fun setupBackPressCallback() {
        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmationDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出应用？")
            .setMessage("确定要退出应用并停止服务器吗？")
            .setPositiveButton("退出") { dialog, which ->
                stopServer()
                finish()
            }
            .setNegativeButton("取消") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startServer() {
        try {
            httpServer.start()
            isServerRunning = true
            binding.statusTextView.text = getString(R.string.status_running)
            binding.toggleButton.text = getString(R.string.stop_server)
            updateUi()
        } catch (e: Exception) {
            binding.statusTextView.text = getString(R.string.server_start_failed, e.message)
        }
    }

    private fun stopServer() {
        httpServer.stop()
        isServerRunning = false
        binding.statusTextView.text = getString(R.string.status_stopped)
        binding.toggleButton.text = getString(R.string.start_server)
        updateUi()
    }

    private fun updateUi() {
        val ipAddress = getLocalIpAddress()
        if (ipAddress != null) {
            binding.ipAddressTextView.text = "http://$ipAddress:$serverPort"
        } else {
            binding.ipAddressTextView.text = getString(R.string.network_not_connected)
        }
    }

    // 获取本地局域网 IP 地址
    fun getLocalIpAddress(): String? {
        try {
            // 获取所有网络接口
            val interfaces = NetworkInterface.getNetworkInterfaces()
            // 遍历所有接口
            for (inter in interfaces) {
                // 排除回环地址和未启动的接口
                while (inter.name.equals("wlan0")) {
                    if (inter.isLoopback || !inter.isUp) continue
                    // 遍历每个接口的所有 IP 地址
                    for (addresses in inter.inetAddresses) {
                        // 只返回 IPv4 地址
                        if (addresses is Inet4Address && !addresses.isLoopbackAddress) {
                            return addresses.hostAddress
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            Log.e(TAG, "Error getting local IP address", e)
        }
        return null
    }

    // 显示删除对话框的辅助函数
    private fun showDeleteDialog(context: Context, file: File) {
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("删除安装包？")
            .setMessage("安装包将被删除。")
            .setPositiveButton("删除") { _, _ ->
                if (file.delete()) {
                    Toast.makeText(context, "删除成功。", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "删除失败。", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("保留") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // 检查安装权限的函数
    private fun checkInstallPermission() {
        if (!packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${packageName}".toUri()
            }
            startActivity(intent)
        }
    }
}