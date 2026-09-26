import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import App from "../App.jsx"; 
import Layout from "./Layout.jsx";
import AuthPage from "./AuthPage.jsx";
import { useAuth } from "./AuthContext.jsx";
import Account from "../pages/Account.jsx";
import History from "../pages/History.jsx";
import HelpCenter from "../pages/HelpCenter.jsx";

function RequireAuth({ children }) {
  const { isAuthed } = useAuth();
  const loc = useLocation();
  if (!isAuthed) return <Navigate to="/auth" replace state={{ from: loc.pathname }} />;
  return children;}

function GuestOnly({ children }) {
  const { isAuthed } = useAuth();
  if (isAuthed) return <Navigate to="/chat" replace />;
  return children;}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/chat" replace />} />
          <Route path="/auth" element={<GuestOnly><AuthPage /></GuestOnly>}/>
          <Route path="/chat" element={<RequireAuth><App /></RequireAuth>}/>
          <Route path="/account" element={<RequireAuth><Account /></RequireAuth>}/>
          <Route path="/history"element={<RequireAuth><History /></RequireAuth>}/>
          <Route path="/helpcenter"element={<RequireAuth><HelpCenter /></RequireAuth>}/>
          <Route path="*" element={<Navigate to="/chat" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
