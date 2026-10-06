# Portale Finanziario Familiare — Linea Saldo nel Flusso Personale ed Etichette Mesi MM/YY

Piano d'intervento per integrare la linea del saldo progressivo nel grafico del flusso mensile personale e formattare gli assi temporali in notazione `MM/YY` (es. `10/26`).

---

## User Review & Confirmed Decisions

> [!IMPORTANT]
> **Requisiti dell'Utente**:
> 1. **Linea del Saldo nel Flusso Mensile Personale**:
>    - Aggiunta della curva del **Saldo Cumulativo/Progressivo** per il budget personale nel tempo.
>    - Inserimento dell'indicatore nella Legenda e nel tooltip interattivo del grafico.
> 2. **Etichette Mesi sotto al Grafico in Notazione `MM/YY`**:
>    - Formattazione dell'asse X dei grafici temporali nel formato `MM/YY` (es. `10/26` per ottobre 2026, `09/26` per settembre 2026).

---

## 1. Technical Architecture & Modifications

- **`MonthlyFlowPoint.kt` / `FinancialViewModel.kt`**:
  - Aggiornamento della data class `MonthlyFlowPoint`:
    - Campo `balance: Double = 0.0` per rappresentare il saldo accumulato.
  - Aggiornamento della funzione `formatMonthLabel(ymKey)`:
    - Conversione da `"2026-10"` a `"10/26"` (`parts[1] + "/" + parts[0].takeLast(2)`).
  - Calcolo progressivo del saldo personale in `personalMonthlyFlow`:
    - Calcolo del saldo cumulativo mese per mese ($\text{Saldo Accumulato} = \sum (\text{Entrate} - \text{Uscite})$).
- **`FlowLineChart.kt`**:
  - Aggiunta del quadratino in legenda per il "Saldo" (colore Viola/Teal `PrimaryViolet`).
  - Inserimento del valore del Saldo nel popover/tooltip informativo.
  - Disegno della curva continua per il saldo `drawCubicLine({ it.balance }, PrimaryViolet, 2.5f)`.

---

## Plan Verification & Deliverables

1. **Verifica Compilazione**: Esecuzione di `compile_applet` per confermare l'assenza di errori di sintassi.
2. **Verifica Grafica**: Visualizzazione delle etichette `MM/YY` sotto l'asse X e presenza della linea del Saldo con relativa legenda e tooltip.
