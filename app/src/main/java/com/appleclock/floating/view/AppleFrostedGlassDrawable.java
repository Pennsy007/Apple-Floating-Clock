package com.appleclock.floating.view;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Random;

/**
 * 苹果官方灵动岛微晶毛玻璃渲染器 (Apple Liquid Frosted Glass Engine)
 * - 纳米级物理磨砂颗粒漫反射纹理 (Nano Frosted Grain Shader)
 * - 多层菲涅尔次表面透光梯度 (Fresnel Translucency Gradient)
 * - 顶部天光月牙反射曲面 (Top Convex Specular Highlight)
 * - 1.2dp 苹果双轮廓水晶高光线 (Apple Dual-Rim Specular Bevel)
 * - 纯 GPU 硬件加速绘制，零卡顿零功耗
 */
public class AppleFrostedGlassDrawable extends Drawable {

    private static Bitmap cachedNoiseBitmap;
    private static BitmapShader cachedNoiseShader;

    private int themeIndex = 1; // 默认暗夜黑曜石
    private float opacity = 0.95f;
    private float density = 3.0f;

    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint noisePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF bodyRect = new RectF();
    private final RectF glossRect = new RectF();
    private final RectF borderRect = new RectF();
    private final RectF shadowRect = new RectF();

    public AppleFrostedGlassDrawable(float displayDensity) {
        this.density = displayDensity > 0 ? displayDensity : 3.0f;
        initPaints();
    }

    private static synchronized void ensureNoiseShader() {
        if (cachedNoiseShader != null) return;
        int size = 64;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Random rng = new Random(5678);
        int[] pixels = new int[size * size];
        for (int i = 0; i < pixels.length; i++) {
            int lum = 220 + rng.nextInt(36); // 微亮颗粒
            int alpha = 8 + rng.nextInt(14);  // 极轻微纳米磨砂感，克制且质感出众
            pixels[i] = (alpha << 24) | (lum << 16) | (lum << 8) | lum;
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size);
        cachedNoiseBitmap = bmp;
        cachedNoiseShader = new BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    private void initPaints() {
        ensureNoiseShader();
        noisePaint.setShader(cachedNoiseShader);
        noisePaint.setStyle(Paint.Style.FILL);

        basePaint.setStyle(Paint.Style.FILL);
        glossPaint.setStyle(Paint.Style.FILL);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.2f * density);

        shadowPaint.setStyle(Paint.Style.FILL);
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
        updateRectsAndShaders();
    }

    private void updateRectsAndShaders() {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        float padShadow = 2.0f * density;
        shadowRect.set(bounds.left, bounds.top + 1.5f * density, bounds.right, bounds.bottom + 3.5f * density);

        bodyRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom);

        float strokeW = 1.2f * density;
        borderRect.set(bodyRect.left + strokeW / 2f, bodyRect.top + strokeW / 2f,
                       bodyRect.right - strokeW / 2f, bodyRect.bottom - strokeW / 2f);

        float glossH = bodyRect.height() * 0.46f;
        glossRect.set(bodyRect.left + 1f * density, bodyRect.top + 0.8f * density,
                      bodyRect.right - 1f * density, bodyRect.top + glossH);

