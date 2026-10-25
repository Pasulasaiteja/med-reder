package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MediReadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediReadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MediReadApp()
                }
            }
        }
    }
}

@Composable
fun MediReadApp() {
    val navController = rememberNavController()
    val viewModel: MediReadViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToMedicineReader = {
                    viewModel.resetState()
                    navController.navigate(Routes.MEDICINE_READER)
                },
                onNavigateToPrescriptionReader = {
                    viewModel.resetState()
                    navController.navigate(Routes.PRESCRIPTION_READER)
                },
                onNavigateToAbout = {
                    navController.navigate(Routes.ABOUT)
                }
            )
        }

        composable(Routes.MEDICINE_READER) {
            ImageCaptureScreen(
                title = "Medicine Strip Reader",
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAnalyze = { bitmap -> viewModel.analyzeMedicineStrip(bitmap) },
                uiState = uiState,
                onReset = { viewModel.resetState() }
            )
        }

        composable(Routes.PRESCRIPTION_READER) {
            ImageCaptureScreen(
                title = "Prescription Reader",
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAnalyze = { bitmap -> viewModel.analyzePrescription(bitmap) },
                uiState = uiState,
                onReset = { viewModel.resetState() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}