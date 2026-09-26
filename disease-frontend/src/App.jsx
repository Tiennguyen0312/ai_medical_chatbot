import {  useEffect, useRef, useState } from "react";
import hippoDoctor from "./assets/doctorhippo.png";
import { useNavigate } from "react-router-dom";
import cloudMessage from "./assets/Cloud.png";

const API_BASE = import.meta.env.VITE_API_BASE || "http://127.0.0.1:8080";
function getSessionId() {
  const key = "chat_session_id";
  let v = localStorage.getItem(key);
  if (!v) {
    v =
      (crypto?.randomUUID?.() ||
        String(Date.now()) + Math.random().toString(16).slice(2));
    localStorage.setItem(key, v);}
  return v;
}

function resetSessionId() {
  const key = "chat_session_id";
  const v =
    (crypto?.randomUUID?.() ||
      String(Date.now()) + Math.random().toString(16).slice(2));
  localStorage.setItem(key, v);
  return v;
}

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

async function apiChat(session_id, message) {
  const token = getAuthToken();
  const username = getUsername();
  const res = await fetch(`${API_BASE}/api/chat`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ session_id, message, username }),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(text || `HTTP ${res.status}`);

  try {
    return text ? JSON.parse(text) : {};
  } catch {
    return { reply: text };
  }
}

async function apiGetMessages(dbSessionId) {
  const token = getAuthToken();
  const username = getUsername();
  if (!username) throw new Error("Missing username. Please login again.");
  const res = await fetch(
    `${API_BASE}/api/history/${dbSessionId}/messages?username=${encodeURIComponent(username)}`,
    {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }
  );
  const text = await res.text();
  if (!res.ok) throw new Error(text || `HTTP ${res.status}`);
  try {
    return text ? JSON.parse(text) : [];
  } catch {
    return [];
  }
}
async function apiGetCurrentMessages(session_id) {
  const token = getAuthToken();
  const username = getUsername();
  if (!username) throw new Error("Missing username. Please login again.");
  const res = await fetch(
    `${API_BASE}/api/chat/messages?session_id=${encodeURIComponent(session_id)}&username=${encodeURIComponent(username)}`,
    {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }
  );
  const text = await res.text();
  if (!res.ok) throw new Error(text || `HTTP ${res.status}`);

  try {
    return text ? JSON.parse(text) : [];
  } catch {
    return [];
  }
}

