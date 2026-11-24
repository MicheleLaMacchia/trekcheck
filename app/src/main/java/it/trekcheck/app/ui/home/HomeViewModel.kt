package it.trekcheck.app.ui.home

import it.trekcheck.app.domain.repository.LocalPlanRepository
import it.trekcheck.app.ui.components.PrimaryActionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val localPlanRepository: LocalPlanRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    initialIsOffline: Boolean = true,
    initialImportActionState: PrimaryActionState = PrimaryActionState.Enabled,
    initialNotice: HomeNotice? = null,
    private val onImportDispatched: (() -> Unit)? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(
        HomeUiState(
            isOffline = initialIsOffline,
            importActionState = initialImportActionState,
            activeNotice = initialNotice,
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
            HomeEvent.ImportGpxClicked -> handleImportGpxClick()
            HomeEvent.DismissNotice -> _uiState.update { it.copy(activeNotice = null) }
        }
    }

    fun setImportActionState(state: PrimaryActionState) {
        _uiState.update { it.copy(importActionState = state) }
    }

    fun setOffline(isOffline: Boolean) {
        _uiState.update { it.copy(isOffline = isOffline) }
    }

    fun clear() {
        scope.cancel()
    }

    private fun handleImportGpxClick() {
        if (_uiState.value.importActionState != PrimaryActionState.Enabled) {
            ignoredImportClicksCount += 1
            return
        }
        onImportDispatched?.invoke()
        _uiState.update { current ->
            current.copy(activeNotice = HomeNotice.ImportComingSoon)
        }
    }
}
