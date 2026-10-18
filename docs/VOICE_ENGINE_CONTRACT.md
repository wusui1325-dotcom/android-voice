# VoiceEngine 契約（1.0.x）

- 套件：`com.wusper.voice.engine`
- 檔案：`VoiceParams.kt`、`VoiceProfile.kt`、`VoiceEngine.kt`
- UI 只綁 `pitchSemitones`、`formantScale` 兩軸
- DSP 實作 `VoiceEngine`；女聲預設由參數主筆維護，只留一套
- 目標裝置：Android 14+、8GB+ RAM、Snapdragon 7 Gen 1+

## 本地錄音推薦（可隨時重做）

1. `startOnboardingRecord()`（約 3–5 秒）
2. `finishOnboardingAnalyze()` → `VoiceProfile`（僅本機）
3. `recommendParams(profile)` → 套用；可微調兩軸  
UI 須提供隨時重錄入口（非僅首次）。

## 實時路徑（產品範圍）

`AudioRecord` → DSP → `AudioTrack`（App 內監看：喇叭／耳機）。

## 產品邊界（老闆定案）

- **要做**：App 內變聲、調參、錄音預覽、隨時重錄推薦。
- **不做**：無 root 下第三方遊戲麥／Discord／系統通話代麥（普通 APK 做不到）。
- **也不開**：root／虛擬驅動版；產品文案標清楚邊界，**不假裝**能通用代麥。
