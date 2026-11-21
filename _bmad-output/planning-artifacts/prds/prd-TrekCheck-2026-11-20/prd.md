---
title: TrekCheck — PRD MVP Free
status: final
created: 2026-11-20
updated: 2026-11-21
---

# PRD — TrekCheck MVP Free

## 1. Scopo e visione

Questo PRD approvato traduce il brief approvato e la baseline MVP in requisiti verificabili per progettazione, sviluppo e verifica. TrekCheck aiuta a preparare un'escursione collegando una traccia GPX ai tempi stimati e alle previsioni nei punti del percorso. L'app fornisce un briefing sintetico, approfondibile e consultabile offline. Non certifica che il percorso sia sicuro.

**Destinatari ipotizzati:** persone che organizzano escursioni, anche senza competenze tecniche, e dispongono già di una traccia GPX. L'effettiva utilità per questo pubblico è da verificare con utenti reali.

## 2. Percorsi d'uso da validare

- **UJ-1 — Preparare un'uscita:** [IPOTESI] Giulia seleziona un GPX, controlla percorso e dislivello, imposta partenza e passo e riceve un riepilogo con meteo stimato, luce e checklist. Se la quota manca, vede le informazioni disponibili e il relativo limite.
- **UJ-2 — Consultare e aggiornare:** [IPOTESI] Giulia riapre offline un briefing salvato. Quando torna online, chiede un aggiornamento e confronta le variazioni. Se il servizio fallisce, trova ancora il briefing precedente.
- **UJ-3 — Condividere:** [IPOTESI] Giulia controlla l'anteprima della Share Card e la condivide con una persona fidata, senza includere coordinate precise o dati personali per impostazione predefinita.

Questi percorsi illustrano le baseline e non rappresentano interviste o test già svolti.

## 3. Glossario

- **Percorso:** geometria e metriche importate dal GPX; può contenere più segmenti.
- **Piano:** percorso, partenza, fuso e profilo di passo scelti.
- **ETA:** orario di passaggio o arrivo stimato, non garantito.
- **Campione meteo:** punto rappresentativo del percorso associato a una ETA e ai dati di previsione disponibili.
- **Briefing / TrekCheck:** riepilogo e dettagli del piano, con eventuali previsioni e condizioni.
- **Generazione meteo riuscita:** generazione o refresh con dati meteo completati e salvati con successo; consuma una unità Free.
- **Snapshot:** insieme salvato di previsioni utilizzato dal briefing e dal confronto dopo refresh.
- **Route-only:** briefing con dati del percorso disponibili senza previsione meteo.

## 4. Requisiti funzionali

Ogni requisito descrive il comportamento atteso per l'MVP Free. Le soglie non indicate come decisioni sono ipotesi iniziali da validare.

### Importazione e analisi del percorso — UJ-1

**FR-1 — Importazione GPX.** L'utente può importare localmente file GPX 1.0/1.1 tramite selettore, apertura o condivisione. Accettare tracce e route e preservare segmenti separati; almeno due punti validi. File malformati, coordinate invalide, file oltre 20 MB o oltre 200.000 punti ricevono un errore comprensibile senza bloccare l'app. File privi di quota o timestamp restano utilizzabili. [IPOTESI: limiti iniziali dalla baseline.]

**FR-2 — Metriche.** L'app calcola distanza, estremi e, se disponibili, quote e dislivelli stimati. Senza quota mostra la distanza e segnala che profilo e dislivello non sono disponibili; non inventa valori.

**FR-3 — Anteprima.** L'utente visualizza traccia, punti iniziale e finale, eventuale punto più alto, metriche e profilo altimetrico disponibile; può rinominare o eliminare il percorso. Se la mappa non è raggiungibile, metriche e funzioni locali restano accessibili.

### Preparazione e stima — UJ-1

**FR-4 — Piano.** L'utente sceglie data e ora di partenza, fuso geografico del percorso modificabile e un profilo di passo, incluso uno personalizzabile. Orari storici presenti nel GPX non sostituiscono la partenza scelta; il profilo scelto influisce sulla stima e gli orari sono mostrati nel fuso del percorso. Gli orari ambigui o inesistenti al cambio d’ora devono essere segnalati e risolti esplicitamente prima del calcolo.

**FR-5 — ETA.** L'app stima tempi di passaggio e arrivo usando distanza, dislivello disponibile e profilo di passo. Se manca la quota usa una stima basata sui dati disponibili, segnalandone il limite. Tutti gli orari derivati sono identificati come stime. La precisione va verificata con tracce e tempi reali prima di qualsiasi dichiarazione quantitativa.

### Meteo, luce e briefing — UJ-1

