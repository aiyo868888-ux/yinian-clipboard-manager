#!/bin/bash
# 一念剪贴板管理器 - 快速测试脚本

set -e  # 遇到错误立即退出

echo "=================================="
echo "  一念剪贴板管理器 - 自动化测试"
echo "=================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查设备连接
echo -e "${YELLOW}1. 检查 Android 设备连接...${NC}"
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}错误: 未检测到 Android 设备，请确保 USB 调试已开启${NC}"
    exit 1
fi
DEVICE=$(adb devices | grep "device$" | head -1 | cut -f1)
echo -e "${GREEN}✓ 设备已连接: $DEVICE${NC}"
echo ""

# 运行单元测试
echo -e "${YELLOW}2. 运行单元测试...${NC}"
./gradlew test --console=plain
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 单元测试通过${NC}"
else
    echo -e "${RED}✗ 单元测试失败${NC}"
    exit 1
fi
echo ""

# 构建 APK
echo -e "${YELLOW}3. 构建 Debug APK...${NC}"
./gradlew assembleDebug --console=plain
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ APK 构建成功${NC}"
else
    echo -e "${RED}✗ APK 构建失败${NC}"
    exit 1
fi
echo ""

# 安装 APK
echo -e "${YELLOW}4. 安装 APK 到设备...${NC}"
adb install -r app/build/outputs/apk/debug/app-debug.apk
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ APK 安装成功${NC}"
else
    echo -e "${RED}✗ APK 安装失败${NC}"
    exit 1
fi
echo ""

# 启动应用
echo -e "${YELLOW}5. 启动应用...${NC}"
adb shell am start -n com.yinian.clipboard/.ui.MainActivity
echo -e "${GREEN}✓ 应用已启动${NC}"
echo ""

# 获取设备 IP
echo -e "${YELLOW}6. 获取设备 IP 地址...${NC}"
DEVICE_IP=$(adb shell ip addr show wlan0 | grep 'inet ' | awk '{print $2}' | cut -d/ -f1)
if [ -n "$DEVICE_IP" ]; then
    echo -e "${GREEN}✓ 设备 IP: $DEVICE_IP${NC}"
    echo ""
    echo "=================================="
    echo "  测试环境已就绪！"
    echo "=================================="
    echo ""
    echo "API 测试地址:"
    echo "  JSON:  http://$DEVICE_IP:8080/api/clipboard"
    echo "  CSV:   http://$DEVICE_IP:8080/api/clipboard/export/csv"
    echo "  健康检查: http://$DEVICE_IP:8080/api/health"
    echo ""
else
    echo -e "${RED}✗ 无法获取设备 IP${NC}"
fi
echo ""

# 询问是否查看日志
read -p "是否查看实时日志? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}查看日志 (Ctrl+C 退出)...${NC}"
    adb logcat | grep "yinian"
fi

echo ""
echo -e "${GREEN}测试脚本执行完成！${NC}"
