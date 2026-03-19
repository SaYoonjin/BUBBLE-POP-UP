import { Link } from "react-router-dom";
import AppHeader from "../components/common/AppHeader";
import Podium from "../components/ranking/Podium";
import RankingList from "../components/ranking/RankingList";

// TODO: Replace with API data
const MOCK_RANKINGS = [
  { rank: 1, name: "마라왕자", storeName: "마라왕자의 마라탕", location: "성수", roi: "62.4%", revenue: "₩124,500,000", reward: "50P" },
  { rank: 2, name: "빵순이", storeName: "빵순이네 붕어빵", location: "홍대", roi: "51.8%", revenue: "₩84,200,000", reward: "30P" },
  { rank: 3, name: "윤진이", storeName: "윤진이네 챱챱쿠키", location: "성수", roi: "38.2%", revenue: "₩62,800,000", reward: "30P", isMe: true },
  { rank: 4, name: "David Lee", storeName: "모닝 베이글 샵", location: "강남", roi: "28.5%", revenue: "₩58,200,000", reward: "20P" },
  { rank: 5, name: "Sarah Kim", storeName: "달콤 솜사탕 팩토리", location: "홍대", roi: "24.1%", revenue: "₩42,150,000", reward: "20P" },
  { rank: 6, name: "Mina Park", storeName: "비건 샐러드 볼", location: "이태원", roi: "19.8%", revenue: "₩39,800,000", reward: "10P" },
  { rank: 7, name: "Jinwoo", storeName: "레트로 문방구", location: "명동", roi: "15.4%", revenue: "₩35,400,000", reward: "10P" },
  { rank: 8, name: "Hanna", storeName: "플라워 팝업", location: "강남", roi: "12.2%", revenue: "₩31,200,000", reward: "10P" },
  { rank: 9, name: "Brian Cho", storeName: "매콤 떡볶이 연구소", location: "건대", roi: "10.5%", revenue: "₩28,900,000", reward: "10P" },
  { rank: 10, name: "Kelly", storeName: "제주 감귤 카페", location: "잠실", roi: "8.4%", revenue: "₩25,600,000", reward: "10P" },
];

export default function RankingPage() {
  const podiumEntries = MOCK_RANKINGS.filter((r) => r.rank <= 3);
  const listEntries = MOCK_RANKINGS.filter((r) => r.rank >= 3); // 3위부터 (본인 포함)

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
            <h1 className="text-4xl font-black leading-tight tracking-tight">시즌 랭킹</h1>
            <p className="text-slate-500 font-medium">이번 시즌 최고의 팝업스토어 마스터를 확인하세요.</p>
          </div>

          {/* Podium */}
          <Podium entries={podiumEntries} />

          {/* Full ranking list (3위부터) */}
          <RankingList entries={listEntries} />
        </div>
      </main>
    </div>
  );
}
