<!-- bmad:context -->
<!-- Verified 2026-11-24 against 0686b3d. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

## TrekCheck

App Android nativa local-first per pianificare escursioni da tracce GPX con stime di percorrenza, luce e briefing meteo. Stack: Kotlin 2.2.20, Jetpack Compose (BOM 2025.10.00), Android Gradle Plugin 8.13.2, Gradle Wrapper 8.14.8, modulo singolo `:app`. Le baseline di prodotto vivono in `docs/`, mentre PRD, architettura, UX ed epic BMAD vivono in `_bmad-output/planning-artifacts/`.

## Policy

- Non lavorare e non fare merge su `main` senza richiesta esplicita dell'utente; crea un branch dedicato per ogni epica, e fai commit e push al completamento di ogni story o epica.
- Mantieni spesa rigorosamente a zero: non configurare billing, piani a pagamento o servizi privi di blocco certo prima di addebiti; usa fixture e modalità locale route-only.
- Non committare né pushare mai segreti, credenziali o token nel repository; usa variabili d'ambiente o configurazioni locali ignorate da git.
- Non abilitare chiamate reali a servizi esterni (meteo, mappe, gateway) e non accedere a documenti esterni al repository senza approvazione esplicita dell'utente.

## Where things are

- Entry point Android: `app/src/main/java/it/trekcheck/app/MainActivity.kt` (package `it.trekcheck.app`); configurazione build e coverage in `app/build.gradle.kts`.
- Prima di introdurre layer, storage, contratti meteo o logica di quota, leggi `_bmad-output/planning-artifacts/architecture/architecture-TrekCheck-2026-11-21/ARCHITECTURE-SPINE.md`.
- Per requisiti funzionali e acceptance criteria consulta `_bmad-output/planning-artifacts/prds/prd-TrekCheck-2026-11-20/prd.md` e `_bmad-output/planning-artifacts/epics.md`; per UI/Compose consulta `DESIGN.md` ed `EXPERIENCE.md` in `_bmad-output/planning-artifacts/ux-designs/ux-TrekCheck-2026-11-21/`.

## Running and verifying

- Usa sempre `./gradlew` invece di un `gradle` globale; toolchain richiesta dai manifesti: Java 21 in CI (bytecode target JVM 17), Android SDK `platforms;android-37.0` e `build-tools;37.0.0`.
- Prima di pushare, esegui la verifica completa della CI (`.github/workflows/android.yml`): `./gradlew --no-daemon testDebugUnitTest lintDebug jacocoTestDebugUnitTestReport jacocoTestDebugUnitTestCoverageVerification assembleDebug` — `testDebugUnitTest` da solo non copre Lint (`warningsAsErrors = true`) né la soglia di coverage JaCoCo (5% linee).

## Conventions that differ from defaults

- Mantieni il singolo modulo Gradle `:app` con confini logici porte/adattatori; non aggiungere moduli Gradle e non far dipendere il dominio core da UI, DTO di provider o feature Premium.
- Salva GPX completo e geometria solo in file privati versionati, mai come una riga Room per TrackPoint; usa unità canoniche SI, `Instant` UTC e `ZoneId` del percorso nel dominio, con `null` esplicito al posto di zeri per dati mancanti.
- Non inviare mai GPX intero, nome percorso, contatti o identificativi nei log o verso `WeatherProvider` (massimo 12 campioni normalizzati con ID opaco, coordinate, quota opzionale ed ETA UTC).
- Il progetto usa `minSdk = 26` in `app/build.gradle.kts` (`compileSdk`/`targetSdk = 37`): non introdurre dipendenze incompatibili con API 26 e non aggiornare toolchain senza verificare la compatibilità con AGP 8.13.2 / API 37.

<!-- /bmad:context -->
