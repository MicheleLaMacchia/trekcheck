package it.trekcheck.app.ui.planconfig

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.trekcheck.app.R
import it.trekcheck.app.domain.model.PaceProfileType
import it.trekcheck.app.domain.model.PaceSpeedInputError
import it.trekcheck.app.domain.model.PlanConfigErrorCode
import it.trekcheck.app.domain.time.AmbiguousTimeChoice
import it.trekcheck.app.domain.time.DepartureResolution
import it.trekcheck.app.ui.components.PrimaryActionState
import it.trekcheck.app.ui.components.TrekCheckPrimaryAction
import it.trekcheck.app.ui.home.HomeFormatters
import it.trekcheck.app.ui.theme.TrekCheckTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlanConfigScreen(
    uiState: PlanConfigUiState,
    onEvent: (PlanConfigEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = TrekCheckTheme.spacing
    val locale = activeLocale()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.saveErrorCode) {
        if (uiState.saveErrorCode != null) {
            scrollState.scrollTo(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("plan_config_screen"),
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
                val backCd = stringResource(R.string.plan_config_back_cd)
                TextButton(
                    onClick = { onEvent(PlanConfigEvent.BackClicked) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("plan_config_back")
                        .semantics { contentDescription = backCd },
                ) {
                    Text(stringResource(R.string.preview_back), style = MaterialTheme.typography.labelLarge)
                }

                uiState.saveErrorCode?.let { errorCode ->
                    ConfigNoticeSurface(
                        title = stringResource(R.string.plan_config_save_error_title),
                        body = saveErrorBody(errorCode),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        borderColor = MaterialTheme.colorScheme.error,
                        iconRes = R.drawable.ic_error_notice,
                        onDismiss = { onEvent(PlanConfigEvent.DismissSaveError) },
                        isLiveRegion = true,
                        tag = "plan_config_save_error",
                    )
                }

                when (uiState.status) {
                    PlanConfigStatus.LOADING -> Text(
                        text = stringResource(R.string.plan_config_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )

                    PlanConfigStatus.ERROR -> LoadErrorContent(
                        code = uiState.loadErrorCode ?: PlanConfigErrorCode.STORAGE_FAILED,
                        onEvent = onEvent,
                    )

                    PlanConfigStatus.READY -> ReadyContent(
                        uiState = uiState,
                        locale = locale,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }

    if (uiState.isDatePickerVisible) {
        DepartureDateDialog(
            initialDate = uiState.localDate,
            onConfirm = { selected -> onEvent(PlanConfigEvent.DateSelected(selected)) },
            onDismiss = { onEvent(PlanConfigEvent.DismissDatePicker) },
        )
    }

    if (uiState.isTimePickerVisible) {
        DepartureTimeDialog(
            initialTime = uiState.localTime,
            onConfirm = { selected -> onEvent(PlanConfigEvent.TimeSelected(selected)) },
            onDismiss = { onEvent(PlanConfigEvent.DismissTimePicker) },
        )
    }

    if (uiState.isTimezoneDialogVisible) {
        TimezonePickerDialog(
            currentZoneId = uiState.routeZoneId,
            deviceZoneId = uiState.deviceZoneId,
            referenceDateTime = LocalDateTime.of(uiState.localDate, uiState.localTime),
            query = uiState.timezoneSearchQuery,
            locale = locale,
            onQueryChange = { onEvent(PlanConfigEvent.TimezoneSearchChanged(it)) },
            onZoneSelected = { onEvent(PlanConfigEvent.ZoneSelected(it)) },
            onDismiss = { onEvent(PlanConfigEvent.DismissTimezoneDialog) },
        )
    }
}

@Composable
private fun ReadyContent(
    uiState: PlanConfigUiState,
    locale: Locale,
    onEvent: (PlanConfigEvent) -> Unit,
) {
    val spacing = TrekCheckTheme.spacing

    Text(
        text = stringResource(R.string.plan_config_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .testTag("plan_config_title")
            .semantics { heading() },
    )
    Text(
        text = uiState.routeTitle,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("plan_config_route_title"),
    )

    if (uiState.hasTimestamps) {
        ConfigNoticeSurface(
            title = stringResource(R.string.plan_config_gpx_timestamps_title),
            body = stringResource(R.string.plan_config_gpx_timestamps_body),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            iconRes = R.drawable.ic_info_notice,
            isLiveRegion = false,
            tag = "plan_config_gpx_timestamps_notice",
        )
    }

    if (!uiState.hasElevation) {
        ConfigNoticeSurface(
            title = stringResource(R.string.plan_config_no_elevation_title),
            body = stringResource(R.string.plan_config_no_elevation_body),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            iconRes = R.drawable.ic_info_notice,
            isLiveRegion = false,
            tag = "plan_config_no_elevation_notice",
        )
    }

    SectionHeading(stringResource(R.string.plan_config_departure_heading))
    DepartureCard(uiState = uiState, locale = locale, onEvent = onEvent)

    when (val res = uiState.resolution) {
        is DepartureResolution.NonExistent -> NonExistentDstNotice(
            resolution = res,
            locale = locale,
            isSaving = uiState.isSaving,
            onApplySuggested = { onEvent(PlanConfigEvent.ApplySuggestedDstTime) },
        )

        is DepartureResolution.Ambiguous -> AmbiguousDstNotice(
            resolution = res,
            selectedChoice = uiState.selectedAmbiguousChoice,
            locale = locale,
            isSaving = uiState.isSaving,
            onSelectChoice = { onEvent(PlanConfigEvent.AmbiguousChoiceSelected(it)) },
        )

        is DepartureResolution.Valid, null -> Unit
    }

    SectionHeading(stringResource(R.string.plan_config_timezone_heading))
    TimezoneCard(uiState = uiState, onEvent = onEvent)

    SectionHeading(stringResource(R.string.plan_config_pace_heading))
    Text(
        text = stringResource(R.string.plan_config_pace_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PaceProfileSection(uiState = uiState, onEvent = onEvent)

    if (!uiState.canSave && !uiState.isSaving) {
        val hintText = when {
            uiState.isDstUnresolved -> stringResource(R.string.plan_config_disabled_hint_dst)
            !uiState.isCustomSpeedValid -> stringResource(R.string.plan_config_disabled_hint_pace)
            else -> null
        }
        hintText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("plan_config_disabled_hint"),
            )
        }
    }

    val actionState = when {
        uiState.isSaving -> PrimaryActionState.InProgress
        uiState.canSave -> PrimaryActionState.Enabled
        else -> PrimaryActionState.Disabled
    }
    val disabledCd = when {
        uiState.isDstUnresolved -> stringResource(R.string.plan_config_disabled_hint_dst)
        !uiState.isCustomSpeedValid -> stringResource(R.string.plan_config_disabled_hint_pace)
        else -> stringResource(R.string.home_action_state_disabled)
    }

    TrekCheckPrimaryAction(
        label = stringResource(R.string.plan_config_action_save),
        inProgressLabel = stringResource(R.string.plan_config_action_saving),
        disabledStateDescription = disabledCd,
        state = actionState,
        onClick = { onEvent(PlanConfigEvent.SaveClicked) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.space2)
            .testTag("plan_config_save"),
    )
}

@Composable
private fun DepartureCard(
    uiState: PlanConfigUiState,
    locale: Locale,
    onEvent: (PlanConfigEvent) -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_config_departure_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            val formattedDate = HomeFormatters.formatLocalDate(uiState.localDate, locale)
            val formattedTime = HomeFormatters.formatLocalTime(uiState.localTime, locale)

            OutlinedButton(
                onClick = { onEvent(PlanConfigEvent.OpenDatePicker) },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("plan_config_date_button"),
            ) {
                Text(
                    text = stringResource(R.string.plan_config_date_button_format, formattedDate),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            OutlinedButton(
                onClick = { onEvent(PlanConfigEvent.OpenTimePicker) },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("plan_config_time_button"),
            ) {
                Text(
                    text = stringResource(R.string.plan_config_time_button_format, formattedTime),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            uiState.resolvedDepartureInstant?.let { instant ->
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                    Text(
                        text = stringResource(R.string.plan_config_resolved_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = HomeFormatters.formatZonedDateTimeWithOffset(instant, uiState.routeZoneId, locale),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("plan_config_resolved_departure"),
                    )
                    if (uiState.isRouteZoneDifferentFromDevice) {
                        Text(
                            text = stringResource(
                                R.string.plan_config_device_equivalent_format,
                                HomeFormatters.formatZonedDateTimeWithOffset(instant, uiState.deviceZoneId, locale),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("plan_config_device_equivalent"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NonExistentDstNotice(
    resolution: DepartureResolution.NonExistent,
    locale: Locale,
    isSaving: Boolean,
    onApplySuggested: () -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    val extended = TrekCheckTheme.extendedColors
    val requestedTime = HomeFormatters.formatLocalTime(resolution.requestedLocalDateTime.toLocalTime(), locale)
    val gapStart = HomeFormatters.formatLocalTime(resolution.gapStartLocal.toLocalTime(), locale)
    val gapEnd = HomeFormatters.formatLocalTime(resolution.gapEndLocal.toLocalTime(), locale)
    val suggestedTime = HomeFormatters.formatLocalTime(resolution.suggestedValidZonedDateTime.toLocalTime(), locale)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_config_dst_notice")
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = extended.warningContainer,
        border = BorderStroke(1.dp, extended.warning),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info_notice),
                    contentDescription = null,
                    tint = extended.onWarningContainer,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.plan_config_dst_nonexistent_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = extended.onWarningContainer,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(
                text = stringResource(
                    R.string.plan_config_dst_nonexistent_body,
                    resolution.zoneId.id,
                    requestedTime,
                    gapStart,
                    gapEnd,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.onWarningContainer,
            )
            OutlinedButton(
                onClick = onApplySuggested,
                enabled = !isSaving,
                border = BorderStroke(1.dp, extended.warning),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("plan_config_dst_use_suggested"),
            ) {
                Text(
                    text = stringResource(R.string.plan_config_dst_use_suggested_format, suggestedTime),
                    color = extended.onWarningContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun AmbiguousDstNotice(
    resolution: DepartureResolution.Ambiguous,
    selectedChoice: AmbiguousTimeChoice?,
    locale: Locale,
    isSaving: Boolean,
    onSelectChoice: (AmbiguousTimeChoice) -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    val extended = TrekCheckTheme.extendedColors
    val requestedTime = HomeFormatters.formatLocalTime(resolution.requestedLocalDateTime.toLocalTime(), locale)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_config_dst_notice")
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = extended.warningContainer,
        border = BorderStroke(1.dp, extended.warning),
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
                    tint = extended.onWarningContainer,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.plan_config_dst_ambiguous_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = extended.onWarningContainer,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(
                text = stringResource(
                    R.string.plan_config_dst_ambiguous_body,
                    resolution.zoneId.id,
                    requestedTime,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.onWarningContainer,
            )

            Column(modifier = Modifier.selectableGroup()) {
                val earlierLabel = stringResource(
                    R.string.plan_config_dst_option_earlier_format,
                    HomeFormatters.formatLocalTime(resolution.earlierOption.toLocalTime(), locale),
                    HomeFormatters.formatUtcOffset(resolution.earlierOption.offset),
                )
                val laterLabel = stringResource(
                    R.string.plan_config_dst_option_later_format,
                    HomeFormatters.formatLocalTime(resolution.laterOption.toLocalTime(), locale),
                    HomeFormatters.formatUtcOffset(resolution.laterOption.offset),
                )

                AmbiguousOptionRow(
                    label = earlierLabel,
                    selected = selectedChoice == AmbiguousTimeChoice.EARLIER,
                    enabled = !isSaving,
                    textColor = extended.onWarningContainer,
                    onClick = { onSelectChoice(AmbiguousTimeChoice.EARLIER) },
                    modifier = Modifier.testTag("plan_config_dst_option_earlier"),
                )
                AmbiguousOptionRow(
                    label = laterLabel,
                    selected = selectedChoice == AmbiguousTimeChoice.LATER,
                    enabled = !isSaving,
                    textColor = extended.onWarningContainer,
                    onClick = { onSelectChoice(AmbiguousTimeChoice.LATER) },
                    modifier = Modifier.testTag("plan_config_dst_option_later"),
                )
            }

            if (selectedChoice == null) {
                Text(
                    text = stringResource(R.string.plan_config_dst_choose_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.onWarningContainer,
                )
            }
        }
    }
}

@Composable
private fun AmbiguousOptionRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = TrekCheckTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(modifier = Modifier.width(spacing.space2))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = textColor)
    }
}

@Composable
private fun TimezoneCard(
    uiState: PlanConfigUiState,
    onEvent: (PlanConfigEvent) -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    val extended = TrekCheckTheme.extendedColors
    val currentOffset = remember(uiState.routeZoneId, uiState.localDate, uiState.localTime) {
        uiState.routeZoneId.rules.getOffset(LocalDateTime.of(uiState.localDate, uiState.localTime))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_config_timezone_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Text(
                text = stringResource(
                    R.string.plan_config_timezone_active_format,
                    uiState.routeZoneId.id,
                    HomeFormatters.formatUtcOffset(currentOffset),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("plan_config_timezone_active"),
            )

            if (uiState.isRouteZoneDifferentFromDevice) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_config_timezone_diff_notice"),
                    shape = MaterialTheme.shapes.small,
                    color = extended.warningContainer,
                    border = BorderStroke(1.dp, extended.warning),
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.space3),
                        verticalArrangement = Arrangement.spacedBy(spacing.space2),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_info_notice),
                                contentDescription = null,
                                tint = extended.onWarningContainer,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(
                                    R.string.plan_config_timezone_diff_body,
                                    uiState.deviceZoneId.id,
                                    uiState.routeZoneId.id,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = extended.onWarningContainer,
                            )
                        }
                        TextButton(
                            onClick = { onEvent(PlanConfigEvent.UseDeviceZoneClicked) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("plan_config_use_device_zone"),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.plan_config_use_device_zone_format,
                                    uiState.deviceZoneId.id,
                                ),
                                color = extended.onWarningContainer,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { onEvent(PlanConfigEvent.OpenTimezoneDialog) },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("plan_config_timezone_button"),
            ) {
                Text(
                    text = stringResource(R.string.plan_config_timezone_change),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun PaceProfileSection(
    uiState: PlanConfigUiState,
    onEvent: (PlanConfigEvent) -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_config_pace_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .padding(spacing.space4)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            PaceOptionRow(
                type = PaceProfileType.RELAXED,
                title = stringResource(R.string.pace_profile_relaxed_title),
                description = stringResource(R.string.pace_profile_relaxed_desc),
                selected = uiState.selectedPaceType == PaceProfileType.RELAXED,
                enabled = !uiState.isSaving,
                onClick = { onEvent(PlanConfigEvent.PaceTypeSelected(PaceProfileType.RELAXED)) },
            )
            PaceOptionRow(
                type = PaceProfileType.NORMAL,
                title = stringResource(R.string.pace_profile_normal_title),
                description = stringResource(R.string.pace_profile_normal_desc),
                selected = uiState.selectedPaceType == PaceProfileType.NORMAL,
                enabled = !uiState.isSaving,
                onClick = { onEvent(PlanConfigEvent.PaceTypeSelected(PaceProfileType.NORMAL)) },
            )
            PaceOptionRow(
                type = PaceProfileType.FAST,
                title = stringResource(R.string.pace_profile_fast_title),
                description = stringResource(R.string.pace_profile_fast_desc),
                selected = uiState.selectedPaceType == PaceProfileType.FAST,
                enabled = !uiState.isSaving,
                onClick = { onEvent(PlanConfigEvent.PaceTypeSelected(PaceProfileType.FAST)) },
            )
            PaceOptionRow(
                type = PaceProfileType.CUSTOM,
                title = stringResource(R.string.pace_profile_custom_title),
                description = stringResource(R.string.pace_profile_custom_desc),
                selected = uiState.selectedPaceType == PaceProfileType.CUSTOM,
                enabled = !uiState.isSaving,
                onClick = { onEvent(PlanConfigEvent.PaceTypeSelected(PaceProfileType.CUSTOM)) },
            )

            if (uiState.selectedPaceType == PaceProfileType.CUSTOM) {
                val showError = uiState.hasEditedCustomSpeed && uiState.customSpeedError != null
                val supportingText = when (uiState.customSpeedError) {
                    PaceSpeedInputError.EMPTY -> stringResource(R.string.plan_config_custom_speed_error_empty)
                    PaceSpeedInputError.NOT_A_NUMBER -> stringResource(R.string.plan_config_custom_speed_error_nan)
                    PaceSpeedInputError.OUT_OF_RANGE -> stringResource(R.string.plan_config_custom_speed_error_range)
                    null -> stringResource(R.string.plan_config_custom_speed_hint)
                }

                OutlinedTextField(
                    value = uiState.customSpeedInput,
                    onValueChange = { onEvent(PlanConfigEvent.CustomSpeedChanged(it)) },
                    label = { Text(stringResource(R.string.plan_config_custom_speed_label)) },
                    supportingText = { Text(supportingText) },
                    isError = showError,
                    singleLine = true,
                    enabled = !uiState.isSaving,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (uiState.canSave) {
                                onEvent(PlanConfigEvent.SaveClicked)
                            }
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.space2)
                        .testTag("plan_config_custom_speed_field"),
                )
            }
        }
    }
}

@Composable
private fun PaceOptionRow(
    type: PaceProfileType,
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = spacing.space1)
            .testTag("plan_config_pace_${type.name}"),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(modifier = Modifier.width(spacing.space2))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadErrorContent(
    code: PlanConfigErrorCode,
    onEvent: (PlanConfigEvent) -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    ConfigNoticeSurface(
        title = stringResource(
            if (code == PlanConfigErrorCode.NOT_FOUND) {
                R.string.preview_error_not_found_title
            } else {
                R.string.preview_error_storage_title
            },
        ),
        body = stringResource(
            if (code == PlanConfigErrorCode.NOT_FOUND) {
                R.string.preview_error_not_found_body
            } else {
                R.string.preview_error_storage_body
            },
        ),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        borderColor = MaterialTheme.colorScheme.error,
        iconRes = R.drawable.ic_error_notice,
        isLiveRegion = true,
        tag = "plan_config_load_error",
    )
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        if (code != PlanConfigErrorCode.NOT_FOUND) {
            OutlinedButton(
                onClick = { onEvent(PlanConfigEvent.RetryClicked) },
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.preview_retry))
            }
        }
        OutlinedButton(
            onClick = { onEvent(PlanConfigEvent.BackClicked) },
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.plan_config_back_to_preview))
        }
    }
}

@Composable
private fun ConfigNoticeSurface(
    title: String,
    body: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    iconRes: Int,
    isLiveRegion: Boolean,
    tag: String,
    onDismiss: (() -> Unit)? = null,
) {
    val spacing = TrekCheckTheme.spacing
    val baseModifier = Modifier
        .fillMaxWidth()
        .testTag(tag)
    val surfaceModifier = if (isLiveRegion) {
        baseModifier.semantics { liveRegion = LiveRegionMode.Polite }
    } else {
        baseModifier
    }

    Surface(
        modifier = surfaceModifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
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
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    modifier = Modifier.semantics { heading() },
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartureDateDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onConfirm(selected)
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("plan_config_date_confirm"),
            ) {
                Text(stringResource(R.string.plan_config_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.preview_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartureTimeDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    var useKeyboardInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_config_time_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                TextButton(
                    onClick = { useKeyboardInput = !useKeyboardInput },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("plan_config_time_toggle_mode"),
                ) {
                    Text(
                        stringResource(
                            if (useKeyboardInput) {
                                R.string.plan_config_time_use_dial
                            } else {
                                R.string.plan_config_time_use_keyboard
                            },
                        ),
                    )
                }
                if (useKeyboardInput) {
                    TimeInput(state = timePickerState)
                } else {
                    TimePicker(state = timePickerState)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                },
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("plan_config_time_confirm"),
            ) {
                Text(stringResource(R.string.plan_config_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.preview_cancel))
            }
        },
    )
}

@Composable
private fun TimezonePickerDialog(
    currentZoneId: ZoneId,
    deviceZoneId: ZoneId,
    referenceDateTime: LocalDateTime,
    query: String,
    locale: Locale,
    onQueryChange: (String) -> Unit,
    onZoneSelected: (ZoneId) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = TrekCheckTheme.spacing
    val allZones = remember(currentZoneId, deviceZoneId) {
        val canonical = ZoneId.getAvailableZoneIds()
            .asSequence()
            .filter { id -> id == "UTC" || (id.contains('/') && !id.startsWith("SystemV/")) }
            .sorted()
            .map { ZoneId.of(it) }
            .toList()
        val pinned = listOf(currentZoneId, deviceZoneId).distinct()
        val remaining = canonical.filter { it !in pinned }
        pinned + remaining
    }
    val trimmedQuery = query.trim().lowercase(locale)
    val filteredZones = remember(allZones, trimmedQuery, locale) {
        if (trimmedQuery.isEmpty()) {
            allZones
        } else {
            allZones.filter { zone ->
                zone.id.lowercase(locale).contains(trimmedQuery) ||
                    zone.getDisplayName(TextStyle.FULL, locale).lowercase(locale).contains(trimmedQuery)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("plan_config_timezone_dialog"),
        title = { Text(stringResource(R.string.plan_config_timezone_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.plan_config_timezone_search_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_config_timezone_search"),
                )
                if (filteredZones.isEmpty()) {
                    Text(
                        text = stringResource(R.string.plan_config_timezone_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = spacing.space2),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .selectableGroup(),
                    ) {
                        items(filteredZones, key = { it.id }) { zone ->
                            val offset = zone.rules.getOffset(referenceDateTime)
                            val displayName = zone.getDisplayName(TextStyle.SHORT, locale)
                            val isSelected = zone == currentZoneId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .selectable(
                                        selected = isSelected,
                                        role = Role.RadioButton,
                                        onClick = { onZoneSelected(zone) },
                                    )
                                    .padding(vertical = spacing.space2, horizontal = spacing.space1)
                                    .testTag("plan_config_timezone_item_${zone.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = isSelected, onClick = null)
                                Spacer(modifier = Modifier.width(spacing.space2))
                                Column {
                                    Text(
                                        text = zone.id,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "${HomeFormatters.formatUtcOffset(offset)} · $displayName",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.preview_cancel))
            }
        },
    )
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
private fun saveErrorBody(code: PlanConfigErrorCode): String = stringResource(
    when (code) {
        PlanConfigErrorCode.NOT_FOUND -> R.string.preview_error_not_found_body
        PlanConfigErrorCode.INVALID_ZONE_ID -> R.string.plan_config_save_error_zone
        PlanConfigErrorCode.INVALID_CUSTOM_PACE -> R.string.plan_config_save_error_pace
        PlanConfigErrorCode.UNRESOLVED_DST -> R.string.plan_config_save_error_dst
        PlanConfigErrorCode.STORAGE_FAILED -> R.string.preview_error_storage_body
    },
)

@Composable
private fun activeLocale(): Locale {
    val configuration = LocalConfiguration.current
    return if (!configuration.locales.isEmpty) {
        configuration.locales[0] ?: Locale.getDefault()
    } else {
        Locale.getDefault()
    }
}
