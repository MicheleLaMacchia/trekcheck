---
name: TrekCheck UX MVP
status: approved
created: 2026-11-21
updated: 2026-11-24
description: Proposta visuale Android Compose per pianificare un'escursione con informazioni leggibili e trasparenti.
sources:
  - ../../prds/prd-TrekCheck-2026-11-20/prd.md
  - ../../briefs/brief-TrekCheck-2026-11-20/brief.md
colors:
  primary: '#245C48'
  on-primary: '#FFFFFF'
  surface: '#FAFAF5'
  on-surface: '#19231E'
  surface-container: '#E8EEE8'
  outline: '#56675C'
  error: '#9B2424'
  warning: '#705500'
typography:
  title:
    note: 'Material 3 headlineSmall, carattere di sistema'
  body:
    note: 'Material 3 bodyLarge, carattere di sistema'
  label:
    note: 'Material 3 labelLarge, carattere di sistema'
rounded:
  sm: '4dp'
  md: '12dp'
  lg: '20dp'
spacing:
  '1': '4dp'
  '2': '8dp'
  '3': '12dp'
  '4': '16dp'
  '6': '24dp'
components:
  primary-action:
    background: '{colors.primary}'
    foreground: '{colors.on-primary}'
    rounded: '{rounded.md}'
  summary-card:
    background: '{colors.surface-container}'
    foreground: '{colors.on-surface}'
    rounded: '{rounded.md}'
---

# DESIGN — TrekCheck

**Stato:** proposta autonoma, non validata con utenti e non ancora approvata come identità di marca. Deriva da Material 3 / Jetpack Compose; i token sono candidati per il tema chiaro, da verificare su dispositivi reali. Il tema scuro userà i ruoli Material con contrasto verificato, non inversioni automatiche. Per le interazioni vedere [EXPERIENCE.md](EXPERIENCE.md). I due documenti prevalgono sui mockup in caso di differenze.

## Brand & Style

Voce calma e concreta; informazioni escursionistiche senza estetica di allarme o promessa di sicurezza. [ASSUNZIONE] Verde bosco discreto, superfici chiare e gerarchia sobria; nessuna immagine decorativa necessaria per comprendere i dati. Visual e logo definitivi rimangono aperti.

## Colors

{colors.primary} identifica azioni principali; {colors.surface} e {colors.on-surface} sostengono testo e contenuti; {colors.surface-container} raggruppa riepiloghi; {colors.error} segnala errori e {colors.warning} segnala attenzione. Nessun significato basato solo sul colore: affiancare etichetta e icona. Testare contrasto testo normale almeno 4,5:1 e testo grande 3:1; verificare separatamente componenti, stato disabilitato e tema scuro prima della consegna.

## Typography

Usare i ruoli {typography.title}, {typography.body} e {typography.label} di Material 3 con font di sistema. Ordine di lettura coerente con la gerarchia; numeri sempre accompagnati da unità. Adattare il layout alla dimensione carattere impostata dal sistema senza tagli; lingua IT/EN senza fissare larghezze ai testi.

## Layout & Spacing

Una colonna verticale scorrevole per i telefoni, margine {spacing.4}, spazi {spacing.2}–{spacing.6}. Riepilogo in alto e dettagli sotto, scelta confermata. Allargare e limitare la larghezza del contenuto su schermi grandi; non nascondere azioni su landscape. Le visualizzazioni mappa e profilo hanno alternative testuali.

## Elevation & Depth

Raggruppare con superfici e contorni; usare elevazione Material 3 leggera solo per distinguere componenti interattivi o livelli. Nessun dato di stato affidato a sola ombra.

## Shapes

{rounded.sm} per messaggi compatti, {rounded.md} per card e azioni, {rounded.lg} per contenitori importanti, coerenti con Material 3. Raggi e densità sono proposte, non requisiti di marca confermati.

## Components

- **Azione primaria** {components.primary-action}: una per schermata dove possibile, etichetta con verbo, target minimo 48 × 48 dp, stati abilitato/disabilitato/progresso distinguibili.
- **Riepilogo** {components.summary-card}: titolo, valori con unità, qualificatori «stimato» e ultimo aggiornamento; contenuto testuale leggibile anche senza colore.
- **Avviso**: intestazione, spiegazione e azione quando disponibile; errori e dati incompleti non condividono un'icona ingannevole di successo.
- **Punto timeline**: ora e fuso, distanza, quota e meteo se disponibili, alternativa testuale e collegamento alla mappa.
- **Mappa/profilo**: indicare fonte e attribuzione ove richiesta; fallback testuale se mappa non caricata.

## Do's and Don'ts

- Mostrare sempre «stimato», data/ora dell'aggiornamento meteo e i dati mancanti.
- Mantenere visibile lo stato offline e il briefing salvato quando il refresh fallisce.
- Non usare «sicuro», semafori rassicuranti o precisione numerica non dimostrata.
- Non rendere un'icona, colore, gesto o mappa l'unico modo per capire o operare.
