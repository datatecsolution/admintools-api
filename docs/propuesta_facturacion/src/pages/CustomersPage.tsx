import { useState } from 'react'
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  MagnifyingGlassIcon,
  PencilSquareIcon,
  PlusIcon,
  TrashIcon,
} from '@heroicons/react/24/outline'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { PageHeader } from '@/components/ui/PageHeader'
import { Spinner } from '@/components/ui/Spinner'
import {
  EmptyState,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeaderCell,
  TableRow,
} from '@/components/ui/Table'
import { CustomerFormDialog } from '@/features/customers/components/CustomerFormDialog'
import {
  useCustomers,
  useDeleteCustomer,
} from '@/features/customers/hooks/useCustomers'
import { useDebounce } from '@/hooks/useDebounce'
import { ApiError } from '@/lib/api'
import type { Customer } from '@/types/api'

/**
 * Listado paginado + CRUD de clientes. Sprint 4 #57 / US-028.
 *
 * Misma estructura que ProductsPage: search server-side con debounce,
 * paginacion, modal de crear/editar, confirm de borrar. Sin filtros
 * locales — los clientes ya se filtran server-side por vendedor del
 * usuario logueado.
 *
 * "Vista de saldos" e "histórico de facturas" del cliente quedan out
 * of scope; son mejoras futuras.
 */

const PAGE_SIZE = 10

export default function CustomersPage() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [openForm, setOpenForm] = useState(false)
  const [editing, setEditing] = useState<Customer | null>(null)
  const [toDelete, setToDelete] = useState<Customer | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const debouncedSearch = useDebounce(search, 300)
  const customers = useCustomers({
    name: debouncedSearch || undefined,
    page,
    size: PAGE_SIZE,
  })
  const del = useDeleteCustomer()

  const handleSearchChange = (value: string) => {
    setSearch(value)
    if (page !== 0) setPage(0)
  }

  const handleNew = () => {
    setEditing(null)
    setOpenForm(true)
  }
  const handleEdit = (c: Customer) => {
    setEditing(c)
    setOpenForm(true)
  }

  const handleConfirmDelete = async () => {
    if (!toDelete) return
    setDeleteError(null)
    try {
      await del.mutateAsync(toDelete.id)
      setToDelete(null)
    } catch (err) {
      if (err instanceof ApiError) {
        const body = err.body as { message?: string } | null
        setDeleteError(body?.message ?? err.statusText)
      } else if (err instanceof Error) {
        setDeleteError(err.message)
      }
    }
  }

  const totalElements = customers.data?.totalElements ?? 0
  const totalPages = customers.data?.totalPages ?? 0
  const showingFrom = totalElements === 0 ? 0 : page * PAGE_SIZE + 1
  const showingTo = Math.min((page + 1) * PAGE_SIZE, totalElements)
  const items = customers.data?.content ?? []

  return (
    <div>
      <PageHeader
        title="Clientes"
        description="Maestro de clientes. Búsqueda por nombre, filtrado por vendedor del usuario logueado."
        actions={
          <Button onClick={handleNew}>
            <PlusIcon className="h-4 w-4" />
            Nuevo
          </Button>
        }
      />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <MagnifyingGlassIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-(--color-muted)" />
          <input
            type="search"
            placeholder="Buscar por nombre…"
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            className="h-10 w-full rounded-md border border-(--color-border) bg-(--color-bg) pl-9 pr-3 text-sm text-(--color-fg) outline-none focus:ring-2 focus:ring-(--color-primary)"
          />
        </div>
      </div>

      {customers.isLoading && (
        <div className="flex items-center justify-center py-12 text-(--color-muted)">
          <Spinner size={20} />
          <span className="ml-2 text-sm">Cargando clientes...</span>
        </div>
      )}

      {customers.isError && (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
          Error al cargar clientes: {customers.error.message}
        </div>
      )}

      {customers.data && totalElements === 0 && (
        <EmptyState>
          {debouncedSearch
            ? `No hay clientes para "${debouncedSearch}".`
            : 'No tenés clientes asignados.'}
        </EmptyState>
      )}

      {customers.data && totalElements > 0 && (
        <>
          <Table>
            <TableHead>
              <TableRow>
                <TableHeaderCell className="w-16">#</TableHeaderCell>
                <TableHeaderCell>Nombre</TableHeaderCell>
                <TableHeaderCell>RTN</TableHeaderCell>
                <TableHeaderCell>Dirección</TableHeaderCell>
                <TableHeaderCell>Teléfono</TableHeaderCell>
                <TableHeaderCell className="w-32 text-right">Acciones</TableHeaderCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((c) => (
                <TableRow key={c.id}>
                  <TableCell className="text-(--color-muted)">{c.id}</TableCell>
                  <TableCell className="font-medium">{c.name}</TableCell>
                  <TableCell className="text-(--color-muted) tabular-nums">
                    {c.rtn || <span className="italic">—</span>}
                  </TableCell>
                  <TableCell className="max-w-xs truncate text-(--color-muted)">
                    {c.address || <span className="italic">—</span>}
                  </TableCell>
                  <TableCell className="text-(--color-muted)">
                    {c.phone || <span className="italic">—</span>}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="inline-flex gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleEdit(c)}
                        aria-label={`Editar ${c.name}`}
                      >
                        <PencilSquareIcon className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setToDelete(c)}
                        aria-label={`Eliminar ${c.name}`}
                      >
                        <TrashIcon className="h-4 w-4 text-(--color-danger)" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <div className="mt-4 flex items-center justify-between text-sm text-(--color-muted)">
            <div className="flex items-center gap-2">
              <span>
                Mostrando {showingFrom}-{showingTo} de {totalElements}
              </span>
              {customers.isFetching && !customers.isLoading && (
                <Spinner size={14} />
              )}
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                aria-label="Página anterior"
              >
                <ChevronLeftIcon className="h-4 w-4" />
              </Button>
              <span className="min-w-[6rem] text-center text-(--color-fg)">
                Página {page + 1} / {totalPages || 1}
              </span>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= totalPages}
                aria-label="Página siguiente"
              >
                <ChevronRightIcon className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </>
      )}

      <CustomerFormDialog
        open={openForm}
        onClose={() => setOpenForm(false)}
        initial={editing}
      />

      <ConfirmDialog
        open={toDelete != null}
        onClose={() => {
          setToDelete(null)
          setDeleteError(null)
        }}
        onConfirm={handleConfirmDelete}
        title={`Eliminar "${toDelete?.name}"`}
        description={deleteError ?? 'Esta acción no se puede deshacer.'}
        confirmLabel="Eliminar"
        variant="danger"
        loading={del.isPending}
      />
    </div>
  )
}
