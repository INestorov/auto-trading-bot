# Reflection & Design Decisions

## Overview

This project implements a simplified automated trading bot consisting of:

- **Backend:** Spring Boot + PostgreSQL
- **Frontend:** React + Vite + Recharts

The primary objective was to design a system that is:
- functionally correct
- debuggable under time pressure

The architecture intentionally favors clarity and explicit behavior over heavy abstraction or framework magic.

---

## Backend Architecture

### Layered Design

The backend follows a clear layered structure:

- **Controllers**
    - Handle HTTP routing and request/response mapping only
- **Services (`BotEngine`, `LiveBotRunner`)**
    - Contain trading logic, backtesting logic, and orchestration
- **Repositories (JDBC)**
    - Execute SQL statements and map rows to Java objects
- **Database (PostgreSQL)**
    - Stores account state, trades, positions, and portfolio snapshots

This separation ensures:
- business logic does not leak into controllers
- persistence logic is explicit and inspectable
- components are independently testable

---

## Why Java `record` Was Used

### Where Records Are Used
Java records are used for:
- DTOs (API request/response objects)
- Repository return models (Account, Trade, Snapshot, Position)

### Why Records Are a Good Fit
Records are ideal when an object:
- represents a **data snapshot**
- is **immutable**
- has **no behavior**
- maps directly to a database row or API payload

Using records:
- eliminates boilerplate (constructors, getters, equals/hashCode)
- enforces immutability by default
- makes intent explicit: these objects carry data only

### When Records Were Not Used
Records were intentionally **not** used for:
- services (`BotEngine`)
- components with mutable state
- objects with lifecycle or behavior

Business logic and state transitions live in services, not data objects.

---

## Why PostgreSQL `TIMESTAMPTZ` Was Used

### The Problem
Trading systems are highly time-dependent. Using naive timestamps can cause:
- incorrect ordering
- timezone-related bugs
- inconsistent frontend charts

### The Solution: `TIMESTAMPTZ`
`TIMESTAMPTZ` stores an absolute point in time rather than a local date/time.

Benefits:
- consistent behavior across environments
- safe for distributed systems
- maps cleanly to Java `OffsetDateTime`
- prevents timezone ambiguity

---

## Why `OffsetDateTime` in Java

`OffsetDateTime` was chosen over `LocalDateTime` because:
- it preserves timezone offset
- it represents an exact instant in time
- it serializes cleanly to ISO-8601 for APIs

This aligns naturally with PostgreSQL `TIMESTAMPTZ` and avoids subtle bugs when rendering time-series data.

---

## Why `BigDecimal` for Prices, Quantities, and Balances

Financial calculations require precision.

Using floating-point numbers (`double`) can introduce:
- rounding errors
- cumulative drift
- incorrect PnL calculations

Therefore:
- PostgreSQL uses `NUMERIC`
- Java uses `BigDecimal`

This ensures deterministic, precise calculations for:
- quantities
- prices
- fees
- portfolio value

---

## Trading Logic Design

### Separation of State and History

The system distinguishes between:
- **State tables**
    - account cash
    - current position
- **History tables**
    - trades
    - portfolio snapshots

This enables:
- fast reads for UI
- clear auditability
- equity curves without recomputation

Snapshots are written on every trading step to allow time-series visualization.

---

## Train vs Live Mode

### TRAIN Mode
- Uses historical candles
- Runs synchronously
- Writes trades and snapshots in a loop

### LIVE Mode
- Driven by a scheduled task (`@Scheduled`)
- Polls latest market price
- Executes strategy incrementally

Scheduling is explicitly enabled using `@EnableScheduling`.

---

## Frontend Design Choices

### React + Vite
Chosen for:
- fast development iteration
- minimal configuration
- modern tooling

### Recharts
Used for:
- responsive charts
- time-series visualization
- quick integration

### Important Charting Decision: Numeric X-Axis
Initially, formatted timestamp strings were used as X-axis values.  
This caused tooltip issues because multiple snapshots shared identical string values.

Correct approach:
- use numeric epoch timestamps for X-axis
- format time only in tooltips

This ensured accurate hover behavior and chart updates.

---

## CORS Considerations

Frontend runs on `localhost:5173` and backend on `localhost:8080`.  
CORS is explicitly configured to allow development requests.

This avoids proxy magic and keeps API behavior explicit.

---
## Why SMA and RSI Were Chosen

### Design Goal
The objective of the trading strategy was **clarity, interpretability, and correctness**, not signal optimization or maximum profitability.

Given the limited timeframe, I intentionally chose indicators that are:
- widely understood
- easy to implement correctly
- suitable as a baseline strategy

---

### Why SMA (Simple Moving Average)

**Simple Moving Averages** were used to identify trend direction and regime changes.

In this system:
- a **fast SMA** reacts to recent price changes
- a **slow SMA** represents the broader trend
- a crossover indicates a potential shift in market direction

SMA was chosen instead of EMA because:
- SMA logic is transparent and easy to verify
- it avoids additional weighting complexity
- it makes debugging and reasoning about trades simpler

For a take-home or interview project, SMA provides a clean and explainable trend signal without hidden behavior.

---

### Why RSI (Relative Strength Index)

SMA crossovers alone can generate:
- late entries
- false signals in ranging markets
- excessive trades during noise

RSI was added as a **momentum and overbought/oversold filter**.

In this implementation:
- buying is avoided when RSI indicates overbought conditions
- selling is encouraged when momentum becomes excessive

RSI helps reduce low-quality trades without introducing complex dependencies or additional state.

---

### Why Use SMA and RSI Together

The combination provides complementary signals:

| Indicator | Purpose |
|---------|--------|
| SMA crossover | Identifies *trend direction* |
| RSI | Filters *momentum exhaustion* |

This creates a simple but robust decision model:
- SMA answers **“Is the market trending?”**
- RSI answers **“Is it too late to enter?”**

The result is a strategy that is:
- stable under different market conditions
- suitable as a foundation for further extensions

---

### Intentional Simplicity

The strategy is not meant to be optimal or production-ready.
It serves as a **baseline framework** that can later be extended with:
- stop-loss / take-profit logic
- volatility filters
- dynamic position sizing
- additional indicators

The emphasis was on correctness, explainability, and architectural clarity rather than performance tuning.

## Tradeoffs and Future Improvements

### What Could Be Improved
- unify LIVE and TRAIN strategy logic
- candle-based LIVE execution instead of price polling
- stop-loss / take-profit support
- pagination and filtering on history endpoints
- aligned BUY/SELL markers on charts

### Why the System Was Not Over-Engineered
Given the deadline, the focus was on:
- correctness
- clarity
- explainability

The result is a system where failures are understandable and traceable, which is valuable both in production debugging and technical interviews.

---

## Conclusion

All design choices were made deliberately to:
- reduce ambiguity
- minimize hidden behavior
- keep business logic explicit
- favor correctness over abstraction

This approach produces a system that is easy to reason about, extend, and explain.
