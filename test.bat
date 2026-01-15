@echo off
REM 一念剪贴板管理器 - Windows 快速测试脚本

echo ==================================
echo   一念剪贴板管理器 - 自动化测试
echo ==================================
echo.

REM 检查设备连接
echo 1. 检查 Android 设备连接...
adb devices | findstr "device" >nul
if errorlevel 1 (
    echo [错误] 未检测到 Android 设备，请确保 USB 调试已开启
    pause
    exit /b 1
)
echo [√] 设备已连接
echo.

REM 运行单元测试
echo 2. 运行单元测试...
call gradlew.bat test --console=plain
if errorlevel 1 (
    echo [×] 单元测试失败
    pause
    exit /b 1
)
echo [√] 单元测试通过
echo.

REM 构建 APK
echo 3. 构建 Debug APK...
call gradlew.bat assembleDebug --console=plain
if errorlevel 1 (
    echo [×] APK 构建失败
    pause
    exit /b 1
)
echo [√] APK 构建成功
echo.

REM 安装 APK
echo 4. 安装 APK 到设备...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo [×] APK 安装失败
    pause
    exit /b 1
)
echo [√] APK 安装成功
echo.

REM 启动应用
echo 5. 启动应用...
adb shell am start -n com.yinian.clipboard/.ui.MainActivity
echo [√] 应用已启动
echo.

REM 获取设备 IP
echo 6. 获取设备 IP 地址...
for /f "tokens=2" %%i in ('adb shell ip addr show wlan0 ^| findstr "inet "') do set DEVICE_IP=%%i
if defined DEVICE_IP (
    echo [√] 设备 IP: %DEVICE_IP%
    echo.
    echo ==================================
    echo   测试环境已就绪！
    echo ==================================
    echo.
    echo API 测试地址:
    echo   JSON:  http://%DEVICE_IP%:8080/api/clipboard
    echo   CSV:   http://%DEVICE_IP%:8080/api/clipboard/export/csv
    echo   健康检查: http://%DEVICE_IP%:8080/api/health
    echo.
) else (
    echo [×] 无法获取设备 IP
)
echo.

REM 询问是否查看日志
set /p LOGS="是否查看实时日志? (y/n): "
if /i "%LOGS%"=="y" (
    echo 查看日志 (Ctrl+C 退出)...
    adb logcat ^| findstr "yinian"
)

echo.
echo [√] 测试脚本执行完成！
pause
