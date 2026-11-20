# TrekCheck — Product & Technical Baseline — Premium

**Versione:** Premium 1.0  
**Data:** 2026-10-13  
**Stato:** source of truth per estensione Premium tramite sviluppo agentico  
**Dipendenza:** `trekcheck-product-technical-baseline-MVP.md`  
**Piattaforma target iniziale:** Android / Google Play  
**Tecnologia target:** Kotlin nativo + Jetpack Compose  
**Nome prodotto canonico:** TrekCheck  
**Motto EN:** “Run a TrekCheck before you go.”  
**Motto IT:** “Fai un TrekCheck prima di partire.”  

---

# 1. Scopo del documento

Questo documento definisce **solo le funzionalità a pagamento** di TrekCheck e le estensioni tecniche necessarie per implementarle sopra il core Free.

Il Premium **non deve duplicare o forkare**:

- parser GPX;
- RouteGeometryStore;
- ETA engine;
- weather sampling;
- Weather BFF;
- daylight engine;
- condition engine;
- checklist engine;
- persistence/offline;
- timezone model.

Tutti questi componenti restano quelli definiti nella baseline MVP Free.

Principio architetturale:

> **Premium = capability layer sopra il core Free, non una seconda applicazione.**

---

# 2. Value proposition Premium

Il Free risponde a:

> “Cosa incontrerò se parto a questa data e a quest'ora?”

Il Premium deve rispondere anche a:

> “Come cambiano le condizioni se parto in un altro momento?”  
> “Puoi continuare a controllare il meteo al posto mio?”  
> “Come preparo meglio lo zaino?”  
> “Come condivido un piano completo con chi resta a casa?”

Il Premium vende quindi **automation + comparison + advanced preparation + richer export**, non semplicemente più dati grezzi.

---

# 3. Entitlement e monetizzazione

## PR-PREM-001 — Pro Lifetime

### Scopo

Sbloccare tutte le capability Premium tramite un acquisto una tantum, coerente con il posizionamento privacy-first e con costi backend contenuti.

### Prezzo candidato

- launch: **€14,99**;
- standard da testare: **€19,99**.

Il prezzo non è hard-coded nel client: usare sempre i dati restituiti da Google Play Billing.

### Implementazione

- Google Play Billing, versione stabile supportata al momento dello sviluppo;
- one-time non-consumable product;
- `purchase`;
- `acknowledge`;
- restore/re-query entitlement;
- offline entitlement cache;
- riconciliazione periodica con Play quando disponibile la rete.

Modello dominio:

```kotlin
enum class EntitlementTier { FREE, PREMIUM }

data class PremiumEntitlement(
    val tier: EntitlementTier,
    val productId: String?,
    val verifiedAt: Instant?,
    val source: EntitlementSource
)
```

L'UI non deve basarsi direttamente su callback Billing: deve osservare un `EntitlementRepository`.

### Capability sbloccate

Premium include:

1. generazioni weather-enabled illimitate;
2. refresh manuali illimitati;
3. **Weather Watch**;
4. **Smart Departure Window**;
5. **Pack Weight Calculator**;
6. **Printable Safety & Emergency Card** completa in PDF/immagine.

Il salvataggio locale non deve avere un paywall separato: con generazioni illimitate, l'utente Premium può naturalmente conservare un numero illimitato di TripPlan/briefing compatibilmente con lo storage del device.

---

# 4. PR-PREM-002 — Unlimited TrekChecks & manual refresh

## Scopo

Rimuovere la quota Free di 3 generazioni weather-enabled e permettere all'utente Premium di generare e aggiornare briefing ogni volta che serve.

## Regole

- con entitlement Premium non leggere/bloccare su `freeWeatherBriefingsGeneratedCount`;
- il contatore Free può restare persistito per eventuale downgrade/restore failure, ma non deve essere incrementato mentre l'entitlement Premium è valido;
- nessun limite applicativo artificiale ai refresh manuali;
- restano validi provider horizon, rate limits, cache e error handling;
- “illimitato” significa **nessuna quota TrekCheck**, non bypass dei limiti tecnici del provider.

---

# 5. PR-PREM-003 — Weather Watch

## 5.1 Scopo

Weather Watch trasforma il TrekCheck da fotografia statica a **monitoraggio pre-partenza**.

