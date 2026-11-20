---
title: TrekCheck — Brief di prodotto MVP Free
status: draft
created: 2026-11-20
updated: 2026-11-20
---

# TrekCheck — Brief di prodotto

## Scopo e problema

Chi prepara un'escursione con una traccia GPX deve spesso combinare manualmente percorso, tempi di percorrenza, previsioni meteo e luce disponibile. TrekCheck intende riunire queste informazioni in un briefing comprensibile prima della partenza, associando le previsioni agli orari stimati di passaggio lungo il percorso. L'utilità e la comprensibilità di questa esperienza sono da validare con utenti reali.

Il pubblico iniziale ipotizzato è composto da persone che organizzano escursioni giornaliere e possiedono già una traccia GPX, anche senza competenze tecniche. Piattaforma iniziale: Android nativo, in italiano e inglese.

## Esperienza proposta

L'utente importa un GPX, controlla traccia e metriche, sceglie data, ora, fuso e profilo di passo, quindi genera un TrekCheck. Un riepilogo in alto presenta distanza, durata e arrivo stimati, meteo e luce; mappa, profilo altimetrico e dettagli lungo il percorso sono disponibili sotto. La checklist presenta suggerimenti basati sulle condizioni rilevate. Il briefing salvato può essere riaperto offline e condiviso come immagine. Le previsioni possono essere aggiornate manualmente quando disponibili ed entro la quota Free.

I tempi sono stime deterministiche basate su distanza, dislivello disponibile e profilo di passo; la loro precisione va verificata con tracce reali. I dati mancanti, l'età delle previsioni e la loro copertura devono essere visibili. In caso di errore di aggiornamento va conservato l'ultimo briefing valido.

## Perimetro e vincoli

- MVP Free: importazione e analisi GPX locale, mappa e metriche, ETA, meteo lungo la traccia, luce, condizioni e checklist, salvataggio offline, confronto dopo refresh e Share Card generata localmente.
- Privacy: nessun account o permesso per la posizione corrente necessario per generare il briefing; il GPX completo resta sul dispositivo e il servizio meteo riceve solo i campioni necessari. Nessun tracciamento in background, navigazione, AI o Billing nell'MVP.
- Android 8.0 (`minSdk 26`) come minimo concordato. Il progetto usa ancora 24 e dovrà essere allineato in implementazione.
- Usare esclusivamente soluzioni gratuite a spesa zero. MET Norway tramite gateway e cache e MapLibre con OpenFreeMap sono candidati iniziali; condizioni aggiornate, limiti, disponibilità e costo dell'intera soluzione devono essere verificati prima dell'adozione operativa. Una quota gratuita non equivale a capacità illimitata.
- `it.trekcheck.app` è l'identificativo provvisorio; riferimenti di supporto e privacy gratuiti da scegliere prima del rilascio. La quota di registrazione Google Play menzionata nelle baseline non è autorizzata dal vincolo di spesa zero; distribuzione di prova tramite GitHub da valutare.

## Confine Free e sviluppi futuri

Ipotesi da validare: tre generazioni meteo Free riuscite, inclusi i refresh riusciti; errori non consumano quota e i briefing già salvati rimangono consultabili. Un Premium a pagamento è previsto come estensione successiva del core, con generazioni senza quota applicativa, monitoraggio meteo, confronto partenze, peso zaino e scheda PDF. Il prezzo non è deciso. «Senza quota applicativa» non annulla i limiti dei servizi esterni.

## Fiducia e sicurezza

TrekCheck supporta la pianificazione e non garantisce la sicurezza di un percorso. Tempi e previsioni possono essere incompleti o cambiare; l'app non sostituisce fonti ufficiali, cartografia, autorità o giudizio personale, e non offre consulenza sanitaria o servizi di guida turistica. Questi limiti devono essere chiari e accessibili; contenuti, funzionamento e obblighi applicabili andranno esaminati prima di un rilascio pubblico.

## Validazione e questioni aperte

Verificare con utenti reali che riescano a generare e comprendere il briefing, che tornino a usarlo e che il limite Free sia comprensibile. Le baseline propongono come indicatori la generazione di due TrekCheck in 30 giorni e il passaggio da importazione a briefing; sono obiettivi ipotetici, non risultati misurati. Occorre definire come raccogliere evidenze rispettando la scelta di telemetria minima e senza dati sensibili.

Restano da verificare: condizioni e costi effettivi dei servizi; precisione dell'ETA; accessibilità del riepilogo; riferimenti gratuiti per supporto e privacy; sostenibilità della quota Free e interesse per Premium. Le stime di tempo e di costo contenute nelle baseline non sono impegni.

## Fonti

- `docs/trekcheck-product-technical-baseline-MVP.md`
- `docs/trekcheck-product-technical-baseline-Premium.md`
- `_bmad-output/brainstorming/brainstorm-trekcheck-2026-11-20/.memlog.md`
