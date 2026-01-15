# 一念剪贴板管理器 - 开发计划

## Overview

开发一个Android剪贴板管理应用，支持自动监听剪贴板、悬浮窗快速保存、标签分类管理、二维码配对跨设备导出功能。

## Task Breakdown

### Task 1: 剪贴板监听与数据持久化
- **ID**: task-1
- **type**: default
- **Description**: 实现后台剪贴板监听服务，使用Room数据库存储剪贴板历史记录（文本、图片），支持Foreground Service确保后台持续运行
- **File Scope**: `app/src/main/java/com/yinian/clipboard/{clipboard,data}`
  - `clipboard/ClipboardListenerService.kt`（前台服务）
  - `clipboard/ClipboardManager.kt`（剪贴板管理器）
  - `data/ClipboardEntity.kt`（Room实体）
  - `data/ClipboardDao.kt`（Room DAO）
  - `data/ClipboardDatabase.kt`（Room数据库）
- **Dependencies**: None
- **Test Command**: `./gradlew test --tests "com.yinian.clipboard.data.*" --coverage`
- **Test Focus**:
  - 剪贴板内容正确解析（文本/图片）
  - Room数据库CRUD操作
  - 前台服务生命周期管理
  - 内存泄漏检测

### Task 2: 悬浮窗实现
- **ID**: task-2
- **type**: ui
- **Description**: 使用WindowManager实现系统悬浮窗，支持拖动、点击展开、隐藏显示，请求SYSTEM_ALERT_WINDOW权限
- **File Scope**: `app/src/main/java/com/yinian/clipboard/floatingwindow/`
  - `FloatingWindowService.kt`（悬浮窗服务）
  - `FloatingView.kt`（Compose悬浮窗UI）
  - `FloatingWindowManager.kt`（权限与显示管理）
- **Dependencies**: None
- **Test Command**: `./gradlew connectedAndroidTest --tests "com.yinian.clipboard.floatingwindow.*" --coverage`
- **Test Focus**:
  - 悬浮窗权限请求流程
  - 触摸事件处理（拖动/点击）
  - 不同屏幕尺寸适配
  - 悬浮窗生命周期（创建/销毁/隐藏）
  - UI测试（Compose UI测试）

### Task 3: 标签系统与主界面UI
- **ID**: task-3
- **type**: ui
- **Description**: 使用Jetpack Compose实现主界面（剪贴板列表）、标签管理（增删改查）、标签筛选功能，使用Material 3设计规范
- **File Scope**: `app/src/main/java/com/yinian/clipboard/{ui,repository}`
  - `ui/screens/MainScreen.kt`（主界面）
  - `ui/screens/TagManagementScreen.kt`（标签管理）
  - `ui/components/ClipboardItem.kt`（剪贴板列表项）
  - `ui/viewmodel/MainViewModel.kt`（MVVM ViewModel）
  - `data/TagEntity.kt`（标签实体）
  - `data/TagDao.kt`（标签DAO）
  - `repository/ClipboardRepository.kt`（仓库层）
- **Dependencies**: depends on task-1（需要数据库结构）
- **Test Command**: `./gradlew test --tests "com.yinian.clipboard.ui.*" --coverage`
- **Test Focus**:
  - Compose UI组件测试
  - ViewModel状态管理测试
  - 标签CRUD操作测试
  - 列表滚动性能测试
  - 筛选功能测试
  - UI截图测试（Paparazzi）

### Task 4: 二维码配对与数据导出
- **ID**: task-4
- **type**: default
- **Description**: 实现二维码生成（手机显示连接信息）、电脑扫码配对、通过WiFi Direct或HTTP服务器传输数据到电脑，支持导出JSON/CSV格式
- **File Scope**: `app/src/main/java/com/yinian/clipboard/export/`
  - `export/QrCodeGenerator.kt`（二维码生成，使用ZXing）
  - `export/ConnectionManager.kt`（WiFi Direct/蓝牙管理）
  - `export/DataExportServer.kt`（HTTP服务器，使用NanoHTTPD）
  - `export/DataExporter.kt`（数据序列化）
  - `ui/screens/ExportScreen.kt`（导出界面）
- **Dependencies**: depends on task-1（需要数据库数据）
- **Test Command**: `./gradlew test --tests "com.yinian.clipboard.export.*" --coverage`
- **Test Focus**:
  - 二维码生成正确性
  - WiFi Direct连接建立
  - HTTP服务器请求处理
  - 数据序列化（JSON/CSV）
  - 网络异常处理
  - 权限请求（WiFi/蓝牙）

