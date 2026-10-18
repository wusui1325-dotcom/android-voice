package com.wusper.voice.engine

/**
 * 首次進 app 本地錄音分析結果（不上網）。
 */
data class VoiceProfile(
    /** 估計基頻 Hz */
    val f0Hz: Float,
    /** 估計第一共振峰相關特徵（相對尺度，供推薦用） */
    val formantHint: Float,
    /** 分析信心 0f..1f */
    val confidence: Float,
)
