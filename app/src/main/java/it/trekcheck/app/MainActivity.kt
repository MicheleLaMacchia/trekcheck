package it.trekcheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import it.trekcheck.app.data.local.InMemoryLocalPlanRepository
import it.trekcheck.app.ui.home.HomeEvent
import it.trekcheck.app.ui.home.HomeNotice
import it.trekcheck.app.ui.home.HomeScreen
import it.trekcheck.app.ui.home.HomeUiState
import it.trekcheck.app.ui.home.HomeViewModel
import it.trekcheck.app.ui.theme.TrekCheckTheme

class MainActivity : ComponentActivity() {
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val restoredNotice = if (savedInstanceState?.getBoolean(KEY_IMPORT_NOTICE_VISIBLE, false) == true) {
            HomeNotice.ImportComingSoon
        } else {
            null
        }

        homeViewModel = HomeViewModel(
            localPlanRepository = InMemoryLocalPlanRepository(),
            initialNotice = restoredNotice,
        )

        setContent {
            val uiState by homeViewModel.uiState.collectAsState()
            TrekCheckApp(
                uiState = uiState,
                onEvent = homeViewModel::onEvent,
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::homeViewModel.isInitialized) {
            outState.putBoolean(
                KEY_IMPORT_NOTICE_VISIBLE,
                homeViewModel.uiState.value.activeNotice == HomeNotice.ImportComingSoon,
            )
        }
    }

    override fun onDestroy() {
        if (::homeViewModel.isInitialized) {
            homeViewModel.clear()
        }
        super.onDestroy()
    }

    private companion object {
        const val KEY_IMPORT_NOTICE_VISIBLE = "import_notice_visible"
    }
}

@Composable
fun TrekCheckApp(
    uiState: HomeUiState = HomeUiState(),
    onEvent: (HomeEvent) -> Unit = {},
) {
    TrekCheckTheme {
        HomeScreen(
            uiState = uiState,
            onEvent = onEvent,
        )
    }
}
