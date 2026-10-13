package com.m4isper.planets.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import com.m4isper.planets.presentation.detail.PlanetDetailScreen
import com.m4isper.planets.presentation.list.PlanetsListScreen

/**
 * Destinations as types instead of strings: a wrong route no longer compiles.
 *
 * Serializable so the back stack can be persisted and survive process death.
 */
@Serializable
sealed interface PlanetsDestination : NavKey {
    @Serializable
    data object List : PlanetsDestination

    @Serializable
    data class Details(val date: String) : PlanetsDestination
}

@Composable
fun PlanetsNavHost() {
    val backStack = rememberNavBackStack(PlanetsDestination.List)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<PlanetsDestination.List> {
                PlanetsListScreen(
                    onPlanetClick = { date ->
                        backStack += PlanetsDestination.Details(date)
                    },
                )
            }
            entry<PlanetsDestination.Details> { key ->
                PlanetDetailScreen(
                    date = key.date,
                    onBackClick = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}