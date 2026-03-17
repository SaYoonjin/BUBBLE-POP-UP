import { useEffect } from "react";

interface CozyModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
}

export default function CozyModal({ isOpen, onClose, title, children }: CozyModalProps) {
  useEffect(() => {
    if (isOpen) document.body.style.overflow = "hidden";
    else document.body.style.overflow = "";
    return () => { document.body.style.overflow = ""; };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-cozy-ink/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-cozy-paper rounded-2xl shadow-cozy-float p-0 w-full max-w-md mx-4 overflow-hidden border-4 border-white">
        {/* Paper texture */}
        <div className="absolute inset-0 opacity-30 pointer-events-none"
          style={{ backgroundImage: "radial-gradient(#d4c5b0 0.5px, transparent 0.5px)", backgroundSize: "10px 10px" }}
        />
        {/* Pin */}
        <div className="absolute -top-2 left-1/2 -translate-x-1/2 size-6 bg-red-500 rounded-full shadow-md border-2 border-red-700 z-20" />
        <div className="relative z-10 p-6 pt-8">
          <div className="flex items-center justify-between mb-4">
            {title && <h2 className="font-cozy-serif text-xl font-bold text-cozy-ink italic">{title}</h2>}
            <button onClick={onClose} className="ml-auto p-1 rounded-lg hover:bg-cozy-warm/50 transition-colors">
              <span className="material-symbols-outlined text-cozy-wood-dark">close</span>
            </button>
          </div>
          {children}
        </div>
      </div>
    </div>
  );
}
