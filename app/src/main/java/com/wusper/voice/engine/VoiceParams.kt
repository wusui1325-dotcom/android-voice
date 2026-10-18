package com.wusper.voice.engine

/**
 * DSP ↔ UI 共用參數（1.0.0 契約定稿）。
 * 女聲預設數值由參數預設主筆填入，UI 僅綁此兩軸滑桿。
 */
data class VoiceParams(
    /** 音高偏移（半音），範圍 -12f..+12f；女聲預設約 +4f..+7f */
    val pitchSemitones: Float = 5.5f,
    /** 共振峰縮放，範圍 0.7f..1.4f；女聲預設約 1.1f..1.25f */
    val formantScale: Float = 1.18f,
) {
    init {
        require(pitchSemitones in -12f..12f) { "pitchSemitones out of range" }
        require(formantScale in 0.7f..1.4f) { "formantScale out of range" }
    }

    companion object {
        val RANGE_PITCH = -12f..12f
        val RANGE_FORMANT = 0.7f..1.4f
    }
}
