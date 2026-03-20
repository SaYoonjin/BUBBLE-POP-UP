import { useEffect, useState } from "react";

interface ModalWrapperProps {
  onClose: () => void;
  children: React.ReactNode;
  panelClassName?: string;
}

export default function ModalWrapper({
  onClose,
  children,
  panelClassName,
}: ModalWrapperProps) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    requestAnimationFrame(() => setVisible(true));
  }, []);

  const handleClose = () => {
    setVisible(false);
    setTimeout(onClose, 200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div
        className={`absolute inset-0 bg-black/40 backdrop-blur-[2px] transition-opacity duration-200 ${visible ? "opacity-100" : "opacity-0"}`}
        onClick={handleClose}
      />
      <div className={`
        relative w-[420px] max-h-[85vh] bg-white rounded-[28px] shadow-2xl overflow-y-auto custom-scrollbar
        transition-all duration-300 ease-out mx-4
        ${panelClassName ?? ""}
        ${visible ? "opacity-100 scale-100 translate-y-0" : "opacity-0 scale-95 translate-y-4"}
      `}>
        <button
          onClick={handleClose}
          className="absolute top-4 right-4 z-10 text-slate-400 hover:text-slate-600 transition-colors"
        >
          <span className="material-symbols-outlined">close</span>
        </button>
        {children}
      </div>
    </div>
  );
}
