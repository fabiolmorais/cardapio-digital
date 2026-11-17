import { api, type Schemas } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

/** As opções dos produtos (ADR 0010): a biblioteca de grupos e os tamanhos e grupos de cada produto. */
export type OptionGroup = Schemas["OptionGroup"];
export type OptionItem = Schemas["OptionItem"];
export type OptionGroupRequest = Schemas["OptionGroupRequest"];
export type OptionPricing = OptionGroup["pricing"];
export type ProductOptions = Schemas["ProductOptions"];
export type ProductOptionsRequest = Schemas["ProductOptionsRequest"];
export type ProductVariant = Schemas["ProductVariant"];
export type OptionGroupPrices = Schemas["OptionGroupPrices"];
export type OptionGroupPrice = Schemas["OptionGroupPrice"];
export type OptionGroupPriceColumn = Schemas["OptionGroupPriceColumn"];

const t = messages.menu.errors;

function failure(response: Response | undefined, error: unknown): Error {
  const problem = error as { detail?: string; fields?: string[] } | undefined;
  if (response?.status === 400 && problem?.fields) return new Error(t.fields);
  if (response && [400, 403, 404, 409, 422].includes(response.status) && problem?.detail) {
    return new Error(problem.detail);
  }
  return new Error(t.generic);
}

export const OPTION_GROUPS_KEY = ["option-groups"] as const;

export async function fetchOptionGroups(): Promise<OptionGroup[]> {
  const { data, error, response } = await api.GET("/api/v1/option-groups", {
    params: { query: { page: 0, size: 100 } },
  });
  if (data) return data.content;
  throw failure(response, error);
}

export async function createOptionGroup(body: OptionGroupRequest): Promise<OptionGroup> {
  const { data, error, response } = await api.POST("/api/v1/option-groups", { body });
  if (data) return data;
  throw failure(response, error);
}

export async function updateOptionGroup(id: string, body: OptionGroupRequest): Promise<OptionGroup> {
  const { data, error, response } = await api.PUT("/api/v1/option-groups/{id}", { params: { path: { id } }, body });
  if (data) return data;
  throw failure(response, error);
}

export async function deactivateOptionGroup(id: string): Promise<void> {
  const { error, response } = await api.DELETE("/api/v1/option-groups/{id}", { params: { path: { id } } });
  if (!response.ok) throw failure(response, error);
}

export async function setOptionItemAvailability(id: string, available: boolean): Promise<OptionItem> {
  const { data, error, response } = await api.PATCH("/api/v1/option-items/{id}/availability", {
    params: { path: { id } },
    body: { available },
  });
  if (data) return data;
  throw failure(response, error);
}

export async function fetchProductOptions(productId: string): Promise<ProductOptions> {
  const { data, error, response } = await api.GET("/api/v1/products/{id}/options", {
    params: { path: { id: productId } },
  });
  if (data) return data;
  throw failure(response, error);
}

export async function updateProductOptions(productId: string, body: ProductOptionsRequest): Promise<ProductOptions> {
  const { data, error, response } = await api.PUT("/api/v1/products/{id}/options", {
    params: { path: { id: productId } },
    body,
  });
  if (data) return data;
  throw failure(response, error);
}

export async function setVariantAvailability(id: string, available: boolean): Promise<ProductVariant> {
  const { data, error, response } = await api.PATCH("/api/v1/product-variants/{id}/availability", {
    params: { path: { id } },
    body: { available },
  });
  if (data) return data;
  throw failure(response, error);
}

// A grade de preços do grupo (complemento da ADR 0010): o preço de cada opção em cada tamanho dos
// produtos que usam o grupo.

export async function fetchGroupPrices(id: string): Promise<OptionGroupPrices> {
  const { data, error, response } = await api.GET("/api/v1/option-groups/{id}/prices", { params: { path: { id } } });
  if (data) return data;
  throw failure(response, error);
}

/** Troca a grade inteira: o que não vier fica em branco (vale o preço da opção). */
export async function saveGroupPrices(id: string, prices: OptionGroupPrice[]): Promise<OptionGroupPrices> {
  const { data, error, response } = await api.PUT("/api/v1/option-groups/{id}/prices", {
    params: { path: { id } },
    body: { prices },
  });
  if (data) return data;
  throw failure(response, error);
}
