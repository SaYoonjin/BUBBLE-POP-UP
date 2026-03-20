export interface RegularOrderRequest {
  menuId: number;
  quantity: number;
  price: number;
}

export interface RegularOrderResponse {
  orderId?: number;
  status?: string;
  message?: string;
  [key: string]: unknown;
}