L'utente prepara il trekking, abilita il monitoraggio e TrekCheck esegue pochi refresh mirati mentre la partenza si avvicina. Una notifica locale viene inviata **solo quando le categorie scelte cambiano oltre la sensibilità configurata**.

Value proposition:

> **Prepare once. TrekCheck keeps watching until you leave.**

## 5.2 Vincoli di prodotto

1. opt-in esplicito per ogni TripPlan;
2. default già pronti, ma `enabled=false` finché l'utente non attiva la feature;
3. categorie selezionabili: temperatura, vento, pioggia;
4. sensibilità: `SMALL`, `MEDIUM`, `LARGE`;
5. smart threshold crossings opzionale;
6. nessun polling continuo;
7. nessun exact alarm;
8. nessuna percentuale inventata di affidabilità;
9. implementazione MVP Premium device-side, senza account/FCM/server scheduler;
10. una sola notifica aggregata per check.

## 5.3 Scheduler

Milestone iniziali:

```text
T-60h
T-24h
T-8h
T-2h
```

Regole:

- schedulare one-time work con WorkManager;
- se il trip viene modificato, invalidare e ricreare schedule;
- se Weather Watch viene disabilitato o il trip eliminato, cancellare tutti i work;
- se una milestone è già passata quando la feature viene attivata, schedulare solo quelle future e, se opportuno, un check iniziale immediato;
- richiedere `NetworkType.CONNECTED`;
- trattare l'orario come best-effort, non exact.

## 5.4 Configurazione utente

Per ogni categoria:

```text
[on/off]
Sensitivity: Small | Medium | Large
```

Default Premium consigliato:

```text
Temperature: ON / Medium
Wind:        ON / Medium
Rain:        ON / Medium
Smart threshold crossings: ON
```

### Mapping iniziale sensibilità

| Categoria | Small | Medium default | Large |
|---|---:|---:|---:|
| Temperatura | ≥ 2 °C | ≥ 4 °C | ≥ 7 °C |
| Vento | ≥ 3 m/s | ≥ 5 m/s | ≥ 8 m/s |
| Pioggia comparabile | ≥ 0,5 mm | ≥ 2 mm | ≥ 5 mm |

Le soglie sono **configurazione di prodotto**, non soglie universali di sicurezza. Devono essere versionate.

## 5.5 Smart threshold crossings

Anche con delta inferiore alla soglia, può essere utile avvisare quando una variabile attraversa una fascia significativa definita dal Condition Engine.

Esempio temperatura:

```text
previous min +2 °C
current min  -1 °C
→ crossing dello zero
```

Esempio vento:

```text
previous max 9 m/s
current max  15 m/s
→ severity band superiore
```

La logica:

- si applica solo a categorie abilitate;
- è disattivabile;
- riusa le fasce versionate del Condition Engine;
- non usa termini `safe/unsafe`.

## 5.6 Confronto route-aware

Il confronto non deve limitarsi al trailhead. Confrontare gli stessi `RouteTimingPoint`/sample rappresentativi lungo la traccia.

Output possibile:

> “Wind increased mainly above 1,900 m between km 7.2 and km 9.4.”

Matching:

- stesso sample id/versione quando possibile;
- se cambiano departure time, timezone, pace profile o route sample version, invalidare la baseline e rigenerarla;
- non comparare precipitazioni 1h vs 6h come equivalenti.

## 5.7 Baseline cumulativa

Per evitare che piccole variazioni successive nascondano un cambiamento complessivo:

```text
initial: 12 °C
T-60h:   10 °C  delta -2
T-24h:    8 °C  delta -4 vs baseline
```

Con `MEDIUM=4°C`, il check T-24h deve notificare.

Usare:

- `latestSnapshotId`;
- `alertBaselineSnapshotId`;
- `lastNotifiedSnapshotId`.

Dopo una notifica, lo snapshot corrente diventa nuova baseline e lastNotified.

## 5.8 Modello dati

```text
WeatherWatchSettings
- tripPlanId
- enabled
- temperatureEnabled
- temperatureSensitivity
- windEnabled
- windSensitivity
- rainEnabled
- rainSensitivity
- smartThresholdCrossings
- thresholdConfigVersion
- updatedAt

WeatherWatchState
- tripPlanId
- latestSnapshotId?
- alertBaselineSnapshotId?
- lastNotifiedSnapshotId?
- lastSuccessfulCheckAt?
- updatedAt

WeatherWatchCheck
- id
- tripPlanId
- scheduledFor
- startedAt?
- completedAt?
- status
- trigger // T60 | T24 | T8 | T2 | MANUAL
- notificationSent
- errorCode?
```

