package com.wusper.voice.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 女聲取向 DSP（強化共振峰／音色，而非只改音高）。
 *
 * 流程：
 * 1) 依 formantScale 重採樣 → 拉高／壓低共振峰（同時暫改音高）
 * 2) 顆粒時域伸縮對齊輸出時長，並把音高收到 pitchSemitones 目標
 * 3) 女聲音色：高頻棚架＋輕氣息噪聲＋軟限幅
 */
class PitchFormantDsp(
    private val sampleRate: Int = 48_000,
) {
    private var hpState = 0f
    private var shelfState = 0f
    private var noisePhase = 0

    fun reset() {
        hpState = 0f
        shelfState = 0f
        noisePhase = 0
    }

    fun process(
        input: ShortArray,
        length: Int,
        pitchSemitones: Float,
        formantScale: Float,
    ): ShortArray {
        if (length <= 8) return input.copyOf(length)

        val pitchRatio = 2.0.pow((pitchSemitones / 12.0)).toFloat().coerceIn(0.5f, 2.0f)
        // 女聲預設常落在 >1；略加權讓「味道」比單純半音更明顯
        val formant = (formantScale.coerceIn(0.7f, 1.4f)).let { f ->
            if (f > 1.02f) (f * 1.06f).coerceAtMost(1.45f) else f
        }

        val mono = FloatArray(length) { input[it] / 32768f }

        // 1) 共振峰：重採樣（頻譜整體伸縮）
        val formed = resample(mono, formant)

        // 2) 伸縮回原長度，並對齊目標音高
        // formed 後音高≈原*formant；目標音高≈原*pitchRatio
        // 讀取速率 = pitchRatio/formant
        val readRate = (pitchRatio / formant).coerceIn(0.45f, 2.2f)
        val pitched = grainStretch(formed, length, readRate)

        // 3) 女聲音色塑形
        val colored = femaleTimbre(pitched, formant, pitchSemitones)

        val out = ShortArray(length)
        for (i in 0 until length) {
            out[i] = (colored[i] * 32767f)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }

    /** 線性插值重採樣：factor>1 → 更高頻／更短 */
    private fun resample(input: FloatArray, factor: Float): FloatArray {
        if (abs(factor - 1f) < 0.01f) return input.copyOf()
        val outLen = maxOf(1, (input.size / factor).roundToInt())
        val out = FloatArray(outLen)
        var i = 0
        while (i < outLen) {
            val src = i * factor
            out[i] = lerp(input, src)
            i++
        }
        return out
    }

    /**
     * 漢寧窗顆粒合成：以 [readRate] 掃過 [input]，輸出固定 [outLen]。
     * readRate>1 → 音更高（讀更快）。
     */
    private fun grainStretch(input: FloatArray, outLen: Int, readRate: Float): FloatArray {
        if (input.isEmpty()) return FloatArray(outLen)
        if (abs(readRate - 1f) < 0.015f && input.size == outLen) return input.copyOf(outLen)

        val grain = min(960, maxOf(128, outLen / 2)) // ~20ms @48k
        val hop = maxOf(64, grain / 4)
        val out = FloatArray(outLen)
        val weight = FloatArray(outLen)
        val window = FloatArray(grain) { n ->
            (0.5 * (1.0 - cos(2.0 * PI * n / (grain - 1).coerceAtLeast(1)))).toFloat()
        }

        var outPos = 0
        var srcPos = 0.0
        val maxSrc = (input.size - 2).coerceAtLeast(1).toDouble()
        while (outPos < outLen) {
            val base = srcPos.coerceIn(0.0, maxSrc)
            for (g in 0 until grain) {
                val dst = outPos + g
                if (dst >= outLen) break
                val sample = lerp(input, (base + g).toFloat()) * window[g]
                out[dst] += sample
                weight[dst] += window[g]
            }
            outPos += hop
            srcPos += hop * readRate
            if (srcPos > maxSrc) {
                // 環繞，維持即時塊長度
                srcPos %= maxSrc
            }
        }
        for (i in 0 until outLen) {
            val w = weight[i]
            out[i] = if (w > 1e-5f) out[i] / w else 0f
        }
        return out
    }

    /**
     * 高頻棚架（亮）＋輕氣息＋去低頻轟鳴，讓女聲不那麼「只是尖」。
     */
    private fun femaleTimbre(x: FloatArray, formant: Float, pitchSemitones: Float): FloatArray {
        val out = FloatArray(x.size)
        val bright = (0.18f + (formant - 1f).coerceAtLeast(0f) * 0.55f + (pitchSemitones.coerceAtLeast(0f) / 12f) * 0.12f)
            .coerceIn(0.1f, 0.55f)
        val breath = (0.02f + (formant - 1f).coerceAtLeast(0f) * 0.04f).coerceIn(0.015f, 0.07f)
        // 一階高通（去 rumble）
        val hpR = 0.995f
        // 高棚：y += bright * (x - lowpass)
        val shelfR = 0.92f

        for (i in x.indices) {
            val s = x[i]
            hpState = hpR * (hpState + s - (if (i > 0) x[i - 1] else s))
            // 簡化：用差分近似高通
            val hp = s - (if (i > 0) x[i - 1] else 0f) * 0.95f
            shelfState = shelfR * shelfState + (1f - shelfR) * s
            val high = s - shelfState
            var y = s + bright * high + 0.12f * hp

            // 輕氣息：高頻區混入少量噪聲（隨振幅）
            noisePhase = (noisePhase + 1) and 0x7fff
            val n = (Random.nextFloat() * 2f - 1f) * breath * (abs(high) * 2f + 0.02f)
            y += n

            // 軟限幅
            y = softClip(y * 1.05f)
            out[i] = y
        }
        return out
    }

    private fun softClip(x: Float): Float {
        return when {
            x > 1f -> 1f - 1f / (x + 1f) * 0.15f + 0.85f // soft
            x < -1f -> -1f + 1f / (-x + 1f) * 0.15f - 0.85f
            else -> x - (x * x * x) * 0.12f
        }.coerceIn(-1.05f, 1.05f)
    }

    private fun lerp(input: FloatArray, index: Float): Float {
        if (input.isEmpty()) return 0f
        if (index <= 0f) return input[0]
        if (index >= input.size - 1) return input[input.size - 1]
        val i0 = index.toInt()
        val frac = index - i0
        return input[i0] + (input[i0 + 1] - input[i0]) * frac
    }
}
