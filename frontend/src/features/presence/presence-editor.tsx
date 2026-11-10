"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Crosshair, Info, Plus, Wifi, X } from "lucide-react";
import dynamic from "next/dynamic";
import { useCallback, useId, useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { canManageTeam } from "@/features/team/team-rules";
import { centsToInput, parseCents } from "@/lib/format";
import { useSession } from "@/lib/auth/use-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { parseCoordinates } from "./parse-coordinates";
import {
  fetchOrderSettings,
  fetchPresenceRule,
  updateOrderSettings,
  updatePresenceRule,
  type OrderSettings,
  type PresenceLocation,
  type PresenceRule,
} from "./presence-api";

const t = messages.presence;
const MAX_NETWORKS = 10;
const RADIUS = { min: 25, max: 500, step: 25, default: 100 };
/** Sem local marcado, o mapa abre no centro de São Paulo, com o aviso para arrastar o pino. */
const DEFAULT_POINT = { latitude: -23.5505, longitude: -46.6333 };
/** As opções de validade da sessão da mesa, em minutos (o back aceita de 30 a 360). */
const IDLE_OPTIONS = [30, 60, 90, 120, 180, 240, 360];

/** O Leaflet só existe no navegador e só nesta tela: carregado à parte, depois do resto. */
const LocationMap = dynamic(() => import("./location-map"), {
  ssr: false,
  loading: () => <div className="h-72 w-full animate-pulse rounded-xl bg-muted" />,
});

/**
 * Presença e pedidos (desenho 12.6): as redes do salão, o nome do Wi-Fi, o local com o raio, a
 * validade da sessão e, ao lado, o pedido pelo celular com a confirmação do garçom e os limites.
 */
export function PresenceEditor() {
  const { status, session } = useSession();
  const allowed = status === "authenticated" && canManageTeam(session.establishment.role);
  const rule = useQuery({ queryKey: ["presence-rule"], queryFn: fetchPresenceRule, enabled: allowed });
  const orders = useQuery({ queryKey: ["order-settings"], queryFn: fetchOrderSettings, enabled: allowed });

  if (status !== "authenticated" || (allowed && (rule.isPending || orders.isPending))) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }
  if (!allowed) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }
  if (rule.isError || !rule.data || orders.isError || !orders.data) {
    return (
      <p role="alert" className="px-4 py-6 text-destructive sm:px-7">
        {t.loadError}
      </p>
    );
  }
  return <PresenceForm rule={rule.data} orders={orders.data} />;
}

