import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import AppHeader from "../components/common/AppHeader";
import CountdownTimer from "../components/common/CountdownTimer";
import MenuSelector from "../components/game/MenuSelector";
import PriceSlider from "../components/game/PriceSlider";
import QuantityCounter from "../components/game/QuantityCounter";
import CozyNewspaper from "../components/game/CozyNewspaper";
import { getStoreMenus, type StoreMenuResponse } from "../api/store";
import bubbleNewsImage from "../assets/Bubblenewsimg.png";
import {
  applyDiscount,
  getSelectedDiscountMultiplier,
  getSelectedDiscountPercent,
  getStoredSelectedDashboardItems,
} from "../utils/dashboardItems";

interface PrepMenu {
  id: number;
  emoji: string;
  name: string;
  costPrice: number;
  previousSalePrice: number;
}

const fallbackMenus: PrepMenu[] = [
  { id: 1, emoji: "🍞", name: "빵", costPrice: 1800, previousSalePrice: 3200 },
  { id: 2, emoji: "🍢", name: "마라꼬치", costPrice: 2200, previousSalePrice: 3900 },
  { id: 3, emoji: "🍬", name: "젤리", costPrice: 900, previousSalePrice: 1800 },
  { id: 4, emoji: "🍽️", name: "떡볶이", costPrice: 2500, previousSalePrice: 4300 },
  { id: 5, emoji: "🍔", name: "햄버거", costPrice: 3100, previousSalePrice: 5600 },
  { id: 6, emoji: "🍨", name: "아이스크림", costPrice: 1400, previousSalePrice: 2600 },
  { id: 7, emoji: "🍗", name: "닭강정", costPrice: 2800, previousSalePrice: 4900 },
  { id: 8, emoji: "🌮", name: "타코", costPrice: 2600, previousSalePrice: 4500 },
  { id: 9, emoji: "🌭", name: "핫도그", costPrice: 1700, previousSalePrice: 3000 },
  { id: 10, emoji: "🧋", name: "버블티", costPrice: 2300, previousSalePrice: 4100 },
];

const mockPopulationRanking = [
  { rank: 1, name: "홍대", change: "2.4%", positive: true, barWidth: "85%" },
  { rank: 2, name: "성수", change: "-", positive: true, barWidth: "65%" },
  { rank: 3, name: "부산", change: "0.8%", positive: false, barWidth: "45%" },
];

const mockRevenueRanking = [
  { rank: 1, name: "홍대", change: "5.2%", positive: true, barWidth: "92%" },
  { rank: 2, name: "성수", change: "-", positive: true, barWidth: "58%" },
  { rank: 3, name: "부산", change: "-", positive: true, barWidth: "35%" },
];

const mockNews = [
  { id: 1, title: "요즘 뜨는 디저트, '약과 쿠키' 인기 급상승 중", content: "전통 간식인 약과와 서양식 쿠키를 결합한 '약과 쿠키'가 MZ세대를 중심으로 큰 인기를 끌고 있습니다. 특히 편의점과 프랜차이즈 카페들이 앞다퉈 신제품을 출시하며 경쟁이 치열해지고 있습니다. 전문가들은 이러한 '할매니얼(할머니+밀레니얼)' 트렌드가 당분간 지속될 것으로 전망했습니다." },
  { id: 2, title: "원두 가격 3개월 만에 소폭 하락세... 카페 사장님들 '안도'", content: "국제 원두 가격이 3개월 만에 하락세로 돌아섰습니다. 브라질 수확량 증가와 물류비 안정이 주요 원인으로 분석됩니다." },
  { id: 3, title: "이번 주말 전국 비 예보, 배달 주문량 증가 예상", content: "기상청에 따르면 이번 주말 전국적으로 비가 내릴 예정입니다. 외출이 줄어드는 만큼 배달 수요가 늘어날 것으로 보입니다." },
  { id: 4, title: "성공적인 카페 운영을 위한 5가지 인테리어 팁", content: "작은 인테리어 변화가 매출에 큰 영향을 줄 수 있습니다. 조명, 좌석 배치, 음악, 향기, 그린 인테리어 5가지를 소개합니다." },
  { id: 5, title: "홍대 주변, 20대 유동인구 지난달 대비 15% 증가", content: "홍대 일대의 20대 유동인구가 지난달 대비 15% 증가한 것으로 나타났습니다. 봄 시즌 개강 효과와 맞물린 것으로 분석됩니다." },
];