        updateShaders();
    }

    private void updateShaders() {
        if (bodyRect.width() <= 0 || bodyRect.height() <= 0) return;

        // 柔和外围立体投影
        shadowPaint.setColor(Color.argb((int) (42 * opacity), 0, 0, 0));

        int colorStart, colorMid, colorEnd;
        int borderStart, borderMid, borderEnd;
        int glossStart, glossEnd;

        int alphaBase = (int) (190 * opacity); // 约 75% 通透度，优雅透底

        switch (themeIndex) {
            case 0: // 冰霜雪晶白 (Apple Ice Glacier)
                colorStart = Color.argb(alphaBase, 255, 255, 255);
                colorMid = Color.argb((int) (210 * opacity), 244, 245, 248);
                colorEnd = Color.argb((int) (225 * opacity), 232, 235, 240);

                borderStart = Color.argb(230, 255, 255, 255);
                borderMid = Color.argb(160, 255, 255, 255);
                borderEnd = Color.argb(90, 255, 255, 255);

                glossStart = Color.argb(90, 255, 255, 255);
                glossEnd = Color.argb(0, 255, 255, 255);
                break;

            case 2: // 远峰冷雾蓝 (Apple Sierra Mist Blue)
                colorStart = Color.argb(alphaBase, 22, 36, 48);
                colorMid = Color.argb((int) (210 * opacity), 16, 28, 40);
                colorEnd = Color.argb((int) (225 * opacity), 10, 18, 26);

                borderStart = Color.argb(150, 160, 210, 255);
                borderMid = Color.argb(80, 110, 160, 210);
                borderEnd = Color.argb(30, 80, 120, 170);

                glossStart = Color.argb(60, 200, 230, 255);
                glossEnd = Color.argb(0, 200, 230, 255);
                break;

            case 3: // 晚霞暖金 (Apple Hermès Sunset Amber)
                colorStart = Color.argb(alphaBase, 36, 26, 18);
                colorMid = Color.argb((int) (210 * opacity), 28, 20, 14);
                colorEnd = Color.argb((int) (225 * opacity), 20, 14, 10);

                borderStart = Color.argb(160, 245, 205, 160);
                borderMid = Color.argb(80, 210, 160, 110);
                borderEnd = Color.argb(30, 170, 120, 80);

                glossStart = Color.argb(60, 255, 230, 200);
                glossEnd = Color.argb(0, 255, 230, 200);
                break;

            case 4: // 暮光幻紫 (Apple Twilight Violet)
                colorStart = Color.argb(alphaBase, 32, 21, 43);
                colorMid = Color.argb((int) (210 * opacity), 24, 14, 34);
                colorEnd = Color.argb((int) (225 * opacity), 16, 8, 24);

                borderStart = Color.argb(160, 210, 170, 255);
                borderMid = Color.argb(80, 160, 110, 210);
                borderEnd = Color.argb(30, 120, 80, 170);

                glossStart = Color.argb(60, 240, 215, 255);
                glossEnd = Color.argb(0, 240, 215, 255);
                break;

            case 1: // 暗夜黑曜石 (Apple Midnight Glass - 经典灵动岛)
            default:
                colorStart = Color.argb(alphaBase, 26, 26, 30);
                colorMid = Color.argb((int) (215 * opacity), 16, 16, 20);
                colorEnd = Color.argb((int) (230 * opacity), 10, 10, 14);

                borderStart = Color.argb(140, 255, 255, 255);
                borderMid = Color.argb(65, 255, 255, 255);
                borderEnd = Color.argb(20, 255, 255, 255);

                glossStart = Color.argb(65, 255, 255, 255);
                glossEnd = Color.argb(0, 255, 255, 255);
                break;
        }

        LinearGradient baseGradient = new LinearGradient(
            bodyRect.centerX(), bodyRect.top,
            bodyRect.centerX(), bodyRect.bottom,
            new int[]{colorStart, colorMid, colorEnd},
            new float[]{0f, 0.5f, 1f},
            Shader.TileMode.CLAMP
        );
        basePaint.setShader(baseGradient);

        LinearGradient borderGradient = new LinearGradient(
            borderRect.centerX(), borderRect.top,
            borderRect.centerX(), borderRect.bottom,
            new int[]{borderStart, borderMid, borderEnd},
            new float[]{0f, 0.45f, 1f},
            Shader.TileMode.CLAMP
        );
        borderPaint.setShader(borderGradient);

        LinearGradient glossGradient = new LinearGradient(
            glossRect.centerX(), glossRect.top,
            glossRect.centerX(), glossRect.bottom,
            glossStart, glossEnd,
            Shader.TileMode.CLAMP
        );
        glossPaint.setShader(glossGradient);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (bodyRect.width() <= 0 || bodyRect.height() <= 0) return;

        float radius = bodyRect.height() / 2f;

        // 1. 柔和外环境阴影 (Soft Ambient Drop Shadow)
        canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint);

        // 2. 核心半透明磨砂亚克力玻璃基底 (Frosted Base)
        canvas.drawRoundRect(bodyRect, radius, radius, basePaint);

        // 3. 纳米磨砂颗粒层 (Nano Frosted Grain Shader)
        canvas.drawRoundRect(bodyRect, radius, radius, noisePaint);

        // 4. 顶部天光月牙反射曲面 (Top Convex Specular Highlight)
        float glossRadius = glossRect.height();
        canvas.drawRoundRect(glossRect, glossRadius, glossRadius, glossPaint);

        // 5. 菲涅尔双轮廓水晶高光线 (Apple Dual-Rim Specular Bevel)
        float borderRadius = borderRect.height() / 2f;
        canvas.drawRoundRect(borderRect, borderRadius, borderRadius, borderPaint);
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
