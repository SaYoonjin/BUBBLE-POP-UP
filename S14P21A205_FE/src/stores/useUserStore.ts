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
      const [userRes, pointsRes] = await Promise.all([
        getUser(),
        getUserPoints(),
      ]);
      set({
        nickname: userRes.data.nickname,
        email: userRes.data.email,
        role: userRes.data.role,
        currentPoints: pointsRes.data.currentPoints,
        isLoaded: true,
      });
    } catch {
      // 401은 client interceptor가 처리
      set({ isLoaded: true });
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
