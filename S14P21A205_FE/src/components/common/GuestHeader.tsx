import { Link } from "react-router-dom";

export default function GuestHeader() {
  return (
    <header className="w-full h-20 flex items-center justify-between px-8 md:px-16 fixed top-0 left-0 z-50 bg-white/80 backdrop-blur-md border-b border-white/20">
      <Link to="/" className="flex items-center gap-3 group">
        <span className="text-3xl select-none transition-transform group-hover:scale-110 duration-300">🫧</span>
        <span className="text-2xl font-bold tracking-tight text-primary">버블버블</span>
      </Link>
      <Link
        to="/login"
        className="px-5 py-2 bg-primary hover:bg-primary-dark text-white text-sm font-bold rounded-full transition-colors shadow-sm"
      >
        로그인
      </Link>
    </header>
  );
}
