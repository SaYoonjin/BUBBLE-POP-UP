import client from "./client";
import type { RegularOrderRequest, RegularOrderResponse } from "../types/order";

export async function postRegularOrder(payload: RegularOrderRequest) {
  const { data } = await client.post<RegularOrderResponse>("/api/orders/regular", payload);
  return data;
}
