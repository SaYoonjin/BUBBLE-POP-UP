import { Link } from "react-router-dom";
import AppHeader from "../components/common/AppHeader";
import Badge from "../components/common/Badge";
import Podium from "../components/ranking/Podium";
import RankingList from "../components/ranking/RankingList";

// API 응답 타입
interface RankingEntry {
  rank: number;
  userId: number;
  nickname: string;
  storeName: string;
  locationName: string;
  menuName: string;
  roi: number;
  totalRevenue: number;
  rewardPoints: number;
  isBankrupt: boolean;
}

interface MyRankingEntry {
  rank: number | null;
  userId: number;
  nickname: string;
  storeName: string;
  locationName: string;
  menuName: string;
  roi: number;
  totalRevenue: number;
  rewardPoints: number;
  isBankrupt: boolean;
}

interface SeasonRankingResponse {
  seasonId: number;
  rankings: RankingEntry[];
  myRankings: MyRankingEntry[];
}

// TODO: Replace with API call - GET /game/seasons/current/rankings/final
const MOCK_DATA: SeasonRankingResponse = {
  seasonId: 12,
  rankings: [
    { rank: 1, userId: 42, nickname: "마라왕자", storeName: "마라왕자의 마라탕", locationName: "홍대", menuName: "마라탕", roi: 62.4, totalRevenue: 15200000, rewardPoints: 30, isBankrupt: false },
    { rank: 2, userId: 10, nickname: "빵순이", storeName: "빵순이네 붕어빵", locationName: "홍대", menuName: "붕어빵", roi: 51.8, totalRevenue: 8420000, rewardPoints: 20, isBankrupt: false },
    { rank: 3, userId: 156, nickname: "윤진이", storeName: "윤진이네 쫀쫀쿠키", locationName: "성수", menuName: "쫀쫀쿠키", roi: 38.2, totalRevenue: 6280000, rewardPoints: 15, isBankrupt: false },
    { rank: 4, userId: 20, nickname: "David Lee", storeName: "모닝 베이글 샵", locationName: "강남", menuName: "베이글", roi: 28.5, totalRevenue: 5820000, rewardPoints: 10, isBankrupt: false },
    { rank: 5, userId: 30, nickname: "Sarah Kim", storeName: "달콤 솜사탕 팩토리", locationName: "홍대", menuName: "솜사탕", roi: 24.1, totalRevenue: 4215000, rewardPoints: 10, isBankrupt: false },
    { rank: 6, userId: 40, nickname: "Mina Park", storeName: "비건 샐러드 볼", locationName: "이태원", menuName: "샐러드", roi: 19.8, totalRevenue: 3980000, rewardPoints: 5, isBankrupt: false },
    { rank: 7, userId: 50, nickname: "Jinwoo", storeName: "레트로 문방구", locationName: "명동", menuName: "문구", roi: 15.4, totalRevenue: 3540000, rewardPoints: 5, isBankrupt: false },
    { rank: 8, userId: 60, nickname: "Hanna", storeName: "플라워 팝업", locationName: "강남", menuName: "꽃다발", roi: 12.2, totalRevenue: 3120000, rewardPoints: 5, isBankrupt: false },
    { rank: 9, userId: 70, nickname: "Brian Cho", storeName: "매콤 떡볶이 연구소", locationName: "건대", menuName: "떡볶이", roi: 10.5, totalRevenue: 2890000, rewardPoints: 5, isBankrupt: false },
    { rank: 10, userId: 80, nickname: "Kelly", storeName: "제주 감귤 카페", locationName: "잠실", menuName: "감귤주스", roi: 8.4, totalRevenue: 2560000, rewardPoints: 5, isBankrupt: false },
  ],
  myRankings: [
    { rank: 3, userId: 156, nickname: "윤진이", storeName: "윤진이네 쫀쫀쿠키", locationName: "성수", menuName: "쫀쫀쿠키", roi: 38.2, totalRevenue: 6280000, rewardPoints: 15, isBankrupt: false },
    { rank: null, userId: 156, nickname: "윤진이", storeName: "윤진이네 첫 매장", locationName: "신촌", menuName: "핫도그", roi: -12.0, totalRevenue: 1200000, rewardPoints: 0, isBankrupt: true },
  ],
};

const fmt = (v: number) => `₩${v.toLocaleString()}`;

