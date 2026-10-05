# Portale Finanziario Familiare — Regola Esplicita Calcolo Fondo Risparmio

Implementazione della regola esplicita per il calcolo dinamico del residuo del Fondo Risparmio basata sui movimenti registrati nelle categorie di risparmio.

---

## User Review & Confirmed Decisions

> [!IMPORTANT]
> **Regola Esplicita dell'Utente per il Fondo Risparmio**:
> 1. **Selezione Categorie**: Si considerano le transazioni reali non future (`!isFuture && date <= todayIso`) registrate su categorie contrassegnate come "Risparmio" (`useForSavings == true` o configurate in Impostazioni).
> 2. **Spesa su Categoria Risparmio** = **INCREMENTO (+) del Risparmio** (sommata al totale).
> 3. **Entrata su Categoria Risparmio** = **DECREMENTO (-) del Risparmio** (sottratta dal totale).
> 4. **Formula del Residuo Risparmio**:
>    $$\text{Fondo Risparmio} = \text{Risparmio Iniziale} + \sum \text{Spese (Risparmio)} - \sum \text{Entrate (Risparmio)}$$

---

## 1. Technical Architecture & Modifications

- **`FinancialViewModel.kt`**:
  - Calcolo dinamico di `savingsMovements` su tutte le transazioni concluse (`!it.isFuture && it.date <= todayIso`) appartenenti all'insieme delle categorie con `useForSavings == true`.
  - Somma delle `Spese` come depositi/incrementi (`totalDeposits`).
  - Somma delle `Entrate` come prelievi/decrementi (`totalWithdrawals`).
  - Aggiornamento reattivo del `currentSavingsFund`.

---

## Plan Verification & Deliverables

1. **Verifica Compilazione**: Esecuzione di `compile_applet` per verificare l'assenza di errori.