type Tab = "prep" | "news";

function roundToHundreds(value: number) {
  return Math.round(value / 100) * 100;
}

function getRecommendedPrice(costPrice: number) {
  return roundToHundreds(costPrice * 1.6);
}

function getSellingPriceDefault(
  costPrice: number,
  previousSalePrice: number,
  day: number,
) {
  if (day >= 2) {
    return previousSalePrice;
  }

  return getRecommendedPrice(costPrice);
}

function isRegularOrderDay(day: number) {
  return day >= 1 && day <= 7 && day % 2 === 1;
}

function mapStoreMenusToPrepMenus(menus: StoreMenuResponse[]) {
  return menus.map((menu) => {
    const fallbackMenu = fallbackMenus.find((entry) => entry.id === menu.menuId);

    return {
      id: menu.menuId,
      emoji: fallbackMenu?.emoji ?? "🍽️",
      name: menu.menuName,
      costPrice: menu.ingredientPrice,
      previousSalePrice:
        fallbackMenu?.previousSalePrice ?? getRecommendedPrice(menu.ingredientPrice),
    } satisfies PrepMenu;
  });
}

export default function PrepPage() {
  const { day: dayParam } = useParams<{ day: string }>();
  const parsedDay = Number(dayParam);
  const day = Number.isNaN(parsedDay) ? 0 : parsedDay;
  const canPrepareToday = isRegularOrderDay(day);
  const [tab, setTab] = useState<Tab>("prep");
  const [menus, setMenus] = useState<PrepMenu[]>(fallbackMenus);
  const [isMenusLoading, setIsMenusLoading] = useState(true);
  const [menuError, setMenuError] = useState<string | null>(null);
  const [selectedMenu, setSelectedMenu] = useState<number | null>(1);
  const [selectedDashboardItems] = useState(getStoredSelectedDashboardItems);
  const [quantity, setQuantity] = useState(120);
  const [expandedNewsId, setExpandedNewsId] = useState<number | null>(1);
  const selectedMenuData = menus.find((menu) => menu.id === selectedMenu) ?? menus[0] ?? fallbackMenus[0];
  const originalCostPrice = selectedMenuData.costPrice;
  const ingredientDiscountMultiplier = getSelectedDiscountMultiplier(
    selectedDashboardItems,
    "INGREDIENT",
  );
  const hasItemDiscount = ingredientDiscountMultiplier < 1;
  const discountRate = getSelectedDiscountPercent(
    selectedDashboardItems,
    "INGREDIENT",
  );
  const discountedCostPrice = applyDiscount(
    originalCostPrice,
    ingredientDiscountMultiplier,
  );
  const recommendedPrice = getRecommendedPrice(originalCostPrice);
  const maxSellingPrice = roundToHundreds(recommendedPrice * 2);
  const defaultSellingPrice = getSellingPriceDefault(
    originalCostPrice,
    selectedMenuData.previousSalePrice,
    day,
  );
  const [price, setPrice] = useState(defaultSellingPrice);
  const totalCost = originalCostPrice * quantity;
  const discountedTotalCost = discountedCostPrice * quantity;
  const discountAmount = totalCost - discountedTotalCost;
  const newsSidebarSections =
    day === 1
      ? [
          {
            title: "유동인구 순위",
            eyebrow: "Foot Traffic Ranking",
            items: mockPopulationRanking,
          },
          {
            title: "오픈 현장",
            eyebrow: "",
            imageSrc: bubbleNewsImage,
            caption: "개점 첫날, 도심 한복판에 긴 대기 행렬",
            captionDetail: "초기 반응이 기대치를 웃돌면서 첫날 흥행 가능성에 관심이 쏠리고 있다.",
            meta: ["DAY 1 현장 스케치"],
            imageAlt: "개점 첫날, 팝업 숍 앞에 모여든 방문객들",
          },
        ]
      : [
          {
            title: "유동인구 순위",
            eyebrow: "Foot Traffic Ranking",
            items: mockPopulationRanking,
          },
          {
            title: "지역 매출 순위",
            eyebrow: "Regional Revenue Ranking",
            items: mockRevenueRanking,
          },
        ];

  useEffect(() => {
    let isActive = true;

    const loadMenus = async () => {
      try {
        const { menus: fetchedMenus } = await getStoreMenus();

        if (!isActive) {
          return;
        }

        if (fetchedMenus.length === 0) {
          setMenuError("메뉴 정보를 불러오지 못했습니다. 기본 메뉴 목록을 표시합니다.");
          return;
        }

        const nextMenus = mapStoreMenusToPrepMenus(fetchedMenus);
        setMenus(nextMenus);
        setSelectedMenu((currentMenuId) =>
          nextMenus.some((menu) => menu.id === currentMenuId) ? currentMenuId : nextMenus[0].id,
        );
        setMenuError(null);
      } catch {
        if (!isActive) {
          return;
        }

        setMenuError("메뉴 정보를 불러오지 못했습니다. 기본 메뉴 목록을 표시합니다.");
      } finally {
        if (isActive) {
          setIsMenusLoading(false);
        }
      }
    };

    void loadMenus();

    return () => {
      isActive = false;
    };
  }, []);

  useEffect(() => {
    setPrice(defaultSellingPrice);
  }, [defaultSellingPrice]);

  useEffect(() => {
    setTab("prep");
  }, [day]);

  return (
    <div className="min-h-screen bg-[#FDFDFB] text-slate-900 font-display flex flex-col">
      <AppHeader />

      {/* Main */}
      <main className="flex-1 flex flex-col items-center py-6 pt-24 px-4 sm:px-8">
        <div className="w-full max-w-[1000px] flex flex-col gap-6">
          {/* Page Header */}
          <div className="flex flex-col gap-5">
            <div className="flex flex-col gap-2.5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-2 text-slate-400 text-sm font-medium">
                    <span className="material-symbols-outlined text-[1.25rem]">calendar_today</span>
                    <span>DAY {day}</span>
                  </div>
                  <CountdownTimer initialSeconds={50} label="준비 시간" />
                </div>
                <h1 className="text-slate-900 text-3xl md:text-[2rem] font-black leading-tight tracking-tight">
                  {tab === "prep" ? "영업 준비" : "버블 뉴스"}
                </h1>
            </div>

            {/* Tabs */}
            <div className="border-b border-slate-100">
              <div className="flex items-center gap-6">
                <button
                  onClick={() => setTab("prep")}
                  className={`pb-2.5 text-[15px] transition-colors ${
                    tab === "prep"
                      ? "border-b-2 border-slate-900 text-slate-900 font-bold"
                      : "text-slate-400 hover:text-slate-600 font-medium"
                  }`}
                >
                  영업 준비
                </button>
                <button
                  onClick={() => setTab("news")}
                  className={`pb-2.5 text-[15px] transition-colors ${
                    tab === "news"
                      ? "border-b-2 border-slate-900 text-slate-900 font-bold"
                      : "text-slate-400 hover:text-slate-600 font-medium"
                  }`}
                >
                  버블 뉴스
                </button>
              </div>
            </div>
          </div>

          {/* Tab: 영업 준비 */}
          {tab === "prep" ? (
            <div className="flex flex-col gap-6">
              {!canPrepareToday && (
                <div className="flex items-start gap-3 rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3 text-sm text-slate-500">
                  <span className="material-symbols-outlined mt-0.5 text-base text-slate-400">
                    event_busy
                  </span>
                  <p className="leading-6">
                    오늘은 정규발주일이 아니어서 영업 준비를 진행할 수 없습니다. 버블 뉴스를 확인해 다음 영업일을 준비하세요.
                  </p>
                </div>
              )}

              <div
                className={`flex flex-col gap-6 transition-opacity ${
                  canPrepareToday ? "" : "pointer-events-none select-none opacity-40"
                }`}
                aria-disabled={!canPrepareToday}
              >
                {(isMenusLoading || menuError) && (
                  <div
                    className={`flex items-start gap-3 rounded-2xl px-4 py-3 text-sm ${
                      menuError
                        ? "border border-amber-200 bg-amber-50/80 text-amber-700"
                        : "border border-slate-200 bg-slate-50/80 text-slate-500"
                    }`}
                  >
                    <span className="material-symbols-outlined mt-0.5 text-base">
                      {menuError ? "error" : "hourglass_top"}
                    </span>
                    <p className="leading-6">
                      {menuError ?? "메뉴 정보를 불러오는 중입니다."}
                    </p>
                  </div>
                )}

                <MenuSelector menus={menus} selectedId={selectedMenu} onSelect={setSelectedMenu} />

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <PriceSlider
                    menuName={selectedMenuData.name}
                    price={price}
                    min={originalCostPrice}
                    max={maxSellingPrice}
                    step={100}
                    originalCostPrice={originalCostPrice}
                    discountedCostPrice={discountedCostPrice}
                    hasItemDiscount={hasItemDiscount}
                    defaultPrice={defaultSellingPrice}
                    defaultPriceLabel={day >= 2 ? "이전 판매가" : "권장가"}
                    onChange={setPrice}
                  />
                  <div className="flex flex-col gap-5">
                    <QuantityCounter quantity={quantity} min={50} max={500} step={10} onChange={setQuantity} />
                    <div className="flex flex-col gap-3.5">
                      {hasItemDiscount && (
                        <div className="rounded-2xl border border-red-100 bg-red-50/60 px-4 py-3 flex items-center justify-between">
                          <div>
                            <p className="text-sm font-bold text-red-500">선택 아이템 혜택 적용</p>
                            <p className="text-xs text-red-400 mt-1">아이템 사용으로 원재료 비용 {discountRate}% 할인</p>
                          </div>
                          <span className="text-lg font-bold text-red-500">-₩{discountAmount.toLocaleString()}</span>
                        </div>
                      )}
                      <div className="flex items-center justify-between px-4 py-2">
                        <span className="text-sm text-slate-500 font-medium">총 예상 비용</span>
                        <div className="text-right">
                          {hasItemDiscount && (
                            <p className="text-sm font-bold text-red-400 line-through decoration-2">
                              ₩{totalCost.toLocaleString()}
                            </p>
                          )}
                          <span className="text-[1.75rem] font-black text-slate-900 tracking-tight">
                            ₩{discountedTotalCost.toLocaleString()}
                          </span>
                        </div>
                      </div>
                      <button className="w-full bg-primary hover:bg-primary-dark text-slate-900 hover:text-white font-bold text-base py-4 px-6 rounded-2xl shadow-lg shadow-primary/20 transition-all flex items-center justify-center gap-2 group">
                        <span>준비 완료하기</span>
                        <span className="material-symbols-outlined group-hover:translate-x-1 transition-transform">
                          arrow_forward
                        </span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            /* Tab: 버블 뉴스 */
            <CozyNewspaper
              items={mockNews}
              expandedId={expandedNewsId}
              onToggle={(id) => setExpandedNewsId(expandedNewsId === id ? null : id)}
              day={day}
              rankings={newsSidebarSections}
            />
          )}
        </div>
      </main>
    </div>
  );
}
