"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { BarList, ChartCard, NoData } from "./charts";
import {
  useMenuProducts,
  useMenuReport,
  useReportCategories,
  type MenuReport,
  type ProductSort,
  type ReportParams,
} from "./reports-api";
import { BlockError, Loading, ReportsScreen, type ReportFilters } from "./reports-page";

const t = messages.reports;
const m = t.menu;

/** Linhas por página na tabela de produtos (como no desenho: "Mostrando 10 de 64"). */
const PAGE_SIZE = 10;

const percent = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 1 });

/** A tela do Cardápio (desenho 14.3), com os blocos básicos (REL-13, 14, 16), e o filtro de categoria. */
export function MenuScreen() {
  return (
    <ReportsScreen
      title={m.title}
      filter={(filters) => <CategoryFilter value={filters.categoryId} onChange={filters.chooseCategory} />}
    >
      {(_, filters) => <MenuBody filters={filters} />}
    </ReportsScreen>
  );
}

/** Com duas categorias ou mais: "Todas as categorias", "Pratos", "Bebidas"… */
export function CategoryFilter({ value, onChange }: { value?: string; onChange: (id: string | null) => void }) {
  const categories = useReportCategories();
  const list = categories.data ?? [];
  if (list.length < 2) return null;
  return (
    <select
      aria-label={m.category}
      value={value ?? ""}
      onChange={(e) => onChange(e.target.value || null)}
      className="h-12 rounded-xl border border-input bg-card px-3.5 text-sm text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <option value="">{m.allCategories}</option>
      {list.map((category) => (
        <option key={category.id} value={category.id}>
          {category.name}
        </option>
      ))}
    </select>
  );
}

function MenuBody({ filters }: { filters: ReportFilters }) {
  const params = { ...filters.query, ...(filters.categoryId ? { categoryId: filters.categoryId } : {}) };
  const report = useMenuReport(params);
  if (report.isError) return <BlockError onRetry={() => report.refetch()} />;
  if (!report.data) return <Loading inline />;
  return (
    <>
      {/* Trocar o período ou a categoria volta a tabela para a primeira página. */}
      <ProductTable key={JSON.stringify(params)} params={params} report={report.data} />
      <div className="grid items-start gap-5 lg:grid-cols-2">
        <ChartCard title={m.byCategory}>
          {report.data.byCategory.length > 0 ? (
            <BarList
              rows={report.data.byCategory.map((c) => ({
                key: c.categoryId,
                label: c.name,
                value: c.revenueCents,
                detail: m.categoryDetail(c.quantity),
              }))}
            />
          ) : (
            <NoData text={m.noProducts} />
          )}
        </ChartCard>
        <SlowSellers report={report.data} />
      </div>
    </>
  );
}

function ProductTable({ params, report }: { params: ReportParams & { categoryId?: string }; report: MenuReport }) {
  const [sort, setSort] = useState<ProductSort>("REVENUE");
  const [page, setPage] = useState(0);
  const products = useMenuProducts({ ...params, sort, page, size: PAGE_SIZE });
  const data = products.data;
  const share = (cents: number) => (report.itemsRevenueCents === 0 ? 0 : cents / report.itemsRevenueCents);

  return (
    <ChartCard
      title={m.products}
      action={
        <div role="group" aria-label={m.sortBy} className="flex gap-1 rounded-[14px] border border-border bg-card p-1">
          {(["REVENUE", "QUANTITY"] as const).map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={sort === option}
              onClick={() => {
                setSort(option);
                setPage(0);
              }}
              className={cn(
                "h-9 rounded-[10px] px-3 text-sm font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                sort === option && "bg-foreground text-background",
              )}
            >
              {m.sort[option]}
            </button>
          ))}
        </div>
      }
    >
      {products.isError ? (
        <BlockError onRetry={() => products.refetch()} />
      ) : !data ? (
        <span aria-hidden className="h-64 rounded-[14px] bg-secondary motion-safe:animate-pulse" />
      ) : data.totalElements === 0 ? (
        <NoData text={m.noProducts} />
      ) : (
        <>
          <div className="relative -mx-5 overflow-x-auto px-5">
            <table className="w-full min-w-[560px] border-collapse text-sm">
              <thead>
                <tr className="text-left text-xs font-semibold tracking-[.4px] text-muted-foreground uppercase">
                  <th scope="col" className="border-b border-border py-3 pr-4 font-semibold">
                    {m.columns.product}
                  </th>
                  <th scope="col" className="border-b border-border px-4 py-3 text-right font-semibold">
                    {m.columns.sold}
                  </th>
                  <th scope="col" className="border-b border-border px-4 py-3 text-right font-semibold">
                    {m.columns.revenue}
                  </th>
                  <th scope="col" className="w-[30%] border-b border-border py-3 pl-4 font-semibold">
                    {m.columns.share}
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((product) => (
                  <tr key={product.productId} className="border-b border-border last:border-b-0">
                    <td className="py-3 pr-4">
                      <span className="flex flex-col">
                        <span>{product.name}</span>
                        <span className="text-xs text-muted-foreground">{product.category}</span>
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right font-mono tabular-nums">{product.quantity}</td>
                    <td className="px-4 py-3 text-right font-mono tabular-nums">{formatCents(product.revenueCents)}</td>
                    <td className="py-3 pl-4">
                      <span className="flex items-center gap-3">
                        <span aria-hidden className="flex h-3 flex-1">
                          <span
                            className="h-3 rounded-r bg-report-now"
                            style={{ width: `${share(product.revenueCents) * 100}%` }}
                          />
                        </span>
                        <span className="w-12 text-right text-xs text-muted-foreground tabular-nums">
                          {percent.format(share(product.revenueCents))}
                          <span className="sr-only"> {m.shareOf}</span>
                        </span>
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <p className="mr-auto text-xs leading-4 text-muted-foreground">
              {m.showing(
                page * PAGE_SIZE + 1,
                page * PAGE_SIZE + data.content.length,
                data.totalElements,
                report.activeProducts,
              )}
            </p>
            {data.totalPages > 1 && (
              <div className="flex gap-2">
                <Button variant="outline" className="h-10" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                  <ChevronLeft aria-hidden />
                  {m.previous}
                </Button>
                <Button
                  variant="outline"
                  className="h-10"
                  disabled={page + 1 >= data.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {m.next}
                  <ChevronRight aria-hidden />
                </Button>
              </div>
            )}
          </div>
        </>
      )}
    </ChartCard>
  );
}

function SlowSellers({ report }: { report: MenuReport }) {
  return (
    <ChartCard title={m.slow} hint={m.slowHint} note={report.slowSellers.length > 0 ? m.slowNote : undefined}>
      {report.slowSellers.length > 0 ? (
        <ul className="flex flex-col gap-2.5">
          {report.slowSellers.map((product) => (
            <li key={product.productId} className="flex items-baseline gap-3 text-sm">
              <span className="flex min-w-0 flex-1 flex-col">
                <span className="truncate">{product.name}</span>
                <span className="text-xs text-muted-foreground">{product.category}</span>
              </span>
              <span className="font-mono text-[13px] whitespace-nowrap tabular-nums">
                {m.slowSold(product.quantity)}
              </span>
            </li>
          ))}
        </ul>
      ) : (
        <NoData text={m.noSlow} />
      )}
    </ChartCard>
  );
}
