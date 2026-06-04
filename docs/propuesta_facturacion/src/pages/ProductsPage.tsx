import { useMemo, useState } from 'react'
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  MagnifyingGlassIcon,
  PencilSquareIcon,
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
import { useCategories } from '@/features/categories/hooks/useCategories'
import { ProductFormDialog } from '@/features/products/components/ProductFormDialog'
import {
  useDeleteProduct,
  useProducts,
} from '@/features/products/hooks/useProducts'
import { useDebounce } from '@/hooks/useDebounce'
import { ApiError } from '@/lib/api'
import type { Product } from '@/types/api'

/**
 * Listado paginado de productos. Sprint 4 #54 / US-025 + #55 / US-026.
 *
 * Search server-side con debounce 300ms; volver a pagina 0 cuando
 * cambia el texto. Filtro por categoria es client-side (filtra la
 * pagina actual) porque el backend no acepta categoryId en /products
 * paginado todavia — se puede agregar en una US futura.
 *
 * Botones Nuevo / Editar activos desde #55: abren ProductFormDialog.
 */

const PAGE_SIZE = 10

function formatPrice(value: number): string {
  return new Intl.NumberFormat('es-HN', {
    style: 'currency',
    currency: 'HNL',
    maximumFractionDigits: 2,
  }).format(value)
}

export default function ProductsPage() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [categoryFilter, setCategoryFilter] = useState<number | 'all'>('all')
  const [toDelete, setToDelete] = useState<Product | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [openForm, setOpenForm] = useState(false)
  const [editing, setEditing] = useState<Product | null>(null)

  const handleNew = () => {
    setEditing(null)
    setOpenForm(true)
  }

  const handleEdit = (p: Product) => {
    setEditing(p)
    setOpenForm(true)
  }

  const debouncedSearch = useDebounce(search, 300)
  const products = useProducts({
    name: debouncedSearch || undefined,
    page,
    size: PAGE_SIZE,
  })
  const categories = useCategories()
  const del = useDeleteProduct()

  // Cuando cambia el search, volver a la primera pagina.
  if (debouncedSearch !== search && page !== 0) {
    // no-op (efecto solo si tipea muy rapido)
  }
  const handleSearchChange = (value: string) => {
    setSearch(value)
    if (page !== 0) setPage(0)
  }

  // Mapa id -> nombre para mostrar en la tabla.
  const categoryMap = useMemo(() => {
    const m = new Map<number, string>()
    categories.data?.forEach((c) => m.set(c.id, c.name))
    return m
  }, [categories.data])

  // Filtro local por categoria (sobre la pagina ya cargada).
  const visibleItems = useMemo(() => {
    if (!products.data) return []
    if (categoryFilter === 'all') return products.data.content
    return products.data.content.filter((p) => p.categoryId === categoryFilter)
  }, [products.data, categoryFilter])

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

  const totalElements = products.data?.totalElements ?? 0
  const totalPages = products.data?.totalPages ?? 0
  const showingFrom = totalElements === 0 ? 0 : page * PAGE_SIZE + 1
  const showingTo = Math.min((page + 1) * PAGE_SIZE, totalElements)

  return (
    <div>
      <PageHeader
        title="Productos"
        description="Catálogo maestro. Búsqueda por nombre y filtro local por categoría."
        actions={
          <Button onClick={handleNew}>
            Nuevo
          </Button>
        }
      />

      {/* Toolbar: search + filtro categoria */}
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
        <select
          value={categoryFilter}
          onChange={(e) =>
            setCategoryFilter(
              e.target.value === 'all' ? 'all' : Number(e.target.value),
            )
          }
          className="h-10 rounded-md border border-(--color-border) bg-(--color-bg) px-3 text-sm text-(--color-fg) outline-none focus:ring-2 focus:ring-(--color-primary) sm:w-56"
        >
          <option value="all">Todas las categorías</option>
          {categories.data?.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      {/* Loading inicial */}
      {products.isLoading && (
        <div className="flex items-center justify-center py-12 text-(--color-muted)">
          <Spinner size={20} />
          <span className="ml-2 text-sm">Cargando productos...</span>
        </div>
      )}

      {/* Error */}
      {products.isError && (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
          Error al cargar productos: {products.error.message}
        </div>
      )}

      {/* Empty */}
      {products.data && totalElements === 0 && (
        <EmptyState>
          {debouncedSearch
            ? `No hay productos para "${debouncedSearch}".`
            : 'No hay productos cargados.'}
        </EmptyState>
      )}

      {/* Tabla */}
      {products.data && totalElements > 0 && (
        <>
          <Table>
            <TableHead>
              <TableRow>
                <TableHeaderCell className="w-20">#</TableHeaderCell>
                <TableHeaderCell>Nombre</TableHeaderCell>
                <TableHeaderCell>Categoría</TableHeaderCell>
                <TableHeaderCell className="text-right">Precio</TableHeaderCell>
                <TableHeaderCell className="w-24 text-center">Estado</TableHeaderCell>
                <TableHeaderCell className="w-32 text-right">Acciones</TableHeaderCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {visibleItems.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-(--color-muted)">
                    Sin resultados con el filtro de categoría seleccionado en
                    esta página.
                  </TableCell>
                </TableRow>
              ) : (
                visibleItems.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell className="text-(--color-muted)">
                      {p.id}
                    </TableCell>
                    <TableCell className="font-medium">{p.name}</TableCell>
                    <TableCell className="text-(--color-muted)">
                      {categoryMap.get(p.categoryId) ?? `#${p.categoryId}`}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatPrice(p.price)}
                    </TableCell>
                    <TableCell className="text-center">
                      <span
                        className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                          p.active
                            ? 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300'
                            : 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300'
                        }`}
                      >
                        {p.active ? 'Activo' : 'Inactivo'}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="inline-flex gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleEdit(p)}
                          aria-label={`Editar ${p.name}`}
                        >
                          <PencilSquareIcon className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setToDelete(p)}
                          aria-label={`Eliminar ${p.name}`}
                        >
                          <TrashIcon className="h-4 w-4 text-(--color-danger)" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>

          {/* Paginacion */}
          <div className="mt-4 flex items-center justify-between text-sm text-(--color-muted)">
            <div className="flex items-center gap-2">
              <span>
                Mostrando {showingFrom}-{showingTo} de {totalElements}
              </span>
              {products.isFetching && !products.isLoading && (
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

      <ProductFormDialog
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
