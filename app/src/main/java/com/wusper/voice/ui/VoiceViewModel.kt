package com.wusper.voice.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.wusper.voice.debug.LatencyProbe
import com.wusper.voice.engine.SimpleVoiceEngine
import com.wusper.voice.engine.VoiceEngine
import com.wusper.voice.engine.VoiceParams

class VoiceViewModel(
    app: Application,
    private val engine: VoiceEngine = SimpleVoiceEngine(),
) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("voice_ui", Context.MODE_PRIVATE)
    private val probe = LatencyProbe(engine)

    var pitch by mutableFloatStateOf(VoiceParams().pitchSemitones)
        private set
    var formant by mutableFloatStateOf(VoiceParams().formantScale)
        private set
    var realtimeOn by mutableStateOf(false)
        private set
    var previewRecording by mutableStateOf(false)
        private set
    var onboarding by mutableStateOf(!prefs.getBoolean(KEY_ONBOARD_DONE, false))
        private set
    var onboardingRecording by mutableStateOf(false)
        private set
    var latencyMs by mutableLongStateOf(0L)
        private set
    var latencySummary by mutableStateOf("")
        private set
    var status by mutableStateOf("就緒")
        private set

    fun onPitchChange(v: Float) {
        pitch = v.coerceIn(VoiceParams.RANGE_PITCH.start, VoiceParams.RANGE_PITCH.endInclusive)
        pushParams()
    }

    fun onFormantChange(v: Float) {
        formant = v.coerceIn(VoiceParams.RANGE_FORMANT.start, VoiceParams.RANGE_FORMANT.endInclusive)
        pushParams()
    }

    fun toggleRealtime() {
        if (realtimeOn) {
            engine.stopRealtime()
            realtimeOn = false
            status = "實時已停止"
        } else {
            if (previewRecording) stopPreview()
            engine.startRealtime()
            realtimeOn = true
            status = "實時變聲中"
            refreshLatency()
        }
    }

    fun togglePreview() {
        if (previewRecording) {
            stopPreview()
        } else {
            if (realtimeOn) {
                engine.stopRealtime()
                realtimeOn = false
            }
            engine.startPreviewRecord()
            previewRecording = true
            status = "錄音中…"
        }
    }

    fun startOnboarding() {
        if (realtimeOn) {
            engine.stopRealtime()
            realtimeOn = false
        }
        engine.startOnboardingRecord()
        onboardingRecording = true
        status = "引導錄音中，請說幾句話"
    }

    fun finishOnboarding() {
        onboardingRecording = false
        status = "分析中…"
        val profile = engine.finishOnboardingAnalyze()
        val recommended = engine.recommendParams(profile)
        pitch = recommended.pitchSemitones
        formant = recommended.formantScale
        pushParams()
        prefs.edit().putBoolean(KEY_ONBOARD_DONE, true).apply()
        onboarding = false
        status = "已套用推薦（f0≈${profile.f0Hz.toInt()} Hz）"
    }

    fun skipOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARD_DONE, true).apply()
        onboarding = false
        status = "已跳過引導"
    }

    fun applyFemalePreset() {
        onPitchChange(5.5f)
        onFormantChange(1.15f)
        status = "已套用女聲預設"
    }

    private fun stopPreview() {
        engine.stopPreviewAndPlay()
        previewRecording = false
        status = "播放預覽"
        refreshLatency()
    }

    fun refreshLatency() {
        latencyMs = probe.sampleOnce()
        latencySummary = probe.summary()
    }

    private fun pushParams() {
        engine.setParams(VoiceParams(pitchSemitones = pitch, formantScale = formant))
    }

    override fun onCleared() {
        engine.stopRealtime()
        super.onCleared()
    }

    init { pushParams() }

    companion object {
        private const val KEY_ONBOARD_DONE = "onboard_done"
    }
}
