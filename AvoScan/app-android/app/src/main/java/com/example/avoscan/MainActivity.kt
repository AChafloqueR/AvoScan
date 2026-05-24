package com.example.avoscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.avoscan.ml.AvoClassifier
import com.example.avoscan.ui.screens.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            val navController = rememberNavController()

            var resultado by remember {
                mutableStateOf<AvoClassifier.Resultado?>(null)
            }

            NavHost(
                navController = navController,
                startDestination = "splash"
            ) {

                composable("splash") {

                    SplashScreen(
                        onFinish = {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    )
                }

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
            }
        }
    }
}