import { useEffect, useMemo, useState } from "react";
import { getStatus, startBot, pauseBot, resetBot, getTrades, getSnapshots, getCandles } from "./api";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
} from "recharts";
import "./style.css";

const DEFAULT_SYMBOL = "BTCUSDT";
const DEFAULT_INTERVAL = "1m";

function isoNowMinusHours(h) {
  const d = new Date(Date.now() - h * 3600_000);
  return d.toISOString();
}

export default function App() {
  const [mode, setMode] = useState("TRAIN"); // TRAIN | LIVE
  const [symbol, setSymbol] = useState(DEFAULT_SYMBOL);

  const [interval, setIntervalValue] = useState(DEFAULT_INTERVAL);

  const [initialBalance, setInitialBalance] = useState(10000);
  const [riskPct, setRiskPct] = useState(0.1);

  // Train range (optional)
  const [fromIso, setFromIso] = useState(isoNowMinusHours(24));
  const [toIso, setToIso] = useState(new Date().toISOString());

  const [status, setStatus] = useState(null);
  const [candles, setCandles] = useState([]);
  const [trades, setTrades] = useState([]);
  const [snaps, setSnaps] = useState([]);
  const [error, setError] = useState("");

  async function refreshAll() {
    setError("");
    try {
      const [st, csRaw, trRaw, ssRaw] = await Promise.all([
        getStatus(),
        getCandles(symbol, interval, 500),
        getTrades(mode, symbol, 300),
        getSnapshots(mode, symbol, 2000),
      ]);

      setStatus(st);
      setCandles(Array.isArray(csRaw) ? csRaw : []);
      setTrades(Array.isArray(trRaw) ? trRaw : []);
      setSnaps(Array.isArray(ssRaw) ? ssRaw : []);
    } catch (e) {
      setError(String(e?.message ?? e));
    }
  }

  useEffect(() => {
    refreshAll();
    const id = window.setInterval(() => refreshAll(), 4000);
    return () => window.clearInterval(id);

  }, [mode, symbol, interval]);

  async function onStart() {
    setError("");
    try {
      await startBot({
        mode,
        symbol,
        interval,
        fromIso: mode === "TRAIN" ? fromIso : null,
        toIso: mode === "TRAIN" ? toIso : null,
        initialBalance,
        riskPct,
      });
      await refreshAll();
    } catch (e) {
      setError(String(e?.message ?? e));
    }
  }

  async function onPause() {
    setError("");
    try {
      await pauseBot();
      await refreshAll();
    } catch (e) {
      setError(String(e?.message ?? e));
    }
  }

  async function onReset() {
    setError("");
    try {
      await resetBot(mode, symbol);
      await refreshAll();
    } catch (e) {
      setError(String(e?.message ?? e));
    }
  }

  const equityData = useMemo(() => {
    return snaps
        .map((s, i) => {
          const when =
              s.ts ??
              s.snapshotTime ??
              s.snapshot_time ??
              s.time ??
              s.createdAt ??
              s.created_at ??
              s.executedAt ??
              s.purchased_at;

          const ms = when ? Date.parse(when) : NaN;

          return {
            // If timestamp is missing/unparseable, fall back to index so chart still works
            t: Number.isFinite(ms) ? ms : i,
            total: Number(s.totalValue ?? s.total ?? 0),
            cash: Number(s.cashBalance ?? s.cash ?? 0),
            pos: Number(s.positionValue ?? s.pos ?? 0),
          };
        })
        .sort((a, b) => a.t - b.t);
  }, [snaps]);

  const tradeMarkers = useMemo(() => {
    return trades.map((t) => {
      const when = t.executedAt ?? t.purchased_at ?? t.ts;
      return {
        purchased_at: when ? new Date(when).toLocaleString() : "",
        price: Number(t.price ?? 0),
        side: t.side,
      };
    });
  }, [trades]);

  const priceLineData = useMemo(() => {
    return candles.map((c) => ({
      purchased_at: c.openTime ? new Date(c.openTime).toLocaleString() : "",
      close: Number(c.close ?? 0),
    }));
  }, [candles]);

  const latest = equityData.length ? equityData[equityData.length - 1] : null;

  return (
    <div className="page">
      <header className="header">
        <div>
          <h1>Auto Trading Bot Dashboard</h1>
          <div className="sub">
            Mode: <b>{mode}</b> • Symbol: <b>{symbol}</b> • Interval: <b>{interval}</b>
          </div>
          {status && (
            <div className="sub">
              Running: <b>{String(status.running)}</b>
            </div>
          )}
        </div>

        <div className="controls">
          <label>
            Mode
            <select value={mode} onChange={(e) => setMode(e.target.value)}>
              <option value="TRAIN">TRAIN</option>
              <option value="LIVE">LIVE</option>
            </select>
          </label>

          <label>
            Symbol
            <select value={symbol} onChange={(e) => setSymbol(e.target.value)}>
              <option value="BTCUSDT">BTCUSDT</option>
              <option value="ETHUSDT">ETHUSDT</option>
            </select>
          </label>

          <label>
            Interval
            <select value={interval} onChange={(e) => setIntervalValue(e.target.value)}>
              <option value="1m">1m</option>
              <option value="5m">5m</option>
              <option value="15m">15m</option>
              <option value="1h">1h</option>
            </select>
          </label>

          <label>
            Initial Balance
            <input
              type="number"
              value={initialBalance}
              onChange={(e) => setInitialBalance(Number(e.target.value))}
            />
          </label>

          <label>
            Risk value (0, 1]
            <input type="number" step="0.01" value={riskPct} onChange={(e) => setRiskPct(Number(e.target.value))} />
          </label>

          {mode === "TRAIN" && (
            <>
              <label>
                From (ISO)
                <input value={fromIso} onChange={(e) => setFromIso(e.target.value)} />
              </label>
              <label>
                To (ISO)
                <input value={toIso} onChange={(e) => setToIso(e.target.value)} />
              </label>
            </>
          )}

          <div className="buttons">
            <button onClick={onStart}>Start</button>
            <button onClick={onPause}>Pause</button>
            <button onClick={onReset}>Reset</button>
          </div>
        </div>
      </header>

      {error && <div className="error">{error}</div>}

      <div className="grid">
        <section className="card">
          <h2>Portfolio Summary</h2>
          {latest ? (
            <div className="summary">
              <div>
                <span>Total</span>
                <b>{latest.total.toFixed(2)}</b>
              </div>
              <div>
                <span>Cash</span>
                <b>{latest.cash.toFixed(2)}</b>
              </div>
              <div>
                <span>Position Value</span>
                <b>{latest.pos.toFixed(2)}</b>
              </div>
            </div>
          ) : (
            <div className="muted">No snapshots yet.</div>
          )}
        </section>

        <section className="card">
          <h2>Equity Curve</h2>
          <div className="chart">
            <ResponsiveContainer width="100%" height={260}>
              <LineChart data={equityData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="t" type="number" hide />
                <Tooltip
                    formatter={(value) => Number(value).toFixed(2)}
                />
                <YAxis
                    domain={([dataMin, dataMax]) => {
                      const step = 100;

                      const min = Math.floor((dataMin - 200) / step) * step;
                      const max = Math.ceil((dataMax + 200) / step) * step;

                      return [min, max];
                    }}
                    tickCount={4}
                    tickFormatter={(v) => v.toLocaleString()}
                />
                <Line type="monotone" dataKey="total" dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="card wide">
          <h2>Price + Trades</h2>
          <div className="chart">
            <ResponsiveContainer width="100%" height={320}>
              <LineChart data={priceLineData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="purchased_at" hide />
                <YAxis
                    domain={([dataMin, dataMax]) => {
                      const step = 100;

                      const min = Math.floor((dataMin - 200) / step) * step;
                      const max = Math.ceil((dataMax + 200) / step) * step;

                      return [min, max];
                    }}
                    tickCount={4}
                    tickFormatter={(v) => v.toLocaleString()}
                />
                <Tooltip />
                <Line type="monotone" dataKey="close" dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/*<div className="chart" style={{ marginTop: 8 }}>*/}
          {/*  <ResponsiveContainer width="100%" height={160}>*/}
          {/*    <ScatterChart>*/}
          {/*      <CartesianGrid strokeDasharray="3 3" />*/}
          {/*      <XAxis dataKey="purchased_at" hide />*/}
          {/*      <YAxis dataKey="price" />*/}
          {/*      <Tooltip />*/}
          {/*      <Scatter name="Trades" data={tradeMarkers} />*/}
          {/*    </ScatterChart>*/}
          {/*  </ResponsiveContainer>*/}
          {/*</div>*/}
        </section>

        <section className="card wide">
          <h2>Trade History</h2>
          <div className="tableWrap">
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Action</th>
                  <th>Qty</th>
                  <th>Price</th>
                  <th>Fee</th>
                  <th>Realized P/L</th>
                </tr>
              </thead>
              <tbody>
                {trades.map((t) => {
                  const when = t.executedAt ?? t.purchased_at ?? t.ts;
                  return (
                    <tr key={t.id ?? `${t.side}-${when}-${t.price}`}>
                      <td>{when ? new Date(when).toLocaleString() : ""}</td>
                      <td>
                        <b>{t.side}</b>
                      </td>
                      <td>{Number(t.quantity ?? 0).toFixed(6)}</td>
                      <td>{Number(t.price ?? 0).toFixed(2)}</td>
                      <td>{Number(t.fee ?? 0).toFixed(4)}</td>
                      <td>{Number(t.realizedPnl ?? 0).toFixed(2)}</td>
                    </tr>
                  );
                })}
                {!trades.length && (
                  <tr>
                    <td colSpan="6" className="muted">
                      No trades yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}