export default function App() {
  const nav=useNavigate();
  const [sessionId, setSessionId] = useState(() => getSessionId());
  const [messages, setMessages] = useState([
    {
      role: "bot",
      text:
        "Hello!",
    },
  ]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [error, setError] = useState("");
  const endRef = useRef(null);

  useEffect(() => {
    console.log("API_BASE =", import.meta.env.VITE_API_BASE, "-> using:", API_BASE);
  }, []);
  useEffect(() => {
  const params = new URLSearchParams(window.location.search);
  const sid = params.get("sid");
  (async () => {
    try {
      setError("");
      if (sid) {
        const data = await apiGetMessages(sid);
        const mapped = (Array.isArray(data) ? data : []).map((m) => ({
          role: m.role === "USER" ? "user" : "bot",
          text: m.content ?? "",
        }));
        if (mapped.length > 0) {
          setMessages(mapped);
        } else {
          setMessages([{ role: "bot", text: "No messages found for this chat session." }]);
        }
        return;
      }
      const data = await apiGetCurrentMessages(sessionId);
      const mapped = (Array.isArray(data) ? data : []).map((m) => ({
        role: m.role === "USER" ? "user" : "bot",
        text: m.content ?? "",
      }));
      if (mapped.length > 0) {
        setMessages(mapped);
      } else {
        setMessages([{ role: "bot", text: "Hello!" }]);
      }
    } catch (e) {
      setError(String(e?.message || e));
    }})();}, []);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  const sendWithText = async (rawText) => {
    const text = (rawText ?? "").trim();
    if (!text || isTyping) return;
    setError("");
    setInput("");
    const isEndCommand = text.toLowerCase() === "end";
    if (!isEndCommand) {
      setMessages((prev) => [...prev, { role: "user", text }]);}
    setIsTyping(true);
    try {
      const data = await apiChat(sessionId, text);
    const botMsg = {
      role: "bot",
      text: data.reply ?? "No reply.",
      options: Array.isArray(data.options) ? data.options : undefined,
      state: data.state,
      redirectTo: data.redirectTo,
};

if (data?.state === "REDIRECT_HELP" && data?.redirectTo) {
  setMessages((prev) => [
    ...prev,
    {
      role: "bot",
      text: data.reply ?? "Please go to Help Center.",
      options: Array.isArray(data.options) ? data.options : undefined,
      state: data.state,
    },
  ]);
  const newId = resetSessionId();
  setSessionId(newId);
  const url = new URL(window.location.href);
  url.searchParams.delete("sid");
  window.history.replaceState({}, "", url.pathname);
  setTimeout(() => nav(data.redirectTo), 0);
  return;
}
if (data.state === "ENDED_DELAYED") {
    setMessages((prev) => [...prev, botMsg]);

    setTimeout(() => {
      const newId = resetSessionId();
      setSessionId(newId);

      const url = new URL(window.location.href);
      url.searchParams.delete("sid");
      window.history.replaceState({}, "", url.toString());

      setMessages([
        {
          role: "bot",
          text: "Hello!",
        },
      ]);
    }, 6000);

    return;
  }

setMessages((prev) => [...prev, botMsg]);
      if (data.state === "ENDED") {
        const newId = resetSessionId();
        setSessionId(newId);
        const url = new URL(window.location.href);
        url.searchParams.delete("sid");
        window.history.replaceState({}, "", url.toString());
        setMessages([
          {
            role: "bot",
            text:
              "Hello my friend! Tell me what symptoms you have (e.g., cough, fever, headache).",
          },
        ]);
      }
      
    } catch (e) {
      setError(String(e?.message || e));
      setMessages((prev) => [
        ...prev,
        {
          role: "bot",
          text:
            "Cannot reach backend (/api/chat). Check Spring Boot (8080), CORS, and that the server is running.",
        },
      ]);
    } finally {
      setIsTyping(false);
    }
  };

  const send = async () => {
    await sendWithText(input);
  };

  const onKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
  <div className="chat-stage">
    <div className="chat-mascot" aria-hidden="true">
  <div className="chat-mascot-wrap">
    <div className="chat-mascot-bubble">
      <img src={cloudMessage} alt="" className="chat-mascot-bubble-img" />
      <div className="chat-mascot-bubble-text">Start typing your symptom so i can help you.</div>
    </div>
    <img src={hippoDoctor} alt="" className="chat-mascot-img" />
  </div>
</div>
    <div className="chat-page">
      <h1 className="chat-title">Medical Chatbot</h1>
      <div className="chat-shell">
        <button
          className="chat-close-btn"
          onClick={() => sendWithText("end")}
          disabled={isTyping}
          title="End chat"
          type="button">✕</button>
        <div className="chat-messages">
          {messages.map((m, i) => (
            <div
              key={i}
              className={`chat-row ${m.role === "user" ? "chat-row-user" : "chat-row-bot"}`}>
              <div className={`chat-bubble ${m.role === "user" ? "chat-bubble-user" : "chat-bubble-bot"}`}>
                <div className="chat-text">{m.text}</div>
                {m.role === "bot" && Array.isArray(m.options) && m.options.length > 0 && (
                  <div className="chat-options">
                    {m.options.map((opt, idx) => (
                      <button
                        key={idx}
                        className="chat-option-btn"
                        onClick={() => sendWithText(String(opt))}
                        disabled={isTyping}
                        type="button"
                        title={opt}>{opt}</button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ))}
          {isTyping && (
            <div className="chat-row chat-row-bot">
              <div className="chat-bubble chat-bubble-bot chat-typing">Typing…</div>
            </div>
          )}
          <div ref={endRef} />
        </div>
        <div className="chat-inputbar">
          {error && <div className="chat-error">{error}</div>}
          <div className="chat-inputrow">
            <textarea
              className="chat-input"
              rows={2}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={onKeyDown}
              placeholder="Type a message (Enter to send, Shift+Enter for new line)"/>
            <button className="chat-send-btn" onClick={send} disabled={isTyping} type="button">Send</button>
          </div>
        </div>
      </div>
    </div>
  </div>
);
}