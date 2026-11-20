# TrekCheck — Product & Technical Baseline — MVP Free Tier

**Versione:** MVP 1.0  
**Data:** 2026-10-13  
**Stato:** source of truth per implementazione del solo MVP Free Tier tramite sviluppo agentico  
**Piattaforma target iniziale:** Android / Google Play  
**Tecnologia target:** Kotlin nativo + Jetpack Compose  
**Mercato iniziale:** Italia + internazionale (UI almeno IT/EN)  
**Nome prodotto canonico:** TrekCheck  
**Motto ufficiale EN:** “Run a TrekCheck before you go.”  
**Motto ufficiale IT:** “Fai un TrekCheck prima di partire.”  

---

# 1. Executive summary

TrekCheck è un'app Android focalizzata sulla **preparazione prima di un'escursione**, non sulla navigazione durante l'attività.

> **TrekCheck**  
> **Run a TrekCheck before you go.**  
> **Fai un TrekCheck prima di partire.**

Il Free Tier deve permettere di:

> **Importare un GPX, impostare quando si parte e ottenere un briefing route-aware su percorso, ETA, meteo che si incontrerà lungo la traccia, luce residua e checklist. Il briefing resta disponibile offline, può essere aggiornato manualmente entro la quota Free e può essere condiviso tramite una Share Card essenziale generata localmente.**

TrekCheck non deve essere:

- un route planner;
- un navigatore turn-by-turn;
- un tracker GPS;
- un social network;
- un'app che richiede account;
- un sistema che carica il GPX completo su cloud proprietario;
- un sistema che usa AI generativa nel Free MVP;
- un sistema che stabilisce se un'escursione è “sicura”.

Il posizionamento è:

> **pre-hike briefing utility, GPX-first, cross-ecosystem, privacy-first e time-aware.**

## 1.1 Obiettivo di validazione

La baseline MVP deve validare soprattutto questa proposizione:

> **“Non voglio vedere solo il meteo della montagna: voglio vedere il meteo che probabilmente incontrerò quando sarò in ciascuna parte del percorso.”**

Il documento Premium è separato e non deve condizionare l'implementazione del core Free.

---

# 2. Posizionamento di prodotto

### 2.1 Problema

Un escursionista spesso prepara il percorso in un'app A, controlla il meteo in un'app B, guarda tramonto/alba in un'app C e crea mentalmente una checklist dell'equipaggiamento.

Il problema non è trovare il GPX.

Il problema è trasformare il GPX in una risposta a domande come:

- quanto durerà realisticamente l'escursione?
- a che ora arriverò nei punti più esposti?
- che temperatura troverò in quota?
- quando è più probabile incontrare pioggia?
- ci sarà vento significativo nella parte alta?
- rischio di concludere vicino al tramonto?
- quali condizioni meritano attenzione?
- cosa dovrei almeno considerare di portare?

### 2.2 Job-to-be-done

> “Ho già scelto il percorso. Prima di partire voglio capire rapidamente cosa incontrerò e cosa non devo dimenticare.”

### 2.3 Target iniziale

**Target primario**

- escursionisti giornalieri;
- trekking non tecnici;
- utenti che importano GPX da Komoot, Wikiloc, Garmin, AllTrails, Strava o siti web;
- utenti Android 25–55 anni;
- Europa come primo mercato naturale, ma app progettata da subito per uso internazionale.

**Target secondario futuro**

- trail runner;
- ciclisti;
- bikepacker;
- fast hiker;
- backpacker multi-day;
- sci alpinismo, solo dopo un'analisi safety molto più rigorosa.

### 2.4 Non-target iniziale

L'MVP non deve essere pensato per:

- alpinismo tecnico;
- valanghe;
- navigazione di emergenza;
- ricerca e soccorso;
- escursioni dove l'app debba fornire decisioni safety-critical;
- tracking live;
- turn-by-turn navigation.

---

## 3. Competitor landscape

### 3.1 Epic Ride Weather

**Posizionamento:** meteo personalizzato lungo un percorso, soprattutto ciclismo.

**Punti forti**

- time-aware;
- integrazione con servizi GPS;
- modello commerciale già validato;
- lunga presenza sul mercato;
- indicazioni su vento, abbigliamento, nutrizione e idratazione.

**Debolezza sfruttabile**

- forte focus cycling;
- logica legata a ecosistemi/integrazioni;
- meno focalizzato sul concetto di “briefing escursionistico compatto”.

### 3.2 Komoot

Komoot mostra meteo previsto sui diversi tratti e suggerimenti contestuali.

**Punti forti**

- base utenti enorme;
- percorso già dentro l'ecosistema;
- navigazione, planning e meteo integrati.

**Debolezza sfruttabile**

- soluzione bundled;
- l'utente deve vivere nel suo ecosistema;
- costo del bundle superiore a una piccola utility dedicata;
- non è progettato come “import qualsiasi GPX → briefing”.

### 3.3 AllTrails

Offre condizioni del sentiero, temperatura, precipitazioni e altri dati in piani a pagamento.

**Punti forti**

- catalogo sentieri;
- community;
- brand;
- navigazione offline.

**Debolezza sfruttabile**

- prodotto generalista;
- forte lock-in al catalogo/servizio;
- maggiore complessità.

### 3.4 Wikiloc

Include meteo tra le funzionalità a pagamento.

**Punti forti**

- enorme catalogo e community;
- forte presenza in Europa;
- già usato da molti escursionisti.

**Debolezza sfruttabile**

- il meteo è una feature accessoria;
- minore enfasi sul briefing time-aware indipendente dal provider.

### 3.5 Trailcast

Nel 2026 sono apparse più app con questo nome.

Una variante offre:

- import GPX;
- meteo route-aware;
- ETA;
- daylight;
- field risks;
- compass;
- dati offline.

Un'altra offre:

- GPX;
- elevation profile;
- statistiche;
- meteo;
- privacy/local-first.

**Conclusione:** il concetto è già stato individuato da altri indie developer. Il time-to-market conta.

### 3.6 HikeReady

Esiste già un servizio che offre:

- import GPX;
- analisi percorso;
- meteo;
- packing list;
- meal plan;
- acquisto lifetime.

Questo è il concorrente più vicino al concept esteso.

### 3.7 Gap difendibile

Il prodotto deve essere più stretto e più chiaro:

1. **GPX-first**
2. **nessun account**
3. **nessun ecosistema proprietario**
4. **briefing time-aware lungo il percorso**
5. **privacy-first**
6. **recommandation engine deterministico e trasparente**
7. **niente AI generativa nell'MVP**
8. **niente navigation/tracking**
10. **esperienza “30 secondi per capire la giornata”**

---


# 4. Scope MVP Free

## 4.1 Definizione

L'MVP Free deve permettere all'utente di:

1. importare un GPX;
2. validarlo e analizzarlo localmente;
3. visualizzare traccia, elevation profile e metriche principali;
4. impostare data/ora di partenza e timezone del percorso;
5. scegliere un profilo di passo;
6. calcolare ETA lungo la traccia;
7. campionare il percorso e associare il forecast previsto all'ora di passaggio;
8. calcolare daylight e condition flags deterministici;
9. produrre una checklist deterministica;
10. salvare il TrekCheck localmente e riaprirlo offline;
11. aggiornare manualmente il forecast entro quota Free/orizzonte provider e vedere la diff;
12. generare una **TrekCheck Share Card Free** in immagine;
13. usare il prodotto senza account, senza background location e senza AI generativa.

La baseline MVP **non include** Billing, Weather Watch, scheduling automatico, notifiche meteo o altre capability Premium.

---

# 5. Funzionalità MVP — specifica dettagliata


## FR-MVP-001 — Import GPX

### Obiettivo

Consentire l'importazione di un file GPX da qualsiasi sorgente Android compatibile.

### Modalità supportate

- file picker Android tramite Storage Access Framework;
- “Apri con”;
- ricezione tramite Share Intent da altre app;
- file in memoria locale;
- file da provider compatibili con Android Documents Provider.

### Tipi GPX

Supportare:

- GPX 1.0;
- GPX 1.1;
- `<trk>`;
- `<trkseg>`;
- `<trkpt>`;
- `<rte>`;
- `<rtept>`.

I waypoint `<wpt>` possono essere letti ma non sono essenziali nell'MVP.

### Validazioni

Il parser deve verificare almeno:

- XML ben formato;
- presenza coordinate;
- latitude in [-90, 90];
- longitude in [-180, 180];
- file non vuoto;
- presenza di almeno 2 punti validi;
- numeri elevation validi se presenti;
- gestione corretta di NaN/Infinity;
- segmenti multipli;
- punti duplicati;
- GPX senza quota;
- GPX senza timestamp;
- GPX con timestamp storico.

### Limiti iniziali

Valori configurabili:

- dimensione massima file: **20 MB**;
- punti massimi: **200.000**;
- oltre la soglia: errore esplicativo o downsampling controllato in una fase futura.

### Security

- parser streaming;
- nessuna risoluzione di external entity;
- nessun DTD remoto;
- nessun upload del file originale al backend.

### Acceptance criteria

- file GPX valido importato senza rete;
- GPX multi-segmento non perde segmenti;
- GPX senza elevazione resta utilizzabile;
- file corrotto produce errore leggibile;
- l'app non deve crashare su XML arbitrario o oversized.

---

## FR-MVP-002 — Analisi geometrica del percorso

Dopo l'import l'app calcola localmente:

- distanza totale;
- numero segmenti;
- numero punti;
- punto iniziale;
- punto finale;
- bounding box;
- quota minima, se disponibile;
- quota massima, se disponibile;
- dislivello positivo;
- dislivello negativo;
- elevation profile;
- indice della quota massima;
- coordinate del punto più alto.

### Distanza

Calcolo con Haversine o algoritmo equivalente.

Per lo scope MVP Haversine è sufficiente.

### Dislivello

La quota GPS può essere rumorosa.

