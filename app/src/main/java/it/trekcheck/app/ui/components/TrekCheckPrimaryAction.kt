package it.trekcheck.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import it.trekcheck.app.R

enum class PrimaryActionState {
    Enabled,
    Disabled,
    InProgress,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrekCheckPrimaryAction(
    label: String,
    state: PrimaryActionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inProgressLabel: String = label,
    inProgressStateDescription: String = "",
    disabledStateDescription: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val resolvedInProgressStateDescription = inProgressStateDescription.ifBlank {
        stringResource(R.string.home_action_state_in_progress)
    }
    val resolvedDisabledStateDescription = disabledStateDescription.ifBlank {
        stringResource(R.string.home_action_state_disabled)
    }

    val isNotDisabled = state != PrimaryActionState.Disabled
    val isInteractive = state == PrimaryActionState.Enabled

    val colors = when (state) {
        PrimaryActionState.Enabled -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )

        PrimaryActionState.Disabled -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PrimaryActionState.InProgress -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }

    val border = when (state) {
        PrimaryActionState.Enabled -> null
        PrimaryActionState.Disabled -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        PrimaryActionState.InProgress -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }

    val semanticModifier = modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .semantics(mergeDescendants = true) {
            when (state) {
                PrimaryActionState.Enabled -> Unit
                PrimaryActionState.Disabled -> {
                    stateDescription = resolvedDisabledStateDescription
                }

                PrimaryActionState.InProgress -> {
                    liveRegion = LiveRegionMode.Polite
                    stateDescription = resolvedInProgressStateDescription
                }
            }
        }

    val rippleConfiguration = if (isInteractive) {
        LocalRippleConfiguration.current
    } else {
        null
    }

    CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
        Button(
            onClick = {
                if (isInteractive) {
                    onClick()
                }
            },
            modifier = semanticModifier,
            enabled = isNotDisabled,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = if (isInteractive) ButtonDefaults.buttonElevation() else null,
            border = border,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            when (state) {
                PrimaryActionState.Enabled,
                PrimaryActionState.Disabled -> {
                    if (leadingIcon != null) {
                        leadingIcon()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                PrimaryActionState.InProgress -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = inProgressLabel,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
