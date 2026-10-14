package com.m4isper.planets.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m4isper.planets.R

/**
 * Shared failure state: both the list and the details show the same
 * "something failed, try again" block.
 *
 * Takes resolved text rather than a resource id, so a caller that has something more
 * specific to say — the cause of a transport error, say — can say it.
 *
 * [onRetry] is nullable so a settled failure — a record the feed does not carry —
 * can say so instead of offering a retry that cannot succeed.
 */
@Composable
fun LoadError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message)
        if (onRetry != null) {
            Button(onClick = onRetry) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}