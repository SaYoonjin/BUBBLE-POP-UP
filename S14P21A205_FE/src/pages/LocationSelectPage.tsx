import { useState } from "react";
import { useNavigate } from "react-router-dom";
import CountdownTimer from "../components/common/CountdownTimer";
import DistrictDetailPanel from "../components/game/DistrictDetailPanel";
import DistrictMap from "../components/game/DistrictMap";

type District = {
  id: number;
  name: string;
  x: string;
  y: string;
  rent: string;
  congestion: "매우 혼잡" | "혼잡" | "보통" | "여유" | "매우 여유";
  grade: "S등급" | "A등급" | "B등급";
  tags: string[];
  description: string;
};

const districts: District[] = [
  {
    id: 1,
    name: "홍대",
    x: "22%",
    y: "29%",
    rent: "₩400,000",
    congestion: "혼잡",
    grade: "A등급",
    tags: ["Youth", "Music", "Art"],
    description: "젊음과 문화의 거리. 인디 음악과 스트릿 아트가 공존하는 MZ세대의 핫플레이스.",
  },
  {
    id: 2,
    name: "여의도",
    x: "18%",
    y: "54%",
    rent: "₩350,000",
    congestion: "매우 여유",
    grade: "B등급",
    tags: ["Finance", "Office"],
    description: "금융의 중심지. 직장인 대상 런치 팝업이 유리합니다.",
  },
  {
    id: 3,
    name: "명동",
    x: "46%",
    y: "29%",
    rent: "₩500,000",
    congestion: "매우 혼잡",
    grade: "S등급",
    tags: ["Tourist", "Shopping"],
    description: "관광객의 성지. 외국인 방문객이 가장 많은 쇼핑 거리입니다.",
  },
  {
    id: 4,
    name: "이태원",
    x: "48%",
    y: "49%",
    rent: "₩300,000",
    congestion: "보통",
    grade: "B등급",
    tags: ["Global", "Food"],
    description: "다국적 문화가 공존하는 거리. 이색적인 팝업에 적합한 지역입니다.",
  },
  {
    id: 5,
    name: "성수",
    x: "68%",
    y: "33%",
    rent: "₩300,000",
    congestion: "혼잡",
    grade: "S등급",
    tags: ["Hip Vibe", "Cafe Tour", "Fashion"],
    description: "MZ세대의 놀이터이자 팝업스토어 성지. 트렌디한 카페와 편집숍이 즐비합니다.",
  },
  {
    id: 6,
    name: "건대",
    x: "79%",
    y: "43%",
    rent: "₩250,000",
    congestion: "여유",
    grade: "B등급",
    tags: ["University", "Nightlife"],
    description: "대학가 특유의 활기. 합리적인 임대료 대비 유동인구가 많은 지역입니다.",
  },
  {
    id: 7,
    name: "강남",
    x: "56%",
    y: "69%",
    rent: "₩600,000",
    congestion: "매우 혼잡",
    grade: "S등급",
    tags: ["Premium", "Business"],
    description: "대한민국 최고의 상권. 높은 임대료만큼 높은 수익 잠재력이 기대됩니다.",
  },
  {
    id: 8,
    name: "잠실",
    x: "81%",
    y: "63%",
    rent: "₩350,000",
    congestion: "보통",
    grade: "A등급",
    tags: ["Family", "Entertainment"],
    description: "롯데월드와 석촌호수를 중심으로 가족 단위 방문객이 많은 지역입니다.",
  },
];

const connections = [
  { from: 1, to: 3 },
  { from: 2, to: 4 },
  { from: 3, to: 4 },
  { from: 4, to: 7 },
  { from: 7, to: 8 },
  { from: 5, to: 6 },
  { from: 6, to: 8 },
  { from: 4, to: 5 },
  { from: 1, to: 2 },
  { from: 3, to: 5 },
];

const LOCATION_SELECTION_SECONDS = 120;

function parseCurrency(value: string) {
  return Number(value.replace(/[^\d]/g, ""));
}

