package com.appleclock.floating.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.appleclock.floating.R;
import com.appleclock.floating.model.ClockConfig;

/**
 * 苹果灵动岛高定悬浮窗根容器
 * 集成灵动岛微边框胶囊、长按呼出对角拉伸手柄、iOS 物理弹性触控动效、边缘阻尼弹簧磁吸与双指无级缩放
 */
@SuppressLint("ClickableViewAccessibility")
public class FloatingClockLayout extends FrameLayout {

    public interface OnPositionChangeListener {
        void onPositionChange(int x, int y);
    }
    public interface OnScaleChangeListener {
        void onScaleChange(float scale);
    }
    public interface OnToggleMsListener {
        void onToggleMs(boolean showMs);
    }
    public interface OnLockChangeListener {
        void onLockChange(boolean locked);
    }
    public interface OnCloseClickListener {
        void onCloseClick();
    }

    public final LinearLayout clockRoot;
    public final LinearLayout clockCapsule;
    public final LinearLayout layoutControls;
    public final LinearLayout containerMs;
    private final FrameLayout layoutResizeHandle;
    private final BiomimeticDewdropDrawable capsuleGlassDrawable;
    private final BiomimeticDewdropDrawable controlsGlassDrawable;

    public final TextView tvMainTime;
    public final TextView tvMsTime;
    public final TextView tvAmPm;
    public final TextView tvDate;

    private final TextView btnToggleMs;
    private final ImageView btnZoomOut;
    private final ImageView btnZoomIn;
    private final ImageView btnLock;
    private final ImageView btnClose;

    private ClockConfig config = new ClockConfig();

    public OnPositionChangeListener onPositionChangeListener;
    public OnScaleChangeListener onScaleChangeListener;
    public OnToggleMsListener onToggleMsListener;
    public OnLockChangeListener onLockChangeListener;
    public OnCloseClickListener onCloseClickListener;

    private WindowManager windowManager;
    private WindowManager.LayoutParams windowLayoutParams;

    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;

    private int initialX = 0;
    private int initialY = 0;
    private float initialTouchX = 0f;
    private float initialTouchY = 0f;
    private float currentRawX = 0f;
    private float currentRawY = 0f;
    private boolean isDragging = false;
    private boolean isScaling = false;

    // ========== 长按对角拉伸缩放交互系统 ==========
    private boolean isResizeMode = false;         // 是否处于编辑/尺寸调整模式
    private boolean isDirectResizing = false;     // 是否正在对角拉动中
    private float resizeAnchorX = 0f;             // 拉伸锚点 X
    private float resizeAnchorY = 0f;             // 拉伸锚点 Y
    private float resizeBaseScale = 1.0f;         // 开始拉伸时的基础 scale
    private float livePreviewScale = 1.0f;        // 实时拖拉预览目标尺寸
    private ValueAnimator resizeGlowAnimator;     // 呼吸发光动画
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private static final long LONG_PRESS_TIMEOUT = 420; // 420ms 长按判定
    private static final float TOUCH_SLOP = 32f;         // 32px 防手抖容差

    private final Runnable longPressRunnable = () -> {
        if (!isScaling && !isDragging && !config.isLocked) {
            enterResizeMode(currentRawX, currentRawY);
        }
    };

    public FloatingClockLayout(Context context) {
        this(context, null);
    }

    public FloatingClockLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingClockLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View bindingView = LayoutInflater.from(context).inflate(R.layout.view_floating_clock, this, true);

        clockRoot = bindingView.findViewById(R.id.clockRoot);
        clockCapsule = bindingView.findViewById(R.id.clockCapsule);
        layoutControls = bindingView.findViewById(R.id.layoutControls);
        containerMs = bindingView.findViewById(R.id.containerMs);
        layoutResizeHandle = bindingView.findViewById(R.id.layoutResizeHandle);

        float density = context.getResources().getDisplayMetrics().density;
        capsuleGlassDrawable = new BiomimeticDewdropDrawable(density);
        controlsGlassDrawable = new BiomimeticDewdropDrawable(density);
        clockCapsule.setBackground(capsuleGlassDrawable);
        layoutControls.setBackground(controlsGlassDrawable);