Pipeline raccomandata:

1. eliminare valori palesemente invalidi;
2. smoothing della serie elevation;
3. filtro mediano o finestra distance-based;
4. ignorare micro-variazioni sotto una soglia configurabile;
5. calcolare ascent/descent sulla serie filtrata.

Valori iniziali candidati:

- smoothing window equivalente a ~30–50 m;
- soglia micro-variazione: ~3 m.

Questi parametri devono vivere in configurazione interna/testabile, non hard-coded nella UI.

### GPX senza elevation

L'app deve:

- calcolare comunque la distanza;
- mostrare “quota non disponibile nel file”;
- disabilitare ascent/descent;
- segnalare che la stima tempo/meteo quota sarà meno precisa;
- NON chiamare un elevation API nell'MVP.

---

## FR-MVP-003 — Preview del percorso

Dopo l'import mostrare una schermata “Route Preview” con:

- nome file;
- nome traccia GPX se presente;
- mappa;
- polyline;
- start marker;
- end marker;
- highest point marker se quota disponibile;
- distanza;
- dislivello +/−;
- quota min/max;
- elevation chart.

Azioni:

- `Prepara escursione`;
- `Rinomina`;
- `Elimina`.

---

## FR-MVP-004 — Configurazione dell'uscita

Prima del briefing l'utente deve impostare:

### Data

Consentire una data compresa entro l'orizzonte disponibile del provider meteo.

Per MET Norway: previsioni fino a circa 9 giorni.

### Ora di partenza

Input locale.

### Time zone

Problema importante: l'utente può preparare un'escursione in un fuso diverso da quello del telefono.

MVP:

- default = timezone del dispositivo;
- mostrare esplicitamente il fuso;
- consentire modifica manuale del timezone da lista IANA.

Esempio:

`Europe/Rome`

Futuro:

- timezone lookup automatico offline o tramite servizio.

### Profilo passo

MVP hiking-only:

- Relaxed;
- Normal;
- Fast;
- Custom.

Il profilo deve essere un moltiplicatore dell'algoritmo ETA, non un valore cosmetico.

### Opzione custom

Possibile input:

- velocità base su terreno pianeggiante.

Range plausibile iniziale:

- 2,0–6,5 km/h.

---

## FR-MVP-005 — ETA route-aware

### Obiettivo

Stimare l'ora di passaggio lungo la traccia.

Questo è uno dei componenti centrali del prodotto.

### Algoritmo suggerito

Per l'MVP usare una funzione hiking gradient-aware, ad esempio una variante della **Tobler Hiking Function**, con:

- velocità dipendente dalla pendenza;
- cap minimo;
- cap massimo;
- moltiplicatore profilo utente.

Formula di partenza possibile:

`v = 6 * exp(-3.5 * abs(s + 0.05))`

dove `s` è la pendenza rise/run.

La formula deve essere incapsulata in un'interfaccia:

`TravelTimeEstimator`

per permettere di sostituirla senza cambiare il resto del sistema.

### Vincoli

- se manca elevation, usare velocità flat configurabile;
- mostrare sempre “tempo stimato” e non “tempo previsto con certezza”;
- non derivare il tempo dai timestamp originali del GPX salvo futura funzione esplicita;
- i timestamp presenti in un GPX storico non devono alterare automaticamente la pianificazione futura.

### Output

Per ogni punto/resample point:

- cumulative distance;
- elapsed estimate;
- ETA assoluto;
- elevation;
- slope locale.

---

## FR-MVP-006 — Campionamento meteo lungo il percorso

### Obiettivo

Non chiedere una previsione per ogni punto GPX.

Creare un set limitato di `WeatherSamplePoint`.

### Algoritmo

Il campionatore deve includere sempre:

- start;
- finish;
- highest point se distinto;
- punti intermedi.

Regola iniziale:

creare un sample almeno ogni:

- **60 minuti ETA**, oppure
- **5 km**,

scegliendo il criterio che genera più dettaglio, ma con:

- minimo 3 sample;
- massimo 12 sample nell'MVP.

In futuro il limite potrà essere aumentato.

### Motivazione

Riduce:

- carico API;
- latenza;
- traffico;
- costo futuro;
- duplicazione di forecast quasi identici.

### Coordinate

Per MET Norway:

- arrotondare lat/lon a massimo 4 decimali;
- usare elevation intera se presente.

---

## FR-MVP-007 — Recupero meteo

### Provider MVP

**MET Norway — Locationforecast 2.0 / `compact`**

Utilizzare il dataset globale. Il provider restituisce una `timeseries` in UTC e non deve essere trattato come una semplice API “meteo giornaliero”: il valore del prodotto deriva dal matching tra **ETA del percorso** e **forecast valido in quell'istante/intervallo**.

### Variabili MVP

Usare solo variabili sufficientemente disponibili globalmente:

- air temperature;
- relative humidity;
- cloud fraction;
- wind speed;
- wind direction;
- precipitation amount;
- symbol/weather condition.

Non rendere requisito globale nell'MVP:

- gust;
- probability of precipitation;
- thunder probability;
- local warnings;
- snow depth.

Questi valori non sono uniformemente disponibili su tutto il pianeta.

### Parametri della request

Per ogni punto campionato usare:

- `lat`;
- `lon`;
- `altitude` quando disponibile dal GPX.

L'altitudine deve essere passata quando nota perché migliora soprattutto la correzione della temperatura rispetto alla quota. Se il GPX non contiene quota, omettere `altitude` e marcare il sample con accuratezza quota ridotta.

Esempio raw request MET:

```http
GET /weatherapi/locationforecast/2.0/compact?lat=41.9028&lon=12.4964&altitude=62 HTTP/1.1
Host: api.met.no
User-Agent: TrekCheck/1.0 https://<REAL_PROJECT_DOMAIN>/contact
Accept: application/json
```

**Nota implementativa:** il contatto nel `User-Agent` deve essere reale e sostituito prima della produzione. Non usare placeholder come `example.com` nel deploy reale.

### Gestione rigorosa di UTC e timezone

MET Norway restituisce timestamp ISO-8601 UTC, ad esempio:

```text
2026-10-13T12:00:00Z
```

La `Z` indica UTC. Per `Europe/Rome` il 13 ottobre 2026 tale istante corrisponde alle 14:00 locali (UTC+2).

Regola architetturale:

1. l'utente imposta **data e ora locali del percorso**;
2. la configurazione del viaggio include un `ZoneId` IANA, ad esempio `Europe/Rome`;
3. il client converte `LocalDate + LocalTime + ZoneId` in `Instant`;
4. ETA e matching forecast sono calcolati internamente in UTC/`Instant`;
5. la UI riconverte gli `Instant` nel `ZoneId` del percorso;
6. non usare il timezone del device per filtrare o visualizzare il giorno del percorso, salvo quando coincide esplicitamente con quello del viaggio.

Esempio Kotlin concettuale:

```kotlin
val departureLocal = LocalDateTime.of(date, time)
val departureInstant = departureLocal
    .atZone(routeZoneId)
    .toInstant()
```

Anti-pattern vietato:

```kotlin
forecast.time.toLocalDateTimeUsingDeviceZone()
```

perché un utente può preparare in Italia un percorso in Giappone, USA, Patagonia, ecc.

### Semantica dei blocchi MET

La struttura della timeseries contiene dati con semantica diversa. Il parser deve conservarla correttamente.

#### `instant.details`

Descrive valori previsti **all'istante indicato da `time`**. Esempi:

- `air_temperature`;
- `relative_humidity`;
- `cloud_area_fraction`;
- `wind_speed`;
- `wind_from_direction`;
- `air_pressure_at_sea_level`.

#### `next_1_hours`

Descrive il periodo **successivo di un'ora**.

Esempi:

- `summary.symbol_code`;
- `details.precipitation_amount`.

`precipitation_amount` in questo blocco è quindi l'accumulo previsto nell'ora successiva, non una misura “istantanea”.

#### `next_6_hours` / `next_12_hours`

Stessa logica ma riferita all'intervallo successivo di 6 o 12 ore. Il relativo `symbol_code` è un riassunto dell'intero periodo e non deve essere mostrato come se descrivesse esattamente l'ETA del punto.

### Esempio raw response reale verificato

