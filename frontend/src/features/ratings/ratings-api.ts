import { api, type Schemas } from "@/lib/api/client";

export type Rating = Schemas["Rating"];
export type RatingSummary = Schemas["RatingSummary"];
export type MyRating = Schemas["MyRating"];
export type RatingSettings = Schemas["RatingSettings"];

/** A mensagem do back (Problem Details) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

export const RATINGS_KEY = ["ratings"] as const;
export const RATING_ALERTS_KEY = ["rating-alerts"] as const;

export async function fetchTodayRatings(): Promise<RatingSummary> {
  const { data, error, response } = await api.GET("/api/v1/ratings/today");
  if (!data) throw problem(error, response.status);
  return data;
}

export async function listTodayRatings(onlyLow: boolean, page: number, size: number) {
  const { data, error, response } = await api.GET("/api/v1/ratings", {
    params: { query: { onlyLow, page, size } },
  });
  if (!data) throw problem(error, response.status);
  return data;
}

export async function fetchRatingAlerts(): Promise<Rating[]> {
  const { data, error, response } = await api.GET("/api/v1/ratings/alerts");
  if (!data) throw problem(error, response.status);
  return data;
}

export async function resolveRating(id: string): Promise<void> {
  const { error, response } = await api.POST("/api/v1/ratings/{id}/resolve", { params: { path: { id } } });
  if (!response.ok) throw problem(error, response.status);
}

export async function fetchMyRating(): Promise<MyRating> {
  const { data, error, response } = await api.GET("/api/v1/ratings/mine");
  if (!data) throw problem(error, response.status);
  return data;
}

export async function fetchRatingSettings(): Promise<RatingSettings> {
  const { data, error, response } = await api.GET("/api/v1/ratings/settings");
  if (!data) throw problem(error, response.status);
  return data;
}

export async function updateRatingSettings(body: RatingSettings): Promise<RatingSettings> {
  const { data, error, response } = await api.PUT("/api/v1/ratings/settings", { body });
  if (!data) throw problem(error, response.status);
  return data;
}
