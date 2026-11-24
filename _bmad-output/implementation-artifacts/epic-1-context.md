# Epic 1 Context: Preparare un percorso localmente

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Permettere all'escursionista di importare localmente una traccia GPX, ispezionarne geometria e metriche, configurare partenza, fuso orario e passo, e generare e riaprire offline un briefing route-only con stime di percorrenza, senza account, permessi di localizzazione né servizi esterni. L'epica costruisce la fondazione local-first dell'app (storage privato, modelli temporali e di dominio, tema accessibile e preferenze) su cui si innesteranno meteo, luce, quota e condivisione.

## Stories

- Story 1.1: Avvio e accesso locale
- Story 1.2: Importare GPX in sicurezza
- Story 1.3: Comprendere e gestire il percorso
- Story 1.4: Impostare partenza e passo
- Story 1.5: Ottenere ETA e briefing route-only
- Story 1.6: Preferenze e limiti chiari

## Requirements & Constraints

- **Privacy e accesso locale:** nessun account, nessun permesso di posizione corrente né notifiche nella versione Free. Il GPX completo resta sul dispositivo; log e diagnostica non devono mai includere GPX, coordinate, nomi percorso, contatti o identificativi.
- **Importazione GPX sicura:** supportare GPX 1.0/1.1 (tracce e route) da selettore, apertura o condivisione Android, preservando segmenti separati con almeno due punti validi. Rifiutare con errore comprensibile XML malformati, DTD remoti/entità esterne, coordinate non valide e file oltre i limiti provvisori di 20 MB o 200.000 punti (da validare). Tracce senza quota o timestamp restano utilizzabili con mancanza esplicita.
- **Metriche e stime:** calcolare distanza, estremi ed eventuale punto più alto; quote, dislivelli e profilo solo se presenti, senza zeri sostitutivi. Stimare durata ed ETA da distanza, dislivello disponibile e profilo di passo (incluso passo personalizzabile validato); senza quota usare i dati disponibili segnalando il limite. Etichettare sempre gli orari come stime e non dichiarare precisioni non misurate su tracce reali.
- **Tempo, fuso e DST:** i timestamp storici nel GPX non sostituiscono la partenza scelta. Mostrare gli orari nel fuso IANA modificabile del percorso anche se diverso dal fuso del dispositivo. Segnalare e risolvere esplicitamente orari ambigui o inesistenti al cambio d'ora (DST) prima del calcolo.
- **Persistenza, offline e costo zero:** percorsi, piani e briefing route-only restano locali fino a cancellazione utente (rinomina ed eliminazione persistite; l'eliminazione rimuove file e riferimenti). Offline i dati salvati restano consultabili e quelli non salvati sono segnalati. Nessuna chiamata esterna né servizi a pagamento.
- **Preferenze e limiti:** italiano/inglese, unità metriche/imperiali, passo e fuso predefiniti persistenti. Sezione informativa con fonti, privacy e limiti: supporto alla pianificazione, nessuna garanzia di sicurezza, nessuna sostituzione di fonti ufficiali o giudizio personale, nessuna consulenza sanitaria o guida turistica.
- **Piattaforma, prestazioni e accessibilità:** Android 8.0 (API 26) minimo; elaborazione fuori dal thread UI. Obiettivi da misurare senza dichiararli raggiunti prima della verifica: avvio a freddo < 2 s, importazione 50.000 punti < 2 s, analisi < 1 s aggiuntivo. Supportare testo ingrandito, TalkBack, tastiera/switch access, portrait/landscape e animazioni ridotte, senza affidarsi solo a colore o gesti.

## Technical Decisions

- **Architettura a livelli:** singolo modulo `:app` con confini logici porte/adattatori. La UI Compose invia eventi ai ViewModel e osserva stato immutabile; mutazioni via casi d'uso e repository. Il dominio definisce modelli e porte propri senza dipendere da UI, DTO esterni o future feature Premium.
- **Storage ibrido:** salvare GPX completo e geometria normalizzata (segmenti, WGS84, quota opzionale) in file privati versionati; persistere in Room solo metadati e riferimenti, mai un'entità per TrackPoint. Metriche sempre sulla geometria completa; geometria semplificata solo per il rendering.
- **Sicurezza XML e backup:** disabilitare DTD remoti ed entità esterne nel parser. Impostare `android:allowBackup="false"` con `data_extraction_rules.xml` e `backup_rules.xml` che escludano file GPX, database e DataStore da backup cloud e trasferimento tra dispositivi.
- **Modelli temporali e unità:** unità canoniche SI nel dominio (metri) e nullabilità esplicita per dati mancanti. Memorizzare partenza ed ETA come `Instant` UTC con `ZoneId` IANA del percorso; formattare nel fuso del percorso e nelle unità utente in presentazione.
- **ID, versionamento ed errori:** identificativi opachi stabili per percorso, piano e briefing; `planVersion: Long` monotono sul piano incrementato a ogni modifica di partenza, fuso, profilo o geometria. Errori tipizzati dai repository, tradotti dalla UI senza cancellare dati validi esistenti.
- **Anteprima tracciato:** canvas vettoriale locale (zero rete) come vista predefinita sicura, con alternativa testuale equivalente; non promettere sfondi mappa offline.

## UX & Interaction Patterns

- **Visual e Material 3:** tono calmo e prudente, senza allarmismi né parole come «sicuro». Token candidati tema chiaro via ruoli Material 3 (tema scuro con ruoli Material a contrasto verificato, senza inversioni automatiche): `primary #245C48`, `onPrimary #FFFFFF`, `surface #FAFAF5`, `onSurface #19231E`, `surfaceContainer #E8EEE8`, `outline #56675C`, `error #9B2424`, `warning #705500`; raggi `4dp`/`12dp`/`20dp`; spaziature `4dp`–`24dp` con margine `16dp`; font di sistema (`headlineSmall`, `bodyLarge`, `labelLarge`). Contrasto ≥ 4,5:1 testo normale e ≥ 3:1 testo grande.
- **Componenti:** *azione primaria* con verbo, target ≥ `48 × 48 dp`, stati abilitato/disabilitato/progresso distinguibili e blocco duplicati; *riepilogo* in alto con dettagli sotto, unità esplicite, qualificatore «stimato» e stato route-only; *avvisi* con spiegazione e azione recuperabile senza gergo tecnico (assenza quota è limite, non errore).
- **Flussi e accessibilità:** Home vuota/popolata → importazione con progresso annunciato da TalkBack → anteprima (rinomina, conferma esplicita eliminazione, canvas e alternativa testuale) → configurazione (data/ora, fuso leggibile, risoluzione DST, passo; indietro conserva le scelte) → briefing route-only offline → impostazioni e informazioni. Nessuna larghezza fissa sui testi; ordine di focus coerente; equivalenti testuali per tracciato e profilo.

## Cross-Story Dependencies

- **Interne all'epica:** Story 1.1 definisce tema Material 3, shell/Home, `allowBackup="false"` e confini architetturali. Story 1.2 fornisce importazione e storage ibrido per anteprima (1.3), configurazione piano (1.4) ed ETA/briefing route-only (1.5). Story 1.6 fornisce preferenze persistenti (lingua, unità, passo, fuso) usate in 1.3–1.5.
- **Verso epiche successive:** il canvas vettoriale di 1.3 è il fallback dello sfondo mappa opt-in in Epic 2; il piano con `Instant`/`ZoneId` e `planVersion` (1.4–1.5) abilita campionamento, meteo e coordinamento idempotente della quota in Epic 2–3; `allowBackup="false"` (1.1) preserva la coerenza locale del contatore Free in Epic 3.
