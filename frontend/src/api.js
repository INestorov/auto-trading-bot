const API = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function getStatus() {
    const r = await fetch(`${API}/api/bot/status`);
    if (!r.ok) throw new Error("status failed");
    return r.json();
}

export async function startBot(payload) {
    const r = await fetch(`${API}/api/bot/start`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    });
    if (!r.ok) throw new Error(await r.text());
}

export async function pauseBot() {
    const r = await fetch(`${API}/api/bot/pause`, { method: "POST" });
    if (!r.ok) throw new Error("pause failed");
}

export async function resetBot(mode, symbol) {
    const r = await fetch(`${API}/api/bot/reset?mode=${encodeURIComponent(mode)}&symbol=${encodeURIComponent(symbol)}`, {
        method: "POST",
    });
    if (!r.ok) throw new Error("reset failed");
}

export async function getTrades(mode, symbol, limit = 200) {
    const r = await fetch(`${API}/api/trades?mode=${mode}&symbol=${symbol}&limit=${limit}`);
    if (!r.ok) throw new Error("trades failed");
    return r.json();
}

export async function getSnapshots(mode, symbol, limit = 1000) {
    const r = await fetch(`${API}/api/portfolio/snapshots?mode=${mode}&symbol=${symbol}&limit=${limit}`);
    if (!r.ok) throw new Error("snapshots failed");
    return r.json();
}

export async function getCandles(symbol, interval, limit = 500) {
    const r = await fetch(`${API}/api/market/candles?symbol=${symbol}&interval=${interval}&limit=${limit}`);
    if (!r.ok) throw new Error("candles failed");
    return r.json();
}