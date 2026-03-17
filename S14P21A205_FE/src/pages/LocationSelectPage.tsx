import { useState } from "react";
import { useNavigate } from "react-router-dom";
import DistrictMap from "../components/game/DistrictMap";
import DistrictDetailPanel from "../components/game/DistrictDetailPanel";

const districts = [
  { id: 1, name: "홍대", x: "22%", y: "30%", rent: "₩400,000", congestion: "High", grade: "A등급", tags: ["Youth", "Music", "Art"], description: "젊음과 문화의 거리. 인디 음악과 스트릿 아트가 공존하는 MZ세대의 핫플레이스." },
  { id: 2, name: "여의도", x: "18%", y: "52%", rent: "₩350,000", congestion: "Medium", grade: "B등급", tags: ["Finance", "Office"], description: "금융의 중심지. 직장인 대상 런치 팝업이 유리합니다." },
  { id: 3, name: "명동", x: "45%", y: "28%", rent: "₩500,000", congestion: "Very High", grade: "S등급", tags: ["Tourist", "Shopping"], description: "관광객의 성지. 외국인 방문객이 가장 많은 쇼핑 거리." },
  { id: 4, name: "이태원", x: "48%", y: "48%", rent: "₩300,000", congestion: "Medium", grade: "B등급", tags: ["Global", "Food"], description: "다국적 문화가 공존하는 거리. 이색적인 팝업에 적합." },
  { id: 5, name: "성수", x: "68%", y: "32%", rent: "₩300,000", congestion: "High", grade: "S등급", tags: ["Hip_Vibe", "Cafe_Tour", "Fashion"], description: "MZ세대의 놀이터이자 팝업스토어 성지. 트렌디한 카페와 편집숍이 즐비한 핫플레이스." },
  { id: 6, name: "건대", x: "78%", y: "42%", rent: "₩250,000", congestion: "Medium", grade: "B등급", tags: ["University", "Nightlife"], description: "대학가 특유의 활기. 저렴한 임대료 대비 높은 유동인구." },
  { id: 7, name: "강남", x: "55%", y: "68%", rent: "₩600,000", congestion: "Very High", grade: "S등급", tags: ["Premium", "Business"], description: "대한민국 최고의 상권. 높은 임대료만큼 높은 수익 잠재력." },
  { id: 8, name: "잠실", x: "80%", y: "62%", rent: "₩350,000", congestion: "High", grade: "A등급", tags: ["Family", "Entertainment"], description: "롯데월드와 석촌호수. 가족 단위 방문객이 많은 지역." },
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

export default function LocationSelectPage() {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const navigate = useNavigate();

  const selectedDistrict = districts.find((d) => d.id === selectedId);

  const handleComplete = (brandName: string) => {
    alert(`${selectedDistrict?.name}에 "${brandName}" 팝업스토어를 오픈합니다!`);
    navigate("/game/1/prep");
  };

  return (
    <div
      className="h-screen w-full overflow-hidden text-slate-800 font-display relative"
      style={{
        backgroundColor: "#F8F9FA",
        backgroundImage: "radial-gradient(#CBD5E1 1.5px, transparent 1.5px)",
        backgroundSize: "40px 40px",
      }}
    >
      {/* Top bar */}
      <div className="absolute top-0 left-0 w-full z-[60] p-6 pointer-events-none flex justify-between items-start">
        {/* Title */}
        <div className="pointer-events-auto flex items-center gap-4">
          <div className="bg-white rounded-2xl shadow-md h-14 px-6 flex items-center gap-3 border border-slate-100">
            <span className="material-symbols-outlined text-indigo-500 text-2xl">location_on</span>
            <div>
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider leading-none">Step 1</p>
              <p className="text-base font-bold text-slate-800 leading-tight">지역 선택</p>
            </div>
          </div>
          <p className="text-sm text-slate-400 hidden sm:block">지역에 마우스를 올려 정보를 확인하세요</p>
        </div>

        {/* Capital */}
        <div className="pointer-events-auto">
          <div className="bg-white rounded-2xl shadow-md h-14 px-5 flex items-center gap-3 border border-slate-100">
            <span className="material-symbols-outlined text-amber-500 text-xl">account_balance_wallet</span>
            <div className="flex flex-col items-end leading-tight">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">초기자본</span>
              <span className="text-indigo-600 font-mono font-bold text-lg">₩5,000,000</span>
            </div>
          </div>
        </div>
      </div>

      {/* Map */}
      <DistrictMap
        districts={districts}
        connections={connections}
        selectedId={selectedId}
        onSelect={setSelectedId}
      />

      {/* Detail Modal */}
      {selectedDistrict && (
        <DistrictDetailPanel
          district={selectedDistrict}
          onComplete={handleComplete}
          onClose={() => setSelectedId(null)}
        />
      )}
    </div>
  );
}
