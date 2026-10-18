# VoiceEngine 契約（1.0.x）

- 套件：`com.wusper.voice.engine`
- 檔案：`VoiceParams.kt`、`VoiceProfile.kt`、`VoiceEngine.kt`
- UI 只綁 `pitchSemitones`、`formantScale` 兩軸
- DSP 實作 `VoiceEngine`；女聲預設由參數主筆維護，只留一套
- 目標裝置：Android 14+、8GB+ RAM、Snapdragon 7 Gen 1+

## 本地錄音推薦（可隨時重做）

不只首次引導；UI 可隨時重跑同一流程：

1. `startOnboardingRecord()`（約 3–5 秒）
2. `finishOnboardingAnalyze()` → `VoiceProfile`（僅本機，不上網）
3. `recommendParams(profile)` → 套用；使用者仍可微調兩軸

## 實時路徑（現行 1.0）

`AudioRecord` → DSP → `AudioTrack`（App 內監看：喇叭／耳機）。  
**不是**系統麥克風替換。

## 產品邊界（老闆定案）

目標要「第三方遊戲麥／Discord 等：別人聽到變聲、原麥不進溝通」。

| 條件 | 結論 |
|------|------|
| 無 root、普通 APK | **做不到** 通用代麥（無法關掉他 App 原麥、無法灌進系統 mic） |
| 自有 WebRTC／自建通道 | 可做變聲上行（非本階段優先） |
| root／虛擬音訊驅動／系統簽名 | 另開產品線；**1.1 不假裝**能用普通 APK 達成 |

1.1 若開「代麥」必須標為 root／特殊構建，不可與監看版混稱已達成通用代麥。

## 女聲預設

見 `FemalePresets.kt` 與 [FEMALE_PRESETS.md](./FEMALE_PRESETS.md)。DSP 的 `recommendParams` 應委派 `FemalePresets.recommend`。
