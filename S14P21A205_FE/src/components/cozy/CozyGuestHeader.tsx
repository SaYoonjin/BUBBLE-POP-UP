import { Link } from "react-router-dom";

export default function CozyGuestHeader() {
  return (
    <header className="relative z-50 flex items-center justify-between px-8 py-6">
      <Link to="/cozy" className="flex items-center gap-3">
        <div className="size-10 rounded-full bg-cozy-primary flex items-center justify-center text-white shadow-lg -rotate-[5deg]">
          <span className="material-symbols-outlined text-[24px]">bubble_chart</span>
        </div>
        <h1 className="font-cozy-serif text-3xl font-black italic tracking-tighter text-white drop-shadow-md">
          The Daily Bubble
        </h1>
      </Link>
      <Link
        to="/cozy/login"
        className="font-cozy-serif italic text-white/90 hover:text-white transition-colors text-lg border-b border-white/30"
      >
        Login
      </Link>
    </header>
  );
}
