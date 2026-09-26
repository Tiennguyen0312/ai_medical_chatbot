import React from "react";
import ReactDOM from "react-dom/client";
import "./App.css";
import AppRouter from "./components/AppRouter.jsx";
import { AuthProvider } from "./components/AuthContext.jsx";
ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AuthProvider>
      <AppRouter />
    </AuthProvider>
  </React.StrictMode>
);
