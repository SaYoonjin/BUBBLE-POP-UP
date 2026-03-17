import { useState } from "react";
import CozyButton from "../components/cozy/CozyButton";
import CozyModal from "../components/cozy/CozyModal";
import CozyHeader from "../components/cozy/CozyHeader";
import CozyBadge from "../components/cozy/CozyBadge";
import CozyStatCard from "../components/cozy/CozyStatCard";
import CozyActionButton from "../components/cozy/CozyActionButton";
import CozyNewsCard from "../components/cozy/CozyNewsCard";
import CozyRankCard from "../components/cozy/CozyRankCard";
import CozyEventToast from "../components/cozy/CozyEventToast";

export default function CozyTestPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [newsOpen, setNewsOpen] = useState<number | null>(null);

  return (
    <div className="min-h-screen bg-cozy-warm/30 font-cozy-display text-cozy-ink">
      {/* ── Header: lobby ── */}
      <section className="mb-6">
        <h3 className="px-4 py-2 bg-[#5d4037] text-white text-xs font-mono">CozyHeader variant="lobby"</h3>
        <CozyHeader variant="lobby" isLoggedIn={true} />
      </section>

      {/* ── Header: game ── */}
      <section className="mb-6">
        <h3 className="px-4 py-2 bg-[#5d4037] text-white text-xs font-mono">CozyHeader variant="game"</h3>
        <CozyHeader
          variant="game"
          gameInfo={{
            location: "강남",
            storeName: "나의 팝업스토어",
            menu: "타코야끼",
            day: 3,
            timer: "14:30",
            population: 45,
            customers: 12,
            stock: 155,
            balance: 8700000,
          }}
        />
      </section>

      <div className="max-w-4xl mx-auto px-6 py-8 space-y-12">
        {/* ── Button ── */}
        <section>
          <h2 className="text-2xl font-black text-[#5D4037] mb-4 font-cozy-display">CozyButton</h2>
          <div className="flex flex-wrap gap-3">
            <CozyButton variant="primary">Primary</CozyButton>
            <CozyButton variant="wood">Wood</CozyButton>
            <CozyButton variant="ghost">Ghost</CozyButton>
            <CozyButton variant="danger">Danger</CozyButton>
          </div>
          <div className="flex flex-wrap gap-3 mt-3">
            <CozyButton size="sm">Small</CozyButton>
            <CozyButton size="md">Medium</CozyButton>
            <CozyButton size="lg">Large</CozyButton>
          </div>
          <div className="flex flex-wrap gap-3 mt-3">
            <CozyButton loading>Loading...</CozyButton>
            <CozyButton disabled>Disabled</CozyButton>
            <CozyButton fullWidth variant="primary">Full Width</CozyButton>
          </div>
        </section>

        {/* ── Badge ── */}
        <section>
          <h2 className="text-2xl font-black text-[#5D4037] mb-4">CozyBadge</h2>
          <div className="flex flex-wrap gap-3">
            <CozyBadge variant="green">영업 중</CozyBadge>
            <CozyBadge variant="rose">파산</CozyBadge>
            <CozyBadge variant="gray">준비 중</CozyBadge>
            <CozyBadge variant="gold" size="md">Best Seller</CozyBadge>
          </div>
        </section>

        {/* ── StatCard ── */}
        <section>
          <h2 className="text-2xl font-black text-[#5D4037] mb-4">CozyStatCard</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <CozyStatCard label="오늘 매출" value="₩810,000" change={{ value: "12%", positive: true }} icon="trending_up" />
            <CozyStatCard label="재고" value="155개" change={{ value: "30개", positive: false }} icon="inventory_2" />
            <CozyStatCard label="잔액" value="₩8.7M" icon="savings" />
            <CozyStatCard label="연속 적자" value="3일" icon="warning" highlight />
          </div>
        </section>

        {/* ── ActionButton ── */}
        <section>
          <h2 className="text-2xl font-black text-[#5D4037] mb-4">CozyActionButton (영업 중 도크)</h2>
          <div className="flex gap-3 p-6 bg-cozy-cream rounded-2xl shadow-inner border-2 border-white">
            <CozyActionButton icon="campaign" label="홍보" onClick={() => alert("홍보!")} />
            <CozyActionButton icon="discount" label="할인" active />
            <CozyActionButton icon="volunteer_activism" label="나눔" />
            <CozyActionButton icon="local_shipping" label="긴급발주" disabled />
          </div>
        </section>

        {/* ── Modal ── */}
        <section>
          <h2 className="text-2xl font-black text-[#5D4037] mb-4">CozyModal</h2>
          <CozyButton onClick={() => setModalOpen(true)}>모달 열기</CozyButton>
          <CozyModal isOpen={modalOpen} onClose={() => setModalOpen(false)} title="Today's Special">
            <p className="text-sm text-cozy-ink/70 mb-4 font-cozy-display">어떤 홍보를 실행할까요?</p>
            <div className="space-y-2">
              <CozyButton fullWidth variant="primary">인플루언서 (500,000원)</CozyButton>
              <CozyButton fullWidth variant="wood">SNS (300,000원)</CozyButton>
              <CozyButton fullWidth variant="ghost">전단지 (100,000원)</CozyButton>
              <CozyButton fullWidth variant="ghost">지인소개 (무료)</CozyButton>
            </div>
          </CozyModal>
        </section>

        {/* ── NewsCard ── */}
        <section>
          <h2 className="text-2xl font-black text-[#5D4037] mb-4">CozyNewsCard</h2>
          <div className="space-y-3">
            <CozyNewsCard
              title="강남 지역 축제 개최! 유동인구 20% 증가"
              content="오늘 16시부터 강남 지역에서 대규모 축제가 시작됩니다. 유동인구가 크게 늘어날 것으로 예상되니 재고를 충분히 확보해두세요."
              isOpen={newsOpen === 0}
              onToggle={() => setNewsOpen(newsOpen === 0 ? null : 0)}
              variant="featured"
            />
            <CozyNewsCard
              title="타코야끼 트렌드 순위 2위 — 인기 상승 중"
              content="이번 시즌 타코야끼의 인기가 급상승하고 있습니다. 판매가 조정을 고려해보세요."
              isOpen={newsOpen === 1}
              onToggle={() => setNewsOpen(newsOpen === 1 ? null : 1)}
            />
            <CozyNewsCard
              title="내일 날씨: 비 (유동인구 30% 감소 예상)"
              content="내일은 비가 올 것으로 예상됩니다. 발주 수량을 줄이는 것을 권장합니다."
              isOpen={newsOpen === 2}
              onToggle={() => setNewsOpen(newsOpen === 2 ? null : 2)}
            />
          </div>
        </section>

        {/* ── RankCard ── */}
        <section>
          <h2 className="text-2xl font-black text-[#5D4037] mb-4">CozyRankCard (포디엄)</h2>
          <div className="flex items-end justify-center gap-4">
            <CozyRankCard rank={2} username="유진" storeName="핫도그천국" roi="28.5%" revenue="₩12.4M" />
            <CozyRankCard rank={1} username="지원" storeName="타코의민족" roi="35.2%" revenue="₩15.8M" isMe />
            <CozyRankCard rank={3} username="민수" storeName="붕어빵나라" roi="22.1%" revenue="₩9.2M" />
          </div>
        </section>

        {/* ── EventToast ── */}
        <section>
          <h2 className="text-2xl font-black text-[#5D4037] mb-4">CozyEventToast</h2>
          <div className="w-80 space-y-3">
            <CozyEventToast type="GOOD_NEWS" title="축제 효과 발동!" description="강남 지역 유동인구 +20%" timeAgo="방금" />
            <CozyEventToast type="WARNING" title="한파 주의보" description="유동인구가 40% 감소합니다" timeAgo="2분 전" />
            <CozyEventToast type="SYSTEM" title="긴급발주 도착" description="50개가 입고되었습니다" timeAgo="5분 전" />
          </div>
        </section>
      </div>
    </div>
  );
}
