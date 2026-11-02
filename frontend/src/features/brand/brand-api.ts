import { api, type Schemas } from "@/lib/api/client";
import { sendToStorage } from "@/lib/api/direct-upload";
import { messages } from "@/messages/pt-BR";

export type Brand = Schemas["Brand"];
export type BrandRequest = Schemas["BrandRequest"];
export type PaletteColor = Schemas["PaletteColor"];

const t = messages.brand.errors;

/** Problem Details do back vira mensagem pronta para a tela. */
function failure(response: Response | undefined, error: unknown): Error {
  const problem = error as { detail?: string; fields?: string[] } | undefined;
  if (response?.status === 400 && problem?.fields) return new Error(t.fields);
  if (response && [400, 403, 404, 409, 422].includes(response.status) && problem?.detail) {
    return new Error(problem.detail);
  }
  return new Error(t.generic);
}

export async function fetchBrand(): Promise<Brand> {
  const { data, error, response } = await api.GET("/api/v1/establishment/brand");
  if (data) return data;
  throw failure(response, error);
}

export async function updateBrand(body: BrandRequest): Promise<Brand> {
  const { data, error, response } = await api.PUT("/api/v1/establishment/brand", { body });
  if (data) return data;
  throw failure(response, error);
}

/** Os três passos da logo, como as fotos: URL assinada, envio direto ao R2 e confirmação. */
export async function uploadLogo(file: File, onProgress: (fraction: number) => void): Promise<Brand> {
  const started = await api.POST("/api/v1/establishment/logo/uploads", {
    body: { contentType: file.type, sizeBytes: file.size },
  });
  if (!started.data) throw failure(started.response, started.error);
  await sendToStorage(started.data, file, onProgress);
  const { data, error, response } = await api.POST("/api/v1/establishment/logo", {
    body: { uploadId: started.data.uploadId },
  });
  if (data) return data;
  throw failure(response, error);
}

export async function removeLogo(): Promise<Brand> {
  const { data, error, response } = await api.DELETE("/api/v1/establishment/logo");
  if (data) return data;
  throw failure(response, error);
}
