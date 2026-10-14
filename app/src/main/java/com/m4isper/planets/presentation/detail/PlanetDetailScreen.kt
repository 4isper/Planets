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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.m4isper.planets.R
import com.m4isper.planets.ui.components.LoadError
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
    val isRetrying by viewModel.isRetrying.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.planet_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
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
                // NotFound is settled: the feed has no such date, so no retry is offered.
                DetailUiState.NotFound -> LoadError(message = stringResource(R.string.state_record_missing))
                DetailUiState.Empty -> FetchState(
                    message = stringResource(R.string.state_not_found),
                    isRetrying = isRetrying,
                    onRetry = viewModel::retry,
                )
                is DetailUiState.Error -> FetchState(
                    message = stringResource(R.string.state_loading_failed),
                    isRetrying = isRetrying,
                    onRetry = viewModel::retry,
                )
                is DetailUiState.Content -> PlanetDetails(planet = state.planet)
            }
        }
    }
}

/** Swaps the failure block for a spinner while the fetch is in flight. */
@Composable
private fun FetchState(
    message: String,
    isRetrying: Boolean,
    onRetry: () -> Unit,
) {
    if (isRetrying) CircularProgressIndicator() else LoadError(message = message, onRetry = onRetry)
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
        RichText(
            text = planet.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (planet.explanation.text.isNotEmpty()) {
            RichText(
                text = planet.explanation,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        planet.permalink?.let { permalink ->
            OutlinedButton(onClick = { uriHandler.openUri(permalink) }) {
                // The feed exposes no separate video URL: videos only play on the
                // NASA page, so the same link gets a context aware label.
                Text(
                    stringResource(
                        if (planet.isVideo) R.string.action_watch_video else R.string.action_open_original,
                    ),
                )
            }
        }
    }
}

/**
 * Styled text (`<strong>`, links). `LinkAnnotation.Url` spans inside [text]
 * are clickable out of the box and open in the browser.
 * Plain cached rows render exactly like before.
 */
@Composable
private fun RichText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.merge(color = color),
    )
}
