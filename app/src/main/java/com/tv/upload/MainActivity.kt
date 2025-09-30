package com.tv.upload

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tv.upload.theme.AppTheme
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

class MainActivity : ComponentActivity() {

    private lateinit var httpServer: HttpServer
    private val serverPort = 8080
    private var isServerRunning by mutableStateOf(false)
    private var currentIpAddress by mutableStateOf<String?>(null)
    private var exitDialog by mutableStateOf(false)

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

        currentIpAddress = getLocalIpAddress()

        httpServer = HttpServer(this, serverPort) { file ->
            // 在这里处理回调
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = FileProvider.getUriForFile(
                    this@MainActivity, "${packageName}.fileprovider", file
                )
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            installActivityResultLauncher.launch(intent)
        }

        setContent {
            AppTheme {
                TvUploadApp(
                    startServer = ::startServer,
                    stopServer = ::stopServer,
                    isServerRunning = isServerRunning,
                    ipAddress = currentIpAddress,
                    serverPort = serverPort
                )
            }
        }

        // 检查是否已授予安装未知应用权限
        checkInstallPermission()
        onBackPressedDispatcher.addCallback {
            showExitConfirmationDialog()
        }
    }

    private fun startServer() {
        try {
            httpServer.start()
            isServerRunning = true
            currentIpAddress = getLocalIpAddress()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.server_start_failed, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopServer() {
        httpServer.stop()
        isServerRunning = false
        currentIpAddress = getLocalIpAddress()
    }

    // 获取本地局域网 IP 地址
    private fun getLocalIpAddress(): String? {
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
            Toast.makeText(this, getString(R.string.ip_address_failed), Toast.LENGTH_LONG).show()
        }
        return null
    }

    // 显示删除对话框的辅助函数
    private fun showDeleteDialog(context: Context, file: File) {
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder
            .setTitle(getString(R.string.delete_apk))
            .setMessage(getString(R.string.delete_apk_future))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                if (file.delete()) {
                    Toast.makeText(context, getString(R.string.delete_success), Toast.LENGTH_LONG)
                        .show()
                } else {
                    Toast.makeText(context, getString(R.string.delete_error), Toast.LENGTH_LONG)
                        .show()
                }
            }.setNegativeButton(getString(R.string.saved)) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun showExitConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.RoundedCornersDialog)
            .setTitle(getString(R.string.exit_app))
            .setMessage(getString(R.string.confirm_exit_app))
            .setPositiveButton(getString(R.string.exit)) { dialog, which ->
                stopServer()
                finish()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                dialog.dismiss()
            }
        val dialog = dialogBuilder.create()
        dialog.show()
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

@Composable
fun TvUploadApp(
    startServer: () -> Unit,
    stopServer: () -> Unit,
    isServerRunning: Boolean,
    ipAddress: String?,
    serverPort: Int
) {
    val focusRequester = remember { FocusRequester() }
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.server_address),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = ipAddress?.let { stringResource(R.string.ip_address, it, serverPort) }
                    ?: stringResource(
                        R.string.network_not_connected
                    ))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        tint = if (isServerRunning) Color.Green else Color.Red,
                        contentDescription = if (isServerRunning) stringResource(R.string.stop_server) else stringResource(
                            R.string.start_server
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServerRunning) stringResource(R.string.status_running) else stringResource(
                            R.string.status_stopped
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (isServerRunning) {
                        stopServer()
                    } else {
                        startServer()
                    }
                }, enabled = true, modifier = Modifier.focusRequester(focusRequester)) {
                    Text(
                        text = if (isServerRunning) stringResource(R.string.stop_server) else stringResource(
                            R.string.start_server
                        )
                    )
                }
            }
        }

    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}