#!/bin/bash
set -e
ADB="/Users/a88888888/Android/sdk/platform-tools/adb"
DEVICE="fd71596f"
APK="/Users/a88888888/Desktop/影视/时钟/app/build/outputs/apk/debug/app-debug.apk"
ARTIFACT="/Users/a88888888/.gemini/antigravity/brain/0a854708-c0b4-4e8c-8c2c-3adac1c2a814/clock_screen.png"

echo "=== 开始全自动监听构建并自动安装部署 ==="

# 循环等待直到 APK 生成（最多等 120 秒）
for i in {1..40}; do
    if [ -f "$APK" ]; then
        echo ">>> 检测到 APK 已生成！大小: $(ls -lh "$APK" | awk '{print $5}')"
        break
    fi
    echo "[$i/40] 等待 APK 生成中... 当前 aapt2 下载大小: $(ls -lh /Users/a88888888/.gradle/.tmp/gradle_download9740788801934146753bin 2>/dev/null | awk '{print $5}' || echo '已完成/解压中')"
    sleep 3
done

if [ ! -f "$APK" ]; then
    echo ">>> 未检测到 APK，检查当前任务日志..."
    tail -n 20 /Users/a88888888/.gemini/antigravity/brain/0a854708-c0b4-4e8c-8c2c-3adac1c2a814/.system_generated/tasks/task-1533.log
    exit 1
fi

echo ">>> 1. 正在通过 ADB 推送安装至手机..."
$ADB -s $DEVICE install -r "$APK"

echo ">>> 2. 静默授予所有必要权限..."
$ADB -s $DEVICE shell appops set com.appleclock.floating SYSTEM_ALERT_WINDOW allow
$ADB -s $DEVICE shell appops set com.appleclock.floating 10021 allow
$ADB -s $DEVICE shell appops set com.appleclock.floating AUTO_START allow
$ADB -s $DEVICE shell pm grant com.appleclock.floating android.permission.POST_NOTIFICATIONS || true

echo ">>> 3. 启动主页面与悬浮时钟服务..."
$ADB -s $DEVICE shell am start -n com.appleclock.floating/.MainActivity
sleep 2
$ADB -s $DEVICE shell am startforegroundservice -n com.appleclock.floating/.service.FloatingClockService || true

echo ">>> 4. 稳定渲染等待..."
sleep 2

echo ">>> 5. 截取真机屏幕..."
$ADB -s $DEVICE shell screencap -p /sdcard/clock_running.png
$ADB -s $DEVICE pull /sdcard/clock_running.png "$ARTIFACT"

echo "=== 部署全部完成！截图已保存至 $ARTIFACT ==="
