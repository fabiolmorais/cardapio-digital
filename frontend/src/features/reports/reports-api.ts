import { useQuery } from "@tanstack/react-query";
import { api, type Schemas } from "@/lib/api/client";

export type ReportContext = Schemas["ReportContext"];

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
