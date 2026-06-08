import type { ReactNode } from 'react'

/**
 * Header reusable para cada pagina de modulo. Sprint 4 #56.
 * Titulo grande + subtitulo opcional + slot derecho para acciones
 * (boton "Nuevo X" tipicamente).
 */

interface PageHeaderProps {
  title: string
  description?: string
  actions?: ReactNode
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-bold text-(--color-fg)">{title}</h1>
        {description && (
          <p className="mt-1 text-sm text-(--color-muted)">{description}</p>
        )}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}
