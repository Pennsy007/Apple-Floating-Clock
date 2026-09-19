package com.appleclock.floating.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;
import com.appleclock.floating.MainActivity;
import com.appleclock.floating.R;
import com.appleclock.floating.model.ClockConfig;
import com.appleclock.floating.util.PermissionHelper;
import com.appleclock.floating.util.PreferenceManager;
import com.appleclock.floating.util.SystemIslandManager;
import com.appleclock.floating.view.FloatingClockLayout;

/**
 * 悬浮时钟前台常驻服务
 * 负责在系统顶层创建、挂载、更新 WindowManager 悬浮视图并保持后台长久运行
 */
public class FloatingClockService extends Service {

    public static final String CHANNEL_ID = "floating_clock_channel";
    public static final int NOTIFICATION_ID = 101;

    public static final String ACTION_START = "com.appleclock.floating.ACTION_START";
    public static final String ACTION_STOP = "com.appleclock.floating.ACTION_STOP";
    public static final String ACTION_RELOAD_CONFIG = "com.appleclock.floating.ACTION_RELOAD_CONFIG";

    public static volatile boolean isServiceRunning = false;

    public static void start(Context context) {
        Intent intent = new Intent(context, FloatingClockService.class);
        intent.setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, FloatingClockService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static void reloadConfig(Context context) {
        Intent intent = new Intent(context, FloatingClockService.class);
        intent.setAction(ACTION_RELOAD_CONFIG);
        context.startService(intent);
    }

    private WindowManager windowManager;
    private PreferenceManager prefManager;

    // 自由移动时钟
    private FloatingClockLayout freeClockView;
    private WindowManager.LayoutParams freeClockParams;

    // 固定居中时钟
    private FloatingClockLayout centerClockView;
    private WindowManager.LayoutParams centerClockParams;

    private TimeTicker timeTicker;
    private ClockConfig currentConfig = new ClockConfig();

    private final android.content.BroadcastReceiver screenStateReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent != null ? intent.getAction() : null;
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // 息屏休眠：完全切断时间轮询与垂直同步回调，CPU 深度休眠，0 功耗
                if (timeTicker != null) {
                    timeTicker.stop();
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                // 亮屏唤醒：毫秒级恢复调度
                if ((freeClockView != null || centerClockView != null) && isServiceRunning) {
                    startTimeTicker();
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        prefManager = new PreferenceManager(this);
        currentConfig = prefManager.loadConfig();

        createNotificationChannel();
        try {
            startForeground(NOTIFICATION_ID, buildNotification());
            isServiceRunning = true;
        } catch (Exception e) {
            android.util.Log.e("FloatingClockService", "Error in startForeground", e);
        }

        // 注册息屏与亮屏动态广播
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenStateReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            startForeground(NOTIFICATION_ID, buildNotification());
            isServiceRunning = true;
        } catch (Exception ignored) {}

        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        } else if (ACTION_RELOAD_CONFIG.equals(action)) {
            currentConfig = prefManager.loadConfig();
            syncWindowsWithConfig();

            if (timeTicker != null) {
                timeTicker.updateMode(
                    currentConfig.showMilliseconds,
                    currentConfig.is24Hour,
                    currentConfig.showDate
                );
            }
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                try {
                    nm.notify(NOTIFICATION_ID, buildNotification());
                } catch (Exception ignored) {}
            }
            return START_STICKY;
        } else {
            currentConfig = prefManager.loadConfig();
            if (!currentConfig.enableCenterClock && !currentConfig.enableFreeClock) {
                currentConfig.enableCenterClock = true;
                currentConfig.enableFreeClock = true;
                prefManager.saveConfig(currentConfig);
            }
            syncWindowsWithConfig();
            startTimeTicker();
        }