Estratto minimale della risposta ottenuta durante il test del 2026-10-12 per Roma:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Expires: Mon, 12 Oct 2026 15:12:07 GMT
Last-Modified: Mon, 12 Oct 2026 14:41:08 GMT
```

```json
{
  "type": "Feature",
  "geometry": {
    "type": "Point",
    "coordinates": [12.4964, 41.9028, 62]
  },
  "properties": {
    "meta": {
      "updated_at": "2026-10-12T13:17:31Z",
      "units": {
        "air_temperature": "celsius",
        "precipitation_amount": "mm",
        "relative_humidity": "%",
        "wind_speed": "m/s"
      }
    },
    "timeseries": [
      {
        "time": "2026-10-13T12:00:00Z",
        "data": {
          "instant": {
            "details": {
              "air_temperature": 20.8,
              "cloud_area_fraction": 16.4,
              "relative_humidity": 53.4,
              "wind_from_direction": 358.3,
              "wind_speed": 5.3
            }
          },
          "next_1_hours": {
            "summary": {
              "symbol_code": "fair_day"
            },
            "details": {
              "precipitation_amount": 0.0
            }
          }
        }
      }
    ]
  }
}
```

Interpretazione dell'esempio:

- temperatura 20,8 °C valida all'istante 12:00 UTC;
- per `Europe/Rome` equivale alle 14:00 locali;
- vento 5,3 m/s all'istante;
- `0.0 mm` indica precipitazione prevista nell'ora successiva;
- `fair_day` riassume l'ora successiva.

### Matching temporale

Per ciascun `WeatherSamplePoint`:

1. conoscere l'ETA in `Instant`;
2. ordinare la timeseries per timestamp;
3. determinare la risoluzione locale osservando la distanza temporale tra entry adiacenti;
4. per valori istantanei scegliere il timestamp più vicino all'ETA entro una tolleranza coerente con la risoluzione;
5. per precipitazione e condition symbol preferire il blocco di periodo che contiene l'ETA;
6. se esiste `next_1_hours`, preferirlo a `next_6_hours`;
7. usare `next_6_hours` o `next_12_hours` solo quando la granularità più fine non è disponibile;
8. conservare sempre il tipo di intervallo usato (`1h`, `6h`, `12h`) per trasparenza e confidence UI.

### Granularità del forecast e orizzonte temporale

Non assumere che l'intera previsione sia oraria. Nel test reale la risposta è risultata molto dettagliata nel breve termine e più rada andando avanti nel tempo. Questo comportamento deve essere trattato come caratteristica normale del provider.

Il parser deve quindi essere **data-driven**:

- non generare timestamp orari mancanti artificialmente;
- non interpolare simboli/precipitazioni senza una regola esplicita;
- misurare `forecastResolutionMinutes`;
- abbassare la confidence del briefing quando la previsione è temporalmente più grossolana.

Metadati suggeriti per ogni sample normalizzato:

```text
forecastIssuedAt
fetchedAt
validAt
intervalStart
intervalEnd
resolutionMinutes
periodType      // instant | 1h | 6h | 12h
provider
```

### Forecast confidence nell'MVP

Non stimare una “probabilità di correttezza”. La confidence MVP deve essere solo una valutazione trasparente della **freschezza e granularità** del dato.

Esempio:

- `HIGH`: forecast recente + risoluzione oraria + partenza ravvicinata;
- `MEDIUM`: risoluzione oraria ma orizzonte più distante, oppure dato meno fresco;
- `LOW`: risoluzione 6h/12h o viaggio vicino al limite dell'orizzonte provider.

La label deve essere accompagnata da una spiegazione, ad esempio:

> Forecast resolution: 6 hours. Refresh closer to departure for a more detailed briefing.

### Fallback

Se un singolo sample fallisce:

- non fallire l'intero briefing;
- mostrare “meteo non disponibile” per quel segmento;
- continuare con gli altri punti.

Se falliscono >50% dei sample:

- mostrare briefing route-only;
- offrire retry.

### Acceptance criteria aggiuntivi

- un timestamp MET con `Z` viene interpretato come UTC;
- il giorno locale viene calcolato tramite `ZoneId` del percorso;
- `2026-10-13T12:00:00Z` con `Europe/Rome` viene mostrato come 14:00 il 13/10/2026;
- `precipitation_amount` non viene interpretato come dato istantaneo;
- il client non presume che tutte le entry siano distanziate di 1 ora;
- la UI può mostrare la risoluzione effettiva del forecast;
- GPX in fuso diverso da quello del device produce ETA e orari locali corretti.

---

## FR-MVP-008 — Backend weather gateway

### Decisione

Per il prodotto commerciale è raccomandato un piccolo **Backend for Frontend (Weather BFF)**.

L'app non deve idealmente interrogare MET Norway direttamente in produzione. Il BFF isola il provider e impedisce che la UI Android debba conoscere dettagli come `next_1_hours`, `Expires` o il formato delle future versioni MET.

### Endpoint applicativo proposto

`POST /v1/weather/route`

Request:

```json
{
  "samples": [
    {
      "id": "sample-01",
      "lat": 42.1234,
      "lon": 13.1234,
      "altitudeM": 1800,
      "eta": "2026-10-17T09:30:00Z"
    },
    {
      "id": "sample-02",
      "lat": 42.1378,
      "lon": 13.1412,
      "altitudeM": 2230,
      "eta": "2026-10-17T11:05:00Z"
    }
  ]
}
```

Regole:

- `eta` è sempre UTC ISO-8601;
- il BFF weather è **timezone-agnostic**: non riceve né restituisce l'orario locale del percorso;
- massimo 12 sample nell'MVP;
- latitude/longitude devono essere validate server-side;
- `altitudeM` è opzionale.

### Responsabilità del backend

Il backend:

1. valida input;
2. limita numero sample;
3. normalizza coordinate e quota secondo la policy provider-specific prima sia della cache key sia della request upstream;
4. costruisce la raw-provider cache key per **provider + coordinate normalizzate + altitude bucket**, mai per ETA;
5. recupera forecast raw da cache;
6. chiama MET Norway solo quando necessario;
7. applica un `User-Agent` reale e identificativo;
8. rispetta gli header HTTP di caching;
9. normalizza la risposta MET in un DTO stabile;
10. esegue il matching ETA → forecast interval;
11. restituisce metadati di freshness/resolution;
12. non persiste il GPX completo.

### Request dal BFF a MET Norway

Esempio TypeScript concettuale:

```ts
const MET_LAT_LON_DECIMALS = 3;
const MET_ALTITUDE_BUCKET_M = 50;

const normalizedLat = roundToDecimals(sample.lat, MET_LAT_LON_DECIMALS);
const normalizedLon = roundToDecimals(sample.lon, MET_LAT_LON_DECIMALS);
const normalizedAltitudeM = sample.altitudeM == null
  ? null
  : Math.round(sample.altitudeM / MET_ALTITUDE_BUCKET_M) * MET_ALTITUDE_BUCKET_M;

const url = new URL(
  'https://api.met.no/weatherapi/locationforecast/2.0/compact'
);
url.searchParams.set('lat', normalizedLat.toFixed(MET_LAT_LON_DECIMALS));
url.searchParams.set('lon', normalizedLon.toFixed(MET_LAT_LON_DECIMALS));
if (normalizedAltitudeM != null) {
  url.searchParams.set('altitude', normalizedAltitudeM.toString());
}

const response = await fetch(url, {
  headers: {
    'User-Agent': 'TrekCheck/1.0 https://<REAL_PROJECT_DOMAIN>/contact',
    'Accept': 'application/json'
  }
});
```

Il valore reale del `User-Agent` deve essere configurato tramite environment/configuration e non duplicato come magic string nel codice.

### Normalizzazione spaziale per cache/upstream

Le micro-variazioni GPS non devono generare cache miss inutili. La normalizzazione è **prescrittiva** nell'MVP e deve essere applicata sia alla cache key sia ai parametri inviati a MET, così il contenuto di una chiave non dipende dal primo request che l'ha popolata.

Default provider-specific MET Norway:

```text
latitude/longitude: 3 decimali (~100 m di ordine di grandezza)
altitude: bucket al multiplo di 50 m più vicino
```

Motivazione:

- MET raccomanda di non usare più di 4 decimali per favorire il caching;
- il forecast globale usato fuori dall'area nordica ha risoluzione orizzontale di circa 9 km;
- 50 m di bucket altimetrico limita l'errore massimo introdotto dal solo bucketing a circa 25 m, molto inferiore all'incertezza complessiva del forecast e sufficiente a consolidare quote GPX rumorose come 1800/1803 m.

Questi valori devono vivere in `MetNorwayProviderConfig`, essere testati e poter cambiare senza modificare il dominio. Non normalizzare la geometria GPX persistita: la normalizzazione riguarda solo le richieste meteo/cache.

### Caching HTTP provider-aware

Il test reale ha restituito, tra gli altri, gli header:

```http
Expires: Mon, 12 Oct 2026 15:12:07 GMT
Last-Modified: Mon, 12 Oct 2026 14:41:08 GMT
Age: 4
```

Il BFF deve:

1. memorizzare payload raw + `Expires` + `Last-Modified`;
2. servire dalla cache fino a `Expires`;
3. alla scadenza, se possibile, fare revalidation con `If-Modified-Since`;
4. non fare refresh aggressivo indipendente dagli header provider;
5. deduplicare richieste concorrenti per la stessa cache key;
6. evitare che 12 sample vicini generino 12 chiamate se mappano alla stessa cella/cache key.

Esempio conditional request:

```http
GET /weatherapi/locationforecast/2.0/compact?lat=41.9028&lon=12.4964&altitude=62 HTTP/1.1
Host: api.met.no
User-Agent: TrekCheck/1.0 https://<REAL_PROJECT_DOMAIN>/contact
Accept: application/json
If-Modified-Since: Mon, 12 Oct 2026 14:41:08 GMT
```

### DTO normalizzato verso Android

Il client Android non deve ricevere direttamente il modello MET. Esempio response BFF:

```json
{
  "provider": "met-no",
  "fetchedAt": "2026-10-12T14:41:13Z",
  "forecastAvailableUntil": "2026-10-21T18:00:00Z",
  "samples": [
    {
      "id": "sample-01",
      "eta": "2026-10-13T12:00:00Z",
      "forecastIssuedAt": "2026-10-12T13:17:31Z",
      "forecastValidAt": "2026-10-13T12:00:00Z",
      "periodType": "1h",
      "resolutionMinutes": 60,
      "temperatureC": 20.8,
      "relativeHumidityPct": 53.4,
      "cloudFractionPct": 16.4,
      "windSpeedMs": 5.3,
      "windFromDirectionDeg": 358.3,
      "precipitationAmountMm": 0.0,
      "conditionCode": "fair_day",
      "confidence": {
        "level": "HIGH",
        "reason": "Hourly forecast available"
      }
    }
  ],
  "attribution": {
    "provider": "MET Norway",
    "license": "CC BY 4.0"
  }
}
```

Il BFF **non deve restituire `localEta`** e non deve formattare date nel timezone del percorso. `eta`, `forecastIssuedAt`, `forecastValidAt`, `fetchedAt` e `forecastAvailableUntil` sono UTC ISO-8601. Il client ricostruisce sempre l'orario locale tramite `Instant.atZone(tripPlan.displayTimezone)`. Questo rende impossibile fare accidentalmente data binding su una stringa locale generata dal backend e mantiene un solo owner della conversione timezone: il client.

### Error mapping

Il BFF deve normalizzare almeno:

- `400`: input TrekCheck invalido;
- `403` upstream: configurazione/provider access issue, spesso User-Agent non accettato;
- `429` upstream: rate limit/throttling;
- `5xx` upstream: MET temporaneamente indisponibile;
- timeout/network: provider unreachable.

Esempio errore BFF:

```json
{
  "code": "WEATHER_PROVIDER_RATE_LIMITED",
  "retryable": true,
  "message": "Weather data is temporarily unavailable.",
  "provider": "met-no"
}
```

Non inoltrare al client stack trace o body HTML raw del provider.

### Perché il BFF serve

- caching condiviso;
- controllo traffico;
- isolamento dal provider;
- possibilità di cambiare provider;
- nessuna logica MET nella UI;
- migliore conformità ai Terms of Service;
- riduzione dell'esposizione diretta dell'IP dell'utente al provider meteo;
- gestione centralizzata di timezone, matching e forecast resolution;
- test contract-based con fixture JSON reali.

### Test obbligatori del BFF

- cache hit prima di `Expires`;
- revalidation dopo expiry;
- risposta MET `200`;
- risposta MET `403`;
- risposta MET `429`;
- risposta MET `5xx`;
- timeseries interamente oraria;
- timeseries con passaggio 1h → 6h;
- assenza di `next_1_hours`;
- sample senza altitude;
- sample con altitude;
- due ETA differenti sulla stessa coordinata usano lo stesso payload provider cached;
- conversione `Instant` ↔ `ZoneId` coperta da test DST.

---

## FR-MVP-009 — Daylight calculation

Non usare API esterne.

Calcolare localmente:

- sunrise;
- sunset;
- durata daylight.

Per il briefing interessa principalmente:

- ora sunset alla zona di percorso;
- ETA finale;
- differenza ETA fine vs sunset.

Possibile implementazione tramite algoritmo astronomico locale.

### Condition flags

- `DAYLIGHT_OK`: arrivo >60 min prima sunset;
- `DAYLIGHT_TIGHT`: arrivo entro 60 min dal sunset;
- `AFTER_SUNSET`: ETA oltre sunset.

Le soglie devono essere configurabili.

---

## FR-MVP-010 — Briefing summary

Schermata principale del prodotto.

### Header

Esempio:

**Monte X — Sab 17 Ott**

- Start: 08:00
- Estimated finish: 14:35
- Distance: 14,8 km
- Ascent: +960 m
- Max altitude: 2.240 m

### “What to expect”

Card sintetica:

- temperature: 5–14 °C;
- max wind: 11 m/s;
- expected precipitation: 1,2 mm;
- sunset: 18:18;
- estimated finish: 14:35.

### Important conditions

Non chiamarle “danger score”.

Usare:

**Conditions to note**

Esempi:

- Strong wind near the highest section
- Low temperature above 1.900 m
- Rain expected after 13:00
- Finish close to sunset

Ogni flag deve mostrare:

- cosa è stato rilevato;
- dove;
- quando;
- dato quantitativo;
- sorgente.

---

## FR-MVP-011 — Timeline route + weather

Visualizzare una timeline:

```text
08:00  Start
       13°C · 4 m/s · dry

