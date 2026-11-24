---
status: final
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - prds/prd-TrekCheck-2026-11-20/prd.md
  - architecture/architecture-TrekCheck-2026-11-21/ARCHITECTURE-SPINE.md
  - ux-designs/ux-TrekCheck-2026-11-21/DESIGN.md
  - ux-designs/ux-TrekCheck-2026-11-21/EXPERIENCE.md
---

# TrekCheck - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for TrekCheck, decomposing the requirements from the PRD, UX Design if it exists, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

- **FR-1 — Importazione GPX.** L'utente può importare localmente file GPX 1.0/1.1 tramite selettore, apertura o condivisione. Accettare tracce e route e preservare segmenti separati; almeno due punti validi. File malformati, coordinate invalide, file oltre 20 MB o oltre 200.000 punti ricevono un errore comprensibile senza bloccare l'app. File privi di quota o timestamp restano utilizzabili. [IPOTESI: limiti iniziali dalla baseline.]
- **FR-2 — Metriche.** L'app calcola distanza, estremi e, se disponibili, quote e dislivelli stimati. Senza quota mostra la distanza e segnala che profilo e dislivello non sono disponibili; non inventa valori.
- **FR-3 — Anteprima.** L'utente visualizza traccia, punti iniziale e finale, eventuale punto più alto, metriche e profilo altimetrico disponibile; può rinominare o eliminare il percorso. Se la mappa non è raggiungibile, metriche e funzioni locali restano accessibili.
- **FR-4 — Piano.** L'utente sceglie data e ora di partenza, fuso geografico del percorso modificabile e un profilo di passo, incluso uno personalizzabile. Orari storici presenti nel GPX non sostituiscono la partenza scelta; il profilo scelto influisce sulla stima e gli orari sono mostrati nel fuso del percorso. Gli orari ambigui o inesistenti al cambio d’ora devono essere segnalati e risolti esplicitamente prima del calcolo.
- **FR-5 — ETA.** L'app stima tempi di passaggio e arrivo usando distanza, dislivello disponibile e profilo di passo. Se manca la quota usa una stima basata sui dati disponibili, segnalandone il limite. Tutti gli orari derivati sono identificati come stime. La precisione va verificata con tracce e tempi reali prima di qualsiasi dichiarazione quantitativa.
- **FR-6 — Punti rappresentativi.** L'app seleziona campioni lungo il percorso, includendo partenza, arrivo e punto più alto quando disponibili, evitando duplicati e rispettando il massimo iniziale di 12 punti; se punti chiave coincidono, non si inventano punti distinti. [IPOTESI: campionamento iniziale dalla baseline da verificare.]
- **FR-7 — Previsioni.** Per ogni campione disponibile, l'app associa la previsione all'ETA e mostra fonte, aggiornamento e risoluzione. Indica dati mancanti o parziali; distingue valori istantanei e precipitazioni riferite a intervalli. Non presuppone che tutte le previsioni siano orarie. I valori istantanei devono essere sufficientemente vicini all’ETA secondo tolleranze da definire; per le precipitazioni il periodo deve contenere l’ETA, altrimenti il dato è indicato come non disponibile.
- **FR-8 — Disponibilità meteo.** Se l'uscita è fuori dall'orizzonte del fornitore, l'app non invia richieste meteo, spiega il limite e offre i dati route-only. Se un servizio non risponde o limita le richieste, informa l'utente e preserva ciò che è già disponibile.
- **FR-9 — Luce.** L'app calcola localmente le informazioni di luce disponibili e mostra la relazione tra arrivo stimato e tramonto, senza bloccare l'utente o dichiarare il percorso sicuro.
- **FR-10 — Briefing.** Il riepilogo in alto mostra percorso, durata e arrivo stimati, meteo e luce disponibili; sotto sono accessibili mappa, profilo, dettagli e timeline. Ogni dato indica quando è una stima o non è disponibile.
- **FR-11 — Timeline e mappa.** L'utente consulta punti rappresentativi con distanza, ETA, quota e meteo quando disponibili; la selezione collega i dettagli al punto sulla mappa. Non è navigazione o tracciamento in tempo reale.
- **FR-12 — Condizioni.** Regole deterministiche e versionate producono segnalazioni comprensibili associate ai dati disponibili e, quando possibile, al punto e all'orario; assenza di segnalazioni non equivale a sicurezza garantita.
- **FR-13 — Checklist.** L'app genera suggerimenti coerenti con le condizioni calcolate e ne mostra il motivo. Non prescrive trattamenti, quantità cliniche o decisioni di sicurezza.
- **FR-14 — Salvataggio locale.** Percorsi, piani, briefing e snapshot necessari sono conservati sul dispositivo fino alla cancellazione richiesta dall'utente. La geometria completa resta locale.
- **FR-15 — Offline.** Senza rete l'utente può riaprire briefing salvati, metriche, ETA, timeline, condizioni e checklist con i dati precedentemente disponibili. Profilo altimetrico e previsioni già salvate restano consultabili; i dati non salvati sono segnalati come non disponibili. La disponibilità di tutte le mappe offline non è garantita e va indicata.
- **FR-16 — Refresh manuale.** Con rete, quota e previsione disponibili, l'utente può aggiornare e confrontare lo snapshot precedente. Le precipitazioni vengono confrontate solo su intervalli equivalenti; in caso contrario l'app spiega che il confronto non è disponibile. Il nuovo snapshot sostituisce quello attuale solo dopo successo e salvataggio; il fallimento conserva il precedente.
- **FR-17 — Quota Free.** [IPOTESI APPROVATA DA VALIDARE] La prima generazione meteo riuscita e ogni refresh meteo riuscito consumano una delle tre unità Free complessive per installazione. Fallimenti, route-only, consultazione offline e condivisione non consumano unità. Eliminare un briefing non ripristina quota; una reinstallazione può azzerare il conteggio locale. A quota esaurita non si inviano richieste meteo, ma restano disponibili route-only e contenuti locali.
- **FR-18 — Share Card.** L'utente può vedere un'anteprima e condividere un'immagine generata localmente con riepilogo disponibile, lingua e unità selezionate, fino a tre segnalazioni e attribuzioni richieste. L'anteprima corrisponde all'export; coordinate precise, contatti, note e GPX non sono inclusi per default. La condivisione non consuma quota e funziona offline con dati salvati.
- **FR-19 — Preferenze.** L'utente può selezionare italiano o inglese, unità metriche o imperiali, passo predefinito e preferenza di fuso; può consultare fonti, informativa privacy e limiti dell'app.
- **FR-20 — Errori.** File, rete, provider, limite Free, fuso, mappa ed export presentano messaggi comprensibili e un'azione appropriata quando disponibile, senza cancellare dati validi esistenti.
- **FR-21 — Comunicazione dei limiti.** Le informazioni sull'app spiegano in modo accessibile che TrekCheck supporta la pianificazione, non garantisce la sicurezza, non sostituisce fonti ufficiali o giudizio personale e non offre consulenza sanitaria o servizi di guida turistica. I testi finali e gli obblighi applicabili saranno esaminati prima della pubblicazione.

