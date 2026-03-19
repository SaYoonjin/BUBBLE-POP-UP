import { Link } from "react-router-dom";

interface HeaderProps {
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
    <header className="flex items-center justify-between px-6 py-4 bg-card-light border-b border-gray-100">
      <Link to="/" className="text-xl font-bold text-primary-dark font-display">
        BubbleBubble
      </Link>
      <nav className="flex items-center gap-6">
        <Link to="/ranking" className="text-sm text-gray-600 hover:text-primary-dark transition-colors">
          랭킹
        </Link>
        {isLoggedIn ? (
          <Link to="/mypage" className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center">
            <span className="material-symbols-outlined text-primary-dark text-lg">person</span>
          </Link>
        ) : (
          <Link to="/login" className="text-sm font-medium text-primary-dark hover:underline">
            로그인
          </Link>
        )}
      </nav>
    </header>
  );
}

function DashboardHeader() {
  return (
    <header className="flex items-center justify-between px-6 py-4 bg-card-light border-b border-gray-100">
      <Link to="/" className="text-xl font-bold text-primary-dark font-display">
        BubbleBubble
      </Link>
      <nav className="flex items-center gap-6">
        <Link to="/mypage" className="text-sm text-gray-600 hover:text-primary-dark transition-colors">
          대시보드
        </Link>
        <Link to="/ranking" className="text-sm text-gray-600 hover:text-primary-dark transition-colors">
          상점
        </Link>
        <Link to="/mypage" className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center">
          <span className="material-symbols-outlined text-primary-dark text-lg">person</span>
        </Link>
      </nav>
    </header>
  );
}

function GameHeader({ gameInfo }: { gameInfo: HeaderProps["gameInfo"] }) {
  const info = gameInfo || {};
  return (
    <header className="flex items-center justify-between px-4 py-3 bg-background-dark text-white">
      <div className="flex items-center gap-3 text-sm">
        <span className="px-2 py-0.5 bg-white/10 rounded-lg">{info.location || "—"}</span>
        <span className="font-medium">{info.storeName || "—"}</span>
        <span className="text-white/60">{info.menu || "—"}</span>
      </div>

      <div className="flex items-center gap-2 text-sm font-mono">
        <span className="px-2 py-0.5 bg-primary/30 rounded-lg font-bold">
          DAY {info.day || "—"}
        </span>
        <span className="text-primary">{info.timer || "00:00"}</span>
      </div>

      <div className="flex items-center gap-4 text-xs">
        <Stat icon="groups" label="유동인구" value={info.population} />
        <Stat icon="person" label="손님" value={info.customers} />
        <Stat icon="inventory_2" label="재고" value={info.stock} />
        <Stat icon="account_balance_wallet" label="잔액" value={info.balance?.toLocaleString()} />
      </div>
    </header>
  );
}

function Stat({ icon, label, value }: { icon: string; label: string; value?: string | number | null }) {
  return (
    <div className="flex items-center gap-1">
      <span className="material-symbols-outlined text-white/50 text-base">{icon}</span>
      <span className="text-white/50">{label}</span>
      <span className="font-medium text-white">{value ?? "—"}</span>
    </div>
  );
}

export default function Header({ variant, gameInfo, isLoggedIn = false }: HeaderProps) {
  switch (variant) {
    case "lobby":
      return <LobbyHeader isLoggedIn={isLoggedIn} />;
    case "dashboard":
      return <DashboardHeader />;
    case "game":
      return <GameHeader gameInfo={gameInfo} />;
  }
}