09:20  km 4.2 · 1,250 m
       10°C · 6 m/s · cloudy

11:10  km 8.1 · 2,210 m
       6°C · 11 m/s · light rain

14:35  Finish
       9°C · 5 m/s
```

Ogni riga:

- ETA;
- cumulative distance;
- elevation;
- temperature;
- wind;
- precipitation;
- icon/condition.

Tap:

- evidenzia punto corrispondente sulla mappa.

---

## FR-MVP-012 — Weather map markers

Sulla mappa mostrare:

- route polyline;
- weather sample markers;
- highest point;
- start/end.

Tap su weather sample:

- ETA;
- elevation;
- temperature;
- wind;
- precipitation.

L'MVP non deve avere:

- radar;
- animated weather layers;
- 3D terrain;
- turn-by-turn.

---

## FR-MVP-013 — Condition engine

Engine completamente deterministico.

Interfaccia:

`ConditionRule`

Ogni regola restituisce:

- type;
- severity;
- title;
- explanation;
- affected sample/segment;
- observed value;
- threshold;
- rule version.

### Regole iniziali candidate

Configurabili e soggette a test con utenti.

#### Wind

- attention: >= 10 m/s;
- elevated: >= 15 m/s.

#### Cold

- attention: <= 5 °C;
- elevated: <= 0 °C.

#### Heat

- attention: >= 30 °C.

#### Rain

- almeno un intervallo con precipitazione significativa;
- total route forecast sopra soglia.

#### Daylight

- finish entro 60 min da sunset;
- finish dopo sunset.

### Importante

L'app non deve mostrare:

- “safe”;
- “unsafe”;
- “go/no-go”;
- probabilità di incidente.

---

## FR-MVP-014 — Checklist engine

Checklist deterministica derivata dalle condizioni.

### Categorie

- weather protection;
- warmth;
- visibility/light;
- navigation basics;
- power;
- hydration reminder generico.

### Esempi

Se pioggia prevista:

- “Consider a waterproof shell.”

Se temperatura minima <10 °C:

- “Consider an insulating layer.”

Se temperatura minima <=5 °C:

- “Consider gloves / warm layer.”

Se finish vicino/dopo sunset:

- “Carry a headlamp and verify battery.”

Sempre:

- offline copy of route / backup navigation;
- charged phone / power reserve.

### Non fare nell'MVP

- prescrizioni mediche;
- calorie personalizzate;
- litri d'acqua “obbligatori”;
- valutazioni su capacità individuale;
- AI-generated safety advice.

---

## FR-MVP-015 — Salvataggio locale

Salvare localmente:

- route metadata;
- route points semplificati;
- original GPX URI se persistibile;
- trip settings;
- weather samples;
- briefing;
- checklist state;
- timestamps di freshness.

### Database

Room.

### Retention

Default:

- fino a cancellazione utente;
- nessuna sincronizzazione cloud.

---

## FR-MVP-016 — Offline behaviour

Dopo la generazione del briefing:

deve essere consultabile senza rete.

Offline devono funzionare:

- route stats;
- ETA;
- timeline salvata;
- forecast già scaricato;
- condition flags;
- checklist;
- elevation profile.

La mappa può mostrare:

- route;
- eventuali tile già cached.

Non promettere “offline maps complete” nell'MVP.

---


## FR-MVP-017 — Refresh forecast manuale

### Scopo

Aggiornare un TrekCheck già generato e mostrare all'utente cosa è cambiato rispetto all'ultimo forecast valido.

### Regole Free

Ogni refresh meteo completato con successo costituisce una nuova **generazione weather-enabled** e consuma una unità della quota Free di `FR-MVP-019`.

Non consumano quota:

- errori provider/rete;
- tentativi fuori orizzonte;
- briefing route-only;
- apertura offline di un briefing esistente;
- Share Card.

### Flusso

```text
Briefing salvato
   ↓
Aggiorna meteo
   ↓
quota disponibile?
   ↓ sì
trip nell'orizzonte provider?
   ↓ sì
Weather BFF
   ↓
nuovo ForecastSnapshot
   ↓
ForecastDiffEngine(previous, current)
   ↓
persist snapshot + update Briefing.latestForecastSnapshotId
   ↓
UI diff
```

La diff mostra almeno:

- variazione min/max temperatura;
- variazione max vento;
- variazione precipitazione quando i periodi sono comparabili;
- nuovi/rimossi condition flags;
- nuova freshness/granularità forecast.

Il Free non usa soglie personalizzabili e non genera notifiche automatiche.

### Forecast fuori orizzonte

Usare capability centralizzata:

```kotlin
data class WeatherProviderCapabilities(
    val maxForecastLeadTime: Duration,
    val detailedForecastLeadTime: Duration
)
```

Configurazione iniziale MET Norway:

```text
maxForecastLeadTime ≈ 9 giorni
detailedForecastLeadTime ≈ 60 ore
```

Se `departureInstant > now + maxForecastLeadTime`:

- non chiamare BFF/provider;
- disabilitare/nascondere `Aggiorna meteo`;
- mostrare `Meteo disponibile circa 9 giorni prima della partenza`;
- mantenere route analysis/ETA/daylight;
- stato `WEATHER_PENDING_HORIZON`;
- nessun consumo quota.

### Quota esaurita

Se `freeWeatherBriefingsGeneratedCount >= 3`:

- non chiamare BFF;
- mostrare `Quota meteo Free esaurita`;
- mantenere pienamente accessibili GPX import, route analysis, briefing salvati/offline e Share Card.

### Stale logic

Indicazione iniziale configurabile:

- `<3h`: fresh;
- `3–12h`: normal;
- `>12h`: refresh suggested se quota disponibile.

### Acceptance criteria

- nessun background scheduler;
- successo incrementa quota una sola volta;
- fallimento non incrementa quota;
- refresh fuori orizzonte non chiama il provider;
- precipitazioni 1h e 6h non vengono confrontate come equivalenti;
- update dello snapshot pointer solo dopo persistenza riuscita.

---

## FR-MVP-018 — Settings Free

- lingua IT/EN;
- unità metriche/imperiali;
- default pace profile;
- default timezone policy;
- privacy;
- data sources/attribution;
- disclaimer.

Persistenza globale tramite Preferences DataStore:

```kotlin
data class GlobalAppPreferences(
    val languageTag: String?,
    val units: UnitsSystem,
    val defaultPaceProfile: PaceProfile,
    val defaultTripZonePolicy: DefaultTripZonePolicy,
    val freeWeatherBriefingsGeneratedCount: Int
)
```

Nessun setting Premium deve comparire nel modello Free.

---

## FR-MVP-019 — Limiti Free Tier

Free include:

- import GPX illimitato;
- route statistics/elevation/ETA illimitati;
- route-only planning illimitato;
- salvataggio locale dei TripPlan/briefing generati;
- Share Card Free illimitata per briefing esistenti;
- fino a **3 generazioni weather-enabled completate con successo**, includendo primo full briefing e refresh manuali riusciti.

### Contatore

`freeWeatherBriefingsGeneratedCount`:

- vive in DataStore;
- è monotono;
- non dipende da `COUNT(Briefing)`;
- non decrementa quando l'utente cancella dati;
- incrementa solo dopo weather response + briefing persistiti con successo.

La disinstallazione può azzerare il contatore: questo bypass è accettato nell'MVP. Non introdurre account/fingerprinting solo per impedirlo.

Quando esaurito, esporre stato `FREE_WEATHER_QUOTA_EXHAUSTED`. Il paywall è fuori scope di questo documento.

---

## FR-MVP-020 — Error handling

Gestire esplicitamente:

- GPX invalido/senza quota/troppo grande;
- nessuna connessione;
- weather provider unavailable/rate limit/timeout;
- forecast fuori orizzonte;
- timezone incoerente;
- dati parziali;
- map provider non raggiungibile;
- quota Free esaurita;
- export Share Card fallito.

Ogni errore deve produrre:

- messaggio utente;
- causa tecnica loggabile senza PII/coordinate inutili;
- azione possibile.

---

## FR-MVP-021 — TrekCheck Share Card Free

### Scopo

Creare un riepilogo visuale essenziale del TrekCheck da condividere rapidamente via WhatsApp, Telegram, email o altre app Android.

È una **share card**, non una scheda di sicurezza/emergenza.

### Output

- immagine PNG/JPEG generata localmente;
- leggibile su smartphone;
- nessun PDF nel Free;
- nessun backend export.

### Contenuto

- nome percorso;
- data/ora partenza nel timezone del percorso;
- distanza;
- dislivello positivo se disponibile;
- durata stimata;
- expected finish;
- min/max temperatura prevista;
- max wind previsto;
- precipitation summary;
- sunset + daylight margin;
- massimo 3 condition flags;
- piccolo elevation profile o route sparkline opzionale;
- footer `Generated with TrekCheck` + motto localizzato.

### Privacy

Per default **non includere**:

- coordinate precise;
- GPX;
- posizione device;
- email/telefono;
- contatti emergenza;
- note private.

### Implementazione

```text
Briefing
  ↓
