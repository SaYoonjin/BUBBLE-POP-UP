import { createBrowserRouter } from "react-router-dom";
import PrivateRoute from "./PrivateRoute";
import GameGuard from "./GameGuard";
import HomePage from "../pages/HomePage";
import LoginPage from "../pages/LoginPage";
import MyPage from "../pages/MyPage";
import LocationSelectPage from "../pages/LocationSelectPage";
import BrandNamingPage from "../pages/BrandNamingPage";
import PrepPage from "../pages/PrepPage";
import PlayPage from "../pages/PlayPage";
import ReportPage from "../pages/ReportPage";
import RankingPage from "../pages/RankingPage";
import ComponentTestPage from "../pages/ComponentTestPage";
import NewsPage from "../pages/NewsPage";
import CozyPrepPage from "../pages/CozyPrepPage";
import WaitingPage from "../pages/WaitingPage";
import AuthCallbackPage from "../pages/AuthCallbackPage";

const router = createBrowserRouter([
  { path: "/", element: <HomePage /> },
  { path: "/login", element: <LoginPage /> },
  { path: "/auth/callback", element: <AuthCallbackPage /> },
  { path: "/test", element: <ComponentTestPage /> },
  { path: "/news", element: <NewsPage /> },
  { path: "/cozy/prep", element: <CozyPrepPage /> },
  {
    element: <PrivateRoute />,
    children: [
      // 가드 없는 페이지 (언제든 접근 가능)
      { path: "/mypage", element: <MyPage /> },
      { path: "/ranking", element: <RankingPage /> },

      // 게임 페이즈 가드 적용
      {
        element: <GameGuard />,
        children: [
          { path: "/game/setup/location", element: <LocationSelectPage /> },
          { path: "/game/setup/naming", element: <BrandNamingPage /> },
          { path: "/game/waiting", element: <WaitingPage /> },
          { path: "/game/:day/prep", element: <PrepPage /> },
          { path: "/game/:day/play", element: <PlayPage /> },
          { path: "/game/:day/report", element: <ReportPage /> },
        ],
      },
    ],
  },
]);

export default router;
