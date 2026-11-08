package com.potatoes.book5.realestate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KingOfRealEstateTheme {
                HelloWorldScreen()
            }
        }
    }
}

@Composable
fun HelloWorldScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "짜감자 부동산 앱입니다",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
private fun HelloWorldScreenPreview() {
    KingOfRealEstateTheme {
        HelloWorldScreen()
    }
}
