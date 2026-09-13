package com.appleclock.floating.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import com.appleclock.floating.util.NetworkTimeManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 高性能高精度时间驱动引擎
 * - 毫秒精准对齐：自动监听 NetworkTimeManager 的毫秒原子授时校准；
 * - 动态对齐：一旦校准完毕立即打断重调度，秒级翻页与真实原子钟 0 延迟契合；
 * - 毫秒模式：基于 Choreographer 垂直同步驱动，120Hz 高刷丝滑无阻。
 */
public class TimeTicker {

    public interface OnTickListener {
        void onTick(String mainTime, String msTime, String amPm, String date);
    }

    private final Context context;
    private final OnTickListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final NetworkTimeManager timeManager;

    private boolean isRunning = false;
    private boolean showMilliseconds = false;
    private boolean is24Hour = true;
    private boolean showDate = false;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("M月d日 EEE", Locale.CHINA);

    private final StringBuilder timeBuffer = new StringBuilder(16);
    private final StringBuilder msBuffer = new StringBuilder(4);

    private final NetworkTimeManager.OnTimeCalibratedListener calibratedListener = offsetMs -> {
        if (isRunning) {
            resync();
        }
    };

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!isRunning || !showMilliseconds) return;
            tickNow();
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    private final Runnable secondRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning || showMilliseconds) return;
            tickNow();

            // 精准计算下一秒边界（微前瞻 +2ms，杜绝调度提前 1ms 唤醒导致秒未翻转）
            long now = getNow();
            long currentMs = now % 1000L;
            long delay = 1000L - currentMs + 2L;
            if (delay < 5L) delay += 1000L;

            handler.postDelayed(this, delay);
        }
    };

    public TimeTicker(Context context, OnTickListener listener) {
        this.context = context.getApplicationContext();
        this.timeManager = NetworkTimeManager.getInstance(this.context);
        this.listener = listener;
    }

    public void start(boolean showMilliseconds, boolean is24Hour, boolean showDate) {
        this.showMilliseconds = showMilliseconds;
        this.is24Hour = is24Hour;
        this.showDate = showDate;
        this.isRunning = true;

        if (timeManager != null) {
            timeManager.addListener(calibratedListener);
        }

        // 启动时立即刷新第一帧
        tickNow();

        if (showMilliseconds) {
            Choreographer.getInstance().postFrameCallback(frameCallback);
        } else {
            // 对齐下一次整秒更新
            scheduleNextSecond();
        }
    }

    public void resync() {
        handler.removeCallbacks(secondRunnable);
        tickNow();
        if (!showMilliseconds && isRunning) {
            scheduleNextSecond();
        }
    }

    private void scheduleNextSecond() {
        long now = getNow();
        long currentMs = now % 1000L;
        long delay = 1000L - currentMs + 2L;
        if (delay < 5L) delay += 1000L;
        handler.postDelayed(secondRunnable, delay);
    }

    public void updateMode(boolean showMilliseconds, boolean is24Hour, boolean showDate) {
        boolean modeChanged = (this.showMilliseconds != showMilliseconds);
        this.showMilliseconds = showMilliseconds;
        this.is24Hour = is24Hour;
        this.showDate = showDate;

        if (modeChanged && isRunning) {
            stop();
            start(showMilliseconds, is24Hour, showDate);
        } else if (isRunning) {
            resync();
        }
    }

    public void stop() {
        this.isRunning = false;
        if (timeManager != null) {
            timeManager.removeListener(calibratedListener);
        }
        handler.removeCallbacks(secondRunnable);
        Choreographer.getInstance().removeFrameCallback(frameCallback);
    }

    private long getNow() {
        return timeManager != null ? timeManager.getCalibratedTimeMillis() : System.currentTimeMillis();
    }

    private void tickNow() {
        long now = getNow();
        calendar.setTimeInMillis(now);

        int hour;
        if (is24Hour) {
            hour = calendar.get(Calendar.HOUR_OF_DAY);
        } else {
            hour = calendar.get(Calendar.HOUR);
            if (hour == 0) hour = 12;
        }
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = (int) (now % 1000);

        timeBuffer.setLength(0);
        appendTwoDigits(timeBuffer, hour);
        timeBuffer.append(':');
        appendTwoDigits(timeBuffer, minute);
        timeBuffer.append(':');
        appendTwoDigits(timeBuffer, second);

        msBuffer.setLength(0);
        msBuffer.append('.');
        if (millisecond < 10) {
            msBuffer.append("00");
        } else if (millisecond < 100) {
            msBuffer.append('0');
        }
        msBuffer.append(millisecond);

        String amPm = "";
        if (!is24Hour) {
            amPm = (calendar.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";
        }

        String dateStr = "";
        if (showDate) {
            dateStr = dateFormat.format(calendar.getTime());
        }

        if (listener != null) {
            listener.onTick(timeBuffer.toString(), msBuffer.toString(), amPm, dateStr);
        }
    }

    private void appendTwoDigits(StringBuilder sb, int value) {
        if (value < 10) {
            sb.append('0');
        }
        sb.append(value);
    }
}
