# VoiceEngine 契約（1.0.0）

- 套件：`com.wusper.voice.engine`
- 檔案：`VoiceParams.kt`、`VoiceProfile.kt`、`VoiceEngine.kt`
- UI 只綁 `pitchSemitones`、`formantScale` 兩軸
- DSP 實作 `VoiceEngine`；女聲預設由參數主筆維護，只留一套
- 目標裝置：Android 14+、8GB+ RAM、Snapdragon 7 Gen 1+

## 首次進 app（本地推薦）

1. UI 偵測尚未完成引導 → 呼叫 `startOnboardingRecord()`
2. 使用者說幾句（約 3–5 秒）→ `finishOnboardingAnalyze()` 得 `VoiceProfile`（僅本機）
3. `recommendParams(profile)` → 套用並可讓使用者微調兩軸滑桿
4. 標記引導完成（SharedPreferences 等，UI 負責）
