package it.trekcheck.app.ui.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import it.trekcheck.app.R
import it.trekcheck.app.domain.model.PaceProfile
import it.trekcheck.app.domain.model.PaceProfileType
import it.trekcheck.app.domain.model.PaceSpeedParser
import it.trekcheck.app.domain.model.RouteManagementErrorCode
import it.trekcheck.app.domain.model.RoutePreviewDetails
import it.trekcheck.app.domain.model.RouteTitleSanitizer
import it.trekcheck.app.domain.model.RouteWaypointSummary
import it.trekcheck.app.ui.components.PrimaryActionState
import it.trekcheck.app.ui.components.TrekCheckPrimaryAction
import it.trekcheck.app.ui.home.DistanceDisplay
import it.trekcheck.app.ui.home.HomeFormatters
import it.trekcheck.app.ui.theme.TrekCheckTheme
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun RoutePreviewScreen(
    uiState: RoutePreviewUiState,
    onEvent: (RoutePreviewEvent) -> Unit,
    modifier: Modifier = Modifier,
    deviceZoneId: ZoneId = ZoneId.systemDefault(),
) {
    val spacing = TrekCheckTheme.spacing
    val locale = activeLocale()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.actionErrorCode) {
        if (uiState.actionErrorCode != null) {
            scrollState.scrollTo(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("route_preview_screen"),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = spacing.space4, vertical = spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                val backDescription = stringResource(R.string.preview_back_cd)
                TextButton(
                    onClick = { onEvent(RoutePreviewEvent.BackClicked) },
                    enabled = !uiState.isDeleting && uiState.renameDialog?.isSaving != true,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("route_preview_back")
                        .semantics { contentDescription = backDescription },
                ) {
                    Text(stringResource(R.string.preview_back), style = MaterialTheme.typography.labelLarge)
                }

                uiState.actionErrorCode?.let { code ->
                    NoticeSurface(
                        title = errorTitle(code),
                        body = errorBody(code),
                        onDismiss = { onEvent(RoutePreviewEvent.DismissActionError) },
                    )
                }

                when (uiState.status) {
                    RoutePreviewStatus.LOADING -> Text(
                        text = stringResource(R.string.preview_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )

                    RoutePreviewStatus.ERROR -> LoadErrorSection(
                        code = uiState.loadErrorCode ?: RouteManagementErrorCode.STORAGE_FAILED,
                        isDeleting = uiState.isDeleting,
                        onEvent = onEvent,
                    )

                    RoutePreviewStatus.READY -> uiState.details?.let { details ->
                        ReadySection(
                            details = details,
                            uiState = uiState,
                            locale = locale,
                            deviceZoneId = deviceZoneId,
                            onEvent = onEvent,
                        )
                    }
                }
            }
        }
    }

    uiState.renameDialog?.let { dialog ->
        RenameDialog(
            draft = dialog.draft,
            errorCode = dialog.errorCode,
            isSaving = dialog.isSaving,
            onEvent = onEvent,
        )
    }
    if (uiState.isDeleteDialogVisible) {
        DeleteDialog(isDeleting = uiState.isDeleting, onEvent = onEvent)
    }
}

@Composable
private fun activeLocale(): Locale {
    val configuration = LocalConfiguration.current
    return if (!configuration.locales.isEmpty) {
        configuration.locales[0] ?: Locale.getDefault()
    } else {
        Locale.getDefault()
    }
}

@Composable
private fun errorTitle(code: RouteManagementErrorCode): String = stringResource(
    when (code) {
        RouteManagementErrorCode.NOT_FOUND -> R.string.preview_error_not_found_title
        RouteManagementErrorCode.CORRUPTED_GEOMETRY -> R.string.preview_error_corrupted_title
        RouteManagementErrorCode.INVALID_TITLE,
        RouteManagementErrorCode.STORAGE_FAILED,
        -> R.string.preview_error_storage_title
    },
)

@Composable
private fun errorBody(code: RouteManagementErrorCode): String = stringResource(
    when (code) {
        RouteManagementErrorCode.NOT_FOUND -> R.string.preview_error_not_found_body
        RouteManagementErrorCode.CORRUPTED_GEOMETRY -> R.string.preview_error_corrupted_body
        RouteManagementErrorCode.INVALID_TITLE,
        RouteManagementErrorCode.STORAGE_FAILED,
        -> R.string.preview_error_storage_body
    },
)

