package com.wusper.voice.engine

/**
 * 女聲參數預設（唯一一套，由參數主筆維護）。
 * 對準音高（半音）與共振峰兩軸；供 UI 初始值與 [VoiceEngine.recommendParams] 使用。
 */
object FemalePresets {

    /** 通用女聲預設（多數來源聲線的起點） */
    val DEFAULT = VoiceParams(
        pitchSemitones = 5.5f,
        formantScale = 1.24f,
    )

    /** 偏柔和／偏低女聲 */
    val SOFT = VoiceParams(
        pitchSemitones = 4.0f,
        formantScale = 1.12f,
    )

    /** 偏明亮／偏高女聲 */
    val BRIGHT = VoiceParams(
        pitchSemitones = 7.0f,
        formantScale = 1.22f,
    )

    /**
     * 依本機 [VoiceProfile] 推薦參數。
     * 以 DEFAULT 為中心，依估計 f0 與 formantHint 微調，並夾在契約範圍內。
     */
    fun recommend(profile: VoiceProfile): VoiceParams {
        val conf = profile.confidence.coerceIn(0f, 1f)
        val targetF0 = 210f
        val f0 = profile.f0Hz.coerceAtLeast(60f)
        val pitchBoost = ((targetF0 / f0).let { kotlin.math.ln(it) / kotlin.math.ln(2f) } * 12f)
            .coerceIn(-6f, 10f)
        val pitch = lerp(DEFAULT.pitchSemitones, pitchBoost, conf * 0.85f)
            .coerceIn(VoiceParams.RANGE_PITCH.start, VoiceParams.RANGE_PITCH.endInclusive)

        val formantTarget = (1.05f + (profile.formantHint - 1f) * 0.15f)
            .coerceIn(1.10f, 1.28f)
        val formant = lerp(DEFAULT.formantScale, formantTarget, conf * 0.7f)
            .coerceIn(VoiceParams.RANGE_FORMANT.start, VoiceParams.RANGE_FORMANT.endInclusive)

        return VoiceParams(pitchSemitones = pitch, formantScale = formant)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return a + (b - a) * x
    }
}
