import { NavLink } from "react-router-dom";
import watermelon from "../assets/watermelon.png";
import apple from "../assets/apple.png";
import mango from "../assets/mango.png";
import pineapple from "../assets/pineapple.png";

function CircleLink({ to, label, imgSrc }) {
  return (
    <NavLink to={to}
      className={({ isActive }) =>`nav-circle ${isActive ? "nav-circle-active" : ""}`}title={label}aria-label={label}>
      <img className="nav-img" src={imgSrc} alt={label} />
      <span className="nav-text">{label}</span>
    </NavLink>
  );
}
export default function Nav() {
  return (
    <nav className="nav-top">
      <div className="nav-inner">
        <CircleLink to="/chat" label="Chat" imgSrc={watermelon} />
        <CircleLink to="/history" label="History" imgSrc={apple} />
        <CircleLink to="/account" label="Account" imgSrc={mango} />
        <CircleLink to="/helpcenter" label="Help" imgSrc={pineapple} />
      </div>
    </nav>
  );
}