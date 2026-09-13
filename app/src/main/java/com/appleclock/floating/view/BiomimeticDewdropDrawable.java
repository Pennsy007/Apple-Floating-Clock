package com.appleclock.floating.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 苹果最新 UI 极致毛玻璃材质渲染器 (Apple Ultra-Thin Frosted Glass)
 * - 遵循 iOS 18 & VisionOS 官方材质规范
 * - 绝对圆润的跑道胶囊形态 (Pill Shape: radius = height / 2)
 * - 零可见生硬描边线框 (Zero Harsh Borders)
 * - 顶面物理漫反射天光折射 (Top Ambient Sheen)
 * - 内置极微弱星晶折射纹理 (Subtle Crystal Sheen，完全不占任何布局空间)
 */
public class BiomimeticDewdropDrawable extends Drawable {

    private int themeIndex = 1; // 默认经典暗夜毛玻璃
    private float opacity = 0.95f;
    private final float density;

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint topSheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint stardustPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF bodyRect = new RectF();
    private final Path clipPath = new Path();

    public BiomimeticDewdropDrawable(float displayDensity) {
        this.density = displayDensity > 0 ? displayDensity : 3.0f;
        initPaints();
    }

    private void initPaints() {
        basePaint.setStyle(Paint.Style.FILL);
        topSheenPaint.setStyle(Paint.Style.FILL);
        stardustPaint.setStyle(Paint.Style.FILL);
    }

    public void setTheme(int theme) {
        this.themeIndex = theme;
        updateShaders();
        invalidateSelf();
    }

    public void setOpacity(float op) {
        this.opacity = Math.max(0.3f, Math.min(1.0f, op));
        updateShaders();
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        bodyRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom);

        // 统一构建完美跑道胶囊裁切路径 (两头绝对正半圆，曲率丝滑连续)
        float radius = bodyRect.height() / 2f;
        clipPath.reset();
        clipPath.addRoundRect(bodyRect, radius, radius, Path.Direction.CW);

        updateShaders();
    }

    private void updateShaders() {
        if (bodyRect.width() <= 0 || bodyRect.height() <= 0) return;

        int colorStart, colorEnd;
        int sheenStart, sheenEnd;
        int stardustColor;

        int baseAlpha = (int) (185 * opacity); // 约 72% 经典苹果通透半透明
        int darkEndAlpha = (int) (205 * opacity);

        switch (themeIndex) {
            case 0: // 冰霜晶透白 (Apple Glacier White)
                colorStart = Color.argb(baseAlpha, 255, 255, 255);
                colorEnd = Color.argb((int) (175 * opacity), 235, 238, 242);
                sheenStart = Color.argb(90, 255, 255, 255);
                sheenEnd = Color.argb(0, 255, 255, 255);
                stardustColor = Color.argb(60, 0, 122, 255);
                break;

            case 2: // 冷杉林雾 (Forest Pine Mist)
                colorStart = Color.argb(baseAlpha, 20, 32, 28);
                colorEnd = Color.argb(darkEndAlpha, 10, 18, 16);
                sheenStart = Color.argb(35, 180, 240, 210);
                sheenEnd = Color.argb(0, 180, 240, 210);
                stardustColor = Color.argb(45, 52, 199, 89);
                break;

            case 3: // 晚霞暖金 (Sunset Amber Glow)
                colorStart = Color.argb(baseAlpha, 34, 24, 18);
                colorEnd = Color.argb(darkEndAlpha, 18, 12, 8);
                sheenStart = Color.argb(40, 255, 230, 190);
                sheenEnd = Color.argb(0, 255, 230, 190);
                stardustColor = Color.argb(45, 255, 159, 10);
                break;

            case 4: // 暮夜星辰 (Twilight Aurora)
                colorStart = Color.argb(baseAlpha, 28, 18, 36);
                colorEnd = Color.argb(darkEndAlpha, 14, 8, 20);
                sheenStart = Color.argb(38, 240, 210, 255);
                sheenEnd = Color.argb(0, 240, 210, 255);
                stardustColor = Color.argb(45, 208, 132, 247);
                break;

            case 1: // 经典暗夜黑曜 (Apple Midnight Dark - 官方默认)
            default:
                colorStart = Color.argb(baseAlpha, 28, 30, 34);
                colorEnd = Color.argb(darkEndAlpha, 14, 16, 18);
                sheenStart = Color.argb(32, 255, 255, 255);
                sheenEnd = Color.argb(0, 255, 255, 255);
                stardustColor = Color.argb(40, 255, 255, 255);
                break;
        }

        // 1. 底层高通透毛玻璃物理材质渐变
        LinearGradient baseGradient = new LinearGradient(
            bodyRect.centerX(), bodyRect.top,
            bodyRect.centerX(), bodyRect.bottom,
            colorStart, colorEnd,
            Shader.TileMode.CLAMP
        );
        basePaint.setShader(baseGradient);

        // 2. 顶面自然天光柔漫反射 (顺滑衰减，无任何线框)
        LinearGradient sheenGradient = new LinearGradient(
            bodyRect.centerX(), bodyRect.top,
            bodyRect.centerX(), bodyRect.top + bodyRect.height() * 0.48f,
            sheenStart, sheenEnd,
            Shader.TileMode.CLAMP
        );
        topSheenPaint.setShader(sheenGradient);

        stardustPaint.setColor(stardustColor);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (bodyRect.width() <= 0 || bodyRect.height() <= 0) return;

        int saveCount = canvas.save();
        // 关键：所有光效材质统一严格裁剪在完美胶囊内部，保证两端绝对正半圆
        canvas.clipPath(clipPath);

        // 1. 苹果毛玻璃主体材质 (纯净半透明)
        canvas.drawRect(bodyRect, basePaint);

        // 2. 顶面漫反射柔光 (完全跟随胶囊弧度自然衰减，0 边框)
        canvas.drawRect(bodyRect.left, bodyRect.top, bodyRect.right, bodyRect.top + bodyRect.height() * 0.5f, topSheenPaint);

        // 3. 背景底层两颗幽微晶莹的星晶点缀 (极小半径 1.2dp，不占任何空间，若隐若现)
        float star1X = bodyRect.right - 14f * density;
        float star1Y = bodyRect.top + 8f * density;
        canvas.drawCircle(star1X, star1Y, 1.2f * density, stardustPaint);

        float star2X = bodyRect.left + 16f * density;
        float star2Y = bodyRect.bottom - 9f * density;
        canvas.drawCircle(star2X, star2Y, 0.9f * density, stardustPaint);

        canvas.restoreToCount(saveCount);
    }

    @Override
    public void setAlpha(int alpha) {
        setOpacity(alpha / 255f);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        basePaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
