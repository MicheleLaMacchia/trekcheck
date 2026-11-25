package it.trekcheck.app.ui.preview

import it.trekcheck.app.domain.model.RouteActionResult
import it.trekcheck.app.domain.model.RouteManagementErrorCode
import it.trekcheck.app.domain.model.RoutePreviewResult
import it.trekcheck.app.domain.model.RouteTitleSanitizer
import it.trekcheck.app.domain.usecase.DeleteRouteUseCase
import it.trekcheck.app.domain.usecase.LoadRoutePreviewUseCase
import it.trekcheck.app.domain.usecase.RenameRouteUseCase
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

class RoutePreviewViewModel(
    private val routeId: String,
    private val loadRoutePreview: LoadRoutePreviewUseCase,
    private val renameRoute: RenameRouteUseCase,
    private val deleteRoute: DeleteRouteUseCase,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(RoutePreviewUiState())
    val uiState: StateFlow<RoutePreviewUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RoutePreviewEffect>(Channel.BUFFERED)
    val effects: Flow<RoutePreviewEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: RoutePreviewEvent) {
        when (event) {
            RoutePreviewEvent.BackClicked -> {
                val state = _uiState.value
                if (!state.isDeleting && state.renameDialog?.isSaving != true) {
                    _effects.trySend(RoutePreviewEffect.NavigateHome)
                }
            }

            RoutePreviewEvent.RetryClicked -> {
                val state = _uiState.value
                if (!state.isDeleting && state.renameDialog?.isSaving != true) {
                    load()
                }
            }

            RoutePreviewEvent.RefreshRequested -> {
                val state = _uiState.value
                if (!state.isDeleting && state.renameDialog?.isSaving != true) {
                    load()
                }
            }

            RoutePreviewEvent.ConfigureClicked -> {
                val state = _uiState.value
                if (state.status == RoutePreviewStatus.READY &&
                    !state.isDeleting &&
                    state.renameDialog?.isSaving != true
                ) {
                    _effects.trySend(RoutePreviewEffect.NavigateToPlanConfig(routeId))
                }
            }

            RoutePreviewEvent.RenameClicked -> {
                val state = _uiState.value
                if (state.isDeleting) return
                val title = state.details?.title ?: return
                _uiState.update { it.copy(renameDialog = RenameDialogState(draft = title)) }
            }

            is RoutePreviewEvent.RenameDraftChanged -> _uiState.update { state ->
                state.renameDialog?.let { dialog ->
                    state.copy(
                        renameDialog = dialog.copy(
                            draft = truncateDraftSurrogateSafe(event.draft),
                            errorCode = null,
                        ),
                    )
                } ?: state
            }

            RoutePreviewEvent.RenameConfirmed -> confirmRename()
            RoutePreviewEvent.RenameDismissed -> _uiState.update { state ->
                if (state.renameDialog?.isSaving == true) state else state.copy(renameDialog = null)
            }

            RoutePreviewEvent.DeleteClicked -> _uiState.update {
                if (it.isDeleting || it.renameDialog?.isSaving == true) it else it.copy(isDeleteDialogVisible = true)
            }

            RoutePreviewEvent.DeleteDismissed -> _uiState.update {
                if (it.isDeleting) it else it.copy(isDeleteDialogVisible = false)
            }

            RoutePreviewEvent.DeleteConfirmed -> confirmDelete()
            RoutePreviewEvent.DismissActionError -> _uiState.update { it.copy(actionErrorCode = null) }
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
        _uiState.update {
            it.copy(status = RoutePreviewStatus.LOADING, loadErrorCode = null)
        }
        loadJob = scope.launch {
            val result = try {
                loadRoutePreview(routeId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                RoutePreviewResult.Failure(RouteManagementErrorCode.STORAGE_FAILED)
            }
            _uiState.update { state ->
                when (result) {
                    is RoutePreviewResult.Success -> state.copy(
                        status = RoutePreviewStatus.READY,
                        details = result.details,
                        loadErrorCode = null,
                    )

                    is RoutePreviewResult.Failure -> state.copy(
                        status = RoutePreviewStatus.ERROR,
                        details = null,
                        loadErrorCode = result.code,
                    )
                }
            }
        }
    }

    private fun confirmRename() {
        val stateSnapshot = _uiState.value
        if (stateSnapshot.isDeleting) return
        val dialog = stateSnapshot.renameDialog ?: return
        if (dialog.isSaving) return
        if (RouteTitleSanitizer.sanitize(dialog.draft) == null) {
            _uiState.update {
                it.copy(renameDialog = dialog.copy(errorCode = RouteManagementErrorCode.INVALID_TITLE))
            }
            return
        }
        _uiState.update { it.copy(renameDialog = dialog.copy(isSaving = true, errorCode = null)) }
        scope.launch {
            val result = try {
                renameRoute(routeId, dialog.draft)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                RouteActionResult.Failure(RouteManagementErrorCode.STORAGE_FAILED)
            }
            _uiState.update { state ->
                when (result) {
                    is RouteActionResult.Success -> state.copy(
                        renameDialog = null,
                        details = state.details?.let { details ->
                            details.copy(title = result.title ?: details.title)
                        },
                    )

                    is RouteActionResult.Failure -> state.copy(
                        renameDialog = state.renameDialog?.copy(isSaving = false, errorCode = result.code),
                    )
                }
            }
        }
    }

    private fun confirmDelete() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        scope.launch {
            val result = try {
                deleteRoute(routeId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                RouteActionResult.Failure(RouteManagementErrorCode.STORAGE_FAILED)
            }
            when (result) {
                is RouteActionResult.Success -> {
                    _uiState.update { it.copy(isDeleting = false, isDeleteDialogVisible = false) }
                    _effects.trySend(RoutePreviewEffect.NavigateHome)
                }

                is RouteActionResult.Failure -> _uiState.update {
                    it.copy(
                        isDeleting = false,
                        isDeleteDialogVisible = false,
                        actionErrorCode = result.code,
                    )
                }
            }
        }
    }

    private fun truncateDraftSurrogateSafe(draft: String): String {
        if (draft.length <= MAX_DRAFT_CHARS) return draft
        var end = MAX_DRAFT_CHARS
        if (Character.isHighSurrogate(draft[end - 1])) {
            end -= 1
        }
        return draft.substring(0, end)
    }

    private companion object {
        const val MAX_DRAFT_CHARS = 400
    }
}
