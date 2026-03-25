import { RouterProvider } from "react-router-dom";
import router from "./router";
import BgmController from "./components/common/BgmController";

export default function App() {
  return (
    <>
      <RouterProvider router={router} />
      <BgmController />
    </>
  );
}
