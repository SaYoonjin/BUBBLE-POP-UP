import client from "./client";

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

export async function getStore() {
  const { data } = await client.get<StoreResponse>("/api/stores");
  return data;
}

export async function getStoreMenus() {
  const { data } = await client.get<StoreMenuListResponse>("/api/stores/menus");
  return data;
}
