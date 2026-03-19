import { useEffect, useState } from "react";

const BRAND_NAME_STORAGE_KEY = "brandName";
const BRAND_NAME_CHANGE_EVENT = "profile:brand-name-changed";
const DEFAULT_BRAND_NAME = "버블스토리";

function dispatchBrandNameChange() {
  window.dispatchEvent(new Event(BRAND_NAME_CHANGE_EVENT));
}

export function getStoredBrandName() {
  try {
    const storedBrandName = localStorage.getItem(BRAND_NAME_STORAGE_KEY)?.trim();
    return storedBrandName || DEFAULT_BRAND_NAME;
  } catch {
    return DEFAULT_BRAND_NAME;
  }
}

export function setStoredBrandName(nextBrandName: string) {
  const trimmedBrandName = nextBrandName.trim();

  if (!trimmedBrandName) {
    return DEFAULT_BRAND_NAME;
  }

  try {
    localStorage.setItem(BRAND_NAME_STORAGE_KEY, trimmedBrandName);
    dispatchBrandNameChange();
  } catch {
    return trimmedBrandName;
  }

  return trimmedBrandName;
}

export function clearStoredBrandName() {
  try {
    localStorage.removeItem(BRAND_NAME_STORAGE_KEY);
    dispatchBrandNameChange();
  } catch {
    // Ignore storage access failures and fall back to default brand name.
  }

  return DEFAULT_BRAND_NAME;
}

export default function useBrandName() {
  const [brandName, setBrandNameState] = useState(getStoredBrandName);

  useEffect(() => {
    const syncBrandName = () => {
      setBrandNameState(getStoredBrandName());
    };

    window.addEventListener("storage", syncBrandName);
    window.addEventListener("focus", syncBrandName);
    window.addEventListener(BRAND_NAME_CHANGE_EVENT, syncBrandName);

    return () => {
      window.removeEventListener("storage", syncBrandName);
      window.removeEventListener("focus", syncBrandName);
      window.removeEventListener(BRAND_NAME_CHANGE_EVENT, syncBrandName);
    };
  }, []);

  return {
    brandName,
    setBrandName: (nextBrandName: string) => {
      const savedBrandName = setStoredBrandName(nextBrandName);
      setBrandNameState(savedBrandName);
      return savedBrandName;
    },
    clearBrandName: () => {
      const clearedBrandName = clearStoredBrandName();
      setBrandNameState(clearedBrandName);
      return clearedBrandName;
    },
  };
}