@Composable
private fun LoadErrorSection(
    code: RouteManagementErrorCode,
    isDeleting: Boolean,
    onEvent: (RoutePreviewEvent) -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    val title = stringResource(
        when (code) {
            RouteManagementErrorCode.NOT_FOUND -> R.string.preview_error_not_found_title
            RouteManagementErrorCode.CORRUPTED_GEOMETRY -> R.string.preview_error_corrupted_title
            RouteManagementErrorCode.INVALID_TITLE,
            RouteManagementErrorCode.STORAGE_FAILED,
            -> R.string.preview_error_storage_title
        },
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("route_preview_error")
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = errorBody(code),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
    if (code != RouteManagementErrorCode.NOT_FOUND) {
        OutlinedButton(
            onClick = { onEvent(RoutePreviewEvent.RetryClicked) },
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.preview_retry))
        }
    }
    OutlinedButton(
        onClick = { onEvent(RoutePreviewEvent.BackClicked) },
        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
    ) {
        Text(stringResource(R.string.preview_back_to_home))
    }
    if (code == RouteManagementErrorCode.CORRUPTED_GEOMETRY) {
        OutlinedButton(
            onClick = { onEvent(RoutePreviewEvent.DeleteClicked) },
            enabled = !isDeleting,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("route_preview_delete"),
        ) {
            Text(stringResource(R.string.preview_action_delete), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun NoticeSurface(
    title: String,
    body: String,
    onDismiss: (() -> Unit)? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.errorContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onErrorContainer,
    borderColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.error,
    tag: String = "route_preview_notice",
) {
    val spacing = TrekCheckTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.semantics { heading() },
            )
            Text(body, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.home_notice_dismiss), color = contentColor)
                }
            }
        }
    }
}

