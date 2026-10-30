"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, RefreshCw } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Button } from "@/components/ui/button";
import { canManageTeam } from "@/features/team/team-rules";
import { useSession } from "@/lib/auth/use-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import {
  createSector,
  createTable,
  deactivateSector,
  fetchSectorDisplays,
  fetchSectors,
  fetchTableDisplay,
  fetchTables,
  renameSector,
  rotateTableToken,
  saveFile,
  updateTable,
  type DiningTable,
  type DownloadedFile,
  type Sector,
} from "./floor-api";
import { SectorFormDialog, TableFormDialog } from "./floor-dialogs";

const t = messages.floor;
const day = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

type DialogState =
  | { kind: "new-sector" }
  | { kind: "rename-sector"; sector: Sector }
  | { kind: "deactivate-sector"; sector: Sector }
  | { kind: "new-table" }
  | { kind: "edit-table"; table: DiningTable }
  | { kind: "rotate"; table: DiningTable }
  | null;

/** Mesas e QR Codes (desenho 12.1): setores no alto, mesas na tabela e o display ao lado. */
export function FloorManager() {
  const { status, session } = useSession();
  const queryClient = useQueryClient();
  const allowed = status === "authenticated" && canManageTeam(session.establishment.role);

  const sectors = useQuery({ queryKey: ["sectors"], queryFn: fetchSectors, enabled: allowed });
  const tables = useQuery({ queryKey: ["tables"], queryFn: fetchTables, enabled: allowed });

  const [sectorFilter, setSectorFilter] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [dialog, setDialog] = useState<DialogState>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<string | null>(null);

  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["sectors"] }),
      queryClient.invalidateQueries({ queryKey: ["tables"] }),
    ]);
  }
  const close = (open: boolean) => !open && setDialog(null);

  if (status !== "authenticated" || (allowed && (sectors.isPending || tables.isPending))) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }
  if (!allowed) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }
  if (sectors.isError || tables.isError) {
    return (
      <p role="alert" className="px-4 py-6 text-destructive sm:px-7">
        {t.loadError}
      </p>
    );
  }

  if (!sectors.data || !tables.data) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }

  const sectorList = sectors.data;
  const tableList = tables.data;
  const activeFilter = sectorList.some((s) => s.id === sectorFilter) ? sectorFilter : null;
  const filterSector = sectorList.find((s) => s.id === activeFilter);
  const sectorName = new Map(sectorList.map((s) => [s.id, s.name]));
  const visible = tableList.filter((table) => !activeFilter || table.sectorId === activeFilter);
  const selected = tableList.find((table) => table.id === selectedId) ?? null;

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-wrap items-end gap-4">
        <div className="mr-auto flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
          <p className="text-sm text-muted-foreground">{t.subtitle}</p>
        </div>
        {sectorList.length > 0 && (
          <Button size="xl" onClick={() => setDialog({ kind: "new-table" })}>
            <Plus aria-hidden />
            {t.newTable}
          </Button>
        )}
      </header>

      {notice && (
        <p role="status" className="text-sm text-success">
          {notice}
        </p>
      )}
      {failure && (
        <p role="alert" className="text-sm text-destructive">
          {failure}
        </p>
      )}

      {sectorList.length === 0 ? (
        <section className="flex max-w-lg flex-col items-start gap-3 rounded-2xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold">{t.start.title}</h2>
          <p className="text-muted-foreground">{t.start.text}</p>
          <Button size="xl" onClick={() => setDialog({ kind: "new-sector" })}>
            {t.start.action}
          </Button>
        </section>
      ) : (
        <div className="flex flex-col gap-6 xl:flex-row xl:items-start">
          <section className="flex min-w-0 flex-1 flex-col gap-3.5">
            <div className="flex flex-wrap gap-2" role="group" aria-label={t.sectors}>
              <SectorPill on={!activeFilter} onClick={() => setSectorFilter(null)}>
                {t.sectorCount(t.all, tableList.filter((x) => x.active).length)}
              </SectorPill>
              {sectorList.map((sector) => (
                <SectorPill key={sector.id} on={sector.id === activeFilter} onClick={() => setSectorFilter(sector.id)}>
                  {t.sectorCount(sector.name, sector.activeTables)}
                </SectorPill>
              ))}
              <Button variant="ghost" className="h-10 px-3" onClick={() => setDialog({ kind: "new-sector" })}>
                <Plus aria-hidden />
                {t.newSector}
              </Button>
            </div>

            {filterSector && (
              <div className="flex flex-wrap gap-2">
                <Button
                  variant="outline"
                  className="h-10 px-3"
                  onClick={() => setDialog({ kind: "rename-sector", sector: filterSector })}
                >
                  {t.renameSector}
                </Button>
                <Button
                  variant="ghost"
                  className="h-10 px-3 text-destructive hover:text-destructive"
                  onClick={() => setDialog({ kind: "deactivate-sector", sector: filterSector })}
                >
                  {t.deactivateSector}
                </Button>
              </div>
            )}

            {/* relative: o texto sr-only (absolute) fica dentro da caixa com rolagem (ver a tela da equipe). */}
            <div className="relative overflow-x-auto rounded-2xl border border-border bg-card">
              <table className="w-full min-w-[560px] border-collapse text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs tracking-wide text-muted-foreground uppercase">
                    <th scope="col" className="px-4 py-3 font-semibold">
                      {t.columns.table}
                    </th>
                    <th scope="col" className="px-4 py-3 font-semibold">
                      {t.columns.sector}
                    </th>
                    <th scope="col" className="px-4 py-3 font-semibold">
                      {t.columns.qr}
                    </th>
                    <th scope="col" className="px-4 py-3 font-semibold">
                      {t.columns.active}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {visible.length === 0 && (
                    <tr>
                      <td colSpan={4} className="px-4 py-6 text-muted-foreground">
                        {t.emptyTables}
                      </td>
                    </tr>
                  )}
                  {visible.map((table) => (
                    <tr
                      key={table.id}
                      className={cn(
                        "border-b border-border last:border-0",
                        table.id === selectedId && "bg-secondary",
                        !table.active && "text-muted-foreground",
                      )}
                    >
                      <td className="px-2 py-1.5">
                        <button
                          type="button"
                          aria-pressed={table.id === selectedId}
                          onClick={() => setSelectedId(table.id)}
                          className="h-10 rounded-lg px-2 font-mono font-semibold hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
                        >
                          {t.tableName(table.label)}
                        </button>
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">{sectorName.get(table.sectorId) ?? "—"}</td>
                      <td className="px-4 py-3 text-muted-foreground">
                        {t.qrIssued(day.format(new Date(table.tokenIssuedAt)))}
                      </td>
                      <td className="px-4 py-2">
                        <ActiveSwitch
                          table={table}
                          onToggle={async () => {
                            setNotice(null);
                            setFailure(null);
                            try {
                              await updateTable(table.id, { active: !table.active });
                            } catch (error) {
                              setFailure((error as Error).message);
                            }
                            await refresh();
                          }}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <TablePanel
            table={selected}
            sectorName={selected ? sectorName.get(selected.sectorId) : undefined}
            establishmentName={session.establishment.name}
            onEdit={(table) => setDialog({ kind: "edit-table", table })}
            onRotate={(table) => setDialog({ kind: "rotate", table })}
          />
        </div>
      )}

      <SectorFormDialog
        key={dialog?.kind === "rename-sector" ? dialog.sector.id : "new-sector"}
        open={dialog?.kind === "new-sector" || dialog?.kind === "rename-sector"}
        onOpenChange={close}
        sector={dialog?.kind === "rename-sector" ? dialog.sector : undefined}
        onSave={async (name) => {
          if (dialog?.kind === "rename-sector") {
            await renameSector(dialog.sector.id, name);
          } else {
            const created = await createSector(name);
            setSectorFilter(created.id);
          }
          await refresh();
        }}
      />
      <TableFormDialog
        key={dialog?.kind === "edit-table" ? dialog.table.id : `new-table-${activeFilter}`}
        open={dialog?.kind === "new-table" || dialog?.kind === "edit-table"}
        onOpenChange={close}
        sectors={sectorList}
        table={dialog?.kind === "edit-table" ? dialog.table : undefined}
        defaultSectorId={activeFilter ?? undefined}
        onSave={async ({ label, sectorId }) => {
          if (dialog?.kind === "edit-table") {
            await updateTable(dialog.table.id, { label, sectorId });
          } else {
            const created = await createTable(label, sectorId);
            setSelectedId(created.id);
          }
          await refresh();
        }}
      />
      <ConfirmDialog
        open={dialog?.kind === "rotate"}
        onOpenChange={close}
        title={dialog?.kind === "rotate" ? t.confirm.rotateTitle(dialog.table.label) : ""}
        description={t.confirm.rotateText}
        confirmLabel={t.confirm.rotate}
        onConfirm={async () => {
          if (dialog?.kind !== "rotate") return;
          await rotateTableToken(dialog.table.id);
          await refresh();
          setNotice(t.panel.rotated);
        }}
      />
      <ConfirmDialog
        open={dialog?.kind === "deactivate-sector"}
        onOpenChange={close}
        title={dialog?.kind === "deactivate-sector" ? t.confirm.deactivateSectorTitle(dialog.sector.name) : ""}
        description={t.confirm.deactivateSectorText}
        confirmLabel={t.confirm.deactivate}
        onConfirm={async () => {
          if (dialog?.kind !== "deactivate-sector") return;
          await deactivateSector(dialog.sector.id);
          setSectorFilter(null);
          await refresh();
        }}
      />
    </div>
  );
}

function SectorPill({ on, onClick, children }: { on: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      type="button"
      aria-pressed={on}
      onClick={onClick}
      className={cn(
        "h-10 rounded-xl border border-border px-3.5 text-sm font-medium transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
        on && "border-transparent bg-foreground text-background hover:bg-foreground/90",
      )}
    >
      {children}
    </button>
  );
}

function ActiveSwitch({ table, onToggle }: { table: DiningTable; onToggle: () => Promise<void> }) {
  const [busy, setBusy] = useState(false);
  return (
    <button
      type="button"
      role="switch"
      aria-checked={table.active}
      aria-label={t.activeLabel(table.label)}
      disabled={busy}
      onClick={async () => {
        setBusy(true);
        await onToggle();
        setBusy(false);
      }}
      className={cn(
        "relative h-7 w-12 rounded-full border border-input bg-muted transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none disabled:opacity-60",
        table.active && "border-transparent bg-success",
      )}
    >
      <span
        aria-hidden
        className={cn(
          "absolute top-0.5 left-0.5 size-5.5 rounded-full bg-muted-foreground transition-transform",
          table.active && "translate-x-5 bg-white",
        )}
      />
    </button>
  );
}

function TablePanel({
  table,
  sectorName,
  establishmentName,
  onEdit,
  onRotate,
}: {
  table: DiningTable | null;
  sectorName: string | undefined;
  establishmentName: string;
  onEdit: (table: DiningTable) => void;
  onRotate: (table: DiningTable) => void;
}) {
  // O SVG do QR vem do back (com o token atual). A chave inclui o token: trocou o QR, a prévia muda.
  const qr = useQuery({
    queryKey: ["table-qr", table?.id, table?.publicToken],
    queryFn: async () => (await fetchTableDisplay(table!.id, "SVG")).blob.text(),
    enabled: table !== null,
    staleTime: Infinity,
  });
  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  async function download(run: () => Promise<DownloadedFile>) {
    setDownloading(true);
    setDownloadError(null);
    try {
      saveFile(await run());
    } catch (error) {
      setDownloadError((error as Error).message);
    } finally {
      setDownloading(false);
    }
  }

  if (!table) {
    return (
      <aside className="w-full rounded-2xl border border-border bg-card p-5 text-muted-foreground xl:max-w-sm">
        {t.panel.empty}
      </aside>
    );
  }
  return (
    <aside
      aria-label={t.tableName(table.label)}
      className="flex w-full flex-col gap-4 rounded-2xl border border-border bg-card p-5 xl:max-w-sm"
    >
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">{t.tableName(table.label)}</h2>
        <Badge variant={table.active ? "secondary" : "outline"}>
          {table.active ? t.panel.qrActive : t.panel.inactive}
        </Badge>
      </div>

      {/* Prévia do display impresso (o PDF sai igual a isto). */}
      <div className="mx-auto flex w-60 flex-col items-center gap-3 rounded-[22px] bg-white px-5 py-5.5 text-[#16130F] shadow-[0_20px_50px_rgba(0,0,0,.45)]">
        <span className="text-center text-xs font-bold tracking-[1.5px] text-[#B8361A] uppercase">
          {establishmentName}
        </span>
        {qr.data ? (
          // eslint-disable-next-line @next/next/no-img-element -- SVG gerado na hora, sem otimização de imagem
          <img
            src={`data:image/svg+xml;charset=utf-8,${encodeURIComponent(qr.data)}`}
            alt={t.panel.qrAlt(table.label)}
            className="size-40"
          />
        ) : (
          <span className="flex size-40 items-center justify-center text-center text-sm text-[#5C554E]">
            {qr.isError ? t.panel.qrError : t.panel.qrLoading}
          </span>
        )}
        <span className="text-center text-[13px] leading-[18px] text-[#5C554E]">{t.panel.callToAction}</span>
        <span className="text-[30px] leading-[30px] font-bold">{t.tableName(table.label)}</span>
      </div>

      <div className="flex flex-col gap-2">
        <span className="text-sm font-medium text-muted-foreground">{t.panel.downloads}</span>
        <div className="grid grid-cols-3 gap-2">
          {(["PNG", "SVG", "PDF"] as const).map((format) => (
            <Button
              key={format}
              variant="outline"
              className="h-11"
              aria-label={t.panel.downloadLabel(format, table.label)}
              disabled={downloading}
              onClick={() => void download(() => fetchTableDisplay(table.id, format))}
            >
              {format}
            </Button>
          ))}
        </div>
        <p className="text-xs text-muted-foreground">{t.panel.downloadHint}</p>
        {sectorName && (
          <Button
            variant="outline"
            className="h-11"
            disabled={downloading}
            onClick={() => void download(() => fetchSectorDisplays(table.sectorId))}
          >
            {t.panel.downloadSector(sectorName)}
          </Button>
        )}
        {downloading && (
          <p role="status" className="text-sm text-muted-foreground">
            {t.panel.downloading}
          </p>
        )}
        {downloadError && (
          <p role="alert" className="text-sm text-destructive">
            {downloadError}
          </p>
        )}
      </div>

      <Button variant="outline" size="xl" onClick={() => onEdit(table)}>
        {t.panel.edit}
      </Button>
      <p className="rounded-xl bg-amber-500/10 px-4 py-3 text-sm">{t.panel.rotateWarning}</p>
      <Button variant="destructive" size="xl" onClick={() => onRotate(table)}>
        <RefreshCw aria-hidden />
        {t.panel.rotate(table.label)}
      </Button>
    </aside>
  );
}
