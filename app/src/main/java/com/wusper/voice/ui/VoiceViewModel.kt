package com.wusper.voice.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wusper.voice.debug.LatencyProbe
import com.wusper.voice.engine.FemalePresets
import com.wusper.voice.engine.SimpleVoiceEngine
import com.wusper.voice.engine.VoiceEngine
import com.wusper.voice.engine.VoiceParams
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val engine: VoiceEngine = SimpleVoiceEngine(),
) : ViewModel() {

    private val probe = LatencyProbe(engine)

    var pitch by mutableFloatStateOf(engine.getParams().pitchSemitones)
        private set
    var formant by mutableFloatStateOf(engine.getParams().formantScale)
        private set
    var realtimeOn by mutableStateOf(false)
        private set
    var previewRecording by mutableStateOf(false)
        private set
    var latencyMs by mutableLongStateOf(0L)
        private set
    var latencySummary by mutableStateOf("")
        private set
    var status by mutableStateOf("就緒")
        private set
    var onboarding by mutableStateOf(true)
        private set
    var onboardingRecording by mutableStateOf(false)
        private set

    fun refreshLatency() {
        latencyMs = probe.sampleOnce()
        val avg = probe.avgOrNull()
        val mn = probe.minOrNull()
        val mx = probe.maxOrNull()
        latencySummary = if (avg != null && mn != null && mx != null) {
            "延遲統計 avg=${"%.0f".format(avg)} min=$mn max=$mx n=${probe.count()}"
        } else ""
    }

    fun onPitchChange(v: Float) {
        pitch = v
        pushParams()
    }

    fun onFormantChange(v: Float) {
        formant = v
        pushParams()
    }

    private fun pushParams() {
        engine.setParams(VoiceParams(pitchSemitones = pitch, formantScale = formant))
    }

    fun toggleRealtime() {
        if (previewRecording) stopPreview()
        if (realtimeOn) {
            engine.stopRealtime()
            realtimeOn = false
            status = "已停止即時變聲"
        } else {
            pushParams()
            engine.startRealtime()
            realtimeOn = true
            status = "即時變聲中"
        }
    }

    fun togglePreview() {
        if (realtimeOn) {
            engine.stopRealtime()
            realtimeOn = false
        }
        if (previewRecording) stopPreview() else {
            engine.startPreviewRecord()
            previewRecording = true
            status = "錄音預覽中…"
        }
    }

    private fun stopPreview() {
        engine.stopPreviewAndPlay()
        previewRecording = false
        status = "播放預覽"
    }

    fun startOnboarding() {
        if (realtimeOn) {
            engine.stopRealtime()
            realtimeOn = false
        }
        engine.startOnboardingRecord()
        onboardingRecording = true
        status = "引導錄音中（請說話 3–5 秒）…"
    }

    fun finishOnboarding() {
        onboardingRecording = false
        viewModelScope.launch {
            val profile = engine.finishOnboardingAnalyze()
            val rec = engine.recommendParams(profile)
            pitch = rec.pitchSemitones
            formant = rec.formantScale
            pushParams()
            onboarding = false
            status = "已套用推薦（f0≈${profile.f0Hz.toInt()}Hz，信心 ${(profile.confidence * 100).toInt()}%）"
        }
    }

    fun skipOnboarding() {
        onboardingRecording = false
        onboarding = false
        applyFemalePreset()
        status = "已跳過引導，套用女聲預設"
    }

    /** 隨時重做本地錄音／推薦（不只首次）。 */
    fun redoOnboarding() {
        if (realtimeOn) {
            engine.stopRealtime()
            realtimeOn = false
        }
        if (previewRecording) {
            engine.stopPreviewAndPlay()
            previewRecording = false
        }
        onboardingRecording = false
        onboarding = true
        status = "可重新錄音以推薦參數"
    }

    fun applyFemalePreset() {
        val p = FemalePresets.DEFAULT
        pitch = p.pitchSemitones
        formant = p.formantScale
        pushParams()
        status = "已套用女聲預設"
    }

    override fun onCleared() {
        engine.stopRealtime()
        super.onCleared()
    }
}
