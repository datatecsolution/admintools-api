import { z } from 'zod'

/**
 * Schema de validacion de Producto. Sprint 4 #55 / US-026.
 *
 * Alineado con ProductRequest del backend (INV-4):
 *   - name max 255 obligatorio
 *   - price >= 0 obligatorio
 *   - categoryId, taxId, type, active obligatorios
 *   - altCode opcional >= 0
 *
 * El tipo 1 = bien (con kardex), 2 = insumo (sin kardex). Se mapea a
 * un campo radio en el form con labels en humano.
 */
/**
 * `prices` es Record<priceTypeId, string-input>. Lo guardamos como
 * string para no pelear con react-hook-form + inputs vacios (que NaN
 * en valueAsNumber). En el submit, convertimos a number/decimal: vacio
 * o "0" → no se envia al PUT (el upsert lo interpreta como delete).
 *
 * El campo `price` del master (Producto.price → articulo.precio_articulo)
 * se sincroniza con el valor del precio "Publico General" (priceTypeId=1).
 * Esto preserva las queries legacy de Swing que leen ese campo directo.
 */
export const productSchema = z.object({
  name: z
    .string()
    .min(1, 'Requerido')
    .max(255, 'Máximo 255 caracteres'),
  categoryId: z
    .number({ message: 'Seleccionar categoría' })
    .int()
    .positive('Seleccionar categoría'),
  taxId: z
    .number({ message: 'Seleccionar impuesto' })
    .int()
    .positive('Seleccionar impuesto'),
  altCode: z
    .number()
    .int()
    .nonnegative('Debe ser >= 0')
    .optional(),
  type: z.union([z.literal(1), z.literal(2)], { message: 'Tipo inválido' }),
  active: z.boolean(),
  // priceTypeId (string para tener clave Record) → valor de input
  prices: z.record(z.string(), z.string()),
})

export type ProductFormValues = z.infer<typeof productSchema>
