import { Link } from "react-router-dom";
import ProfileDropdown from "../common/ProfileDropdown";

interface CozyAppHeaderProps {
  nickname?: string;
}

export default function CozyAppHeader({ nickname = "Owner" }: CozyAppHeaderProps) {
  return (
    <header className="w-full h-16 flex items-center justify-between px-6 md:px-12 fixed top-0 left-0 z-50 bg-white/70 backdrop-blur-md border-b border-white/20 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.15)]">
      <Link to="/cozy/dashboard" className="flex items-center gap-3">
        <div className="size-8 rounded-full bg-cozy-primary flex items-center justify-center text-white shadow-md">
          <span className="material-symbols-outlined text-[18px]">bubble_chart</span>
        </div>
        <span className="text-xl font-cozy-serif font-black italic tracking-tight text-cozy-ink">버블버블</span>
      </Link>
      <ProfileDropdown nickname={nickname} />
    </header>
  );
}