ShareCardUseCase
  ↓
ShareCardModel
  ↓
off-screen Compose/Canvas renderer
  ↓
PNG/JPEG in cache app
  ↓
FileProvider
  ↓
Android Sharesheet
```

Requisiti:

- mai `file://` URI;
- cleanup periodico cache export;
- rendering fuori main thread quando costoso;
- IT/EN;
- metric/imperial;
- preview coerente con output.

### Acceptance criteria

- funziona offline su briefing già salvato;
- export non consuma quota weather;
- nessun dato nascosto entra nell'immagine;
- nessun claim `safe/unsafe/go`.

---

# 6. UX flow MVP Free

## 6.1 Onboarding

Massimo 2–3 schermate:

1. `Importa il GPX. TrekCheck analizza il percorso prima che tu parta.`
2. `Il meteo viene associato alla posizione e all'ora stimata di passaggio.`
3. `Le previsioni sono informative: verifica sempre le condizioni locali prima di partire.`

No account creation.

## 6.2 Home

- `Import GPX` / `Importa GPX`;
- ultimi TripPlan salvati;
- stato weather (`pending`, `fresh`, `stale`, `quota esaurita`);
- accesso Settings.

## 6.3 Route Preview

- mappa;
- polyline;
- distanza;
- dislivello;
- min/max elevation;
- elevation profile;
- eventuale warning elevation missing/noisy.

CTA: `Prepare TrekCheck`.

## 6.4 Trip Setup

- data;
- ora partenza;
- timezone;
- pace profile;
- custom pace se previsto.

Se fuori weather horizon, mostrare che il percorso può essere pianificato ma il meteo non è ancora disponibile.

## 6.5 Generating

Progress deterministico:

```text
Analyzing route
Estimating timing
Checking weather
Building TrekCheck
```

## 6.6 Briefing

Ordine consigliato:

1. route summary;
2. conditions to note;
3. weather overview;
4. map;
5. timeline;
6. daylight;
7. checklist;
8. route statistics;
9. forecast source/freshness;
10. disclaimer;
11. azioni `Aggiorna meteo` e `Condividi` quando disponibili.

## 6.7 Share Card

`Condividi` → preview → Android Sharesheet.

L'utente deve vedere prima dell'export gli stessi dati che finiranno nell'immagine.

---

# 7. Fuori scope MVP

Le capability a pagamento sono definite esclusivamente in `trekcheck-product-technical-baseline-Premium.md`.

Il core Free non deve dipendere da Billing, scheduler automatici, notification permission o modelli dati Premium.

---

# 8. Servizi esterni — verifica costi, licenze e vincoli

## 8.1 MET Norway Weather API

### Uso proposto

Provider meteo principale MVP.

Endpoint:

`Locationforecast 2.0 / compact`

### Copertura

Globale. Il test manuale del 2026-10-12 sulle coordinate di Roma (`41.9028, 12.4964`) ha restituito `HTTP 200` e una timeseries valida, confermando operativamente l'utilizzabilità fuori dalla Norvegia.

Forecast:

circa 9 giorni, con granularità che può diminuire aumentando l'orizzonte. **Non assumere forecast orario per tutto l'orizzonte.**

### Costo

**Utilizzabile gratuitamente anche per uso commerciale**, nel rispetto delle licenze e dei Terms of Service.

Non esiste un normale modello “free tier con X chiamate incluse” come per molte API commerciali: è un servizio pubblico. Questo non equivale però a SLA garantito o capacità illimitata.

### Licenza / attribuzione

I dati MET devono essere attribuiti secondo la licenza applicabile indicata dal provider. Per il prodotto mostrare chiaramente:

`Weather data: MET Norway`

oltre alle informazioni/licenze richieste nella sezione Data Sources / Attribution.

### User-Agent

È obbligatorio usare un identificativo reale e contattabile.

Esempio valido di formato:

```text
TrekCheck/1.0 https://<REAL_PROJECT_DOMAIN>/contact
```

oppure un indirizzo email di supporto reale.

Il test iniziale eseguito con PowerShell usando `Invoke-RestMethod -Headers` ha prodotto `403`; la chiamata eseguita con `curl.exe -A` ha invece mostrato esplicitamente lo `User-Agent` inviato e ha ottenuto `200 OK`. Per i test Windows preferire:

```powershell
curl.exe -v `
  -A "TrekCheckTest/0.1 <REAL_CONTACT>" `
  "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=41.9028&lon=12.4964"
```

Se si usa Windows PowerShell 5.1 con `Invoke-RestMethod`, usare il parametro dedicato `-UserAgent` invece di affidarsi a `-Headers` per questo header.

### UTC e timezone

Tutti i timestamp del provider sono UTC.

Regola di dominio:

```text
User local route time
      + ZoneId IANA
          ↓
       Instant UTC
          ↓
  Weather matching / ETA
          ↓
       Instant UTC
          ↓
Route ZoneId for presentation
```

Il timezone del telefono non è la fonte di verità del viaggio.

### Semantica dati

- `instant.details`: valori validi all'istante `time`;
- `next_1_hours`: summary/accumuli dell'ora successiva;
- `next_6_hours`: summary/accumuli delle 6 ore successive;
- `next_12_hours`: summary/accumuli delle 12 ore successive.

Il modello AI che implementa l'integrazione **non deve flattenare questi blocchi senza conservarne l'intervallo temporale**.

### Altitude

Quando il GPX contiene quota valida, passare `altitude=<metri>` nella request.

Esempio:

```text
...?lat=42.4660&lon=13.5580&altitude=2100
```

Quando manca quota:

- omettere il parametro;
- non inventare elevation;
- marcare il briefing come meno accurato rispetto agli effetti altimetrici.

### HTTP caching

Nel test reale MET ha restituito:

- `Expires`;
- `Last-Modified`;
- `Age`.

Questi header devono guidare il BFF. Non introdurre TTL arbitrari più aggressivi del provider senza una ragione documentata.

Strategia:

```text
request
  ↓
cache key(lat/lon/altitude/provider)
  ↓
cache fresh? ── yes → use cached payload
  │
  no
  ↓
If-Modified-Since (when available)
  ↓
MET Norway
  ↓
refresh payload + caching metadata
```

### Errori rilevanti

- `403`: client non accettato/configurazione User-Agent/access policy;
- `429`: throttling/rate limit;
- `5xx`: indisponibilità temporanea;
- timeout/network: retry controllato;
- payload parziale: degradare il singolo sample, non l'intero briefing se possibile.

### Vincoli operativi rilevanti

- User-Agent identificativo obbligatorio;
- caching necessario;
- non generare traffico inutile;
- coordinate normalizzate/arrotondate secondo policy applicativa;
- niente aggiornamento continuo su mobile quando app non in uso;
- nessuno SLA;
- può applicare throttling;
- mobile/web dovrebbero preferibilmente usare un proxy gateway.

### Privacy

Le chiamate dirette espongono al provider:

- IP utente;
- coordinate richieste.

Per questo è preferibile un backend proxy. Il backend non deve inviare il GPX completo: solo i sample necessari.

### Verifica manuale 2026-10-12

Request Roma:

```text
lat=41.9028
lon=12.4964
```

Risultato:

```text
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 40932
Expires: Mon, 12 Oct 2026 15:12:07 GMT
Last-Modified: Mon, 12 Oct 2026 14:41:08 GMT
```

Il forecast del 13 ottobre conteneva:

- valori orari nel breve periodo;
- temperatura;
- umidità;
- pressione;
- nuvolosità;
- direzione e velocità vento;
- precipitazione;
- `symbol_code`;
- successiva riduzione della granularità temporale all'aumentare dell'orizzonte.

Questa fixture reale deve essere conservata nel repository di test, opportunamente ridotta, ad esempio:

`backend/weather-worker/test/fixtures/met-no-rome-2026-10-12.json`

### Conclusione

**Adatto all'MVP commerciale senza costo API diretto.**

È il provider consigliato, ma deve restare dietro l'astrazione `WeatherProvider` e il BFF perché:

- non offre SLA;
- policy/versioni possono cambiare;
- in futuro può essere utile introdurre provider secondari o commerciali.

---

## 8.2 Open-Meteo

### Free API

La free API:

- no key;
- fino a 10.000 call/day;
- **solo non-commercial use**.

### Commercial app

Per una app monetizzata, la free API pubblica non è la scelta corretta.

Serve:

- customer API commerciale;
- oppure self-host del progetto open-source.

### Conclusione

**NON usare l'endpoint free pubblico nell'app commerciale.**

Può essere provider futuro a pagamento o self-host.

---

## 8.3 OpenFreeMap

### Uso proposto

Map tiles/style per visualizzazione route.

### Costo

Il servizio pubblico dichiara:

- gratuito;
- nessun limite sul numero di map views/requests;
- nessuna registrazione;
- nessuna API key;
- uso commerciale consentito.

### Attribution

Richiesta.

Indicazione tipica:

`OpenFreeMap © OpenMapTiles Data from OpenStreetMap`

Con MapLibre parte dell'attribution può essere gestita dal client, ma verificare sempre che sia visibile.

### Rischi

- no SLA;
- servizio “as-is”;
- può cambiare/discontinuare;
- terms possono cambiare.

### Mitigazione

Definire:

`MapTileProvider`

astratto.

Nessun URL hard-coded distribuito in decine di classi.

### Conclusione

**Adatto al lancio con costo zero**, ma non trattarlo come infrastruttura garantita per sempre.

---

## 8.4 OpenStreetMap standard tile servers

OpenStreetMap data è libero.

I server standard `tile.openstreetmap.org` non sono invece un servizio commerciale garantito.

Policy:

- best effort;
- caching obbligatorio;
- no bulk;
- no prefetch;
- offline download vietato;
- accesso può essere bloccato;
- i servizi commerciali devono essere consapevoli che l'accesso può essere ritirato.

### Conclusione

**Non usarli come tile provider principale del prodotto distribuito.**

Usare OSM come fonte dati tramite un provider compatibile, ad esempio OpenFreeMap.

---

## 8.5 MapLibre Native

SDK mapping open source.

Licenza principale:

BSD 2-Clause.

### Costo

Zero.

### Uso commerciale

Consentito dalla licenza.

### Scelta tecnica

Per minimizzare rischio dovuto a API Compose non ancora completamente stabili, l'MVP può usare:

- MapLibre Native Android;
- `MapView` integrata dentro Jetpack Compose con `AndroidView`.

In futuro valutare MapLibre Compose.

---

## 8.6 Cloudflare Workers

### Uso proposto

Weather BFF / caching gateway.

### Free plan corrente

Al 2026:

- 100.000 richieste/giorno;
- 10 ms CPU per request;
- fino a 50 subrequest per request sul piano free;
- 128 MB RAM.

### Costo

**Zero finché si resta nel free tier.**

Non è corretto però definirlo “gratuito per sempre”:

- quote possono cambiare;
- il progetto può crescere;
- non c'è una garanzia contrattuale che il free tier futuro resti identico.

### Architettura per sfruttarlo bene

L'app invia **una richiesta per briefing**.

Il Worker fa massimo 12 chiamate MET.

Questo resta entro il limite di 50 subrequests.

Caching shared per coordinate/forecast.

### Conclusione

Per il target iniziale 200–300 €/mese è altamente probabile restare a costo zero.

---

## 8.7 OpenTopography

### Dataset

I dati elevazione sottostanti possono avere licenze aperte.

### Free API

Per non-academics ha limiti bassi.

Soprattutto:

**l'integrazione for-profit della API in un prodotto commerciale non è permessa con free API key.**

Serve Enterprise API key.

Le key gratuite non possono essere embeddate nell'app per far usare l'API a terzi.

### Conclusione

**Non usare OpenTopography free API nell'MVP commerciale.**

---

## 8.8 Copernicus DEM

Copernicus GLO-30 / GLO-90:

- copertura globale;
- licenza gratuita;
- dati riutilizzabili secondo condizioni e attribution.

Tuttavia:

- accesso GLO-30 ha requisiti di registrazione/categorie aggiornati nel 2026;
- integrare un DEM globale richiede storage, processing e delivery;
- il dataset è gratuito, l'infrastruttura non necessariamente.

### Conclusione

Possibile futuro provider di elevation self-hosted.

Non necessario nell'MVP.

---

## 8.9 Sunrise / sunset

Nessun servizio esterno necessario.

Calcolo astronomico locale.

### Costo

Zero.

### Dipendenza

Zero.

---

## 8.10 GPX elevation

MVP:

usare `<ele>` dal GPX.

### Costo

Zero.

### Limite

Se elevation non esiste:

- route stats parziali;
- ETA meno preciso;
- meteo senza altitude esplicita.

Questa degradazione è accettabile nell'MVP.

---

# 9. È possibile un MVP con servizi 100% free?

## Risposta

**Sì, con una precisazione.**

Stack:

- MET Norway: €0, commerciale consentito;
- OpenFreeMap: €0, commerciale consentito;
- MapLibre: €0;
- GPX parsing: locale;
- elevation: dal GPX;
- daylight: locale;
- Room: locale;
- Cloudflare Worker Free: €0 entro quota;
- Google Play developer account: costo di registrazione una tantum.

Quindi il runtime può costare **€0/mese durante la fase iniziale**.

Non si può però promettere “€0 per sempre” perché:

- free tier Cloudflare può cambiare;
- OpenFreeMap non offre SLA;
- MET Norway può modificare policy/limiti;
- il traffico può crescere;

### Budget iniziale ragionevole

- Play Console: circa $25 una tantum;
- dominio: ~€10–20/anno;
- backend: €0;
- weather: €0;
- maps: €0;
- grafica: €0–30;
- privacy/support static site: €0 tramite hosting statico.

Target:

**< €100 prima del lancio.**

---


# 10. Architettura tecnica Android — MVP Free

## 10.1 Stack

### Client

- Kotlin;
- Jetpack Compose;
- Material 3;
- Coroutines / Flow;
- Room;
- Preferences DataStore;
- Navigation Compose;
- OkHttp/Retrofit o Ktor Client;
- MapLibre Native Android;
- java.time.

**Non richiesti dal Free MVP:** Google Play Billing, WorkManager per monitoraggio meteo, notification APIs.

### Build

- Gradle Kotlin DSL;
- version catalog;
- JDK compatibile con toolchain Android stabile;
- target SDK conforme ai requisiti Play vigenti (baseline 2026: API 36+);
- minSdk suggerito: 26.

### Backend

- Cloudflare Worker;
- TypeScript;
- `fetch` standard;
- edge cache;
- nessun database utente.

## 10.2 Architettura applicativa

- UI layer;
- domain layer;
- data layer;
- repositories;
- UDF/ViewModel/immutable UI state.

Struttura logica:

```text
app/
  ui/
  navigation/

core/
  model/
  gpx/
  geo/
  time/
  weather/
  rules/
  export/

data/
  local/
  remote/
  repository/

domain/
  usecase/

feature/
  home/
  importroute/
  routepreview/
  tripsetup/
  briefing/
  sharecard/
  settings/

backend/
  weather-worker/
```

Per l'MVP può restare un singolo modulo Gradle mantenendo i boundary a package.

## 10.3 Contratto weather provider

```kotlin
interface WeatherProvider {
    suspend fun getRouteForecast(
        samples: List<WeatherRequestSample>,
        routeZoneId: ZoneId
    ): RouteWeatherForecast
}

data class WeatherRequestSample(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Int?,
    val eta: Instant
)
```

Il dominio non dipende da DTO MET raw.

## 10.4 Time model

Tipi canonici:

- `Instant` per eventi assoluti;
- `ZoneId` per il contesto geografico del trip;
- `ZonedDateTime` solo boundary UI/input/output.

Il BFF è timezone-agnostic e restituisce solo UTC ISO-8601.

Vietati campi BFF `localEta`, `localForecastTime` o equivalenti.

---

# 11. Modello dati MVP Free

## Route

```text
id
name
sourceFileName
importedAt
distanceM
ascentM?
descentM?
minElevationM?
maxElevationM?
startLat/startLon
endLat/endLon
boundingBox
hasElevation
pointCount
segmentCount
geometryStorageVersion
geometryFileName
geometryChecksum
simplifiedGeometryEncoded?
simplificationVersion?
```

## RouteGeometryStore

**Nessuna tabella `TrackPoint`.**

La geometria full-resolution vive in file app-private, formato binario versionato e compatto (protobuf-lite o equivalente; gzip opzionale).

```kotlin
interface RouteGeometryStore {
    suspend fun write(routeId: String, geometry: NormalizedRouteGeometry): GeometryRef
    suspend fun read(routeId: String): NormalizedRouteGeometry
    suspend fun delete(routeId: String)
}
```

Preservare segment boundaries, lat/lon ed elevation opzionale. Salvare separatamente una geometria semplificata per rendering.

## TripPlan

```text
id
routeId
departureInstant
displayTimezone
paceProfile
customFlatSpeed?
createdAt
```

## RouteTimingPoint

```text
distanceM
lat
lon
elevationM?
elapsedSeconds
etaInstant
```

## WeatherSample

```text
lat/lon
altitudeM?
routeDistanceM
etaInstant
airTemperatureC
relativeHumidityPct?
windSpeedMs
windFromDirectionDeg
precipitationMm?
symbolCode?
forecastValidAt
resolution
periodType
fetchedAt
provider
```

## ConditionFlag

```text
type
severity
titleKey
messageKey
routeDistanceM?
etaInstant?
observedValue?
threshold?
ruleVersion
```

