import { useState, useEffect } from "react";

interface DistrictInfo {
  name: string;
  rent: string;
  congestion: string;
  grade: string;
  tags: string[];
  description: string;
}

interface DistrictDetailPanelProps {
  district: DistrictInfo;
  onComplete: (brandName: string) => void;
  onClose: () => void;
}

export default function DistrictDetailPanel({ district, onComplete, onClose }: DistrictDetailPanelProps) {
  const [brandName, setBrandName] = useState("");
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    requestAnimationFrame(() => setVisible(true));
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = ""; };
  }, []);

  const handleClose = () => {
    setVisible(false);
    setTimeout(onClose, 200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className={`absolute inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity duration-200 ${visible ? "opacity-100" : "opacity-0"}`}
        onClick={handleClose}
      />

      {/* Modal */}
      <div className={`
        relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl mx-4 overflow-hidden
        transition-all duration-300 ease-out
        ${visible ? "opacity-100 scale-100 translate-y-0" : "opacity-0 scale-95 translate-y-4"}
      `}>
        {/* Close button */}
        <button
          onClick={handleClose}
          className="absolute top-4 right-4 z-20 w-9 h-9 rounded-full bg-slate-100 hover:bg-slate-200 flex items-center justify-center transition-colors"
        >
          <span className="material-symbols-outlined text-slate-500 text-xl">close</span>
        </button>

        {/* Header image area */}
        <div className="relative h-40 bg-gradient-to-br from-indigo-500 to-indigo-700 flex items-end p-6">
          <div className="absolute inset-0 opacity-10" style={{
            backgroundImage: "radial-gradient(white 1px, transparent 1px)",
            backgroundSize: "20px 20px",
          }} />
          <div className="absolute top-4 left-5 flex gap-2">
            <span className="bg-white/20 backdrop-blur-sm text-white text-[11px] font-bold px-3 py-1 rounded-full">
              {district.grade}
            </span>
            {district.congestion === "Very High" && (
              <span className="bg-rose-500/90 text-white text-[11px] font-bold px-3 py-1 rounded-full flex items-center gap-1">
                HOT
              </span>
            )}
          </div>
          <div>
            <h2 className="text-3xl font-bold text-white drop-shadow-sm">{district.name}</h2>
            <p className="text-indigo-100 text-sm mt-0.5">SEOUL, KOREA</p>
          </div>
        </div>

        {/* Body */}
        <div className="p-6">
          <p className="text-slate-500 text-sm leading-relaxed mb-5">{district.description}</p>

          {/* Tags */}
          <div className="flex flex-wrap gap-2 mb-5">
            {district.tags.map((tag) => (
              <span key={tag} className="px-3 py-1 rounded-full bg-indigo-50 text-indigo-600 text-xs font-semibold border border-indigo-100">
                #{tag}
              </span>
            ))}
          </div>

          {/* Stats */}
          <div className="grid grid-cols-2 gap-3 mb-6">
            <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide block mb-1">일일 임대료</span>
              <span className="text-lg font-bold text-slate-800 font-mono">{district.rent}</span>
            </div>
            <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide block mb-1">혼잡도</span>
              <span className={`text-lg font-bold ${
                district.congestion === "Very High" ? "text-rose-500" :
                district.congestion === "High" ? "text-amber-500" : "text-emerald-500"
              }`}>{district.congestion}</span>
            </div>
          </div>

          {/* Brand input */}
          <div className="mb-4">
            <label className="text-sm font-semibold text-slate-600 mb-2 block">팝업 브랜드명</label>
            <input
              type="text"
              value={brandName}
              onChange={(e) => setBrandName(e.target.value)}
              className="w-full h-12 rounded-xl bg-slate-50 border border-slate-200 focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100 transition-all px-4 text-base font-medium placeholder:text-slate-300 text-slate-800"
              placeholder="브랜드 이름을 입력하세요"
              autoFocus
            />
          </div>

          {/* Submit button */}
          <button
            onClick={() => brandName.trim() && onComplete(brandName.trim())}
            disabled={!brandName.trim()}
            className="w-full h-12 bg-indigo-500 hover:bg-indigo-600 text-white text-base font-bold rounded-xl shadow-md hover:shadow-lg transition-all active:scale-[0.99] flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <span>이 지역에 오픈하기</span>
            <span className="material-symbols-outlined text-lg">arrow_forward</span>
          </button>
        </div>
      </div>
    </div>
  );
}
