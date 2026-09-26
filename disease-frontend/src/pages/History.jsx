import { useEffect, useState } from "react";
import owlDoctor from "../assets/OwlFixer.png";
import cloudMessage from "../assets/Cloud.png";

const API_BASE = import.meta.env.VITE_API_BASE || "http://127.0.0.1:8080";
function getAuthToken() {
  const direct = localStorage.getItem("token");
  if (direct) return direct;
  try {
    const raw = localStorage.getItem("auth_state_v1");
    if (raw) {
      const parsed = JSON.parse(raw);
      return parsed?.token || null;
    }
  } catch {}
  return null;
}
function getUsername() {
  try {
    const raw = localStorage.getItem("auth_state_v1");
    return raw ? JSON.parse(raw)?.user?.username ?? null : null;
  } catch {
    return null;
  }
}
async function apiGetHistory() {
  const token = getAuthToken();
  const username = getUsername();
  if (!username) throw new Error("Missing username. Please login again.");
  const res = await fetch(
    `${API_BASE}/api/history?username=${encodeURIComponent(username)}`,
    { headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } });
  const text = await res.text();
  if (!res.ok) throw new Error(text || `HTTP ${res.status}`);
  return text ? JSON.parse(text) : [];
}

async function apiGetCases(sessionId) {
  const token = getAuthToken();
  const username = getUsername();
  if (!username) throw new Error("Missing username. Please login again.");
  const res = await fetch(
    `${API_BASE}/api/history/${sessionId}/cases?username=${encodeURIComponent(username)}`,
    { headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } }
  );
  const text = await res.text();
  if (!res.ok) throw new Error(text || `HTTP ${res.status}`);
  return text ? JSON.parse(text) : [];
}

async function apiGetSessionKey(dbSessionId) {
  const token = getAuthToken();
  const username = getUsername();
  if (!username) throw new Error("Missing username. Please login again.");

  const res = await fetch(
    `${API_BASE}/api/history/${dbSessionId}?username=${encodeURIComponent(username)}`,
    { headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } }
  );

  const text = await res.text();
  if (!res.ok) throw new Error(text || `HTTP ${res.status}`);
  return text ? JSON.parse(text) : null;
}

export default function History() {
  const [items, setItems] = useState([]);
  const [openSessionId, setOpenSessionId] = useState(null);
  const [casesMap, setCasesMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadingCases, setLoadingCases] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        setLoading(true);
        setError("");
        const data = await apiGetHistory();
        if (!alive) return;
        setItems(Array.isArray(data) ? data : []);
      } catch (e) {
        if (!alive) return;
        setError(String(e?.message || e));} finally {
        if (!alive) return;
        setLoading(false);}
    })();
    return () => {
      alive = false;};}, []);
  const toggleOpen = async (sessionId) => {
    if (openSessionId === sessionId) {
      setOpenSessionId(null);
      return;}
    setOpenSessionId(sessionId);
    if (casesMap[sessionId]) return;
    try {
      setLoadingCases(true);
      setError("");
      const cs = await apiGetCases(sessionId);
      setCasesMap((prev) => ({ ...prev, [sessionId]: Array.isArray(cs) ? cs : [] }));
    } catch (e) {
      setError(String(e?.message || e));
    } finally {
      setLoadingCases(false);}};
  const openChat = async (dbSessionId) => {
    try {
      setError("");
      const data = await apiGetSessionKey(dbSessionId);
      const key = data?.clientSessionKey;
      if (!key) throw new Error("Missing clientSessionKey from backend");
      localStorage.setItem("chat_session_id", key);
      window.location.href = `/chat?sid=${dbSessionId}`;
    } catch (e) {
      setError(String(e?.message || e));}};
  return (
    <div className="history-page">
      <div className="history-layout">
        <div className="history-left">
          <div className="history-shell">
            <h2 className="history-title">Chat History</h2>
            {error && <div className="history-error">{error}</div>}
            {loading ? (
              <div className="history-muted">Loading…</div> ) : items.length === 0 ? (
              <div className="history-muted">No chats yet.</div> ) : (
              <div className="history-list">
                {items.map((it) => {
                  const isOpen = openSessionId === it.id;
                  const cases = casesMap[it.id] || [];
                  return (
                    <div
                      key={it.id}
                      className={`history-card ${isOpen ? "history-card-open" : ""}`}>
                      <div className="history-card-head">
                        <button
                          className="history-card-title"
                          onClick={() => toggleOpen(it.id)}
                          title="View cases"
                          type="button">
                          {it.title && it.title.trim() ? it.title : `Chat ${it.id}`}
                        </button>
                        <div className="history-card-middle">
                          <span
                            className={`history-status-badge history-status-${String(
                              it.status || "unknown"
                            ).toLowerCase()}`}>
                            {it.status ?? "UNKNOWN"}
                          </span>
                          <span className="history-subline">
                            Cases: <b>{it.caseCount ?? 0}</b>
                          </span>
                        </div>
                        <div className="history-card-actions">
                          <button className="history-open-btn"
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              openChat(it.id);
                            }}>Open</button>
                        </div>
                      </div>
                      {isOpen && (
                        <div className="history-cases">
                          {loadingCases && cases.length === 0 ? (
                            <div className="history-muted">Loading cases…</div>
                          ) : cases.length === 0 ? (
                            <div className="history-muted">No cases in this chat.</div>
                          ) : (
                            <div className="history-case-list">
                              {cases.map((c) => (
                                <div key={c.id} className="history-case-card">
                                  <div className="history-case-head">
                                    <div className="history-case-title">
                                      {c.top1Disease || "Unknown condition"}
                                    </div>
                                    <div className="history-case-status">{c.status}</div>
                                  </div>
                                  <div className="history-case-sub">
                                    Advice shown: {c.adviceShown ? "Yes" : "No"} · Updated:{" "}
                                    {c.updatedAt}
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
        <div className="history-right">
          <div className="history-character-wrap">
            <div className="history-bubble-wrap">
              <img
                src={cloudMessage}
                alt="Speech bubble"
                className="history-bubble-img"/>
              <div className="history-bubble-text">You can see all your chat details here.<br/><br/>If you want to see the diseases in each chat, click the chat name.<br/><br/>If you want to continue an old chat, click the Open button.
              </div>
            </div>
            <img
              src={owlDoctor}
              alt="History owl"
              className="history-owl-img"/>
          </div>
        </div>
      </div>
    </div>
  );
}