export default function RankingPage() {
  const data = MOCK_DATA;

  // myRankings의 userId로 rankings 내 본인 항목 표시
  const myUserIds = new Set(data.myRankings.map((m) => m.userId));

  const podiumEntries = data.rankings
    .filter((r) => r.rank <= 3)
    .map((r) => ({ ...r, isMe: myUserIds.has(r.userId) }));

  const listEntries = data.rankings
    .filter((r) => r.rank >= 4)
    .map((r) => ({ ...r, isMe: myUserIds.has(r.userId) }));

  // 10위 밖인 나의 기록만 별도 표시
  const myOutsideEntries = data.myRankings.filter(
    (m) => m.rank === null || m.rank > 10
  );

  return (
    <div className="min-h-screen bg-[#FDFDFB] text-slate-900 font-display flex flex-col">
      <AppHeader nickname="Owner" />

      <main className="flex-1 flex flex-col items-center py-8 px-4 sm:px-6 pt-24">
        <div className="w-full max-w-[1100px] flex flex-col gap-8">
          {/* Top buttons */}
          <div className="flex flex-col sm:flex-row justify-between gap-4 w-full">
            <Link to="/" className="flex items-center justify-center gap-2 px-6 py-2.5 rounded-2xl bg-white border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition-all shadow-sm">
              <span className="material-symbols-outlined text-xl">arrow_back</span>
              로비로 돌아가기
            </Link>
            <Link to="/mypage" className="flex items-center justify-center gap-2 px-6 py-2.5 rounded-2xl bg-primary text-white font-bold hover:bg-primary-dark transition-all shadow-sm">
              <span className="material-symbols-outlined text-xl">history</span>
              시즌 통산 기록 확인하기
            </Link>
          </div>

          {/* Title */}
          <div className="flex flex-col items-center text-center gap-2 mt-4">
            <Badge variant="gray" size="md">시즌 {data.seasonId}</Badge>
            <h1 className="text-4xl font-black leading-tight tracking-tight">시즌 랭킹</h1>
            <p className="text-slate-500 font-medium">이번 시즌 최고의 팝업스토어 마스터를 확인하세요.</p>
          </div>

          {/* Podium (1~3위) */}
          <Podium entries={podiumEntries} />

          {/* Full ranking list (4위~) */}
          <RankingList entries={listEntries} />

          {/* 나의 기록 (10위 밖 or 파산) */}
          {myOutsideEntries.length > 0 && (
            <div className="flex flex-col gap-4 pb-12">
              <h2 className="text-lg font-bold text-slate-700 flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">person</span>
                나의 기록
              </h2>
              {myOutsideEntries.map((entry, idx) => (
                <div key={idx} className={`flex items-center rounded-2xl p-4 shadow-soft border relative overflow-hidden ${
                  entry.isBankrupt
                    ? "bg-slate-50 border-slate-200"
                    : "bg-primary/5 border-primary"
                }`}>
                  <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-primary" />

                  {/* Rank */}
                  <div className="w-12 flex justify-center mr-4 ml-2">
                    <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-primary text-white">
                      {entry.rank ?? "-"}
                    </span>
                  </div>

                  {/* Avatar */}
                  <div className="size-12 rounded-full bg-slate-100 overflow-hidden mr-4 flex-shrink-0 flex items-center justify-center border-2 border-white shadow-sm ring-2 ring-primary/30">
                    <span className="material-symbols-outlined text-2xl text-slate-400">person</span>
                  </div>

                  {/* Info */}
                  <div className="flex flex-col md:flex-row md:items-center flex-1 gap-1 md:gap-4 overflow-hidden">
                    <div className="flex flex-col min-w-[140px]">
                      <span className="font-bold text-slate-900 text-lg truncate flex items-center gap-1.5">
                        {entry.nickname}
                        {entry.isBankrupt && <Badge variant="rose" size="sm">파산</Badge>}
                      </span>
                      <span className="text-xs text-primary font-bold">나 (Player)</span>
                    </div>
                    <div className="flex flex-col text-sm text-slate-500">
                      <span className="flex items-center gap-1 font-medium text-slate-700 truncate">
                        <span className="material-symbols-outlined text-base">store</span>
                        {entry.storeName}
                      </span>
                      <span className="flex items-center gap-1 text-xs truncate">
                        <span className="material-symbols-outlined text-sm">location_on</span>
                        {entry.locationName} · {entry.menuName}
                      </span>
                    </div>
                  </div>

                  {/* Stats */}
                  <div className="flex items-center gap-2 md:gap-6 ml-2">
                    <div className="hidden sm:flex flex-col items-end justify-center w-24">
                      <span className="text-xs text-slate-400 font-medium mb-0.5">ROI</span>
                      <span className={`font-bold ${entry.roi < 0 ? "text-rose-500" : "text-slate-700"}`}>
                        {entry.roi.toFixed(1)}%
                      </span>
                    </div>
                    <div className="flex flex-col items-end w-[140px]">
                      <span className="font-mono font-bold text-primary text-lg">{fmt(entry.totalRevenue)}</span>
                    </div>
                    <div className="w-16 flex justify-end">
                      <span className="bg-primary/20 text-primary-dark text-xs font-bold px-2.5 py-1 rounded-full">
                        {entry.rewardPoints}P
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