## Acceptance Criteria

- [ ] 剪贴板监听稳定运行（后台不被杀死，监听实时）
- [ ] 悬浮窗功能完整（可拖动、点击、展开、隐藏）
- [ ] 标签增删改查正常（创建、编辑、删除、筛选）
- [ ] 二维码配对导出成功（扫码连接→数据传输→导出文件）
- [ ] 所有单元测试通过
- [ ] 代码覆盖率 ≥90%
- [ ] 通过Android 8-14兼容性测试
- [ ] 通过性能测试（内存占用 <100MB，CPU占用 <5%）

## Technical Notes

### 关键技术决策

1. **技术栈**：Kotlin + Jetpack Compose + Room + Hilt + Coroutines + Flow
2. **架构模式**：MVVM + Clean Architecture（UI → ViewModel → Repository → DAO）
3. **异步处理**：Kotlin Coroutines + Flow（剪贴板监听、数据库操作、网络传输）
4. **权限管理**：
   - `SYSTEM_ALERT_WINDOW`（悬浮窗）
   - `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`（后台监听）
   - `ACCESS_WIFI_STATE` + `CHANGE_WIFI_STATE`（WiFi Direct）
   - `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN`（蓝牙，Android 12+）
5. **跨设备传输**：优先WiFi Direct（点对点高速），降级到HTTP服务器（同局域网）

### 约束条件

1. **Android版本**：最低SDK 26（Android 8.0），目标SDK 34（Android 14）
2. **数据库性能**：使用索引优化查询，分页加载（每页20条）
3. **内存管理**：Glide加载大图片，LRU缓存策略
4. **安全性**：导出数据加密（AES-256），HTTPS传输

### 潜在风险

1. **Android后台限制**：Android 10+ 对后台启动Activity有严格限制，需要通过通知渠道引导用户
2. **剪贴板权限**：部分手机厂商（小米/华为）有自定义剪贴板权限，需要适配
3. **WiFi Direct兼容性**：部分设备不支持WiFi Direct，需要提供HTTP降级方案
4. **悬浮窗权限**：Android 13+ 需要用户手动授予，权限请求流程需要清晰引导

### 开发依赖

```gradle
// Room 数据库
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Jetpack Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// Hilt 依赖注入
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-compiler:2.50")

// 二维码生成
implementation("com.google.zxing:core:3.5.2")

// HTTP服务器
implementation("org.nanohttpd:nanohttpd:2.3.1")

// 图片加载
implementation("com.github.bumptech.glide:compose:1.0.0-beta01")

// 单元测试
testImplementation("junit:junit:4.13.2")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.1")
```

### 性能优化

1. **数据库优化**：
   - 使用`@Index`为常用查询字段建索引
   - 使用`Flow`实现响应式数据流
   - 分页加载（Paging 3库）

2. **UI优化**：
   - 使用`LazyColumn`实现虚拟滚动
   - 图片缩略图缓存（Glide）
   - 避免过度重组（`remember`、`derivedStateOf`）

3. **内存优化**：
   - 大图片压缩（采样率inSampleSize）
   - WeakReference缓存剪贴板数据
   - LeakCanary检测内存泄漏

### 测试策略

1. **单元测试**（JUnit 5 + MockK）：
   - 数据库操作测试（Room提供in-memory数据库）
   - ViewModel业务逻辑测试
   - Repository层测试

2. **UI测试**（Compose Testing）：
   - 悬浮窗交互测试
   - 主界面列表滚动测试
   - 标签管理UI测试

3. **集成测试**（AndroidX Test）：
   - 剪贴板监听端到端测试
   - 二维码配对流程测试
   - 数据导出功能测试

4. **性能测试**：
   - Benchmark（Jetpack Benchmark）测量关键操作耗时
   - Memory Profiler检测内存泄漏
   - CPU Profiler优化CPU占用

### 开发里程碑

1. **Week 1**：完成Task 1（剪贴板监听 + 数据库）
2. **Week 2**：完成Task 2（悬浮窗）
3. **Week 3**：完成Task 3（主界面 + 标签系统）
4. **Week 4**：完成Task 4（二维码配对 + 数据导出）
5. **Week 5**：测试优化 + 兼容性适配 + 发布准备
