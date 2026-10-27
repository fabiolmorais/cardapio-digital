import { api, type Schemas } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

export type Device = Schemas["Device"];
export type DeviceType = Device["type"];
export type DevicePage = Schemas["PageResponseDevice"];
export type PairingStarted = Schemas["PairingStarted"];
export type ClaimPairingResponse = Schemas["ClaimPairingResponse"];
export type LinkDeviceRequest = Schemas["LinkDeviceRequest"];

const t = messages.devices.errors;

/** Pedido de vínculo que não existe mais (vencido, desvinculado ou já usado). */
export class PairingGoneError extends Error {}

function failure(response: Response | undefined, error: unknown): Error {
  const problem = error as { detail?: string; fields?: string[] } | undefined;
  if (response?.status === 400 && problem?.fields) return new Error(t.fields);
  if (response && [400, 403, 404, 422].includes(response.status) && problem?.detail) {
    return new Error(problem.detail);
  }
  return new Error(t.generic);
}

// Lado do aparelho novo (sem login).

export async function startPairing(): Promise<PairingStarted> {
  const { data, error, response } = await api.POST("/api/v1/device-pairings");
  if (data) return data;
  throw failure(response, error);
}

/** O token chega no cookie HttpOnly device_token; o JavaScript nunca o vê. */
export async function claimPairing(id: string, secret: string): Promise<ClaimPairingResponse> {
  const { data, error, response } = await api.POST("/api/v1/device-pairings/{id}/claim", {
    params: { path: { id } },
    body: { secret },
  });
  if (data) return data;
  if (response.status === 404) throw new PairingGoneError();
  throw failure(response, error);
}

// Lado do painel (dono e gerente).

export async function fetchDevices(): Promise<DevicePage> {
  const { data, error, response } = await api.GET("/api/v1/devices", { params: { query: { page: 0, size: 100 } } });
  if (data) return data;
  throw failure(response, error);
}

export async function linkDevice(body: LinkDeviceRequest): Promise<Device> {
  const { data, error, response } = await api.POST("/api/v1/devices", { body });
  if (data) return data;
  throw failure(response, error);
}

export async function unlinkDevice(id: string): Promise<void> {
  const { error, response } = await api.DELETE("/api/v1/devices/{id}", { params: { path: { id } } });
  if (!response.ok) throw failure(response, error);
}

// Aparelho vinculado (cookie device_token): a tela do PIN.

export type CurrentDevice = Schemas["CurrentDevice"];
export type PinMember = Schemas["PinMember"];
export type LoginResponse = Schemas["LoginResponse"];

/** O aparelho não está vinculado (sem cookie, cookie desconhecido ou desvinculado). */
export class DeviceNotLinkedError extends Error {}

/** Erro do login por PIN, com o status para a tela decidir o que fazer. */
export class PinLoginError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

export async function fetchCurrentDevice(): Promise<CurrentDevice> {
  const { data, error, response } = await api.GET("/api/v1/auth/device");
  if (data) return data;
  if (response.status === 401) throw new DeviceNotLinkedError();
  throw failure(response, error);
}

export async function fetchPinMembers(): Promise<PinMember[]> {
  const { data, error, response } = await api.GET("/api/v1/auth/device/team-members", {
    params: { query: { page: 0, size: 100 } },
  });
  if (data) return data.content;
  if (response.status === 401) throw new DeviceNotLinkedError();
  throw failure(response, error);
}

export async function pinLogin(memberId: string, pin: string): Promise<LoginResponse> {
  const { data, error, response } = await api.POST("/api/v1/auth/pin", { body: { memberId, pin } });
  if (data) return data;
  const problem = error as { detail?: string } | undefined;
  throw new PinLoginError(problem?.detail ?? messages.pin.errors.generic, response.status);
}
