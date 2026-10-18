package com.wusper.voice.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** 1.0.0 stub 引擎：可實時／預覽；正式 DSP 可替換實作。 */
class SimpleVoiceEngine : VoiceEngine {

    private val paramsRef = AtomicReference(VoiceParams())
    private val realtimeRunning = AtomicBoolean(false)
    private val previewRecording = AtomicBoolean(false)
    private val onboardingRecording = AtomicBoolean(false)
    private val latencyRef = AtomicLong(0)

    private var realtimeThread: Thread? = null
    private var previewThread: Thread? = null
    private var onboardingThread: Thread? = null
    private var previewBuffer: ShortArray = ShortArray(0)
    private var onboardingBuffer: ShortArray = ShortArray(0)

    private val sampleRate = 48_000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    override fun setParams(params: VoiceParams) { paramsRef.set(params) }
    override fun getParams(): VoiceParams = paramsRef.get()
    override fun isRealtimeRunning(): Boolean = realtimeRunning.get()
    override fun latencyMs(): Long = latencyRef.get()

    override fun startRealtime() {
        if (!realtimeRunning.compareAndSet(false, true)) return
        realtimeThread = thread(name = "voice-realtime", priority = Thread.MAX_PRIORITY) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runRealtimeLoop()
        }
    }

    override fun stopRealtime() {
        realtimeRunning.set(false)
        realtimeThread?.join(1_500)
        realtimeThread = null
    }

    override fun startPreviewRecord() {
        if (realtimeRunning.get()) stopRealtime()
        if (!previewRecording.compareAndSet(false, true)) return
        previewBuffer = ShortArray(0)
        previewThread = thread(name = "voice-preview-rec") {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            previewBuffer = captureUntil({ previewRecording.get() }, 30)
            previewRecording.set(false)
        }
    }

    override fun stopPreviewAndPlay() {
        previewRecording.set(false)
        previewThread?.join(2_000)
        previewThread = null
        val captured = previewBuffer
        if (captured.isEmpty()) return
        thread(name = "voice-preview-play") { playProcessed(captured, paramsRef.get()) }
    }

    override fun startOnboardingRecord() {
        if (realtimeRunning.get()) stopRealtime()
        if (!onboardingRecording.compareAndSet(false, true)) return
        onboardingBuffer = ShortArray(0)
        onboardingThread = thread(name = "voice-onboard-rec") {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            onboardingBuffer = captureUntil({ onboardingRecording.get() }, 8)
            onboardingRecording.set(false)
        }
    }

    override fun finishOnboardingAnalyze(): VoiceProfile {
        onboardingRecording.set(false)
        onboardingThread?.join(2_000)
        onboardingThread = null
        return analyzeLocal(onboardingBuffer)
    }

    override fun recommendParams(profile: VoiceProfile): VoiceParams {
        val targetF0 = 220f
        val semitones = if (profile.f0Hz > 40f) {
            (12.0 * kotlin.math.log((targetF0 / profile.f0Hz).toDouble(), 2.0))
                .toFloat()
                .coerceIn(4f, 7f)
        } else {
            5.5f
        }
        val formant = (1.1f + (profile.formantHint - 1f) * 0.15f).coerceIn(1.1f, 1.25f)
        return VoiceParams(pitchSemitones = semitones, formantScale = formant)
    }

    private fun minBufferSize(): Int {
        val inSz = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
        val outSz = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
        return max(inSz, outSz).coerceAtLeast(sampleRate / 50)
    }

    private fun runRealtimeLoop() {
        val bufSize = minBufferSize()
        val recorder = buildRecorder(bufSize) ?: run { realtimeRunning.set(false); return }
        val track = buildTrack(bufSize) ?: run { recorder.release(); realtimeRunning.set(false); return }
        val input = ShortArray(bufSize)
        try {
            recorder.startRecording()
            track.play()
            while (realtimeRunning.get()) {
                val frameStart = System.nanoTime()
                val read = recorder.read(input, 0, input.size)
                if (read <= 0) continue
                val processed = applyPitch(input, read, paramsRef.get().pitchSemitones)
                track.write(processed, 0, processed.size)
                val frameMs = (System.nanoTime() - frameStart) / 1_000_000L
                latencyRef.set((read * 1000L / sampleRate) + frameMs)
            }
        } finally {
            try { recorder.stop() } catch (_: Exception) {}
            try { track.stop() } catch (_: Exception) {}
            recorder.release()
            track.release()
            realtimeRunning.set(false)
        }
    }

    private fun captureUntil(keepGoing: () -> Boolean, maxSeconds: Int): ShortArray {
        val bufSize = minBufferSize()
        val recorder = buildRecorder(bufSize) ?: return ShortArray(0)
        val chunks = ArrayList<Short>()
        val input = ShortArray(bufSize)
        val maxSamples = sampleRate * maxSeconds
        try {
            recorder.startRecording()
            while (keepGoing() && chunks.size < maxSamples) {
                val read = recorder.read(input, 0, input.size)
                if (read <= 0) continue
                for (i in 0 until read) chunks.add(input[i])
            }
        } finally {
            try { recorder.stop() } catch (_: Exception) {}
            recorder.release()
        }
        return chunks.toShortArray()
    }

    private fun playProcessed(pcm: ShortArray, params: VoiceParams) {
        val processed = applyPitch(pcm, pcm.size, params.pitchSemitones)
        val bufSize = minBufferSize()
        val track = buildTrack(max(bufSize, 2048)) ?: return
        try {
            track.play()
            var offset = 0
            while (offset < processed.size) {
                val n = min(bufSize, processed.size - offset)
                track.write(processed, offset, n)
                offset += n
            }
            Thread.sleep((processed.size * 1000L / sampleRate) + 50)
        } finally {
            try { track.stop() } catch (_: Exception) {}
            track.release()
        }
    }

    private fun analyzeLocal(pcm: ShortArray): VoiceProfile {
        if (pcm.size < sampleRate / 4) {
            return VoiceProfile(f0Hz = 140f, formantHint = 1f, confidence = 0.2f)
        }
        val minPeriod = sampleRate / 400
        val maxPeriod = sampleRate / 80
        var bestPeriod = minPeriod
        var bestCorr = 0.0
        val len = min(pcm.size, sampleRate)
        for (period in minPeriod..maxPeriod step 2) {
            var corr = 0.0
            var energy = 0.0
            var i = 0
            while (i + period < len) {
                val a = pcm[i].toDouble()
                val b = pcm[i + period].toDouble()
                corr += a * b
                energy += a * a
                i += 4
            }
            val norm = if (energy > 1.0) corr / energy else 0.0
            if (norm > bestCorr) {
                bestCorr = norm
                bestPeriod = period
            }
        }
        val f0 = sampleRate.toFloat() / bestPeriod
        return VoiceProfile(
            f0Hz = f0,
            formantHint = (f0 / 150f).coerceIn(0.7f, 1.4f),
            confidence = bestCorr.coerceIn(0.0, 1.0).toFloat(),
        )
    }

    private fun applyPitch(input: ShortArray, length: Int, pitchSemitones: Float): ShortArray {
        val rate = 2.0.pow((pitchSemitones / 12.0)).toFloat().coerceIn(0.5f, 2.0f)
        if (abs(rate - 1f) < 0.01f) return input.copyOf(length)
        val outLen = max(1, (length / rate).roundToInt())
        val out = ShortArray(outLen)
        var i = 0
        while (i < outLen) {
            val src = i * rate
            val i0 = src.toInt()
            val i1 = min(i0 + 1, length - 1)
            val frac = src - i0
            if (i0 >= length) break
            val s0 = input[i0].toFloat()
            val s1 = input[i1].toFloat()
            out[i] = (s0 + (s1 - s0) * frac).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            i++
        }
        return out
    }

    private fun buildRecorder(bufSize: Int): AudioRecord? = try {
        AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate, channelConfigIn, audioFormat, bufSize * 2,
        ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
    } catch (_: Exception) { null }

    private fun buildTrack(bufSize: Int): AudioTrack? = try {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(audioFormat)
                    .setChannelMask(channelConfigOut)
                    .build(),
            )
            .setBufferSizeInBytes(bufSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
            .takeIf { it.state == AudioTrack.STATE_INITIALIZED }
    } catch (_: Exception) { null }
}
