package it.trekcheck.app.ui.preview

import it.trekcheck.app.domain.model.RouteManagementErrorCode
import it.trekcheck.app.domain.model.RoutePreviewDetails

enum class RoutePreviewStatus {
    LOADING,
    READY,
    ERROR,
}

data class RenameDialogState(
    val draft: String,
    val errorCode: RouteManagementErrorCode? = null,
    val isSaving: Boolean = false,
)

data class RoutePreviewUiState(
    val status: RoutePreviewStatus = RoutePreviewStatus.LOADING,
    val details: RoutePreviewDetails? = null,
    val loadErrorCode: RouteManagementErrorCode? = null,
    val renameDialog: RenameDialogState? = null,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val actionErrorCode: RouteManagementErrorCode? = null,
)

sealed interface RoutePreviewEvent {
    data object BackClicked : RoutePreviewEvent
    data object RetryClicked : RoutePreviewEvent
    data object RenameClicked : RoutePreviewEvent
    data class RenameDraftChanged(val draft: String) : RoutePreviewEvent
    data object RenameConfirmed : RoutePreviewEvent
    data object RenameDismissed : RoutePreviewEvent
    data object DeleteClicked : RoutePreviewEvent
    data object DeleteConfirmed : RoutePreviewEvent
    data object DeleteDismissed : RoutePreviewEvent
    data object DismissActionError : RoutePreviewEvent
}

sealed interface RoutePreviewEffect {
    data object NavigateHome : RoutePreviewEffect
}
