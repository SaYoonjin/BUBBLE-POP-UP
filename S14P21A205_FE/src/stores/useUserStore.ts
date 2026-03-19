import { create } from "zustand";
import { getUser, getUserPoints, patchNickname } from "../api/user";

interface UserState {
  nickname: string | null;
  email: string | null;
  role: string | null;
  currentPoints: number | null;
  isLoaded: boolean;

  fetchUser: () => Promise<void>;
  updateNickname: (nickname: string) => Promise<void>;
  clearUser: () => void;
}

export const useUserStore = create<UserState>((set) => ({
  nickname: null,
  email: null,
  role: null,
  currentPoints: null,
  isLoaded: false,

  fetchUser: async () => {
    try {
      // 유저 정보는 필수, 포인트는 실패해도 OK
      const userRes = await getUser();
      let points: number | null = null;
      try {
        const pointsData = await getUserPoints();
        points = pointsData.currentPoints;
      } catch { /* 포인트 조회 실패는 무시 */ }

      set({
        nickname: userRes.data.nickname,
        email: userRes.data.email,
        role: userRes.data.role,
        currentPoints: points,
        isLoaded: true,
      });
    } catch {
      // getUser 자체 실패 → 기존 데이터 유지하고 로드 완료 표시
      set((prev) => ({ ...prev, isLoaded: true }));
    }
  },

  updateNickname: async (nickname: string) => {
    const res = await patchNickname(nickname);
    set({ nickname: res.data.nickname });
  },

  clearUser: () =>
      set({
        nickname: null,
        email: null,
        role: null,
        currentPoints: null,
        isLoaded: false,
      }),
}));
