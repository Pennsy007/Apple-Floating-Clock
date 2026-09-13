#!/bin/bash
set -e
ADB="/Users/a88888888/Android/sdk/platform-tools/adb"
DEVICE="fd71596f"
APK_PATH="/Users/a88888888/Desktop/影视/时钟/app/build/outputs/apk/debug/app-debug.apk"
ARTIFACT_DIR="/Users/a88888888/.gemini/antigravity/brain/0a854708-c0b4-4e8c-8c2c-3adac1c2a814"

echo ">>> 1. 检查 APK 文件..."
if [ ! -f "$APK_PATH" ]; then
    echo "APK 尚未生成: $APK_PATH"
    exit 1
fi

echo ">>> 2. 推送并安装应用到手机..."
$ADB -s $DEVICE install -r "$APK_PATH"

echo ">>> 3. 静默授予所有必要权限（悬浮窗、通知、小米后台弹出等）..."
# 悬浮窗权限 (SYSTEM_ALERT_WINDOW)
$ADB -s $DEVICE shell appops set com.appleclock.floating SYSTEM_ALERT_WINDOW allow
# 小米 HyperOS / MIUI 专有后台弹出界面权限 (Code 10021)
$ADB -s $DEVICE shell appops set com.appleclock.floating 10021 allow
# 自启动权限
$ADB -s $DEVICE shell appops set com.appleclock.floating AUTO_START allow
# 通知权限 (Android 13+)
$ADB -s $DEVICE shell pm grant com.appleclock.floating android.permission.POST_NOTIFICATIONS || true

echo ">>> 4. 启动主页面与悬浮时钟服务..."
$ADB -s $DEVICE shell am start -n com.appleclock.floating/.MainActivity
sleep 2
$ADB -s $DEVICE shell am startforegroundservice -n com.appleclock.floating/.service.FloatingClockService || true

echo ">>> 5. 等待 2 秒让悬浮时钟稳定渲染..."
sleep 2

echo ">>> 6. 截取真机运行效果图并回传..."
$ADB -s $DEVICE shell screencap -p /sdcard/clock_running.png
$ADB -s $DEVICE pull /sdcard/clock_running.png "$ARTIFACT_DIR/clock_screen.png"

echo ">>> 全部自动化部署与验证完成！"
