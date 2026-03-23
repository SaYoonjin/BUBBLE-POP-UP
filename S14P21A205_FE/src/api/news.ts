import client from "./client";

export interface NewsArticleResponse {
  newsId: number;
  newsTitle: string;
  newsContent: string;
}

export interface TodayNewsResponse {
  news: NewsArticleResponse[];
}

export interface AreaRankingItemResponse {
  rank: number;
  areaName: string;
  changeRate: number;
}

export interface NewsRankingResponse {
  areaRevenueRanking: AreaRankingItemResponse[];
  areaTrafficRanking: AreaRankingItemResponse[];
}

export async function getTodayNews(day: number) {
  const { data } = await client.get<TodayNewsResponse>(`/api/news/${day}`);
  return data;
}

export async function getNewsRanking(day: number) {
  const { data } = await client.get<NewsRankingResponse>(`/api/news/${day}/ranking`);
  return data;
}
