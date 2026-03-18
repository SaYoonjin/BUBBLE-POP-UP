import { useState } from "react";
import AppHeader from "../components/common/AppHeader";
import Badge from "../components/common/Badge";
import NicknameEditModal from "../components/mypage/NicknameEditModal";
import ProfileSummaryCard from "../components/mypage/ProfileSummaryCard";
import SeasonHistoryCard from "../components/mypage/SeasonHistoryCard";
import SeasonHistoryEmptyState from "../components/mypage/SeasonHistoryEmptyState";

type RankVariant = "gold" | "gray" | "rose";
type SeasonStatus = "default" | "bankrupt" | "comeback";

interface SeasonHistoryItem {
  id: string;
  season: number;
  rank: string;
  rankValue: number | null;
  rankVariant: RankVariant;
  status: SeasonStatus;
  location: string;
  storeName: string;
  revenue: string;
  rewardPoints: string;
}

const seasonHistory: SeasonHistoryItem[] = [
  {
    id: "season-5-bankrupt",
    season: 5,
    rank: "파산",
    rankValue: null,
    rankVariant: "rose",
    status: "bankrupt",
    location: "강남",
    storeName: "디저트 팝업 스토어",
    revenue: "₩0",
    rewardPoints: "0P",
  },
  {
    id: "season-5-comeback",
    season: 5,
    rank: "2위",
    rankValue: 2,
    rankVariant: "gray",
    status: "comeback",
    location: "강남",
    storeName: "디저트 팝업 스토어",
    revenue: "₩1,120,000",
    rewardPoints: "420P",
  },
  {
    id: "season-4-bankrupt",
    season: 4,
    rank: "파산",
    rankValue: null,
    rankVariant: "rose",
    status: "bankrupt",
    location: "성수",
    storeName: "수제 맥주 팝업",
    revenue: "₩0",
    rewardPoints: "0P",
  },
  {
    id: "season-3-default",
    season: 3,
    rank: "1위",
    rankValue: 1,
    rankVariant: "gold",
    status: "default",
    location: "홍대",
    storeName: "패션 악세사리 팝업",
    revenue: "₩1,340,000",
    rewardPoints: "500P",
  },
  {
    id: "season-2-default",
    season: 2,
    rank: "4위",
    rankValue: 4,
    rankVariant: "gray",
    status: "default",
    location: "이태원",
    storeName: "비건 베이커리",
    revenue: "₩780,000",
    rewardPoints: "180P",
  },
  {
    id: "season-1-default",
    season: 1,
    rank: "8위",
    rankValue: 8,
    rankVariant: "gray",
    status: "default",
    location: "명동",
    storeName: "타코야끼 팝업",
    revenue: "₩320,000",
    rewardPoints: "30P",
  },
];

const MAX_VISIBLE_SEASONS = 10;

const seasonStatusPriority: Record<SeasonStatus, number> = {
  comeback: 0,
  default: 1,
  bankrupt: 2,
};

