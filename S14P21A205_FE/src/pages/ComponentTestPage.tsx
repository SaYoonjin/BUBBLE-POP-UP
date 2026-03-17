import { useState } from "react";
import Button from "../components/common/Button";
import Modal from "../components/common/Modal";
import Header from "../components/common/Header";
import Badge from "../components/common/Badge";
import StatCard from "../components/common/StatCard";
import ActionButton from "../components/common/ActionButton";
import NewsCard from "../components/common/NewsCard";
import RankCard from "../components/common/RankCard";
import EventToast from "../components/common/EventToast";

export default function ComponentTestPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [newsOpen, setNewsOpen] = useState<number | null>(null);

  return (
    <div className="min-h-screen bg-background-light">
      {/* ── Header: lobby ── */}
      <section className="mb-6">
        <h3 className="px-4 py-2 bg-gray-800 text-white text-xs font-mono">Header variant="lobby"</h3>
        <Header variant="lobby" isLoggedIn={true} />
      </section>

      {/* ── Header: dashboard ── */}
      <section className="mb-6">
        <h3 className="px-4 py-2 bg-gray-800 text-white text-xs font-mono">Header variant="dashboard"</h3>
        <Header variant="dashboard" />
      </section>

      {/* ── Header: game ── */}
      <section className="mb-6">
        <h3 className="px-4 py-2 bg-gray-800 text-white text-xs font-mono">Header variant="game"</h3>
        <Header
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
          <h2 className="text-lg font-bold mb-4 text-gray-900">Button</h2>
          <div className="flex flex-wrap gap-3">
            <Button variant="primary">Primary</Button>
            <Button variant="outline">Outline</Button>
            <Button variant="ghost">Ghost</Button>
            <Button variant="danger">Danger</Button>
          </div>
          <div className="flex flex-wrap gap-3 mt-3">
            <Button size="sm">Small</Button>
            <Button size="md">Medium</Button>
            <Button size="lg">Large</Button>
          </div>
          <div className="flex flex-wrap gap-3 mt-3">
            <Button loading>Loading...</Button>
            <Button disabled>Disabled</Button>
            <Button fullWidth variant="primary">Full Width</Button>
          </div>
        </section>

        {/* ── Badge ── */}
        <section>
          <h2 className="text-lg font-bold mb-4 text-gray-900">Badge</h2>
          <div className="flex flex-wrap gap-3">
            <Badge variant="green">영업 중</Badge>
            <Badge variant="rose">파산</Badge>
            <Badge variant="gray">준비 중</Badge>
            <Badge variant="gold" size="md">1위</Badge>
          </div>
        </section>

        {/* ── StatCard ── */}
        <section>
          <h2 className="text-lg font-bold mb-4 text-gray-900">StatCard</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <StatCard label="오늘 매출" value="₩810,000" change={{ value: "12%", positive: true }} icon="trending_up" />
            <StatCard label="재고" value="155개" change={{ value: "30개", positive: false }} icon="inventory_2" />
            <StatCard label="잔액" value="₩8,700,000" icon="account_balance_wallet" />
            <StatCard label="연속 적자일" value="3일" icon="warning" highlight />
          </div>
        </section>

        {/* ── ActionButton ── */}
        <section>
          <h2 className="text-lg font-bold mb-4 text-gray-900">ActionButton (영업 중 도크)</h2>
          <div className="flex gap-3 p-4 bg-gray-50 rounded-2xl">
            <ActionButton icon="campaign" label="홍보" onClick={() => alert("홍보!")} />
            <ActionButton icon="discount" label="할인" active />
            <ActionButton icon="volunteer_activism" label="나눔" />
            <ActionButton icon="local_shipping" label="긴급발주" disabled />
          </div>
        </section>

        {/* ── Modal ── */}
        <section>
          <h2 className="text-lg font-bold mb-4 text-gray-900">Modal</h2>
          <Button onClick={() => setModalOpen(true)}>모달 열기</Button>
          <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title="홍보 선택">
            <p className="text-sm text-gray-600 mb-4">어떤 홍보를 실행할까요?</p>
            <div className="space-y-2">
              <Button fullWidth variant="primary">인플루언서 (500,000원)</Button>
              <Button fullWidth variant="outline">SNS (300,000원)</Button>
              <Button fullWidth variant="outline">전단지 (100,000원)</Button>
              <Button fullWidth variant="ghost">지인소개 (무료)</Button>
            </div>
          </Modal>
        </section>

        {/* ── NewsCard ── */}
        <section>
          <h2 className="text-lg font-bold mb-4 text-gray-900">NewsCard</h2>
          <div className="space-y-2">
            <NewsCard
              title="🔥 강남 지역 축제 개최! 유동인구 20% 증가"
              content="오늘 16시부터 강남 지역에서 대규모 축제가 시작됩니다. 유동인구가 크게 늘어날 것으로 예상되니 재고를 충분히 확보해두세요."
              isOpen={newsOpen === 0}
              onToggle={() => setNewsOpen(newsOpen === 0 ? null : 0)}
              variant="featured"
            />
            <NewsCard
              title="타코야끼 트렌드 순위 2위 → 인기 상승 중"
              content="이번 시즌 타코야끼의 인기가 급상승하고 있습니다. 판매가 조정을 고려해보세요."
              isOpen={newsOpen === 1}
              onToggle={() => setNewsOpen(newsOpen === 1 ? null : 1)}
            />
            <NewsCard
              title="내일 날씨: 비 (유동인구 30% 감소 예상)"
              content="내일은 비가 올 것으로 예상됩니다. 발주 수량을 줄이는 것을 권장합니다."
              isOpen={newsOpen === 2}
              onToggle={() => setNewsOpen(newsOpen === 2 ? null : 2)}
            />
          </div>
        </section>

        {/* ── RankCard ── */}
        <section>
          <h2 className="text-lg font-bold mb-4 text-gray-900">RankCard (포디엄)</h2>
          <div className="flex items-end justify-center gap-4">
            <RankCard rank={2} username="유진" storeName="핫도그천국" roi="28.5%" revenue="₩12,400,000" />
            <RankCard rank={1} username="지원" storeName="타코의민족" roi="35.2%" revenue="₩15,800,000" isMe />
            <RankCard rank={3} username="민수" storeName="붕어빵나라" roi="22.1%" revenue="₩9,200,000" />
          </div>
        </section>

        {/* ── EventToast ── */}
        <section>
          <h2 className="text-lg font-bold mb-4 text-gray-900">EventToast</h2>
          <div className="w-80 space-y-2">
            <EventToast
              type="GOOD_NEWS"
              title="축제 효과 발동!"
              description="강남 지역 유동인구 +20%"
              timeAgo="방금"
            />
            <EventToast
              type="WARNING"
              title="한파 주의보"
              description="유동인구가 40% 감소합니다"
              timeAgo="2분 전"
            />
            <EventToast
              type="SYSTEM"
              title="긴급발주 도착"
              description="50개가 입고되었습니다"
              timeAgo="5분 전"
            />
          </div>
        </section>
      </div>
    </div>
  );
}
