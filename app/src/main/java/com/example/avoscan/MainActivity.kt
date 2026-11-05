package com.example.avoscan

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.avoscan.ml.AvoClassifier
import com.example.avoscan.ml.BenchmarkUtil          // <-- AGREGADO
import com.example.avoscan.ui.components.BottomBar
import com.example.avoscan.ui.screens.*
import com.example.avoscan.ui.theme.AvoScanTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 0.3f }
        BenchmarkUtil.ejecutar(this)

        setContent {
            AvoScanTheme {

                val navController = rememberNavController()

                var resultado by remember {
                    mutableStateOf<AvoClassifier.Resultado?>(null)
                }

                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                val routesWithBottomBar = setOf(
                    "dashboard", "camera", "historial", "reportes"
                )

                Scaffold(
                    bottomBar = {
                        if (currentRoute in routesWithBottomBar) {
                            BottomBar(navController = navController)
                        }
                    }
                ) { padding ->

                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("splash") {
                            SplashScreen(onFinish = {
                                navController.navigate("dashboard") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            })
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                onDetectarClick    = { navController.navigate("camera") },
                            ) { navController.navigate("historial") }
                        }

                        composable("camera") {
                            CameraScreen(onResultado = { res ->
                                resultado = res
                                navController.navigate("resultado")
                            })
                        }

                        // Resultado nuevo (viene de la cámara)
                        composable("resultado") {
                            ResultadoScreen(
                                resultado = resultado,
                                onVolverClick = { navController.popBackStack() }
                            )
                        }

                        // Historial (lista clickeable)
                        composable("historial") {
                            HistorialScreen(
                                onItemClick = { analisisId ->
                                    navController.navigate("resultado_historial/$analisisId")
                                }
                            )
                        }

                        // Resultado abierto desde el historial
                        composable(
                            route = "resultado_historial/{analisisId}",
                            arguments = listOf(navArgument("analisisId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val analisisId = backStackEntry.arguments?.getLong("analisisId") ?: 0L
                            ResultadoHistorialScreen(
                                analisisId = analisisId,
                                onVolverClick = { navController.popBackStack() }
                            )
                        }

                        composable("reportes")  { ReportesScreen() }
                    }
                }
            }
        }
    }
}