import { create } from "zustand";

interface GameState {
  /** join API 응답에서 받은 playableFromDay (이번 시즌 한정) */
  playableFromDay: number | null;

  setPlayableFromDay: (day: number) => void;
  clearGame: () => void;
}

export const useGameStore = create<GameState>((set) => ({
  playableFromDay: null,

  setPlayableFromDay: (day: number) => set({ playableFromDay: day }),
  clearGame: () => set({ playableFromDay: null }),
}));
