package it.trekcheck.app.ui.planconfig

import it.trekcheck.app.domain.model.PaceProfileType
import it.trekcheck.app.domain.model.PaceSpeedInputError
import it.trekcheck.app.domain.model.PaceSpeedParseResult
import it.trekcheck.app.domain.model.PaceSpeedParser
import it.trekcheck.app.domain.model.PlanConfigErrorCode
import it.trekcheck.app.domain.time.AmbiguousTimeChoice
import it.trekcheck.app.domain.time.DepartureResolution
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class PlanConfigStatus {
    LOADING,
    READY,
    ERROR,
}

data class PlanConfigUiState(
    val status: PlanConfigStatus = PlanConfigStatus.LOADING,
    val routeId: String = "",
    val routeTitle: String = "",
    val hasElevation: Boolean = false,
    val hasTimestamps: Boolean = false,
    val hasSavedConfig: Boolean = false,
    val localDate: LocalDate = LocalDate.of(2026, 1, 1),
    val localTime: LocalTime = LocalTime.of(8, 0),
    val routeZoneId: ZoneId = ZoneId.of("UTC"),
    val deviceZoneId: ZoneId = ZoneId.of("UTC"),
    val resolution: DepartureResolution? = null,
    val selectedAmbiguousChoice: AmbiguousTimeChoice? = null,
    val selectedPaceType: PaceProfileType = PaceProfileType.NORMAL,
    val customSpeedInput: String = "",
    val hasEditedCustomSpeed: Boolean = false,
    val customSpeedError: PaceSpeedInputError? = null,
    val isDatePickerVisible: Boolean = false,
    val isTimePickerVisible: Boolean = false,
    val isTimezoneDialogVisible: Boolean = false,
    val timezoneSearchQuery: String = "",
    val isSaving: Boolean = false,
    val loadErrorCode: PlanConfigErrorCode? = null,
    val saveErrorCode: PlanConfigErrorCode? = null,
) {
    val resolvedDepartureInstant: Instant?
        get() = when (val res = resolution) {
            is DepartureResolution.Valid -> res.instantUtc
            is DepartureResolution.NonExistent -> null
            is DepartureResolution.Ambiguous -> when (selectedAmbiguousChoice) {
                AmbiguousTimeChoice.EARLIER -> res.earlierOption.toInstant()
                AmbiguousTimeChoice.LATER -> res.laterOption.toInstant()
                null -> null
            }

            null -> null
        }

    val isDstUnresolved: Boolean
        get() = status == PlanConfigStatus.READY && resolvedDepartureInstant == null

    val isCustomSpeedValid: Boolean
        get() = selectedPaceType != PaceProfileType.CUSTOM ||
            PaceSpeedParser.parseCustomKmh(customSpeedInput) is PaceSpeedParseResult.Valid

    val canSave: Boolean
        get() = status == PlanConfigStatus.READY &&
            !isSaving &&
            resolvedDepartureInstant != null &&
            isCustomSpeedValid

    val isRouteZoneDifferentFromDevice: Boolean
        get() = routeZoneId != deviceZoneId
}

sealed interface PlanConfigEvent {
    data object BackClicked : PlanConfigEvent
    data object RetryClicked : PlanConfigEvent
    data object OpenDatePicker : PlanConfigEvent
    data object DismissDatePicker : PlanConfigEvent
    data class DateSelected(val date: LocalDate) : PlanConfigEvent
    data object OpenTimePicker : PlanConfigEvent
    data object DismissTimePicker : PlanConfigEvent
    data class TimeSelected(val time: LocalTime) : PlanConfigEvent
    data object OpenTimezoneDialog : PlanConfigEvent
    data object DismissTimezoneDialog : PlanConfigEvent
    data class TimezoneSearchChanged(val query: String) : PlanConfigEvent
    data class ZoneSelected(val zoneId: ZoneId) : PlanConfigEvent
    data object UseDeviceZoneClicked : PlanConfigEvent
    data object ApplySuggestedDstTime : PlanConfigEvent
    data class AmbiguousChoiceSelected(val choice: AmbiguousTimeChoice) : PlanConfigEvent
    data class PaceTypeSelected(val type: PaceProfileType) : PlanConfigEvent
    data class CustomSpeedChanged(val rawInput: String) : PlanConfigEvent
    data object SaveClicked : PlanConfigEvent
    data object DismissSaveError : PlanConfigEvent
}

sealed interface PlanConfigEffect {
    data object Cancel : PlanConfigEffect
    data object SavedAndClose : PlanConfigEffect
}
