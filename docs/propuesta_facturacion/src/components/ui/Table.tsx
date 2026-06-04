import type { HTMLAttributes, ReactNode, TdHTMLAttributes, ThHTMLAttributes } from 'react'

/**
 * Componentes de tabla simples con estilo unificado. Sprint 4 #56.
 *
 * No hace render virtual; para tablas largas (productos), TanStack
 * Query con paginacion server-side resuelve la performance sin
 * necesidad de window/virtualization.
 */

export function Table({ className = '', ...rest }: HTMLAttributes<HTMLTableElement>) {
  return (
    <div className="overflow-x-auto rounded-md border border-(--color-border) bg-(--color-surface)">
      <table
        className={`w-full border-collapse text-sm text-(--color-fg) ${className}`}
        {...rest}
      />
    </div>
  )
}

export function TableHead(props: HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <thead
      className="border-b border-(--color-border) bg-slate-50 text-left text-xs uppercase tracking-wider text-(--color-muted) dark:bg-slate-900"
      {...props}
    />
  )
}

export function TableBody(props: HTMLAttributes<HTMLTableSectionElement>) {
  return <tbody {...props} />
}

interface TableRowProps extends HTMLAttributes<HTMLTableRowElement> {
  interactive?: boolean
}

export function TableRow({
  interactive = false,
  className = '',
  ...rest
}: TableRowProps) {
  return (
    <tr
      className={`border-t border-(--color-border) first:border-t-0 ${
        interactive ? 'cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-700/30' : ''
      } ${className}`}
      {...rest}
    />
  )
}

export function TableCell({
  className = '',
  ...rest
}: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={`px-4 py-3 align-middle ${className}`} {...rest} />
}

export function TableHeaderCell({
  className = '',
  ...rest
}: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={`px-4 py-3 font-semibold align-middle ${className}`}
      scope="col"
      {...rest}
    />
  )
}

export function EmptyState({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-md border border-dashed border-(--color-border) bg-(--color-surface) p-8 text-center text-sm text-(--color-muted)">
      {children}
    </div>
  )
}
