package com.m4isper.planets.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.m4isper.planets.presentation.detail.PlanetDetailScreen
import com.m4isper.planets.presentation.list.PlanetsListScreen

object PlanetsRoutes {
    const val LIST = "planets"
    const val DETAILS = "planet/{date}"

    fun details(date: String): String = "planet/$date"
}

@Composable
fun PlanetsNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = PlanetsRoutes.LIST,
    ) {
        composable(route = PlanetsRoutes.LIST) {
            PlanetsListScreen(
                onPlanetClick = { date -> navController.navigate(PlanetsRoutes.details(date)) },
            )
        }
        composable(
            route = PlanetsRoutes.DETAILS,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) {
            PlanetDetailScreen(
                onBackClick = { navController.navigateUp() },
            )
        }
    }
}
