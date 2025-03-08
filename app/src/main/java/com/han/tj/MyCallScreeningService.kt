package com.han.tj

import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.telephony.emergency.EmergencyNumber
import android.util.Log

class MyCallScreeningService : CallScreeningService() {

    override fun onScreenCall(call: Call.Details) {
        val sharedPref = getSharedPreferences("decline_call_pref", MODE_PRIVATE)
        val blockCallsEnabled = sharedPref.getBoolean("blockCalls", true)
        val phoneNumber = call.handle.schemeSpecificPart ?: ""
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

/*긴급 전화목록 확인
           try {
                 // Map<Int, List<EmergencyNumber>> 형식으로 반환됩니다.
                 // Key: 활성 구독 ID (활성 구독이 없으면 SubscriptionManager.getDefaultSubscriptionId())
                 // Value: 해당 구독에 대한 긴급번호 목록. 우선순위가 높은 번호가 인덱스가 낮은 위치에 배치됩니다.
                 val emergencyNumbersMap: Map<Int, List<EmergencyNumber>> = telephonyManager.emergencyNumberList

                 if (emergencyNumbersMap.isEmpty()) {
                     Log.d("EmergencyNumber", "No emergency numbers available.")
                 } else {
                     for ((subscriptionId, emergencyNumbers) in emergencyNumbersMap) {
                         Log.d("EmergencyNumber", "Subscription ID: $subscriptionId")
                         emergencyNumbers.forEach { emergencyNumber ->
                             // getNumber()와 getEmergencyServiceCategories() 메서드를 사용하여 값을 가져옵니다.
                             Log.d("EmergencyNumber", "Number: ${emergencyNumber.getNumber()}")
                             Log.d("EmergencyNumber", "Categories: ${emergencyNumber.getEmergencyServiceCategories()}")
                         }
                     }
                 }
             } catch (e: SecurityException) {
                 Log.e("EmergencyNumber", "Missing permission READ_PHONE_STATE or no carrier privileges: ${e.message}")
             }
     */

        if (telephonyManager.isEmergencyNumber(phoneNumber)) {
            Log.d("MyCallScreeningService", "Emergency call detected: $phoneNumber. Allowing call.")
            respondToCall(call, CallResponse.Builder().setDisallowCall(false).build())
            return
        }

        // 스크리닝 기능이 꺼져 있으면 그냥 통화 허용
        if (!blockCallsEnabled) {
            respondToCall(call, CallResponse.Builder().setDisallowCall(false).build())
            return
        }

        // 연락처에 등록된 번호인지 확인
        val isInContacts = ContactUtils.isNumberInContacts(this, phoneNumber)

        if (!isInContacts) {
            Log.d("MyCallScreeningService", "Blocking call from unknown number: $phoneNumber")
            val response = CallResponse.Builder()
                .setDisallowCall(true)    // 통화 거절
                .setSkipCallLog(false)    // 통화 기록에 남길지 여부
                .setSkipNotification(true)// 부재중 알림 생략
                .build()
            respondToCall(call, response)
        } else {
            respondToCall(call, CallResponse.Builder().setDisallowCall(false).build())
        }
    }
}
