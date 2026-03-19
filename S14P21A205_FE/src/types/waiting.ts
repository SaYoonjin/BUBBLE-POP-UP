export type WaitingMode = "prep_locked" | "next_business_day";

export interface WaitingRouteState {
  mode: WaitingMode;
  brandName: string;
  districtName: string;
  nextPath: string;
  endTimestampMs?: number;
  targetDay?: number;
}