### NonFunctional Requirements

- **NFR-1 — Privacy:** nessun account o permesso di posizione corrente richiesto per il briefing; il GPX completo resta sul dispositivo. Al servizio meteo sono inviati solo campioni necessari, quota se disponibile e orario, senza nome percorso o identità. Telemetria minima o assente; nessun dato sensibile negli eventuali eventi.
- **NFR-2 — Sicurezza e affidabilità:** importazione XML protetta; input e dimensioni verificati; errori e dati parziali riconoscibili; dati precedenti mantenuti in caso di errore di refresh.
- **NFR-3 — Costo:** soluzioni gratuite e spesa zero come vincolo utente. Verificare preventivamente condizioni, quote e assenza di addebiti per ogni servizio, gateway incluso; non configurare piani o operazioni a pagamento. La sola esistenza di un piano gratuito non prova costo zero a qualsiasi volume.
- **NFR-4 — Piattaforma e accessibilità:** Android 8 / API 26 minimo; italiano e inglese; unità metriche e imperiali; contenuti leggibili con dimensioni di testo maggiorate e informazioni comprensibili senza affidarsi al solo colore. La copertura effettiva va verificata.
- **NFR-5 — Prestazioni:** [OBIETTIVI DA VALIDARE] importazione 50.000 punti sotto 2 s su dispositivo moderno di fascia media, analisi possibilmente sotto 1 s aggiuntivo, avvio a freddo sotto 2 s; nessun blocco dell'interfaccia. Ambiente e metodo di misura devono essere definiti.
- **NFR-6 — Tempi e fusi:** memorizzare eventi come istanti e usare il fuso del percorso per la visualizzazione; verificare casi DST e fuso del dispositivo differente.

### Additional Requirements

