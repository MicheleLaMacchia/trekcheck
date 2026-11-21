---
title: TrekCheck — esperienza MVP Free
status: draft
created: 2026-11-21
updated: 2026-11-21
sources:
  - ../../prds/prd-TrekCheck-2026-11-20/prd.md
  - ../../briefs/brief-TrekCheck-2026-11-20/brief.md
---

# EXPERIENCE — TrekCheck

**Stato:** specifica operativa proposta per implementazione e validazione, non testata con persone. I contenuti e l'identità visiva in [DESIGN.md](DESIGN.md) e questo documento prevalgono sui [wireframe](mockups/wireframe.html). I percorsi con Giulia sono esempi, non resoconti di test.

## Foundation

Android 8+ nativo, Kotlin e Jetpack Compose / Material 3; italiano e inglese, metriche e imperiali. Supporto alla pianificazione pre-partenza senza account, GPS corrente o navigazione. Primi dati e GPX restano locali; il briefing richiede solo i campioni meteo autorizzati. Il riepilogo sopra e dettagli sotto è scelta approvata; la composizione sotto è [ASSUNZIONE] da validare.

## Information Architecture

| Superficie | Scopo / ingresso | Uscita e requisito |
|---|---|---|
| Home | Ultimi briefing e azione «Importa GPX»; vuoto se non presenti | selettore o briefing salvato; FR-1, FR-14 |
| Importazione | Selettore Android, apertura o condivisione; avanzamento / errore | anteprima o ritorno senza perdita; FR-1, FR-20 |
| Anteprima percorso | nome modificabile, mappa, estremi, distanza, dislivello/profilo disponibili; elimina con conferma | configurazione o Home; FR-2, FR-3 |
| Configurazione uscita | data, ora, fuso leggibile e modificabile, profilo e passo personalizzato | genera o route-only; FR-4–FR-8 |
| Briefing | riepilogo in alto; dati, timeline, mappa, profilo e checklist sotto; disclaimer accessibile | refresh, condividi, Home; FR-9–FR-17, FR-21 |
| Confronto refresh | prima/dopo e motivi dei dati non comparabili | briefing aggiornato o ultimo salvato; FR-16 |
| Anteprima condivisione | immagine, attribuzioni, privacy e comando condivisione Android | sharesheet o briefing; FR-18 |
| Impostazioni / informazioni | lingua, unità, passo, fuso, fonti, privacy e limiti | Home / ritorno; FR-19, FR-21 |

I dettagli mappa, profilometria e timeline sono sezioni del briefing navigabili, non obbligano a schermate separate. I wireframe esemplificano Home/importazione, anteprima, configurazione, briefing, refresh e condivisione; impostazioni, errori e dettagli puntuali restano descritti qui.

## Voice and Tone

Italiano semplice, neutro e prudente; inglese con equivalenti verificati. Esempi: «Arrivo stimato», «Meteo aggiornato alle 10:30», «Previsioni non disponibili per questo punto», «Potrai riprovare». Non dire «sicuro», «nessun rischio» o «devi partire». L'avviso descrive motivo e prossima azione, senza termini tecnici del fornitore.

## Component Patterns

- **Azione primaria** usa {components.primary-action}, conferma un'azione per volta; durante la generazione il progresso è annunciato e impedisce duplicati.
- **Riepilogo** usa {components.summary-card}; sequenza proposta: percorso/distanza, partenza/fuso, durata e arrivo stimati, meteo/freschezza, luce, stato dei dati. Segnalazioni e checklist seguono nel flusso.
- **Timeline** mostra ordine, distanza, ETA nel fuso percorso, quota/meteo se disponibili; selezione collega la mappa senza renderla indispensabile.
- **Quota** prima di una generazione con meteo: mostra unità rimaste e che solo una generazione riuscita consuma quota; se la copertura parziale è ambigua non promettere il consumo: il criterio è aperto e blocca l'implementazione della relativa UX.
- **Condivisione** mostra anteprima effettiva e contenuto incluso; coordinate precise, contatti e note esclusi per default. Solo azione esplicita apre Android Sharesheet.

## State Patterns

| Stato | Comportamento e contenuto visibile |
|---|---|
| Home vuota | spiegazione breve e «Importa GPX»; nessun accesso o profilo richiesto |
| GPX non valido, troppo grande, quota assente | errore e selezione di altro file; assenza quota è limite non errore |
| Nessuna mappa | dettaglio del percorso e metriche rimangono visibili; attribuzione quando pertinente |
| Orario DST ambiguo/inesistente | spiegazione e richiesta di scelta o modifica prima della generazione |
| Elaborazione | progresso accessibile; stato di errore e ritorno senza perdita delle scelte |
| Fuori orizzonte | mostra quando meteo non è disponibile e permette route-only; nessuna chiamata meteo |
| Parziale | identificare ogni campo/punto non disponibile, fonte, freschezza e risoluzione; non inventare |
| Offline | mostrare «Dati salvati», data aggiornamento e contenuti disponibili; nessuna promessa di mappa offline |
| Refresh fallito | conservare e mostrare briefing precedente, indicare fallimento e possibilità di riprovare |
| Quota esaurita | spiegare limite e mantenere consultazione, route-only e condivisione; non simulare acquisti |
| Export fallito | messaggio e opzione di riprovare senza cancellare il briefing |

## Interaction Primitives

Selettore di sistema, controlli Material per input, data e ora, focus e indicazioni di errore esplicite. Confermare solo azioni distruttive come eliminare; indietro conserva il piano quando possibile. Quando si condividono o inviano campioni meteo, comunicare cosa lascia il dispositivo. L'invio non richiede nome percorso o GPX completo.

## Accessibility Floor

Target almeno 48 × 48 dp; ordine di focus e titoli coerenti; TalkBack annuncia progresso, errori e aggiornamenti; etichette accessibili per icone, punti mappa e grafici; equivalenti testuali a mappa e grafico; contrasto secondo DESIGN; ingrandimento testo senza troncamenti; non usare solo colore o movimento. Ridurre animazioni secondo preferenze sistema. Verifica su device ed eventuali adeguamenti restano da svolgere.

## Responsive & Platform

Viewport variabili, portrait e landscape con contenuto scrollabile; supporto tastiera e switch access con focus visibile. Non chiedere permessi di localizzazione o notifiche nel Free. Verificare comportamento del selettore file e condivisione su API 26 e versioni recenti.

## Key Flows

**UJ-1 — Preparare un'uscita.** Giulia importa un GPX, controlla distanza e quota, imposta partenza/fuso/passo, vede la quota disponibile e avvia la generazione. Momento di valore: il briefing associa orari stimati e meteo lungo la traccia, con età e limiti. In assenza di quota o meteo, visualizza la parte disponibile e il motivo.

**UJ-2 — Consultare e aggiornare.** Giulia riapre offline un briefing datato. Con rete e quota richiede refresh; vede differenze confrontabili. Il fallimento mantiene la versione precedente. Momento di valore: capire che cosa è cambiato, senza perdere il piano.

**UJ-3 — Condividere.** Giulia apre l'anteprima della scheda con informazioni già presenti, controlla i campi e attiva la condivisione. Momento di valore: invia il riepilogo senza esporre coordinate precise o dati personali di default.

## Decisioni aperte

Verificare con persone reali leggibilità del riepilogo e delle differenze, design e copy IT/EN, scala dei profili e posizionamento effettivo dei dettagli. Definire il criterio di successo meteo parziale prima di specificare la conferma del consumo quota. Termini, attribuzioni, disponibilità e costo zero dei fornitori rimangono da verificare prima dell'integrazione.