function formatCurrency(value: number) {
  return `₩${value.toLocaleString("ko-KR")}`;
}

export default function LocationSelectPage() {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const navigate = useNavigate();

  const selectedDistrict = districts.find((district) => district.id === selectedId);
  const selectedInteriorCost = selectedDistrict
    ? Math.round(parseCurrency(selectedDistrict.rent) * 7 * 0.1)
    : null;

  const handleComplete = (brandName: string) => {
    alert(`${selectedDistrict?.name}에 "${brandName}" 팝업스토어를 오픈합니다.`);
    navigate("/game/1/prep");
  };

  const handleTimerComplete = () => {
    navigate("/game/1/prep");
  };

  return (
    <div className="relative min-h-screen w-full overflow-hidden bg-[#FDFDFB] font-display text-slate-800">
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundColor: "#FDFDFB",
          backgroundImage:
            "radial-gradient(circle at top left, rgba(168,191,169,0.18), transparent 34%), radial-gradient(circle at bottom right, rgba(212,165,165,0.14), transparent 30%), linear-gradient(180deg, rgba(255,255,255,0.0), rgba(255,255,255,0.45))",
        }}
      />
      <div className="pointer-events-none absolute inset-0 opacity-[0.05] [background-image:radial-gradient(#334155_1px,transparent_1px)] [background-size:24px_24px]" />
      <div className="absolute left-[-8%] top-[8%] size-56 rounded-full bg-primary/10 blur-3xl" />
      <div className="absolute bottom-[10%] right-[-6%] size-64 rounded-full bg-accent-rose/10 blur-3xl" />

      <div className="relative z-10 flex min-h-screen flex-col px-4 py-4 sm:px-6 sm:py-6">
        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
            <div className="flex flex-col gap-3">
              <div className="flex items-center gap-4">
                <div className="flex h-16 items-center gap-3 rounded-[22px] border border-white/70 bg-white/90 px-6 shadow-premium backdrop-blur">
                  <div className="flex size-10 items-center justify-center rounded-2xl bg-primary/15 text-primary-dark">
                    <span className="material-symbols-outlined text-2xl">location_on</span>
                  </div>
                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">
                      Step 1
                    </p>
                    <p className="text-base font-bold leading-tight text-slate-800">지역 선택</p>
                  </div>
                </div>
                <p className="hidden text-sm text-slate-500 sm:block">
                  2분 안에 지역을 고르고, 팝업 브랜드명을 입력하세요.
                </p>
              </div>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-stretch">
              <div className="flex min-h-16 items-center gap-3 rounded-[22px] border border-white/70 bg-white/90 px-5 py-3 shadow-premium backdrop-blur">
                <div className="flex size-10 items-center justify-center rounded-2xl bg-amber-100 text-amber-600">
                  <span className="material-symbols-outlined text-xl">account_balance_wallet</span>
                </div>
                <div className="flex flex-col leading-tight">
                  <span className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">
                    초기 자본
                  </span>
                  <span className="font-mono text-lg font-bold text-primary-dark">₩10,000,000</span>
                </div>
              </div>

              <div className="flex min-h-16 min-w-[196px] flex-col justify-center rounded-[22px] border border-white/70 bg-white/90 px-5 py-3 shadow-premium backdrop-blur">
                <span className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">
                  제한 시간
                </span>
                <div className="mt-1">
                  <CountdownTimer
                    initialSeconds={LOCATION_SELECTION_SECONDS}
                    label="지역 선택 제한 시간"
                    onComplete={handleTimerComplete}
                    variant="inline"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-6 flex-1">
          <DistrictMap
            districts={districts}
            connections={connections}
            selectedId={selectedId}
            onSelect={setSelectedId}
          />
        </div>
      </div>

      {selectedDistrict && (
        <DistrictDetailPanel
          district={selectedDistrict}
          interiorCost={selectedInteriorCost !== null ? formatCurrency(selectedInteriorCost) : null}
          onComplete={handleComplete}
          onClose={() => setSelectedId(null)}
        />
      )}
    </div>
  );
}