**Vietato** aggiungere un singolo enum `ForecastSnapshot.role`.

## 5.9 Notification permission

Su Android 13+ richiedere `POST_NOTIFICATIONS` **solo quando l'utente attiva Weather Watch** e dopo aver spiegato il valore.

Se negato:

- Weather Watch può mantenere schedule/check locali se ha ancora valore nella UI;
- mostrare chiaramente che gli alert push locali non potranno essere visualizzati;
- non aprire automaticamente Settings di sistema senza azione esplicita.

## 5.10 Notification content

Una sola notifica aggregata:

```text
TrekCheck updated — Monte Velino
Conditions changed since your last check:
• wind +18 km/h in the upper section
• minimum temperature -4 °C
• rain now expected after 13:00
```

CTA: `View updated TrekCheck`.

## 5.11 Error handling

- provider unavailable → retry WorkManager con backoff;
- forecast fuori orizzonte → nessun falso “no changes”;
- permission negata → check loggato, niente notification;
- dati non comparabili → ignorare quella metrica e registrare reason code;
- trip nel passato → cancel schedule;
- device force-stopped → documentare comportamento best-effort.

## 5.12 Acceptance criteria

- nessun polling continuo;
- massimo una notifica per check;
- categoria OFF non notifica;
- Small/Medium/Large testati ai boundary;
- variazioni cumulative rilevate;
- baseline invalidata quando cambia la timeline del trip;
- no 1h-vs-6h precipitation delta;
- schedule cancellata alla cancellazione trip;
- niente claim safety-critical.

---

# 6. PR-PREM-004 — Smart Departure Window

## 6.1 Scopo

Permettere all'utente di confrontare **più orari o giorni di partenza** sullo stesso GPX e capire come cambiano meteo, ETA, luce residua e condition flags.

La feature risponde alla domanda:

> **“Se parto prima o scelgo il giorno successivo, quali condizioni incontrerò?”**

È una funzione di **decision support**, non un suggerimento safety-critical.

## 6.2 Modalità MVP Premium

Supportare due modalità:

### A. Compare times — stesso giorno

- 2–4 orari selezionabili;
- preset proposti intorno all'orario corrente del TripPlan, es. `-2h`, `base`, `+2h`;
- possibilità di impostare manualmente gli orari;
- tutti gli scenari devono restare nello stesso `ZoneId` del percorso.

### B. Compare days

- confronto tra massimo 2 giorni nell'MVP Premium;
- l'utente seleziona data + ora per ogni scenario;
- entrambi devono ricadere nell'orizzonte disponibile del provider.

## 6.3 Motore di simulazione

Non duplicare il core. Per ogni scenario:

```text
same Route
+ scenario departureInstant
+ same pace profile
→ ETA engine
→ route timing points
→ weather temporal matching
→ daylight
→ condition engine
→ scenario summary
```

La route geometry e le metriche statiche non vengono ricalcolate se non necessario.

### Riutilizzo weather cache

Una response MET contiene una timeseries per località. Con route sample spaziali equivalenti, simulare più orari deve riusare quanto possibile la stessa raw cache/BFF response.

**Non** fare una chiamata provider per ogni scenario se i dati necessari sono già nella cache/timeseries.

## 6.4 Output UI

Tabella comparativa minima:

| Metrica | 06:00 | 08:00 | 10:00 |
|---|---:|---:|---:|
| Fine prevista | 13:40 | 15:40 | 17:40 |
| Temp min/max | 7/18°C | 9/21°C | 11/24°C |
| Vento max | 22 km/h | 28 km/h | 38 km/h |
| Pioggia | 0 mm | 0,8 mm | 3,5 mm |
| Margine dal tramonto | +4h | +2h | +20m |
| Condition flags | 0 | 1 | 3 |

Ogni colonna apre il briefing dettagliato dello scenario senza alterare il TripPlan principale finché l'utente non sceglie `Usa questo orario`.

## 6.5 Scenario ranking

TrekCheck può evidenziare:

> **“Condizioni più favorevoli tra gli scenari confrontati.”**

Non usare:

- `best/safest time` come claim assoluto;
- `safe to go`;
- punteggi di sicurezza.

