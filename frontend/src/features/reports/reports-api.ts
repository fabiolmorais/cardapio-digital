import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type Schemas } from "@/lib/api/client";

export type ReportContext = Schemas["ReportContext"];
export type OverviewReport = Schemas["OverviewReport"];
export type SalesReport = Schemas["SalesReport"];
export type Grouping = SalesReport["grouping"];
export type MenuReport = Schemas["MenuReport"];
export type KitchenReport = Schemas["KitchenReport"];
export type TeamReport = Schemas["TeamReport"];
export type TableReport = Schemas["TableReport"];
export type PhoneRodizioReport = Schemas["PhoneRodizioReport"];
export type SummarySettings = Schemas["DailySummarySettings"];
export type SummarySettingsRequest = Schemas["DailySummarySettingsRequest"];
export type DailySummary = Schemas["DailySummary"];
export type SummaryTestResult = Schemas["DailySummaryTestResult"];
export type ProductSort = "REVENUE" | "QUANTITY" | "GROWTH";

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

/** Cozinha e tempos: o preparo médio e os itens que mais demoram. */
export function useKitchenReport(params: ReportParams & { categoryId?: string }) {
  return useQuery({
    queryKey: ["reports", "kitchen", params],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/kitchen", { params: { query: params } });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}

/** Atendimento e equipe (Business). */
export function useTeamReport(params: ReportParams) {
  return useQuery({
    queryKey: ["reports", "team", params],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/team", { params: { query: params } });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}

export function useTableReport(params: ReportParams) {
  return useQuery({
    queryKey: ["reports", "tables", params],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/tables", { params: { query: params } });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}

export function usePhoneRodizioReport(params: ReportParams) {
  return useQuery({
    queryKey: ["reports", "phone-and-rodizio", params],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/phone-and-rodizio", { params: { query: params } });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}

/** As categorias ativas do cardápio, para o filtro do Cardápio e da Cozinha. */
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

/** A configuração do resumo diário e quem pode receber. */
export function useSummarySettings() {
  return useQuery({
    queryKey: ["reports", "daily-summary", "settings"],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/daily-summary/settings");
      if (!data) throw new Error(String(response.status));
      return data;
    },
    retry: false,
  });
}

export function useSaveSummarySettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: SummarySettingsRequest) => {
      const { data, response } = await api.PUT("/api/v1/reports/daily-summary/settings", { body });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    onSuccess: (saved) => {
      queryClient.setQueryData(["reports", "daily-summary", "settings"], saved);
      // O horário muda o "dia" da prévia.
      void queryClient.invalidateQueries({ queryKey: ["reports", "daily-summary", "preview"] });
    },
  });
}

export type ReportAlerts = Schemas["ReportAlerts"];
export type RevenueGoals = Schemas["RevenueGoals"];
export type RevenueGoal = Schemas["RevenueGoal"];

/** A meta deste mês e a do próximo (Business): só busca quando o plano tem. */
export function useRevenueGoals(enabled: boolean) {
  return useQuery({
    queryKey: ["reports", "goals"],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/goals");
      if (!data) throw new Error(String(response.status));
      return data;
    },
    enabled,
    retry: false,
  });
}

export function useSaveRevenueGoals() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (goals: RevenueGoal[]) => {
      const { data, response } = await api.PUT("/api/v1/reports/goals", { body: { goals } });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    onSuccess: (saved) => {
      queryClient.setQueryData(["reports", "goals"], saved);
      // A Visão geral mostra a meta: ela muda junto.
      void queryClient.invalidateQueries({ queryKey: ["reports", "overview"] });
    },
  });
}

/** Os alertas em tempo real (Business): só busca quando o plano tem. */
export function useReportAlerts(enabled: boolean) {
  return useQuery({
    queryKey: ["reports", "alerts"],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/alerts");
      if (!data) throw new Error(String(response.status));
      return data;
    },
    enabled,
    retry: false,
  });
}

export function useSaveReportAlerts() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: ReportAlerts) => {
      const { data, response } = await api.PUT("/api/v1/reports/alerts", { body });
      if (!data) throw new Error(String(response.status));
      return data;
    },
    onSuccess: (saved) => queryClient.setQueryData(["reports", "alerts"], saved),
  });
}

/** O resumo do último dia que já terminou, como ele sairia agora. */
export function useSummaryPreview() {
  return useQuery({
    queryKey: ["reports", "daily-summary", "preview"],
    queryFn: async () => {
      const { data, response } = await api.GET("/api/v1/reports/daily-summary/preview");
      if (!data) throw new Error(String(response.status));
      return data;
    },
    retry: false,
  });
}

/** "Enviar um teste para mim": o resumo do último dia, só para quem pediu. */
export function useSendSummaryTest() {
  return useMutation({
    mutationFn: async () => {
      const { data, response } = await api.POST("/api/v1/reports/daily-summary/test");
      if (!data) throw new Error(String(response.status));
      return data;
    },
  });
}

/** As telas que exportam em CSV (R12), pelo nome do endereço da API. */
export type ExportScreen = "overview" | "sales" | "menu" | "kitchen" | "team" | "tables" | "phone-and-rodizio";
export type ExportParams = ReportParams & { categoryId?: string; groupBy?: Grouping };

const EXPORT_NAMES: Record<ExportScreen, string> = {
  overview: "visao-geral",
  sales: "vendas",
  menu: "cardapio",
  kitchen: "cozinha",
  team: "equipe",
  tables: "mesas",
  "phone-and-rodizio": "celular-e-rodizio",
};

/** Baixa a planilha da tela ("ohchefe-vendas-2026-11-01-a-2026-11-30.csv"). */
export async function exportReport(screen: ExportScreen, params: ExportParams) {
  const { from, to, sectorId, categoryId, groupBy } = params;
  const base = { from, to, sectorId };
  const options = { parseAs: "blob" as const };
  const result = await (() => {
    switch (screen) {
      case "overview":
        return api.GET("/api/v1/reports/overview/export", { params: { query: base }, ...options });
      case "sales":
        return api.GET("/api/v1/reports/sales/export", { params: { query: { ...base, groupBy } }, ...options });
      case "menu":
        return api.GET("/api/v1/reports/menu/export", { params: { query: { ...base, categoryId } }, ...options });
      case "kitchen":
        return api.GET("/api/v1/reports/kitchen/export", { params: { query: { ...base, categoryId } }, ...options });
      case "team":
        return api.GET("/api/v1/reports/team/export", { params: { query: base }, ...options });
      case "tables":
        return api.GET("/api/v1/reports/tables/export", { params: { query: base }, ...options });
      case "phone-and-rodizio":
        return api.GET("/api/v1/reports/phone-and-rodizio/export", { params: { query: base }, ...options });
    }
  })();
  if (!result.data) throw new Error(String(result.response.status));
  // O nome é o mesmo que o servidor põe no Content-Disposition: o cabeçalho não chega ao front, que
  // fica em outro endereço (o CORS não o expõe).
  const name = `ohchefe-${EXPORT_NAMES[screen]}-${from}-a-${to}.csv`;
  const url = URL.createObjectURL(result.data as Blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}
