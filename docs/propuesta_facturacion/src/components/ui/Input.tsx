import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react'

/**
 * Input primitivo con label opcional, mensaje de error, helper text.
 * Sprint 4 #56 (warm-up del patron modulo).
 *
 * Uso tipico con react-hook-form:
 *   <Input
 *     label="Nombre"
 *     error={errors.name?.message}
 *     {...register('name')}
 *   />
 */

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helper?: ReactNode
  containerClassName?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, helper, containerClassName = '', className = '', id, ...rest },
  ref,
) {
  const inputId =
    id ?? (label ? `input-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined)
  const hasError = Boolean(error)
  return (
    <div className={`flex flex-col gap-1 ${containerClassName}`}>
      {label && (
        <label
          htmlFor={inputId}
          className="text-sm font-medium text-(--color-fg)"
        >
          {label}
        </label>
      )}
      <input
        ref={ref}
        id={inputId}
        aria-invalid={hasError || undefined}
        aria-describedby={hasError ? `${inputId}-error` : undefined}
        className={`h-10 w-full rounded-md border bg-(--color-bg) px-3 text-sm text-(--color-fg) outline-none transition-colors focus:ring-2 focus:ring-(--color-primary) disabled:cursor-not-allowed disabled:opacity-50 ${
          hasError
            ? 'border-(--color-danger) focus:ring-(--color-danger)'
            : 'border-(--color-border)'
        } ${className}`}
        {...rest}
      />
      {hasError ? (
        <p id={`${inputId}-error`} className="text-xs text-red-500">
          {error}
        </p>
      ) : helper ? (
        <p className="text-xs text-(--color-muted)">{helper}</p>
      ) : null}
    </div>
  )
})
