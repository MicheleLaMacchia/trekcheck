# Riconciliazione — baseline Premium e PRD MVP Free

Fonte confrontata: `docs/trekcheck-product-technical-baseline-Premium.md` con `prd.md` e `addendum.md` nella stessa cartella. Il PRD ha perimetro Free: le specifiche dettagliate Premium restano rinviate e non sono requisiti da introdurre nell'MVP.

## Elementi coerenti

- Il PRD esclude dall'MVP Billing, Weather Watch, confronto partenze, calcolo zaino e scheda PDF Premium; la Share Card Free rimane locale e disponibile entro il suo perimetro.
- Il Premium è presentato come estensione successiva del core Free, con prezzo non deciso; la quota Free è un'ipotesi da validare e l'assenza di quota applicativa Premium non elimina limiti di provider o cache.
- I principi di privacy, assenza di navigazione/tracciamento e linguaggio senza garanzie di sicurezza sono coerenti con la baseline Premium.

## Lacune concrete da valutare per architettura o PRD Premium

1. La baseline richiede esplicitamente che il core Free **non dipenda dalle feature Premium** e che i motori GPX, ETA, meteo, luce, condizioni, checklist, persistenza e fusi non siano duplicati. Il PRD menziona una «estensione sul core Free», ma non specifica direzione delle dipendenze né divieto di fork. Proposta: preservare questi vincoli nel futuro documento di architettura; non richiedono implementare Premium nell'MVP.
2. La baseline vieta un paywall separato per il salvataggio locale e mantiene la Share Card Free senza Premium. Il PRD prevede salvataggio e Share Card per il Free, ma non esplicita il vincolo per un futuro Premium. Proposta: annotarlo nella progettazione del Premium, senza anticipare una scelta commerciale ulteriore o alterare l'attuale perimetro Free.
3. La baseline descrive acquisto una tantum, entitlement centralizzato e ripristino offline; il PRD rimanda acquisti e prezzo, coerentemente con l'ambito MVP e con il vincolo di spesa zero. Questi dettagli sono ipotesi della baseline da riesaminare in un futuro PRD Premium, non decisioni nuove o lavoro necessario adesso.

Le specifiche su scheduling, confronto scenari, zaino e PDF sono intenzionalmente escluse dall'MVP e restano nella baseline Premium. Nessun contrasto sostanziale rilevato sul perimetro attuale.