Il ranking deve essere deterministico e trasparente. Versione iniziale:

1. severity dei condition flags;
2. presenza/quantità pioggia durante ETA;
3. vento massimo;
4. heat/cold severity;
5. daylight margin.

Definire `DepartureScenarioScorePolicy(version=1)` con pesi configurabili nel codice/config, testati e documentati. L'UI deve permettere di vedere **perché** uno scenario risulta più favorevole.

## 6.6 Modello dati

Gli scenari possono essere ephemeral; non richiedono Room finché l'utente non ne seleziona uno.

```kotlin
data class DepartureScenario(
    val id: String,
    val departureInstant: Instant,
    val routeZoneId: ZoneId
)

data class DepartureScenarioResult(
    val scenario: DepartureScenario,
    val estimatedFinish: Instant,
    val summary: WeatherSummary,
    val conditionFlags: List<ConditionFlag>,
    val daylightMargin: Duration?,
    val score: ScenarioScore
)
```

## 6.7 Edge cases

- scenario fuori provider horizon → non eseguibile, indicare quando il meteo sarà disponibile;
- scenario oltre forecastAvailableUntil → parziale/non disponibile;
- timezone DST boundary → usare sempre `Instant` + `ZoneId`;
- dati provider con risoluzione diversa → mostrare forecast resolution;
- scenario con arrivo dopo sunset → condition flag, non blocco.

## 6.8 Acceptance criteria

- massimo 4 scenari time-based nel primo rilascio;
- nessun duplicate provider fetch evitabile;
- selezionare uno scenario aggiorna il TripPlan solo dopo conferma;
- ranking spiegabile e deterministico;
- nessun claim safety-critical;
- compare days rispetta provider horizon.

---

# 7. PR-PREM-005 — Pack Weight Calculator

## 7.1 Scopo

Trasformare la checklist in uno strumento pratico per capire **quanto pesa realmente lo zaino** e quali elementi suggeriti dal TrekCheck devono ancora essere considerati.

La feature non prescrive quanta acqua o quante calorie una persona “deve” assumere. È un **calculator logistico**, non sanitario.

## 7.2 Funzioni

L'utente può:

- creare uno o più Gear/Pack Profile locali;
- aggiungere item con nome, categoria, peso, quantità;
- indicare item `carried`, `worn` o `consumable`;
- marcare item come già nello zaino;
- vedere peso totale;
- vedere subtotali per categoria;
- importare come suggerimenti gli item prodotti dal Checklist Engine del TrekCheck;
- salvare il pack associato a un TripPlan;
- duplicare un pack per una nuova escursione.

## 7.3 Categorie iniziali

- backpack/container;
- clothing;
- rain protection;
- insulation;
- navigation/electronics;
- lighting;
- first aid/safety;
- food;
- water;
- miscellaneous.

Categorie custom future sono ammesse ma non necessarie alla prima release.

## 7.4 Peso

Tipo canonico interno:

```text
grams: Long
```

Rendering:

- metric: g / kg;
- imperial: oz / lb.

Calcoli:

```text
baseWeight = carried non-consumable
consumablesWeight = food + water + other consumables
wornWeight = worn items
totalCarriedWeight = baseWeight + consumablesWeight
```

Il peso indossato è mostrato separatamente e non incluso nel totale zaino.

## 7.5 Integrazione con Checklist Engine

Il Checklist Engine Free produce raccomandazioni come:

```text
RAIN_SHELL_RECOMMENDED
INSULATION_LAYER_RECOMMENDED
HEADLAMP_RECOMMENDED
```

Il Premium Pack Calculator può mappare queste raccomandazioni a item/gear category e mostrare:

```text
Suggested by this TrekCheck
[ ] Rain shell — choose from my gear
[ ] Insulation layer — add custom item
```

Non aggiungere automaticamente peso senza scelta dell'utente.

## 7.6 Gear Library locale

Modello:

```text
GearItem
- id
- name
- category
- weightGrams
- defaultQuantity
- isConsumable
- notes?

PackProfile
- id
- name
- createdAt
- updatedAt

PackProfileItem
- packProfileId
- gearItemId
- quantity
- status // CARRIED | WORN
- packed // bool
```

Per un TripPlan può esistere un `TripPack` derivato/copied da un `PackProfile`.

Tutto locale; nessun account/cloud richiesto.

## 7.7 Acqua e cibo

