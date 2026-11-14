import { api, type Schemas } from "@/lib/api/client";
import { sendToStorage } from "@/lib/api/direct-upload";
import { messages } from "@/messages/pt-BR";
import { tusUpload } from "./tus-upload";

export type MenuCategory = Schemas["MenuCategory"];
export type Product = Schemas["Product"];
export type ProductRequest = Schemas["ProductRequest"];

const t = messages.menu.errors;

/** Problem Details do back vira mensagem pronta para a tela (409, 422, 404 e a regra de preço trazem uma frase). */
function failure(response: Response | undefined, error: unknown): Error {
  const problem = error as { detail?: string; fields?: string[] } | undefined;
  if (response?.status === 400 && problem?.fields) return new Error(t.fields);
  if (response && [400, 403, 404, 409, 422].includes(response.status) && problem?.detail) {
    return new Error(problem.detail);
  }
  return new Error(t.generic);
}

export async function fetchCategories(): Promise<MenuCategory[]> {
  const { data, error, response } = await api.GET("/api/v1/menu-categories", {
    params: { query: { page: 0, size: 100 } },
  });
  if (data) return data.content;
  throw failure(response, error);
}

export async function createCategory(name: string): Promise<MenuCategory> {
  const { data, error, response } = await api.POST("/api/v1/menu-categories", { body: { name } });
  if (data) return data;
  throw failure(response, error);
}

export async function renameCategory(id: string, name: string): Promise<MenuCategory> {
  const { data, error, response } = await api.PATCH("/api/v1/menu-categories/{id}", {
    params: { path: { id } },
    body: { name },
  });
  if (data) return data;
  throw failure(response, error);
}

export async function deactivateCategory(id: string): Promise<void> {
  const { error, response } = await api.DELETE("/api/v1/menu-categories/{id}", { params: { path: { id } } });
  if (!response.ok) throw failure(response, error);
}

export async function reorderCategories(ids: string[]): Promise<void> {
  const { error, response } = await api.PUT("/api/v1/menu-categories/order", { body: { ids } });
  if (!response.ok) throw failure(response, error);
}

export async function reorderProducts(categoryId: string, ids: string[]): Promise<void> {
  const { error, response } = await api.PUT("/api/v1/menu-categories/{id}/product-order", {
    params: { path: { id: categoryId } },
    body: { ids },
  });
  if (!response.ok) throw failure(response, error);
}

export type ProductPage = { products: Product[]; total: number };

/** Os produtos de uma categoria, ou a busca pelo nome em todas (com texto, a categoria é ignorada). */
export async function fetchProducts(filter: { categoryId?: string; q?: string }): Promise<ProductPage> {
  const query = filter.q ? { q: filter.q } : { categoryId: filter.categoryId };
  const { data, error, response } = await api.GET("/api/v1/products", {
    params: { query: { ...query, page: 0, size: 100 } },
  });
  if (data) return { products: data.content, total: data.totalElements };
  throw failure(response, error);
}

export async function fetchProduct(id: string): Promise<Product> {
  const { data, error, response } = await api.GET("/api/v1/products/{id}", { params: { path: { id } } });
  if (data) return data;
  throw failure(response, error);
}

export async function createProduct(body: ProductRequest): Promise<Product> {
  const { data, error, response } = await api.POST("/api/v1/products", { body });
  if (data) return data;
  throw failure(response, error);
}

export async function updateProduct(id: string, body: ProductRequest): Promise<Product> {
  const { data, error, response } = await api.PUT("/api/v1/products/{id}", { params: { path: { id } }, body });
  if (data) return data;
  throw failure(response, error);
}

export async function changeAvailability(id: string, available: boolean): Promise<Product> {
  const { data, error, response } = await api.PATCH("/api/v1/products/{id}/availability", {
    params: { path: { id } },
    body: { available },
  });
  if (data) return data;
  throw failure(response, error);
}

export async function deactivateProduct(id: string): Promise<void> {
  const { error, response } = await api.DELETE("/api/v1/products/{id}", { params: { path: { id } } });
  if (!response.ok) throw failure(response, error);
}

// Fotos e vídeos (Guias 10 e 22) do prato ou da opção (o sabor da pizza; complemento da ADR 0010).
// O arquivo vai direto do navegador para o armazenamento (R2) ou para o serviço de vídeo (TUS).

