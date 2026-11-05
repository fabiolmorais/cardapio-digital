import { api, type Schemas } from "@/lib/api/client";
import { activeStaffRegistration, isIos, isStandalone, serviceWorkerSupported, STAFF_SW_SCOPE } from "./service-worker";

export type PushSettings = Schemas["PushSettings"];

/**
 * Como estão os avisos neste aparelho:
 * - unsupported: o navegador não recebe web push;
 * - needs-install: iPhone fora do app instalado (lá o push só funciona na tela inicial);
 * - denied: a pessoa bloqueou os avisos no navegador;
 * - off / on: dá para ligar, ou já está ligado.
 */
export type PushStatus = "unsupported" | "needs-install" | "denied" | "off" | "on";

export class PushPermissionDenied extends Error {}

export async function fetchPushSettings(): Promise<PushSettings> {
  const { data, response } = await api.GET("/api/v1/push/settings");
  if (!data) throw new Error(String(response.status));
  return data;
}

function pushSupported(): boolean {
  return serviceWorkerSupported() && "PushManager" in window && "Notification" in window;
}

async function currentSubscription(): Promise<PushSubscription | null> {
  const registration = await navigator.serviceWorker.getRegistration(STAFF_SW_SCOPE);
  return (await registration?.pushManager.getSubscription()) ?? null;
}

export async function pushStatus(): Promise<PushStatus> {
  if (!pushSupported()) return isIos() && !isStandalone() ? "needs-install" : "unsupported";
  if (Notification.permission === "denied") return "denied";
  if (Notification.permission !== "granted") return "off";
  return (await currentSubscription()) ? "on" : "off";
}

function base64UrlToBytes(value: string): Uint8Array<ArrayBuffer> {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  const raw = atob(base64);
  const bytes = new Uint8Array(new ArrayBuffer(raw.length));
  for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
  return bytes;
}

function sameKey(subscription: PushSubscription, key: Uint8Array): boolean {
  const current = subscription.options.applicationServerKey;
  if (!current) return false;
  const bytes = new Uint8Array(current);
  return bytes.length === key.length && bytes.every((b, i) => b === key[i]);
}

async function sendToServer(subscription: PushSubscription) {
  const json = subscription.toJSON();
  const { response } = await api.POST("/api/v1/push/subscriptions", {
    body: { endpoint: subscription.endpoint, keys: { p256dh: json.keys?.p256dh ?? "", auth: json.keys?.auth ?? "" } },
  });
  if (!response.ok) throw new Error(String(response.status));
}

/** Pede a permissão, inscreve o navegador com a chave VAPID do servidor e avisa o back. */
export async function enablePush(publicKey: string): Promise<void> {
  if ((await Notification.requestPermission()) !== "granted") throw new PushPermissionDenied();
  const registration = await activeStaffRegistration();
  const key = base64UrlToBytes(publicKey);
  let subscription = await registration.pushManager.getSubscription();
  // Inscrição feita com outra chave (o servidor trocou as chaves VAPID): refaz.
  if (subscription && !sameKey(subscription, key)) {
    await subscription.unsubscribe();
    subscription = null;
  }
  subscription ??= await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: key });
  await sendToServer(subscription);
}

/**
 * Confirma no back a inscrição que o navegador já tem (quem entrou agora neste aparelho passa a
 * receber os avisos). Sem inscrição ou sem permissão, não faz nada.
 */
export async function syncPush(): Promise<void> {
  if (!pushSupported() || Notification.permission !== "granted") return;
  const subscription = await currentSubscription();
  if (subscription) await sendToServer(subscription);
}

/** Para de receber os avisos neste aparelho: apaga no back e cancela no navegador. */
export async function disablePush(): Promise<void> {
  if (!pushSupported()) return;
  const subscription = await currentSubscription();
  if (!subscription) return;
  await api.DELETE("/api/v1/push/subscriptions", { params: { query: { endpoint: subscription.endpoint } } });
  await subscription.unsubscribe();
}
