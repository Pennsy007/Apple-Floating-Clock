package com.appleclock.floating;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.appleclock.floating.databinding.ActivityMainBinding;
import com.appleclock.floating.model.ClockConfig;
import com.appleclock.floating.service.FloatingClockService;
import com.appleclock.floating.service.TimeTicker;
import com.appleclock.floating.util.NetworkTimeManager;
import com.appleclock.floating.util.PermissionHelper;
import com.appleclock.floating.util.PreferenceManager;
import java.util.Locale;

/**
 * 悬浮时钟主控设置页面 (遵循 Apple 极简主义设计)
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_CODE = 2001;

    private ActivityMainBinding binding;
    private PreferenceManager prefManager;
    private NetworkTimeManager timeManager;
    private ClockConfig config;

    private TimeTicker previewTicker;
    private com.appleclock.floating.view.BiomimeticDewdropDrawable previewGlassDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefManager = new PreferenceManager(this);
        timeManager = NetworkTimeManager.getInstance(this);
        config = prefManager.loadConfig();

        setupViews();
        setupListeners();
        startPreviewClock();

        // 启动时静默自动同步一次北京时间
        autoSyncTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOverlayPermission();
        updateServiceButtonState();
        config = prefManager.loadConfig();
        syncConfigToViews();
        applyPreviewConfig();
    }

    private void setupViews() {
        float density = getResources().getDisplayMetrics().density;
        previewGlassDrawable = new com.appleclock.floating.view.BiomimeticDewdropDrawable(density);
        binding.previewClock.clockCapsule.setBackground(previewGlassDrawable);

        binding.previewClock.tvMainTime.setFontFeatureSettings("tnum");
        binding.previewClock.tvMsTime.setFontFeatureSettings("tnum");

        syncConfigToViews();
        applyPreviewConfig();
    }

    private void syncConfigToViews() {
        binding.switchFixedCenter.setChecked(config.isFixedCenter);
        binding.switchMilliseconds.setChecked(config.showMilliseconds);
        binding.switchIslandMode.setChecked(config.isIslandMode);

        // 缩放滑块 (0.5x ~ 2.0x，max 150)
        int scaleProgress = Math.round((config.scale - 0.5f) * 100f);
        if (scaleProgress < 0) scaleProgress = 0;
        if (scaleProgress > 150) scaleProgress = 150;
        binding.seekBarScale.setProgress(scaleProgress);
        binding.tvScaleValue.setText(String.format(Locale.getDefault(), "%.2fx", config.scale));

        updateCalibrationStatusUI();
        updateThemeSelectionUI(config.glassTheme);
    }

    private void setupListeners() {
        binding.btnGrantPermission.setOnClickListener(v ->
            PermissionHelper.requestOverlayPermission(this, REQUEST_OVERLAY_CODE)
        );

        // 一键高精授时校准
        binding.btnSyncNetworkTime.setOnClickListener(v -> manualSyncTime());

        // 固定居中/自由移动模式切换
        binding.switchFixedCenter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.isFixedCenter = isChecked;
            saveAndNotify();
        });

        // 毫秒开关
        binding.switchMilliseconds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.showMilliseconds = isChecked;
            saveAndNotify();
            applyPreviewConfig();
            if (previewTicker != null) {
                previewTicker.updateMode(config.showMilliseconds, config.is24Hour, config.showDate);
            }
        });

        // 顶部迷你贴边模式开关
        binding.switchIslandMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.isIslandMode = isChecked;
            saveAndNotify();
            applyPreviewConfig();
        });

        // 毛玻璃 5 色系切换
        binding.btnThemeWhite.setOnClickListener(v -> selectGlassTheme(0));
        binding.btnThemeDark.setOnClickListener(v -> selectGlassTheme(1));
        binding.btnThemeBlue.setOnClickListener(v -> selectGlassTheme(2));
        binding.btnThemeAmber.setOnClickListener(v -> selectGlassTheme(3));
        binding.btnThemeViolet.setOnClickListener(v -> selectGlassTheme(4));

        // 缩放 SeekBar
        binding.seekBarScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float newScale = 0.5f + (progress / 100f);
                    config.scale = newScale;
                    binding.tvScaleValue.setText(String.format(Locale.getDefault(), "%.2fx", newScale));
                    saveAndNotify();
                    applyPreviewConfig();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.btnToggleFloating.setOnClickListener(v -> toggleFloatingService());
    }

    private void selectGlassTheme(int index) {
        config.glassTheme = index;
        updateThemeSelectionUI(index);
        saveAndNotify();
        applyPreviewConfig();
    }

    private void updateThemeSelectionUI(int index) {
        binding.labelThemeWhite.setTextColor(ContextCompat.getColor(this, R.color.apple_text_secondary));
        binding.labelThemeDark.setTextColor(ContextCompat.getColor(this, R.color.apple_text_secondary));
        binding.labelThemeBlue.setTextColor(ContextCompat.getColor(this, R.color.apple_text_secondary));
        binding.labelThemeAmber.setTextColor(ContextCompat.getColor(this, R.color.apple_text_secondary));
        binding.labelThemeViolet.setTextColor(ContextCompat.getColor(this, R.color.apple_text_secondary));

        int activeColor = ContextCompat.getColor(this, R.color.apple_white);

        switch (index) {
            case 0:
                binding.tvSelectedThemeName.setText("当前材质：晨露冰晶 (Glacier Morning Dew)");
                binding.labelThemeWhite.setTextColor(activeColor);
                break;
            case 1:
                binding.tvSelectedThemeName.setText("当前材质：暗夜星曜 (Midnight Starlight)");
                binding.labelThemeDark.setTextColor(activeColor);
                break;
            case 2:
                binding.tvSelectedThemeName.setText("当前材质：冷杉林雾 (Forest Pine Mist)");
                binding.labelThemeBlue.setTextColor(activeColor);
                break;
            case 3:
                binding.tvSelectedThemeName.setText("当前材质：落日暖霞 (Sunset Amber Glow)");
                binding.labelThemeAmber.setTextColor(activeColor);
                break;
            case 4:
                binding.tvSelectedThemeName.setText("当前材质：暮夜星辰 (Twilight Aurora)");
                binding.labelThemeViolet.setTextColor(activeColor);
                break;
        }
    }

    private void autoSyncTime() {
        timeManager.syncNetworkTime(new NetworkTimeManager.SyncCallback() {
            @Override
            public void onSuccess(long offsetMs, long rttMs) {
                updateCalibrationStatusUI();
                if (FloatingClockService.isServiceRunning) {
                    FloatingClockService.reloadConfig(MainActivity.this);
                }
            }
            @Override
            public void onError(String message) {}
        });
    }

    private void manualSyncTime() {
        binding.tvCalibrationStatus.setText("正在与国家授时中心/阿里/拼多多原子钟毫秒级对齐...");
        binding.tvCalibrationStatus.setTextColor(ContextCompat.getColor(this, R.color.apple_orange));
        binding.btnSyncNetworkTime.setEnabled(false);

        timeManager.syncNetworkTime(new NetworkTimeManager.SyncCallback() {
            @Override
            public void onSuccess(long offsetMs, long rttMs) {
                binding.btnSyncNetworkTime.setEnabled(true);
                updateCalibrationStatusUI();
                Toast.makeText(MainActivity.this, "高精授时同步完成！网络时延 " + rttMs + "ms，偏差已校准", Toast.LENGTH_SHORT).show();
                if (FloatingClockService.isServiceRunning) {
                    FloatingClockService.reloadConfig(MainActivity.this);
                }
            }

            @Override
            public void onError(String message) {
                binding.btnSyncNetworkTime.setEnabled(true);
                binding.tvCalibrationStatus.setText("校准失败 (" + message + ")，已采用本地高精时钟");
                binding.tvCalibrationStatus.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.apple_red));
                Toast.makeText(MainActivity.this, "同步异常，已回退本地高精时钟", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCalibrationStatusUI() {
        if (timeManager.isCalibrated()) {
            long offset = timeManager.getNetworkOffset();
            binding.tvCalibrationStatus.setText("已对齐 · 本地时钟偏差 " + (offset >= 0 ? "+" : "") + offset + "ms · 毫秒级零延迟");
            binding.tvCalibrationStatus.setTextColor(ContextCompat.getColor(this, R.color.ios_system_green));
        } else {
            binding.tvCalibrationStatus.setText("未校准 · 点击右侧一键校准");
            binding.tvCalibrationStatus.setTextColor(ContextCompat.getColor(this, R.color.apple_text_secondary));
        }
    }

    private void applyPreviewConfig() {
        if (previewGlassDrawable != null) {
            previewGlassDrawable.setTheme(config.glassTheme);
            previewGlassDrawable.setOpacity(config.opacity);
        }

        binding.previewClock.tvMainTime.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 28 * config.scale);
        binding.previewClock.tvMsTime.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15 * config.scale);

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) ((config.isIslandMode ? 14 : 18) * density * config.scale);
        int padV = (int) ((config.isIslandMode ? 6 : 9) * density * config.scale);
        binding.previewClock.clockCapsule.setPadding(padH, padV, padH, padV);

        int textColor, msTextColor, secondaryColor, msPillBg;
        switch (config.glassTheme) {
            case 0: // 晨露冰晶 (Glacier Morning Dew)
                textColor = Color.parseColor("#1C1C1E");
                msTextColor = Color.parseColor("#007AFF");
                secondaryColor = Color.parseColor("#636366");
                msPillBg = Color.parseColor("#26007AFF");
                break;
            case 2: // 冷杉林雾 (Forest Pine Mist)
                textColor = Color.parseColor("#F2FBF6");
                msTextColor = Color.parseColor("#34C759");
                secondaryColor = Color.parseColor("#A3C7B6");
                msPillBg = Color.parseColor("#2E34C759");
                break;
            case 3: // 落日暖霞 (Sunset Amber Glow)
                textColor = Color.parseColor("#FFFDF8");
                msTextColor = Color.parseColor("#FF9F0A");
                secondaryColor = Color.parseColor("#CCA885");
                msPillBg = Color.parseColor("#2EFF9F0A");
                break;
            case 4: // 暮夜星辰 (Twilight Aurora)
                textColor = Color.parseColor("#FAF5FF");
                msTextColor = Color.parseColor("#D084F7");
                secondaryColor = Color.parseColor("#B398C8");
                msPillBg = Color.parseColor("#2EAF52DE");
                break;
            case 1: // 暗夜星曜 (Midnight Starlight - 经典)
            default:
                textColor = Color.parseColor("#F5F5F7");
                msTextColor = Color.parseColor("#30D158");
                secondaryColor = Color.parseColor("#8E8E93");
                msPillBg = Color.parseColor("#2E30D158");
                break;
        }

        binding.previewClock.tvMainTime.setTextColor(textColor);
        binding.previewClock.tvMsTime.setTextColor(msTextColor);
        binding.previewClock.tvAmPm.setTextColor(secondaryColor);
        binding.previewClock.tvDate.setTextColor(secondaryColor);

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(18 * density);
        pillBg.setColor(msPillBg);
        binding.previewClock.containerMs.setBackground(pillBg);

        binding.previewClock.containerMs.setVisibility(config.showMilliseconds ? View.VISIBLE : View.GONE);
        binding.previewClock.tvAmPm.setVisibility(!config.is24Hour ? View.VISIBLE : View.GONE);
        binding.previewClock.tvDate.setVisibility(config.showDate ? View.VISIBLE : View.GONE);
    }

    private void startPreviewClock() {
        if (previewTicker != null) {
            previewTicker.stop();
        }
        previewTicker = new TimeTicker(this, (mainTime, msTime, amPm, date) -> {
            binding.previewClock.tvMainTime.setText(mainTime);
            if (config.showMilliseconds) {
                binding.previewClock.tvMsTime.setText(msTime);
            }
            if (!config.is24Hour) {
                binding.previewClock.tvAmPm.setText(amPm);
            }
            if (config.showDate) {
                binding.previewClock.tvDate.setText(date);
            }
        });
        previewTicker.start(config.showMilliseconds, config.is24Hour, config.showDate);
    }

    private void checkOverlayPermission() {
        boolean hasPermission = PermissionHelper.canDrawOverlays(this);
        binding.cardPermission.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
    }

    private void updateServiceButtonState() {
        if (FloatingClockService.isServiceRunning) {
            binding.btnToggleFloating.setText(R.string.btn_stop_service);
            binding.btnToggleFloating.setBackgroundResource(R.drawable.bg_ios_gray_button);
            binding.btnToggleFloating.setTextColor(Color.parseColor("#FF3B30")); // iOS 红色
        } else {
            binding.btnToggleFloating.setText(R.string.btn_start_service);
            binding.btnToggleFloating.setBackgroundResource(R.drawable.bg_ios_blue_button);
            binding.btnToggleFloating.setTextColor(Color.WHITE);
        }
    }

    private void toggleFloatingService() {
        if (!PermissionHelper.canDrawOverlays(this)) {
            Toast.makeText(this, "请先授予悬浮窗显示权限", Toast.LENGTH_SHORT).show();
            PermissionHelper.requestOverlayPermission(this, REQUEST_OVERLAY_CODE);
            return;
        }

        if (FloatingClockService.isServiceRunning) {
            FloatingClockService.stop(this);
            Toast.makeText(this, "悬浮时钟已关闭", Toast.LENGTH_SHORT).show();
        } else {
            FloatingClockService.start(this);
            Toast.makeText(this, "悬浮时钟已启动，可在任意界面查看", Toast.LENGTH_SHORT).show();
        }
        binding.btnToggleFloating.postDelayed(this::updateServiceButtonState, 250);
    }

    private void saveAndNotify() {
        prefManager.saveConfig(config);
        if (FloatingClockService.isServiceRunning) {
            FloatingClockService.reloadConfig(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (previewTicker != null) {
            previewTicker.stop();
            previewTicker = null;
        }
    }
}