**FR-6 — Punti rappresentativi.** L'app seleziona campioni lungo il percorso, includendo partenza, arrivo e punto più alto quando disponibili, evitando duplicati e rispettando il massimo iniziale di 12 punti; se punti chiave coincidono, non si inventano punti distinti. [IPOTESI: campionamento iniziale dalla baseline da verificare.]

**FR-7 — Previsioni.** Per ogni campione disponibile, l'app associa la previsione all'ETA e mostra fonte, aggiornamento e risoluzione. Indica dati mancanti o parziali; distingue valori istantanei e precipitazioni riferite a intervalli. Non presuppone che tutte le previsioni siano orarie. I valori istantanei devono essere sufficientemente vicini all’ETA secondo tolleranze da definire; per le precipitazioni il periodo deve contenere l’ETA, altrimenti il dato è indicato come non disponibile.

**FR-8 — Disponibilità meteo.** Se l'uscita è fuori dall'orizzonte del fornitore, l'app non invia richieste meteo, spiega il limite e offre i dati route-only. Se un servizio non risponde o limita le richieste, informa l'utente e preserva ciò che è già disponibile.

**FR-9 — Luce.** L'app calcola localmente le informazioni di luce disponibili e mostra la relazione tra arrivo stimato e tramonto, senza bloccare l'utente o dichiarare il percorso sicuro.

**FR-10 — Briefing.** Il riepilogo in alto mostra percorso, durata e arrivo stimati, meteo e luce disponibili; sotto sono accessibili mappa, profilo, dettagli e timeline. Ogni dato indica quando è una stima o non è disponibile.

**FR-11 — Timeline e mappa.** L'utente consulta punti rappresentativi con distanza, ETA, quota e meteo quando disponibili; la selezione collega i dettagli al punto sulla mappa. Non è navigazione o tracciamento in tempo reale.

**FR-12 — Condizioni.** Regole deterministiche e versionate producono segnalazioni comprensibili associate ai dati disponibili e, quando possibile, al punto e all'orario; assenza di segnalazioni non equivale a sicurezza garantita.

**FR-13 — Checklist.** L'app genera suggerimenti coerenti con le condizioni calcolate e ne mostra il motivo. Non prescrive trattamenti, quantità cliniche o decisioni di sicurezza.

### Salvataggio, aggiornamento e condivisione — UJ-2, UJ-3

**FR-14 — Salvataggio locale.** Percorsi, piani, briefing e snapshot necessari sono conservati sul dispositivo fino alla cancellazione richiesta dall'utente. La geometria completa resta locale.

**FR-15 — Offline.** Senza rete l'utente può riaprire briefing salvati, metriche, ETA, timeline, condizioni e checklist con i dati precedentemente disponibili. Profilo altimetrico e previsioni già salvate restano consultabili; i dati non salvati sono segnalati come non disponibili. La disponibilità di tutte le mappe offline non è garantita e va indicata.

**FR-16 — Refresh manuale.** Con rete, quota e previsione disponibili, l'utente può aggiornare e confrontare lo snapshot precedente. Le precipitazioni vengono confrontate solo su intervalli equivalenti; in caso contrario l'app spiega che il confronto non è disponibile. Il nuovo snapshot sostituisce quello attuale solo dopo successo e salvataggio; il fallimento conserva il precedente.

**FR-17 — Quota Free.** [IPOTESI APPROVATA DA VALIDARE] La prima generazione meteo riuscita e ogni refresh meteo riuscito consumano una delle tre unità Free complessive per installazione. Fallimenti, route-only, consultazione offline e condivisione non consumano unità. Eliminare un briefing non ripristina quota; una reinstallazione può azzerare il conteggio locale. A quota esaurita non si inviano richieste meteo, ma restano disponibili route-only e contenuti locali.

**FR-18 — Share Card.** L'utente può vedere un'anteprima e condividere un'immagine generata localmente con riepilogo disponibile, lingua e unità selezionate, fino a tre segnalazioni e attribuzioni richieste. L'anteprima corrisponde all'export; coordinate precise, contatti, note e GPX non sono inclusi per default. La condivisione non consuma quota e funziona offline con dati salvati.

**FR-19 — Preferenze.** L'utente può selezionare italiano o inglese, unità metriche o imperiali, passo predefinito e preferenza di fuso; può consultare fonti, informativa privacy e limiti dell'app.

**FR-20 — Errori.** File, rete, provider, limite Free, fuso, mappa ed export presentano messaggi comprensibili e un'azione appropriata quando disponibile, senza cancellare dati validi esistenti.

**FR-21 — Comunicazione dei limiti.** Le informazioni sull'app spiegano in modo accessibile che TrekCheck supporta la pianificazione, non garantisce la sicurezza, non sostituisce fonti ufficiali o giudizio personale e non offre consulenza sanitaria o servizi di guida turistica. I testi finali e gli obblighi applicabili saranno esaminati prima della pubblicazione.

## 5. Requisiti trasversali

