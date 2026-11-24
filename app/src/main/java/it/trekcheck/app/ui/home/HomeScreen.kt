package it.trekcheck.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.trekcheck.app.R
import it.trekcheck.app.domain.model.SavedPlanDataAvailability
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.ui.components.TrekCheckPrimaryAction
import it.trekcheck.app.ui.theme.TrekCheckTheme
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = TrekCheckTheme.spacing
    val configuration = LocalConfiguration.current
    val activeLocale = if (!configuration.locales.isEmpty) {
        configuration.locales[0] ?: Locale.getDefault()
    } else {
        Locale.getDefault()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = spacing.space4,
                    vertical = spacing.space6,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                item(key = "header") {
                    HomeHeader(isOffline = uiState.isOffline)
                }

                if (uiState.activeNotice != null) {
                    item(key = "notice") {
                        HomeNoticeCard(
                            notice = uiState.activeNotice,
                            onDismiss = { onEvent(HomeEvent.DismissNotice) },
                        )
                    }
                }

                if (uiState.isEmpty) {
                    item(key = "empty_card") {
                        EmptyLocalPlansCard()
                    }
                    item(key = "import_action") {
                        ImportGpxSection(
                            uiState = uiState,
                            onImportClick = { onEvent(HomeEvent.ImportGpxClicked) },
                        )
                    }
                } else {
                    item(key = "import_action") {
                        ImportGpxSection(
                            uiState = uiState,
                            onImportClick = { onEvent(HomeEvent.ImportGpxClicked) },
                        )
                    }
                    item(key = "saved_plans_heading") {
                        SavedPlansHeader(isOffline = uiState.isOffline)
                    }
                    itemsIndexed(
                        items = uiState.savedPlans,
                        key = { index, plan -> "plan_${index}_${plan.id}" },
                    ) { _, plan ->
                        SavedPlanCard(
                            plan = plan,
                            activeLocale = activeLocale,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(isOffline: Boolean) {
    val spacing = TrekCheckTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LocalModeStatusBadge(isOffline = isOffline)
    }
}

@Composable
private fun LocalModeStatusBadge(isOffline: Boolean) {
    val spacing = TrekCheckTheme.spacing
    val (label, iconRes) = if (isOffline) {
        stringResource(R.string.home_status_local_offline) to R.drawable.ic_offline_local
    } else {
        stringResource(R.string.home_status_local_only) to R.drawable.ic_info_notice
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.space3,
                vertical = spacing.space2,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomeNoticeCard(
    notice: HomeNotice,
    onDismiss: () -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    val extendedColors = TrekCheckTheme.extendedColors

    when (notice) {
        HomeNotice.ImportComingSoon -> {
            val dismissContentDescription = stringResource(R.string.home_notice_import_dismiss_cd)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = MaterialTheme.shapes.small,
                color = extendedColors.warningContainer,
                border = BorderStroke(1.dp, extendedColors.warning),
            ) {
                Column(
                    modifier = Modifier.padding(spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info_notice),
                            contentDescription = null,
                            tint = extendedColors.onWarningContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.home_notice_import_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = extendedColors.onWarningContainer,
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_notice_import_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extendedColors.onWarningContainer,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics { contentDescription = dismissContentDescription },
                        ) {
                            Text(
                                text = stringResource(R.string.home_notice_dismiss),
                                style = MaterialTheme.typography.labelLarge,
                                color = extendedColors.onWarningContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLocalPlansCard() {
    val spacing = TrekCheckTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_route_trail),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.home_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(
                text = stringResource(R.string.home_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImportGpxSection(
    uiState: HomeUiState,
    onImportClick: () -> Unit,
) {
    val spacing = TrekCheckTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        TrekCheckPrimaryAction(
            label = stringResource(R.string.home_action_import_gpx),
            state = uiState.importActionState,
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth(),
            inProgressLabel = stringResource(R.string.home_action_import_gpx_in_progress),
            inProgressStateDescription = stringResource(R.string.home_action_state_in_progress),
            disabledStateDescription = stringResource(R.string.home_action_state_disabled),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_add_gpx),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        Text(
            text = stringResource(R.string.home_action_import_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedPlansHeader(isOffline: Boolean) {
    val spacing = TrekCheckTheme.spacing

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space1),
    ) {
        Text(
            text = stringResource(R.string.home_saved_plans_heading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .semantics { heading() },
        )
        if (isOffline) {
            Text(
                text = stringResource(R.string.home_saved_plans_offline_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedPlanCard(
    plan: SavedPlanSummary,
    activeLocale: Locale,
) {
    val spacing = TrekCheckTheme.spacing
    val distanceText = when (val distance = HomeFormatters.resolveDistance(plan.distanceMeters, activeLocale)) {
        is DistanceDisplay.Meters -> stringResource(R.string.unit_meters_format, distance.meters)
        is DistanceDisplay.Kilometers -> stringResource(R.string.unit_kilometers_format, distance.formattedValue)
    }
    val elevationText = HomeFormatters.resolveElevationGainMeters(plan.elevationGainMeters)?.let { gain ->
        stringResource(R.string.home_plan_elevation_gain_format, gain)
    } ?: stringResource(R.string.home_plan_elevation_unavailable)
    val durationText = when (val duration = HomeFormatters.resolveDuration(plan.estimatedDurationSeconds)) {
        null -> stringResource(R.string.home_plan_duration_unavailable)
        DurationDisplay.UnderOneMinute -> stringResource(R.string.home_plan_duration_under_one_minute)
        is DurationDisplay.Minutes -> stringResource(R.string.home_plan_duration_minutes_format, duration.minutes)
        is DurationDisplay.Hours -> stringResource(R.string.home_plan_duration_hours_format, duration.hours)
        is DurationDisplay.HoursAndMinutes -> stringResource(
            R.string.home_plan_duration_hours_minutes_format,
            duration.hours,
            duration.minutes,
        )
    }
    val departureText = plan.departureInstantUtc?.let { departure ->
        stringResource(
            R.string.home_plan_departure_format,
            HomeFormatters.formatZonedDateTime(departure, plan.routeZoneId, activeLocale),
        )
    } ?: stringResource(R.string.home_plan_departure_unconfigured)
    val updatedText = stringResource(
        R.string.home_plan_saved_format,
        HomeFormatters.formatZonedDateTime(plan.updatedAtUtc, plan.routeZoneId, activeLocale),
    )
    val (modeLabel, modeIconRes) = when (plan.dataAvailability) {
        SavedPlanDataAvailability.ROUTE_ONLY ->
            stringResource(R.string.home_plan_mode_route_only) to R.drawable.ic_route_trail

        SavedPlanDataAvailability.SAVED_BRIEFING ->
            stringResource(R.string.home_plan_mode_saved_briefing) to R.drawable.ic_info_notice
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_route_trail),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Text(
                    text = elevationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            Text(
                text = departureText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = spacing.space2,
                            vertical = spacing.space1,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(modeIconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(spacing.space1))
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text(
                    text = updatedText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
    }
}
