package it.trekcheck.app.ui.home

import it.trekcheck.app.domain.model.ImportErrorCode
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.ui.components.PrimaryActionState

sealed interface HomeNotice {
    data class ImportSucceeded(
        val planTitle: String,
        val hasElevation: Boolean,
        val hasTimestamps: Boolean,
    ) : HomeNotice

    data class ImportFailed(
        val errorCode: ImportErrorCode,
    ) : HomeNotice
}

sealed interface HomeEffect {
    data object LaunchGpxPicker : HomeEffect
}

data class HomeUiState(
    val savedPlans: List<SavedPlanSummary> = emptyList(),
    val isOffline: Boolean = true,
    val importActionState: PrimaryActionState = PrimaryActionState.Enabled,
    val activeNotice: HomeNotice? = null,
) {
    val isEmpty: Boolean
        get() = savedPlans.isEmpty()
}

sealed interface HomeEvent {
    data object ImportGpxClicked : HomeEvent
    data object ChooseAnotherFileClicked : HomeEvent
    data object DismissNotice : HomeEvent
}
