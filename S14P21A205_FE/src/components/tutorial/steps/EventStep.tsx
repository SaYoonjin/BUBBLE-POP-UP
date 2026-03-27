import { useEffect, useRef, useState } from "react";
import type { GameAlert } from "../../play/EventSidebar";
import TutorialPlayLayout from "../TutorialPlayLayout";
import { MOCK_EVENTS, EVENT_ICON_MAP, type EventCategory } from "../mockData";
import { sendToUnity } from "../../../utils/unity";
import { useEventEffectStore } from "../../play/effects/useEventEffect";
import type { EventEffectType } from "../../play/effects/effects";

/** 서울숲/성수 = locationId 5 → 0-based index 4 */
const SEONGSU_REGION_INDEX = 4;

const DISASTER_UNITY_WEATHER: Record<string, string> = {
  disaster_earthquake: `Earthquake,${SEONGSU_REGION_INDEX}`,
  disaster_flood: `Rain,${SEONGSU_REGION_INDEX}`,
  disaster_typhoon: `Wind,${SEONGSU_REGION_INDEX}`,
  disaster_fire: `Fire,${SEONGSU_REGION_INDEX}`,
};

const CATEGORY_TO_EFFECT: Record<string, EventEffectType> = {
  celebrity: "CELEBRITY_APPEARANCE",
  holiday: "SUBSTITUTE_HOLIDAY",
  subsidy: "GOVERNMENT_SUBSIDY",
  price_down: "PRICE_DOWN",
  price_up: "PRICE_UP",
  disaster_earthquake: "EARTHQUAKE",
  disaster_flood: "FLOOD",
  disaster_typhoon: "TYPHOON",
  disaster_fire: "FIRE",
  disease: "INFECTIOUS_DISEASE",
  policy: "POLICY_CHANGE",
  festival: "FESTIVAL",
};

/** 이벤트별 실제 게임 효과 데이터 */
interface EventEffectInfo {
  effects: string[];
}

const EVENT_EFFECT_INFO: Record<EventCategory, EventEffectInfo> = {
  celebrity: { effects: ["유동인구 증가"] },
  holiday: { effects: ["유동인구 증가"] },
  subsidy: { effects: ["자본금 증가", "유동인구 소폭 증가"] },
  price_down: { effects: ["해당 메뉴 원재료 가격 하락"] },
  price_up: { effects: ["해당 메뉴 원재료 가격 상승"] },
  disaster_earthquake: { effects: ["해당 지역 재고 대량 손실", "해당 지역 유동인구 감소"] },
  disaster_flood: { effects: ["해당 지역 재고 대량 손실", "해당 지역 유동인구 감소"] },
  disaster_typhoon: { effects: ["해당 지역 재고 대량 손실", "해당 지역 유동인구 감소"] },
  disaster_fire: { effects: ["해당 지역 재고 대량 손실", "해당 지역 유동인구 감소"] },
  disease: { effects: ["유동인구 큰 폭 감소"] },
  policy: { effects: ["전체 원가 상승"] },
  festival: { effects: ["해당 지역 유동인구 대폭 증가"] },
};

