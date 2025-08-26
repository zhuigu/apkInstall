#-------------------------------------------------------------------------------
# Android App Specific ProGuard Rules
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
#-------------------------------------------------------------------------------

# Android 默认优化规则已经通过 build.gradle.kts 中的 getDefaultProguardFile("proguard-android-optimize.txt") 引入。
# 这里添加的是项目特有的规则。

#-------------------------------------------------------------------------------
# 1. 通用 Android 规则
#-------------------------------------------------------------------------------

# 如果你的项目使用了 WebView 并与 JavaScript 接口交互，需要保留 JavaScript 接口类及其公共方法。
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# 保留所有的 JNI 本地方法。
-keepclasseswithmembers class * {
    native <methods>;
}

# 保留所有 Enum 类的 values() 和 valueOf() 方法，因为它们经常通过反射调用。
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 如果你使用了 Java 的序列化机制（即实现了 java.io.Serializable 接口），需要保留相关类。
# Kotlin 项目中通常更推荐使用 Kotlinx Serialization 或 Parcelable。
#-keepnames class * implements java.io.Serializable
#-keepclassmembers class * implements java.io.Serializable {
#    static final long serialVersionUID;
#    private static final java.io.ObjectStreamField[] serialPersistentFields;
#    private void writeObject(java.io.ObjectOutputStream);
#    private void readObject(java.io.ObjectInputStream);
#    private void readObjectNoData();
#    private void writeReplace();
#    private void readResolve();
#}

# 对于 AndroidX 库，R8 通常能很好地处理，但偶尔遇到问题时，可以添加：
#-keep class androidx.** { *; }

#-------------------------------------------------------------------------------
# 2. Ktor 和 Kotlinx Serialization 相关的混淆规则
#-------------------------------------------------------------------------------

# 2.1 Kotlinx Serialization (Ktor 的内容协商依赖此)
# 保持所有被 @Serializable 注解的类及其成员不被混淆。
# 这是因为 Kotlinx Serialization 在运行时需要通过反射访问这些信息。
-keepnames class * { @kotlinx.serialization.Serializable <fields>; }
-keepclassmembers class * { @kotlinx.serialization.Serializable <fields>; }

# 保持实现了 kotlinx.serialization.KSerializer 或 GeneratedSerializer 的类不被混淆。
# 这是 Kotlinx Serialization 自动生成的序列化器类。
-keepnames class * implements kotlinx.serialization.KSerializer { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepnames class * implements kotlinx.serialization.GeneratedSerializer { *; }
-keep class * implements kotlinx.serialization.GeneratedSerializer { *; }

# 保持伴生对象中的 serializer() 方法不被混淆，因为这些方法通常返回序列化器实例。
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer();
}

# 2.2 Ktor Core 和 Features (io.ktor:ktor-server-core, io.ktor:ktor-server-content-negotiation)
# 保持 Ktor 核心组件的类和方法不被混淆，尤其是那些可能被反射访问或服务加载的部分。
-keep class io.ktor.** { *; }
# 忽略 Ktor 模块中可能未使用的代码产生的警告。
-dontwarn io.ktor.**

# 保持 Ktor 插件系统所需的类，例如 ApplicationPlugin 及其实现。
# 如果你自定义了 Ktor 插件，可能需要更具体的规则。
-keepnames class io.ktor.server.application.ApplicationPlugin { *; }
-keepnames class io.ktor.server.plugins.** { *; }

# 2.3 Ktor Server Netty (io.ktor:ktor-server-netty)
# Netty 作为 Ktor 的底层网络框架，通常需要特定的混淆规则，因为它直接操作字节缓冲区和底层系统资源。
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# Netty 对 sun.misc.Unsafe 的使用 (用于高性能操作，尽管在 Android 上可能受限)
-keep class sun.misc.Unsafe { *; }

# Netty 的直接内存管理和清除器，确保它们在混淆后仍能正常工作。
-keep class io.netty.util.internal.Cleaner { *; }
-keep class io.netty.util.internal.PlatformDependent { *; }

# 如果你的 Ktor Netty 服务器在 Android 上使用了 Epoll 或 KQueue (通常用于 Linux/macOS 优化 I/O)，
# 则需要保留相关类。在 Android 上，这可能不常用或默认不启用，但为安全起见可以保留。
-keep class io.netty.channel.epoll.** { *; }
-keep class io.netty.channel.kqueue.** { *; }
-keep class io.netty.buffer.ByteBufAllocator { *; }

# 2.4 Ktor Resources (io.ktor:ktor-server-resources)
# 保持被 @Resource 注解的类不被混淆，因为它们通常通过反射被 Ktor 发现和路由。
-keepnames @io.ktor.resources.Resource class * { *; }
-keepclassmembers class * {
    @io.ktor.resources.Resource <methods>;
    @io.ktor.resources.Resource <fields>;
}

# 2.5 Kotlin Coroutines (Ktor 广泛使用)
# Ktor 大量使用 Kotlin Coroutines。虽然 R8 通常能很好地处理它们，
# 但为了防止潜在问题，可以保留或忽略警告。
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 2.6 Service Loader 机制 (通用规则，Ktor 可能使用)
# Ktor 和一些库可能通过 Java 的 ServiceLoader 机制加载组件。
# 确保 META-INF/services 文件被保留，并且文件中引用的类没有被混淆。
# 这通常由 R8 自动处理，但如果遇到 ClassNotFoundException，可以考虑检查。
# 例如，如果 Ktor 客户端会加载引擎：
#-keep service io.ktor.client.engine.HttpClientEngineFactory

#-------------------------------------------------------------------------------
# 3. 调试信息保留 (强烈建议在开发和测试阶段保留)
#-------------------------------------------------------------------------------

# 保留源代码文件名和行号信息，以便在崩溃堆栈跟踪中显示准确位置。
-keepattributes SourceFile,LineNumberTable

# 如果保留了行号信息，这个规则会将混淆后的类名显示为原始类名。
# 如果不希望暴露原始文件名，可以注释掉此行。
#-renamesourcefileattribute SourceFile