        tvMainTime = bindingView.findViewById(R.id.tvMainTime);
        tvMsTime = bindingView.findViewById(R.id.tvMsTime);
        tvAmPm = bindingView.findViewById(R.id.tvAmPm);
        tvDate = bindingView.findViewById(R.id.tvDate);

        // 开启专业级等宽数字排版 (Tabular Numbers)，彻底告别丑陋生硬的 monospace
        tvMainTime.setFontFeatureSettings("tnum");
        tvMsTime.setFontFeatureSettings("tnum");

        btnToggleMs = bindingView.findViewById(R.id.btnToggleMs);
        btnZoomOut = bindingView.findViewById(R.id.btnZoomOut);
        btnZoomIn = bindingView.findViewById(R.id.btnZoomIn);
        btnLock = bindingView.findViewById(R.id.btnLock);
        btnClose = bindingView.findViewById(R.id.btnClose);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
                isDragging = false;
                cancelLongPressTimer();
                performHaptic();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float newScale = Math.min(1.8f, Math.max(0.6f, config.scale * detector.getScaleFactor()));
                if (Math.abs(newScale - config.scale) > 0.015f) {
                    applyScale(newScale);
                    if (onScaleChangeListener != null) {
                        onScaleChangeListener.onScaleChange(newScale);
                    }
                }
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScaling = false;
                performHaptic();
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isResizeMode) {
                    exitResizeMode();
                    return true;
                }
                toggleControls();
                performHaptic();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isResizeMode) {
                    exitResizeMode();
                    return true;
                }
                boolean nextMs = !config.showMilliseconds;
                config.showMilliseconds = nextMs;
                updateDisplayMode();
                if (onToggleMsListener != null) {
                    onToggleMsListener.onToggleMs(nextMs);
                }
                // 苹果双击弹簧微动效
                clockCapsule.animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(120)
                    .withEndAction(() -> clockCapsule.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(180)
                        .setInterpolator(new OvershootInterpolator(1.4f))
                        .start())
                    .start();
                performHaptic();
                return true;
            }
        });

        setupButtons();
    }

    // ========== 进入长按尺寸调整模式 ==========
    private void enterResizeMode(float startX, float startY) {
        isResizeMode = true;
        isDirectResizing = true;
        resizeAnchorX = startX;
        resizeAnchorY = startY;
        resizeBaseScale = config.scale;
        livePreviewScale = config.scale;

        // 强震动反馈，给用户明确物理触感
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        // 显示右下角拉伸把手（伴随果味回弹浮现）
        if (layoutResizeHandle != null) {
            layoutResizeHandle.setTranslationX(0f);
            layoutResizeHandle.setTranslationY(0f);
            layoutResizeHandle.setVisibility(View.VISIBLE);
            layoutResizeHandle.setScaleX(0.2f);
            layoutResizeHandle.setScaleY(0.2f);
            layoutResizeHandle.setAlpha(0f);
            layoutResizeHandle.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(240)
                .setInterpolator(new OvershootInterpolator(2.0f))
                .start();
        }

        // 启动手柄高光呼吸动效（绝不改动胶囊尺寸，杜绝抖动）
        startResizeGlowAnimation();
    }

    // ========== 退出尺寸调整模式 ==========
    public void exitResizeMode() {
        if (!isResizeMode) return;
        isResizeMode = false;
        isDirectResizing = false;

        stopResizeGlowAnimation();

        // 恢复硬件矩阵
        if (clockCapsule != null) {
            clockCapsule.setScaleX(1.0f);
            clockCapsule.setScaleY(1.0f);
        }
        if (layoutResizeHandle != null) {
            layoutResizeHandle.animate()
                .scaleX(0.2f)
                .scaleY(0.2f)
                .alpha(0f)
                .translationX(0f)
                .translationY(0f)
                .setDuration(180)
                .withEndAction(() -> layoutResizeHandle.setVisibility(View.GONE))
                .start();
        }

        performHaptic();

        if (onScaleChangeListener != null) {
            onScaleChangeListener.onScaleChange(config.scale);
        }
    }

    private void startResizeGlowAnimation() {
        stopResizeGlowAnimation();
        // 呼吸动效只作用于右下角拉伸把手的光晕透明度，100% 杜绝时钟内容抖动
        if (layoutResizeHandle == null) return;
        resizeGlowAnimator = ValueAnimator.ofFloat(0.6f, 1.0f);
        resizeGlowAnimator.setDuration(700);
        resizeGlowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        resizeGlowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        resizeGlowAnimator.addUpdateListener(anim -> {
            if (layoutResizeHandle != null && isResizeMode && !isDirectResizing) {
                float f = (float) anim.getAnimatedValue();
                layoutResizeHandle.setAlpha(f);
            }
        });
        resizeGlowAnimator.start();
    }

    private void stopResizeGlowAnimation() {
        if (resizeGlowAnimator != null) {
            resizeGlowAnimator.cancel();
            resizeGlowAnimator = null;
        }
        if (layoutResizeHandle != null) {
            layoutResizeHandle.setAlpha(1.0f);
        }
    }

    private void cancelLongPressTimer() {
        longPressHandler.removeCallbacks(longPressRunnable);
    }

    private void setupButtons() {
        btnToggleMs.setOnClickListener(v -> {
            boolean nextMs = !config.showMilliseconds;
            config.showMilliseconds = nextMs;
            updateDisplayMode();
            if (onToggleMsListener != null) {
                onToggleMsListener.onToggleMs(nextMs);
            }
            performHaptic();
        });

        btnZoomIn.setOnClickListener(v -> {
            float newScale = Math.min(2.5f, config.scale + 0.15f);
            applyScale(newScale);
            if (onScaleChangeListener != null) {
                onScaleChangeListener.onScaleChange(newScale);
            }
            performHaptic();
        });

        btnZoomOut.setOnClickListener(v -> {
            float newScale = Math.max(0.5f, config.scale - 0.15f);
            applyScale(newScale);
            if (onScaleChangeListener != null) {
                onScaleChangeListener.onScaleChange(newScale);
            }
            performHaptic();
        });

        btnLock.setOnClickListener(v -> {
            config.isLocked = !config.isLocked;
            updateLockIcon();
            if (onLockChangeListener != null) {
                onLockChangeListener.onLockChange(config.isLocked);
            }
            performHaptic();
        });

        btnClose.setOnClickListener(v -> {
            performHaptic();
            if (onCloseClickListener != null) {
                onCloseClickListener.onCloseClick();
            }
        });
    }

    public void attachWindow(WindowManager wm, WindowManager.LayoutParams params) {
        this.windowManager = wm;
        this.windowLayoutParams = params;
    }

    public void applyConfig(ClockConfig newConfig) {
        this.config = newConfig;
        applyGlassTheme(newConfig.glassTheme);
        applyScale(newConfig.scale);
        applyOpacity(newConfig.opacity);
        updateDisplayMode();
        updateLockIcon();
    }

    public void applyGlassTheme(int themeIndex) {
        config.glassTheme = themeIndex;
        int textColor;
        int msTextColor;
        int secondaryColor;
        int iconTint;
        int controlTextColor;
        int msPillBg;

        switch (themeIndex) {
            case 0: // 晨露冰晶 (Glacier Morning Dew)
                textColor = Color.parseColor("#1C1C1E");
                msTextColor = Color.parseColor("#007AFF");
                secondaryColor = Color.parseColor("#636366");
                iconTint = Color.parseColor("#3A3A3C");
                controlTextColor = Color.parseColor("#1C1C1E");
                msPillBg = Color.parseColor("#26007AFF");
                break;
            case 2: // 冷杉林雾 (Forest Pine Mist)
                textColor = Color.parseColor("#F2FBF6");
                msTextColor = Color.parseColor("#34C759");
                secondaryColor = Color.parseColor("#A3C7B6");
                iconTint = Color.parseColor("#D0EBDD");
                controlTextColor = Color.parseColor("#F2FBF6");
                msPillBg = Color.parseColor("#2E34C759");
                break;
            case 3: // 落日暖霞 (Sunset Amber Glow)
                textColor = Color.parseColor("#FFFDF8");
                msTextColor = Color.parseColor("#FF9F0A");
                secondaryColor = Color.parseColor("#CCA885");
                iconTint = Color.parseColor("#F0D5BE");
                controlTextColor = Color.parseColor("#FFFDF8");
                msPillBg = Color.parseColor("#2EFF9F0A");
                break;
            case 4: // 暮夜星辰 (Twilight Aurora)
                textColor = Color.parseColor("#FAF5FF");
                msTextColor = Color.parseColor("#D084F7");
                secondaryColor = Color.parseColor("#B398C8");
                iconTint = Color.parseColor("#D4BDE6");
                controlTextColor = Color.parseColor("#FAF5FF");
                msPillBg = Color.parseColor("#2EAF52DE");
                break;
            case 1: // 暗夜星曜 (Midnight Starlight - 经典)
            default:
                textColor = Color.parseColor("#F5F5F7");
                msTextColor = Color.parseColor("#30D158");
                secondaryColor = Color.parseColor("#8E8E93");
                iconTint = Color.parseColor("#D1D1D6");
                controlTextColor = Color.parseColor("#F5F5F7");
                msPillBg = Color.parseColor("#2E30D158");
                break;
        }

        if (capsuleGlassDrawable != null) {
            capsuleGlassDrawable.setTheme(themeIndex);
        }
        if (controlsGlassDrawable != null) {
            controlsGlassDrawable.setTheme(themeIndex);
        }
        tvMainTime.setTextColor(textColor);
        tvMsTime.setTextColor(msTextColor);
        tvAmPm.setTextColor(secondaryColor);
        tvDate.setTextColor(secondaryColor);

        android.graphics.PorterDuff.Mode mode = android.graphics.PorterDuff.Mode.SRC_IN;
        btnZoomOut.setColorFilter(iconTint, mode);
        btnZoomIn.setColorFilter(iconTint, mode);
        btnLock.setColorFilter(iconTint, mode);
        btnClose.setColorFilter(iconTint, mode);
        btnToggleMs.setTextColor(controlTextColor);

        containerMs.setBackgroundColor(Color.TRANSPARENT);
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(18 * density);
        pillBg.setColor(msPillBg);
        containerMs.setBackground(pillBg);
    }

    public void applyScale(float scale) {
        scale = Math.min(1.8f, Math.max(0.6f, scale));
        config.scale = scale;

        // 线性精准字号，彻底根除平方暴涨 Bug
        tvMainTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f * scale);
        tvMsTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f * scale);
        tvAmPm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * scale);
        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * scale);

        float density = getResources().getDisplayMetrics().density;

        int padH = (int) ((config.isIslandMode ? 14 : 18) * density * scale);
        int padV = (int) ((config.isIslandMode ? 6 : 9) * density * scale);
        clockCapsule.setPadding(padH, padV, padH, padV);

        if (windowManager != null && windowLayoutParams != null) {
            try {
                windowManager.updateViewLayout(this, windowLayoutParams);
            } catch (Exception ignored) {}
        }
    }

    public void applyOpacity(float opacity) {
        config.opacity = opacity;
        if (capsuleGlassDrawable != null) {
            capsuleGlassDrawable.setOpacity(opacity);
        }
        if (controlsGlassDrawable != null) {
            controlsGlassDrawable.setOpacity(opacity);
        }
        clockCapsule.setAlpha(1.0f);
    }

    public void applyTheme(String themeColor, String msColor) {
        config.themeColorHex = themeColor;
        config.msColorHex = msColor;
        try {
            tvMainTime.setTextColor(Color.parseColor(themeColor));
            tvMsTime.setTextColor(Color.parseColor(msColor));
        } catch (Exception ignored) {}
    }

    private void updateDisplayMode() {
        containerMs.setVisibility(config.showMilliseconds ? View.VISIBLE : View.GONE);
        tvAmPm.setVisibility(!config.is24Hour ? View.VISIBLE : View.GONE);
        tvDate.setVisibility(config.showDate ? View.VISIBLE : View.GONE);

        btnToggleMs.setText(config.showMilliseconds ? "毫秒:开" : "毫秒:关");
        int offColor = (config.glassTheme == 0)
                ? Color.parseColor("#8E8E93")
                : Color.parseColor("#99FFFFFF");
        try {
            btnToggleMs.setTextColor(config.showMilliseconds
                    ? Color.parseColor(config.msColorHex)
                    : offColor);
        } catch (Exception ignored) {}
    }

    private void updateLockIcon() {
        btnLock.setImageResource(config.isLocked ? R.drawable.ic_lock : R.drawable.ic_unlock);
    }

    public void updateTime(String mainTime, String msTime, String amPm, String date) {
        tvMainTime.setText(mainTime);
        if (config.showMilliseconds) {
            tvMsTime.setText(msTime);
        }
        if (!config.is24Hour) {
            tvAmPm.setText(amPm);
        }
        if (config.showDate) {
            tvDate.setText(date);
        }
    }

    private void toggleControls() {
        boolean show = (layoutControls.getVisibility() != View.VISIBLE);
        if (show) {
            layoutControls.setAlpha(0f);
            layoutControls.setVisibility(View.VISIBLE);
            layoutControls.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
        } else {
            layoutControls.animate().alpha(0f).setDuration(140).withEndAction(() -> {
                layoutControls.setVisibility(View.GONE);
            }).start();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        currentRawX = event.getRawX();
        currentRawY = event.getRawY();

        // 双指捏合缩放优先处理
        if (event.getPointerCount() > 1) {
            cancelLongPressTimer();
            if (isResizeMode) exitResizeMode();
            scaleGestureDetector.onTouchEvent(event);
            return true;
        }

        // 单指手势传递（点击与双击检测）
        gestureDetector.onTouchEvent(event);

        if (isScaling || windowManager == null || windowLayoutParams == null || config.isLocked) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = windowLayoutParams.x;
                initialY = windowLayoutParams.y;
                initialTouchX = currentRawX;
                initialTouchY = currentRawY;
                isDragging = false;

                if (isResizeMode) {
                    // 已在编辑拉伸模式下：记录当前位置为拖拉原点
                    isDirectResizing = true;
                    resizeAnchorX = currentRawX;
                    resizeAnchorY = currentRawY;
                    resizeBaseScale = config.scale;
                } else {
                    // 普通模式下：启动 420ms 长按判定
                    cancelLongPressTimer();
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);

                    // 按压微陷弹性动效
                    clockCapsule.animate()
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .setDuration(100)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float totalDx = currentRawX - initialTouchX;
                float totalDy = currentRawY - initialTouchY;

                // 1. 如果正在对角拉伸（无论是长按直接拉，还是点击手柄拉）
                if (isResizeMode && isDirectResizing) {
                    // 斜对角位移投影：向右下拖拽增加尺寸，向左上推移减小尺寸
                    float dragDist = (currentRawX - resizeAnchorX) + (currentRawY - resizeAnchorY);
                    float scaleDelta = dragDist / 500.0f;
                    float targetScale = Math.min(1.8f, Math.max(0.6f, resizeBaseScale + scaleDelta));
                    livePreviewScale = targetScale;

                    // GPU 硬件加速无级平滑缩放，完全零 Layout / 零 IPC 开销，彻底告别抖动
                    float visualRatio = targetScale / resizeBaseScale;
                    clockCapsule.setPivotX(0f);
                    clockCapsule.setPivotY(0f);
                    clockCapsule.setScaleX(visualRatio);
                    clockCapsule.setScaleY(visualRatio);

                    if (layoutResizeHandle != null) {
                        float extraW = clockCapsule.getWidth() * (visualRatio - 1f);
                        float extraH = clockCapsule.getHeight() * (visualRatio - 1f);
                        layoutResizeHandle.setTranslationX(extraW);
                        layoutResizeHandle.setTranslationY(extraH);
                    }
                    return true;
                }

                // 2. 长按防手抖判定：若位移超出 TOUCH_SLOP，取消长按并进入拖拽
                if (!isDragging && (Math.abs(totalDx) > TOUCH_SLOP || Math.abs(totalDy) > TOUCH_SLOP)) {
                    isDragging = true;
                    cancelLongPressTimer();
                }

                // 3. 普通移动拖拽
                if (isDragging) {
                    windowLayoutParams.x = (int) (initialX + totalDx);
                    windowLayoutParams.y = (int) (initialY + totalDy);
                    try {
                        windowManager.updateViewLayout(this, windowLayoutParams);
                    } catch (Exception ignored) {}
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelLongPressTimer();

                if (isResizeMode && isDirectResizing) {
                    isDirectResizing = false;
                    float dist = Math.abs(currentRawX - resizeAnchorX) + Math.abs(currentRawY - resizeAnchorY);

                    // 瞬间平稳复位 GPU 变换矩阵
                    clockCapsule.setScaleX(1.0f);
                    clockCapsule.setScaleY(1.0f);
                    if (layoutResizeHandle != null) {
                        layoutResizeHandle.setTranslationX(0f);
                        layoutResizeHandle.setTranslationY(0f);
                    }

                    if (dist > TOUCH_SLOP) {
                        // 拖动拉伸完成：一次性精准提交最终尺寸与字号排版
                        applyScale(livePreviewScale);
                        if (onScaleChangeListener != null) {
                            onScaleChangeListener.onScaleChange(livePreviewScale);
                        }
                        performHaptic();
                        exitResizeMode();
                    }
                    return true;
                }

                // 按压松手弹性回弹
                if (!isResizeMode) {
                    clockCapsule.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(220)
                        .setInterpolator(new OvershootInterpolator(1.3f))
                        .start();
                }

                if (isDragging) {
                    isDragging = false;
                    handleDragRelease();
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    /**
     * 拖拽松手处理：带有 iOS 弹簧阻尼的边缘磁吸停靠
     */
    private void handleDragRelease() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int viewWidth = getWidth();
        if (viewWidth <= 0) viewWidth = clockCapsule.getWidth();
        if (viewWidth <= 0) viewWidth = 400;

        if (config.isIslandMode) {
            int startX = windowLayoutParams.x;
            int startY = windowLayoutParams.y;
            int targetX = 0;
            int targetY = 8;

            ValueAnimator snapAnim = ValueAnimator.ofFloat(0f, 1f);
            snapAnim.setDuration(220);
            snapAnim.setInterpolator(new DecelerateInterpolator(1.8f));
            snapAnim.addUpdateListener(anim -> {
                float f = anim.getAnimatedFraction();
                if (windowManager != null && windowLayoutParams != null) {
                    windowLayoutParams.x = (int) (startX + (targetX - startX) * f);
                    windowLayoutParams.y = (int) (startY + (targetY - startY) * f);
                    try {
                        windowManager.updateViewLayout(this, windowLayoutParams);
                    } catch (Exception ignored) {}
                }
            });
            snapAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    performHaptic();
                }
            });
            snapAnim.start();
            return;
        }

        if (!config.snapToEdge) {
            if (onPositionChangeListener != null) {
                onPositionChangeListener.onPositionChange(windowLayoutParams.x, windowLayoutParams.y);
            }
            return;
        }

        int targetX;
        int centerX = windowLayoutParams.x + viewWidth / 2;
        if (centerX < screenWidth / 2) {
            targetX = 24;
        } else {
            targetX = Math.max(0, screenWidth - viewWidth - 24);
        }

        int startX = windowLayoutParams.x;
        if (Math.abs(startX - targetX) < 10) {
            if (onPositionChangeListener != null) {
                onPositionChangeListener.onPositionChange(targetX, windowLayoutParams.y);
            }
            return;
        }

        ValueAnimator snapAnim = ValueAnimator.ofInt(startX, targetX);
        snapAnim.setDuration(240);
        snapAnim.setInterpolator(new DecelerateInterpolator(1.8f));
        snapAnim.addUpdateListener(anim -> {
            if (windowManager != null && windowLayoutParams != null) {
                windowLayoutParams.x = (int) anim.getAnimatedValue();
                try {
                    windowManager.updateViewLayout(this, windowLayoutParams);
                } catch (Exception ignored) {}
            }
        });
        snapAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                performHaptic();
                if (onPositionChangeListener != null) {
                    onPositionChangeListener.onPositionChange(targetX, windowLayoutParams.y);
                }
            }
        });
        snapAnim.start();
    }

    private void performHaptic() {
        if (!config.hapticEnabled) return;
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    public ClockConfig getConfig() {
        return config;
    }
}
