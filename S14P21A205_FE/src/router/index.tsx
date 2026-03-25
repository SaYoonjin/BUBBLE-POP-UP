import { createBrowserRouter, Outlet } from "react-router-dom";
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
import NewsPage from "../pages/NewsPage";
import CozyPrepPage from "../pages/CozyPrepPage";
import WaitingPage from "../pages/WaitingPage";
import AuthCallbackPage from "../pages/AuthCallbackPage";
import ForbiddenPage from "../pages/ForbiddenPage";
import NotFoundPage from "../pages/NotFoundPage";
import BgmController from "../components/common/BgmController";

function AppShell() {
  return (
    <>
      <Outlet />
      <BgmController />
    </>
  );
}

const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { path: "/", element: <HomePage /> },
      { path: "/login", element: <LoginPage /> },
      { path: "/auth/callback", element: <AuthCallbackPage /> },
      { path: "/news", element: <NewsPage /> },
      { path: "/cozy/prep", element: <CozyPrepPage /> },
      { path: "/403", element: <ForbiddenPage /> },
      {
        element: <PrivateRoute />,
        children: [
          // åª›Â€???ë…¿ë’— ?ì„ì” ï§žÂ€ (?ëª„ì £???ë¬Žë  åª›Â€??
          { path: "/mypage", element: <MyPage /> },

          // å¯ƒëš¯ì—« ?ì„ì” ï§?åª›Â€???ê³¸ìŠœ
          {
            element: <GameGuard />,
            children: [
              { path: "/game/setup/location", element: <LocationSelectPage /> },
              { path: "/game/setup/naming", element: <BrandNamingPage /> },
              { path: "/game/waiting", element: <WaitingPage /> },
              { path: "/game/:day/prep", element: <PrepPage /> },
              { path: "/game/:day/play", element: <PlayPage /> },
              { path: "/game/:day/report", element: <ReportPage /> },
              { path: "/ranking", element: <RankingPage /> },
            ],
          },
        ],
      },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);

export default router;
