import { createBrowserRouter } from "react-router-dom";
//import PrivateRoute from "./PrivateRoute";
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

const router = createBrowserRouter([
  { path: "/", element: <HomePage /> },
  { path: "/login", element: <LoginPage /> },
  { path: "/test", element: <ComponentTestPage /> },
  { path: "/news", element: <NewsPage /> },
  { path: "/cozy/prep", element: <CozyPrepPage /> },
  {
    //element: <PrivateRoute />,
    children: [
      { path: "/mypage", element: <MyPage /> },
      { path: "/game/setup/location", element: <LocationSelectPage /> },
      { path: "/game/setup/naming", element: <BrandNamingPage /> },
      { path: "/game/waiting", element: <WaitingPage /> },
      { path: "/game/:day/prep", element: <PrepPage /> },
      { path: "/game/:day/play", element: <PlayPage /> },
      { path: "/game/:day/report", element: <ReportPage /> },
      { path: "/ranking", element: <RankingPage /> },
    ],
  },
]);

export default router;
