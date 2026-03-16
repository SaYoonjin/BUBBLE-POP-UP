import { Link } from "react-router-dom";

interface CozyHeaderProps {
  variant: "lobby" | "dashboard" | "game";
  gameInfo?: {
    location?: string;
    storeName?: string;
    menu?: string;
    day?: number;
    timer?: string;
    population?: number;
    customers?: number;
    stock?: number;
    balance?: number;
  };
  isLoggedIn?: boolean;
}

function LobbyHeader({ isLoggedIn }: { isLoggedIn: boolean }) {
  return (
    <header className="sticky top-0 z-50 bg-[#fff9f4]/95 backdrop-blur-md border-b border-cozy-wood-light px-6 py-4 shadow-sm">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        <Link to="/" className="flex items-center gap-3 group">
          <div className="size-10 bg-cozy-primary/10 rounded-full flex items-center justify-center text-cozy-primary group-hover:bg-cozy-primary group-hover:text-white transition-colors">
            <span className="material-symbols-outlined text-2xl">storefront</span>
          </div>
          <h2 className="text-cozy-ink text-xl font-black tracking-tight font-cozy-display">BubbleBubble</h2>
        </Link>
        <nav className="hidden md:flex items-center gap-8 bg-white/50 px-6 py-2 rounded-full shadow-inner border border-cozy-wood-light">
          <Link to="/" className="text-cozy-ink hover:text-cozy-primary text-sm font-bold transition-colors flex items-center gap-1">
            <span className="material-symbols-outlined text-[18px]">cottage</span> 로비
          </Link>
          <Link to="/ranking" className="text-cozy-wood-dark hover:text-cozy-primary text-sm font-medium transition-colors flex items-center gap-1">
            <span className="material-symbols-outlined text-[18px]">leaderboard</span> 랭킹
          </Link>
        </nav>
        <div className="flex items-center gap-3">
          {isLoggedIn ? (
            <Link to="/mypage" className="flex items-center gap-2 pl-2 pr-1 py-1 bg-white border border-cozy-wood-light rounded-full shadow-sm hover:shadow-md transition-shadow">
              <span className="text-xs font-bold text-cozy-ink">Owner</span>
              <div className="size-8 rounded-full bg-cozy-primary/20 flex items-center justify-center">
                <span className="material-symbols-outlined text-cozy-primary text-lg">person</span>
              </div>
            </Link>
          ) : (
            <Link to="/login" className="text-sm font-bold text-cozy-primary hover:underline">로그인</Link>
          )}
        </div>
      </div>
    </header>
  );
}

function GameHeader({ gameInfo }: { gameInfo: CozyHeaderProps["gameInfo"] }) {
  const info = gameInfo || {};
  return (
    <header className="sticky top-0 z-50 bg-[#5d4037] text-white px-4 py-3 shadow-lg border-b-4 border-[#3e2723]">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        <div className="flex items-center gap-3 text-sm">
          <span className="px-3 py-1 bg-white/10 rounded-xl font-cozy-hand text-lg">{info.location || "—"}</span>
          <span className="font-bold font-cozy-display">{info.storeName || "—"}</span>
          <span className="text-white/60">{info.menu || "—"}</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="bg-cozy-primary px-4 py-1.5 rounded-full shadow-lg font-black text-sm tracking-wider">
            DAY {info.day || "—"}
          </div>
          <span className="font-mono text-cozy-sage text-lg">{info.timer || "00:00"}</span>
        </div>
        <div className="flex items-center gap-5 text-xs font-cozy-display">
          <CozyGameStat icon="groups" value={info.population} />
          <CozyGameStat icon="person" value={info.customers} />
          <CozyGameStat icon="inventory_2" value={info.stock} />
          <CozyGameStat icon="savings" value={info.balance?.toLocaleString()} />
        </div>
      </div>
    </header>
  );
}

function CozyGameStat({ icon, value }: { icon: string; value?: string | number | null }) {
  return (
    <div className="flex items-center gap-1 bg-white/10 px-2 py-1 rounded-lg">
      <span className="material-symbols-outlined text-white/60 text-base">{icon}</span>
      <span className="font-bold text-white">{value ?? "—"}</span>
    </div>
  );
}

export default function CozyHeader({ variant, gameInfo, isLoggedIn = false }: CozyHeaderProps) {
  switch (variant) {
    case "lobby":
    case "dashboard":
      return <LobbyHeader isLoggedIn={isLoggedIn} />;
    case "game":
      return <GameHeader gameInfo={gameInfo} />;
  }
}
