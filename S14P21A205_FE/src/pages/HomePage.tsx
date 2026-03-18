import LobbyPage from "./LobbyPage";
import DashboardPage from "./DashboardPage";

export default function HomePage() {
  const token = localStorage.getItem("accessToken");
  return token ? <DashboardPage /> : <LobbyPage />;
}
