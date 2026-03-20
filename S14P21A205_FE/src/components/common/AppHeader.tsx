import { Link } from "react-router-dom";
import ProfileDropdown from "./ProfileDropdown";
import useAuth from "../../hooks/useAuth";
import { useUserStore } from "../../stores/useUserStore";

export default function AppHeader() {
  const { isLoggedIn } = useAuth();
  const nickname = useUserStore((s) => s.nickname);
  const displayNickname = nickname || "Owner";

  return (
    <header
      className={`fixed left-0 top-0 z-50 flex w-full items-center justify-between ${
        isLoggedIn
          ? "h-16 bg-white px-6 shadow-sm md:px-12"
          : "h-20 border-b border-white/20 bg-white/80 px-8 backdrop-blur-md md:px-16"
      }`}
    >
      <Link
        to="/"
        className={`flex items-center ${
          isLoggedIn ? "gap-2" : "gap-3 group"
        }`}
      >
        <span
          className={`select-none ${
            isLoggedIn
              ? "text-2xl"
              : "text-3xl transition-transform duration-300 group-hover:scale-110"
          }`}
        >
          🫧
        </span>
        <span
          className={`font-bold tracking-tight text-primary ${
            isLoggedIn ? "text-xl" : "text-2xl"
          }`}
        >
          BUBBLEPOPUP
        </span>
      </Link>

      {isLoggedIn ? (
        <ProfileDropdown nickname={displayNickname} />
      ) : (
        <Link
          to="/login"
          className="rounded-full bg-primary px-5 py-2 text-sm font-bold text-white shadow-sm transition-colors hover:bg-primary-dark"
        >
          로그인
        </Link>
      )}
    </header>
  );
}
