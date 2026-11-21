# Addendum — note per architettura e verifica

- Vincoli utente: nessuna spesa e nessuna soluzione a pagamento; nessun merge su main senza istruzione esplicita. minSdk 26 è deciso, ma non ancora implementato.
- Candidati soggetti a verifica: MET Norway, gateway con cache, MapLibre e OpenFreeMap. La baseline contiene dettagli tecnici, limiti e attribuzioni da rivalutare rispetto ai termini attuali.
- Geometria completa in storage privato versionato, non una riga database per punto; geometria semplificata solo per rendering. Gateway UTC-only; chiave cache grezza normalizzata e indipendente dall'ETA.
- Per la quota Free persistente, incrementare solo dopo generazione e salvataggio riusciti; cancellare un briefing non riduce il conteggio. La quota è ipotesi da validare.
- Prove previste: GPX validi e malformati, multi-segmento, quote mancanti, limite punti/file, DST, ETA, forecast parziale/fuori orizzonte, quota, diff precipitazioni comparabili, errori, offline, esportazione e accessibilità. La definizione di dispositivi e tolleranze resta aperta.
- I percorsi utente nel PRD sono esempi illustrativi, non testimonianze; misure, soglie e tempi indicati come obiettivi non sono risultati.
