package com.han.tj

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var switchBlockCalls: Switch
    private lateinit var sendParentsSMS: Switch
    private lateinit var btnSetCallScreeningRole: Button
    private lateinit var btnRequestSmsPermission: Button
    private lateinit var etParentPhone1: TextInputEditText
    private lateinit var etParentPhone2: TextInputEditText

    // ActivityResult API를 이용한 권한 요청 런처
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (permission, isGranted) ->
            Log.d("Permissions", "$permission granted: $isGranted")
        }
    }

    // 콜 스크리닝 역할 요청 런처
    private val callScreeningRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "콜 스크리닝 역할이 부여되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "콜 스크리닝 역할 부여가 거부되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // View 초기화
        etParentPhone1 = findViewById(R.id.et_parent_phone1)
        etParentPhone2 = findViewById(R.id.et_parent_phone2)
        switchBlockCalls = findViewById(R.id.switch_block_calls)
        sendParentsSMS = findViewById(R.id.send_parents_sms)
        btnSetCallScreeningRole = findViewById(R.id.btn_set_call_screening_role)
        btnRequestSmsPermission = findViewById(R.id.btn_request_sms_permission)

        // SharedPreferences에서 기존 값 불러오기
        val pref = getSharedPreferences("decline_call_pref", MODE_PRIVATE)
        switchBlockCalls.isChecked = pref.getBoolean("blockCalls", true)
        sendParentsSMS.isChecked = pref.getBoolean("sendSms", true)

        // 기존에 저장된 부모 연락처 불러오기
        etParentPhone1.setText(pref.getString("parent_phone1", ""))
        etParentPhone2.setText(pref.getString("parent_phone2", ""))

        // 스위치 변경 시 SharedPreferences에 저장
        switchBlockCalls.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("blockCalls", isChecked).apply()
        }
        sendParentsSMS.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("sendSms", isChecked).apply()
        }

        // 콜 스크리닝 역할 요청 버튼 클릭
        btnSetCallScreeningRole.setOnClickListener {
            requestCallScreeningRole()
        }

        // SMS 전달을 위한 권한 신청 버튼 클릭 처리
        btnRequestSmsPermission.setOnClickListener {
            // 알림 접근 설정 화면으로 이동
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        // 저장 버튼 클릭 처리
        findViewById<Button>(R.id.btn_save_parent_contacts).setOnClickListener {
            val phone1 = etParentPhone1.text.toString().trim()
            val phone2 = etParentPhone2.text.toString().trim()

            val editor = pref.edit()
            editor.putString("parent_phone1", phone1)
            editor.putString("parent_phone2", phone2)
            editor.apply()

            Toast.makeText(this, "부모 연락처가 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 기타 필요한 권한 체크 코드
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        // onResume()에서 알림 접근 권한 활성 여부를 확인하고 Toast 메시지 표시
        if (isNotificationAccessEnabled()) {
            Toast.makeText(this, "알림 접근 권한이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledPackages.contains(packageName)
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                // 역할 요청 인텐트 생성 및 실행
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                callScreeningRoleLauncher.launch(intent)
            } else {
                Toast.makeText(this, "콜 스크리닝 역할이 이미 부여되어 있습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "이 기기는 콜 스크리닝 역할을 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.ANSWER_PHONE_CALLS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.RECEIVE_MMS,
            android.Manifest.permission.POST_NOTIFICATIONS
        )

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsToRequest.forEach { permission ->
                if (!shouldShowRequestPermissionRationale(permission)) {
                    Log.d("Permissions", "$permission might be permanently denied.")
                    // 사용자가 '다시 묻지 않음' 선택 시 설정 안내 로직 추가 가능
                }
            }
            Log.d("Permissions", "Requesting permissions: $permissionsToRequest")
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("Permissions", "All required permissions are already granted.")
        }
    }
}
