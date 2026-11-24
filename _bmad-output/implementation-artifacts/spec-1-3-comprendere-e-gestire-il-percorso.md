---
title: 'Story 1.3: Comprendere e gestire il percorso'
type: 'feature'
created: '2026-11-24'
status: 'done'
route: 'dispatch'
baseline_commit: '1122637dc71d074aaf9b9863dd672405af4fec8a'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Dopo Story 1.2 i percorsi GPX vengono importati e listati nella Home, ma le schede non sono apribili: l'utente non può ispezionare metriche complete, estremi, eventuale punto più alto, tracciato vettoriale e profilo altimetrico con alternative testuali, né rinominare o eliminare un percorso salvato rimuovendone file privati e metadati.

**Approach:** Introdurre la schermata locale di anteprima percorso apribile dalla Home, calcolando metriche su geometria completa (`geometry_v1.bin`) con nullabilità esplicita e qualificatore «stimato», disegnando tracciato 2D e profilo altimetrico disponibile su Compose `Canvas` locale da geometria semplificata per segmento con equivalenti testuali accessibili, e persistendo rinomina ed eliminazione confermata (metadati Room + directory privata su disco).

## Boundaries & Constraints

**Always:**
- Mantenere il singolo modulo `:app` e i confini porte/adattatori (AD-1): dominio puro Kotlin senza dipendenze Android/Compose o DTO esterni.
- Calcolare tutte le metriche (distanza, punto iniziale, punto finale, eventuale punto più alto, quote min/max, dislivelli) sulla geometria completa (`geometry_v1.bin`); usare la geometria semplificata esclusivamente per il rendering (AD-2).
- Usare nullabilità esplicita senza zeri sostitutivi per quota, punto più alto, dislivelli e profilo assenti (AD-3, FR-2); etichettare dislivelli e profilo come «stimati».
- Disegnare il tracciato con `Canvas` vettoriale locale (zero rete), preservando i segmenti separati, affiancando un'alternativa testuale equivalente per tracciato e profilo (TalkBack, ordine focus, target ≥ 48×48 dp, contrasto M3 ≥ 4,5:1) e chiarendo il funzionamento offline senza promettere sfondi mappa offline (AD-7, UX-DR4, UX-DR11).
- Persistere la rinomina validata (non vuota, max 160 caratteri surrogate-safe) in Room senza alterare `planVersion`, ed eseguire l'eliminazione solo dopo conferma esplicita rimuovendo sia la riga Room sia `filesDir/routes/<routeId>/` (`source_v1.gpx` e `geometry_v1.bin`) (FR-3, FR-14, AD-2, AD-5).
- Escludere GPX, coordinate, nomi percorso, path o identità da errori tipizzati e diagnostica; zero permessi nel manifest e zero occorrenze di «sicur*» o «safe/safety» in `strings.xml`.

**Never:**
- Non introdurre chiamate di rete, SDK di mappe esterne o tile online (Story 2.2), né configurazione di partenza, fuso, passo o calcolo ETA (Story 1.4–1.5).
- Non creare tabelle o entità Room per singolo punto (`TrackPoint`) né migrare inutilmente lo schema Room v1.
- Non sostituire dati altimetrici mancanti con `0 m` né bloccare l'anteprima offline.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Apertura anteprima con quota | Tap su percorso salvato (o «Apri anteprima» da avviso import) con ≥ 2 punti quotati | Mostra titolo, distanza, inizio, fine, punto più alto (quota + distanza), quote min/max, salita/discesa «stimate», canvas 2D, profilo altimetrico e alternative testuali | N/A |
| Percorso senza quota o con 1 sola quota | `hasElevation = false` oppure `hasElevation = true` ma `elevationGainMeters = null` | Distanza, inizio/fine e canvas 2D visibili; punto più alto e min/max solo se `hasElevation`; dislivelli e profilo assenti segnalati come limite senza `0 m` | Avviso informativo locale |
| Tracciato multi-segmento o degenere | Segmenti disgiunti, punti tutti coincidenti, linea piatta/verticale (`minLat == maxLat`, `minEle == maxEle`) | Segmenti semplificati separatamente senza linee spurie; proiezione centrata senza `NaN`/divisioni per zero; marker inizio/fine/vetta distinti non solo per colore | Nessun crash |
| Offline e ricreazione Activity | Rete assente + rotazione/`recreate()` in anteprima | Anteprima, metriche, canvas e alternative testuali restano visibili per lo stesso percorso; nessuna promessa di mappe offline | Stato preservato |
| Rinomina percorso | Nuovo titolo valido vs stringa vuota/spazi o > 160 caratteri | Valido: salvato in Room (`title`, `updatedAtUtc`), visibile subito in anteprima e Home; vuoto: errore inline senza sovrascrivere; > 160: troncato/validato surrogate-safe | `INVALID_TITLE` / `NOT_FOUND` |
| Eliminazione con conferma | Tap «Elimina percorso» → «Annulla» oppure «Elimina» | «Annulla» mantiene dati intatti; «Elimina» rimuove riga `route_metadata` e cartella `routes/<id>/` (`source_v1.gpx`, `geometry_v1.bin`) e torna in Home | `STORAGE_FAILED` gestito senza crash |
| Geometria mancante o corrotta | `readGeometry(routeId)` restituisce `null` o lancia `IOException` | Stato di errore recuperabile in anteprima con ritorno in Home o eliminazione percorso; zero dettagli sensibili nei codici errore | `NOT_FOUND` / `CORRUPTED_GEOMETRY` |

