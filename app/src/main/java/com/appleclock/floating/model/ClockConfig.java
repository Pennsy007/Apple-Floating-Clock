package com.appleclock.floating.model;

/**
 * 悬浮时钟全局配置模型
 */
public class ClockConfig {
    // 是否精确到毫秒 (默认关闭，仅显示时分秒)
    public boolean showMilliseconds = false;

    // 自由缩放比例 (0.5f ~ 3.0f，默认 1.0f)
    public float scale = 1.0f;

    // 不透明度 (0.2f ~ 1.0f，默认 0.95f)
    public float opacity = 0.95f;

    // 苹果精选主题索引 (0:经典白, 1:原色钛金, 2:远峰蓝, 3:爱马仕橙, 4:荧光绿, 5:极光紫)
    public int appleThemeIndex = 0;

    // 主题高光配色
    public String themeColorHex = "#FFFFFF";
    public String msColorHex = "#34C759";

    // 是否锁定位置 (锁定后禁止单指拖拽)
    public boolean isLocked = false;

    // 是否采用 24 小时制
    public boolean is24Hour = true;

    // 是否显示顶部日期与星期
    public boolean showDate = false;

    // 悬浮窗屏幕坐标位置 (-1 表示居中或默认位置)
    public int posX = -1;
    public int posY = -1;

    // 触觉震动反馈
    public boolean hapticEnabled = true;

    // 边缘弹簧磁吸 (开启后松手平滑吸附至屏幕边缘)
    public boolean snapToEdge = true;

    // 用户自定义毫秒级时间补偿 (例如抢购提前 100ms)
    public long userCompensation = 0L;

    // 毛玻璃色系 (0: 冰霜极光白(iOS Light), 1: 深邃灵动黑(iOS Dark), 2: 远峰极客蓝, 3: 爱马仕暖金, 4: 暗夜紫罗兰)
    public int glassTheme = 0;

    // 是否开启安卓顶部居中灵动岛模式
    public boolean isIslandMode = false;

    // 位置模式：是否固定在屏幕中间 (true: 固定中间防误触; false: 自由移动拖拽)
    public boolean isFixedCenter = false;

    public ClockConfig() {}
}