L'utente può inserire manualmente:

- litri/ml di acqua, convertiti in peso con assunzione pratica `1 L ≈ 1 kg`;
- peso cibo.

TrekCheck **non** deve nell'ambito di questa feature:

- prescrivere “devi bere X litri”;
- diagnosticare rischio disidratazione;
- stimare fabbisogni clinici;
- certificare la disponibilità/potabilità di fonti d'acqua.

Un eventuale Hydration Planning avanzato richiede specifica separata.

## 7.8 UX

Nel briefing Premium:

```text
PACK
Total carried     5.4 kg
Base weight       3.1 kg
Consumables       2.3 kg
Worn              1.2 kg

Checklist match
✓ Shell packed
! Insulation suggested but not packed
✓ Headlamp packed
```

## 7.9 Acceptance criteria

- calcoli peso testati unitariamente;
- conversioni metric/imperial corrette;
- checklist suggestion non modifica pack senza conferma;
- nessun dato lascia il device;
- pack riutilizzabile tra TripPlan;
- nessun claim sanitario/idratativo.

---

# 8. PR-PREM-006 — Printable Safety & Emergency Card

## 8.1 Relazione con Share Card Free

Il Free Tier include già la **TrekCheck Share Card Free**: immagine sintetica con statistiche, meteo, daylight e branding.

Il Premium aggiunge un export più completo, pensato per:

- stampa;
- backup cartaceo;
- condivisione con familiari/amici;
- riepilogo strutturato prima della partenza.

Non deve essere presentato come sostituto di dispositivi SOS, PLB, navigazione o servizi di soccorso.

## 8.2 Output Premium

Supportare:

1. **PDF 1 pagina** come formato principale;
2. immagine ad alta risoluzione opzionale;
3. Android Sharesheet;
4. stampa tramite framework Android.

Generazione 100% locale.

## 8.3 Contenuto

### Header

- TrekCheck logo/name;
- route/trip name;
- date;
- timezone;
- generated-at timestamp.

### Route summary

- distanza;
- ascent/descent;
- quota min/max;
- departure time;
- expected finish;
- durata stimata.

### Map / geometry

- mappa semplificata della traccia con start/end;
- marker per pochi punti chiave/ETA;
- attribution map provider se vengono renderizzati tile;
- in alternativa, route schematic senza basemap per export completamente offline.

### Elevation profile

- profilo altimetrico;
- marker del punto più alto;
- eventuali marker ETA chiave.

### Timeline

Tabella compatta:

```text
08:00 Start       950 m   12°C
09:30 Point A    1500 m    9°C
11:10 High point 2200 m    5°C
14:40 Finish      960 m   16°C
```

### Weather summary

- min/max temperature;
- max wind;
- precipitation summary;
- main condition flags;
- forecast issue/freshness timestamp;
- data source attribution.

### Daylight

- sunrise/sunset se rilevanti;
- expected finish;
- daylight margin.

### Emergency / personal section

**Solo dati configurati dall'utente**, ad esempio:

```text
Emergency contact: Mario +39 ...
Planned return by: 17:00
Personal note: “Car parked at ...”
```

Non auto-popolare numeri di soccorso locali da database non validati nell'MVP Premium.

## 8.4 Privacy

Prima dell'export mostrare opzioni:

- include/exclude exact start/end coordinates;
- include/exclude personal emergency contact;
- include/exclude personal notes.

Default privacy:

- coordinates precise OFF;
- personal contact OFF finché non configurato;
- notes OFF.

La preview deve corrispondere al PDF effettivamente generato.

## 8.5 Implementazione Android

Approccio consigliato:

```text
SafetyCardUseCase
   ↓
SafetyCardDocumentModel
   ↓
PDF renderer locale
   ↓
app-private/cache file
   ↓
FileProvider / PrintManager / ACTION_SEND
```

Possibili tecnologie:

- Android `PdfDocument` per layout controllato;
- `PrintDocumentAdapter`/PrintManager per stampa;
- rendering vettoriale custom per elevation/route;
- evitare dipendenza da servizi cloud PDF.

Se la basemap richiede rete e non è disponibile:

- generare comunque il PDF con route schematic;
- non fallire l'intero export.

## 8.6 Branding

Footer consigliato:

> **Generated with TrekCheck — Run a TrekCheck before you go.**

Versione italiana localizzata.

## 8.7 Safety language

