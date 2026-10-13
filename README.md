# Market Journal AI

**Market Journal AI** is an automated Indian stock market research and swing trading analysis tool built natively for Android.

Operating entirely locally on the Android device without requiring an external self-hosted backend, the application collects permitted public market data, calculates quantitative technical indicators, filters liquidity, identifies high-conviction swing setups, monitors catalysts via financial RSS feeds, tracks forward session performance (T+1 to T+120), and generates structured research reports.

---

## Target Device Specifications
- **Hardware Target**: Samsung Galaxy Note 20 Ultra 5G (Android 11+, API 30 to 36, 12 GB RAM, 256 GB storage).
- **Display & Usability**: Full edge-to-edge support with Material 3 dynamic surfaces and accessible touch targets (≥48dp).

---

## Architecture & Technology Stack
- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM with unidirectional data flow (Coroutines & StateFlow)
- **Local Persistence**: Room / SQLite (Stocks, Candles, Indicator Snapshots, Scans, News, Performance, Job Logs)
- **Networking**: Retrofit, OkHttp, XmlPullParser
- **Background Orchestration**: WorkManager with network constraints, battery checks, and IST session awareness
- **AI Module**: Gemini 3.5 Flash direct REST integration with offline rule-based fallback

---

## Core Modules & Features

### 1. Modular Market Data Provider
- **Bundled NSE Historical Dataset (DEMO)**: Offline, authentic 90-day daily candle series for 15 liquid NSE equities and the Nifty 50 benchmark (`^NSEI`), clearly labelled `DEMO`.
- **Yahoo Finance Direct**: Real-time HTTP queries for NSE `.NS` tickers (`RELIANCE.NS`, `TCS.NS`, etc.) and `^NSEI` with 15-minute delay disclosures.
- **CSV Import/Export**: Import custom Bhavcopy files or standard OHLCV CSVs directly into the local SQLite database.

### 2. Technical Scanner
- **RSI**: 14-period standard Wilder's smoothing.
- **MACD**: 12 Fast, 26 Slow, 9 Signal EMA difference and histogram.
- **EMAs**: 20 EMA and 50 EMA trend alignment.
- **ATR**: 14-period Average True Range for dynamic volatility stop-losses.
- **Volume & RVOL**: 20-day SMA volume and Relative Volume surge metrics.
- **Support & Resistance**: 20-day swing pivots, highest high, lowest low.
- **Relative Strength**: Stock % return versus Nifty 50 benchmark over 20 and 50 sessions.
- **Strategies**:
  - **20-Day Breakout**: Close breaks 20-day high with RVOL ≥ 1.35x and positive relative strength.
  - **EMA Pullback**: Uptrend with price pulling back to 20 EMA on dry volume with RSI 40-55.
  - **Momentum**: Expanding EMAs, positive MACD histogram, and RSI > 58.
- **Candidate Scoring**: 0 to 100 transparent composite score (Trend 25, Momentum 20, Volume 20, RS 20, R:R 15).

### 3. Forward Performance Tracking (Zero Look-Ahead Bias)
- Tracks every candidate, including losing trades.
- Evaluates performance across **T+1, T+3, T+5, T+10, T+20, T+60, and T+120** trading sessions.
- Computes Max Favorable Excursion (MFE) and Max Adverse Excursion (MAE).

### 4. Risk & Position Sizing Calculator
- Inputs: Trading Capital (₹) and Max Allowed Risk % (e.g., 1.5%).
- Computes: Recommended Shares, Capital Utilization, Stop Loss in ₹, Target 1 & Target 2 in ₹, and Risk-to-Reward ratio.

### 5. Historical Backtest Lab
- Simulates Breakout, Pullback, and Momentum strategies over historical bars.
- Models Indian transaction costs: **STT (0.1%), Brokerage (₹20), GST, Exchange Turnover, Stamp Duty, and Execution Slippage (0.1%)**.
- Outputs: Win Rate %, Profit Factor, Net Total Return %, and Max Drawdown %.

### 6. Background Jobs & Research Schedule
Configured via `WorkManager` for Asia/Kolkata (IST):
- **08:45 AM**: Overnight news check
- **09:30 AM**: Market opening scan
- **12:30 PM**: Midday pullback scan
- **04:15 PM**: End-of-Day report
- **08:30 PM**: Evening news digest
- Automatically checks for weekends (Saturday/Sunday) and market holidays.

### 7. Optional Gemini AI Explainer
- Synthesizes technical indicators, market structure, and catalysts into a 3-part swing trading research note.
- Secure key configuration via `.env.example` or the in-app Settings screen.
- Functions seamlessly offline with deterministic rule-based rationales if Gemini is unconfigured or unavailable.

---

## Installation & Setup

1. **Prerequisites**: Android Studio Ladybug / Meerkat or later, JDK 17 / 21.
2. **Build Configuration**:
   ```bash
   gradle assembleDebug
   ```
3. **Run Unit Tests**:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. **Gemini API Key (Optional)**:
   - Configure `GEMINI_API_KEY` in the AI Studio Secrets panel or `.env`, or input it directly in **Settings > Gemini API Configuration**.

---

## Statutory Disclaimer
*Market Journal AI is an educational research and study tool. It does not provide financial or investment recommendations and contains no brokerage connectivity. Candidate scores reflect mathematical setup rules, NOT a probability of future profit. Always manage risk strictly.*
