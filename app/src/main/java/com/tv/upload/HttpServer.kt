package com.tv.upload

import android.content.Context
import android.os.PowerManager
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.io.FileOutputStream

class HttpServer(
    private val context: Context,
    private val port: Int,
    private val onApkReceived: (File) -> Unit
) {

    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? =
        null
    private var wakeLock: PowerManager.WakeLock? = null
    fun start() {
        if (server != null) return
        // 休眠锁
        // 获取 PowerManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        // 创建 WakeLock，并设置为 PARTIAL_WAKE_LOCK，只保持 CPU 运行
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ApkInstaller::WakeLockTag")
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)

        server = embeddedServer(Netty, port = port) {
            // 配置 Ktor 路由
            routing {
                // 保留这个路由，用于处理根路径的请求
                get("/") {
                    val inputStream = context.assets.open("index.html")
                    call.respondOutputStream(ContentType.Text.Html) {
                        inputStream.copyTo(this)
                    }
                }
                // 提供静态文件，直接从 assets 目录加载
                staticFiles("/assets", File(context.applicationInfo.dataDir + "/files/assets"))

                // 处理 POST 请求，进行文件上传
                post("/upload") {
                    val multipart = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 200)

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            val fileName = part.originalFileName
                            if (fileName != null) {
                                val apkFile = File(context.filesDir, "temp.apk")
                                try {
                                    // 直接使用流式传输，避免一次性加载到内存
                                    part.provider().toInputStream().use { input ->
                                        FileOutputStream(apkFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }

                                    // 通知安装文件
                                    onApkReceived(apkFile)

                                    call.respondText("APK uploaded successfully and installation started.")
                                    return@forEachPart
                                } catch (e: Exception) {
                                    call.respondText(
                                        "File upload failed: ${e.message}",
                                        status = HttpStatusCode.InternalServerError
                                    )
                                    return@forEachPart // 异常后返回，结束请求处理
                                }
                            }
                        }
                        part.dispose()
                    }
                    call.respondText("No file received.", status = HttpStatusCode.BadRequest)
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1_000, 2_000)
        server = null
        // 释放 WakeLock
        wakeLock?.release()
    }
}