        return START_STICKY;
    }

    private int getLayoutType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }

    private void syncWindowsWithConfig() {
        Context themedContext = new androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_FloatingClock);
        float density = getResources().getDisplayMetrics().density;
        int layoutType = getLayoutType();
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        // 1. 固定居中时钟管理
        if (currentConfig.enableCenterClock) {
            if (centerClockView == null) {
                centerClockParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    flags,
                    PixelFormat.TRANSLUCENT
                );
                centerClockParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                centerClockParams.x = 0;
                centerClockParams.y = (int) (110 * density);

                centerClockView = new FloatingClockLayout(themedContext);
                centerClockView.setFixedCenterMode(true);
                centerClockView.attachWindow(windowManager, centerClockParams);
                centerClockView.applyConfig(currentConfig);

                centerClockView.onScaleChangeListener = scale -> {
                    currentConfig.scale = scale;
                    prefManager.updateScale(scale);
                    if (freeClockView != null) freeClockView.applyConfig(currentConfig);
                };

                centerClockView.onToggleMsListener = showMs -> {
                    currentConfig.showMilliseconds = showMs;
                    prefManager.saveConfig(currentConfig);
                    if (freeClockView != null) freeClockView.applyConfig(currentConfig);
                    if (timeTicker != null) {
                        timeTicker.updateMode(showMs, currentConfig.is24Hour, currentConfig.showDate);
                    }
                };

                centerClockView.onLockChangeListener = locked -> {
                    currentConfig.isLocked = locked;
                    prefManager.saveConfig(currentConfig);
                };

                centerClockView.onCloseClickListener = () -> {
                    currentConfig.enableCenterClock = false;
                    prefManager.saveConfig(currentConfig);
                    removeCenterClock();
                    checkIfAllClosed();
                };

                try {
                    windowManager.addView(centerClockView, centerClockParams);
                } catch (Exception e) {
                    android.util.Log.e("FloatingClockService", "Failed to add center clock", e);
                }
            } else {
                centerClockView.applyConfig(currentConfig);
                try {
                    windowManager.updateViewLayout(centerClockView, centerClockParams);
                } catch (Exception ignored) {}
            }
        } else {
            removeCenterClock();
        }

        // 2. 自由移动时钟管理
        if (currentConfig.enableFreeClock) {
            if (freeClockView == null) {
                freeClockParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    flags,
                    PixelFormat.TRANSLUCENT
                );
                freeClockParams.gravity = Gravity.TOP | Gravity.START;
                freeClockParams.x = (currentConfig.posX != -1) ? currentConfig.posX : (int) (60 * density);
                freeClockParams.y = (currentConfig.posY != -1) ? currentConfig.posY : (int) (320 * density);

                freeClockView = new FloatingClockLayout(themedContext);
                freeClockView.setFixedCenterMode(false);
                freeClockView.attachWindow(windowManager, freeClockParams);
                freeClockView.applyConfig(currentConfig);

                freeClockView.onPositionChangeListener = (x, y) -> {
                    currentConfig.posX = x;
                    currentConfig.posY = y;
                    prefManager.updatePosition(x, y);
                };

                freeClockView.onScaleChangeListener = scale -> {
                    currentConfig.scale = scale;
                    prefManager.updateScale(scale);
                    if (centerClockView != null) centerClockView.applyConfig(currentConfig);
                };

                freeClockView.onToggleMsListener = showMs -> {
                    currentConfig.showMilliseconds = showMs;
                    prefManager.saveConfig(currentConfig);
                    if (centerClockView != null) centerClockView.applyConfig(currentConfig);
                    if (timeTicker != null) {
                        timeTicker.updateMode(showMs, currentConfig.is24Hour, currentConfig.showDate);
                    }
                };

                freeClockView.onLockChangeListener = locked -> {
                    currentConfig.isLocked = locked;
                    prefManager.saveConfig(currentConfig);
                };

                freeClockView.onCloseClickListener = () -> {
                    currentConfig.enableFreeClock = false;
                    prefManager.saveConfig(currentConfig);
                    removeFreeClock();
                    checkIfAllClosed();
                };

                try {
                    windowManager.addView(freeClockView, freeClockParams);
                } catch (Exception e) {
                    android.util.Log.e("FloatingClockService", "Failed to add free clock", e);
                }
            } else {
                freeClockView.applyConfig(currentConfig);
                try {
                    windowManager.updateViewLayout(freeClockView, freeClockParams);
                } catch (Exception ignored) {}
            }
        } else {
            removeFreeClock();
        }

        checkIfAllClosed();
    }

    private void removeCenterClock() {
        if (centerClockView != null) {
            try {
                windowManager.removeView(centerClockView);
            } catch (Exception ignored) {}
            centerClockView = null;
        }
    }

    private void removeFreeClock() {
        if (freeClockView != null) {
            try {
                windowManager.removeView(freeClockView);
            } catch (Exception ignored) {}
            freeClockView = null;
        }
    }

    private void checkIfAllClosed() {
        if (centerClockView == null && freeClockView == null) {
            stopSelf();
        }
    }

    private String lastMainTime = "12:00:00";
    private String lastMsTime = ".000";

    private void startTimeTicker() {
        if (timeTicker != null) {
            timeTicker.stop();
        }
        timeTicker = new TimeTicker(this, (mainTime, msTime, amPm, date) -> {
            lastMainTime = mainTime;
            lastMsTime = msTime;
            if (centerClockView != null) {
                centerClockView.updateTime(mainTime, msTime, amPm, date);
            }
            if (freeClockView != null) {
                freeClockView.updateTime(mainTime, msTime, amPm, date);
            }
            if (currentConfig.isIslandMode) {
                SystemIslandManager islandMgr = SystemIslandManager.getInstance(FloatingClockService.this);
                if (islandMgr.shouldUpdateSecond(mainTime)) {
                    NotificationManager nm = getSystemService(NotificationManager.class);
                    if (nm != null) {
                        try {
                            nm.notify(NOTIFICATION_ID, islandMgr.buildIslandNotification(mainTime, msTime));
                        } catch (Exception ignored) {}
                    }
                }
            }
        });
        timeTicker.start(
            currentConfig.showMilliseconds,
            currentConfig.is24Hour,
            currentConfig.showDate
        );
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_desc));
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        if (currentConfig.isIslandMode) {
            return SystemIslandManager.getInstance(this).buildIslandNotification(lastMainTime, lastMsTime);
        }
        return buildNormalNotification();
    }

    private Notification buildNormalNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent stopIntent = new Intent(this, FloatingClockService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_close, getString(R.string.action_stop), pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        SystemIslandManager.getInstance(this).stopIsland();
        if (timeTicker != null) {
            timeTicker.stop();
            timeTicker = null;
        }

        try {
            unregisterReceiver(screenStateReceiver);
        } catch (Exception ignored) {}

        removeCenterClock();
        removeFreeClock();
    }
}
