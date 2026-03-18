import LobbyPage from "./LobbyPage";
import DashboardPage from "./DashboardPage";

export default function HomePage() {
  const token = localStorage.getItem("accessToken");
  const shouldBypassAuth = import.meta.env.DEV;

  return token || shouldBypassAuth ? <DashboardPage /> : <LobbyPage />;
}
