package com.wusper.voice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wusper.voice.R
import com.wusper.voice.engine.VoiceParams
import kotlinx.coroutines.delay

@Composable
fun VoiceScreen(vm: VoiceViewModel) {
    LaunchedEffect(vm.realtimeOn) {
        while (vm.realtimeOn) {
            vm.refreshLatency()
            delay(500)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("變聲器 1.0.0", style = MaterialTheme.typography.headlineSmall)
        Text(vm.status, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.latency, vm.latencyMs), style = MaterialTheme.typography.bodySmall)
        if (vm.latencySummary.isNotEmpty()) {
            Text(vm.latencySummary, style = MaterialTheme.typography.labelSmall)
        }

        if (vm.onboarding) {
            Text("本地短錄 3–5 秒以推薦參數（不上網；可隨時重做）")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (vm.onboardingRecording) vm.finishOnboarding() else vm.startOnboarding()
                }) {
                    Text(if (vm.onboardingRecording) "完成分析" else "開始引導錄音")
                }
                OutlinedButton(onClick = vm::skipOnboarding) { Text("跳過") }
            }
            Spacer(Modifier.height(8.dp))
        }

        Text("${stringResource(R.string.pitch_label)} ${"%.1f".format(vm.pitch)}")
        Slider(
            value = vm.pitch,
            onValueChange = vm::onPitchChange,
            valueRange = VoiceParams.RANGE_PITCH,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("${stringResource(R.string.formant_label)} ${"%.2f".format(vm.formant)}")
        Slider(
            value = vm.formant,
            onValueChange = vm::onFormantChange,
            valueRange = VoiceParams.RANGE_FORMANT,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = vm::toggleRealtime) {
                Text(
                    if (vm.realtimeOn) stringResource(R.string.stop_realtime)
                    else stringResource(R.string.start_realtime),
                )
            }
            FilledTonalButton(onClick = vm::togglePreview) {
                Text(
                    if (vm.previewRecording) stringResource(R.string.stop_preview)
                    else stringResource(R.string.start_preview),
                )
            }
        }

        OutlinedButton(onClick = vm::applyFemalePreset) { Text("套用女聲預設") }
        if (!vm.onboarding) {
            OutlinedButton(onClick = vm::redoOnboarding, modifier = Modifier.fillMaxWidth()) {
                Text("重新錄音推薦參數")
            }
        }
    }
}
