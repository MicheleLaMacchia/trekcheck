package it.trekcheck.app.ui.home

import it.trekcheck.app.domain.model.ImportErrorCode
import it.trekcheck.app.domain.model.ImportGpxResult
import it.trekcheck.app.domain.repository.GpxInputSource
import it.trekcheck.app.domain.repository.LocalPlanRepository
import it.trekcheck.app.domain.usecase.ImportGpxUseCase
import it.trekcheck.app.ui.components.PrimaryActionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
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

class HomeViewModel(
    private val localPlanRepository: LocalPlanRepository,
    private val importGpxUseCase: ImportGpxUseCase? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    initialIsOffline: Boolean = true,
    initialImportActionState: PrimaryActionState = PrimaryActionState.Enabled,
    initialNotice: HomeNotice? = null,
    private val onImportDispatched: (() -> Unit)? = null,
) {
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + dispatcher)
    private var activeImportJob: Job? = null

    private val _uiState = MutableStateFlow(
        HomeUiState(
            isOffline = initialIsOffline,
            importActionState = initialImportActionState,
            activeNotice = initialNotice,
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    var ignoredImportClicksCount: Int = 0
        private set

    init {
        scope.launch {
            localPlanRepository.observeSavedPlans().collect { plans ->
                _uiState.update { current ->
                    current.copy(savedPlans = plans)
                }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.ImportGpxClicked,
            HomeEvent.ChooseAnotherFileClicked -> handleLaunchPickerClick()

            HomeEvent.DismissNotice -> _uiState.update { it.copy(activeNotice = null) }

            is HomeEvent.OpenRouteClicked -> {
                _uiState.update { it.copy(activeNotice = null) }
                _effects.trySend(HomeEffect.OpenRoutePreview(event.routeId))
            }
        }
    }

    fun importGpxSource(source: GpxInputSource) {
        val useCase = importGpxUseCase ?: return
        if (_uiState.value.importActionState != PrimaryActionState.Enabled || activeImportJob?.isActive == true) {
            ignoredImportClicksCount += 1
            return
        }

        _uiState.update {
            it.copy(
                importActionState = PrimaryActionState.InProgress,
                activeNotice = null,
            )
        }

        activeImportJob = scope.launch {
            try {
                val notice = when (val result = useCase(source)) {
                    is ImportGpxResult.Success -> HomeNotice.ImportSucceeded(
                        planTitle = result.plan.title,
                        hasElevation = result.hasElevation,
                        hasTimestamps = result.hasTimestamps,
                        planId = result.plan.id,
                    )

                    is ImportGpxResult.Failure -> HomeNotice.ImportFailed(
                        errorCode = result.error.code,
                    )
                }
                _uiState.update {
                    it.copy(
                        importActionState = PrimaryActionState.Enabled,
                        activeNotice = notice,
                    )
                }
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(importActionState = PrimaryActionState.Enabled) }
                throw cancellation
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        importActionState = PrimaryActionState.Enabled,
                        activeNotice = HomeNotice.ImportFailed(ImportErrorCode.STORAGE_FAILED),
                    )
                }
            } finally {
                activeImportJob = null
            }
        }
    }

    fun setImportActionState(state: PrimaryActionState) {
        _uiState.update { it.copy(importActionState = state) }
    }

    fun setOffline(isOffline: Boolean) {
        _uiState.update { it.copy(isOffline = isOffline) }
    }

    fun invokeOnScopeCompletion(handler: () -> Unit): DisposableHandle =
        scopeJob.invokeOnCompletion { handler() }

    fun clear() {
        _effects.close()
        scope.cancel()
    }

    private fun handleLaunchPickerClick() {
        if (_uiState.value.importActionState != PrimaryActionState.Enabled || activeImportJob?.isActive == true) {
            ignoredImportClicksCount += 1
            return
        }
        onImportDispatched?.invoke()
        _effects.trySend(HomeEffect.LaunchGpxPicker)
    }
}
