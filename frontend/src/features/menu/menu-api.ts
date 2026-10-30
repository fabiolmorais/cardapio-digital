import { api, type Schemas } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

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
