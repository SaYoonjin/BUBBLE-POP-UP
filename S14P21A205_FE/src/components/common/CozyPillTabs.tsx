interface Tab {
  key: string;
  label: string;
  icon?: string;
}

interface CozyPillTabsProps {
  tabs: Tab[];
  activeKey: string;
  onChange: (key: string) => void;
}

export default function CozyPillTabs({ tabs, activeKey, onChange }: CozyPillTabsProps) {
  return (
    <div className="flex items-center justify-center gap-2 bg-cozy-cream rounded-full p-1.5 shadow-inner border border-cozy-wood-light w-fit mx-auto">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          onClick={() => onChange(tab.key)}
          className={`px-6 py-2 rounded-full text-sm font-bold transition-all ${
            tab.key === activeKey
              ? "bg-white text-cozy-ink shadow-md"
              : "text-cozy-wood-dark hover:text-cozy-ink"
          }`}
        >
          {tab.icon && (
            <span className="material-symbols-outlined text-[16px] align-middle mr-1">{tab.icon}</span>
          )}
          {tab.label}
        </button>
      ))}
    </div>
  );
}
