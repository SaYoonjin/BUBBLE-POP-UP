import { Navigate, Outlet } from "react-router-dom";
import { isAuthenticated } from "../hooks/useAuth";

export default function PrivateRoute() {
  return isAuthenticated() ? <Outlet /> : <Navigate to="/login" replace />;
}
