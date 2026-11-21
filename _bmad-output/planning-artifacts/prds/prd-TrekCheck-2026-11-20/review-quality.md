# Revisione qualità — PRD TrekCheck MVP Free

## Valutazione complessiva

Il documento descrive con chiarezza obiettivo, perimetro, preferenze dell'utente e dipendenze non ancora verificate. È una base utile per la progettazione, ma alcuni comportamenti e criteri di accettazione richiedono precisazioni prima di creare storie implementabili; l'approvazione dell'utente riguarda la bozza, non dimostra fattibilità, gratuità dei servizi o accuratezza delle stime.

## Prontezza decisionale — adeguata

Le scelte chiave sono distinguibili dalle ipotesi, compresi minSdk 26, spesa zero, fornitori candidati e quota Free. I termini dei fornitori, il meccanismo che impedisca addebiti e le condizioni di fallback restano aperti.

- **[high] Quota gratuita e limite di spesa** (§5 NFR-3; §8): richiedere spesa zero non specifica cosa fare quando una quota gratuita sta per esaurirsi, il piano richiede un metodo di pagamento o una dipendenza smette di essere gratuita. *Correzione:* rinviare a progettazione e verifiche una procedura per limiti rigidi, arresto controllato e funzionamento locale; non affermare che un provider sia già autorizzato o verificato.

## Sostanza — adeguata

Il valore del meteo associato a posizione e ora è preciso; dati mancanti, accesso offline e limiti di sicurezza sono concreti. Le stime prestazionali sono etichettate come obiettivi, ma il riferimento hardware e il metodo di misura non sono definiti.

- **[medium] Accessibilità e prestazioni da rendere misurabili** (§5 NFR-4–5): «leggibili» e «nessun blocco» non indicano criteri, dispositivi o configurazioni. *Correzione:* definire in UX/storie scenari con dimensione testo, screen reader e misurazioni ripetibili; mantenere le soglie come obiettivi fino a validazione.

## Coerenza strategica — adeguata

Il nucleo Free, l'assenza di account e il Premium differito sostengono il caso d'uso pre-partenza. Le metriche sono marcate come ipotesi e affiancate da contromisure.

- **[medium] Raccolta delle metriche non risolta** (§7): misurare ritorno e completamento potrebbe richiedere identificatori persistenti, mentre telemetria minima/assente è un vincolo. *Correzione:* specificare come verificare tramite ricerca volontaria o strumenti compatibili con la privacy prima di introdurre raccolta dati.

## Chiarezza del completamento — carente

Molti requisiti includono condizioni verificabili, tra cui quote, aggiornamento fallito e precipitazioni non comparabili. Alcuni comportamenti critici rimangono troppo generici per essere implementati o testati univocamente.

- **[high] Stato meteo parziale e quota** (§4 FR-7, FR-16–17): non è definito quando una risposta meteo parziale conti come generazione riuscita o cosa si verifichi se il salvataggio e l'incremento del contatore vengono interrotti. *Correzione:* prima dell'implementazione definire soglia minima di riuscita, transazione/idempotenza e casi di test; non consumare quota su fallimento.
- **[high] Orari ambigui o inesistenti** (§4 FR-4, §5 NFR-6): «gestiti chiaramente» non specifica cosa succeda se un orario locale si ripete o non esiste per cambio d'ora. *Correzione:* definire comportamento di selezione/validazione e test dei due casi prima delle storie relative.
- **[medium] Casi senza alba/tramonto** (§4 FR-9): il comportamento ad alte latitudini, con evento solare assente o dati insufficienti, non è stabilito. *Correzione:* definire uno stato non disponibile esplicito senza inventare orari e testarlo.
- **[medium] Campioni coincidenti o dati mancanti** (§4 FR-6–7): il massimo è definito, ma non come gestire percorsi molto brevi o in cui partenza, arrivo e punto più alto coincidono. *Correzione:* precisare deduplicazione, minimo effettivo e fallback senza dati inventati.
- **[medium] Esportazione e coordinate** (§4 FR-18): «coordinate precise ... non incluse» non chiarisce se una miniatura della traccia possa rivelare luoghi anche senza coordinate testuali. *Correzione:* definire contenuti grafici e criteri di privacy della card prima del design.

## Trasparenza del perimetro — adeguata

Le esclusioni MVP e la separazione Premium sono esplicite; l'effetto delle decisioni dell'utente sulle versioni precedenti è tracciato. I criteri e le soglie proposte non sono presentati come risultati.

- **[medium] Limiti GPX** (§4 FR-1): 20 MB e 200.000 punti sono etichettati come ipotesi ma il comportamento ai limiti è formulato come definitivo. *Correzione:* indicare che sono soglie provvisorie da confermare e testare esattamente a limite e oltre.

## Usabilità a valle — adeguata

FR-1–21, UJ-1–3 e SM-1–3 sono unici e ordinati; il glossario contiene i concetti principali. Il documento collega i gruppi di requisiti ai percorsi e le metriche ai requisiti, facilitando UX e storie.

- **[low] Indice delle ipotesi** (§8): le ipotesi sono sparse in percorsi, FR e metriche e non sono tutte indicizzate con responsabilità e momento della verifica. *Correzione:* raccogliere nell'elenco aperto i singoli punti con soggetto e condizione di riesame prima di finalizzare la pianificazione.

## Forma — adeguata

Tre percorsi illustrativi, esplicitamente non derivati da interviste, sono appropriati a un'app consumer; il livello di dettaglio sostiene le successive fasi UX e architettura. Il testo non spaccia un'ipotesi di persona per una ricerca condotta.

## Note meccaniche

- I riferimenti FR-1–FR-21, UJ-1–UJ-3 e SM-1–SM-3 sono continui; i gruppi di requisiti citano i percorsi e le metriche citano i requisiti.
- La terminologia è comprensibile; «briefing / TrekCheck» è definito, mentre «quota» può riferirsi sia a quota altimetrica sia a limite Free: chiarire il contesto quando serve.
- Le ipotesi sono marcate con espressioni italiane, ma non esiste un indice completo con rimandi puntuali. Nessun elemento di questa revisione certifica l'esecuzione di test, l'idoneità legale o i termini correnti dei fornitori.
