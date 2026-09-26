import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";
import signBoard from "../assets/signBoard.png";
import bearNurse from "../assets/Bearnurse.png";
import tigerPolice from "../assets/TigerCop.png";
import owlFixer from "../assets/OwlFixer.png";
import doctorHippo from "../assets/doctorhippo.png";
const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";
function createFreshChatSessionId() {
  const next =
    crypto?.randomUUID?.() ||
    `${Date.now()}_${Math.random().toString(16).slice(2)}`;

  localStorage.setItem("chat_session_id", next);
  return next;
}
async function postJson(url, body) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  let data = null;
  try {
    data = await res.json();
  } catch {}
  if (!res.ok) {
    const msg = data?.message || data?.detail || `Request failed (${res.status})`;
    throw new Error(msg);
  }
  return data;
}

function SignupModal({ open, busy, onClose, onSubmitSignup }) {
  const [err, setErr] = useState("");
  const [form, setForm] = useState({
    firstName: "",
    middleName: "",
    lastName: "",
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
    heightCm: "",
    weightKg: "",
    acceptTerms: false,
  });
  const setField = (name, value) => setForm((prev) => ({ ...prev, [name]: value }));
  const canSubmit = useMemo(() => {
    if (!form.acceptTerms) return false;
    if (!form.firstName.trim()) return false;
    if (!form.lastName.trim()) return false;
    if (!form.username.trim()) return false;
    if (!form.email.trim()) return false;
    if (!form.password) return false;
    if (form.password !== form.confirmPassword) return false;
    if (!String(form.heightCm).trim()) return false;
    if (!String(form.weightKg).trim()) return false;
    return true;
  }, [form]);
  const validate = () => {
    if (!form.acceptTerms) return "Please accept the terms and conditions.";
    if (!form.firstName.trim()) return "First name is required.";
    if (!form.lastName.trim()) return "Last name is required.";
    if (!form.username.trim()) return "Username is required.";
    if (!form.email.trim()) return "Email is required.";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) return "Email format is invalid.";
    if (!form.password) return "Password is required.";
    if (form.password.length < 6) return "Password must be at least 6 characters.";
    if (form.password !== form.confirmPassword) return "Passwords don't match.";
    const h = Number(form.heightCm);
    const w = Number(form.weightKg);
    if (!Number.isFinite(h) || !Number.isFinite(w)) return "Height and weight must be numbers.";
    if (h < 50 || h > 250) return "Height (cm)";
    if (w < 10 || w > 400) return "Weight (kg)";
    return "";
  };
  
  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    const v = validate();
    if (v) {
      setErr(v);
      return;
    }
    const payload = {
      firstName: form.firstName.trim(),
      middleName: form.middleName.trim(),
      lastName: form.lastName.trim(),
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      heightCm: Number(form.heightCm),
      weightKg: Number(form.weightKg),
      acceptTerms: !!form.acceptTerms,
    };
    try {
      await onSubmitSignup(payload);
      onClose();
    } catch (e2) {
      setErr(String(e2?.message || e2));
    }
  };
  if (!open) return null;
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal" onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Create account</h3>
          <button className="icon-btn" onClick={onClose} type="button" aria-label="Close" disabled={busy}>
            ✕
          </button>
        </div>
        {err ? <div className="form-error">{err}</div> : null}
      <form className="form" onSubmit={submit}>
        <div className="form-grid">
          <div className="form-group">
            <label>First name *</label>
            <input className="input" value={form.firstName} onChange={(e) => setField("firstName", e.target.value)} /></div>
            <div className="form-group">
              <label>Middle name</label>
              <input className="input" value={form.middleName} onChange={(e) => setField("middleName", e.target.value)} /></div>
            <div className="form-group">
              <label>Last name *</label>
              <input className="input" value={form.lastName} onChange={(e) => setField("lastName", e.target.value)} />
            </div>
            <div className="form-group">
              <label>Username (User ID) *</label>
              <input className="input" value={form.username} onChange={(e) => setField("username", e.target.value)} />
            </div>
            <div className="form-group">
              <label>Email (Gmail) *</label>
              <input
                className="input"
                type="email"
                value={form.email}
                onChange={(e) => setField("email", e.target.value)}
                placeholder="name@gmail.com"
              />
            </div>
            <div className="form-group">
              <label>Password *</label>
              <input
                className="input"
                type="password"
                value={form.password}
                onChange={(e) => setField("password", e.target.value)}
                autoComplete="new-password"
              />
            </div>
            <div className="form-group">
              <label>Confirm password *</label>
              <input
                className="input"
                type="password"
                value={form.confirmPassword}
                onChange={(e) => setField("confirmPassword", e.target.value)}
                autoComplete="new-password"
              />
            </div>
          </div>
          <div className="form-grid-2">
            <div className="form-group">
              <label>Height (cm) *</label>
              <input
                className="input"
                type="number"
                value={form.heightCm}
                onChange={(e) => setField("heightCm", e.target.value)}
                placeholder="e.g., 165"
                min="50"
                max="250"
                step="0.1"
              />
            </div>
            <div className="form-group">
              <label>Weight (kg) *</label>
              <input
                className="input" type="number"
                value={form.weightKg}
                onChange={(e) => setField("weightKg", e.target.value)}
                placeholder="e.g., 58"
                min="10"
                max="400"
                step="0.1"/>
            </div>
          </div>
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={form.acceptTerms}
              onChange={(e) => setField("acceptTerms", e.target.checked)}
              disabled={busy}/>
            <span>I accept the terms and conditions (Academic demo)</span>
          </label>
          <div className="form-actions">
            <button className="btn btn-secondary" type="button" onClick={onClose} disabled={busy}>
              Cancel
            </button>
            <button className="btn btn-primary" type="submit" disabled={!canSubmit || busy}>
              {busy ? "Creating..." : "Create account"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AuthPage() {
  const [mode, setMode] = useState("login");
  const [openSignup, setOpenSignup] = useState(false);

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");

  const nav = useNavigate();
  const { login } = useAuth();

  const buildUserFromAuthResponse = (data) => ({
    id: data.id,
    username: data.username,
    firstName: data.firstName,
    middleName: data.middleName,
    lastName: data.lastName,
    email: data.email,
    bmi: data.bmi,
    bmiStatus: data.bmiStatus,
  });

  async function onLogin(e) {
    e.preventDefault();
    setErr("");

    if (!username.trim() || !password.trim()) {
      setErr("Please enter username and password.");
      return;
    }

    setBusy(true);
    try {
      const data = await postJson(`${API_BASE}/api/auth/login`, {
        username: username.trim(),
        password: password.trim(),
      });

      const token = data.token || null;
      const user = buildUserFromAuthResponse(data);

      login({ token, user });
      createFreshChatSessionId();
      nav("/chat", { replace: true });
    } catch (e2) {
      setErr(e2?.message || "Failed");
    } finally {
      setBusy(false);
    }
  }

  async function onSubmitSignup(payload) {
    const data = await postJson(`${API_BASE}/api/auth/signup`, payload);

    const token = data.token || null;
    const user = buildUserFromAuthResponse(data);

    login({ token, user });
    createFreshChatSessionId();
    nav("/chat", { replace: true });
  }

  return (
  <div className="auth-page">
    <div className="auth-left">
  <div className="auth-left-inner">
    <div className="auth-left-top">
      <div className="auth-sign-wrap">
        <img src={signBoard} alt="" className="auth-sign-img" />
        <div className="auth-sign-text">
         <span className="sign-line sign-line-1">Welcome</span>
      <span className="sign-line sign-line-2">to</span>
      <span className="sign-line sign-line-3">the</span>
      <span className="sign-line sign-line-4">Fospital</span>
</div>
      </div>
    </div>

    <div className="auth-left-bottom">
      <div className="auth-mascot-row">
        <div className="auth-mascot-card">
          <img src={bearNurse} alt="Bear nurse" className="auth-mascot-item" />
        </div>

        <div className="auth-mascot-card">
          <img src={tigerPolice} alt="Tiger police" className="auth-mascot-item" />
        </div>

        <div className="auth-mascot-card">
          <img src={owlFixer} alt="Owl fixer" className="auth-mascot-item" />
        </div>

        <div className="auth-mascot-card">
          <img src={doctorHippo} alt="Doctor hippo" className="auth-mascot-item" />
        </div>
      </div>
    </div>
  </div>
</div>

    <div className="auth-right">
      <div className="auth-panel">
        
        <p className="subtle">
          {mode === "login" ? "Login to continue" : "Create an account"}
        </p>

        <div className="tab-row">
          <button
            className={`tab-btn ${mode === "login" ? "tab-active" : ""}`}
            onClick={() => setMode("login")}
            disabled={busy}
            type="button"
          >
            Login
          </button>

          <button
            className={`tab-btn ${mode === "signup" ? "tab-active" : ""}`}
            onClick={() => {
              setMode("signup");
              setOpenSignup(true);
            }}
            disabled={busy}
            type="button"
          >
            Sign up
          </button>
        </div>

        {err ? <div className="form-error">{err}</div> : null}

        <form className="auth-form" onSubmit={onLogin}>
          <input
            className="input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Username"
            autoComplete="username"
            disabled={busy}
          />

          <input
            className="input"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Password"
            type="password"
            autoComplete="current-password"
            disabled={busy}
          />

          <button className="btn btn-primary full" type="submit" disabled={busy}>
            {busy ? "Please wait..." : "Login"}
          </button>
        </form>
      </div>

      <SignupModal
        open={openSignup}
        busy={busy}
        onClose={() => setOpenSignup(false)}
        onSubmitSignup={onSubmitSignup}
      />
    </div>
  </div>
);
}