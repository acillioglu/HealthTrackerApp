package com.huawei.healthtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.common.ApiException
import com.huawei.hms.hihealth.data.Scopes
import com.huawei.hms.support.api.entity.auth.Scope
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService
import com.huawei.hms.support.hwid.ui.HuaweiIdAuthButton

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var btnLogin: HuaweiIdAuthButton
    private lateinit var mAuthParam: HuaweiIdAuthParams
    private lateinit var mAuthService: HuaweiIdAuthService

    private val REQUEST_SIGN_IN_LOGIN = 1001


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            signIn()
        }


    }

    private fun signIn() {

        val scopeList = listOf(
            Scope(Scopes.HEALTHKIT_CALORIES_BOTH),
            Scope(Scopes.HEALTHKIT_HEIGHTWEIGHT_BOTH),
        )

        mAuthParam =
            HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM).apply {
                setIdToken()
                    .setAccessToken()
                    .setScopeList(scopeList)
            }.createParams()

        mAuthService = HuaweiIdAuthManager.getService(this, mAuthParam)

        val authHuaweiIdTask = mAuthService.silentSignIn()

        authHuaweiIdTask.addOnSuccessListener {
            val intent = Intent(this, CalorieTrackerActivity::class.java)
            startActivity(intent)
        }
            .addOnFailureListener {
                startActivityForResult(mAuthService.signInIntent, REQUEST_SIGN_IN_LOGIN)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_SIGN_IN_LOGIN -> {

                val authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data)
                if (authHuaweiIdTask.isSuccessful) {

                    val intent = Intent(this, CalorieTrackerActivity::class.java)
                    startActivity(intent)

                } else {
                    Log.i(
                        TAG,
                        "signIn failed: ${(authHuaweiIdTask.exception as ApiException).statusCode}"
                    )
                }
            }
        }
    }

}