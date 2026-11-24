---
title: 'Story 1.1: Avvio e accesso locale'
type: 'feature'
created: '2026-11-24'
status: 'done'
route: 'dispatch'
baseline_commit: '9b5fd5169229246995a13285c0d646cab654cf7c'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'app attuale mostra solo un segnaposto statico (`MainActivity.kt`) con `android:allowBackup="true"`, tema Material default e nessun confine architetturale o schermata Home; non rispetta i requisiti di accesso locale senza account/permessi, i token visuali di `DESIGN.md` né le regole di esclusione backup di AD-6.

**Approach:** Costruire la fondazione local-first di TrekCheck nel modulo `:app`: tema Material 3 (chiaro e scuro a contrasto verificato) con token `DESIGN.md`, componente `TrekCheckPrimaryAction` accessibile (≥ 48×48 dp, stati abilitato/disabilitato/progresso), schermata Home con stato vuoto e lista locale consultabile offline collegata a `HomeViewModel` e porta `LocalPlanRepository`, e configurazione manifest/backup (`allowBackup="false"`, `backup_rules.xml`, `data_extraction_rules.xml`) verificata da test unitari.

## Boundaries & Constraints

**Always:**
- Mantenere il singolo modulo Gradle `:app` e i confini logici porte/adattatori (AD-1): dominio puro Kotlin senza dipendenze Android, UI o DTO esterni.
- Usare unità canoniche SI (`Double` metri) e `Instant` UTC nel dominio, con nullabilità esplicita per dati assenti (AD-3).
- Applicare i token di `DESIGN.md` tramite ruoli Material 3 (tema chiaro e tema scuro con contrasto verificato ≥ 4,5:1 testo normale e ≥ 3:1 testo grande; nessun significato affidato al solo colore).
- Garantire target interattivi ≥ 48×48 dp, layout scorrevole adattabile a testo ingrandito/landscape e semantica TalkBack.
- Dichiarare `android:allowBackup="false"`, `backup_rules.xml` e `data_extraction_rules.xml` escludendo file privati, database e DataStore; non richiedere alcun permesso di localizzazione, notifiche o rete.
- Registrare lo stato della misura avvio a freddo (NFR-5 < 2 s) senza dichiararla raggiunta in assenza di misura su dispositivo fisico/emulatore di riferimento.

**Never:**
- Non implementare il selettore/parser GPX o la persistenza Room (appartengono a Story 1.2).
- Non aggiungere moduli Gradle, SDK di rete/mappe o dipendenze incompatibili con `minSdk = 26`.
- Non introdurre login, account, telemetria o permessi di posizione/notifiche.
- Non usare inversioni automatiche per il tema scuro né termini come «sicuro» nella copy.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Prima apertura (vuota) | `savedPlans = emptyList()`, offline o online | Home mostra titolo, spiegazione breve senza login/GPS e pulsante primario «Importa GPX» abilitato | N/A |
| Home popolata offline | `savedPlans` non vuota, rete assente | Elenco percorsi locali consultabile con stato offline esplicito + pulsante «Importa GPX» | Nessuna perdita dati |
| Tap su «Importa GPX» in Story 1.1 | `importActionState = Enabled` | Avviso recuperabile locale che informa che l'importazione GPX è in arrivo nella story successiva (nessun crash, nessun permesso) | Messaggio dismissibile |
| Azione primaria in progresso / disabilitata | `InProgress` o `Disabled` | Stato visivo + semantico distinto; click ignorato (nessun duplicato) | Click soppresso |
| Configurazione Manifest e Backup | `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml` | `allowBackup="false"`, esclusione `file`/`database`/`sharedpref` in cloud-backup e device-transfer, zero permessi posizione/notifiche | Test unitario fallisce se violato |
| Contrasto colori temi | `TrekCheckLightColorScheme`, `TrekCheckDarkColorScheme`, `warning` | Rapporto di contrasto WCAG ≥ 4,5:1 per coppie testo/superficie | Test unitario fallisce se sotto soglia |

</frozen-after-approval>

## Code Map

