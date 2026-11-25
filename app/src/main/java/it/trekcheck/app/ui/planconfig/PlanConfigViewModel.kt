package it.trekcheck.app.ui.planconfig

import it.trekcheck.app.domain.model.LoadPlanConfigResult
import it.trekcheck.app.domain.model.PaceProfile
import it.trekcheck.app.domain.model.PaceProfileType
import it.trekcheck.app.domain.model.PaceSpeedParseResult
import it.trekcheck.app.domain.model.PaceSpeedParser
import it.trekcheck.app.domain.model.PlanConfigErrorCode
import it.trekcheck.app.domain.model.SavePlanConfigResult
import it.trekcheck.app.domain.time.AmbiguousTimeChoice
import it.trekcheck.app.domain.time.DepartureResolution
import it.trekcheck.app.domain.time.LocalDepartureResolver
import it.trekcheck.app.domain.usecase.LoadRoutePlanConfigUseCase
import it.trekcheck.app.domain.usecase.SaveRoutePlanConfigUseCase
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlanConfigViewModel(
    private val routeId: String,
    private val loadPlanConfig: LoadRoutePlanConfigUseCase,
    private val savePlanConfig: SaveRoutePlanConfigUseCase,
    private val clock: Clock = Clock.systemUTC(),
    private val deviceZoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val localeProvider: () -> Locale = { Locale.getDefault() },
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var loadJob: Job? = null
    private var saveJob: Job? = null

    private val _uiState = MutableStateFlow(
        PlanConfigUiState(
            routeId = routeId,
            deviceZoneId = deviceZoneProvider(),
        ),
    )
    val uiState: StateFlow<PlanConfigUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PlanConfigEffect>(Channel.BUFFERED)
    val effects: Flow<PlanConfigEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: PlanConfigEvent) {
        when (event) {
            PlanConfigEvent.BackClicked -> {
                if (!_uiState.value.isSaving) {
                    _effects.trySend(PlanConfigEffect.Cancel)
                }
            }

            PlanConfigEvent.RetryClicked -> {
                if (!_uiState.value.isSaving) {
                    load()
                }
            }

            PlanConfigEvent.OpenDatePicker -> {
                if (_uiState.value.status == PlanConfigStatus.READY && !_uiState.value.isSaving) {
                    _uiState.update { it.copy(isDatePickerVisible = true) }
                }
            }

            PlanConfigEvent.DismissDatePicker -> {
                _uiState.update { it.copy(isDatePickerVisible = false) }
            }

            is PlanConfigEvent.DateSelected -> updateSchedule(
                date = event.date,
                closeDatePicker = true,
            )

            PlanConfigEvent.OpenTimePicker -> {
                if (_uiState.value.status == PlanConfigStatus.READY && !_uiState.value.isSaving) {
                    _uiState.update { it.copy(isTimePickerVisible = true) }
                }
            }

            PlanConfigEvent.DismissTimePicker -> {
                _uiState.update { it.copy(isTimePickerVisible = false) }
            }

            is PlanConfigEvent.TimeSelected -> updateSchedule(
                time = LocalTime.of(event.time.hour, event.time.minute),
                closeTimePicker = true,
            )

            PlanConfigEvent.OpenTimezoneDialog -> {
                if (_uiState.value.status == PlanConfigStatus.READY && !_uiState.value.isSaving) {
                    _uiState.update {
                        it.copy(
                            isTimezoneDialogVisible = true,
                            timezoneSearchQuery = "",
                        )
                    }
                }
            }

            PlanConfigEvent.DismissTimezoneDialog -> {
                _uiState.update {
                    it.copy(
                        isTimezoneDialogVisible = false,
                        timezoneSearchQuery = "",
                    )
                }
            }

            is PlanConfigEvent.TimezoneSearchChanged -> {
                _uiState.update {
                    it.copy(timezoneSearchQuery = event.query.take(MAX_TIMEZONE_QUERY_CHARS))
                }
            }

            is PlanConfigEvent.ZoneSelected -> updateSchedule(
                zoneId = event.zoneId,
                closeTimezoneDialog = true,
            )

            PlanConfigEvent.UseDeviceZoneClicked -> updateSchedule(
                zoneId = deviceZoneProvider(),
            )

            PlanConfigEvent.ApplySuggestedDstTime -> {
                _uiState.update { state ->
                    val nonExistent = state.resolution as? DepartureResolution.NonExistent
                        ?: return@update state
                    val suggested = nonExistent.suggestedValidZonedDateTime
                    val nextDate = suggested.toLocalDate()
                    val nextTime = LocalTime.of(suggested.hour, suggested.minute)
                    val nextResolution = LocalDepartureResolver.resolve(nextDate, nextTime, state.routeZoneId)
                    state.copy(
                        localDate = nextDate,
                        localTime = nextTime,
                        resolution = nextResolution,
                        selectedAmbiguousChoice = null,
                        saveErrorCode = null,
                    )
                }
            }

            is PlanConfigEvent.AmbiguousChoiceSelected -> {
                _uiState.update { state ->
                    if (state.resolution !is DepartureResolution.Ambiguous) return@update state
                    state.copy(
                        selectedAmbiguousChoice = event.choice,
                        saveErrorCode = null,
                    )
                }
            }

            is PlanConfigEvent.PaceTypeSelected -> {
                _uiState.update { state ->
                    if (state.isSaving) return@update state
                    val error = if (event.type == PaceProfileType.CUSTOM && state.hasEditedCustomSpeed) {
                        (PaceSpeedParser.parseCustomKmh(state.customSpeedInput) as? PaceSpeedParseResult.Invalid)?.error
                    } else {
                        null
                    }
                    state.copy(
                        selectedPaceType = event.type,
                        customSpeedError = error,
                        saveErrorCode = null,
                    )
                }
            }

            is PlanConfigEvent.CustomSpeedChanged -> {
                _uiState.update { state ->
                    if (state.isSaving) return@update state
                    val trimmedLengthSafe = event.rawInput.take(MAX_SPEED_INPUT_CHARS)
                    val parsed = PaceSpeedParser.parseCustomKmh(trimmedLengthSafe)
                    val error = (parsed as? PaceSpeedParseResult.Invalid)?.error
                    state.copy(
                        customSpeedInput = trimmedLengthSafe,
                        hasEditedCustomSpeed = true,
                        customSpeedError = error,
                        saveErrorCode = null,
                    )
                }
            }

            PlanConfigEvent.SaveClicked -> submit()

            PlanConfigEvent.DismissSaveError -> {
                _uiState.update { it.copy(saveErrorCode = null) }
            }
        }
    }

    fun invokeOnScopeCompletion(onComplete: () -> Unit) {
        val rootJob = scope.coroutineContext[Job]
        if (rootJob == null) {
            onComplete()
            return
        }
        rootJob.invokeOnCompletion { onComplete() }
    }

    fun clear() {
        _effects.close()
        scope.cancel()
    }

    private fun load() {
        loadJob?.cancel()
        val currentDeviceZone = deviceZoneProvider()
        _uiState.update {
            it.copy(
                status = PlanConfigStatus.LOADING,
                deviceZoneId = currentDeviceZone,
                loadErrorCode = null,
                saveErrorCode = null,
            )
        }
        loadJob = scope.launch {
            val result = try {
                loadPlanConfig(routeId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                LoadPlanConfigResult.Failure(PlanConfigErrorCode.STORAGE_FAILED)
            }

            _uiState.update { state ->
                when (result) {
                    is LoadPlanConfigResult.Failure -> state.copy(
                        status = PlanConfigStatus.ERROR,
                        loadErrorCode = result.code,
                    )

                    is LoadPlanConfigResult.Success -> {
                        val config = result.config
                        val zone = config.routeZoneId
                        val (initialDate, initialTime, initialZoned) = if (config.departureInstantUtc != null) {
                            val zoned = ZonedDateTime.ofInstant(config.departureInstantUtc, zone)
                            Triple(
                                zoned.toLocalDate(),
                                LocalTime.of(zoned.hour, zoned.minute),
                                zoned,
                            )
                        } else {
                            // Never derive departure from historical GPX timestamps; default to tomorrow 08:00 in route zone
                            val defaultDate = ZonedDateTime.ofInstant(clock.instant(), zone)
                                .toLocalDate()
                                .plusDays(1)
                            Triple(defaultDate, DEFAULT_DEPARTURE_TIME, null)
                        }
                        val resolution = LocalDepartureResolver.resolve(initialDate, initialTime, zone)
                        val initialAmbiguousChoice = if (
                            resolution is DepartureResolution.Ambiguous &&
                            initialZoned != null
                        ) {
                            if (initialZoned.offset == resolution.earlierOption.offset) {
                                AmbiguousTimeChoice.EARLIER
                            } else {
                                AmbiguousTimeChoice.LATER
                            }
                        } else {
                            null
                        }
                        val customInput = config.paceProfile.customFlatSpeedMetersPerSecond
                            ?.let { PaceSpeedParser.formatCustomKmh(it, localeProvider()) }
                            ?: ""

                        state.copy(
                            status = PlanConfigStatus.READY,
                            routeId = config.routeId,
                            routeTitle = config.routeTitle,
                            hasElevation = config.hasElevation,
                            hasTimestamps = config.hasTimestamps,
                            hasSavedConfig = config.departureInstantUtc != null,
                            localDate = initialDate,
                            localTime = initialTime,
                            routeZoneId = zone,
                            deviceZoneId = deviceZoneProvider(),
                            resolution = resolution,
                            selectedAmbiguousChoice = initialAmbiguousChoice,
                            selectedPaceType = config.paceProfile.type,
                            customSpeedInput = customInput,
                            hasEditedCustomSpeed = false,
                            customSpeedError = null,
                            loadErrorCode = null,
                            saveErrorCode = null,
                        )
                    }
                }
            }
        }
    }

    private fun updateSchedule(
        date: LocalDate? = null,
        time: LocalTime? = null,
        zoneId: ZoneId? = null,
        closeDatePicker: Boolean = false,
        closeTimePicker: Boolean = false,
        closeTimezoneDialog: Boolean = false,
    ) {
        _uiState.update { state ->
            if (state.status != PlanConfigStatus.READY || state.isSaving) return@update state
            val nextDate = date ?: state.localDate
            val nextTime = time ?: state.localTime
            val nextZone = zoneId ?: state.routeZoneId
            val nextResolution = LocalDepartureResolver.resolve(nextDate, nextTime, nextZone)
            state.copy(
                localDate = nextDate,
                localTime = nextTime,
                routeZoneId = nextZone,
                resolution = nextResolution,
                selectedAmbiguousChoice = null,
                isDatePickerVisible = if (closeDatePicker) false else state.isDatePickerVisible,
                isTimePickerVisible = if (closeTimePicker) false else state.isTimePickerVisible,
                isTimezoneDialogVisible = if (closeTimezoneDialog) false else state.isTimezoneDialogVisible,
                timezoneSearchQuery = if (closeTimezoneDialog) "" else state.timezoneSearchQuery,
                saveErrorCode = null,
            )
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.status != PlanConfigStatus.READY || state.isSaving || saveJob?.isActive == true) {
            return
        }
        if (state.resolvedDepartureInstant == null) {
            _uiState.update { it.copy(saveErrorCode = PlanConfigErrorCode.UNRESOLVED_DST) }
            return
        }

        val paceProfile = when (state.selectedPaceType) {
            PaceProfileType.RELAXED,
            PaceProfileType.NORMAL,
            PaceProfileType.FAST,
            -> PaceProfile(state.selectedPaceType)

            PaceProfileType.CUSTOM -> {
                when (val parsed = PaceSpeedParser.parseCustomKmh(state.customSpeedInput)) {
                    is PaceSpeedParseResult.Invalid -> {
                        _uiState.update {
                            it.copy(
                                hasEditedCustomSpeed = true,
                                customSpeedError = parsed.error,
                                saveErrorCode = PlanConfigErrorCode.INVALID_CUSTOM_PACE,
                            )
                        }
                        return
                    }

                    is PaceSpeedParseResult.Valid -> PaceProfile(
                        type = PaceProfileType.CUSTOM,
                        customFlatSpeedMetersPerSecond = parsed.speedMetersPerSecond,
                    )
                }
            }
        }

        _uiState.update { it.copy(isSaving = true, saveErrorCode = null) }
        saveJob = scope.launch {
            val result = try {
                savePlanConfig(
                    routeId = state.routeId,
                    localDate = state.localDate,
                    localTime = state.localTime,
                    routeZoneId = state.routeZoneId,
                    ambiguousTimeChoice = state.selectedAmbiguousChoice,
                    paceProfile = paceProfile,
                )
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isSaving = false) }
                throw cancellation
            } catch (_: Throwable) {
                SavePlanConfigResult.Failure(PlanConfigErrorCode.STORAGE_FAILED)
            } finally {
                saveJob = null
            }

            when (result) {
                SavePlanConfigResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.trySend(PlanConfigEffect.SavedAndClose)
                }

                is SavePlanConfigResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveErrorCode = result.code,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val DEFAULT_DEPARTURE_TIME: LocalTime = LocalTime.of(8, 0)
        const val MAX_SPEED_INPUT_CHARS = 16
        const val MAX_TIMEZONE_QUERY_CHARS = 80
    }
}
