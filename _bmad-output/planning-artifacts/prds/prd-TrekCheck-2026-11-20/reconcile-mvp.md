# Riconciliazione PRD con baseline MVP

**Fonte:** `docs/trekcheck-product-technical-baseline-MVP.md`. **Destinazioni:** `prd.md` e `addendum.md` nella cartella corrente. Riconciliazione documentale: non è una verifica di implementazione o dei servizi esterni. Le scelte successive dell'utente prevalgono sulle ipotesi della baseline.

## Elementi principali recepiti

Il PRD copre import GPX locale, segmenti, limiti indicativi, metriche, configurazione del piano, ETA stimata, campionamento meteo, luce, briefing, checklist, offline, refresh, quota Free, Share Card, privacy e vincolo di spesa zero. L'addendum conserva vincoli di storage, UTC e cache. Il minimo Android 26 deriva dalla scelta successiva dell'utente. I provider sono correttamente candidati, ancora da verificare; non è dimostrato un costo zero illimitato.

## Gap e precisazioni da valutare

1. **Comportamento su copertura meteo parziale e quota.** FR-7 riconosce dati parziali e FR-17 consuma una quota per una generazione «riuscita», ma il PRD non definisce il minimo meteo necessario perché una risposta parziale sia considerata riuscita. La baseline distingue stati parziali, persistenza e consumo dopo successo, senza un criterio sintetico univoco per tutte le combinazioni. Definire un criterio di successo e testarlo prima dell'implementazione; non assumere che una risposta parziale consumi o non consumi sempre quota.
2. **Matching temporale.** FR-7 richiede l'associazione all'ETA e distingue intervalli, ma non precisa il trattamento dei valori lontani dall'ETA: la baseline prevede un timestamp vicino entro tolleranza coerente con la risoluzione, un periodo che contenga l'ETA per precipitazione/simbolo e nessuna interpolazione non definita. Riportare queste regole nel requisito o come vincolo di architettura verificabile; le tolleranze restano da definire e validare.
3. **Campionamento e priorità dei punti.** FR-6 specifica punti essenziali e massimo 12, mentre la baseline propone anche intervalli iniziali 60 minuti o 5 km e minimo 3, con deduplicazione e priorità. Sono parametri candidati, non promesse definitive: mantenere la loro natura di ipotesi e chiarire come trattare coincidenze e limite massimo.
4. **Freschezza e granularità.** Il PRD richiede fonte, età e risoluzione, ma non menziona esplicitamente la proposta di baseline di spiegare il diverso grado di affidabilità in base a risoluzione e freschezza. Valutare se formulare una spiegazione qualitativa senza percentuali inventate; soglie e label della baseline sono configurazioni candidate da verificare.
5. **Sicurezza GPX e attribuzioni.** NFR-2 parla di XML protetto senza esplicitare disabilitazione di entità esterne/DTD remoti; il PRD richiede attribuzioni nella Share Card ma non enuncia esplicitamente la loro visibilità nella mappa e nel briefing. Aggiungere condizioni verificabili, soggette ai termini attuali dei provider.
6. **Continuità offline.** FR-15 elenca i contenuti riapribili ma omette esplicitamente profilo altimetrico e previsioni già scaricate, entrambi nella baseline. Precisare che devono essere disponibili se salvati e segnalare dati non salvati, senza promettere mappe complete offline.
7. **Trasparenza dei profili e del fuso.** FR-4 richiede profili selezionabili e fuso modificabile, ma non precisa che il profilo deve incidere effettivamente sul calcolo né che il fuso mostrato è quello del percorso. Sono criteri comportamentali utili per i test; i valori numerici proposti restano da calibrare.

## Nota sul perimetro

Dettagli interni come formato geometria, cache grezza senza ETA, limite 12 e regole provider possono restare nell'addendum e nell'architettura; il PRD non deve duplicare l'intera baseline. Non sono state verificate le condizioni attuali di MET Norway e OpenFreeMap. Il PRD resta soggetto alla revisione e alle decisioni dell'utente.
