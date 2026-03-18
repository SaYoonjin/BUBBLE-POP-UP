import { useEffect, useRef, useState } from "react";

interface SeasonSortOptionItem {
  value: string;
  label: string;
}

interface SeasonSortDropdownProps {
  value: string;
  options: SeasonSortOptionItem[];
  onChange: (value: string) => void;
}

export default function SeasonSortDropdown({
  value,
  options,
  onChange,
}: SeasonSortDropdownProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const selectedOption = options.find((option) => option.value === value) ?? options[0];

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-700 shadow-soft transition-colors hover:border-primary/35"
      >
        <span className="flex items-center gap-2">
          <span className="material-symbols-outlined text-[18px] text-slate-400">
            tune
          </span>
          <span>{selectedOption?.label}</span>
        </span>
        <span
          className={`material-symbols-outlined text-slate-400 transition-transform ${
            open ? "rotate-180" : ""
          }`}
        >
          expand_more
        </span>
      </button>

      {open && (
        <div className="absolute right-0 top-[calc(100%+0.5rem)] z-20 w-full overflow-hidden rounded-2xl border border-slate-200 bg-white p-2 shadow-premium">
          {options.map((option) => {
            const selected = option.value === value;

            return (
              <button
                key={option.value}
                type="button"
                onClick={() => {
                  onChange(option.value);
                  setOpen(false);
                }}
                className={`flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-left text-sm transition-colors ${
                  selected
                    ? "bg-primary/10 font-semibold text-primary-dark"
                    : "text-slate-600 hover:bg-slate-50"
                }`}
              >
                <span>{option.label}</span>
                {selected && (
                  <span className="material-symbols-outlined text-[18px] text-primary">
                    check
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
