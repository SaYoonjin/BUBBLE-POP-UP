import { useEffect, useRef, useState } from "react";

const TRACKS = [
  { id: "bgm1", label: "BGM 1", src: "/bgm/Bubblepopup_bgm1.mp3" },
  { id: "bgm2", label: "BGM 2", src: "/bgm/Bubblepopup_bgm2.mp3" },
  { id: "bgm3", label: "BGM 3", src: "/bgm/Bubblepopup_bgm3.mp3" },
  { id: "bgm4", label: "BGM 4", src: "/bgm/Bubblepopup_bgm4.mp3" },
] as const;

type TrackId = (typeof TRACKS)[number]["id"];

const DEFAULT_TRACK_ID: TrackId = "bgm1";
const STORAGE_KEYS = {
  trackId: "bubblepopup-bgm-track",
  isPlaying: "bubblepopup-bgm-playing",
} as const;
const DEFAULT_VOLUME = 0.35;

function isTrackId(value: string | null): value is TrackId {
  return TRACKS.some((track) => track.id === value);
}

function readStoredTrackId() {
  const storedValue = window.localStorage.getItem(STORAGE_KEYS.trackId);
  return isTrackId(storedValue) ? storedValue : DEFAULT_TRACK_ID;
}

function readStoredPlayingState() {
  const storedValue = window.localStorage.getItem(STORAGE_KEYS.isPlaying);
  return storedValue === null ? true : storedValue === "true";
}

export default function BgmController() {
  const [selectedTrackId, setSelectedTrackId] = useState<TrackId>(() => readStoredTrackId());
  const [isPlaying, setIsPlaying] = useState(() => readStoredPlayingState());
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [requiresInteraction, setRequiresInteraction] = useState(false);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const loadedTrackIdRef = useRef<TrackId | null>(null);

  const selectedTrack =
    TRACKS.find((track) => track.id === selectedTrackId) ?? TRACKS[0];

  useEffect(() => {
    const audio = new Audio();
    audio.loop = true;
    audio.preload = "auto";
    audio.volume = DEFAULT_VOLUME;
    audioRef.current = audio;

    return () => {
      audio.pause();
      audio.src = "";
      audioRef.current = null;
    };
  }, []);

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEYS.trackId, selectedTrackId);
  }, [selectedTrackId]);

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEYS.isPlaying, String(isPlaying));
  }, [isPlaying]);

  useEffect(() => {
    const audio = audioRef.current;

    if (!audio) {
      return;
    }

    if (loadedTrackIdRef.current !== selectedTrackId) {
      audio.src = selectedTrack.src;
      audio.currentTime = 0;
      loadedTrackIdRef.current = selectedTrackId;
    }

    if (!isPlaying) {
      audio.pause();
      setRequiresInteraction(false);
      return;
    }

    let cancelled = false;

    void audio.play().then(
      () => {
        if (!cancelled) {
          setRequiresInteraction(false);
        }
      },
      () => {
        if (!cancelled) {
          setRequiresInteraction(true);
        }
      },
    );

    return () => {
      cancelled = true;
    };
  }, [isPlaying, selectedTrackId, selectedTrack.src]);

  useEffect(() => {
    if (!requiresInteraction || !isPlaying) {
      return;
    }

    const resumePlayback = () => {
      const audio = audioRef.current;

      if (!audio) {
        return;
      }

      void audio.play().then(
        () => setRequiresInteraction(false),
        () => setRequiresInteraction(true),
      );
    };

    window.addEventListener("pointerdown", resumePlayback);
    window.addEventListener("keydown", resumePlayback);

    return () => {
      window.removeEventListener("pointerdown", resumePlayback);
      window.removeEventListener("keydown", resumePlayback);
    };
  }, [isPlaying, requiresInteraction]);

  useEffect(() => {
    if (!isPanelOpen) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      if (panelRef.current?.contains(event.target as Node)) {
        return;
      }

      setIsPanelOpen(false);
    };

    window.addEventListener("pointerdown", handlePointerDown);

    return () => {
      window.removeEventListener("pointerdown", handlePointerDown);
    };
  }, [isPanelOpen]);

  const handleTrackSelect = (trackId: TrackId) => {
    setSelectedTrackId(trackId);
    setIsPlaying(true);
  };

  const handlePlaybackToggle = () => {
    setIsPlaying((prev) => !prev);
  };

  return (
    <div
      ref={panelRef}
      className="fixed bottom-4 right-4 z-[80] flex items-end gap-3 md:bottom-6 md:right-6"
    >
      {isPanelOpen ? (
        <div className="w-[18rem] rounded-[1.75rem] border border-white/70 bg-white/92 p-4 shadow-premium backdrop-blur-md">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
                Background Music
              </p>
              <p className="mt-1 text-base font-bold text-slate-900">
                {selectedTrack.label}
              </p>
            </div>
            <button
              type="button"
              onClick={handlePlaybackToggle}
              className="flex h-10 w-10 items-center justify-center rounded-full border border-primary/25 bg-primary/10 text-primary-dark transition-colors hover:bg-primary/20"
              aria-label={isPlaying ? "Pause background music" : "Play background music"}
            >
              <span className="material-symbols-outlined text-[20px]">
                {isPlaying ? "pause" : "play_arrow"}
              </span>
            </button>
          </div>

          {requiresInteraction ? (
            <p className="mt-3 rounded-2xl border border-amber-200 bg-amber-50 px-3 py-2 text-xs leading-relaxed text-amber-700">
              Autoplay is blocked by the browser. Tap anywhere once and the BGM will start.
            </p>
          ) : null}

          <div className="mt-3 space-y-2">
            {TRACKS.map((track) => {
              const isActive = track.id === selectedTrackId;

              return (
                <button
                  key={track.id}
                  type="button"
                  onClick={() => handleTrackSelect(track.id)}
                  className={`flex w-full items-center justify-between rounded-2xl border px-3 py-3 text-left transition-all ${
                    isActive
                      ? "border-primary/40 bg-primary/10 shadow-sm"
                      : "border-slate-200 bg-white hover:border-primary/25 hover:bg-slate-50"
                  }`}
                  aria-pressed={isActive}
                >
                  <div>
                    <p className="text-sm font-semibold text-slate-900">{track.label}</p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {isActive ? "Now selected" : "Switch to this track"}
                    </p>
                  </div>
                  <span
                    className={`material-symbols-outlined text-[20px] ${
                      isActive ? "text-primary-dark" : "text-slate-300"
                    }`}
                  >
                    {isActive ? "radio_button_checked" : "radio_button_unchecked"}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      ) : null}

      <button
        type="button"
        onClick={() => setIsPanelOpen((prev) => !prev)}
        className={`flex h-14 w-14 items-center justify-center rounded-full border border-white/70 text-white shadow-lg backdrop-blur-md transition-transform hover:scale-105 ${
          isPlaying
            ? "bg-primary-dark/90"
            : "bg-slate-700/85"
        }`}
        aria-label="Open background music controls"
        aria-expanded={isPanelOpen}
      >
        <span className="material-symbols-outlined text-[26px]">
          {isPlaying ? "music_note" : "music_off"}
        </span>
      </button>
    </div>
  );
}
