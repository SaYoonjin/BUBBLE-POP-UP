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

const router = createBrowserRouter([
  { path: "/", element: <LobbyPage /> },
  { path: "/login", element: <LoginPage /> },
  { path: "/test", element: <ComponentTestPage /> },
  { path: "/test/cozy", element: <CozyTestPage /> },
  { path: "/news", element: <NewsPage /> },
  {
    element: <PrivateRoute />,
    children: [
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