- AD-1: confini porte/adattatori, stato immutabile e dominio indipendente dal Premium.
- AD-2: geometria in file privati versionati, segmenti conservati, niente entità DB per punto, parser XML sicuro.
- AD-3: Instant, ZoneId, unità SI e handling DST; dati mancanti espliciti.
- AD-4: campioni normalizzati max 12, gateway UTC, cache senza ETA, no richiesta fuori orizzonte (216 h), diff per intervalli comparabili; contratto `WeatherBatchResult` per-ID (`Available`/`Unavailable` + `GlobalFailure`); tolleranza istantanei ±45 min (`PT1H`) / ±180 min (`PT6H`), precipitazioni sull'intervallo più breve che contiene l'ETA.
- AD-5: `BriefingGenerationCoordinator` serializzato con `Mutex`, `planVersion` sul piano, protocollo idempotente Reserve → Commit Storage → Settle con `operationId` e `reconcile()` al riavvio; successo parziale se ≥ 60% campioni validi (≥ 2 metriche su 3) e Partenza + Quota Massima (o mediano) validi.
- AD-6: motori locali deterministici e versionati, export privato con preview coerente; Share Card senza sagoma 2D (solo profilo altimetrico stilizzato opzionale); `android:allowBackup="false"` con regole che escludono GPX, Room e DataStore.
- AD-7: nessuna attivazione fino a verifica termini e spesa zero; fallback route-only e fixture; nessun billing; `BuildConfig.WEATHER_MODE` (`SIMULATED` predefinito, `REAL_ZERO_COST` dopo verifica); canvas vettoriale locale predefinito, tile mappa solo opt-in con informativa.
- Starter: progetto Android esistente con modulo :app e Gradle Wrapper; non rigenerare come progetto nuovo. `minSdk = 26` già allineato; compatibilità AGP/API 37 da verificare.

### UX Design Requirements

- UX-DR1: tema Material 3 e token proposti (colori, tipografia, spaziatura, raggi); validare contrasti, tema scuro, testo ingrandito.
- UX-DR2: azione primaria con stati e target 48dp; componente riepilogo con stima, unità e freschezza.
- UX-DR3: Home vuota/popolata, importazione e apertura GPX, progresso ed errori recuperabili.
- UX-DR4: anteprima con nome, metriche, mappa e profilo o alternative testuali, conferma eliminazione.
- UX-DR5: configurazione con data/ora, fuso, DST ambiguo/inesistente, profilo e passo personalizzato.
- UX-DR6: briefing con riepilogo sopra, timeline/mappa/profilo/checklist sotto; dati mancanti, parziali, offline e fuori orizzonte distinti.
- UX-DR7: quota disponibile prima della richiesta e consumo chiaro secondo il criterio approvato (≥ 60% + punti chiave); indicare cosa lascia il dispositivo.
- UX-DR8: confronto refresh con dati comparabili, conservazione briefing precedente su fallimento.
- UX-DR9: anteprima Share Card coerente e Sharesheet, esclusione dati personali/coordinate precise di default.
- UX-DR10: impostazioni IT/EN, unità, passo, fuso, fonti, privacy e avvertenze; nessun permesso di localizzazione o notifiche Free.
- UX-DR11: TalkBack, focus e tastiera, contrasti, target, alternative a mappa/grafico, orientamento e schermi variabili.
- UX-DR12: verifiche con utenti reali ancora da svolgere; wireframe e stile proposti, non approvati come identità definitiva.

### FR Coverage Map

- FR-1 → Epic 1
- FR-2 → Epic 1
- FR-3 → Epic 1
- FR-4 → Epic 1
- FR-5 → Epic 1
- FR-6 → Epic 2
- FR-7 → Epic 2
- FR-8 → Epic 2
- FR-9 → Epic 2
- FR-10 → Epic 2
- FR-11 → Epic 2
- FR-12 → Epic 2
- FR-13 → Epic 2
- FR-14 → Epic 1, 2
- FR-15 → Epic 1, 2
- FR-16 → Epic 3
- FR-17 → Epic 3
- FR-18 → Epic 3
- FR-19 → Epic 1, 3
- FR-20 → Epic 1, 2, 3
- FR-21 → Epic 1, 2, 3

## Epic List

### Epic 1 — Preparare un percorso localmente
Importazione GPX, metriche, configurazione, ETA, consultazione e gestione route-only/offline. FR-1–5, FR-14–15, FR-19–21. Priorità P0.

### Epic 2 — Consultare un briefing lungo il percorso
Luce, condizioni e meteo associati a ETA, con dati simulati prima della verifica dei servizi; briefing parziale e route-only continuano a funzionare. FR-6–15, FR-20–21. Priorità P1; rete reale bloccata dalla verifica di termini e costo zero.

