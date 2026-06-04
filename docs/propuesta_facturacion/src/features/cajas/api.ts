import { api } from '@/lib/api'
import type {
  CajaCatalogItem,
  UserCaja,
  UserCajaUpsert,
} from '@/types/api'

/**
 * Endpoints de cajas. Sprint 4.5 fix.
 *
 * Backend:
 *   GET    /cajas                       lista del catalogo (cajas del cliente)
 *   GET    /users/{id}/cajas            cajas asignadas al usuario (con default)
 *   PUT    /users/{id}/cajas            ADMIN — reemplaza el set completo
 *
 * El PUT exige exactamente 1 default. Si la lista llega vacia, se
 * permite (usuario sin cajas; codigo_caja del Usuario queda en 0).
 */

export const cajasApi = {
  list: () => api.get<CajaCatalogItem[]>('/cajas'),
}

export const userCajasApi = {
  list: (userId: number) => api.get<UserCaja[]>(`/users/${userId}/cajas`),
  replace: (userId: number, cajas: UserCajaUpsert[]) =>
    api.put<UserCaja[]>(`/users/${userId}/cajas`, cajas),
}
