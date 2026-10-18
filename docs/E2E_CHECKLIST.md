# 1.0.0 端到端驗收清單

目標裝置：Android 14+、8GB+ RAM、Snapdragon 7 Gen 1+

## 權限
- [ ] 首次啟動請求 `RECORD_AUDIO`
- [ ] 拒絕後無法 `startRealtime`／`startPreviewRecord`，並有可重試入口
- [ ] 設定頁返回授權後可立即開始

## 實時變聲
- [ ] `startRealtime` 後入耳可辨女聲方向（依預設 pitch／formant）
- [ ] 兩軸滑桿即時生效（對應 `VoiceParams`）
- [ ] `stopRealtime` 乾淨停止，無殘響／無崩潰

## 錄音預覽
- [ ] `startPreviewRecord` → `stopPreviewAndPlay` 可聽變聲結果
- [ ] 預覽使用當前參數，不寫入第二套預設

## 延遲
- [ ] Debug 面板顯示 `LatencyProbe.summary()`
- [ ] 實時鏈在目標機上多數樣本 ≤ 40ms（暫定預算，可再調）

## Release
- [ ] versionName `1.0.0`、簽章 keystore 來自 Drive `a-v`
- [ ] APK 上傳 GitHub Release