export default function MyPage() {
  const [nickname, setNickname] = useState("버블킹");
  const [isNicknameModalOpen, setIsNicknameModalOpen] = useState(false);
  const [draftNickname, setDraftNickname] = useState("버블킹");
  const [nicknameError, setNicknameError] = useState("");

  const seasonHistoryBySeason = seasonHistory.reduce<Map<number, SeasonHistoryItem[]>>(
    (groups, record) => {
      const existingSeasonRecords = groups.get(record.season);

      if (existingSeasonRecords) {
        existingSeasonRecords.push(record);
      } else {
        groups.set(record.season, [record]);
      }

      return groups;
    },
    new Map(),
  );

  const visibleSeasonGroups = Array.from(seasonHistoryBySeason.entries())
    .sort(([seasonA], [seasonB]) => seasonB - seasonA)
    .slice(0, MAX_VISIBLE_SEASONS)
    .map(([season, records]) => [
      season,
      [...records].sort(
        (recordA, recordB) =>
          seasonStatusPriority[recordA.status] - seasonStatusPriority[recordB.status],
      ),
    ] as const);
  const visibleSeasonHistory = visibleSeasonGroups.flatMap(([, records]) => records);
  const rankedSeasons = seasonHistory.filter(
    (season): season is SeasonHistoryItem & { rankValue: number } => season.rankValue !== null,
  );
  const bestRankLabel =
    rankedSeasons.length > 0
      ? `${Math.min(...rankedSeasons.map((season) => season.rankValue))}위`
      : "-";
  const summaryBadges = [
    `참여 시즌 ${seasonHistoryBySeason.size}회`,
    `최고 순위 ${bestRankLabel}`,
  ];

  const openNicknameModal = () => {
    setDraftNickname(nickname);
    setNicknameError("");
    setIsNicknameModalOpen(true);
  };

  const closeNicknameModal = () => {
    setDraftNickname(nickname);
    setNicknameError("");
    setIsNicknameModalOpen(false);
  };

  const saveNickname = () => {
    const nextNickname = draftNickname.trim();

    if (!nextNickname) {
      setNicknameError("닉네임을 입력해 주세요.");
      return;
    }

    setNickname(nextNickname);
    setDraftNickname(nextNickname);
    setNicknameError("");
    setIsNicknameModalOpen(false);
  };

  return (
    <div className="flex min-h-screen flex-col bg-[#FDFDFB] font-display text-slate-900">
      <AppHeader nickname={nickname} />

      <main className="flex-grow w-full max-w-6xl mx-auto px-4 py-10 pt-24 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-[320px_minmax(0,1fr)] lg:items-start">
          <section className="lg:sticky lg:top-24">
            <ProfileSummaryCard
              nickname={nickname}
              email="user@ssafy.com"
              summaryBadges={summaryBadges}
              onEditNickname={openNicknameModal}
            />
          </section>

          <section className="space-y-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
              <div className="space-y-2">
                <p className="text-sm font-semibold uppercase tracking-[0.24em] text-primary-dark/80">
                  History
                </p>
                <div>
                  <h2 className="text-3xl font-bold tracking-tight text-slate-900">
                    시즌 기록
                  </h2>
                  <p className="mt-2 text-sm leading-6 text-slate-500">
                    최근 10개 시즌 기준으로 핵심 기록만 정리해서 보여드려요.
                  </p>
                </div>
              </div>

              <Badge variant="gray" size="md">
                {visibleSeasonGroups.length > 0 ? `최근 ${visibleSeasonGroups.length}개 시즌` : "기록 없음"}
              </Badge>
            </div>

            {visibleSeasonGroups.length > 0 ? (
              <div className="space-y-4">
                {visibleSeasonHistory.map((season) => (
                  <SeasonHistoryCard
                    key={season.id}
                    season={season.season}
                    location={season.location}
                    storeName={season.storeName}
                    revenue={season.revenue}
                    rewardPoints={season.rewardPoints}
                    rank={season.rank}
                    rankVariant={season.rankVariant}
                    status={season.status}
                  />
                ))}
              </div>
            ) : (
              <SeasonHistoryEmptyState nickname={nickname} />
            )}
          </section>
        </div>
      </main>

      <footer className="mt-12 border-t border-gray-100 bg-white py-8">
        <div className="mx-auto max-w-6xl px-4 text-center text-xs text-gray-400">
          <p>© 2026 BubbleBubble. All rights reserved.</p>
          <div className="mt-2 space-x-4">
            <a className="hover:underline" href="#">
              이용약관
            </a>
            <a className="hover:underline" href="#">
              개인정보처리방침
            </a>
          </div>
        </div>
      </footer>

      <NicknameEditModal
        isOpen={isNicknameModalOpen}
        nickname={draftNickname}
        error={nicknameError}
        onChange={(value) => {
          setDraftNickname(value);
          if (nicknameError) {
            setNicknameError("");
          }
        }}
        onClose={closeNicknameModal}
        onSave={saveNickname}
      />
    </div>
  );
}