export type ProductMedia = Schemas["ProductMedia"];
type MediaUpload = Schemas["MediaUpload"];
type VideoUpload = Schemas["VideoUpload"];

/** De quem é a galeria: de um prato ou de uma opção. Os passos e os limites são os mesmos. */
export type MediaOwner = { kind: "product" | "option"; id: string };

const product = (owner: MediaOwner) => ({ params: { path: { productId: owner.id } } });
const option = (owner: MediaOwner) => ({ params: { path: { optionId: owner.id } } });

export async function fetchMedia(owner: MediaOwner): Promise<ProductMedia[]> {
  const { data, error, response } =
    owner.kind === "product"
      ? await api.GET("/api/v1/products/{productId}/media", product(owner))
      : await api.GET("/api/v1/option-items/{optionId}/media", option(owner));
  if (data) return data.content;
  throw failure(response, error);
}

/** Passo 1: o back confere o dono, o tipo e o tamanho e devolve a URL assinada. */
async function startMediaUpload(owner: MediaOwner, file: File): Promise<MediaUpload> {
  const body = { contentType: file.type, sizeBytes: file.size };
  const { data, error, response } =
    owner.kind === "product"
      ? await api.POST("/api/v1/products/{productId}/media/uploads", { ...product(owner), body })
      : await api.POST("/api/v1/option-items/{optionId}/media/uploads", { ...option(owner), body });
  if (data) return data;
  throw failure(response, error);
}

/** Passo 3: o back confere a foto, gera os tamanhos e a põe no fim da galeria. */
async function addMedia(owner: MediaOwner, uploadId: string): Promise<ProductMedia> {
  const body = { uploadId };
  const { data, error, response } =
    owner.kind === "product"
      ? await api.POST("/api/v1/products/{productId}/media", { ...product(owner), body })
      : await api.POST("/api/v1/option-items/{optionId}/media", { ...option(owner), body });
  if (data) return data;
  throw failure(response, error);
}

/** Os três passos de uma foto. */
export async function uploadPhoto(
  owner: MediaOwner,
  file: File,
  onProgress: (fraction: number) => void,
): Promise<ProductMedia> {
  const upload = await startMediaUpload(owner, file);
  await sendToStorage(upload, file, onProgress); // passo 2: direto no R2
  return addMedia(owner, upload.uploadId);
}

/** O back cria o vídeo (em conversão, no fim da galeria) e devolve a autorização do envio. */
async function startVideoUpload(owner: MediaOwner, file: File): Promise<VideoUpload> {
  const body = { contentType: file.type, sizeBytes: file.size };
  const { data, error, response } =
    owner.kind === "product"
      ? await api.POST("/api/v1/products/{productId}/videos", { ...product(owner), body })
      : await api.POST("/api/v1/option-items/{optionId}/videos", { ...option(owner), body });
  if (data) return data;
  throw failure(response, error);
}

/**
 * Os dois passos de um vídeo. Depois deles, o serviço de vídeo ainda converte: a galeria mostra
 * "convertendo" até ficar pronto. Se o envio falhar, o vídeo criado sai da galeria (e do serviço
 * de vídeo): sem isso, ele ficaria "convertendo" para sempre.
 */
export async function uploadVideo(
  owner: MediaOwner,
  file: File,
  onProgress: (fraction: number) => void,
): Promise<void> {
  const upload = await startVideoUpload(owner, file);
  try {
    await tusUpload(upload, file, onProgress);
  } catch (error) {
    await removeMedia(owner, upload.mediaId).catch(() => undefined);
    throw error;
  }
}

export async function removeMedia(owner: MediaOwner, mediaId: string): Promise<void> {
  const { error, response } =
    owner.kind === "product"
      ? await api.DELETE("/api/v1/products/{productId}/media/{mediaId}", {
          params: { path: { productId: owner.id, mediaId } },
        })
      : await api.DELETE("/api/v1/option-items/{optionId}/media/{mediaId}", {
          params: { path: { optionId: owner.id, mediaId } },
        });
  if (!response.ok) throw failure(response, error);
}

export async function reorderMedia(owner: MediaOwner, ids: string[]): Promise<void> {
  const body = { ids };
  const { error, response } =
    owner.kind === "product"
      ? await api.PUT("/api/v1/products/{productId}/media-order", { ...product(owner), body })
      : await api.PUT("/api/v1/option-items/{optionId}/media-order", { ...option(owner), body });
  if (!response.ok) throw failure(response, error);
}
