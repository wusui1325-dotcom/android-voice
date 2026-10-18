# 女聲參數預設（唯一一套）

維護者：參數主筆（Kimi）

| 名稱 | pitchSemitones | formantScale | 用途 |
|------|----------------|--------------|------|
| DEFAULT | 5.5 | 1.18 | 通用起點／UI 初始 |
| SOFT | 4.0 | 1.12 | 偏柔和 |
| BRIGHT | 7.0 | 1.22 | 偏明亮 |

推薦 API：`FemalePresets.recommend(VoiceProfile)`，供 `VoiceEngine.recommendParams` 呼叫。

範圍以契約為準：pitch `-12..+12`、formant `0.7..1.4`。
