package com.m4isper.planets.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.m4isper.planets.presentation.detail.PlanetDetailScreen
import com.m4isper.planets.presentation.list.PlanetsListScreen
import kotlinx.serialization.Serializable

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
        // Forward: details enter from the right, the list leaves to the left.
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        },
        // Back: mirrored. The predictive variant is driven by the swipe itself,
        // and its lambda receives the SwipeEdge, so the direction can be made to
        // differ for left and right gestures if the app grows more than two screens.
        popTransitionSpec = { enterFromLeft() },
        predictivePopTransitionSpec = { enterFromLeft() },
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

/**
 * Both screens are full width, so back is a plain horizontal slide: the screen
 * being revealed enters from the left while the current one leaves to the right,
 * which is exactly the direction the user's finger is already moving.
 */
private fun AnimatedContentTransitionScope<*>.enterFromLeft() =
    slideInHorizontally(initialOffsetX = { -it }) togetherWith
        slideOutHorizontally(targetOffsetX = { it })