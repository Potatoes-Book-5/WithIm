package com.potatoes.book5.realestate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.potatoes.book5.realestate.ui.AppRoot
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KingOfRealEstateTheme {
                AppRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as RealEstateApplication).container.voiceContext.setForeground(true)
    }
    override fun onStop() {
        (application as RealEstateApplication).container.voiceContext.setForeground(false)
        super.onStop()
    }

    /** 위치 권한 대화상자나 시스템 설정에서 돌아오면 권한 상태를 다시 확인한다(T-029). */
    override fun onResume() {
        super.onResume()
        (application as RealEstateApplication).container.fusedLocationBridge.refreshPermission()
    }
}
