import client from "./client";
import type { RegularOrderRequest, RegularOrderResponse } from "../types/order";

export interface CurrentOrderResponse {
  menuId: number;
  menuName: string;
  costPrice: number;
  recommendedPrice: number;
  maxSellingPrice: number;
  sellingPrice: number;
  stock: number;
}

export async function postRegularOrder(payload: RegularOrderRequest) {
  const { data } = await client.post<RegularOrderResponse>("/api/orders/regular", payload);
  return data;
}

export async function getCurrentOrder() {
  const { data } = await client.get<CurrentOrderResponse>("/api/orders");
  return data;
}
