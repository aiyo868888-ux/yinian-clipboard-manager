# 🔐 安全问题快速修复指南

## 问题 1: CORS 完全开放

### 当前代码（危险）
`app/src/main/java/com/yinian/clipboard/export/DataExportServer.kt:276-279`
```kotlin
private fun addCORSHeaders(response: Response) {
    response.addHeader("Access-Control-Allow-Origin", "*")  // ❌ 危险！
    response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
    response.addHeader("Access-Control-Allow-Headers", "Content-Type")
}
```

### 修复方案

#### 方案 A: 仅允许本地访问（推荐）
```kotlin
// 1. 修改服务器监听地址
fun startServer(port: Int = DEFAULT_PORT): Boolean {
    return try {
        // 只监听 localhost，不暴露到局域网
        server = ExportHttpServer("127.0.0.1", port, repository)
        server?.start()
        Timber.d("HTTP服务器已启动（仅本地访问），端口: $port")
        true
    } catch (e: Exception) {
        Timber.e(e, "启动HTTP服务器失败")
        false
    }
}

// 2. 限制 CORS 头
private fun addCORSHeaders(response: Response) {
    // 只允许本地访问
    response.addHeader("Access-Control-Allow-Origin", "http://127.0.0.1:8080")
    response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS")
    response.addHeader("Access-Control-Allow-Headers", "Content-Type")
}
```

#### 方案 B: 添加 API Key 认证（更安全）
```kotlin
// 1. 在 Application 中生成随机 API Key
class ClipboardApplication : Application() {
    val apiKey = UUID.randomUUID().toString()
}

// 2. 验证请求
override fun serve(session: IHTTPSession): Response {
    // 验证 API Key
    val providedKey = session.headers["Authorization"]
    val expectedKey = (context.applicationContext as ClipboardApplication).apiKey

    if (providedKey != expectedKey) {
        return newFixedLengthResponse(
            Response.Status.UNAUTHORIZED,
            "application/json",
            """{"error":"Unauthorized"}"""
        )
    }

    // 继续处理请求...
}
```

---

## 问题 2: 敏感数据明文存储

### 当前代码
`app/src/main/java/com/yinian/clipboard/data/ClipboardEntity.kt`
```kotlin
@Entity(tableName = "clipboard_items")
data class ClipboardEntity(
    @ColumnInfo(name = "text_content") val textContent: String?  // ❌ 明文
)
```

### 修复方案: 使用 SQLCipher

#### Step 1: 添加依赖
`app/build.gradle.kts`
```kotlin
dependencies {
    // SQLCipher for encrypted database
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
}
```

#### Step 2: 修改数据库配置
`app/src/main/java/com/yinian/clipboard/data/ClipboardDatabase.kt`
```kotlin
@Provides
@Singleton
fun provideClipboardDatabase(
    @ApplicationContext context: Context
): ClipboardDatabase {
    // 生成加密密钥（不要硬编码！）
    val passphrase: ByteArray = SQLiteDatabase.getBytes(
        getSecurePassphrase().toCharArray()
    )
    val factory = SupportFactory(passphrase)

    return Room.databaseBuilder(
        context,
        ClipboardDatabase::class.java,
        "clipboard_database"
    )
        .openHelperFactory(factory)  // 添加加密支持
        .addMigrations(MIGRATION_1_2)
        .build()
}

private fun getSecurePassphrase(): String {
    // 方案1: 使用 Android KeyStore
    // 方案2: 使用设备唯一标识
    return Settings.Secure.ANDROID_ID + "_clipboard_db_salt"
}
```

---

## 问题 3: HTTP 服务器无超时配置

### 当前代码
`app/src/main/java/com/yinian/clipboard/export/DataExportServer.kt`
```kotlin
inner class ExportHttpServer(
    private val port: Int,
    private val repository: ClipboardRepository
) : NanoHTTPD(port) {  // ❌ 无超时配置
```

### 修复方案
```kotlin
inner class ExportHttpServer(
    private val port: Int,
    private val repository: ClipboardRepository
) : NanoHTTPD(port) {

    init {
        // 配置服务器超时
        setServerSocketFactory(
            java.net.ServerSocket(
                port,
                50,  // backlog
                java.net.InetAddress.getByName("127.0.0.1")
            )
        )

        // 设置超时时间
        setTimeout(30000)  // 30秒
    }

    // 添加请求超时处理
    override fun serve(session: IHTTPSession): Response {
        return try {
            withTimeout(30000) {  // 30秒超时
                handleRequest(session)
            }
        } catch (e: TimeoutCancellationException) {
            newFixedLengthResponse(
                Response.Status.REQUEST_TIMEOUT,
                "application/json",
                """{"error":"Request timeout"}"""
            )
        }
    }
}
```

---

## 问题 4: 查询无限制 → 可能OOM

### 当前代码
`app/src/main/java/com/yinian/clipboard/data/ClipboardDao.kt:65-66`
```kotlin
@Query("SELECT * FROM clipboard_items ORDER BY created_at DESC")
fun getAllByCreatedAt(): Flow<List<ClipboardEntity>>  // ❌ 无LIMIT
```

### 修复方案
```kotlin
// 方案1: 添加 LIMIT（简单快速）
@Query("SELECT * FROM clipboard_items ORDER BY created_at DESC LIMIT 1000")
fun getAllByCreatedAt(): Flow<List<ClipboardEntity>>

// 方案2: 使用 Paging 3（推荐）
@Query("SELECT * FROM clipboard_items ORDER BY created_at DESC")
fun pagingSource(): PagingSource<Int, ClipboardEntity>

// 方案3: 添加时间范围过滤
@Query("""
    SELECT * FROM clipboard_items
    WHERE created_at > :startTime
    ORDER BY created_at DESC
    LIMIT 1000
""")
fun getRecent(startTime: Long): Flow<List<ClipboardEntity>>
```

---

## 🎯 修复优先级

### 立即修复（5分钟）
1. ✅ 修改 CORS 为 localhost only
2. ✅ 添加查询 LIMIT 1000
3. ✅ 添加服务器超时配置

### 本周修复（1小时）
4. 实现 API Key 认证
5. 添加数据库加密（SQLCipher）

---

## 📝 修复后测试

```bash
# 1. 构建项目
gradlew.bat build

# 2. 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 3. 测试安全限制
# 尝试从局域网其他设备访问: http://手机IP:8080/api/clipboard
# 预期结果: 连接被拒绝（如果使用方案A）

# 4. 测试本地访问
# 在手机浏览器访问: http://127.0.0.1:8080/api/clipboard
# 预期结果: 正常返回数据
```

---

**修复时间估计**: 30分钟 - 2小时（取决于选择的方案）
**影响范围**: 3个文件
**测试要求**: 必须测试本地和远程访问

---

> ⚠️ **警告**: 修复前请备份当前代码！
> ⚠️ **警告**: 数据库加密后，旧数据无法迁移，需要清空数据库！
