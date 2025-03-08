package com.han.tj

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract

object ContactUtils {

    fun normalizeName(name: String): String {
        return name.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
    }

    // 이름으로 연락처에 등록되어 있는지 확인 (모든 연락처를 가져와서 비교)
    fun isNameInContacts(context: Context, name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        val normalizedSender = normalizeName(name)
        val contentResolver = context.contentResolver
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val contactName = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                if (normalizeName(contactName).equals(normalizedSender, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    // 전화번호 정규화 (숫자만 남김)
    fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^\\d]"), "")
    }

    // 전화번호가 연락처에 등록되어 있는지 확인
    fun isNumberInContacts(context: Context, phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrEmpty()) return false
        val normalized = normalizePhoneNumber(phoneNumber)
        val contentResolver = context.contentResolver
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(normalized)
            .build()
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            cursor != null && cursor.moveToFirst()
        } finally {
            cursor?.close()
        }
    }
}
