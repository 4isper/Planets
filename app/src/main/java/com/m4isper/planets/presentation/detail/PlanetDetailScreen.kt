package com.m4isper.planets.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.m4isper.planets.ui.model.PlanetUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanetDetailScreen(
    date: String,
    onBackClick: () -> Unit,
    viewModel: PlanetDetailViewModel = viewModel(
        key = "planet-detail-$date",
        factory = planetDetailViewModelFactory(LocalContext.current.applicationContext, date),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детализация") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                DetailUiState.Loading -> CircularProgressIndicator()
                DetailUiState.Empty -> Text("Нет данных")
                is DetailUiState.Content -> PlanetDetails(planet = state.planet)
            }
        }
    }
}

@Composable
private fun PlanetDetails(
    planet: PlanetUi,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (planet.hasImage) {
            AsyncImage(
                model = planet.imageUrl,
                contentDescription = planet.imageContentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = planet.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
                text = planet.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        if (planet.explanation.isNotEmpty()) {
            Text(
                text = planet.explanation,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        planet.permalink?.let { permalink ->
            OutlinedButton(onClick = { uriHandler.openUri(permalink) }) {
                // The feed exposes no separate video URL: videos only play on the
                // NASA page, so the same link gets a context aware label.
                Text(planet.openLabel)
            }
        }
    }
}