Usare:

- `planned`;
- `estimated`;
- `forecast`;
- `expected`.

Non usare:

- `safe route`;
- `guaranteed return`;
- `rescue plan`;
- `certified emergency card`.

## 8.8 Acceptance criteria

- PDF generabile offline da briefing salvato;
- route schematic fallback senza rete;
- preview/export coerenti;
- coordinate e dati personali esclusi per default;
- condivisione via FileProvider;
- stampa Android funzionante;
- attribution presente quando richiesto;
- nessun backend upload.

---

# 9. Architettura Premium

## 9.1 Moduli/package aggiuntivi

```text
app/
  billing/

core/
  entitlement/
  forecastchange/
  departurecompare/
  pack/
  export/

feature/
  paywall/
  weatherwatch/
  departurecompare/
  pack/
  safetycard/
```

Il core Free non deve importare feature package Premium. Le feature Premium dipendono dai core interfaces Free.

## 9.2 Dependency direction

```text
Free core/domain/data
        ↑
Premium domain capabilities
        ↑
Premium UI
```

Vietato:

```text
Free core → Premium feature
```

## 9.3 Feature gates

Definire capability centralizzate:

```kotlin
enum class PremiumCapability {
    UNLIMITED_WEATHER,
    WEATHER_WATCH,
    SMART_DEPARTURE_WINDOW,
    PACK_WEIGHT_CALCULATOR,
    SAFETY_PDF
}
```

L'UI chiama `EntitlementService.canUse(capability)`. Non spargere `if (isPremium)` arbitrari in decine di composable.

---

# 10. Persistenza Premium

Aggiunte Room/DataStore:

```text
WeatherWatchSettings
WeatherWatchState
WeatherWatchCheck

GearItem
PackProfile
PackProfileItem
TripPack / TripPackItem (se separati dai template)
```

Gli scenari Smart Departure restano ephemeral salvo selezione.

Gli export PDF/immagine sono temporanei e rigenerabili; non creare una tabella `ExportFile` nell'MVP Premium salvo necessità misurata.

---

# 11. Privacy Premium

Premium mantiene i principi Free:

- no account obbligatorio;
- no GPX completo sul cloud;
- no background location;
- Weather Watch device-side;
- gear/pack solo locale;
- Safety PDF generato localmente;
- soltanto coordinate weather sample possono raggiungere il BFF/provider.

Google Play Billing comunica con Google secondo il normale flusso Play; documentarlo nella privacy policy.

---

# 12. Google Play / Android

## Billing

Usare versione stabile corrente della Play Billing Library al momento dello sviluppo.

## Notifications

`POST_NOTIFICATIONS` richiesto solo quando l'utente Premium attiva Weather Watch, non durante onboarding generale.

## WorkManager

Usato solo per Weather Watch, con one-time work e vincolo rete.

## Files/export

Usare `FileProvider` per PDF/immagini condivise.

---

# 13. Test strategy Premium

## 13.1 Billing/entitlement

- purchase success;
- pending/cancelled;
- acknowledge;
- restore;
- offline cache;
- entitlement perso/ristabilito;
- paywall non altera dati Free.

## 13.2 Weather Watch

- category ON/OFF;
- Small/Medium/Large boundary;
- cumulative baseline;
- threshold crossing;
- 1h vs 6h precipitation mismatch;
- T-60/T-24/T-8/T-2 schedule;
- reschedule/cancel;
- notification permission denied;
- no false `no changes` on provider error.

## 13.3 Smart Departure Window

- 2/3/4 same-day scenarios;
- compare days;
- DST;
- scenario outside horizon;
- selection commits only after confirm;
- cache reuse;
- deterministic ranking.

## 13.4 Pack Weight

- grams canonical;
- quantity;
- carried vs worn;
- consumable totals;
- metric/imperial;
- duplicate profile;
- checklist suggestions;
- no automatic mutation.

## 13.5 Safety Card

- PDF one-page layout;
- long route name;
- route without elevation;
- route-only/weather pending;
- privacy toggles;
- FileProvider;
- offline schematic fallback;
- IT/EN.

---

# 14. Effort incrementale stimato

| Feature | Ore indicative |
|---|---:|
| Billing + entitlement/paywall | 8–14 |
| Weather Watch | 25–40 |
| Smart Departure Window | 12–20 |
| Pack Weight Calculator | 18–30 |
| Premium Safety & Emergency Card | 12–20 |
| integrazione/polish/test trasversali | 12–20 |