## Briefing

```text
id
tripPlanId
generatedAt
lastForecastFetchAt?
estimatedFinishInstant
minTempC?
maxTempC?
maxWindMs?
precipTotalMm?
sunsetInstant?
status
latestForecastSnapshotId?
```

## GlobalAppPreferences — DataStore

```text
languageTag?
unitsSystem
defaultPaceProfile
defaultTripZonePolicy
freeWeatherBriefingsGeneratedCount
```

## ForecastSnapshot

```text
id
tripPlanId
generatedAt
providerUpdatedAt?
departureInstant
routeSampleVersion
resolutionSummary
```

I sample possono essere tabella figlia `ForecastSnapshotSample` o payload normalizzato serializzato. Nessun enum `role`.

Il refresh manuale confronta `Briefing.latestForecastSnapshotId` con il nuovo snapshot, poi aggiorna il pointer solo dopo successo.

Retention: mantenere snapshot corrente + pochi precedenti (default 3) per diagnostica/diff; cleanup degli altri.

## Share Card

Nessuna entità DB. File temporaneo in cache, rigenerabile dal Briefing.

---

# 12. Pipeline funzionale MVP Free

```text
GPX file
  ↓
Secure streaming parser
  ↓
Normalized route + RouteGeometryStore
  ↓
Route metric/elevation engine
  ↓
Travel time estimator
  ↓
Weather sample selector
  ↓
Weather BFF
  ↓
Forecast mapper
  ↓
Daylight calculator
  ↓
Condition engine
  ↓
Checklist engine
  ↓
Briefing + ForecastSnapshot
  ↓
Room / local files
  ↓
Compose UI
```

Refresh manuale:

```text
Briefing.latestForecastSnapshot
  +
new Weather BFF result
  ↓
ForecastDiffEngine
  ↓
UI diff + persist current snapshot
```

Share:

```text
Briefing
  ↓
ShareCardUseCase
  ↓
local image
  ↓
Android Sharesheet
```

Non esiste un background weather path nel Free MVP.

---

# 13. Weather BFF — dettagli

## Endpoint

`POST /v1/weather/route`

### Request constraints

- max 12 samples;
- il client può inviare coordinate GPX ad alta precisione; il BFF le normalizza prima dell'upstream;
- verso MET non usare più di 3 decimali nell'MVP (`MetNorwayProviderConfig`), comunque mai oltre i 4 raccomandati dal provider;
- altitude numerica opzionale; il BFF applica bucket provider-specific prima dell'upstream;
- ETA ISO-8601 UTC;
- payload max molto piccolo.

### Backend validation

Rifiutare:

- sample >12;
- coordinate invalid;
- altitude fuori range plausibile;
- timestamp assurdi;
- payload oversized.

### Cache key

Separare concettualmente la cache del **payload raw provider** da un eventuale cache del DTO route-specific.

Raw MET cache key MVP:

```text
metno:v2:
lat=42.123:
lon=13.123:
altBucket=1800
```

Regole:

- **NON includere ETA/forecastBucket** nella raw-provider cache key: Locationforecast restituisce l'intera timeseries disponibile per quella località;
- normalizzare lat/lon/altitude prima di costruire la key e usare gli stessi valori nella request upstream;
- eventuale cache di una response già matched su ETA deve essere un layer separato e, se introdotto, può includere hash dei sample/ETA; non è necessaria nell'MVP;
- la cache raw deve rispettare `Expires` / `Last-Modified` MET.

### Upstream headers

User-Agent custom, ad esempio:

```text
TrekCheckWeather/0.1 (+https://example.com; support@example.com)
```

Il nome reale verrà deciso prima della pubblicazione.

### DTO interno

Il client non deve conoscere schema MET. Il DTO deve contenere solo timestamp UTC; nessun `localEta` o stringa locale generata dal BFF.

Esempio:

```json
{
  "provider": "met.no",
  "samples": [
    {
      "lat": 42.1234,
      "lon": 13.1234,
      "validAt": "2026-10-17T09:00:00Z",
      "temperatureC": 6.1,
      "windSpeedMs": 8.4,
      "windDirectionDeg": 245,
      "precipitationMm": 0.4,
      "humidityPct": 80,
      "conditionCode": "lightrain"
    }
  ]
}
```

---


# 14. Privacy

## Principio

Privacy-first.

### Nessun account

MVP senza login.

### Nessuna current device location

Per generare il briefing **non è necessario chiedere GPS/location permission**.

Le coordinate arrivano dal GPX.

Questo è fortemente preferibile:

- meno friction;
- meno policy Play;
- meno privacy complexity.

### Dati che lasciano il dispositivo

Solo:

- coordinate campionate del percorso;
- altitude sample;
- orario previsto.

Non inviare:

- file GPX intero;
- nome percorso;
- email;
- device contacts;
- identity.


### Privacy policy

Deve dichiarare:

- dati inviati al weather gateway;
- uso MET Norway;
- eventuali log Cloudflare;
- retention;
- nessuna vendita dati.

---


# 15. Google Play policy

## Target API

Dal 31 agosto 2026:

nuove app e aggiornamenti devono target Android 16 / API 36 o superiore.

Quindi il progetto deve partire direttamente su API 36+.

## Location

L'MVP non richiede location permission.

Questo evita la complessa procedura Play per background location.

## Testing nuovi account personali

Se l'account sviluppatore personale è stato creato dopo il 13 novembre 2023:

- closed test;
- almeno 12 tester;
- opt-in continuativo per almeno 14 giorni;
- poi richiesta accesso produzione.

Questo va inserito nel release plan.

---


# 16. Sicurezza e responsabilità

L'app riguarda attività outdoor.

Il linguaggio è critico.

## Non usare

- “safe route”;
- “safe to go”;
- “no risk”;
- “danger probability”;
- “you should proceed”;
- “you must cancel”.

## Usare

- “conditions to note”;
- “forecast indicates”;
- “consider”;
- “estimated”;
- “verify current local conditions”.

## Disclaimer

Concetto:

> L'app è uno strumento di pianificazione. Le previsioni e le stime possono essere incomplete o cambiare rapidamente, soprattutto in ambiente montano. Non sostituisce fonti meteorologiche ufficiali, cartografia, strumenti di navigazione, indicazioni delle autorità o il giudizio dell'utente.

Il disclaimer non deve essere usato per giustificare output irresponsabili.

---

# 17. Prestazioni

Target iniziali:

- import GPX 50K punti: <2 s su mid-range moderno;
- route analysis: <1 s aggiuntivo quando possibile;
- cold launch: <2 s target;
- briefing weather: dipende rete, target percepito <4 s;
- no ANR;
- map scrolling fluido;
- route polyline semplificata per rendering.

### Polyline simplification

Usare Douglas–Peucker o equivalente per rendering.

Non usare la geometria semplificata per:

- distance totals;
- ascent;
- ETA se peggiora precisione.

---


# 18. Test strategy MVP Free

## 18.1 Unit tests obbligatori

### GPX

- GPX 1.0/1.1;
- multi-segment;
- route;
- no elevation/time;
- duplicate points;
- malformed XML;
- invalid coordinates;
- 100K+ / boundary 200K points.

### Geometry/elevation

- known distances;
- cumulative distance;
- ascent/descent smoothing/noise;
- bounding box;
- high point.

### ETA

- flat/climb/descent;
- missing elevation;
- pace multipliers;
- timezone/DST independence.

### Weather sample selector

- short/long route;
- highest point retained;
- max 12;
- duplicates removed.

### Condition / checklist

- threshold just below/equal/above;
- daylight edge cases;
- no safety claims generated.

### ForecastDiffEngine

- same snapshot;
- temperature/wind changes;
- precipitation comparable/non-comparable;
- 1h vs 6h mismatch;
- missing sample;
- pointer update only after successful persistence.

### Share Card

- IT/EN;
- metric/imperial;
- full weather vs route-only;
- max 3 condition flags;
- offline export;
- privacy fields absent by default.

## 18.2 Integration tests

- client repository ↔ mocked BFF;
- BFF ↔ mocked MET;
- caching / 304 / 429 / 5xx;
- partial route response;
- refresh outside horizon does not call provider;
- quota exhausted does not call BFF;
- successful weather generation increments Free counter once;
- deleting briefing does not decrement counter;
- 200K points do not produce 200K Room inserts;
- BFF DTO contains no local timestamps;
- raw cache key independent from ETA;
- altitude/coordinate normalization collapses equivalent requests.

## 18.3 UI tests

Critical flow:

```text
Home
→ Import GPX
→ Route Preview
→ Trip Setup
→ Generate TrekCheck
→ Briefing
→ Reopen offline
→ Refresh/manual diff (if quota)
→ Share Card
```

---

# 19. Telemetry

MVP raccomandato: **minima o nulla**.

Google Play Console fornisce crash/ANR/Vitals. Se vengono aggiunti analytics, consentire solo eventi non sensibili:

- import succeeded;
- briefing generated;
- share card exported;
- Free quota reached.

Non inviare coordinate, route name, GPX o weather sample locations.

---

# 20. Boundary commerciale del Free MVP

Il Free MVP serve a validare il core senza dipendere dal sistema Premium.

Misurare:

- import → briefing completion;
- riuso su una seconda route;
- comprensione della timeline meteo;
- failure rate weather;
- percentuale utenti che raggiunge la quota Free;
- Share Card usage.

Non implementare Billing in questa baseline.

---

# 21. Go-to-market iniziale

## ASO

Keyword candidate:

- GPX weather;
- hiking weather;
- trail weather;
- route weather;
- hiking planner;
- GPX planner;
- hiking forecast;
- trekking weather;
- mountain weather.

Store localization: EN/IT prima, poi DE/ES/FR.

## Community/content

- community hiking/Komoot/Garmin;
- gruppi trekking;
- contenuti `How to check weather along a GPX route`;
- video corto import → briefing → Share Card.

---

# 22. Metriche e criteri di validazione

