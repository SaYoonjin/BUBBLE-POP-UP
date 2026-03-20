import client from "./client";

// --- 타입 정의 ---

export interface StoreResponse {
  location: string;
  popupName: string;
  menu: string;
  day: number;
}

export interface MenuItem {
  menuId: number;
  menuName: string;
  ingredientPrice: number;
  discount: number;
}

export interface MenuListResponse {
  menus: MenuItem[];
}

export interface StoreMenuResponse {
  menuId: number;
  menuName: string;
  ingredientPrice: number;
  discount: number;
}

export interface StoreMenuListResponse {
  menus: StoreMenuResponse[];
}

export interface StoreResponse {
  location: string;
  popupName: string;
  menu: string;
  day: number;
}

export interface LocationItem {
  locationId: number;
  locationName: string;
  rent: number;
  interiorCost: number;
  discount: number;
}

export interface LocationListResponse {
  locations: LocationItem[];
}

export interface UpdateStoreLocationResponse {
  locationId: number;
  balance: number;
}

// --- API 함수 ---

/** 내 매장 정보 조회 */
export async function getStore() {
  const { data } = await client.get<StoreResponse>("/api/stores");
  return data;
}

/** 메뉴 목록 조회 */
export async function getMenuList() {
  const { data } = await client.get<MenuListResponse>("/api/stores/menus");
  return data;
}

/** 지역 목록 조회 */
export async function getLocationList() {
  const { data } = await client.get<LocationListResponse>("/api/stores/locations");
  return data;
}

/** 매장 지역 변경 */
export async function updateStoreLocation(locationId: number) {
  const { data } = await client.patch<UpdateStoreLocationResponse>("/api/stores/location", { locationId });
  return data;
}

export async function getStoreMenus() {
  const { data } = await client.get<StoreMenuListResponse>("/api/stores/menus");
  return data;
}
