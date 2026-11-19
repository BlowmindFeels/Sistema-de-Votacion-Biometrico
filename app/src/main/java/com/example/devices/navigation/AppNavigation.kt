package com.example.devices.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.devices.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginExitoso = {
                    navController.navigate(Routes.VOTACIONES) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onIrARegistro = { navController.navigate(Routes.REGISTRO) }
            )
        }

        composable(Routes.REGISTRO) {
            RegistroScreen(
                onRegistroExitoso = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Routes.VOTACIONES) {
            VotacionesScreen(
                onVotacionClick = { id -> navController.navigate(Routes.detalle(id)) }
            )
        }

        composable(
            route = Routes.DETALLE,
            arguments = listOf(navArgument("votacionId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("votacionId") ?: 0
            DetalleVotacionScreen(
                onVerResultados = { navController.navigate(Routes.resultados(id)) },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RESULTADOS,
            arguments = listOf(navArgument("votacionId") { type = NavType.IntType })
        ) {
            ResultadosScreen(onVolver = { navController.popBackStack() })
        }
    }
}