### Epic 3 — Aggiornare e condividere il piano
Refresh e diff, quota Free, preferenze e Share Card locale. FR-14–21. Priorità P2; il consumo quota segue il criterio parziale approvato (≥ 60% + punti chiave) e il protocollo idempotente di AD-5.

## Epic 1: Preparare un percorso localmente

Obiettivo: ottenere e riaprire un piano route-only anche senza servizi esterni. Il progetto corrente è il punto di partenza (`minSdk = 26` già allineato). Copre FR-1–5, FR-14–15, FR-19–21; NFR-1–6; UX-DR1–5, 10–12.

### Story 1.1: Avvio e accesso locale
Come escursionista voglio aprire TrekCheck e importare una traccia senza account, per iniziare la pianificazione. NFR-1, NFR-4–5; AD-1, AD-6; UX-DR1–3, UX-DR11.

**Acceptance Criteria:**

- **Given** una nuova installazione **When** apro Home **Then** vedo stato vuoto e azione Importa GPX senza login o richiesta posizione.
- **Given** Android 8 o successivo **When** avvio l'app **Then** il requisito minimo è API 26 e il flusso è accessibile; il progetto usa Wrapper esistente e controlli CI.
- **Given** un dispositivo senza rete **When** apro Home **Then** i dati locali disponibili rimangono consultabili.
- **Given** il tema dell'app **When** apro Home in tema chiaro o scuro **Then** sono applicati i token Material 3 di DESIGN.md (colori, tipografia, spaziatura, raggi) tramite ruoli Material, e l'azione primaria ha stati abilitato/disabilitato/progresso distinguibili e target ≥ 48 × 48 dp.
- **Given** il manifest dell'app **When** verifico la configurazione **Then** `android:allowBackup="false"` e le regole `data_extraction_rules.xml` / `backup_rules.xml` escludono file privati, database e DataStore da backup cloud e trasferimento tra dispositivi; un test o un controllo lint verifica la configurazione.
- **Given** un dispositivo di riferimento definito **When** misuro l'avvio a freddo **Then** registro il risultato rispetto all'obiettivo NFR-5 (< 2 s) senza dichiararlo raggiunto se non misurato.

### Story 1.2: Importare GPX in sicurezza
Come escursionista voglio scegliere, aprire o condividere un GPX, per analizzarlo sul dispositivo. FR-1, FR-20; NFR-1–2, NFR-5; AD-2; UX-DR3.

**Acceptance Criteria:**

- **Given** GPX 1.0/1.1 track/route con almeno due punti validi **When** lo importo tramite selettore, apri o condividi **Then** segmenti e dati validi sono preservati localmente.
- **Given** un import riuscito **When** salvo il percorso **Then** GPX e geometria normalizzata (segmenti, coordinate, quota opzionale) sono in file privati versionati e solo metadati e riferimenti sono persistiti nel database, senza un'entità per punto.
- **Given** file malformato, XML con entità esterne/DTD remoto o coordinate non valide **When** importo **Then** la lettura viene rifiutata con errore recuperabile senza accessi esterni.
- **Given** i limiti provvisori 20 MB e 200.000 punti **When** provo esattamente al limite e oltre **Then** il limite è applicato e l'eccesso genera messaggio chiaro; i limiti restano da validare.
- **Given** quota o timestamp assenti **When** importo **Then** la traccia valida è disponibile e la mancanza è esplicita.
- **Given** un import in corso **When** il file è grande **Then** l'elaborazione avviene fuori dal thread UI, il progresso è visibile e annunciato da TalkBack, e una fixture da 50.000 punti viene misurata rispetto all'obiettivo NFR-5 (< 2 s).
- **Given** errori o diagnostica dell'import **When** vengono registrati **Then** non contengono GPX, coordinate, nomi di percorso o identità.

### Story 1.3: Comprendere e gestire il percorso
Come escursionista voglio vedere le metriche e un'anteprima del percorso per capire cosa ho importato. FR-2–3, FR-14; AD-2, AD-7; UX-DR4.

**Acceptance Criteria:**

- **Given** geometria valida **When** apro anteprima **Then** vedo distanza, punti iniziale/finale ed eventuale punto più alto; quota, dislivelli e profilo solo se disponibili e stimati ove appropriato.
- **Given** l'anteprima **When** visualizzo il tracciato **Then** è disegnato con il canvas vettoriale locale (zero richieste di rete) da una geometria semplificata solo per il rendering, mentre le metriche usano la geometria completa; è presente un'alternativa testuale.
- **Given** rete assente **When** apro anteprima **Then** metriche, tracciato locale e alternativa testuale restano accessibili, senza promettere sfondi mappa offline.
- **Given** percorso salvato **When** rinomino o confermo eliminazione **Then** l'azione è persistita e la cancellazione rimuove file e riferimenti pertinenti.

