package com.soundgram.chipster.util.userid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Base64
import androidx.core.content.ContextCompat
import java.util.*


//단말기 고유값 추출 클래스
object UniqueDeviceID {
    /**
     * UUID를 가져온다
     * @param mContext
     * * @return
     */
    fun getUniqueID(mContext: Context): String {
        var tmDevice: String
        var tmSerial: String
        val androidId: String
        val deviceUuid: UUID
        var deviceId: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Q(29)이상일 때 UUID가져오는 방식
            deviceUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
            // DRM - WIDEVINE_UUID(앱 삭제 or 데이터 삭제해도 변경x)
            val mediaDrm = MediaDrm(deviceUuid)
            deviceId = Base64.encodeToString(
                mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID),
                0
            ).trim { it <= ' ' }
        } else {
            // 기존 UUID 조합해서 사용하는 방식
            if (ContextCompat.checkSelfPermission(
                    mContext.applicationContext,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val tm =
                    mContext.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                tmDevice = "" + tm.deviceId
                tmSerial = "" + tm.simSerialNumber
                androidId = "" + Settings.Secure.getString(
                    mContext.applicationContext.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                deviceUuid = UUID(
                    androidId.hashCode().toLong(),
                    tmDevice.hashCode().toLong() shl 32 or tmSerial.hashCode()
                        .toLong()
                )
                deviceId = deviceUuid.toString()
            }
        }
        return deviceId ?: ""
    }
}