---
title: 'Story 1.2: Importare GPX in sicurezza'
type: 'feature'
created: '2026-11-24'
status: 'done'
route: 'dispatch'
baseline_commit: '18eb615eb3271d938b70cbc8364f01ba814fa522'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Dopo Story 1.1 il pulsante «Importa GPX» mostra un segnaposto e i percorsi risiedono solo in memoria (`InMemoryLocalPlanRepository`), senza parser GPX, ingressi Android (selettore/apertura/condivisione) né persistenza locale ibrida.

**Approach:** Implementare importazione GPX 1.0/1.1 (`trk`/`rte`) da SAF `OpenDocument`, `ACTION_VIEW` e `ACTION_SEND` con parser XML streaming protetto da DTD/entità esterne, limiti provvisori (20 MB, 200.000 punti), storage ibrido atomico (file privati versionati per GPX e geometria + soli metadati/riferimenti in Room), elaborazione off-UI-thread con progresso TalkBack ed errori recuperabili privi di dati sensibili nei log.

## Boundaries & Constraints

**Always:**
- Singolo modulo `:app` e confini porte/adattatori (AD-1): dominio puro Kotlin senza dipendenze UI/DTO esterni.
- Parser XML sicuro (AD-2, NFR-2): disabilitare DTD remoti/entità esterne e rigettare `<!DOCTYPE`/`<!ENTITY` senza accessi esterni.
- Storage ibrido (AD-2): GPX raw e geometria normalizzata a segmenti in file privati versionati; solo metadati e riferimenti in Room (mai un'entità Room per `TrackPoint`).
- Limiti provvisori in streaming: ≤ 20 MB (`20_971_520` byte) e ≤ 200.000 punti; almeno 2 punti validi, segmenti preservati.
- Quota e timestamp assenti espliciti (`null` + flag `hasElevation`/`hasTimestamps`), mai zeri sostitutivi (AD-3); assenza quota è limite informativo, non errore.
- Elaborazione fuori dal thread UI con progresso visibile, blocco duplicati e annuncio TalkBack (`LiveRegionMode.Polite`).
- Zero log di GPX, coordinate, nomi percorso, URI o identità (`ImportErrorCode` opachi); zero permessi di posizione, rete, archiviazione o notifiche.

**Never:**
- Non implementare anteprima tracciato, canvas, rinomina o eliminazione (Story 1.3), né partenza/passo/DST/ETA (Story 1.4–1.5).
- Non usare timestamp storici del GPX come `departureInstantUtc` (resta `null`).
- Non aggiungere chiamate di rete né termini «sicur*»/«safe» in `strings.xml`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| GPX 1.0/1.1 valido (`trk`/`rte`) | SAF, `ACTION_VIEW` o `ACTION_SEND`, ≥ 2 punti, segmenti multipli | `source_v1.gpx` + `geometry_v1.bin` salvati; 1 riga Room; Home aggiornata | N/A |
| Quota o timestamp assenti | GPX valido senza `<ele>` e/o `<time>` | Import riuscito; `elevationGainMeters = null`, flag espliciti; avviso limite | Nessun errore |
| XML malformato o XXE/DTD | Tag rotti, `<!DOCTYPE`, `<!ENTITY` | Rifiuto immediato locale, zero rete, dati esistenti intatti | `UnsafeXml` / `MalformedXml` |
| Coordinate / punti invalidi | `lat`/`lon` fuori range, `NaN`, `< 2` punti | Import rifiutato, dati esistenti intatti | `InvalidCoordinates` / `InsufficientPoints` |
| Limiti 20 MB / 200.000 punti | Esattamente al limite vs +1 byte / +1 punto | Al limite accettato; oltre interrotto in streaming | `FileTooLarge` / `TooManyPoints` |
| Errore I/O o DB | URI illeggibile o errore scrittura | Rollback file parziali; percorsi salvati intatti | `ReadFailed` / `StorageFailed` |
| Import in corso / click ripetuti | Tap durante elaborazione | `InProgress` annunciato da TalkBack; click soppressi | Nessun duplicato |
| Diagnostica ed errori | Qualsiasi `GpxImportError` | Solo `ImportErrorCode` opachi, zero dati sensibili | Verificato da test |

</frozen-after-approval>

## Code Map

- `build.gradle.kts`, `app/build.gradle.kts` -- Aggiungere KSP `2.2.20-2.0.4` e Room `2.8.5` (`room-runtime`, `room-ktx`, `ksp(room-compiler)`).
- `app/src/main/AndroidManifest.xml` -- Aggiungere `intent-filter` `ACTION_VIEW` e `ACTION_SEND` su `MainActivity` senza permessi.
- `app/src/main/java/it/trekcheck/app/domain/model/SavedPlanSummary.kt` & `LocalPlanRepository.kt` -- Estendere metadati opzionali (`hasElevation`, `hasTimestamps`) e implementare `LocalPlanRepository` via Room mantenendo `InMemoryLocalPlanRepository` per test.
- `app/src/main/java/it/trekcheck/app/ui/home/HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt` -- Sostituire `ImportComingSoon` con flusso importazione, progresso TalkBack, avvisi di successo/limite ed errori recuperabili («Scegli un altro file»).
- `app/src/main/java/it/trekcheck/app/MainActivity.kt` -- Registrare `OpenDocument`, gestire `ACTION_VIEW`/`ACTION_SEND` (`onCreate` + `onNewIntent`), collegare Room, file storage e `ImportGpxUseCase`.
- `app/src/test/java/it/trekcheck/app/ManifestAndBackupConfigTest.kt` -- Verificare intent-filter GPX, zero permessi, backup rules e divieto «sicur*»/«safe».

## Tasks & Acceptance

**Execution:**
- [x] `build.gradle.kts`, `app/build.gradle.kts` -- Configurare KSP `2.2.20-2.0.4` e Room `2.8.5` -- Abilita persistenza metadati Room (AD-2).
- [x] `app/src/main/java/it/trekcheck/app/domain/model/GpxModels.kt`, `ImportGpxResult.kt`, `domain/repository/GpxImportPorts.kt` -- Definire modelli geometria/segmenti, limiti (20 MB / 200k punti), errori tipizzati opachi e porte `GpxParser`, `RouteFileStorage`, `RouteImportRepository` -- Confini AD-1/AD-2/AD-3.
- [x] `app/src/main/java/it/trekcheck/app/data/gpx/SafeXmlGpxParser.kt` -- Parser XML streaming sicuro (rifiuto DTD/entità + feature XXE), GPX 1.0/1.1 `trk`/`rte`, limiti streaming, distanza Haversine per segmento e dislivello positivo opzionale -- FR-1, AD-2, NFR-2, NFR-5.
- [x] `app/src/main/java/it/trekcheck/app/data/local/files/VersionedRouteFileStorage.kt` -- Salvataggio atomico `source_v1.gpx` e `geometry_v1.bin` in `filesDir/routes/<id>/` con rollback su errore -- AD-2.
- [x] `app/src/main/java/it/trekcheck/app/data/local/db/RouteMetadataEntity.kt`, `RouteMetadataDao.kt`, `TrekCheckDatabase.kt`, `RoomLocalPlanRepository.kt` -- Entità Room soli metadati/riferimenti (zero `TrackPoint`), DAO `Flow` e repository con cleanup file su errore DB -- AD-2, FR-14.
- [x] `app/src/main/java/it/trekcheck/app/domain/usecase/ImportGpxUseCase.kt` -- Orchestrare validazione, parsing, storage ibrido e mappatura errori opachi senza log sensibili -- AD-1, NFR-1.
- [x] `app/src/main/java/it/trekcheck/app/data/gpx/ContentResolverGpxSource.kt`, `AndroidManifest.xml`, `MainActivity.kt` -- Intent-filter `ACTION_VIEW`/`ACTION_SEND`, selettore SAF `OpenDocument` e avvio importazione su `Dispatchers.IO` -- FR-1, NFR-1.
- [x] `app/src/main/java/it/trekcheck/app/ui/home/HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `res/values/strings.xml` -- Progresso off-UI-thread con TalkBack, avvisi successo/limite ed errori recuperabili -- UX-DR3, UX-DR11, FR-20.
- [x] `app/src/test/java/it/trekcheck/app/` -- Test unitari e Robolectric: GPX 1.0/1.1, XXE/DTD/malformato/coordinate, boundary 20 MB e 200k punti, misura fixture 50.000 punti (NFR-5), storage binario + rollback, Room DB, privacy errori, ViewModel/UI/MainActivity/Manifest -- Copertura completa I/O e AC.

**Acceptance Criteria:**
- Given GPX 1.0/1.1 (`trk`/`rte`) con ≥ 2 punti validi, when importo da SAF, `ACTION_VIEW` o `ACTION_SEND`, then segmenti e dati validi sono salvati in file privati versionati e soli metadati/riferimenti persistono in Room senza un'entità per punto.
- Given XML malformato, DTD/entità esterne, coordinate invalide o file oltre 20 MB / 200.000 punti, when importo, then la lettura è rifiutata localmente con errore recuperabile senza accessi esterni né alterazione dei dati esistenti.
- Given quota o timestamp assenti, when importo, then il percorso valido è salvato e la mancanza è segnalata esplicitamente come limite.
- Given un import su file grande (fixture 50.000 punti), when in corso, then gira off-UI-thread con progresso annunciato da TalkBack, blocca duplicati e registra il tempo misurato rispetto a NFR-5 (< 2 s).
- Given errori o diagnostica dell'import, when prodotti, then escludono GPX, coordinate, nomi percorso e identità; il manifest resta privo di permessi di posizione, rete o archiviazione.

## Implementation Notes

- **NFR-5 (fixture 50.000 punti):** misurata in ambiente JVM (`--rerun-tasks`: `SafeXmlGpxParserTest` parse `1.064,74 ms` su `4.637.610` byte [`492,25 ms` a caldo]; `VersionedRouteFileStorageTest` write `98,40 ms` / read `88,14 ms` su `1.250.016` byte `geometry_v1.bin`; `ImportGpxUseCaseTest` pipeline completa `893,47 ms` [`263,61 ms` a caldo]). La misura su dispositivo fisico/emulatore di riferimento (< 2 s) resta **Non misurata su device** per assenza di hardware Android in VM headless.
- **Verifica CI locale (post-patch):** `./gradlew --no-daemon --rerun-tasks testDebugUnitTest lintDebug jacocoTestDebugUnitTestReport jacocoTestDebugUnitTestCoverageVerification assembleDebug` — `BUILD SUCCESSFUL`: `82` test su `17` suite (`0` fallimenti/errori/skip), `lintDebug` (`0` warning/errori con `warningsAsErrors = true`), JaCoCo linee `1.067/2.366` (`45,10%`, soglia `5%`) / branch `46,14%`, `assembleDebug` OK.
- **Scelte e deviazioni:** aggiunto `kotlinx-coroutines-test:1.11.0`, aggiornato `robolectric:4.18.1` e impostato `android:launchMode="singleTop"` su `MainActivity` per ricevere `ACTION_VIEW`/`ACTION_SEND` tramite `onNewIntent`.

## Spec Change Log

## Review Triage Log

| ID | Fonte | Posizione | Verdetto | Evidenza e decisione |
|----|-------|-----------|----------|----------------------|
| BH-1 | `blind-hunter` | `SafeXmlGpxParser.kt:141-154,230-241` | `medium` → `patch` | `isDoctypeFeatureRejection` verifica `msg.contains("entity")`, classificando entità non dichiarate (es. `&foo;`) come `UnsafeXml` invece di `MalformedXml`; restringere ai messaggi DTD/entità esterne e mantenere il pre-scan stream conservativo previsto in `Design Notes`. |
| BH-2 | `blind-hunter` | `SafeXmlGpxParser.kt:484-523` | `medium` → `patch` | `hasElevation` usa `validElevationPairs > 0` scartando quote su segmenti a 1 punto, e delta estremi (`1e308` vs `-1e308`) vanno in overflow a `+Infinity` causando `IAE` -> `StorageFailed`; impostare `hasElevation` da qualsiasi `<ele>` valido, dislivelli opzionali su coppie valide e bloccare overflow non finiti come `InvalidCoordinates`. |
| BH-3 | `blind-hunter` | `SafeXmlGpxParser.kt:291-388,530-538` | `medium` → `patch` | Tag figli in namespace esteri, URL arbitrari con sottostringa `"topografix.com/gpx"`, `<trkseg>` sotto `<trk>` fuori `<gpx>` e `<time>` non ISO-8601 vengono accettati; validare namespace GPX esatti, gerarchia `<gpx>/<trk>/<trkseg>` e sintassi temporale ISO-8601. |
| BH-4 | `blind-hunter` | `MainActivity.kt:55-57,86-96` | `high` → `patch` | `handleIncomingIntent` non esclude `Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`, rieseguendo `ACTION_VIEW`/`ACTION_SEND` alla riapertura da Recents e duplicando il percorso; ignorare intent con quel flag. |
| BH-5 | `blind-hunter` | `MainActivity.kt:63,106-110`, `HomeViewModel.kt:133-140` | `medium` → `patch` | `database.close()` sincrono può gareggiare con `Dispatchers.IO` su `onCleared()`, manca `try/catch` su `ActivityNotFoundException` del picker e doppi tap accodano effetti `LaunchGpxPicker`; chiudere il DB al completamento del job dello scope, gestire `ActivityNotFoundException` e deduplicare l'apertura picker in `MainActivity`. |
| BH-6 | `blind-hunter` | `HomeScreen.kt:76-99`, `MainActivity.kt:45-74` | `medium` → `patch` | Quando la lista è scrollata in basso la comparsa di `HomeNoticeCard` all'indice 1 resta fuori viewport e l'annuncio `LiveRegionMode.Polite` di TalkBack può perdersi (`patch` con `scrollToItem` su nuovo `activeNotice`); la perdita del banner transitorio dopo process-kill è `low` (rigettato). |
| BH-7 | `blind-hunter` | `AndroidManifest.xml:29-58` | `medium` → `patch` | I filtri `ACTION_VIEW` omettono `text/plain`, `.Gpx` e pattern multi-punto oltre 2 punti (mentre URI `content://` opachi senza `.gpx` restano coperti da MIME GPX dedicati, `ACTION_SEND` e picker SAF); aggiungere `text/plain`, `.Gpx` e `pathPattern` fino a 4 punti. |
| BH-8 | `blind-hunter` | `VersionedRouteFileStorage.kt:29-36,102-146` | `high` → `patch` | `readGeometry` alloca `ArrayList(segmentCount)` prima del bound causando OOM, somma `pointCount` con rischio overflow `Int`, ignora byte residui e lascia `IllegalArgumentException` invece di `IOException`; validare limiti/overflow, wrap `IOException` e EOF (`read() == -1`). |
| BH-9 | `blind-hunter` | `ImportGpxUseCase.kt:69-159`, `RoomLocalPlanRepository.kt:24-35` | `medium` → `patch` | `teeStream.use` può trasformare `IOException` su `rawStream.close()` in `StorageFailed`, e `saveImportedRoute` su `CancellationException` elimina i file anche se l'insert Room ha già committato; separare chiusura stream e cancellare anche la riga DB su rollback di cancellazione. |
| BH-10 | `blind-hunter` | `RouteMetadataEntity.kt:10-65`, `RoomLocalPlanRepository.kt:46-82` | `low` (rigettato) | `sourceKind` non è persistito perché non richiesto in Story 1.2 né usato da caller (`low`, modifica schema DB non necessaria), e `toSavedPlanSummary()` riceve solo righe già validate da `SavedPlanSummary` in ingresso (`false`). |
| BH-11 | `blind-hunter` | `HomeScreen.kt:282-367`, `HomeViewModel.kt:71-76` | `low` → `patch` | Condividere `ic_info_notice` tra warning ed errori riduce la distinzione non cromatica (`patch`: aggiungere `ic_error_notice.xml` dedicato); la soppressione di `importGpxSource` mentre `InProgress` è invece intenzionale per spec e test (`false`). |
| BH-12 | `blind-hunter` | `app/src/test/java/it/trekcheck/app/` | `medium` → `patch` | Mancano test su `GpxSourceKind.MIXED`, `<metadata><name>`, `&foo;`, `geometry_v1.bin` corrotto, cancellazione/staging failure, DB su disco e edge case `MainActivity`; coprire insieme a `VG-1..5`. |
| EC-1 | `edge-case-hunter` | `VersionedRouteFileStorage.kt:117-134` | `high` → `patch` | Stessa causa di `BH-8`: `segmentCount` non limitato prima di `ArrayList` e `totalPoints += pointCount` esposto a overflow `Int`. |
| EC-2 | `edge-case-hunter` | `VersionedRouteFileStorage.kt:30-33,50-53` | `low` → `patch` | `!stagingDir.exists() && !stagingDir.mkdirs()` può fallire in race tra due chiamate concorrenti; sostituire con `!stagingDir.mkdirs() && !stagingDir.isDirectory`. |
| EC-3 | `edge-case-hunter` | `SafeXmlGpxParser.kt:497` | `medium` → `patch` | Stessa causa di `BH-2`: segmenti a punto singolo con `<ele>` scartati da `hasElevation`. |
| EC-4 | `edge-case-hunter` | `SafeXmlGpxParser.kt:90-92` | `medium` → `patch` | `catch (throwable: Throwable)` cattura `Error` JVM e `CancellationException` mappandoli a `MalformedXml`; rilanciare prima di `mapParseFailure`. |
| EC-5 | `edge-case-hunter` | `SafeXmlGpxParser.kt:135` | `low` → `patch` | `CharacterCodingException` e `UTFDataFormatException` (sottoclassi `IOException` ma non `CharConversionException`) vengono mappati a `ReadFailed` invece di `MalformedXml`; escluderli dal ramo `ReadFailed`. |
| EC-6 | `edge-case-hunter` | `SafeXmlGpxParser.kt:146-149` | `medium` → `patch` | Stessa causa di `BH-1`: `msg.contains("entity")` classifica errori XML su entità non dichiarate come `UnsafeXml`. |
| EC-7 | `edge-case-hunter` | `SafeXmlGpxParser.kt:191-211` | `medium` → `patch` | `SecurityException` o `RuntimeException` dallo stream `ContentResolver` durante `read()` non imposta `underlyingReadFailed` e diventa `MalformedXml` invece di `ReadFailed`; catturare `RuntimeException` sullo stream delegato. |
| EC-8 | `edge-case-hunter` | `SafeXmlGpxParser.kt:309-355` | `medium` → `patch` | Stessa causa di `BH-3`: tag figli in namespace esteri accettati dentro elementi GPX. |
| EC-9 | `edge-case-hunter` | `SafeXmlGpxParser.kt:484-491` | `medium` → `patch` | Stessa causa di `BH-2`: overflow `Double` su `<ele>` estremi provoca `IAE` in `ParsedGpxRoute`. |
| EC-10 | `edge-case-hunter` | `ImportGpxUseCase.kt:270-274` | `low` → `patch` | `substring(0, MAX_TITLE_LENGTH)` può spezzare una coppia surrogata UTF-16 all'indice 159; arretrare di 1 se `Character.isHighSurrogate`. |
| EC-11 | `edge-case-hunter` | `MainActivity.kt:86-96` | `high` → `patch` | Stessa causa di `BH-4`: `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` non escluso in `handleIncomingIntent`. |
| EC-12 | `edge-case-hunter` | `MainActivity.kt:63` | `medium` → `patch` | Stessa causa di `BH-5`: `ActivityNotFoundException` non gestita su `openGpxDocumentLauncher.launch`. |
| EC-13 | `edge-case-hunter` | `MainActivity.kt:174-181` | `medium` → `patch` | Su API < 33 `getParcelableExtra<Uri>(Intent.EXTRA_STREAM)` genera `CHECKCAST` non sicuro verso `Uri`; usare `getParcelableExtra<Parcelable>(...) as? Uri`. |
| EC-14 | `edge-case-hunter` | `HomeViewModel.kt:71-76` | `false` (rigettato) | La soppressione di chiamate `importGpxSource` mentre `InProgress` è comportamento esplicito della matrice I/O e verificato da `HomeViewModelTest:205-207`. |
| EC-15 | `edge-case-hunter` | `HomeViewModel.kt:133-140`, `MainActivity.kt:63` | `medium` → `patch` | Stessa causa di `BH-5`: doppio tap rapido su `Importa GPX` può emettere due lanci picker sovrapposti; bloccare in `MainActivity` fino al callback SAF. |
| EC-16 | `edge-case-hunter` | `GpxModels.kt:128-143` | `low` → `patch` | `ParsedGpxRoute.init` non impone che `hasElevation` coincida con presenza/assenza di `minElevationMeters`/`maxElevationMeters`; allineare il `require`. |
| VG-1 | `verification-gap` | `MainActivity.kt:37-43`, `MainActivityTest.kt:26-38` | `high` → `patch` | Evidenza pre-verificata: `MainActivityTest` verifica solo l'intent `ACTION_OPEN_DOCUMENT` in uscita senza consegnare `RESULT_OK`/`RESULT_CANCELED` al launcher SAF; aggiungere test con `shadowOf(activity).receiveResult`. |
| VG-2 | `verification-gap` | `TrekCheckDatabase.kt:19-24`, `RoomLocalPlanRepositoryTest.kt:48` | `high` → `patch` | Evidenza pre-verificata: i test usano solo `inMemoryDatabaseBuilder` o `recreate()` (che mantiene il `ViewModelStore`), senza verificare chiusura/riapertura DB su disco tramite `TrekCheckDatabase.create`; aggiungere round-trip su disco. |
| VG-3 | `verification-gap` | `HomeScreen.kt:283-328`, `HomeScreenTest.kt:66-95` | `medium` → `patch` | Evidenza pre-verificata: in `HomeScreenTest` mancano le asserzioni Compose UI per `ImportSucceeded(true, true)` e `ImportSucceeded(false, false)`; aggiungerle. |
| VG-4 | `verification-gap` | `AndroidManifest.xml:19-68`, `MainActivity.kt:86-96,158-181` | `high` → `patch` | Evidenza pre-verificata: mancano verifiche `PackageManager.queryIntentActivities` sugli intent-filter impliciti, test `ACTION_SEND` su SDK 26 (< 33), `clipData` fallback e URI assente (`READ_FAILED`); aggiungere test in `MainActivityIntentImportTest`. |
| VG-5 | `verification-gap` | `ImportGpxUseCase.kt:63-89`, `ImportGpxUseCaseTest.kt:91-133` | `medium` → `patch` | Evidenza pre-verificata: nessun test copre `stagingWriteFailed -> StorageFailed` o stream > 20 MB con `knownByteSize == null`; aggiungerli in `ImportGpxUseCaseTest`. |
| VG-6 | `verification-gap` | `ImportGpxUseCase.kt:100-104`, `VersionedRouteFileStorage.kt:55-58` | `high` → `patch` | Nel `catch` di `commitStagedRoute`, `ImportGpxUseCase` invoca `cleanupRouteSafely(routeId)` annullando la guardia di `VersionedRouteFileStorage` su `routeDir.exists()` e cancellando i file di un percorso già esistente in caso di collisione ID; rimuovere la chiamata dal `catch` di `commitStagedRoute`. |

## Design Notes

- **Parser XML sicuro multipiattaforma:** doppia difesa compatibile JVM e Android API 26–37: (1) stream wrapper a conteggio byte (20 MB) che rigetta immediatamente `<!DOCTYPE` e `<!ENTITY`; (2) `SAXParserFactory` con feature XXE disabilitate ove supportate + `EntityResolver` che lancia eccezione su ogni risoluzione esterna.
- **Formato `geometry_v1.bin`:** header `"TCGM"` + `version = 1` + `segmentCount` + per segmento `pointCount` e tuple `(lat, lon, hasEle, [ele])`; ~1,25 MB per 50.000 punti, I/O in pochi ms.

## Verification

**Commands:**
- `./gradlew --no-daemon testDebugUnitTest lintDebug jacocoTestDebugUnitTestReport jacocoTestDebugUnitTestCoverageVerification assembleDebug` -- expected: BUILD SUCCESSFUL, 0 warning Lint, test verdi inclusa misura fixture 50k punti, coverage linee ≥ 5%, APK debug generato.

**Manual checks (if no CLI):**
- Su dispositivo/emulatore API 26+: verifica selettore SAF, «Apri con», «Condividi», annuncio TalkBack del progresso e misura NFR-5 su device di riferimento.