### Story 1.4: Impostare partenza e passo
Come escursionista voglio scegliere partenza, fuso e passo per adattare il piano. FR-4, FR-19; NFR-6; AD-3; UX-DR5, UX-DR10.

**Acceptance Criteria:**

- **Given** un percorso **When** configuro il piano **Then** imposto data, ora, fuso IANA modificabile e profilo, incluso passo personalizzabile con validazione.
- **Given** un GPX con timestamp storici **When** configuro il piano **Then** la partenza scelta dall'utente prevale e i timestamp del file non vengono usati come orario di partenza.
- **Given** ora locale inesistente o ambigua per DST **When** tento di procedere **Then** vedo il problema e scelgo una risoluzione o modifico l'ora prima del calcolo.
- **Given** fuso del telefono diverso **When** visualizzo piano e orari **Then** l'interfaccia indica il fuso del percorso e l'istante è coerente.

### Story 1.5: Ottenere ETA e briefing route-only
Come escursionista voglio una stima locale di percorrenza per pianificare anche senza meteo. FR-5, FR-10, FR-14–15; AD-3; UX-DR2, UX-DR6.

**Acceptance Criteria:**

- **Given** distanza, dislivello disponibile e profilo scelto **When** genero un piano **Then** vedo ETA di passaggio e arrivo etichettate come stime; cambiare profilo influisce sul calcolo.
- **Given** quota assente **When** calcolo **Then** uso i dati disponibili e mostro il limite, senza inventare dislivello.
- **Given** un briefing route-only **When** lo apro **Then** il componente riepilogo mostra percorso/distanza, partenza/fuso, durata e arrivo stimati con unità e qualificatore «stimato», e indica che il meteo non è richiesto.
- **Given** una generazione in corso **When** premo di nuovo l'azione **Then** non avvio duplicati e il progresso è annunciato.
- **Given** briefing route-only salvato **When** riapro offline **Then** metriche, ETA e contenuti salvati sono disponibili; i non salvati sono indicati.
- **Given** un insieme di tracce reali **When** si verifica l'ETA **Then** gli scostamenti sono riportati senza dichiarare una precisione non misurata.

### Story 1.6: Preferenze e limiti chiari
Come escursionista voglio scegliere lingua e unità e conoscere i limiti dell'app. FR-19–21; UX-DR10–11.

**Acceptance Criteria:**

- **Given** impostazioni **When** scelgo italiano/inglese, unità metriche/imperiali, passo e fuso preferiti **Then** tali preferenze persistono e i valori mostrano unità coerenti.
- **Given** pagina informazioni **When** la apro **Then** trovo fonti, privacy e limiti: supporto alla pianificazione, nessuna garanzia sicurezza, consulenza sanitaria o guida turistica.
- **Given** l'app Free **When** la uso **Then** non vengono richiesti permessi di localizzazione o notifiche.
- **Given** ingrandimento testo, TalkBack o tastiera **When** uso i flussi **Then** etichette, ordine focus, target 48dp e contenuti sono verificabili senza affidarsi solo a colore o gesti.
- **Given** orientamento portrait/landscape, schermi di dimensioni diverse o animazioni ridotte nel sistema **When** uso i flussi **Then** il contenuto resta scorrevole, nessuna azione è nascosta e le animazioni rispettano la preferenza.

## Epic 2: Consultare un briefing lungo il percorso

Obiettivo: comprendere ETA, meteo, luce e condizioni lungo la traccia, con fixture finché i servizi non sono verificati. Copre FR-6–15, FR-20–21; NFR-1–6; UX-DR6, UX-DR11–12.

### Story 2.1: Selezionare punti significativi
Come escursionista voglio informazioni nei punti rappresentativi della mia traccia per capire come variano le condizioni. FR-6; AD-4; UX-DR6.

**Acceptance Criteria:**

- **Given** una traccia con inizio, fine e punto alto disponibili **When** seleziono i campioni **Then** tali punti sono inclusi compatibilmente con deduplicazione e massimo 12.
- **Given** punti coincidenti o traccia breve **When** campiono **Then** non creo duplicati o dati inventati; la selezione è deterministica.
- **Given** soglie distanza/tempo della baseline **When** le applico **Then** sono configurabili e marcate come ipotesi fino a validazione.

