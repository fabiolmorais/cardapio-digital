"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Crosshair, Info, Plus, Wifi, X } from "lucide-react";
import dynamic from "next/dynamic";
import { useCallback, useId, useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { canManageTeam } from "@/features/team/team-rules";
import { useSession } from "@/lib/auth/use-session";
import { messages } from "@/messages/pt-BR";
import { fetchPresenceRule, updatePresenceRule, type PresenceLocation, type PresenceRule } from "./presence-api";

const t = messages.presence;
const MAX_NETWORKS = 10;
const RADIUS = { min: 25, max: 500, step: 25, default: 100 };
/** Sem local marcado, o mapa abre no centro de São Paulo, com o aviso para arrastar o pino. */
const DEFAULT_POINT = { latitude: -23.5505, longitude: -46.6333 };

/** O Leaflet só existe no navegador e só nesta tela: carregado à parte, depois do resto. */
const LocationMap = dynamic(() => import("./location-map"), {
  ssr: false,
  loading: () => <div className="h-72 w-full animate-pulse rounded-xl bg-muted" />,
});

/** Presença e pedidos (desenho 12.6): as redes do salão, o nome do Wi-Fi e o local com o raio. */
export function PresenceEditor() {
  const { status, session } = useSession();
  const allowed = status === "authenticated" && canManageTeam(session.establishment.role);
  const rule = useQuery({ queryKey: ["presence-rule"], queryFn: fetchPresenceRule, enabled: allowed });

  if (status !== "authenticated" || (allowed && rule.isPending)) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }
  if (!allowed) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }
  if (rule.isError || !rule.data) {
    return (
      <p role="alert" className="px-4 py-6 text-destructive sm:px-7">
        {t.loadError}
      </p>
    );
  }
  return <PresenceForm rule={rule.data} />;
}

/** O formulário nasce do que o back tem; a resposta do back, ao salvar, vira o cache. */
function PresenceForm({ rule }: { rule: PresenceRule }) {
  const queryClient = useQueryClient();
  const id = useId();
  const [networks, setNetworks] = useState<string[]>(rule.networks);
  const [typed, setTyped] = useState("");
  const [wifiName, setWifiName] = useState(rule.wifiName ?? "");
  const [location, setLocation] = useState<PresenceLocation | null>(rule.location ?? null);
  const [notice, setNotice] = useState<string | null>(null);
  const [hint, setHint] = useState<string | null>(null);
  const [locating, setLocating] = useState(false);

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
    mutationFn: () => updatePresenceRule({ networks, wifiName: wifiName.trim() || undefined, location: location ?? undefined }),
    onSuccess: (saved) => {
      queryClient.setQueryData(["presence-rule"], saved);
      setNetworks(saved.networks);
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

      <div className="flex max-w-3xl flex-col gap-5">
        <section aria-labelledby={`${id}wifi`} className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 sm:p-6">
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
                <li key={network} className="flex h-9 items-center gap-1 rounded-lg bg-muted pr-1 pl-3 font-mono text-sm">
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
              <Button type="button" variant="outline" className="h-11" disabled={full} onClick={() => addNetwork(typed)}>
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

        <section aria-labelledby={`${id}gps`} className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 sm:p-6">
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
    </form>
  );
}
