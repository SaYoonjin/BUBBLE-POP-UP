import { Navigate, Outlet } from "react-router-dom";

export default function PrivateRoute() {
  const token = localStorage.getItem("accessToken");
  const shouldBypassAuth = import.meta.env.DEV;

  return token || shouldBypassAuth ? <Outlet /> : <Navigate to="/login" replace />;
}
