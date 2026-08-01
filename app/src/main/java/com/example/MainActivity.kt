package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.MainAppContainer
import com.example.ui.HydrationViewModel
import com.example.ui.theme.MyApplicationTheme

/**
 * Activity utama aplikasi Hidrasi & Kopi.
 * Bertanggung jawab menginisialisasi UI Edge-to-Edge Jetpack Compose dan menyambungkan ViewModel.
 */
class MainActivity : ComponentActivity() {
    
    // Inisialisasi ViewModel menggunakan Kotlin property delegate by viewModels()
    private val viewModel: HydrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Aktifkan tampilan layar penuh tanpa batas (Edge-to-Edge)
        enableEdgeToEdge()
        
        // Set konten UI menggunakan Jetpack Compose
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Container navigasi & struktur aplikasi utama
                    MainAppContainer(viewModel = viewModel)
                }
            }
        }
    }
}