</frozen-after-approval>

## Code Map

- `app/src/main/java/it/trekcheck/app/domain/model/GpxModels.kt` -- Riusare `GpxTrackPoint`, `GpxSegment`, `GpxBoundingBox`, `NormalizedRouteGeometry` e `GpxImportLimits`.
- `app/src/main/java/it/trekcheck/app/data/gpx/SafeXmlGpxParser.kt` (`L667-683`) & `domain/usecase/ImportGpxUseCase.kt` (`L227-278`) -- Estrarre in utility di dominio condivise `GeoDistance.haversineDistanceMeters` e `RouteTitleSanitizer` (`MAX_TITLE_LENGTH = 160`, surrogate-safe) per evitare duplicazioni.
- `app/src/main/java/it/trekcheck/app/data/local/files/VersionedRouteFileStorage.kt` (`L96-169`) -- Riusare `readGeometry(routeId)` e `deleteRoute(routeId)` senza modifiche al formato binario `geometry_v1.bin`.
- `app/src/main/java/it/trekcheck/app/data/local/db/RouteMetadataEntity.kt`, `RouteMetadataDao.kt`, `RoomLocalPlanRepository.kt` -- Schema Room v1 già completo dei metadati scalari (non modificare colonne né `1.json`); aggiungere query DAO `observeById` e `updateTitle` e implementare la porta di gestione percorso in `RoomLocalPlanRepository` (aggiornando anche `InMemoryLocalPlanRepository` per i test).
- `app/src/main/java/it/trekcheck/app/ui/home/HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `HomeFormatters.kt` -- Rendere cliccabile `SavedPlanCard`, aggiungere azione rapida di apertura nell'avviso `ImportSucceeded` e condividere i formattatori di unità.
- `app/src/main/java/it/trekcheck/app/MainActivity.kt` -- Gestire navigazione locale `Home` ↔ `RoutePreview(routeId)` preservata su `recreate()`, `BackHandler` e rientro su `ACTION_VIEW`/`ACTION_SEND`.
- `app/src/test/java/it/trekcheck/app/ManifestAndBackupConfigTest.kt` -- Mantenere verdi i guardrail su zero permessi, backup disabilitato e assenza di `"sicur"`/`"safe"` in `strings.xml`.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/it/trekcheck/app/domain/geo/GeoDistance.kt` & `domain/model/RouteTitleSanitizer.kt` -- Estrarre distanza Haversine e sanitizzazione/validazione titolo (1..160 char, surrogate-safe) riusandoli in `SafeXmlGpxParser` e `ImportGpxUseCase` -- Fonte unica SI e validazione coerente (AD-1, AD-3).
- [x] `app/src/main/java/it/trekcheck/app/domain/model/RoutePreviewModels.kt` & `domain/repository/RouteManagementRepository.kt` -- Definire modelli immutabili di anteprima (`RouteWaypointSummary`, `SimplifiedRoutePreview`, `ElevationProfilePreview`, `RoutePreviewDetails`), codici errore opachi e porta `RouteManagementRepository` -- Confini AD-1/AD-2/AD-3 e privacy NFR-1.
- [x] `app/src/main/java/it/trekcheck/app/domain/geo/RouteGeometrySimplifier.kt` -- Implementare analisi su geometria completa (estremi, eventuale punto più alto con distanza progressiva) e semplificazione iterativa Ramer–Douglas–Peucker per segmento + campionamento profilo solo per rendering -- Rispetta AD-2 e NFR-5 (< 1 s su 50k punti).
- [x] `app/src/main/java/it/trekcheck/app/domain/usecase/LoadRoutePreviewUseCase.kt`, `RenameRouteUseCase.kt`, `DeleteRouteUseCase.kt` -- Orchestrare caricamento off-UI-thread, rinomina validata (senza incremento `planVersion`) ed eliminazione completa con codici errore privi di dati sensibili -- FR-2, FR-3, FR-14, AD-5.
- [x] `app/src/main/java/it/trekcheck/app/data/local/db/RouteMetadataDao.kt`, `RoomLocalPlanRepository.kt`, `data/local/InMemoryLocalPlanRepository.kt` -- Aggiungere `observeById`/`updateTitle` in Room e implementare lettura metadati, rinomina e cancellazione atomica (riga DB + `deleteRoute` su file privati in `NonCancellable`) -- AD-2, FR-14.
- [x] `app/src/main/java/it/trekcheck/app/ui/preview/RoutePreviewContract.kt`, `RoutePreviewViewModel.kt`, `RouteTrackCanvas.kt`, `ElevationProfileCanvas.kt`, `RoutePreviewScreen.kt` -- Creare schermata anteprima Material 3 con riepilogo metriche, canvas vettoriale 2D, grafico altimetrico condizionale, alternative testuali TalkBack, dialog rinomina, dialog conferma eliminazione e CTA «Configura uscita» disabilitata per Story 1.4 -- FR-2, FR-3, AD-7, UX-DR4, UX-DR11.
- [x] `app/src/main/java/it/trekcheck/app/ui/home/HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `HomeFormatters.kt`, `MainActivity.kt`, `res/values/strings.xml` -- Collegare apertura percorso da `SavedPlanCard` e `ImportSucceeded`, navigazione `Home` ↔ `RoutePreview` persistente su `recreate()` e stringhe IT prive di termini vietati -- UX-DR3, UX-DR4.
- [x] `app/src/test/java/it/trekcheck/app/` -- Test unitari e Compose/Robolectric per tutti i casi della matrice I/O: RDP multi-segmento/degenere e misura 50k punti (NFR-5), use case e privacy errori, Room/file rename+delete, `RoutePreviewViewModel`, `RoutePreviewScreenTest` (TalkBack, alternative testuali, target 48dp, assenza quota, dialog) e navigazione `MainActivity` -- Copertura completa AC e CI.

**Acceptance Criteria:**
- Given un percorso salvato in Home, when lo apro in anteprima, then vedo distanza, punto iniziale, punto finale ed eventuale punto più alto calcolati sulla geometria completa, con quote, dislivelli «stimati» e profilo altimetrico mostrati solo se presenti nel file.
- Given l'anteprima del percorso, when visualizzo tracciato e profilo disponibile, then il rendering avviene localmente su `Canvas` vettoriale senza rete a partire da geometria semplificata per segmento, accompagnato da alternative testuali equivalenti accessibili a TalkBack.
- Given un dispositivo offline, when apro e ruoto l'anteprima, then metriche, tracciato vettoriale e alternative testuali restano pienamente consultabili senza promettere sfondi mappa offline.
- Given un percorso aperto in anteprima, when lo rinomino o confermo l'eliminazione nel dialog dedicato, then la rinomina persiste in Room aggiornando Home e anteprima, mentre l'eliminazione rimuove sia la riga Room sia `source_v1.gpx` e `geometry_v1.bin` riportando alla Home.
- Given un tracciato da 50.000 punti e i controlli statici di progetto, when eseguo la suite di verifica, then l'analisi off-UI-thread viene misurata rispetto a NFR-5 (< 1 s), nessun log/errore espone dati sensibili e manifest/stringhe rispettano zero permessi e divieto di «sicur*»/«safe».

## Implementation Notes

- **NFR-5 (analisi 50.000 punti):** misurata in ambiente JVM (`RouteGeometrySimplifierTest.analysisOf50kPointsStaysUnderOneSecond`) in `96 ms` pre-patch e `149 ms` post-patch (< `1.000 ms`, analisi completa inclusi dislivelli + RDP per segmento + campionamento profilo). La misura su dispositivo fisico/emulatore di riferimento resta **Non misurato su device** per assenza di hardware Android in VM headless.
- **Verifica CI finale (post-patch):** `./gradlew --no-daemon testDebugUnitTest lintDebug jacocoTestDebugUnitTestReport jacocoTestDebugUnitTestCoverageVerification assembleDebug` — `BUILD SUCCESSFUL`: `133` test su `26` suite (`0` fallimenti/errori/skip), `lintDebug` (`No issues found`, `warningsAsErrors = true`), JaCoCo linee `1.639/3.692` (`44,39%`, soglia `5%`) / branch `40,72%`, `assembleDebug` OK.
- **Scelte e deviazioni:** i 3 finding `verification-gap` e i `blind-hunter`/`edge-case-hunter` accettati (`18` patch, `2` rigettati `EC-7`/`EC-9`, `0` differiti) sono stati tutti risolti in questa sessione; `RoutePreviewViewModel` è mantenuto in `HomeViewModelHolder` per preservare bozze dialog e stato su `recreate()` e attendere la chiusura DB dopo tutti gli scope.

## Spec Change Log

## Review Triage Log

| ID | Fonte | Posizione | Verdetto | Evidenza e decisione |
|----|-------|-----------|----------|----------------------|
| BH-1 | `blind-hunter` | `MainActivity.kt` (`RoutePreviewHost`) | `medium` → `patch` | `RoutePreviewViewModel` è creato con `remember(routeId)` e distrutto su `recreate()`, perdendo `RenameDialog` (bozza/errori), `DeleteDialog` e `actionErrorCode` e ricalcolando RDP; gestirlo in `HomeViewModelHolder` per `routeId` e ripulirlo su chiusura/cambio route. |
| BH-2 | `blind-hunter` | `LoadRoutePreviewUseCase.kt`, `RouteGeometrySimplifier.kt`, `InMemoryLocalPlanRepository.kt`, `RoutePreviewScreen.kt`, `strings.xml` | `high` → `patch` | `LoadRoutePreviewUseCase` legge `gain`/`loss` da `metadata` invece che da `geometry_v1.bin` (`InMemoryLocalPlanRepository` restituisce `loss = null`), `RoutePreviewScreen` richiede `gain != null && loss != null` e `preview_elevation_partial_body` assume una sola quota anche con quote in segmenti disgiunti; calcolare `gain`/`loss` sulla geometria completa in `RouteGeometrySimplifier.analyze` (soglia condivisa `3.0 m`), aggiornare `RoutePreviewScreen` e il testo parziale. |
| BH-3 | `blind-hunter` | `RouteGeometrySimplifier.kt` (`sampleProfile`), `ElevationProfileCanvas.kt` | `medium` → `patch` | `sampleProfile` unisce punti quotati oltre gap privi di quota e cambi segmento; preservare sequenze contigue (`runs`) per segmento senza gap e disegnare path separati in `ElevationProfileCanvas` senza inventare pendenze dove mancano quote. |
| BH-4 | `blind-hunter` | `RoutePreviewScreen.kt` (`RenameDialog`), `RoutePreviewViewModel.kt` | `medium` → `patch` | `OutlinedTextField` in `RenameDialog` imposta `ImeAction.Done` senza `singleLine = true` né `KeyboardActions(onDone = ...)` e senza contatore caratteri; aggiungere `singleLine = true`, `KeyboardActions(onDone = ...)` e indicatore `${draft.length}/160`. |
| BH-5 | `blind-hunter` | `RoutePreviewScreen.kt`, `RouteTrackCanvas.kt`, `strings.xml` | `medium` → `patch` | `highestText` usa `?: 0.0`, la legenda cita sempre il triangolo anche con `highestPoint == null`, e i marker opachi sovrapposti nascondono `end` nei circuiti chiusi (`start == end`); rimuovere fallback `0.0`, rendere condizionale la legenda e differenziare dimensioni/anelli dei marker. |
| BH-6 | `blind-hunter` | `RoutePreviewScreen.kt`, `RoutePreviewViewModel.kt` | `medium` → `patch` | `actionErrorCode` è reso in fondo alla colonna senza titolo specifico né `heading()`, e `BackClicked`/`RenameClicked` non sono bloccati durante `isDeleting`/`isSaving`; spostare il banner in alto con scroll, aggiungere `heading()` + titolo mappato e bloccare azioni concorrenti nel ViewModel. |
| BH-7 | `blind-hunter` | `RouteMetadataDao.kt`, `RoomLocalPlanRepositoryTest.kt` | `low` → `patch` | `RouteMetadataDao.observeById` e i relativi stub di test non sono usati da alcun chiamante; rimuovere codice morto. |
| BH-8 | `blind-hunter` | `strings.xml` (`preview_profile_alt_format`) | `low` → `patch` | `preview_profile_alt_format` duplica la parola `"stimata"` interpolando `preview_gain_format`/`preview_loss_format`; correggere la stringa in `"Salita %4$s, discesa %5$s."`. |
| BH-9 | `blind-hunter` | `HomeScreen.kt` (`SavedPlanCard`, `HomeNoticeCard`), `strings.xml` | `low` → `patch` | `SavedPlanCard` applica `semantics { onClick(label = ...) }` prima del `clickable` interno di `Surface`, e `home_notice_open_preview` non cita il nome percorso in `contentDescription`; usare `Modifier.clickable(onClickLabel = ...)` su `Surface` non cliccabile e aggiungere `contentDescription` su `home_notice_open_preview`. |
| BH-10 | `blind-hunter` | `app/src/test/java/it/trekcheck/app/` | `medium` → `patch` | Mancano test su segmenti a 1 punto/quote sparse in `sampleProfile`, fallback/errori in `RoutePreviewViewModelTest` e `RoutePreviewScreenTest` (`LOADING`, `NOT_FOUND`, `STORAGE_FAILED`, `isSaving`/`isDeleting`, dark theme, `fontScale = 2f`, IME `Done`), apertura da `SavedPlanCard` in `MainActivityRoutePreviewTest` e semantica `SavedPlanCard` in `HomeOpenRouteTest`. |
| EC-1 | `edge-case-hunter` | `RoutePreviewScreen.kt:327-350` | `high` → `patch` | Stessa causa di `BH-2`: `gain != null && loss != null` nasconde il profilo quando solo uno è presente; gestire indipendentemente `gain`/`loss` e calcolarli da `geometry_v1.bin`. |
| EC-2 | `edge-case-hunter` | `RoomLocalPlanRepository.kt:58-63`, `VersionedRouteFileStorage.kt:163-169` | `medium` → `patch` | `deleteRoute` non verifica il booleano restituito da `deleteRecursively()`, lasciando file orfani su disco senza restituire `STORAGE_FAILED`; lanciare `IOException` se `routeDir.exists() && !routeDir.deleteRecursively()`. |
| EC-3 | `edge-case-hunter` | `RoutePreviewViewModel.kt:50-53` | `medium` → `patch` | Stessa causa di `BH-6`: `RenameClicked` non controlla `isDeleting`; bloccare mentre `isDeleting` è `true`. |
| EC-4 | `edge-case-hunter` | `MainActivity.kt:151-156` | `medium` → `patch` | Stessa causa di `BH-1`: `HomeViewModelHolder.onCleared()` chiude Room senza attendere eventuali job attivi di `RoutePreviewViewModel`; attendere anche lo scope preview prima di `database.close()`. |
| EC-5 | `edge-case-hunter` | `RoutePreviewScreen.kt:476-478` | `low` → `patch` | `meters.roundToLong().toInt()` può andare in overflow oltre `Int`; applicare `coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()`. |
| EC-6 | `edge-case-hunter` | `ElevationProfileCanvas.kt:33-48` | `low` → `patch` | `maxElevationMeters - minElevationMeters` può fare overflow a `+Infinity` producendo `NaN` in `mapY`; verificare `elevationRange.isFinite() && elevationRange > 0.0` e differenza finita. |
| EC-7 | `edge-case-hunter` | `RouteGeometrySimplifier.kt:130-136` | `low` (rigettato) | Attraversamento antimeridiano ±180° è altamente improbabile nell'uso escursionistico quotidiano e richiede media circolare/unwrapping su più file oltre a una correzione banale. |
| EC-8 | `edge-case-hunter` | `RoutePreviewViewModel.kt:59` | `low` → `patch` | `event.draft.take(MAX_DRAFT_CHARS)` può spezzare una coppia surrogata UTF-16 all'indice 399; arretrare di 1 su `Character.isHighSurrogate`. |
| EC-9 | `edge-case-hunter` | `RoomLocalPlanRepository.kt:58-63` | `false` (rigettato) | `Design Notes` della spec e `RouteManagementRepository.kt:12` fissano esplicitamente la sequenza DB prima dei file in `NonCancellable` (senza rollback cross-store, la cui dicitura nel task list non è modificabile dalla review); la verifica fallimento I/O è già coperta da `EC-2`. |
| VG-1 | `verification-gap` | `RoomLocalPlanRepositoryTest.kt`, `MainActivityRoutePreviewTest.kt` | `high` → `patch` | Evidenza pre-verificata: `elevationLossMeters` in `RoomLocalPlanRepository.getRouteMetadata` e `Salita`/`Discesa`/`elevation_profile_canvas`/`elevation_profile_text_alternative` nel flusso end-to-end non erano asseriti; aggiungere asserzioni in entrambi i test. |
| VG-2 | `verification-gap` | `MainActivityRoutePreviewTest.kt`, `MainActivity.kt:133` | `medium` → `patch` | Evidenza pre-verificata: nessun test verifica che `ACTION_VIEW`/`ACTION_SEND` in `onNewIntent` chiuda un'anteprima aperta (`selectedRouteId = null`); aggiungere test in `MainActivityRoutePreviewTest`. |
| VG-3 | `verification-gap` | `RoutePreviewViewModelTest.kt`, `RoutePreviewScreenTest.kt` | `medium` → `patch` | Evidenza pre-verificata: mancano test sui rami di fallimento rename/delete, `DismissActionError` e rendering `route_preview_notice`; aggiungerli in entrambi i test. |

## Design Notes

- **Separazione metriche vs rendering (AD-2):** `LoadRoutePreviewUseCase` calcola distanza, estremi e `highestPoint` iterando tutti i punti di `NormalizedRouteGeometry`; `RouteGeometrySimplifier` esegue poi RDP iterativo (stack esplicito, `O(N log N)` medio, sicuro su 200.000 punti) su coordinate metriche locali `((lon - lon0) * cos(meanLat), lat - lat0)` separatamente per ogni `GpxSegment`, ancorando primo/ultimo punto del segmento e l'indice del `highestPoint` globale.
- **Ordine di eliminazione (AD-2, FR-14):** `RoomLocalPlanRepository.deleteRoute` elimina prima la riga `route_metadata` e poi la directory `filesDir/routes/<routeId>/` in `withContext(NonCancellable)`, evitando che i `Flow` della UI osservino metadati privi di `geometry_v1.bin`.

## Verification

**Commands:**
- `./gradlew --no-daemon testDebugUnitTest lintDebug jacocoTestDebugUnitTestReport jacocoTestDebugUnitTestCoverageVerification assembleDebug` -- expected: BUILD SUCCESSFUL, 0 warning/errori Lint (`warningsAsErrors = true`), tutti i test unitari e Robolectric/Compose verdi inclusa misura analisi 50k punti, coverage linee JaCoCo ≥ 5%, APK debug compilato.

**Manual checks (if no CLI):**
- Su dispositivo/emulatore API 26+: verificare lettura TalkBack delle alternative testuali del canvas e del profilo, navigazione focus tastiera, ingrandimento testo 200% e misura NFR-5 (< 1 s analisi) su hardware reale.
