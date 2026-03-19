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

export async function getStoreMenus() {
  const { data } = await client.get<StoreMenuListResponse>("/api/stores/menus");
  return data;
}
