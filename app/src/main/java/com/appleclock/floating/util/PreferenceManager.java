package com.appleclock.floating.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.appleclock.floating.model.ClockConfig;

/**
 * 用户时钟配置本地持久化管理器
 */
public class PreferenceManager {

    private static final String PREF_NAME = "floating_clock_prefs";
    private static final String KEY_SHOW_MS = "key_show_ms";
    private static final String KEY_SCALE = "key_scale";
    private static final String KEY_OPACITY = "key_opacity";
    private static final String KEY_THEME_INDEX = "key_theme_index";
    private static final String KEY_THEME_COLOR = "key_theme_color";
    private static final String KEY_MS_COLOR = "key_ms_color";
    private static final String KEY_IS_LOCKED = "key_is_locked";
    private static final String KEY_IS_24_HOUR = "key_is_24_hour";
    private static final String KEY_SHOW_DATE = "key_show_date";
    private static final String KEY_POS_X = "key_pos_x";
    private static final String KEY_POS_Y = "key_pos_y";
    private static final String KEY_HAPTIC = "key_haptic";
    private static final String KEY_SNAP_EDGE = "key_snap_edge";
    private static final String KEY_NETWORK_OFFSET = "key_network_offset";
    private static final String KEY_USER_COMPENSATION = "key_user_compensation";
    private static final String KEY_IS_CALIBRATED = "key_is_calibrated";
    private static final String KEY_GLASS_THEME = "key_glass_theme";
    private static final String KEY_ISLAND_MODE = "key_island_mode";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public ClockConfig loadConfig() {
        ClockConfig config = new ClockConfig();
        config.showMilliseconds = prefs.getBoolean(KEY_SHOW_MS, false);
        config.scale = Math.min(1.8f, Math.max(0.6f, prefs.getFloat(KEY_SCALE, 1.0f)));
        config.opacity = prefs.getFloat(KEY_OPACITY, 0.95f);
        config.appleThemeIndex = prefs.getInt(KEY_THEME_INDEX, 0);
        config.themeColorHex = prefs.getString(KEY_THEME_COLOR, "#FFFFFF");
        config.msColorHex = prefs.getString(KEY_MS_COLOR, "#34C759");
        config.isLocked = prefs.getBoolean(KEY_IS_LOCKED, false);
        config.is24Hour = prefs.getBoolean(KEY_IS_24_HOUR, true);
        config.showDate = prefs.getBoolean(KEY_SHOW_DATE, false);
        config.posX = prefs.getInt(KEY_POS_X, -1);
        config.posY = prefs.getInt(KEY_POS_Y, -1);
        config.hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true);
        config.snapToEdge = prefs.getBoolean(KEY_SNAP_EDGE, true);
        config.userCompensation = prefs.getLong(KEY_USER_COMPENSATION, 0L);
        config.glassTheme = prefs.getInt(KEY_GLASS_THEME, 0);
        config.isIslandMode = prefs.getBoolean(KEY_ISLAND_MODE, false);
        return config;
    }

    public void saveConfig(ClockConfig config) {
        prefs.edit()
            .putBoolean(KEY_SHOW_MS, config.showMilliseconds)
            .putFloat(KEY_SCALE, config.scale)
            .putFloat(KEY_OPACITY, config.opacity)
            .putInt(KEY_THEME_INDEX, config.appleThemeIndex)
            .putString(KEY_THEME_COLOR, config.themeColorHex)
            .putString(KEY_MS_COLOR, config.msColorHex)
            .putBoolean(KEY_IS_LOCKED, config.isLocked)
            .putBoolean(KEY_IS_24_HOUR, config.is24Hour)
            .putBoolean(KEY_SHOW_DATE, config.showDate)
            .putInt(KEY_POS_X, config.posX)
            .putInt(KEY_POS_Y, config.posY)
            .putBoolean(KEY_HAPTIC, config.hapticEnabled)
            .putBoolean(KEY_SNAP_EDGE, config.snapToEdge)
            .putLong(KEY_USER_COMPENSATION, config.userCompensation)
            .putInt(KEY_GLASS_THEME, config.glassTheme)
            .putBoolean(KEY_ISLAND_MODE, config.isIslandMode)
            .apply();
    }

    public void updatePosition(int x, int y) {
        prefs.edit().putInt(KEY_POS_X, x).putInt(KEY_POS_Y, y).apply();
    }

    public void updateScale(float scale) {
        prefs.edit().putFloat(KEY_SCALE, scale).apply();
    }

    public long getNetworkOffset() {
        return prefs.getLong(KEY_NETWORK_OFFSET, 0L);
    }

    public void saveNetworkOffset(long offset) {
        prefs.edit().putLong(KEY_NETWORK_OFFSET, offset).apply();
    }

    public long getUserCompensation() {
        return prefs.getLong(KEY_USER_COMPENSATION, 0L);
    }

    public void saveUserCompensation(long compensation) {
        prefs.edit().putLong(KEY_USER_COMPENSATION, compensation).apply();
    }

    public boolean isCalibrated() {
        return prefs.getBoolean(KEY_IS_CALIBRATED, false);
    }

    public void saveCalibrated(boolean calibrated) {
        prefs.edit().putBoolean(KEY_IS_CALIBRATED, calibrated).apply();
    }
}
