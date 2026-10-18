package com.wusper.voice.perm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 執行階段錄音權限（1.0.0）。
 * 目標 Android 14+；實時變聲與預覽錄音前必須通過。
 */
object AudioPermission {
    const val REQUEST_CODE = 4101

    val REQUIRED: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    fun hasRecordAudio(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** @return true 若已授權；false 若已觸發系統請求 */
    fun ensure(activity: Activity): Boolean {
        if (hasRecordAudio(activity)) return true
        ActivityCompat.requestPermissions(activity, REQUIRED, REQUEST_CODE)
        return false
    }

    fun grantedFromResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode != REQUEST_CODE) return false
        if (permissions.isEmpty() || grantResults.isEmpty()) return false
        val i = permissions.indexOf(Manifest.permission.RECORD_AUDIO)
        if (i < 0 || i >= grantResults.size) return false
        return grantResults[i] == PackageManager.PERMISSION_GRANTED
    }

    /** Android 14+ 可選：說明為何需要麥克風（呼叫端自行決定是否顯示教育 UI） */
    fun needsLegacyStorage(): Boolean = Build.VERSION.SDK_INT < 29
}
