import { Outlet, useLocation } from "react-router-dom";
import Nav from "./Nav.jsx";

export default function Layout() {
  const location = useLocation();
  const isAuthPage = location.pathname === "/auth";
  return (
    <div className={`app-frame ${isAuthPage ? "app-frame-auth" : ""}`}>
      {!isAuthPage && (<header className="app-top"><Nav /></header>)}
      <main className={`app-main ${isAuthPage ? "app-main-auth" : ""}`}>
        <Outlet />
      </main>
    </div>
  );
}