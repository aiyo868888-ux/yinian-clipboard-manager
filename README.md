# 一念剪贴板管理器

一款强大的 Android 剪贴板管理应用，支持自动监听、标签分类、悬浮窗快捷操作和跨设备数据导出。

## 功能特性

### ✅ 已完成功能

- **剪贴板自动监听**：后台自动监听系统剪贴板变化，智能去重（2秒窗口）
- **多类型支持**：支持文本、HTML、图片三种类型
- **悬浮窗**：圆形可拖动悬浮窗，点击快速访问
- **标签系统**：为剪贴板内容添加标签，支持多标签分类
- **智能筛选**：按类型、标签、收藏状态、关键词实时筛选
- **数据导出**：二维码配对 + HTTP 导出，支持 JSON/CSV 格式
- **Material 3 设计**：遵循最新 Material Design 规范

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose + Material 3
- **架构**：MVVM + Clean Architecture
- **依赖注入**：Hilt
- **数据库**：Room (SQLite)
- **并发**：Coroutines + Flow
- **日志**：Timber
- **二维码**：ZXing
- **HTTP 服务器**：NanoHTTPD

## 系统要求

- Android 8.0 (API 26) 或更高版本
- 悬浮窗权限
- 网络权限（用于数据导出）

## 安装

```bash
# 克隆仓库
git clone https://github.com/your-username/yinian-clipboard-manager.git

# 打开项目
cd yinian-clipboard-manager

# 使用 Android Studio 打开项目并构建
```

## 项目结构

```
app/src/main/java/com/yinian/clipboard/
├── data/              # 数据层
│   ├── ClipboardEntity.kt
│   ├── ClipboardDao.kt
│   ├── ClipboardDatabase.kt
│   └── TagEntity.kt
├── clipboard/         # 剪贴板监听
│   ├── SystemClipboardMonitor.kt
│   └── ClipboardListenerService.kt
├── floatingwindow/    # 悬浮窗
│   ├── FloatingWindowManager.kt
│   └── FloatingWindowService.kt
├── repository/        # 仓库层
│   └── ClipboardRepository.kt
├── export/            # 数据导出
│   ├── QrCodeGenerator.kt
│   └── DataExportServer.kt
└── ui/                # UI 层
    ├── MainActivity.kt
    ├── viewmodel/
    ├── components/
    └── screens/
```

## 使用说明

### 1. 剪贴板监听
应用启动后自动开始监听剪贴板，所有复制的内容会自动保存到应用中。

### 2. 悬浮窗
- 首次使用需要授予悬浮窗权限
- 拖动悬浮窗可移动位置
- 点击悬浮窗可快速打开应用

### 3. 标签管理
- 在主界面点击剪贴板项目可添加标签
- 支持创建、编辑、删除标签
- 一个剪贴板内容可关联多个标签

### 4. 数据导出
1. 确保手机和电脑在同一 Wi-Fi 网络
2. 打开"导出"标签页
3. 开启导出服务器
4. 在电脑浏览器输入显示的地址（或扫描二维码）
5. 选择导出格式（JSON 或 CSV）

## API 接口

应用启动 HTTP 服务器后，提供以下接口：

- `GET /api/clipboard` - 获取所有剪贴板数据（JSON）
- `GET /api/clipboard/export/csv` - 导出 CSV 格式
- `GET /api/health` - 健康检查

## 测试

```bash
# 运行单元测试
./gradlew test

# 运行仪表盘测试
./gradlew connectedAndroidTest
```

**当前测试覆盖率**：约 40%（目标 90%）

## 开发计划

### 🚧 待完成功能
- [ ] 补充单元测试到 90% 覆盖率
- [ ] 实现标签筛选优化（当前为临时实现）
- [ ] 添加深色模式支持
- [ ] 性能优化（Paging 3 分页加载）
- [ ] WiFi Direct 点对点传输
- [ ] 数据加密传输
- [ ] 云端同步支持

### 🐛 已知问题
- 标签筛选功能为临时实现，返回全部数据后在前端过滤
- 部分硬编码字符串未移至 string resources
- 未进行性能测试（内存、CPU 占用）

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 作者

一念团队

---

**项目状态**：核心功能完成，可用于日常使用。

**最后更新**：2025-01-15
