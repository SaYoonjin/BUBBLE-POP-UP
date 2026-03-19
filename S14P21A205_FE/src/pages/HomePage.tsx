import LobbyPage from "./LobbyPage";
import DashboardPage from "./DashboardPage";
import { isAuthenticated } from "../hooks/useAuth";

export default function HomePage() {
  return isAuthenticated() ? <DashboardPage /> : <LobbyPage />;
}
