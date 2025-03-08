package com.han.tj

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log

class SmsAlertNotificationListenerService : NotificationListenerService() {

    private val allowedSmsPackages = listOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // SMS/MMS 앱의 알림인지 확인
        if (!allowedSmsPackages.contains(sbn.packageName)) {
            Log.d("SmsAlert", "Ignoring notification from package: ${sbn.packageName}")
            return
        }

        // SharedPreferences에서 SMS 전송 여부 체크
        val sharedPref = getSharedPreferences("decline_call_pref", Context.MODE_PRIVATE)
        val sendSmsEnabled = sharedPref.getBoolean("sendSms", true)
        if (!sendSmsEnabled) {
            Log.d("SmsAlert", "SMS 전달 기능이 비활성화 되어 있음.")
            return
        }

        val extras = sbn.notification.extras
        val sender = extras.getString("android.title") ?: ""
        val message = extras.getCharSequence("android.text")?.toString() ?: ""
        Log.d("SmsAlert", "SMS Notification - sender: $sender, message: $message")

        if (sender.isBlank()) return

        // 발신자 정보가 전화번호 형식인지 이름 형식인지 판단 (정규표현식 기반)
        val isPhoneNumber = isValidPhoneNumber(sender)
        val isInContacts = if (isPhoneNumber) {
            ContactUtils.isNumberInContacts(this, sender)
        } else {
            ContactUtils.isNameInContacts(this, sender)
        }

        if (isInContacts) {
            Log.d("SmsAlert", "Sender is in contacts: $sender. Ignoring alert.")
            return
        }

        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (telephonyManager.isEmergencyNumber(sender)) {
            Log.d("SmsAlert", "Sender is an emergency number: $sender. Ignoring alert.")
            return
        }

        val parentPhone1 = sharedPref.getString("parent_phone1", "")?.trim() ?: ""
        val parentPhone2 = sharedPref.getString("parent_phone2", "")?.trim() ?: ""

        val alertContent = "의심 SMS 알림:\n발신자: $sender\n내용: $message"

        if (parentPhone1.isNotEmpty()) {
            sendSms(parentPhone1, alertContent)
        }
        if (parentPhone2.isNotEmpty()) {
            sendSms(parentPhone2, alertContent)
        }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("SmsAlert", "Alert SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("SmsAlert", "Failed to send SMS to $phoneNumber: ${e.message}")
        }
    }

    private fun isValidPhoneNumber(sender: String): Boolean {
        // 특수문자 제거 후, 전화번호 형식 패턴과 비교 (예: 010-0000-0000 또는 010-000-0000)
        // 이 예시는 한국 전화번호 형식을 기준으로 작성되었습니다.
        val normalized = sender.replace(Regex("[^\\d-]"), "")
        val pattern = Regex("^010-\\d{3,4}-\\d{4}$")
        return pattern.matches(normalized)
    }

}
