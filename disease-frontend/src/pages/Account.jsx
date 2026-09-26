import { useNavigate } from "react-router-dom";
import { useAuth } from "../components/AuthContext";
import tigerPolice from "../assets/TigerCop.png";

export default function Account() {
const nav = useNavigate();
const { auth, isAuthed, logout } = useAuth();
const u = auth?.user;
const handleLogout = () => {
    localStorage.removeItem("chat_session_id");
    localStorage.removeItem("token");
    logout();
    nav("/auth");};
  if (!isAuthed) {
    return (
      <div className="acc-container">
        <div className="acc-shell acc-shell-center">
          <div className="acc-left">
            <h2 className="acc-title">Account</h2>
            <p className="acc-sub">You are not logged in.</p>
            <button className="acc-button" onClick={() => nav("/auth")}>
              Go to Login
            </button>
          </div>
        </div>
      </div>
    );
  }
const fullName = [u?.firstName, u?.middleName, u?.lastName].filter(Boolean).join(" ");
  return (
    <div className="acc-container">
      <div className="acc-shell">
        <div className="acc-left">
          <h2 className="acc-title">Account</h2>
          <div className="acc-book-wrap">
            <div className="book acc-book">
              <div className="acc-page-content">
                <div className="acc-row">
                  <span className="acc-label">Full name</span>
                  <span className="acc-value">{fullName || "-"}</span>
                </div>
                <div className="acc-row">
                  <span className="acc-label">Username</span>
                  <span className="acc-value">{u?.username || "-"}</span>
                </div>
                <div className="acc-row">
                  <span className="acc-label">Email</span>
                  <span className="acc-value">{u?.email || "-"}</span>
                </div>
                <div className="acc-row">
                  <span className="acc-label">BMI</span>
                  <span className="acc-value">{u?.bmi ?? "-"}</span>
                </div>
                <div className="acc-row">
                  <span className="acc-label">BMI status</span>
                  <span
                  className={`acc-badge ${
                  u?.bmiStatus === "Underweight"
                  ? "acc-badge-underweight"
                  : u?.bmiStatus === "Healthy"
                  ? "acc-badge-healthy"
                  : u?.bmiStatus === "Overweight"
                  ? "acc-badge-overweight"
                  : u?.bmiStatus === "Obese"
                  ? "acc-badge-obese"
                  : u?.bmiStatus === "Severely Obese"
                  ? "acc-badge-severely-obese"
                  : "acc-badge-default"}`}>{u?.bmiStatus || "-"}</span>
                </div>
              </div>
              <div className="cover acc-cover">
                <p className="acc-cover-title">Medical Record</p>
              </div>
            </div>
          </div>
          <button className="acc-button acc-logout" onClick={handleLogout}>
            Logout
          </button>
        </div>
        <div className="acc-right">
          <img src={tigerPolice} alt="Tiger police mascot" className="acc-mascot" />
        </div>
      </div>
    </div>
  );
}