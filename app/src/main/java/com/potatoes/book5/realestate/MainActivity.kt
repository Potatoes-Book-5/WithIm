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
}
