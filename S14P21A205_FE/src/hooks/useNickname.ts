import { useEffect, useState } from "react";

const NICKNAME_STORAGE_KEY = "profileNickname";
const NICKNAME_CHANGE_EVENT = "profile:nickname-changed";
const DEFAULT_NICKNAME = "버블킹";

export function getStoredNickname() {
  try {
    const storedNickname = localStorage.getItem(NICKNAME_STORAGE_KEY)?.trim();
    return storedNickname || DEFAULT_NICKNAME;
  } catch {
    return DEFAULT_NICKNAME;
  }
}

export function setStoredNickname(nextNickname: string) {
  const trimmedNickname = nextNickname.trim();

  if (!trimmedNickname) {
    return DEFAULT_NICKNAME;
  }

  try {
    localStorage.setItem(NICKNAME_STORAGE_KEY, trimmedNickname);
    window.dispatchEvent(new Event(NICKNAME_CHANGE_EVENT));
  } catch {
    return trimmedNickname;
  }

  return trimmedNickname;
}

export default function useNickname() {
  const [nickname, setNicknameState] = useState(getStoredNickname);

  useEffect(() => {
    const syncNickname = () => {
      setNicknameState(getStoredNickname());
    };

    window.addEventListener("storage", syncNickname);
    window.addEventListener("focus", syncNickname);
    window.addEventListener(NICKNAME_CHANGE_EVENT, syncNickname);

    return () => {
      window.removeEventListener("storage", syncNickname);
      window.removeEventListener("focus", syncNickname);
      window.removeEventListener(NICKNAME_CHANGE_EVENT, syncNickname);
    };
  }, []);

  return {
    nickname,
    setNickname: (nextNickname: string) => {
      const savedNickname = setStoredNickname(nextNickname);
      setNicknameState(savedNickname);
      return savedNickname;
    },
  };
}
