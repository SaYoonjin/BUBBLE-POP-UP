import { createBrowserRouter } from "react-router-dom";
import PrivateRoute from "./PrivateRoute";
import LobbyPage from "../pages/LobbyPage";
import LoginPage from "../pages/LoginPage";
import MyPage from "../pages/MyPage";
import LocationSelectPage from "../pages/LocationSelectPage";
import BrandNamingPage from "../pages/BrandNamingPage";
import PrepPage from "../pages/PrepPage";
import PlayPage from "../pages/PlayPage";
import ReportPage from "../pages/ReportPage";
import RankingPage from "../pages/RankingPage";
import ComponentTestPage from "../pages/ComponentTestPage";
import CozyTestPage from "../pages/CozyTestPage";
import NewsPage from "../pages/NewsPage";
import CozyPrepPage from "../pages/CozyPrepPage";
import DashboardPage from "../pages/DashboardPage";
import CozyLobbyPage from "../pages/CozyLobbyPage";
import CozyLoginPage from "../pages/CozyLoginPage";
import CozyDashboardPage from "../pages/CozyDashboardPage";
import CozyMyPage from "../pages/CozyMyPage";

const router = createBrowserRouter([
  { path: "/", element: <LobbyPage /> },
  { path: "/login", element: <LoginPage /> },
  { path: "/test", element: <ComponentTestPage /> },
  { path: "/test/cozy", element: <CozyTestPage /> },
  { path: "/news", element: <NewsPage /> },
  { path: "/cozy", element: <CozyLobbyPage /> },
  { path: "/cozy/login", element: <CozyLoginPage /> },
  { path: "/cozy/dashboard", element: <CozyDashboardPage /> },
  { path: "/cozy/mypage", element: <CozyMyPage /> },
  { path: "/cozy/prep", element: <CozyPrepPage /> },
  {
    element: <PrivateRoute />,
    children: [
      { path: "/dashboard", element: <DashboardPage /> },
      { path: "/mypage", element: <MyPage /> },
      { path: "/game/setup/location", element: <LocationSelectPage /> },
      { path: "/game/setup/naming", element: <BrandNamingPage /> },
      { path: "/game/:day/prep", element: <PrepPage /> },
      { path: "/game/:day/play", element: <PlayPage /> },
      { path: "/game/:day/report", element: <ReportPage /> },
      { path: "/ranking", element: <RankingPage /> },
    ],
  },
]);

export default router;
