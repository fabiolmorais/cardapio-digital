import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api, type Schemas } from "@/lib/api/client";

export type ReportContext = Schemas["ReportContext"];
export type OverviewReport = Schemas["OverviewReport"];
export type SalesReport = Schemas["SalesReport"];
export type Grouping = SalesReport["grouping"];
export type MenuReport = Schemas["MenuReport"];
export type ProductSort = "REVENUE" | "QUANTITY";

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

/** O Cardápio: por categoria, os que vendem pouco e os totais (a tabela vem paginada à parte). */
export function useMenuReport(params: ReportParams & { categoryId?: string }) {
  return useQuery({
    queryKey: ["reports", "menu", params],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/menu", { params: { query: params } });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}

/** Uma página da tabela de produtos do período. */
export function useMenuProducts(
  params: ReportParams & { categoryId?: string; sort: ProductSort; page: number; size: number },
) {
  return useQuery({
    queryKey: ["reports", "menu", "products", params],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/menu/products", { params: { query: params } });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}

/** As categorias ativas do cardápio, para o filtro do relatório de cardápio. */
export function useReportCategories() {
  return useQuery({
    queryKey: ["reports", "categories"],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/menu-categories", {
        params: { query: { page: 0, size: 100 } },
      });
      if (!data) throw new Error(String(response.status));
      return data.content;
    },
    staleTime: 5 * 60_000,
  });
}
