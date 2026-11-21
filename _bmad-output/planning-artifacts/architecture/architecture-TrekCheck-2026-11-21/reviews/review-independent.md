# Revisione indipendente — spine TrekCheck

## Esito

Coerente come proposta iniziale con PRD MVP e codice; non pronto a implementare integrazione meteo finché le questioni sotto non sono chiuse. Le versioni di Kotlin, AGP, Gradle, BOM, compile/target e minSdk coincidono con i file di progetto; ciò non verifica compatibilità o versioni correnti online.

## Risultati

1. **Link sorgente errato:** il riferimento relativo alla baseline in `sources` risale un livello di troppo; dal percorso dello spine servono quattro `..` per raggiungere la radice del repository, non cinque.
2. **Contratto idempotente quota non ancora implementabile:** AD-5 fissa l'invariante ma rinvia il protocollo fra DataStore e storage. Due implementazioni conformi potrebbero differire nell'ordine di commit, nella gestione delle operazioni pendenti o dei duplicati dopo crash. Il documento lo indica correttamente come blocco, che deve comparire come dipendenza esplicita prima delle storie di refresh/generazione meteo, non essere interpretato come dettaglio opzionale.
3. **Proprietà dei dati di risposta meteo:** AD-4 fissa ID, orari, validità e freschezza ma non definisce forma concreta di risposta per dati parziali, per-ID, fallimento globale e unità; la tolleranza resta correttamente aperta. Un contratto di interfaccia con fixture deve precedere sviluppi paralleli client/gateway, anche se il provider reale resta simulato.
4. **Gate di costo:** AD-7 prescrive zero spesa ma non fornisce un meccanismo concreto di blocco verificabile per un fornitore con addebiti, e alcune quote gratuite non offrono limiti di spesa rigidi. Va esplicitato che una soluzione senza un meccanismo provabile di blocco prima di ogni addebito non può essere attivata; cache e monitoraggio non sono garanzie.
5. **Privacy mappe:** il divieto di invio del GPX completo non descrive i dati impliciti nelle richieste dei tile (aree della mappa e indirizzo IP al fornitore). Privacy e termini del fornitore mappa devono essere verificati e comunicati prima di abilitarlo; usare fallback senza tile se non verificato.
6. **Backup Android:** reinstallazione e ripristino backup potrebbero reintrodurre contatore e dati a differenza delle ipotesi. Decidere separatamente inclusione/esclusione di file, preferenze, quota e operazioni pendenti nei backup per non rompere privacy e consistenza. Nessuna scelta di backup è implicita nella sola memorizzazione privata.
7. **Sorgente previsioni:** per route e snapshot va vincolata una politica di propagazione fra modifica piano, cancellazione, refresh in volo e cambio ETA, altrimenti una risposta vecchia può aggiornare un briefing con piano cambiato. Il coordinator deve verificare versione del piano prima di promuovere lo snapshot e mantenere dati precedenti se obsoleti.

Questa è una revisione documentale, senza installazioni, chiamate a fornitori o test di implementazione.
