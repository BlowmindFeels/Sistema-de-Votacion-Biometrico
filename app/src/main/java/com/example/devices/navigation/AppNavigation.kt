package com.example.devices.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.devices.data.SesionManager
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
                onVotacionClick = { id -> navController.navigate(Routes.detalle(id)) },
                onAdmin = { navController.navigate(Routes.ADMIN) },
                onSesionCerrada = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
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

        composable(Routes.ADMIN) {
            // La pantalla solo se ofrece a admins; el backend igual rechaza a quien no lo sea.
            if (!SesionManager.esAdmin) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            AdminScreen(
                onVolver = { navController.popBackStack() },
                onNuevaVotacion = { navController.navigate(Routes.NUEVA_VOTACION) },
                onVerResultados = { id -> navController.navigate(Routes.resultados(id)) }
            )
        }

        composable(Routes.NUEVA_VOTACION) {
            NuevaVotacionScreen(
                onCreada = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
