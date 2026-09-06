package com.scarguard.app.vision

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Pure-Kotlin skin tone / redness analysis. No OpenCV or ML dependency: this is a deliberately
 * simple, explainable heuristic, good enough to flag a trend worth a human looking at -- it is
 * NOT a diagnostic tool. See [analyze] for the algorithm.
 */
object RednessAnalyzer {

    /** All photos are normalized to this size before comparison so scores are comparable. */
    private const val ANALYSIS_SIZE = 256

    // Hue is in degrees [0, 360). Red falls near 0/360 with a wrap-around.
    private const val RED_HUE_MAX = 30f
    private const val RED_HUE_MIN = 330f
    private const val MIN_SATURATION = 0.30f
    private const val MIN_VALUE = 0.20f

    data class ColorSummary(
        val rednessScore: Float, // 0..100, higher = more/stronger red-toned pixels
        val avgRed: Float,
        val avgGreen: Float,
        val avgBlue: Float,
    )

    data class Comparison(
        val baseline: ColorSummary,
        val current: ColorSummary,
        val rednessDelta: Float,   // current.rednessScore - baseline.rednessScore
        val colorShiftScore: Float, // 0..100, overall tone/darkness change vs baseline (bruising, discoloration)
        val heatmap: Bitmap,       // current photo with redder-than-baseline regions tinted
    )

    /** Summarizes a single photo. Use this once when a baseline photo is captured. */
    fun analyze(source: Bitmap): ColorSummary {
        val bmp = normalize(source)
        var redSum = 0.0
        var greenSum = 0.0
        var blueSum = 0.0
        var rednessWeightSum = 0.0
        val hsv = FloatArray(3)
        val pixels = IntArray(bmp.width * bmp.height)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)

        for (pixel in pixels) {
            redSum += Color.red(pixel)
            greenSum += Color.green(pixel)
            blueSum += Color.blue(pixel)
            Color.colorToHSV(pixel, hsv)
            rednessWeightSum += rednessWeight(hsv)
        }

        val count = pixels.size.toDouble()
        return ColorSummary(
            rednessScore = (rednessWeightSum / count * 100.0).toFloat().coerceIn(0f, 100f),
            avgRed = (redSum / count).toFloat(),
            avgGreen = (greenSum / count).toFloat(),
            avgBlue = (blueSum / count).toFloat(),
        )
    }

    /**
     * Compares a new photo against the stored baseline summary, producing deltas plus a visual
     * heatmap highlighting where the current photo is redder than the baseline was.
     */
    fun compare(
        currentPhoto: Bitmap,
        baseline: ColorSummary,
        baselinePhoto: Bitmap?,
    ): Comparison {
        val current = analyze(currentPhoto)
        val normalizedCurrent = normalize(currentPhoto)
        val normalizedBaseline = baselinePhoto?.let { normalize(it) }

        val heatmap = buildHeatmap(normalizedCurrent, normalizedBaseline)

        val colorShift = colorShiftScore(baseline, current)

        return Comparison(
            baseline = baseline,
            current = current,
            rednessDelta = current.rednessScore - baseline.rednessScore,
            colorShiftScore = colorShift,
            heatmap = heatmap,
        )
    }

    /** Weight in [0,1]: how strongly a pixel reads as inflamed/red skin. */
    private fun rednessWeight(hsv: FloatArray): Double {
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]
        if (sat < MIN_SATURATION || value < MIN_VALUE) return 0.0
        val isRedHue = hue <= RED_HUE_MAX || hue >= RED_HUE_MIN
        if (!isRedHue) return 0.0
        // Weight by how saturated the red is, so vivid red counts more than a faint pink tinge.
        return sat.toDouble()
    }

    /** 0..100 score for overall average-color drift (catches bruising/darkening, not just redness). */
    private fun colorShiftScore(a: ColorSummary, b: ColorSummary): Float {
        val dr = a.avgRed - b.avgRed
        val dg = a.avgGreen - b.avgGreen
        val db = a.avgBlue - b.avgBlue
        val distance = kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble())
        // Max possible Euclidean distance across 8-bit RGB channels is ~441.7; scale to 0..100.
        return (distance / 441.7 * 100.0).toFloat().coerceIn(0f, 100f)
    }

    private fun buildHeatmap(current: Bitmap, baseline: Bitmap?): Bitmap {
        val width = current.width
        val height = current.height
        val out = current.copy(Bitmap.Config.ARGB_8888, true)
        val currentPixels = IntArray(width * height)
        current.getPixels(currentPixels, 0, width, 0, 0, width, height)

        val baselinePixels: IntArray? = if (baseline != null && baseline.width == width && baseline.height == height) {
            IntArray(width * height).also { baseline.getPixels(it, 0, width, 0, 0, width, height) }
        } else null

        val outPixels = IntArray(width * height)
        val hsvCur = FloatArray(3)
        val hsvBase = FloatArray(3)

        for (i in currentPixels.indices) {
            val curPixel = currentPixels[i]
            Color.colorToHSV(curPixel, hsvCur)
            val curWeight = rednessWeight(hsvCur)

            val baseWeight = if (baselinePixels != null) {
                Color.colorToHSV(baselinePixels[i], hsvBase)
                rednessWeight(hsvBase)
            } else 0.0

            val delta = (curWeight - baseWeight).coerceAtLeast(0.0)
            outPixels[i] = if (delta > 0.08) {
                val alpha = (delta * 255).coerceIn(0.0, 200.0).toInt()
                blendRed(curPixel, alpha)
            } else {
                curPixel
            }
        }
        out.setPixels(outPixels, 0, width, 0, 0, width, height)
        return out
    }

    private fun blendRed(basePixel: Int, overlayAlpha: Int): Int {
        val a = overlayAlpha / 255f
        val r = (Color.red(basePixel) * (1 - a) + 255 * a).toInt().coerceIn(0, 255)
        val g = (Color.green(basePixel) * (1 - a)).toInt().coerceIn(0, 255)
        val b = (Color.blue(basePixel) * (1 - a)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun normalize(source: Bitmap): Bitmap =
        if (source.width == ANALYSIS_SIZE && source.height == ANALYSIS_SIZE) {
            source
        } else {
            Bitmap.createScaledBitmap(source, ANALYSIS_SIZE, ANALYSIS_SIZE, true)
        }
}
