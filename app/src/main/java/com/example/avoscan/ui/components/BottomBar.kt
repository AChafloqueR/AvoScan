package com.example.avoscan.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

@Composable
fun BottomBar(
    navController: NavController
) {

    NavigationBar(
        containerColor = Color(0xFF0B2E1A)
    ) {

        NavigationBarItem(
            selected = false,
            onClick = {
                navController.navigate("home")
            },
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Inicio"
                )
            },
            label = {
                Text("Inicio")
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = {
                navController.navigate("camera")
            },
            icon = {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Detectar"
                )
            },
            label = {
                Text("Detectar")
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = {
                navController.navigate("historial")
            },
            icon = {
                Icon(
                    Icons.Default.History,
                    contentDescription = "Historial"
                )
            },
            label = {
                Text("Historial")
            }
        )
    }
}