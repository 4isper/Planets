package com.m4isper.planets.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.planets.ui.model.PlanetUi
import com.m4isper.planets.ui.components.LoadError
import com.m4isper.planets.ui.components.PlanetCard
import kotlinx.coroutines.flow.collect

/** How many items before the end trigger the next page request. */
private const val PREFETCH_DISTANCE = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanetsListScreen(
    onPlanetClick: (date: String) -> Unit,
    viewModel: PlanetsListViewModel = hiltViewModel(),
) {
    val planets by viewModel.items.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val content = uiState as? ListUiState.Content

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nasa") })
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = content?.isRefreshing == true,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    planets.isNotEmpty() -> PlanetsGrid(
                        planets = planets,
                        isAppending = content?.isAppending == true,
                        appendError = content?.appendError == true,
                        onPlanetClick = onPlanetClick,
                        onEndReached = viewModel::loadMore,
                        onRetryAppend = viewModel::loadMore,
                    )
                    uiState is ListUiState.Loading -> CircularProgressIndicator()
                    uiState is ListUiState.Error -> LoadError(
                        message = "Ошибка загрузки",
                        onRetry = viewModel::refresh,
                    )
                    else -> Text("Нет данных")
                }
            }
        }
    }
}

@Composable
private fun PlanetsGrid(
    planets: List<PlanetUi>,
    isAppending: Boolean,
    appendError: Boolean,
    onPlanetClick: (date: String) -> Unit,
    onEndReached: () -> Unit,
    onRetryAppend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    // Watch the scroll position instead of firing from inside the last item:
    // if a fetched page contains only already cached dates the list does not grow,
    // no new item is composed, and an item-scoped effect would never re-fire.
    LaunchedEffect(gridState, planets.size) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.collect { lastVisibleIndex ->
            if (lastVisibleIndex >= planets.lastIndex - PREFETCH_DISTANCE) {
                onEndReached()
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = planets,
            key = { planet -> planet.date },
        ) { planet ->
            PlanetCard(
                planet = planet,
                onClick = { onPlanetClick(planet.date) },
            )
        }

        if (isAppending) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (appendError) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LoadError(
                    message = "Не удалось загрузить ещё",
                    onRetry = onRetryAppend,
                )
            }
        }
    }
}