@Composable
private fun ReadySection(
    details: RoutePreviewDetails,
    uiState: RoutePreviewUiState,
    locale: Locale,
    deviceZoneId: ZoneId,
    onEvent: (RoutePreviewEvent) -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    val extended = TrekCheckTheme.extendedColors

    Text(
        text = details.title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .testTag("route_preview_title")
            .semantics { heading() },
    )
    Text(
        text = stringResource(R.string.preview_offline_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("route_preview_offline_note"),
    )

    SummaryCard(details = details, locale = locale, deviceZoneId = deviceZoneId)

    if (!details.hasElevation) {
        NoticeSurface(
            title = stringResource(R.string.preview_elevation_missing_title),
            body = stringResource(R.string.preview_elevation_missing_body),
            containerColor = extended.warningContainer,
            contentColor = extended.onWarningContainer,
            borderColor = extended.warning,
            tag = "route_preview_elevation_notice",
        )
    } else if (details.elevationGainMeters == null && details.profile == null) {
        NoticeSurface(
            title = stringResource(R.string.preview_elevation_partial_title),
            body = stringResource(R.string.preview_elevation_partial_body),
            containerColor = extended.warningContainer,
            contentColor = extended.onWarningContainer,
            borderColor = extended.warning,
            tag = "route_preview_elevation_notice",
        )
    }

    SectionHeading(stringResource(R.string.preview_track_heading))
    RouteTrackCanvas(
        track = details.track,
        contentDescription = stringResource(R.string.preview_track_cd),
    )
    Text(
        text = stringResource(
            if (details.highestPoint != null) {
                R.string.preview_track_legend
            } else {
                R.string.preview_track_legend_no_highest
            },
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = trackAlternativeText(details, locale),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("route_track_text_alternative"),
    )

    details.profile?.let { profile ->
        SectionHeading(stringResource(R.string.preview_profile_heading))
        ElevationProfileCanvas(
            profile = profile,
            contentDescription = stringResource(R.string.preview_profile_cd),
        )
        Text(
            text = profileAlternativeText(
                profile = profile,
                gainMeters = details.elevationGainMeters,
                lossMeters = details.elevationLossMeters,
                locale = locale,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("elevation_profile_text_alternative"),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        val canConfigure = !uiState.isDeleting && uiState.renameDialog?.isSaving != true
        val isConfigured = details.departureInstantUtc != null
        TrekCheckPrimaryAction(
            label = stringResource(
                if (isConfigured) {
                    R.string.preview_action_configure_edit
                } else {
                    R.string.preview_action_configure
                },
            ),
            state = if (canConfigure) PrimaryActionState.Enabled else PrimaryActionState.Disabled,
            onClick = { onEvent(RoutePreviewEvent.ConfigureClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("route_preview_configure"),
        )
        Text(
            text = stringResource(
                if (isConfigured) {
                    R.string.preview_action_configure_saved_hint
                } else {
                    R.string.preview_action_configure_hint
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { onEvent(RoutePreviewEvent.RenameClicked) },
            enabled = !uiState.isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("route_preview_rename"),
        ) {
            Text(stringResource(R.string.preview_action_rename), style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(
            onClick = { onEvent(RoutePreviewEvent.DeleteClicked) },
            enabled = !uiState.isDeleting,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("route_preview_delete"),
        ) {
            Text(
                stringResource(R.string.preview_action_delete),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun SummaryCard(
    details: RoutePreviewDetails,
    locale: Locale,
    deviceZoneId: ZoneId,
) {
    val spacing = TrekCheckTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("route_preview_summary"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            SectionHeading(stringResource(R.string.preview_summary_heading))
            MetricRow(stringResource(R.string.preview_metric_distance), formatDistance(details.distanceMeters, locale))
            MetricRow(stringResource(R.string.preview_metric_start), waypointText(details.start, locale))
            MetricRow(stringResource(R.string.preview_metric_end), waypointText(details.end, locale))
            details.highestPoint?.let { highest ->
                MetricRow(
                    stringResource(R.string.preview_metric_highest),
                    highestText(highest, locale),
                )
            }
            val min = details.minElevationMeters
            val max = details.maxElevationMeters
            if (min != null && max != null) {
                MetricRow(
                    stringResource(R.string.preview_metric_elevation_range),
                    stringResource(R.string.preview_elevation_range_format, formatMeters(min), formatMeters(max)),
                )
            }
            details.elevationGainMeters?.let { gain ->
                MetricRow(
                    stringResource(R.string.preview_metric_gain),
                    stringResource(R.string.preview_gain_format, clampMeterLong(gain)),
                )
            }
            details.elevationLossMeters?.let { loss ->
                MetricRow(
                    stringResource(R.string.preview_metric_loss),
                    stringResource(R.string.preview_loss_format, clampMeterLong(loss)),
                )
            }
            val departure = details.departureInstantUtc
            if (departure != null) {
                MetricRow(
                    stringResource(R.string.preview_metric_departure),
                    HomeFormatters.formatZonedDateTimeWithOffset(departure, details.routeZoneId, locale),
                )
                val timezoneValue = if (details.routeZoneId != deviceZoneId) {
                    stringResource(
                        R.string.preview_timezone_diff_format,
                        details.routeZoneId.id,
                        deviceZoneId.id,
                    )
                } else {
                    details.routeZoneId.id
                }
                MetricRow(stringResource(R.string.preview_metric_timezone), timezoneValue)
                MetricRow(
                    stringResource(R.string.preview_metric_pace),
                    paceProfileLabel(details.paceProfile, locale),
                )
            } else {
                MetricRow(
                    stringResource(R.string.preview_metric_departure),
                    stringResource(R.string.preview_metric_departure_unconfigured),
                )
            }
        }
    }
}

@Composable
internal fun paceProfileLabel(paceProfile: PaceProfile, locale: Locale): String = when (paceProfile.type) {
    PaceProfileType.RELAXED -> stringResource(R.string.pace_profile_relaxed_title)
    PaceProfileType.NORMAL -> stringResource(R.string.pace_profile_normal_title)
    PaceProfileType.FAST -> stringResource(R.string.pace_profile_fast_title)
    PaceProfileType.CUSTOM -> {
        val speed = paceProfile.customFlatSpeedMetersPerSecond
        if (speed != null) {
            stringResource(
                R.string.pace_profile_custom_value_format,
                PaceSpeedParser.formatCustomKmh(speed, locale),
            )
        } else {
            stringResource(R.string.pace_profile_custom_title)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricRow(label: String, value: String) {
    val spacing = TrekCheckTheme.spacing
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space1),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun clampMeterLong(meters: Double): Long =
    if (meters.isFinite()) {
        meters.roundToLong().coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
    } else {
        0L
    }

@Composable
private fun formatMeters(meters: Double): String =
    stringResource(R.string.unit_meters_format, clampMeterLong(meters).toInt())

@Composable
private fun formatDistance(meters: Double, locale: Locale): String =
    when (val distance = HomeFormatters.resolveDistance(meters, locale)) {
        is DistanceDisplay.Meters -> stringResource(R.string.unit_meters_format, distance.meters)
        is DistanceDisplay.Kilometers -> stringResource(R.string.unit_kilometers_format, distance.formattedValue)
    }

@Composable
private fun coordinatesText(point: RouteWaypointSummary, locale: Locale): String =
    stringResource(
        R.string.preview_coordinates_format,
        String.format(locale, "%.5f", point.latitude),
        String.format(locale, "%.5f", point.longitude),
    )

@Composable
private fun waypointText(point: RouteWaypointSummary, locale: Locale): String {
    val coordinates = coordinatesText(point, locale)
    val elevation = point.elevationMeters ?: return coordinates
    return stringResource(R.string.preview_waypoint_with_elevation_format, coordinates, formatMeters(elevation))
}

@Composable
private fun highestText(point: RouteWaypointSummary, locale: Locale): String {
    val coordinates = coordinatesText(point, locale)
    val distance = formatDistance(point.distanceFromStartMeters, locale)
    val elevation = point.elevationMeters
        ?: return stringResource(R.string.preview_highest_no_ele_format, coordinates, distance)
    return stringResource(
        R.string.preview_highest_format,
        coordinates,
        formatMeters(elevation),
        distance,
    )
}

@Composable
private fun trackAlternativeText(details: RoutePreviewDetails, locale: Locale): String {
    val base = stringResource(
        R.string.preview_track_alt_format,
        details.pointCount,
        details.segmentCount,
        waypointText(details.start, locale),
        waypointText(details.end, locale),
        formatDistance(details.track.widthMeters, locale),
        formatDistance(details.track.heightMeters, locale),
    )
    val highest = details.highestPoint ?: return base
    return base + " " + stringResource(R.string.preview_track_alt_highest_format, highestText(highest, locale))
}

@Composable
private fun profileAlternativeText(
    profile: it.trekcheck.app.domain.model.ElevationProfilePreview,
    gainMeters: Double?,
    lossMeters: Double?,
    locale: Locale,
): String {
    val parts = mutableListOf(
        stringResource(
            R.string.preview_profile_alt_base_format,
            formatDistance(profile.totalDistanceMeters, locale),
            formatMeters(profile.minElevationMeters),
            formatMeters(profile.maxElevationMeters),
        ),
    )
    gainMeters?.let { gain ->
        parts += stringResource(
            R.string.preview_profile_alt_gain_format,
            stringResource(R.string.preview_gain_format, clampMeterLong(gain)),
        )
    }
    lossMeters?.let { loss ->
        parts += stringResource(
            R.string.preview_profile_alt_loss_format,
            stringResource(R.string.preview_loss_format, clampMeterLong(loss)),
        )
    }
    return parts.joinToString(" ")
}

@Composable
private fun RenameDialog(
    draft: String,
    errorCode: RouteManagementErrorCode?,
    isSaving: Boolean,
    onEvent: (RoutePreviewEvent) -> Unit,
) {
    val errorText = errorCode?.let {
        stringResource(
            when (it) {
                RouteManagementErrorCode.INVALID_TITLE -> R.string.preview_rename_error_empty
                RouteManagementErrorCode.NOT_FOUND -> R.string.preview_rename_error_not_found
                RouteManagementErrorCode.CORRUPTED_GEOMETRY,
                RouteManagementErrorCode.STORAGE_FAILED,
                -> R.string.preview_rename_error_storage
            },
        )
    }
    val counterHint = stringResource(
        R.string.preview_rename_hint_counter,
        draft.length,
        RouteTitleSanitizer.MAX_TITLE_LENGTH,
    )
    AlertDialog(
        onDismissRequest = { onEvent(RoutePreviewEvent.RenameDismissed) },
        title = { Text(stringResource(R.string.preview_rename_dialog_title)) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { onEvent(RoutePreviewEvent.RenameDraftChanged(it)) },
                label = { Text(stringResource(R.string.preview_rename_field_label)) },
                singleLine = true,
                isError = errorText != null,
                enabled = !isSaving,
                supportingText = { Text(errorText ?: counterHint) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onEvent(RoutePreviewEvent.RenameConfirmed) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("route_rename_field"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(RoutePreviewEvent.RenameConfirmed) },
                enabled = !isSaving,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("route_rename_confirm"),
            ) {
                Text(stringResource(R.string.preview_rename_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(RoutePreviewEvent.RenameDismissed) },
                enabled = !isSaving,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.preview_cancel))
            }
        },
    )
}

@Composable
private fun DeleteDialog(
    isDeleting: Boolean,
    onEvent: (RoutePreviewEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(RoutePreviewEvent.DeleteDismissed) },
        title = { Text(stringResource(R.string.preview_delete_dialog_title)) },
        text = {
            Text(
                if (isDeleting) {
                    stringResource(R.string.preview_delete_in_progress)
                } else {
                    stringResource(R.string.preview_delete_dialog_body)
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(RoutePreviewEvent.DeleteConfirmed) },
                enabled = !isDeleting,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("route_delete_confirm"),
            ) {
                Text(stringResource(R.string.preview_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(RoutePreviewEvent.DeleteDismissed) },
                enabled = !isDeleting,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("route_delete_cancel"),
            ) {
                Text(stringResource(R.string.preview_cancel))
            }
        },
    )
}
