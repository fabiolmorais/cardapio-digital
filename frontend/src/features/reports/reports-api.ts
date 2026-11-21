import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api, type Schemas } from "@/lib/api/client";

export type ReportContext = Schemas["ReportContext"];
export type OverviewReport = Schemas["OverviewReport"];
export type SalesReport = Schemas["SalesReport"];
export type Grouping = SalesReport["grouping"];

/** O período (dias do restaurante) e, se escolhido, o setor. */
export type ReportParams = { from: string; to: string; sectorId?: string };

export async function fetchReportContext(): Promise<ReportContext> {
  const { data, response } = await api.GET("/api/v1/reports/context");
  if (!data) throw new Error(String(response.status));
  return data;
}

/** O plano, o fuso, o "hoje" do restaurante e desde quando há dados. Só dono e gerente (403 para os outros). */
export function useReportContext(enabled: boolean) {
  return useQuery({ queryKey: ["reports", "context"], queryFn: fetchReportContext, enabled, retry: false });
}

export function canSeeReports(role: string) {
  return role === "OWNER" || role === "MANAGER";
}

/** A Visão geral. Trocar o período mantém os números antigos na tela até os novos chegarem. */
export function useOverviewReport(params: ReportParams) {
  return useQuery({
    queryKey: ["reports", "overview", params],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/overview", { params: { query: params } });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}

export function useSalesReport(params: ReportParams & { groupBy: Grouping }) {
  return useQuery({
    queryKey: ["reports", "sales", params],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/sales", { params: { query: params } });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}

/** Os setores ativos, para o filtro (com um setor só, o filtro nem aparece). */
export function useReportSectors() {
  return useQuery({
    queryKey: ["reports", "sectors"],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/sectors", { params: { query: { page: 0, size: 100 } } });
      if (!data) throw new Error(String(response.status));
      return data.content;
    },
    staleTime: 5 * 60_000,
  });
}