### Story 2.2: Verificare i servizi gratuiti prima dell'uso
Come proprietario voglio evitare spese e uso non conforme per poter valutare il meteo in sicurezza economica. FR-7–8; NFR-3; AD-7.

**Acceptance Criteria:**

- **Given** candidati MET Norway, OpenFreeMap e gateway senza carta di credito (es. Cloudflare Workers Free senza upgrade) **When** verifico condizioni, uso commerciale, attribuzioni, `User-Agent`, limiti e hard-stop `429/503` **Then** registro fonte, data, quote e meccanismo concreto che esclude addebiti; verifica non raggiungibile è «non verificata».
- **Given** build flag `BuildConfig.WEATHER_MODE = SIMULATED` (predefinito) o verifica non riuscita **When** eseguo l'app **Then** provider reale e deploy restano disabilitati; fixture e route-only rimangono disponibili senza rete.
- **Given** il canvas vettoriale locale di Story 1.3 **When** l'utente attiva lo sfondo mappa online nelle impostazioni **Then** prima del primo caricamento vede un'informativa su IP e area visualizzata inviati al fornitore, con link a termini e attribuzione; con opzione disattivata, offline o fornitore non verificato si usa automaticamente il canvas locale.

### Story 2.3: Associare previsioni a punti e tempi con fixture
Come escursionista voglio vedere il meteo previsto lungo il percorso per capire cosa potrei incontrare. FR-7–8, FR-11, FR-20; AD-3–4; UX-DR6.

**Acceptance Criteria:**

- **Given** contratto `WeatherBatchResult` (`Success` con mappa per-`SampleId` di `Available`/`Unavailable(reason)` o `GlobalFailure`) e risposte simulate con ETA UTC e risoluzioni miste (`PT1H` / `PT6H`) **When** associo campioni e tempi **Then** mostro fonte, validità, freschezza, risoluzione e valori disponibili nel fuso del percorso.
- **Given** valori istantanei (temperatura, vento) **When** mappo sull'ETA **Then** seleziono il timestamp più vicino (a parità vince il successivo) con tolleranza massima `±45 min` su `PT1H` e `±180 min` su `PT6H`; fuori tolleranza il valore è `null` (assente) senza interpolazione.
- **Given** precipitazioni **When** mappo sull'ETA **Then** seleziono l'intervallo `[periodStart, periodEnd)` più breve (`PT1H` preferito a `PT6H`) che contiene `etaUtc`, riportando `precipitationWindow`; se assente indico dato non disponibile.
- **Given** un'uscita completamente fuori orizzonte (`> now + 216h` o `< now - 1h`) **When** creo il piano **Then** nessuna chiamata provider è eseguita (`OutOfHorizon`) e route-only rimane accessibile.
- **Given** un `GlobalFailure` simulato (`Offline`, `RateLimited`, `ProviderUnavailable`) **When** richiedo il meteo **Then** vedo un messaggio comprensibile con l'azione successiva (riprovare più tardi o usare route-only), senza termini tecnici del fornitore, e i dati già disponibili restano intatti.
- **Given** payload di campioni **When** è inviato attraverso la porta `WeatherProvider` **Then** non contiene file GPX, nome percorso, contatti o identità e non supera 12 campioni.

### Story 2.4: Luce, segnalazioni e checklist
Come escursionista voglio conoscere luce e condizioni da considerare per preparare il materiale. FR-9, FR-12–13, FR-21; AD-6; UX-DR6.

**Acceptance Criteria:**

- **Given** posizione e tempo utili **When** calcolo la luce **Then** mostro localmente tramonto e margine quando disponibili; se non calcolabili o inesistenti mostro «non disponibile».
- **Given** soglie versionate e dati sufficienti **When** genero condizioni e checklist **Then** le regole sono deterministiche, spiegano il motivo e non prescrivono sicurezza o trattamenti sanitari.
- **Given** una segnalazione derivata da un campione **When** la mostro **Then** è associata, quando possibile, al punto e all'orario stimato che l'hanno generata.
- **Given** dati mancanti **When** calcolo **Then** distinguo «non disponibile» da assenza di segnalazioni e non dichiaro il percorso sicuro.

### Story 2.5: Riepilogo e timeline accessibili
Come escursionista voglio leggere rapidamente il briefing e approfondire i punti, per comprendere le condizioni previste. FR-10–11, FR-14–15, FR-20–21; UX-DR1–2, UX-DR6, UX-DR11–12.

**Acceptance Criteria:**

