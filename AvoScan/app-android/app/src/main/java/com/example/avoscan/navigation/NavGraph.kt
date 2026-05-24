package com.example.avoscan.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.avoscan.ml.AvoClassifier
import com.example.avoscan.ui.screens.*

@Composable
fun NavGraph(
    navController: NavHostController
) {

    var resultado by remember {
        mutableStateOf<AvoClassifier.Resultado?>(null)
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        composable("home") {

            HomeScreen(

                onStartClick = {

                    navController.navigate("camera")
                },

                onHistorialClick = {
                    navController.navigate("historial")
                }
            )
        }

        composable("camera") {

            CameraScreen(
                onResultado = { res ->

                    resultado = res

                    navController.navigate("loading")
                }
            )
        }

        composable("loading") {

            LoadingScreen(

                onFinish = {

                    navController.navigate("resultado")
                }
            )
        }

        composable("resultado") {

            ResultadoScreen(
                resultado = resultado,
                onHistorialClick = {
                    navController.navigate("historial")
                }
            )
        }

        composable("historial") {

            HistorialScreen()
        }

        composable("splash") {

            SplashScreen(

                onFinish = {

                    navController.navigate("home") {

                        popUpTo("splash") {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}