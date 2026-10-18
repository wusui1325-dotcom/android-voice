package com.wusper.voice.debug

import com.wusper.voice.engine.VoiceEngine
import kotlin.math.max
import kotlin.math.min

/**
 * 端到端延遲採樣（1.0.0）。
 * 讀取 [VoiceEngine.latencyMs]，供 debug／驗收；不替代 DSP 內部量測。
 */
class LatencyProbe(
    private val engine: VoiceEngine,
    private val windowSize: Int = 32
) {
    private val samples = ArrayDeque<Long>(windowSize)

    fun sampleOnce(): Long {
        val ms = max(0L, engine.latencyMs())
        if (samples.size >= windowSize) samples.removeFirst()
        samples.addLast(ms)
        return ms
    }

    fun clear() = samples.clear()

    fun count(): Int = samples.size

    fun lastOrNull(): Long? = samples.lastOrNull()

    fun minOrNull(): Long? = samples.minOrNull()

    fun maxOrNull(): Long? = samples.maxOrNull()

    fun avgOrNull(): Double? {
        if (samples.isEmpty()) return null
        return samples.sum().toDouble() / samples.size
    }

    /** 簡易驗收：連續樣本皆 ≤ [budgetMs] 且至少 [minSamples] 筆 */
    fun passesBudget(budgetMs: Long = 40L, minSamples: Int = 8): Boolean {
        if (samples.size < minSamples) return false
        return samples.all { it in 0L..budgetMs }
    }

    fun summary(): String {
        val avg = avgOrNull()
        return buildString {
            append("n=").append(count())
            append(" last=").append(lastOrNull() ?: "-")
            append(" min=").append(minOrNull() ?: "-")
            append(" max=").append(maxOrNull() ?: "-")
            append(" avg=")
            if (avg == null) append("-") else append(String.format("%.1f", avg))
            append("ms")
        }
    }
}
