package com.example.jaygame.ui.battle

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

/** drawable 리소스를 디코드하여 [size]×[size]로 스케일링한 ImageBitmap 반환. */
fun decodeScaledBitmap(context: Context, @DrawableRes resId: Int, size: Int): ImageBitmap? {
    val opts = BitmapFactory.Options().apply { inScaled = false }
    return try {
        val raw = BitmapFactory.decodeResource(context.resources, resId, opts)
        if (raw != null) {
            val scaled = android.graphics.Bitmap.createScaledBitmap(raw, size, size, true)
            if (scaled !== raw) raw.recycle()
            scaled.asImageBitmap()
        } else {
            ContextCompat.getDrawable(context, resId)?.toBitmap(size, size)?.asImageBitmap()
        }
    } catch (_: Exception) {
        ContextCompat.getDrawable(context, resId)?.toBitmap(size, size)?.asImageBitmap()
    }
}

/** fx_anim/manifest.json 엔트리 */
data class AnimSheetInfo(val frames: Int, val cellW: Int = 128, val cellH: Int = 128)

/** fx_anim/manifest.json을 파싱하여 애니메이션 시트 정보 맵 반환 */
fun loadAnimManifest(context: Context): Map<String, AnimSheetInfo> {
    return try {
        val json = context.assets.open("fx_anim/manifest.json").bufferedReader().readText()
        val obj = org.json.JSONObject(json)
        val result = mutableMapOf<String, AnimSheetInfo>()
        for (key in obj.keys()) {
            val entry = obj.getJSONObject(key)
            result[key] = AnimSheetInfo(
                frames = entry.optInt("frames", 1).coerceAtLeast(1),
                cellW = entry.optInt("cellW", 128),
                cellH = entry.optInt("cellH", 128),
            )
        }
        result
    } catch (_: Exception) { emptyMap() }
}

/** assets 폴더에서 PNG를 디코드하여 ImageBitmap 반환. inScaled=false로 밀도 스케일링 방지. */
fun decodeAssetBitmap(context: Context, assetPath: String): ImageBitmap? {
    return try {
        context.assets.open(assetPath).use { stream ->
            val opts = BitmapFactory.Options().apply { inScaled = false }
            BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
        }
    } catch (_: Exception) { null }
}

/**
 * assets 폴더에서 스프라이트 시트를 로딩하여 SpriteSheetAnimator 반환.
 * 시트를 (cols * targetFrameWidth) x (rows * targetFrameHeight)로 스케일링.
 * 파일이 없으면 null 반환 (fallback to 정적 스프라이트).
 */
fun decodeAssetSpriteSheet(
    context: Context,
    assetPath: String,
    targetFrameWidth: Int,
    targetFrameHeight: Int,
    cols: Int,
    rows: Int,
): SpriteSheetAnimator? {
    val bmp = try {
        context.assets.open(assetPath).use { stream ->
            val opts = BitmapFactory.Options().apply { inScaled = false }
            BitmapFactory.decodeStream(stream, null, opts)
        }
    } catch (_: Exception) { return null }
    bmp ?: return null

    val totalW = targetFrameWidth * cols
    val totalH = targetFrameHeight * rows
    val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, totalW, totalH, true)
    if (scaled !== bmp) bmp.recycle()

    return SpriteSheetAnimator(
        sheet = scaled.asImageBitmap(),
        cols = cols,
        rows = rows,
        frameWidth = targetFrameWidth,
        frameHeight = targetFrameHeight,
    )
}