/** O formulário nasce do que o back tem; a resposta do back, ao salvar, vira o cache. */
function PresenceForm({ rule, orders }: { rule: PresenceRule; orders: OrderSettings }) {
  const queryClient = useQueryClient();
  const id = useId();
  const [networks, setNetworks] = useState<string[]>(rule.networks);
  const [typed, setTyped] = useState("");
  const [wifiName, setWifiName] = useState(rule.wifiName ?? "");
  const [location, setLocation] = useState<PresenceLocation | null>(rule.location ?? null);
  const [idleMinutes, setIdleMinutes] = useState(rule.sessionIdleMinutes);
  const [notice, setNotice] = useState<string | null>(null);
  const [hint, setHint] = useState<string | null>(null);
  const [locating, setLocating] = useState(false);
  const [pasted, setPasted] = useState("");
  const [ordering, setOrdering] = useState(orders.orderingEnabled);
  const [approval, setApproval] = useState(orders.approvalRequired);
  const [maxAmount, setMaxAmount] = useState(orders.maxAmountCents == null ? "" : centsToInput(orders.maxAmountCents));
  const [maxItems, setMaxItems] = useState(orders.maxItems == null ? "" : String(orders.maxItems));

  const changed = () => setNotice(null);
  const full = networks.length >= MAX_NETWORKS;
  const current = rule.currentNetwork;

  function addNetwork(value: string) {
    const network = value.trim();
    if (!network) return;
    changed();
    if (networks.includes(network) || networks.includes(`${network}/32`)) {
      setHint(t.wifi.alreadyAdded);
      return;
    }
    setHint(null);
    setNetworks((list) => [...list, network]);
    setTyped("");
  }

  const movePin = useCallback((latitude: number, longitude: number) => {
    setNotice(null);
    setLocation((point) => ({ latitude, longitude, radiusMeters: point?.radiusMeters ?? RADIUS.default }));
  }, []);

  /** O ponto colado (coordenadas ou o endereço do Maps): mais preciso que o GPS do computador. */
  function applyPasted() {
    const parsed = parseCoordinates(pasted);
    if (parsed === "short-link") return setHint(t.location.pasteShort);
    if (parsed === null) return setHint(t.location.pasteInvalid);
    setHint(null);
    movePin(parsed.latitude, parsed.longitude);
    setPasted("");
  }

  function locateHere() {
    setLocating(true);
    setHint(null);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        movePin(coords.latitude, coords.longitude);
        setLocating(false);
      },
      (error) => {
        setHint(error.code === 1 ? t.location.denied : t.location.failed);
        setLocating(false);
      },
      { enableHighAccuracy: true, timeout: 15_000, maximumAge: 0 },
    );
  }

  const save = useMutation({
    mutationFn: async () => {
      // Os limites, conferidos aqui para a mensagem sair clara (o back confere de novo).
      const amount = parseCents(maxAmount);
      if (amount !== null && (Number.isNaN(amount) || amount < 100)) throw new Error(t.orders.invalidAmount);
      const items = maxItems.trim() === "" ? null : Number(maxItems);
      if (items !== null && (!Number.isInteger(items) || items < 1 || items > 100))
        throw new Error(t.orders.invalidItems);
      const savedRule = await updatePresenceRule({
        networks,
        wifiName: wifiName.trim() || undefined,
        location: location ?? undefined,
        sessionIdleMinutes: idleMinutes,
      });
      const savedOrders = await updateOrderSettings({
        orderingEnabled: ordering,
        approvalRequired: approval,
        maxAmountCents: amount ?? undefined,
        maxItems: items ?? undefined,
      });
      return { savedRule, savedOrders };
    },
    onSuccess: ({ savedRule, savedOrders }) => {
      queryClient.setQueryData(["presence-rule"], savedRule);
      queryClient.setQueryData(["order-settings"], savedOrders);
      setNetworks(savedRule.networks);
      setNotice(t.saved);
    },
  });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    save.mutate();
  }

  const point = location ?? { ...DEFAULT_POINT, radiusMeters: RADIUS.default };

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-wrap items-end gap-4">
        <div className="mr-auto flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
          <p className="text-sm text-muted-foreground">{t.subtitle}</p>
        </div>
        <Button type="submit" size="xl" disabled={save.isPending}>
          {save.isPending ? t.saving : t.save}
        </Button>
      </header>

      {notice && (
        <p role="status" className="text-sm text-success">
          {notice}
        </p>
      )}
      {save.isError && (
        <p role="alert" className="text-sm text-destructive">
          {save.error.message}
        </p>
      )}

      {/* Duas colunas, como no desenho 12.6: a presença à esquerda; pedidos, limites e sessão à direita. */}
      <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_380px]">
        <div className="flex min-w-0 flex-col gap-5">
          <section
            aria-labelledby={`${id}wifi`}
            className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 sm:p-6"
          >
            <div className="flex flex-col gap-1">
              <h2 id={`${id}wifi`} className="text-lg font-semibold">
                {t.wifi.title}
              </h2>
              <p className="text-sm text-muted-foreground">{t.wifi.text}</p>
            </div>

            {networks.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t.wifi.empty}</p>
            ) : (
              <ul className="flex flex-wrap gap-2">
                {networks.map((network) => (
                  <li
                    key={network}
                    className="flex h-9 items-center gap-1 rounded-lg bg-muted pr-1 pl-3 font-mono text-sm"
                  >
                    {network}
                    <button
                      type="button"
                      aria-label={t.wifi.remove(network)}
                      onClick={() => {
                        changed();
                        setNetworks((list) => list.filter((n) => n !== network));
                      }}
                      className="flex size-7 items-center justify-center rounded-md text-muted-foreground hover:bg-background hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
                    >
                      <X aria-hidden className="size-4" />
                    </button>
                  </li>
                ))}
              </ul>
            )}

            {current && (
              <div className="flex flex-wrap items-center gap-3">
                <Button
                  type="button"
                  variant="outline"
                  disabled={full || networks.includes(current)}
                  onClick={() => addNetwork(current)}
                >
                  <Wifi aria-hidden />
                  {t.wifi.useCurrent}
                </Button>
                <span className="text-sm text-muted-foreground">{t.wifi.current(current)}</span>
              </div>
            )}
            <p className="text-sm text-muted-foreground">{t.wifi.currentHint}</p>

            <div className="flex flex-col gap-2">
              <Label htmlFor={`${id}ip`} className="text-muted-foreground">
                {t.wifi.addLabel}
              </Label>
              <div className="flex gap-2">
                <Input
                  id={`${id}ip`}
                  value={typed}
                  placeholder={t.wifi.addPlaceholder}
                  autoComplete="off"
                  spellCheck={false}
                  disabled={full}
                  onChange={(event) => setTyped(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      event.preventDefault();
                      addNetwork(typed);
                    }
                  }}
                  className="h-11 rounded-xl font-mono"
                />
                <Button
                  type="button"
                  variant="outline"
                  className="h-11"
                  disabled={full}
                  onClick={() => addNetwork(typed)}
                >
                  <Plus aria-hidden />
                  {t.wifi.add}
                </Button>
              </div>
              {full && <p className="text-sm text-muted-foreground">{t.wifi.full}</p>}
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor={`${id}name`} className="text-muted-foreground">
                {t.wifi.name}
              </Label>
              <Input
                id={`${id}name`}
                value={wifiName}
                maxLength={60}
                autoComplete="off"
                aria-describedby={`${id}nameHint`}
                onChange={(event) => {
                  changed();
                  setWifiName(event.target.value);
                }}
                className="h-11 rounded-xl"
              />
              <p id={`${id}nameHint`} className="text-sm text-muted-foreground">
                {t.wifi.nameHint}
              </p>
            </div>
          </section>

          <section
            aria-labelledby={`${id}gps`}
            className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 sm:p-6"
          >
            <div className="flex flex-col gap-1">
              <h2 id={`${id}gps`} className="text-lg font-semibold">
                {t.location.title}
              </h2>
              <p className="text-sm text-muted-foreground">{t.location.text}</p>
            </div>

            <label className="flex items-center gap-3 font-medium">
              <input
                type="checkbox"
                checked={location !== null}
                onChange={(event) => {
                  changed();
                  setLocation(event.target.checked ? point : null);
                }}
                className="size-5 accent-primary"
              />
              {t.location.enable}
            </label>

            {location && (
              <>
                <div className="flex flex-wrap items-center gap-3">
                  <Button type="button" variant="outline" disabled={locating} onClick={locateHere}>
                    <Crosshair aria-hidden />
                    {locating ? t.location.locating : t.location.useHere}
                  </Button>
                  <span className="text-sm text-muted-foreground">
                    {t.location.point(location.latitude.toFixed(5), location.longitude.toFixed(5))}
                  </span>
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor={`${id}paste`}>{t.location.paste}</Label>
                  <div className="flex flex-wrap gap-2">
                    <Input
                      id={`${id}paste`}
                      value={pasted}
                      onChange={(event) => setPasted(event.target.value)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter") {
                          event.preventDefault();
                          applyPasted();
                        }
                      }}
                      placeholder={t.location.pastePlaceholder}
                      aria-describedby={`${id}paste-hint`}
                      autoComplete="off"
                      className="h-11 min-w-0 flex-1 rounded-xl px-3 text-base"
                    />
                    <Button
                      type="button"
                      variant="outline"
                      className="h-11"
                      onClick={applyPasted}
                      disabled={!pasted.trim()}
                    >
                      {t.location.usePasted}
                    </Button>
                  </div>
                  <p id={`${id}paste-hint`} className="text-sm text-muted-foreground">
                    {t.location.pasteHint}
                  </p>
                </div>
                <LocationMap
                  latitude={location.latitude}
                  longitude={location.longitude}
                  radiusMeters={location.radiusMeters}
                  onMove={movePin}
                />
                <p className="text-sm text-muted-foreground">{t.location.mapHint}</p>
                <div className="flex flex-col gap-2">
                  <Label htmlFor={`${id}radius`}>{t.location.radius(location.radiusMeters)}</Label>
                  <input
                    id={`${id}radius`}
                    type="range"
                    min={RADIUS.min}
                    max={RADIUS.max}
                    step={RADIUS.step}
                    value={location.radiusMeters}
                    onChange={(event) => {
                      changed();
                      setLocation({ ...location, radiusMeters: Number(event.target.value) });
                    }}
                    className="w-full accent-primary"
                  />
                </div>
              </>
            )}
          </section>

          {hint && (
            <p role="alert" className="text-sm text-destructive">
              {hint}
            </p>
          )}

          <p className="flex gap-3 rounded-2xl bg-muted p-4 text-sm">
            <Info aria-hidden className="mt-0.5 size-4 shrink-0" />
            {t.staff}
          </p>
        </div>
        <div className="flex min-w-0 flex-col gap-5">
          <section
            aria-labelledby={`${id}orders`}
            className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 sm:p-6"
          >
            <h2 id={`${id}orders`} className="text-lg font-semibold">
              {t.orders.title}
            </h2>
            <Toggle
              checked={ordering}
              onChange={(value) => {
                changed();
                setOrdering(value);
              }}
              label={t.orders.enabled}
              description={t.orders.enabledHint}
            />
            <Toggle
              checked={approval}
              disabled={!ordering}
              onChange={(value) => {
                changed();
                setApproval(value);
              }}
              label={t.orders.approval}
              description={t.orders.approvalHint}
            />
            <div className="flex flex-col gap-1 pt-2">
              <h3 className="font-semibold">{t.orders.limits}</h3>
              <p className="text-sm text-muted-foreground">{t.orders.limitsHint}</p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="flex flex-col gap-2">
                <Label htmlFor={`${id}amount`} className="text-muted-foreground">
                  {t.orders.maxAmount}
                </Label>
                <div className="flex h-11 items-center gap-2 rounded-xl border border-input px-3 dark:bg-input/30 focus-within:ring-3 focus-within:ring-ring/50">
                  <span className="text-sm font-semibold text-muted-foreground">R$</span>
                  <input
                    id={`${id}amount`}
                    inputMode="decimal"
                    value={maxAmount}
                    disabled={!ordering}
                    placeholder={t.orders.noLimit}
                    autoComplete="off"
                    onChange={(event) => {
                      changed();
                      setMaxAmount(event.target.value);
                    }}
                    className="h-full min-w-0 flex-1 bg-transparent text-base outline-none disabled:opacity-50"
                  />
                </div>
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor={`${id}items`} className="text-muted-foreground">
                  {t.orders.maxItems}
                </Label>
                <Input
                  id={`${id}items`}
                  inputMode="numeric"
                  value={maxItems}
                  disabled={!ordering}
                  placeholder={t.orders.noLimit}
                  autoComplete="off"
                  onChange={(event) => {
                    changed();
                    setMaxItems(event.target.value.replace(/\D/g, "").slice(0, 3));
                  }}
                  className="h-11 rounded-xl"
                />
              </div>
            </div>
            <p className="text-sm text-muted-foreground">{t.orders.emptyHint}</p>
          </section>
          <section
            aria-labelledby={`${id}session`}
            className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-5 sm:p-6"
          >
            <div className="flex flex-col gap-1">
              <h2 id={`${id}session`} className="text-lg font-semibold">
                {t.session.title}
              </h2>
              <p className="text-sm text-muted-foreground">{t.session.text}</p>
            </div>
            <Label htmlFor={`${id}idle`} className="text-muted-foreground">
              {t.session.label}
            </Label>
            <Select
              id={`${id}idle`}
              value={String(idleMinutes)}
              onValueChange={(minutes) => {
                changed();
                setIdleMinutes(Number(minutes));
              }}
              options={IDLE_OPTIONS.map((minutes) => ({ value: String(minutes), label: t.session.option(minutes) }))}
              className="h-11 max-w-xs"
            />
          </section>
        </div>
      </div>
    </form>
  );
}

/** Um interruptor de verdade (checkbox com role="switch"), com o título e a explicação no rótulo. */
function Toggle({
  checked,
  disabled,
  onChange,
  label,
  description,
}: {
  checked: boolean;
  disabled?: boolean;
  onChange: (value: boolean) => void;
  label: string;
  description: string;
}) {
  return (
    <label className={cn("flex items-start gap-4", disabled && "opacity-50")}>
      <span className="flex flex-1 flex-col gap-0.5">
        <span className="font-medium">{label}</span>
        <span className="text-sm text-muted-foreground">{description}</span>
      </span>
      <input
        type="checkbox"
        role="switch"
        checked={checked}
        disabled={disabled}
        onChange={(event) => onChange(event.target.checked)}
        className="mt-1 size-5 shrink-0 accent-primary"
      />
    </label>
  );
}
