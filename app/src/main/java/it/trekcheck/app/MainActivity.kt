package it.trekcheck.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.trekcheck.app.data.gpx.ContentResolverGpxSource
import it.trekcheck.app.data.gpx.SafeXmlGpxParser
import it.trekcheck.app.data.local.db.RoomLocalPlanRepository
import it.trekcheck.app.data.local.db.TrekCheckDatabase
import it.trekcheck.app.data.local.files.VersionedRouteFileStorage
import it.trekcheck.app.domain.repository.GpxInputSource
import it.trekcheck.app.domain.usecase.ImportGpxUseCase
import it.trekcheck.app.ui.home.HomeEffect
import it.trekcheck.app.ui.home.HomeEvent
import it.trekcheck.app.ui.home.HomeScreen
import it.trekcheck.app.ui.home.HomeUiState
import it.trekcheck.app.ui.home.HomeViewModel
import it.trekcheck.app.ui.theme.TrekCheckTheme
import java.io.File
import java.io.InputStream

class MainActivity : ComponentActivity() {
    private lateinit var homeViewModel: HomeViewModel
    private var isGpxPickerLaunchInFlight = false

    private val openGpxDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        isGpxPickerLaunchInFlight = false
        if (uri != null) {
            importUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val holder = ViewModelProvider(
            this,
            HomeViewModelHolderFactory(applicationContext),
        )[HomeViewModelHolder::class.java]
        homeViewModel = holder.homeViewModel

        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        setContent {
            LaunchedEffect(homeViewModel) {
                homeViewModel.effects.collect { effect ->
                    when (effect) {
                        HomeEffect.LaunchGpxPicker -> launchGpxPicker()
                    }
                }
            }

            val uiState by homeViewModel.uiState.collectAsState()
            TrekCheckApp(
                uiState = uiState,
                onEvent = homeViewModel::onEvent,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun launchGpxPicker() {
        if (isGpxPickerLaunchInFlight) return
        isGpxPickerLaunchInFlight = true
        try {
            openGpxDocumentLauncher.launch(GPX_PICKER_MIME_TYPES)
        } catch (_: ActivityNotFoundException) {
            isGpxPickerLaunchInFlight = false
            homeViewModel.importGpxSource(UnreadableGpxInputSource)
        }
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        if ((incomingIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return
        val action = incomingIntent.action ?: return
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return

        val uri = extractGpxUri(incomingIntent)
        if (uri != null) {
            importUri(uri)
        } else {
            homeViewModel.importGpxSource(UnreadableGpxInputSource)
        }
    }

    private fun importUri(uri: Uri) {
        homeViewModel.importGpxSource(ContentResolverGpxSource(applicationContext, uri))
    }

    private class HomeViewModelHolder(
        private val database: TrekCheckDatabase,
        val homeViewModel: HomeViewModel,
    ) : ViewModel() {
        override fun onCleared() {
            homeViewModel.invokeOnScopeCompletion {
                database.close()
            }
            homeViewModel.clear()
            super.onCleared()
        }
    }

    private class HomeViewModelHolderFactory(
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val routesRootDir = File(appContext.filesDir, VersionedRouteFileStorage.ROUTES_DIR_NAME)
            val routeFileStorage = VersionedRouteFileStorage(routesRootDir)
            val database = TrekCheckDatabase.create(appContext)
            val repository = RoomLocalPlanRepository(
                routeMetadataDao = database.routeMetadataDao(),
                routeFileStorage = routeFileStorage,
            )
            val importGpxUseCase = ImportGpxUseCase(
                gpxParser = SafeXmlGpxParser(),
                routeFileStorage = routeFileStorage,
                routeImportRepository = repository,
                fallbackRouteTitle = appContext.getString(R.string.import_default_route_title),
            )
            val viewModel = HomeViewModel(
                localPlanRepository = repository,
                importGpxUseCase = importGpxUseCase,
            )
            return HomeViewModelHolder(
                database = database,
                homeViewModel = viewModel,
            ) as T
        }
    }

    private object UnreadableGpxInputSource : GpxInputSource {
        override val suggestedTitle: String? = null
        override val knownByteSize: Long? = null
        override fun openStream(): InputStream? = null
    }

    internal companion object {
        val GPX_PICKER_MIME_TYPES = arrayOf(
            "application/gpx+xml",
            "application/gpx",
            "application/xml",
            "text/xml",
            "application/octet-stream",
            "*/*",
        )

        internal fun extractGpxUri(intent: Intent?): Uri? {
            if (intent == null) return null
            return when (intent.action) {
                Intent.ACTION_VIEW -> {
                    intent.data ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                }

                Intent.ACTION_SEND -> {
                    intent.extractSendStreamUri()
                        ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                }

                else -> null
            }
        }

        private fun Intent.extractSendStreamUri(): Uri? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
            }
        }
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
