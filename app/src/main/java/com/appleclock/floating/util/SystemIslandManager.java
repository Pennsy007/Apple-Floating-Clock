package com.appleclock.floating.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import com.appleclock.floating.MainActivity;
import com.appleclock.floating.R;

/**
 * 专为 vivo 原子岛 & 小米 HyperOS 焦点通知深度定制的系统级原生灵动岛上岛管理器
 * 完美支持小米澎湃OS（Focus Notification 胶囊）与 vivo OriginOS（Atomic Island 实时活动）
 */
public class SystemIslandManager {

    public static final String ISLAND_CHANNEL_ID = "hyperos_vivo_island_channel";

    private static volatile SystemIslandManager instance;

    private final Context context;
    private final NotificationManager notificationManager;
    private RemoteViews tinyViews;
    private RemoteViews bigViews;
    private PendingIntent contentIntent;
    private String lastUpdatedSecond = "";

    private SystemIslandManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        initChannel();
        initViews();
    }

    public static SystemIslandManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SystemIslandManager.class) {
                if (instance == null) {
                    instance = new SystemIslandManager(context);
                }
            }
        }
        return instance;
    }

    private void initChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                ISLAND_CHANNEL_ID,
                "灵动岛状态栏时钟",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("用于小米澎湃OS焦点通知与vivo原子岛原生上岛胶囊");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void initViews() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        contentIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        tinyViews = new RemoteViews(context.getPackageName(), R.layout.layout_island_capsule_tiny);
        bigViews = new RemoteViews(context.getPackageName(), R.layout.layout_island_capsule_big);
    }

    /**
     * 构建集成了小米 HyperOS 焦点通知 + vivo 原子岛 + 通用 Android 实时活动规范的 Notification
     */
    public Notification buildIslandNotification(String mainTime, String msTime) {
        if (tinyViews == null || bigViews == null) {
            initViews();
        }

        tinyViews.setTextViewText(R.id.tvIslandTinyTime, mainTime);
        bigViews.setTextViewText(R.id.tvIslandBigTime, mainTime);
        bigViews.setTextViewText(R.id.tvIslandBigMs, msTime);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ISLAND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock)
            .setContentTitle("北京时间 · 灵动岛")
            .setContentText(mainTime)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCustomContentView(tinyViews)
            .setCustomBigContentView(bigViews)
            .setCustomHeadsUpContentView(tinyViews);

        Notification notification = builder.build();

        // 注入厂商专属灵动岛/实时活动协议
        injectBrandProtocols(notification, tinyViews, bigViews);

        return notification;
    }

    /**
     * 针对小米澎湃OS与 vivo OriginOS 的私有底层协议注入
     */
    private void injectBrandProtocols(Notification notification, RemoteViews tiny, RemoteViews big) {
        Bundle extras = notification.extras;
        if (extras == null) {
            extras = new Bundle();
            notification.extras = extras;
        }

        // ==========================================
        // 1. 小米澎湃OS (Xiaomi HyperOS / MIUI) 焦点通知协议
        // ==========================================
        extras.putBoolean("miui.focus.isFocus", true);
        extras.putBoolean("miui.focus.isPromoted", true);
        extras.putBoolean("miui.focus.enableAlert", true);
        extras.putBoolean("miui.focus.hasCustomBg", true);
        extras.putParcelable("miui.focus.rv.tiny", tiny);
        extras.putParcelable("miui.focus.rv", big);
        extras.putParcelable("miui.focus.rvBar", tiny);

        // ==========================================
        // 2. vivo (OriginOS 4 / 5) 原子岛协议
        // ==========================================
        extras.putBoolean("vivo.summary", true);
        extras.putBoolean("vivo.atomic.island", true);
        extras.putString("vivo.notice.type", "capsule");
        extras.putParcelable("vivo.capsule.view", tiny);
        extras.putBoolean("com.vivo.notification.capsule", true);
        extras.putParcelable("com.vivo.notification.custom_capsule", tiny);
        extras.putString("com.vivo.notification.type", "capsule");

        // ==========================================
        // 3. 通用 Android 实时活动 (Live Activity) 胶囊规范
        // ==========================================
        extras.putBoolean("android.substName", true);
        extras.putBoolean("extra_capsule", true);
        extras.putParcelable("extra_capsule_view", tiny);
    }

    public boolean shouldUpdateSecond(String mainTime) {
        if (mainTime.equals(lastUpdatedSecond)) {
            return false;
        }
        lastUpdatedSecond = mainTime;
        return true;
    }

    public void stopIsland() {
        lastUpdatedSecond = "";
    }
}