- **Given** un briefing completo, parziale o route-only **When** lo apro **Then** riepilogo compare in alto, dettagli sotto, con etichette stima, dati mancanti e ultimo aggiornamento.
- **Given** un punto timeline **When** lo seleziono **Then** dettagli e mappa sono collegati, ma un'alternativa testuale mostra le stesse informazioni essenziali.
- **Given** un briefing con meteo, luce, condizioni e checklist **When** lo salvo **Then** briefing e snapshot sono conservati localmente e, riaperti offline, mostrano «Dati salvati» con data dell'aggiornamento; i dati non salvati sono indicati come non disponibili.
- **Given** testo ingrandito e lettore schermo **When** navigo il briefing **Then** ordine, etichette e contrasto sono verificati; risultati di test utenti restano da raccogliere.

## Epic 3: Aggiornare e condividere il piano

Obiettivo: rivedere i cambiamenti e condividere il briefing senza perdere dati; FR-14–21; NFR-1–4, NFR-6; UX-DR7–11.

### Story 3.1: Implementare e verificare il criterio di copertura parziale
Come proprietario voglio una regola deterministica e testata di successo parziale per evitare consumi Free sorprendenti. FR-7, FR-16–17; AD-5; UX-DR7.

**Acceptance Criteria:**

- **Given** un campione `Available` **When** valuto la validità **Then** è `ValidSample` se ha almeno 2 metriche su 3 tra temperatura, vento e precipitazione (o temperatura + vento se la finestra precipitazioni non è fornita).
- **Given** una risposta batch per `N` campioni **When** applico il criterio approvato **Then** è `COMPLETE` se il 100% ha 3/3 metriche, è `PARTIAL` (sufficiente, 1 quota) se almeno il **60% dei campioni** (`ceil(0.60 * N)`) è `ValidSample` **E** sono validi i punti chiave (**Partenza** e **Quota Massima**, o punto mediano se manca la quota nel GPX).
- **Given** copertura sotto soglia o punto chiave mancante **When** valuto la risposta **Then** restituisco `InsufficientWeatherCoverage`, non promuovo lo snapshot e consumo 0 quote, con test unitari su scenari completi, parziali, soglia limite, mancanti ed errore.

### Story 3.2: Consumare la quota una sola volta
Come escursionista voglio che una generazione fallita non consumi quota e che il conteggio sia comprensibile. FR-16–17; AD-5; UX-DR7.

**Acceptance Criteria:**

- **Given** `BriefingGenerationCoordinator` serializzato con `Mutex` e piano con `planVersion` **When** avvio una generazione **Then** eseguo il protocollo persistente in 3 fasi: (1) Reserve (`pendingOperationId` con `opId`, `planId`, `expectedPlanVersion` in DataStore previa `reconcile()`), (2) Commit Storage atomico in Room solo se piano esiste e `planVersion == expectedPlanVersion`, (3) Settle idempotente in DataStore (`opId` in `chargedOperationIds`, `usedCount += 1`, pulizia `pendingOperationId`).
- **Given** modifica o cancellazione del piano durante la richiesta in volo **When** arriva la risposta o scatta la modifica **Then** il `Job` in volo viene cancellato o la risposta viene scartata come `StalePlanDiscard` senza consumare quota, lasciando lo snapshot precedente marcato come non allineato.
- **Given** crash tra salvataggio Room e aggiornamento DataStore **When** riavvio o ripeto l'operazione **Then** `reconcile()` verifica su storage lo snapshot per `pendingOperationId`: se promosso completa `Settle` una sola volta, altrimenti ripulisce senza consumare quota; nessuna nuova generazione parte prima della riconciliazione.
- **Given** tre generazioni riuscite **When** richiedo meteo **Then** non si inviano chiamate e restano disponibili route-only e consultazione offline. Cancellazione briefing non riduce contatore; con `allowBackup="false"` (Story 1.1) una reinstallazione azzera il contatore locale.
- **Given** sto per richiedere meteo **When** vedo la conferma **Then** il numero di unità disponibili, la regola di consumo (solo una generazione riuscita, completa o parziale ≥ 60% con punti chiave, consuma una unità) e i dati che lasciano il dispositivo (campioni con coordinate, quota opzionale ed ETA, senza nome percorso né GPX) sono chiari; durante la generazione non posso avviarne una seconda.

### Story 3.3: Confrontare un aggiornamento
Come escursionista voglio confrontare previsioni nuove e precedenti per capire cosa è cambiato. FR-16; AD-4–5; UX-DR8.

**Acceptance Criteria:**

