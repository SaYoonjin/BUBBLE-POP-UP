import client from "./client";

export type ShopItemCategory = "INGREDIENT" | "RENT";

export interface ShopItemResponse {
  itemId: number;
  itemName: string;
  category: ShopItemCategory;
  point: number;
  discountRate: number;
}

export interface ShopItemListResponse {
  items: ShopItemResponse[];
}

export interface PurchasedItemResponse {
  itemId: number;
  discountRate: number;
}

export interface PurchasedItemListResponse {
  items: PurchasedItemResponse[];
}

export async function getShopItems() {
  const { data } = await client.get<ShopItemListResponse>("/shop/items");
  return data;
}

export async function getPurchasedItems() {
  const { data } = await client.get<PurchasedItemListResponse>("/shop/purchased");
  return data;
}
