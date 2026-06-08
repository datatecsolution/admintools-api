import { forwardRef, type ButtonHTMLAttributes } from 'react'

/**
 * Boton primitivo del POS. 4 variantes basicas + 3 tamanios.
 * Sprint 4 #52.
 */

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
}

const variantClasses: Record<Variant, string> = {
  primary:
    'bg-(--color-primary) text-white hover:opacity-90 active:opacity-80 disabled:opacity-50',
  secondary:
    'bg-(--color-surface) text-(--color-fg) border border-(--color-border) hover:bg-slate-100 dark:hover:bg-slate-700 disabled:opacity-50',
  ghost:
    'text-(--color-fg) hover:bg-slate-100 dark:hover:bg-slate-700 disabled:opacity-50',
  danger:
    'bg-(--color-danger) text-white hover:opacity-90 active:opacity-80 disabled:opacity-50',
}

const sizeClasses: Record<Size, string> = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-10 px-4 text-sm',
  lg: 'h-11 px-5 text-base',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'primary', size = 'md', className = '', children, ...rest },
  ref,
) {
  return (
    <button
      ref={ref}
      className={`inline-flex items-center justify-center gap-2 rounded-md font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-(--color-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--color-bg) ${variantClasses[variant]} ${sizeClasses[size]} ${className}`}
      {...rest}
    >
      {children}
    </button>
  )
})