export default function EventStep() {
  const [alerts, setAlerts] = useState<GameAlert[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<EventCategory | null>(null);
  const [hoveredCategory, setHoveredCategory] = useState<EventCategory | null>(null);
  const unityIframeRef = useRef<HTMLIFrameElement | null>(null);
  const restoreTimerRef = useRef<number | null>(null);
  const buttonRefs = useRef<Map<EventCategory, HTMLButtonElement>>(new Map());
  const triggerEffect = useEventEffectStore((s) => s.triggerEffect);
  const activeEffect = useEventEffectStore((s) => s.activeEffect);

  // 이펙트가 끝나면 버튼 하이라이트 해제
  useEffect(() => {
    if (activeEffect === null) {
      setSelectedCategory(null);
    }
  }, [activeEffect]);

  const triggerUnityWeather = (category: string) => {
    const unityEffect = DISASTER_UNITY_WEATHER[category];
    if (unityEffect) {
      if (restoreTimerRef.current) clearTimeout(restoreTimerRef.current);
      sendToUnity(unityIframeRef, "SetWeather", unityEffect);
      restoreTimerRef.current = window.setTimeout(() => {
        sendToUnity(unityIframeRef, "SetWeather", `Clear,${SEONGSU_REGION_INDEX}`);
        restoreTimerRef.current = null;
      }, 15000);
    }
  };

  const handleEventTrigger = (event: typeof MOCK_EVENTS[0]) => {
    // 이펙트가 진행 중이면 다음 이벤트 실행 불가
    if (activeEffect !== null) return;

    const newAlert: GameAlert = {
      id: Date.now(),
      type: event.isPositive ? "event" : "bad_event",
      title: event.alertTitle ?? event.name,
      description: event.description,
      createdAt: Date.now(),
    };
    setAlerts((prev) => [newAlert, ...prev]);
    setSelectedCategory(event.category);

    const effectType = CATEGORY_TO_EFFECT[event.category];
    if (effectType) triggerEffect(effectType);
    triggerUnityWeather(event.category);
  };

  const hoveredEvent = hoveredCategory
    ? MOCK_EVENTS.find((e) => e.category === hoveredCategory)
    : null;
  const hoveredInfo = hoveredCategory ? EVENT_EFFECT_INFO[hoveredCategory] : null;

  // 툴팁 위치 계산
  const getTooltipPosition = (category: EventCategory) => {
    const btn = buttonRefs.current.get(category);
    if (!btn) return { left: 0 };
    const rect = btn.getBoundingClientRect();
    const parentRect = btn.closest(".absolute")?.getBoundingClientRect();
    if (!parentRect) return { left: 0 };
    // 버튼 중앙 기준, 부모 컨테이너 내에서의 상대 위치
    const centerX = rect.left - parentRect.left + rect.width / 2;
    // 툴팁 폭(280px)의 절반만큼 빼서 중앙 정렬, 화면 밖 안 나가도록 클램프
    const tooltipW = 280;
    const left = Math.max(8, Math.min(centerX - tooltipW / 2, parentRect.width - tooltipW - 8));
    return { left };
  };

  return (
    <TutorialPlayLayout alerts={alerts} unityIframeRef={unityIframeRef} showActionBar={false}>
      {/* 이벤트 도감 — 하단 고정, 한 줄 */}
      <div className="absolute inset-x-0 bottom-0 z-30">
        {/* 호버 툴팁 — 이벤트 바 바로 위, 컴팩트 */}
        {hoveredCategory && hoveredEvent && hoveredInfo && (
          <div
            className="absolute bottom-full mb-1.5 z-40 pointer-events-none"
            style={{ left: getTooltipPosition(hoveredCategory).left, width: 240 }}
          >
            <div className={`rounded-lg shadow-md border px-3 py-2 text-[11px] ${
              hoveredEvent.isPositive
                ? "bg-white border-green-200"
                : "bg-white border-rose-200"
            }`}>
              {/* 효과 */}
              <div className="flex items-center gap-1 flex-wrap">
                {hoveredInfo.effects.map((effect, i) => (
                  <span key={i} className={`font-bold ${
                    hoveredEvent.isPositive ? "text-green-700" : "text-rose-700"
                  }`}>{effect}{i < hoveredInfo.effects.length - 1 ? "," : ""}</span>
                ))}
              </div>
            </div>
          </div>
        )}

        <div className="bg-white/95 backdrop-blur-sm border-t border-slate-200 shadow-[0_-4px_20px_rgba(0,0,0,0.08)]">
          <div className="flex items-center gap-1.5 px-3 py-2 overflow-x-auto custom-scrollbar">
            <span className="shrink-0 text-[11px] font-bold text-slate-600 flex items-center gap-1 mr-1">
              <span className="material-symbols-outlined text-primary text-sm">menu_book</span>
              이벤트
            </span>
            {MOCK_EVENTS.map((event) => (
              <button
                key={event.category}
                ref={(el) => { if (el) buttonRefs.current.set(event.category, el); }}
                onClick={() => handleEventTrigger(event)}
                onMouseEnter={() => setHoveredCategory(event.category)}
                onMouseLeave={() => setHoveredCategory(null)}
                disabled={activeEffect !== null}
                className={`shrink-0 flex items-center gap-1 px-2 py-1 rounded-lg border text-[11px] transition-all ${
                  activeEffect !== null && selectedCategory !== event.category
                    ? "border-slate-100 bg-slate-50 opacity-50 cursor-not-allowed"
                    : selectedCategory === event.category
                      ? event.isPositive
                        ? "border-green-300 bg-green-50 font-bold"
                        : "border-rose-300 bg-rose-50 font-bold"
                      : "border-slate-100 bg-white hover:border-slate-300 hover:shadow-sm"
                }`}
              >
                <span className="text-xs">{EVENT_ICON_MAP[event.category]}</span>
                <span className="text-slate-700 whitespace-nowrap">{event.name}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </TutorialPlayLayout>
  );
}