- **Given** quota disponibile e risposta valida **When** aggiorno **Then** confronto temperatura, vento e precipitazione solo se gli intervalli sono equivalenti; altri confronti spiegano l'indisponibilità.
- **Given** errore, mancata rete o salvataggio fallito **When** aggiorno **Then** il precedente briefing resta corrente e leggibile, con messaggio e possibilità di riprovare.
- **Given** uscita fuori orizzonte o quota esaurita **When** richiedo refresh **Then** nessuna chiamata meteo avviene e il piano locale resta disponibile.

### Story 3.4: Condividere una scheda privata
Come escursionista voglio condividere un riepilogo per informare una persona fidata. FR-18; AD-6; UX-DR9.

**Acceptance Criteria:**

- **Given** briefing salvato **When** apro anteprima **Then** vedo contenuto effettivamente esportato in lingua e unità scelte con attribuzioni e massimo tre segnalazioni.
- **Given** impostazioni predefinite MVP **When** esporto **Then** non includo GPX, coordinate precise, contatti, note né **sagoma 2D del tracciato**; includo solo riepilogo testuale/metrico e profilo altimetrico stilizzato opzionale senza georeferenziazione.
- **Given** dispositivo offline **When** condivido i dati salvati **Then** l'immagine è generata localmente, Sharesheet si apre solo su azione esplicita e nessuna quota è consumata.
- **Given** errore export **When** riprovo **Then** il briefing non è cancellato e il messaggio spiega l'azione successiva.

### Story 3.5: Usare meteo reale senza spesa
Come escursionista voglio ricevere previsioni reali quando i servizi gratuiti sono stati verificati, per ottenere un briefing utile senza creare costi al progetto. FR-6–8, FR-16–17; AD-4, AD-5, AD-7.

**Dipendenze e gate operativo:** completare 2.2, 2.3, 3.1 e 3.2; l'attivazione di `BuildConfig.WEATHER_MODE = REAL_ZERO_COST` e l'eventuale deploy gateway sono subordinati alla verifica documentata di Story 2.2 (termini, attribuzioni, `User-Agent` e hard-stop `429/503` senza carta di credito).

**Acceptance Criteria:**

- **Given** le verifiche documentate di Story 2.2 sono positive e `WEATHER_MODE=REAL_ZERO_COST` è configurato su infrastruttura senza billing **When** richiedo un briefing meteo **Then** gateway normalizza coordinate/quota, applica validazioni, cache raw senza ETA e `User-Agent` richiesto, restituisce `WeatherBatchResult` UTC e attribuzioni, senza GPX o identificativi.
- **Given** quote esaurite, termini non verificati o configurazione non capace di prevenire costi **When** tento l'accesso **Then** nessuna chiamata reale o deploy a pagamento avviene e route-only rimane disponibile.
- **Given** risposte complete, parziali (`>= 60%` + punti chiave), sotto soglia, timeout, 429 e 5xx **When** applico il criterio approvato e ripeto le richieste **Then** quota, recupero, cache e fallback rispettano i test di 3.1–3.3; nessun segreto compare nei log.

## Verifica copertura e stato del piano

- FR-1–21 e NFR-1–6 sono inventariati e coperti; `minSdk = 26` è allineato in `app/build.gradle.kts` e `android:allowBackup="false"` è fissato per l'MVP. UX-DR1–12 sono coperti dalle storie indicate negli epic.
- **Decisioni architetturali approvate (2026-11-24):** contratto `WeatherBatchResult` per-ID e tolleranze adattive (`±45m` su `PT1H`, `±180m` su `PT6H`, orizzonte 216h); soglia successo parziale `>= 60%` + Partenza e Quota Massima/mediana; protocollo idempotente `operationId` con `planVersion` + `Mutex`; gate `WEATHER_MODE` zero-billing + tile mappa opt-in con canvas vettoriale locale; Share Card senza sagoma 2D + `allowBackup="false"`.
- **Indipendenza:** Epic 1 è route-only; Epic 2 utilizza fixture deterministiche (`WEATHER_MODE=SIMULATED`); Epic 3 completa quota, refresh, Share Card e (dopo gate Story 2.2) integrazione reale zero-cost.
- **Gate operativi residui (non bloccanti per l'avvio dello sprint):** verifica documentata servizi reali prima di attivare `WEATHER_MODE=REAL_ZERO_COST` in Story 3.5; validazione prestazioni, accessibilità e usabilità con utenti/dispositivi reali prima del rilascio pubblico.
- **Stato:** approvato per l'avvio dell'implementazione MVP Free.