Totale incrementale indicativo sopra MVP Free:

**~87–144 ore**.

Le stime non includono eventuale redesign globale o backend server-side Weather Watch.

---

# 15. Ordine di implementazione consigliato

1. Billing + `EntitlementRepository`;
2. unlimited weather gate;
3. Weather Watch;
4. Smart Departure Window;
5. Printable Safety & Emergency Card;
6. Pack Weight Calculator;
7. hardening e store conversion UX.

Motivazione:

- Weather Watch e Smart Departure sfruttano il core meteo/ETA già esistente;
- Safety Card ha alto valore percepito e costo tecnico relativamente basso;
- Pack Calculator introduce un piccolo nuovo dominio dati, quindi può arrivare dopo.

---

# 16. Metriche Premium

Raccogliere solo analytics non sensibili se/Quando vengono introdotti:

- paywall viewed;
- purchase started/completed;
- Weather Watch enabled;
- Smart Departure used;
- scenario selected;
- Pack Calculator used;
- Safety Card exported;
- notification opened.

Mai inviare:

- coordinate;
- route name;
- GPX;
- gear item names personali;
- emergency contact data.

---

# 17. Decisioni vincolanti per sviluppo agentico

1. Premium dipende dal core MVP, non viceversa.
2. Nessun fork di ETA/weather/condition engine.
3. Entitlement centralizzato in repository/service.
4. Weather Watch usa WorkManager one-time, non exact alarms/polling.
5. `ForecastSnapshot` non ha enum `role`.
6. Smart Departure riusa weather cache/timeseries.
7. Ranking scenari è deterministico e spiegabile.
8. Pack Weight non prescrive fabbisogni idrici/sanitari.
9. Share Card Free resta disponibile anche senza Premium.
10. Safety PDF Premium è generato localmente.
11. Dati personali nell'export sono opt-in.
12. Nessun claim `safe/unsafe/go`.
13. Ogni nuova feature Premium richiede test di entitlement + test senza rete quando applicabile.

---

# 18. ADR Premium iniziali

- ADR-P001 Lifetime Pro via Google Play Billing;
- ADR-P002 Capability-based entitlement gate;
- ADR-P003 Weather Watch device-side WorkManager;
- ADR-P004 Forecast snapshot roles via WeatherWatchState references;
- ADR-P005 Smart Departure scenario engine reuses Free core;
- ADR-P006 Pack data local-only and grams canonical;
- ADR-P007 Safety PDF local generation + privacy opt-in.

---

# 19. Definition of Done Premium

Una feature Premium è Done solo se:

- capability gate implementato;
- comportamento Free non regredisce;
- entitlement offline/restore testato quando rilevante;
- acceptance criteria superati;
- test unit/integration/UI aggiunti;
- stringhe IT/EN;
- accessibility minima;
- privacy impact valutato;
- no coordinate/PII in telemetry;
- offline/failure state definito;
- baseline aggiornata se cambia il contratto.

---

# 20. Fuori scope Premium 1.0

Non includere senza nuova approvazione:

- multi-day trekking;
- Wear OS;
- water-source discovery;
- hydration prescription;
- terrain/solar shadow analysis;
- server-side Weather Watch/FCM;
- adaptive scheduler;
- official warning aggregation;
- AI natural-language summary;
- cloud sync/account.

---

# 21. Branding e copy Premium

Nome: **TrekCheck**.

Motto:

> **Run a TrekCheck before you go.**  
> **Fai un TrekCheck prima di partire.**

Copy feature suggerita:

- `Weather Watch — TrekCheck keeps watching until you leave.`
- `Compare departure times.`
- `Build your pack.`
- `Export your TrekCheck.`

Usare sempre formulazioni di supporto alla decisione, non certificazioni di sicurezza.

---

# 22. Changelog

### Premium 1.0 — 2026-10-13

- separata la baseline Premium dalla v0.5 monolitica;
- mantenute le capability già pianificate: unlimited weather/refresh + Weather Watch;
- aggiunto **Smart Departure Window**;
- aggiunto **Pack Weight Calculator**;
- diviso l'export: Share Card semplice nel Free, **Printable Safety & Emergency Card** completa nel Premium;
- mantenuti privacy-first, local-first e basso costo backend come vincoli architetturali.
