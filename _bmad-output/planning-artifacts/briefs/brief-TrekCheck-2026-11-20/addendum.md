# Addendum — decisioni e approfondimenti per le fasi successive

## Decisioni dell'utente

- Android 8 / minSdk 26; allineamento del codice ancora da fare.
- Riepilogo in alto, dettagli sotto; chiarire scopo e limiti dell'app in modo accessibile.
- ETA deterministica basata su distanza, dislivello disponibile e profilo di passo; validazione su tracce reali prima di dichiararne precisione.
- Candidati iniziali: MET Norway, gateway con cache, MapLibre e OpenFreeMap; la verifica di condizioni e costo zero resta aperta.
- Solo soluzioni gratuite, a spesa zero. Nessuna quota di pubblicazione è autorizzata.
- Tre generazioni meteo Free riuscite e Premium a pagamento sono ipotesi da validare; prezzo rinviato.
- Identificativo `it.trekcheck.app` provvisorio; supporto e privacy con soluzioni gratuite da individuare.

## Aspetti da portare in PRD e architettura

- **GPX:** conservare i segmenti e la geometria locale versionata; evitare una riga Room per punto.
- **Tempi:** usare istanti UTC con fuso della traccia distinto e verificare i cambi ora.
- **Meteo e cache:** non richiedere previsioni fuori dal loro orizzonte; normalizzare la cache del provider e distinguerla dal matching ETA. Mostrare origine, freschezza e risoluzione delle previsioni, senza confrontare precipitazioni su intervalli non equivalenti.
- **Quota e refresh:** incrementare il conteggio delle generazioni Free consumate solo dopo una generazione meteo riuscita e conservare il briefing precedente se il refresh fallisce.

Il Premium riusa il core Free; Weather Watch è opt-in e best effort, non polling continuo. Scheda Premium generata localmente con dati personali esclusi per default. I dettagli completi e i criteri di accettazione sono nelle baseline, che restano la fonte per il rispettivo ambito; le decisioni successive dell'utente prevalgono in caso di conflitto.

## Ipotesi da verificare

Persona iniziale, esperienza effettiva del riepilogo, prestazioni, efficacia del limite Free, interesse Premium, condizioni dei servizi, quali indicatori misurare e come raccogliere i dati rispettando la privacy. Le percentuali e le ore riportate nei documenti sono ipotesi e non risultati.
