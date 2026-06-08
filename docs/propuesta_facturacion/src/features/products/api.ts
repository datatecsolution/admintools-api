import { api } from '@/lib/api'
import type { PageResponse, Product, ProductInput } from '@/types/api'

/**
 * Endpoints de productos (master). Sprint 4 #54 + #55.
 *
 * Backend: ProductMasterCtl (Sprint 3 INV-4):
 *   GET    /products?name=&page=&size=   paginado Page<ProductResponse>
 *   POST   /products                      ADMIN
 *   PUT    /products/{id}                 ADMIN
 *   DELETE /products/{id}                 ADMIN
 *
 * No hay GET /products/{id} en el master — el detalle viene de la
 * misma respuesta de la lista. Si en el futuro se necesita
 * recuperar un producto aislado, hay que agregar el endpoint.
 */

export interface ProductsQuery {
  name?: string
  page: number
  size: number
}

export const productsApi = {
  list: ({ name, page, size }: ProductsQuery) => {
    const params = new URLSearchParams()
    if (name) params.set('name', name)
    params.set('page', String(page))
    params.set('size', String(size))
    return api.get<PageResponse<Product>>(`/products?${params.toString()}`)
  },
  create: (input: ProductInput) => api.post<Product>('/products', input),
  update: (id: number, input: ProductInput) =>
    api.put<Product>(`/products/${id}`, input),
  remove: (id: number) => api.del<void>(`/products/${id}`),
}
