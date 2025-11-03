package com.example.farmermilkcollectionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.farmermilkcollectionapp.ui.MainScreen
import com.example.farmermilkcollectionapp.ui.theme.FarmerMilkCollectionAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FarmerMilkCollectionAppTheme {
                MainScreen()
            }
        }
    }
}
