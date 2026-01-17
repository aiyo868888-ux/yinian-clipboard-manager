@echo off
cd /d "%~dp0"
gradlew.bat assembleDebug
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===============================================
    echo 构建成功！APK 位置：
    echo app\build\outputs\apk\debug\app-debug.apk
    echo ===============================================
) else (
    echo.
    echo ===============================================
    echo 构建失败，请检查错误信息
    echo ===============================================
)
pause