- **NFR-1 — Privacy:** nessun account o permesso di posizione corrente richiesto per il briefing; il GPX completo resta sul dispositivo. Al servizio meteo sono inviati solo campioni necessari, quota se disponibile e orario, senza nome percorso o identità. Telemetria minima o assente; nessun dato sensibile negli eventuali eventi.
- **NFR-2 — Sicurezza e affidabilità:** importazione XML protetta; input e dimensioni verificati; errori e dati parziali riconoscibili; dati precedenti mantenuti in caso di errore di refresh.
- **NFR-3 — Costo:** soluzioni gratuite e spesa zero come vincolo utente. Verificare preventivamente condizioni, quote e assenza di addebiti per ogni servizio, gateway incluso; non configurare piani o operazioni a pagamento. La sola esistenza di un piano gratuito non prova costo zero a qualsiasi volume.
- **NFR-4 — Piattaforma e accessibilità:** Android 8 / API 26 minimo; italiano e inglese; unità metriche e imperiali; contenuti leggibili con dimensioni di testo maggiorate e informazioni comprensibili senza affidarsi al solo colore. La copertura effettiva va verificata.
- **NFR-5 — Prestazioni:** [OBIETTIVI DA VALIDARE] importazione 50.000 punti sotto 2 s su dispositivo moderno di fascia media, analisi possibilmente sotto 1 s aggiuntivo, avvio a freddo sotto 2 s; nessun blocco dell'interfaccia. Ambiente e metodo di misura devono essere definiti.
- **NFR-6 — Tempi e fusi:** memorizzare eventi come istanti e usare il fuso del percorso per la visualizzazione; verificare casi DST e fuso del dispositivo differente.

## 6. Fuori ambito MVP

Account, sincronizzazione cloud del GPX, navigazione e tracking, location in background, AI, acquisti e Billing, monitoraggio automatico/notifiche meteo, confronto di partenze Premium, calcolo zaino e PDF Premium. Il Premium è una possibile estensione sul core Free, con prezzo non ancora deciso.

## 7. Validazione del prodotto

- **SM-1 — Valore iniziale:** [IPOTESI] misurare la quota di persone che completano un briefing dopo importazione; la baseline propone oltre il 50%, non ancora verificato. Valida FR-1–FR-10.
- **SM-2 — Ritorno:** [IPOTESI] almeno due TrekCheck in 30 giorni come indicatore proposto; verificare se è misurabile rispettando la privacy. Valida FR-14–FR-16.
- **SM-3 — Comprensione:** osservare se utenti distinguono dato stimato, aggiornamento e dato mancante e comprendono la quota prima di consumarla. Valida FR-7, FR-10, FR-17 e FR-21.
- **Contromisure:** monitorare errori meteo e importazione, abbandoni dovuti a quota, crash e dati mancanti; non aumentare uso o conversione a scapito di privacy e chiarezza. Metodo e soglie da definire senza introdurre telemetria sensibile.

## 8. Dipendenze e questioni aperte

1. MET Norway, gateway/cache e OpenFreeMap sono candidati: termini attuali, uso commerciale, disponibilità, attribuzione, limiti e spesa zero restano da verificare. MapLibre ha licenza BSD 2-Clause verificata; rispettarne i termini.
2. Confermare algoritmo e precisione ETA con tracce reali; risolvere ambiguità DST e comportamento dei dati parziali con esempi verificati.
3. Validare accessibilità, comprensione del briefing e quota Free; prezzo Premium rinviato.
4. Definire riferimenti gratuiti per supporto e privacy, e rivedere gli obblighi prima del rilascio. La quota Play citata dalla baseline non è autorizzata dal vincolo zero.
5. Identificativo `it.trekcheck.app` provvisorio; il codice esistente usa ancora minSdk 24 e va allineato a 26 in una futura implementazione.
6. Definire strategia di misurazione privacy-first, dispositivi di riferimento e criteri di test; nessuna metrica o prestazione è dichiarata già raggiunta.
7. Prima di implementare il consumo quota, definire il minimo di dati meteo che rende riuscita una generazione parziale e verificare che salvataggio e incremento del contatore avvengano coerentemente anche in caso di errore.
8. Prima dell’uso operativo dei servizi, definire limiti rigidi che impediscano addebiti e un comportamento route-only se i servizi gratuiti non sono disponibili. Assicurare attribuzioni visibili in app secondo i termini verificati.

## 9. Fonti

Brief approvato `_bmad-output/planning-artifacts/briefs/brief-TrekCheck-2026-11-20/brief.md`, relativo addendum, baseline `docs/trekcheck-product-technical-baseline-MVP.md` e `docs/trekcheck-product-technical-baseline-Premium.md`, registro brainstorming BMAD. Le decisioni successive dell'utente prevalgono su ipotesi precedenti.
