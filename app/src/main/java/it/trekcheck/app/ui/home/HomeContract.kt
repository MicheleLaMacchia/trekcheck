package it.trekcheck.app.ui.home

import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.ui.components.PrimaryActionState

enum class HomeNotice {
    ImportComingSoon,
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
    data object DismissNotice : HomeEvent
}
