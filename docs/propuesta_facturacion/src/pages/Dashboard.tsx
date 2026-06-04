/**
 * Dashboard placeholder. Sprint 4 #52.
 *
 * Sprint posterior (probablemente 7-8) lo llena con KPIs reales (ventas
 * del dia, alertas de stock, etc.). Hoy es la landing autenticada.
 */
export default function Dashboard() {
  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-(--color-fg)">Inicio</h1>
        <p className="mt-1 text-sm text-(--color-muted)">
          Panel administrativo del POS — Sprint 4 en construcción.
        </p>
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {['Productos', 'Categorías', 'Clientes', 'Usuarios'].map((module) => (
          <div
            key={module}
            className="rounded-lg border border-(--color-border) bg-(--color-surface) p-5"
          >
            <h2 className="text-sm font-medium text-(--color-muted)">{module}</h2>
            <p className="mt-2 text-2xl font-semibold text-(--color-fg)">—</p>
            <p className="mt-1 text-xs text-(--color-muted)">
              Pendiente módulo
            </p>
          </div>
        ))}
      </div>
    </div>
  )
}
