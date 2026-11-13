"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";

/** Quem está logado e em qual restaurante (GET /api/v1/me). */
async function fetchMe() {
  const { data, response } = await api.GET("/api/v1/me");
  if (!data) throw new Error(String(response.status));
  return data;
}

/** Os dados de quem está logado, com o restaurante (nome, fuso, papel). Uma busca só por tela. */
export function useMe(enabled = true) {
  return useQuery({ queryKey: ["me"], queryFn: fetchMe, enabled, retry: false });
}
