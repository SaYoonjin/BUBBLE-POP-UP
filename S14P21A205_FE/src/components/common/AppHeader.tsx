import { Link } from "react-router-dom";
import ProfileDropdown from "./ProfileDropdown";

interface AppHeaderProps {
  nickname?: string;
}

export default function AppHeader({ nickname = "Owner" }: AppHeaderProps) {
  return (
    <header className="w-full h-16 flex items-center justify-between px-6 md:px-12 fixed top-0 left-0 z-50 bg-white shadow-sm">
      <Link to="/" className="flex items-center gap-2">
        <span className="text-2xl select-none">🫧</span>
        <span className="text-xl font-bold tracking-tight text-primary">버블버블</span>
      </Link>
      <ProfileDropdown nickname={nickname} />
    </header>
  );
}
