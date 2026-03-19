import client from "./client";

export interface UserPointsResponse {
  currentPoints: number;
}

export async function getUserPoints() {
  const { data } = await client.get<UserPointsResponse>("/api/users/points");
  return data;
}
