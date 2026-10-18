package com.wusper.voice.engine

/**
 * 實時變聲引擎介面（1.0.0 契約）。
 * DSP 實作此介面；UI 只依賴此契約，不直連原生細節。
 */
interface VoiceEngine {
    fun setParams(params: VoiceParams)
    fun getParams(): VoiceParams

    /** 開始麥克風 → 變聲 → 喇叭 實時鏈 */
    fun startRealtime()

    fun stopRealtime()

    /** app 內錄音（預覽用） */
    fun startPreviewRecord()

    /** 停止錄音並以當前參數播放預覽 */
    fun stopPreviewAndPlay()

    /**
     * 首次引導：本地短錄一段（建議 3–5 秒），僅裝置內分析。
     * 與 [startPreviewRecord] 分開：此流程目標是建檔推薦，不是聽效果。
     */
    fun startOnboardingRecord()

    /**
     * 結束引導錄音，本地識別聲線並回傳 [VoiceProfile]。
     * 不上傳、不依賴網路。
     */
    fun finishOnboardingAnalyze(): VoiceProfile

    /**
     * 依 [VoiceProfile] 推薦 [VoiceParams]（女聲目標；預設表由參數主筆維護）。
     */
    fun recommendParams(profile: VoiceProfile): VoiceParams

    /** 端到端延遲（毫秒），供量測 */
    fun latencyMs(): Long

    fun isRealtimeRunning(): Boolean
}
