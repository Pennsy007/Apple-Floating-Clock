package com.appleclock.floating.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * 权限检查与系统跳转工具
 */
public class PermissionHelper {

    /**
     * 检查是否具备悬浮窗系统权限
     */
    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    /**
     * 跳转至系统的悬浮窗授权页面
     */
    public static void requestOverlayPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName())
            );
            activity.startActivityForResult(intent, requestCode);
        }
    }
}