Metriche:

- store page → install;
- install → first GPX import;
- import → first TrekCheck;
- TrekCheck → second TrekCheck;
- crash-free sessions;
- GPX without elevation rate;
- weather failure rate;
- generation latency;
- Free quota reached;
- Share Card exported.

North-star iniziale:

> **% utenti che generano almeno 2 TrekCheck in 30 giorni.**

Continuare se:

- >50% di chi importa genera un briefing;
- >20% torna per seconda route entro 60 giorni;
- ETA+weather è percepito come valore distintivo.

Rivalutare/pivotare se gli utenti usano solo il viewer GPX, non capiscono il valore time-aware o la retention resta quasi nulla dopo traffico sufficiente.

---

# 23. Effort stimato MVP Free

| Area | Ore |
|---|---:|
| setup/architettura | 6–8 |
| GPX parser + fixtures | 10–15 |
| geometry/elevation | 10–15 |
| ETA engine | 8–12 |
| weather sampling/integration | 10–14 |
| Cloudflare Worker | 8–12 |
| map + elevation chart | 12–18 |
| briefing UI | 15–20 |
| persistence + manual diff | 8–12 |
| Share Card | 4–7 |
| settings/localization | 5–8 |
| testing/polish | 15–25 |

Totale indicativo: **~95–130 ore**.

---

# 24. Manutenzione prevista

Core Free stabile: circa **2–5 ore/mese** mediamente.

- Android target/dependencies;
- MET changes;
- map provider;
- crash review;
- GPX edge cases;
- Play policy.

---

# 25. Rischi tecnici

## R1 — Forecast montagna

MET globale ha risoluzione ampia rispetto a micro-topografia. Passare altitude, mostrare fonte/freshness e non fare claim hyper-local/safety.

## R2 — Provider gratuito

Mitigazione: provider abstraction + BFF + cache.

## R3 — Elevation noise

Smoothing + fixture reali + valori `estimated`.

## R4 — Timezone

`Instant` + `ZoneId`, test DST e timezone diverso dal device.

## R5 — Competitor

Difendere con UX, cross-provider GPX, privacy, velocità e brand.

---

# 26. Decisioni architetturali vincolanti MVP Free

1. Android native Kotlin + Compose.
2. No login/user cloud/background location/navigation/route discovery.
3. No AI/LLM nel MVP.
4. GPX completo locale.
5. Solo weather sample coordinates verso BFF.
6. MET Norway via BFF; provider abstraction obbligatoria.
7. OpenFreeMap + MapLibre.
8. GPX elevation come fonte quota MVP.
9. Daylight locale.
10. Condition/checklist engine deterministici.
11. IT/EN.
12. Briefing offline.
13. No `TrackPoint @Entity`.
14. BFF UTC-only e raw cache senza ETA.
15. Nessuna call weather fuori horizon.
16. Nessun Billing/WorkManager weather/notifiche nel Free core.
17. Share Card locale via FileProvider.

---

# 27. Naming e branding

Nome canonico: **TrekCheck**.

Motto EN:

> **Run a TrekCheck before you go.**

Motto IT:

> **Fai un TrekCheck prima di partire.**

Store descriptor di lavoro:

- IT: `TrekCheck — Meteo e analisi GPX`;
- EN: `TrekCheck — GPX Weather & Trip Briefing`.

Prima del lancio commerciale effettuare verifica formale store/domain/EUIPO-WIPO.

---

# 28. Roadmap MVP Free

## M0 — Technical spike

GPX streaming, RouteGeometryStore, MapLibre/OpenFreeMap, MET BFF/cache, timezone tests, benchmark 200K.

## M1 — Core route

Import, metriche/elevation, Trip Setup, ETA.

## M2 — Weather briefing

Sampling, weather mapping, daylight, conditions, checklist, persistence/offline.

## M3 — Free polish

Manual refresh/diff, Free quota, Share Card, IT/EN, accessibility, error states.

## M4 — Release

Privacy/legal, closed testing se richiesto, store assets, hardening.

---

# 29. Indicazioni per sviluppo AI agentico

## 29.1 Source of truth

Questo file è la source of truth del Free Tier. In caso di conflitto con codice generato, prevale questa baseline finché non viene aggiornata esplicitamente.

## 29.2 Guardrail prescrittivi

- no `TrackPoint @Entity`;
- Free counter in DataStore, non `COUNT(Briefing)`;
- no timestamp locali dal BFF;
- no weather call fuori horizon;
- raw cache key senza ETA;
- no Premium leakage nel core Free;
- refresh manuale usa `ForecastDiffEngine`;
- Share Card locale, non backend.

## 29.3 Weather checklist

Prima di considerare completata l'integrazione:

1. UTC parsed correttamente;
2. route `ZoneId` usato in UI;
3. DST test;
4. `instant` distinto da `next_1_hours/6_hours/12_hours`;
5. forecast non orario gestito;
6. Expires/Last-Modified rispettati;
7. User-Agent MET reale/configurabile;
8. 403/429/5xx gestiti;
9. fixture MET reale sanitizzata;
10. attribution presente;
11. no `safe/unsafe`.

## 29.4 ADR iniziali

- ADR-001 Native Kotlin;
- ADR-002 local-first/no account;
- ADR-003 MET Norway;
- ADR-004 Weather BFF;
- ADR-005 OpenFreeMap + MapLibre;
- ADR-006 GPX elevation MVP;
- ADR-007 deterministic condition engine;
- ADR-008 RouteGeometryStore fuori Room per-point;
- ADR-009 BFF UTC-only/cache normalized;
- ADR-010 Free quota DataStore;
- ADR-011 local Share Card.

## 29.5 Definition of Done

Feature Done solo se requirement + acceptance criteria + test + localization + error states + accessibility minima + privacy impact + documentazione sono completati.

---

# 30. Open questions prima del coding completo

1. package name;
2. minSdk 26 confermato;
3. MapLibre Native vs Compose wrapper;
4. algoritmo ETA finale dopo tracce reali;
5. max weather samples (default 12);
6. design visuale;
7. dominio/support email;
8. hosting privacy page.

---

# 31. Raccomandazione finale

Il Free deve validare un'esperienza semplice:

```text
GPX
+
ETA route-aware
+
forecast at expected arrival
+
daylight
+
transparent condition/checklist rules
+
offline persistence
+
shareable card
=
TrekCheck Free
```

Se questo core viene riutilizzato e compreso dagli utenti, il Premium può aggiungere automation/comparison/preparation senza riscrivere il dominio fondamentale.

---

# 32. Fonti e verifiche esterne

Verificate/ricontrollate il 2026-10-13.

## Mercato / competitor

- Epic Ride Weather — Google Play  
  https://play.google.com/store/apps/details?id=com.greensopinion.rideweather
- Epic Ride Weather FAQ / pricing  
  https://www.epicrideweather.com/frequently-asked-questions/
- Epic Ride Weather — AppBrain  
  https://www.appbrain.com/app/epic-ride-weather/com.greensopinion.rideweather
- Komoot Premium Weather  
  https://www.komoot.com/premium/weather
- AllTrails Premium features  
  https://support.alltrails.com/hc/it/articles/37200882853140
- Wikiloc Premium  
  https://www.wikiloc.com/premium
- Trailcast — Google Play  
  https://play.google.com/store/apps/details?id=com.aftertimemedia.weatherly
- Trailcast Tours & Weather — Google Play  
  https://play.google.com/store/apps/details?id=lemmidevpixelcraft.trailcast
- HikeReady  
  https://hikeready.app/

## Weather

- MET Norway API  
  https://api.met.no/
- MET Norway Terms of Service  
  https://api.met.no/doc/TermsOfService
- MET Norway Locationforecast  
  https://api.met.no/weatherapi/locationforecast/2.0/documentation
- MET Norway Locationforecast HOWTO  
  https://api.met.no/doc/locationforecast/HowTO
- MET Norway data model  
  https://api.met.no/doc/locationforecast/datamodel
- Open-Meteo pricing  
  https://open-meteo.com/en/pricing

## Maps

- OpenFreeMap  
  https://openfreemap.org/
- OpenFreeMap Terms  
  https://openfreemap.org/tos/
- OpenStreetMap Tile Usage Policy  
  https://operations.osmfoundation.org/policies/tiles/
- MapLibre Compose / Native  
  https://maplibre.org/
- MapLibre Native license  
  https://github.com/maplibre/maplibre-native/blob/main/LICENSE.md

## Elevation

- OpenTopography developers/API  
  https://opentopography.org/developers
- OpenTopography Terms  
  https://opentopography.org/usageterms
- Copernicus DEM  
  https://dataspace.copernicus.eu/explore-data/data-collections/copernicus-contributing-missions/collections-description/COP-DEM

## Backend

- Cloudflare Workers pricing  
  https://www.cloudflare.com/plans/
- Cloudflare Workers limits  
  https://developers.cloudflare.com/workers/platform/limits/

## Android / Google Play

- Android app architecture  
  https://developer.android.com/topic/architecture
- Android data and file storage overview  
  https://developer.android.com/training/data-storage
- Android Preferences DataStore  
  https://developer.android.com/topic/libraries/architecture/datastore
- Android architecture recommendations  
  https://developer.android.com/topic/architecture/recommendations
- Target API requirements  
  https://support.google.com/googleplay/android-developer/answer/11926878
- Personal account closed testing requirements  
  https://support.google.com/googleplay/android-developer/answer/14151465

---


# 33. Changelog

### MVP 1.0 — 2026-10-13

- split della baseline v0.5 in documenti Free e Premium;
- Free reso indipendente da Billing/Weather Watch/notifiche;
- quota Free definita come 3 generazioni weather-enabled riuscite;
- refresh manuale usa `ForecastDiffEngine`;
- aggiunta **TrekCheck Share Card Free**;
- mantenuti GPX, ETA, meteo route-aware, daylight, checklist, offline e privacy-first.
