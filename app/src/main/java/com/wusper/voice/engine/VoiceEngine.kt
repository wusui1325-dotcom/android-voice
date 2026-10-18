package com.wusper.voice.engine

/**
 * 實時變聲引擎介面（1.0.0 契約定稿）。
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

    /** 端到端延遲（毫秒），供量測 */
    fun latencyMs(): Long

    fun isRealtimeRunning(): Boolean
}
