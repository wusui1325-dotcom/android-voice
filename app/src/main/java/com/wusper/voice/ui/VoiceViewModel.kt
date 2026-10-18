package com.wusper.voice.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wusper.voice.engine.SimpleVoiceEngine
import com.wusper.voice.engine.VoiceEngine
import com.wusper.voice.engine.FemalePresets
import com.wusper.voice.engine.VoiceParams
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val engine: VoiceEngine = SimpleVoiceEngine(),
) : ViewModel() {

    var pitch by mutableFloatStateOf(engine.getParams().pitchSemitones)
        private set
    var formant by mutableFloatStateOf(engine.getParams().formantScale)
        private set
    var realtimeOn by mutableStateOf(false)
        private set
    var previewOn by mutableStateOf(false)
        private set
    var latencyMs by mutableLongStateOf(0L)
        private set
    var status by mutableStateOf("就緒")
        private set

    private var latencyJob: Job? = null

    init {
        latencyJob = viewModelScope.launch {
            while (isActive) {
                latencyMs = engine.latencyMs()
                delay(400)
            }
        }
    }

    fun onPitch(v: Float) {
        pitch = v
        engine.setParams(VoiceParams(pitchSemitones = pitch, formantScale = formant))
    }

    fun onFormant(v: Float) {
        formant = v
        engine.setParams(VoiceParams(pitchSemitones = pitch, formantScale = formant))
    }

    fun toggleRealtime() {
        if (previewOn) stopPreview()
        if (realtimeOn) {
            engine.stopRealtime()
            realtimeOn = false
            status = "已停止即時變聲"
        } else {
            engine.setParams(VoiceParams(pitchSemitones = pitch, formantScale = formant))
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
        if (previewOn) {
            stopPreview()
        } else {
            engine.startPreviewRecord()
            previewOn = true
            status = "錄音預覽中…"
        }
    }

    private fun stopPreview() {
        engine.stopPreviewAndPlay()
        previewOn = false
        status = "播放預覽"
    }

    fun runOnboarding() {
        viewModelScope.launch {
            status = "引導錄音中（請說話 3–5 秒）…"
            engine.startOnboardingRecord()
            delay(4_000)
            val profile = engine.finishOnboardingAnalyze()
            val rec = engine.recommendParams(profile)
            onPitch(rec.pitchSemitones)
            onFormant(rec.formantScale)
            status = "已套用推薦（f0≈${profile.f0Hz.toInt()}Hz，信心 ${(profile.confidence * 100).toInt()}%）"
        }
    }

    override fun onCleared() {
        latencyJob?.cancel()
        engine.stopRealtime()
        super.onCleared()
    }
}