- `app/build.gradle.kts` -- Configurazione modulo `:app` (`minSdk = 26`, `compileSdk/targetSdk = 37`, Compose BOM `2025.10.00`, Lint `warningsAsErrors = true`, JaCoCo ≥ 5%). Non modificare toolchain né aggiungere nuovi moduli.
- `app/src/main/AndroidManifest.xml` -- Manifest corrente con `allowBackup="true"` (riga 3): aggiornare a `false` e collegare `fullBackupContent` e `dataExtractionRules`.
- `app/src/main/java/it/trekcheck/app/MainActivity.kt` -- Entry point attuale con segnaposto statico (`welcomeMessage()`, `appTitle()`, `TrekCheckApp()`): sostituire con `TrekCheckTheme` e `HomeScreen` collegato a `HomeViewModel`.
- `app/src/test/java/it/trekcheck/app/WelcomeMessageTest.kt` -- Test segnaposto esistente: sostituire o aggiornare con i nuovi test di dominio, ViewModel, contrasto tema e configurazione manifest/backup.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/res/xml/backup_rules.xml` & `app/src/main/res/xml/data_extraction_rules.xml` -- Creare regole XML che escludono `file`, `database` e `sharedpref` da backup cloud e trasferimento dispositivo -- Rispetta AD-6 e AC 5 su API 26–30 e API 31+.
- [x] `app/src/main/AndroidManifest.xml` -- Impostare `android:allowBackup="false"`, `android:fullBackupContent="@xml/backup_rules"`, `android:dataExtractionRules="@xml/data_extraction_rules"` -- Chiude il rischio di sincronizzazione incoerente di GPX/DB/quota.
- [x] `app/src/main/java/it/trekcheck/app/domain/model/SavedPlanSummary.kt` & `app/src/main/java/it/trekcheck/app/domain/repository/LocalPlanRepository.kt` -- Definire modello di dominio immutabile (unità SI, `Instant`, nullabilità esplicita) e porta repository local-first -- Stabilisce confine AD-1/AD-3 per Home ed Epic 1.
- [x] `app/src/main/java/it/trekcheck/app/data/local/InMemoryLocalPlanRepository.kt` -- Implementare adattatore locale predefinito (vuoto iniziale, offline-safe, pronto per sostituzione con Room/file in Story 1.2) -- Consente consultazione locale senza rete né dipendenze esterne.
- [x] `app/src/main/java/it/trekcheck/app/ui/theme/TrekCheckTheme.kt` -- Implementare `TrekCheckTheme`, schemi chiaro (token esatti `DESIGN.md`) e scuro (contrasto verificato), `TrekCheckExtendedColors` (`warning`/`onWarning`), `TrekCheckShapes` (`4/12/20 dp`), `TrekCheckSpacing` (`4/8/12/16/24 dp`) e tipografia di sistema M3 -- Soddisfa UX-DR1 e AC 4.
- [x] `app/src/main/java/it/trekcheck/app/ui/components/TrekCheckPrimaryAction.kt` -- Implementare pulsante primario M3 con `PrimaryActionState` (`Enabled`, `Disabled`, `InProgress`), target minimo `48.dp × 48.dp`, `RoundedCornerShape(12.dp)`, blocco duplicati e semantica TalkBack -- Soddisfa UX-DR2 e AC 4.
- [x] `app/src/main/java/it/trekcheck/app/ui/home/HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt` -- Implementare stato immutabile, gestione eventi, stato vuoto, lista percorsi locali offline, layout scorrevole `widthIn(max = 600.dp)` con margine `16.dp` e semantica accessibile -- Soddisfa UX-DR3, UX-DR11, AC 1 e AC 3.
- [x] `app/src/main/java/it/trekcheck/app/MainActivity.kt` -- Collegare `TrekCheckApp` a `TrekCheckTheme`, `HomeViewModel` e `HomeScreen` (senza login né permessi) -- Attiva il nuovo flusso di avvio locale.
- [x] `app/src/test/java/it/trekcheck/app/` -- Aggiungere test unitari per `HomeViewModel` (vuoto, popolato offline, stati azione primaria, blocco duplicati), `TrekCheckThemeContrastTest` (WCAG ≥ 4,5:1 chiaro/scuro), `ManifestAndBackupConfigTest` (verifica XML manifest e regole backup, assenza permessi GPS/notifiche) -- Copre tutti i casi della matrice I/O e garantisce il gate JaCoCo/CI.

**Acceptance Criteria:**
- Given una nuova installazione, when apro Home, then vedo stato vuoto e azione «Importa GPX» senza login né richiesta di posizione.
- Given Android 8+ (API 26), when compilo e verifico il progetto con Gradle Wrapper e CI, then `minSdk = 26`, Lint (warningsAsErrors), unit test, coverage JaCoCo e `assembleDebug` passano senza errori.
- Given un dispositivo senza rete con dati locali presenti, when apro Home, then i percorsi salvati restano consultabili e viene indicato lo stato locale/offline.
- Given tema chiaro o scuro, when visualizzo Home, then i ruoli Material 3 applicano i token di `DESIGN.md` con contrasto verificato e l'azione primaria distingue `Enabled`/`Disabled`/`InProgress` con target ≥ 48×48 dp.
- Given manifest e risorse XML, when eseguo i test unitari, then `android:allowBackup="false"` e le esclusioni in `backup_rules.xml` / `data_extraction_rules.xml` sono verificate.
- Given l'assenza di un dispositivo fisico/emulatore di riferimento in ambiente VM, when chiudo la story, then registro lo stato di NFR-5 avvio a freddo come «Non misurato» insieme al comando `adb shell am start -W` per la verifica su device.

## Implementation Notes

- **NFR-5 (avvio a freddo < 2 s):** **Non misurato** in ambiente VM headless (assenza di dispositivo fisico/emulatore di riferimento). Comando per verifica su dispositivo: `adb shell am start -W -n it.trekcheck.app/.MainActivity` (registrare `TotalTime`).
- **Verifica UI/accessibilità:** `TrekCheckThemeContrastTest` verifica matematicamente i contrasti WCAG (≥ 4,5:1), mentre i test Robolectric (`TrekCheckPrimaryActionTest`, `HomeScreenTest`, `TrekCheckThemeTest`, `MainActivityTest`) verificano rendering, target `>= 48.dp`, stati `Enabled`/`Disabled`/`InProgress`, focusability e ripristino su configurazione in JVM; restano da verificare su dispositivo reale TalkBack, ingrandimento testo di sistema e rendering portrait/landscape.
- **Verifica Gradle nella VM:** tutti i task CI (`testDebugUnitTest`, `lintDebug`, `jacocoTestDebugUnitTestReport`, `jacocoTestDebugUnitTestCoverageVerification`, `assembleDebug`) superati (`19` test iniziali, poi `32` test su `10` suite inclusi Compose UI/Robolectric `@Config(sdk = [35])` dopo i patch di review, `0` errori/warning Lint, coverage linee `33,33%` — `246/738` — vs soglia `5%`). In questa VM `repo.maven.apache.org` restituiva HTTP 429, quindi la build locale ha usato uno script `--init-script` esterno al repository puntato al mirror ufficiale Google (`maven-central.storage-download.googleapis.com`) senza modificare `settings.gradle.kts`.

## Spec Change Log

## Review Triage Log

| ID | Fonte | Posizione | Verdetto | Evidenza e decisione |
|----|-------|-----------|----------|----------------------|
| EC-1 | `edge-case-hunter` | `HomeScreen.kt:107-112` | `low` → `patch` | `key = { plan -> plan.id }` in `LazyColumn` può collidere con chiavi statiche (`"header"`, `"notice"`, ecc.) o con eventuali ID duplicati causando `IllegalArgumentException`; prefissare `"plan_${index}_${plan.id}"` con `itemsIndexed`. |
| EC-2 | `edge-case-hunter` | `HomeViewModel.kt:38-46` | `false` (rigettato) | L'unico adattatore esistente è `InMemoryLocalPlanRepository` basato su `MutableStateFlow`, che non emette eccezioni; inghiottire errori con `catch { emit(emptyList()) }` maschererebbe guasti reali nelle story successive su stato non raggiungibile qui. |
| EC-3 | `edge-case-hunter` | `HomeFormatters.kt:26-36` | `low` → `patch` | `(durationSeconds / 60L).toInt()` rischia overflow su `Long` grandi e tronca `1..59s` a `0 min`; convertire interamente in aritmetica `Long` e gestire negativi/secondi residui in modo coerente. |
| EC-4 | `edge-case-hunter` | `HomeFormatters.kt:11-24` | `false` (rigettato) | L'unico chiamante in produzione è `SavedPlanCard` su `SavedPlanSummary`, il cui `init` impone già `isFinite()` e `>= 0.0` su `distanceMeters` e `elevationGainMeters`; valori `NaN`/negativi non sono raggiungibili. |
| EC-5 | `edge-case-hunter` | `HomeFormatters.kt:12-17` | `low` → `patch` | `distanceMeters in [999.5, 1000.0)` entra nel ramo `< 1000.0` e viene arrotondato a `"1000 m"` invece di `"1,0 km"`; confrontare `distanceMeters.roundToInt() < 1000`. |
| BH-1 | `blind-hunter` | `MainActivity.kt:18-63` | `low` → `patch` | In Story 1.1 l'unico stato mutabile dall'utente è `activeNotice` (già salvato/ripristinato via `KEY_IMPORT_NOTICE_VISIBLE`) e `HomeViewModel` è intenzionalmente puro Kotlin senza `androidx.lifecycle.ViewModel`; manca però la guardia `if (::homeViewModel.isInitialized)` in `onSaveInstanceState`. |
| BH-2 | `blind-hunter` | `HomeScreen.kt:354-357`, `HomeFormatters.kt`, `strings.xml` | `medium` → `patch` | Il cambio lingua IT/EN in-app (FR-19) appartiene a Story 1.6 con copy EN verificata, ma `HomeScreen.kt` hardcoda `"Partenza:"`/`"Salvato:"` in Kotlin e `HomeFormatters` hardcoda `Locale.ITALIAN` e `"stimato"`/`"stimati"` invece di usare `strings.xml` e il locale corrente. |
| BH-3 | `blind-hunter` | `HomeFormatters.kt:10-47` | `low` → `patch` | Raggruppato con EC-3 ed EC-5 per `999.6 m` → `"1000 m"` e troncamento/overflow in `formatEstimatedDuration`; l'esposizione di `zoneId.id` è intenzionale (verificata da test) e i valori negativi/`NaN` sono bloccati da `SavedPlanSummary`. |
| BH-4 | `blind-hunter` | `TrekCheckPrimaryAction.kt:30-129` | `high` → `patch` | Passare `enabled = isEnabled` (`false`) a Material 3 `Button` in stato `InProgress` applica semantica `Disabled` e rimuove il focus tastiera/Switch Access (violando UX-DR2/UX-DR11); mantenere il nodo non-disabled in `InProgress` sopprimendo `onClick`, fornire default per `stateDescription` e usare `MaterialTheme.shapes.medium`. |
| BH-5 | `blind-hunter` | `HomeScreen.kt:46-456` | `medium` → `patch` | `SavedPlanCard` omette silenziosamente `estimatedDurationSeconds == null` violando la regola di nullabilità esplicita; inoltre `updatedText` manca di allineamento verticale in `FlowRow`, i middot `"· "` prefissi vanno a capo in testa riga, `Chiudi` manca di `contentDescription` contestuale e il badge usa `ic_offline_local` anche per `SAVED_BRIEFING` / online. |
| BH-6 | `blind-hunter` | `TrekCheckTheme.kt:24-200`, `TrekCheckPrimaryAction.kt:65`, `TrekCheckThemeContrastTest.kt` | `medium` → `patch` | Gli schemi chiaro/scuro omettono `inverseSurface`/`inverseOnSurface`/`inversePrimary`/`surfaceDim`/`surfaceBright`/`surfaceTint`/`scrim` (esponendo token viola default); il bordo `Disabled` su `outlineVariant` ha contrasto ~1,51:1 su `surface` (passare a `outline`), e mancano test per `errorContainer / onErrorContainer`. |
| BH-7 | `blind-hunter` | `HomeViewModel.kt:16-77`, `LocalPlanRepository.kt` | `low` (rigettato) | I test hook (`ignoredImportClicksCount`, `onImportDispatched`) non impattano gli utenti e verranno sostituiti in Story 1.2 con il selettore reale; `.catch` non è dimostrabile su `InMemoryLocalPlanRepository` e l'ordinamento lista è fuori dal flusso vuoto di Story 1.1. |
| BH-8 | `blind-hunter` | `backup_rules.xml`, `data_extraction_rules.xml`, `ManifestAndBackupConfigTest.kt` | `low` → `patch` | I domini `file`/`database`/`sharedpref` con `allowBackup="false"` soddisfano AD-6, ma `ManifestAndBackupConfigTest` va rafforzato per verificare anche `<uses-permission-sdk-23>`, figli diretti `<exclude>`, assenza di `<include>` e scansione di `"sicur"`/`"safe"` su tutte le risorse stringa. |
| BH-9 | `blind-hunter` | `SavedPlanSummaryTest.kt`, `HomeFormattersTest.kt`, test repo e UI | `medium` → `patch` | Mancano boundary test (`POSITIVE_INFINITY`, `elevationGainMeters = NaN`, validi `0.0`/`0L`), test diretto di `InMemoryLocalPlanRepository` e test Compose UI (`@Preview` opzionale escluso per non diluire i test reali). |
| BH-10 | `blind-hunter` | `spec-1-1-avvio-e-accesso-locale.md`, `sprint-status.yaml` | `false` (rigettato) | Per regola dello Step 4 si rigettano i finding la cui correzione modifica la spec della build corrente; `sprint-status.yaml` viene sincronizzato a `done` nello Step 5 alla chiusura della build. |
| BH-11 | `blind-hunter` | `HomeScreen.kt:348-456` | `low` → `patch` | L'azione di apertura di `SavedPlanCard` appartiene a Story 1.3 (anteprima percorso), mentre aggiungere `semantics { heading() }` su `plan.title` migliora la navigazione per intestazioni di TalkBack. |
| VG-1 | `verification-gap` | `HomeScreen.kt:46-456`, `TrekCheckPrimaryAction.kt:30-129` | `high` → `patch` | Evidenza pre-verificata: zero test Compose UI eseguiti per verificare rendering stati vuoto/popolato, blocco click/semantica `48.dp` di `TrekCheckPrimaryAction`, fallback `null` e wiring eventi `ImportGpxClicked`/`DismissNotice`; aggiungere test Compose `ui-test-junit4` con Robolectric in `testDebugUnitTest`. |
| VG-2 | `verification-gap` | `MainActivity.kt:18-76` | `medium` → `patch` | Evidenza pre-verificata: nessun test eseguito su `MainActivity` (`onCreate`, salvataggio/ripristino `KEY_IMPORT_NOTICE_VISIBLE`, `onDestroy`, wrapper `TrekCheckTheme`); aggiungere test Robolectric in `testDebugUnitTest` (o `defer` se incompatibile con API 37). |
| VG-3 | `verification-gap` | `TrekCheckTheme.kt`, `TrekCheckThemeContrastTest.kt` | `medium` → `patch` | Evidenza pre-verificata: mancano verifiche di contrasto su `surfaceContainer / onSurfaceVariant` e `surfaceContainer / primary`, asserzioni su `TrekCheckTypography` e test composable su `TrekCheckTheme(darkTheme = true/false)`. |
| VG-4 | `verification-gap` | `HomeViewModel.kt:63-65`, `HomeViewModelTest.kt` | `medium` → `patch` | Evidenza pre-verificata: `HomeViewModel.clear()` viene invocato a fine test senza verificare con emissione successiva del repository che la raccolta Flow si arresti davvero. |
| VG-5 | `verification-gap` | `HomeFormattersTest.kt:34-51` | `medium` → `patch` | Evidenza pre-verificata: `formatsInstantInExplicitRouteZoneId` verifica solo ora e fuso ma non la parte giorno/mese né il cambio di data oltre mezzanotte UTC. |

## Design Notes

- **Contrasto tema scuro:** usare una palette dark coerente con il verde bosco (`primary = #8CD5B6`, `onPrimary = #003828`, `surface = #111513`, `onSurface = #E2E7E3`, `surfaceContainer = #1D2420`, `outline = #8B968E`, `error = #FFB4AB`, `onError = #690005`, `warning = #E5C158`, `onWarning = #3A2C00`) e verificarne i rapporti WCAG tramite formula di luminanza relativa in test unitario.
- **ViewModel leggero e testabile:** implementare `HomeViewModel` come state holder puro Kotlin basato su `StateFlow` (istanziato/ricordato nell'Activity o factory semplice) così da testarlo interamente in JUnit JVM senza dipendenze Android/Robolectric e senza aggiungere librerie superflue in questa story.

## Verification

**Commands:**
- `./gradlew --no-daemon testDebugUnitTest lintDebug jacocoTestDebugUnitTestReport jacocoTestDebugUnitTestCoverageVerification assembleDebug` -- expected: BUILD SUCCESSFUL, 0 warning Lint, test verdi, coverage linee ≥ 5%, APK debug generato.

**Manual checks (if no CLI):**
- Misurazione avvio a freddo NFR-5 (< 2 s) su dispositivo fisico/emulatore di riferimento (non eseguibile in VM headless): `adb shell am start -W -n it.trekcheck.app/.MainActivity` e registrazione di `TotalTime` prima di dichiarare raggiunto l'obiettivo.
