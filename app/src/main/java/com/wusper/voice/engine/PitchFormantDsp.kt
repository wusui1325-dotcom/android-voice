package com.wusper.voice.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 時域顆粒變調＋共振峰縮放，輸出時長與輸入對齊（即時鏈用）。
 */
class PitchFormantDsp {
    fun process(
        input: ShortArray,
        length: Int,
        pitchSemitones: Float,
        formantScale: Float,
    ): ShortArray {
        val pitch = 2.0.pow((pitchSemitones / 12.0)).toFloat().coerceIn(0.5f, 2.0f)
        val formant = formantScale.coerceIn(0.7f, 1.4f)
        if (length <= 4) return input.copyOf(length)

        val readRate = (pitch / formant).coerceIn(0.5f, 2.0f)
        if (kotlin.math.abs(readRate - 1f) < 0.01f && kotlin.math.abs(formant - 1f) < 0.02f) {
            return input.copyOf(length)
        }

        val grain = min(1024, length).coerceAtLeast(64)
        val hop = (grain / 4).coerceAtLeast(16)
        val out = FloatArray(length)
        val weight = FloatArray(length)
        val window = FloatArray(grain) { n ->
            (0.5 * (1.0 - cos(2.0 * PI * n / (grain - 1)))).toFloat()
        }

        var outPos = 0
        var srcPos = 0.0
        val wrapAt = (length - grain / 2).coerceAtLeast(1)
        while (outPos < length) {
            val i0 = srcPos.toInt().coerceIn(0, length - 1)
            for (g in 0 until grain) {
                val dst = outPos + g
                if (dst >= length) break
                val src = i0 + (g * readRate)
                val s = lerp(input, length, src) * window[g]
                out[dst] += s
                weight[dst] += window[g]
            }
            outPos += hop
            srcPos += hop * readRate
            if (srcPos > wrapAt) {
                srcPos %= wrapAt.toDouble()
            }
        }

        val result = ShortArray(length)
        val tilt = 1f + (formant - 1f) * 0.15f
        for (i in 0 until length) {
            val w = weight[i]
            val sample = if (w > 1e-6f) out[i] / w else input[i].toFloat()
            result[i] = (sample * tilt)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return result
    }

    private fun lerp(input: ShortArray, length: Int, index: Float): Float {
        if (index <= 0f) return input[0].toFloat()
        if (index >= length - 1) return input[length - 1].toFloat()
        val i0 = index.toInt()
        val frac = index - i0
        return input[i0].toFloat() + (input[i0 + 1] - input[i0]) * frac
    }
}
