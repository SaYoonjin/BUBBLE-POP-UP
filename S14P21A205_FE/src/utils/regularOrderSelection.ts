export interface StoredRegularOrderSelection {
  menuId: number;
  price: number;
}

const REGULAR_ORDER_STORAGE_KEY_PREFIX = "bubblepopup-regular-order";
const REGULAR_ORDER_LATEST_STORAGE_KEY = `${REGULAR_ORDER_STORAGE_KEY_PREFIX}-latest`;

interface RegularOrderSelectionScope {
  seasonNumber: number | null;
  playableDay: number | null;
}

function getScopedStorageKey({ seasonNumber, playableDay }: RegularOrderSelectionScope) {
  if (
    seasonNumber == null ||
    !Number.isFinite(seasonNumber) ||
    playableDay == null ||
    !Number.isFinite(playableDay)
  ) {
    return null;
  }

  return `${REGULAR_ORDER_STORAGE_KEY_PREFIX}-${seasonNumber}-${playableDay}`;
}

function parseStoredRegularOrderSelection(rawValue: string | null) {
  if (!rawValue) {
    return null;
  }

  try {
    const parsedValue = JSON.parse(rawValue) as Partial<StoredRegularOrderSelection>;

    if (
      typeof parsedValue.menuId !== "number" ||
      !Number.isFinite(parsedValue.menuId) ||
      typeof parsedValue.price !== "number" ||
      !Number.isFinite(parsedValue.price)
    ) {
      return null;
    }

    return {
      menuId: parsedValue.menuId,
      price: parsedValue.price,
    } satisfies StoredRegularOrderSelection;
  } catch {
    return null;
  }
}

export function readStoredRegularOrderSelection(
  scope: RegularOrderSelectionScope,
  options?: { includeLatestFallback?: boolean },
) {
  try {
    const scopedKey = getScopedStorageKey(scope);

    if (scopedKey) {
      const scopedValue = parseStoredRegularOrderSelection(
        window.localStorage.getItem(scopedKey),
      );

      if (scopedValue) {
        return scopedValue;
      }
    }

    if (options?.includeLatestFallback) {
      return parseStoredRegularOrderSelection(
        window.localStorage.getItem(REGULAR_ORDER_LATEST_STORAGE_KEY),
      );
    }

    return null;
  } catch {
    return null;
  }
}

export function writeStoredRegularOrderSelection(
  scope: RegularOrderSelectionScope,
  value: StoredRegularOrderSelection,
) {
  try {
    const serializedValue = JSON.stringify(value);
    const scopedKey = getScopedStorageKey(scope);

    if (scopedKey) {
      window.localStorage.setItem(scopedKey, serializedValue);
    }

    window.localStorage.setItem(REGULAR_ORDER_LATEST_STORAGE_KEY, serializedValue);
  } catch {
    // Ignore storage failures and fall back to server data.
  }
}
