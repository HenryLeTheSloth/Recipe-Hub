import { api } from './http'
import type { Page } from './admin'

export type IngredientGroup =
  | 'VEGETABLE' | 'FRUIT' | 'MEAT' | 'SEAFOOD' | 'EGG_DAIRY' | 'GRAIN' | 'SPICE' | 'SAUCE' | 'OTHER'
export type IngredientStatus = 'APPROVED' | 'PENDING'

export const GROUP_LABELS: Record<IngredientGroup, string> = {
  VEGETABLE: 'Rau củ',
  FRUIT: 'Trái cây',
  MEAT: 'Thịt',
  SEAFOOD: 'Hải sản',
  EGG_DAIRY: 'Trứng & sữa',
  GRAIN: 'Ngũ cốc',
  SPICE: 'Gia vị',
  SAUCE: 'Nước chấm & sốt',
  OTHER: 'Khác',
}

export interface Ingredient {
  id: number
  name: string
  slug: string
  group: IngredientGroup
  emoji: string | null
  status: IngredientStatus
}

export interface IngredientInput {
  name: string
  group: IngredientGroup
  emoji?: string | null
}

export interface Category {
  id: number
  name: string
  slug: string
  sortOrder: number
}

export interface Tag {
  id: number
  name: string
  slug: string
}

/** Danh mục và tag dùng chung một kiểu CRUD; tag bỏ qua sortOrder. */
export type NamedKind = 'categories' | 'tags'
export type NamedItem = Category | Tag

export const listNamed = (kind: NamedKind) => api<NamedItem[]>(`/${kind}`)
export const saveNamed = (kind: NamedKind, id: number | null, body: { name: string; sortOrder?: number }) =>
  api<NamedItem>(id ? `/admin/${kind}/${id}` : `/admin/${kind}`, { method: id ? 'PUT' : 'POST', json: body })
export const deleteNamed = (kind: NamedKind, id: number) => api<void>(`/admin/${kind}/${id}`, { method: 'DELETE' })

export function listIngredients(q?: string, group?: IngredientGroup): Promise<Ingredient[]> {
  const p = new URLSearchParams()
  if (q) p.set('q', q)
  if (group) p.set('group', group)
  return api<Ingredient[]>('/ingredients?' + p)
}
export const suggestIngredient = (r: IngredientInput) => api<Ingredient>('/ingredients', { method: 'POST', json: r })

export function adminListIngredients(status: IngredientStatus | '', q: string, page: number, size: number) {
  const p = new URLSearchParams({ q, page: String(page), size: String(size) })
  if (status) p.set('status', status)
  return api<Page<Ingredient>>('/admin/ingredients?' + p)
}
export const updateIngredient = (id: number, r: IngredientInput & { status: IngredientStatus }) =>
  api<Ingredient>(`/admin/ingredients/${id}`, { method: 'PUT', json: r })
export const deleteIngredient = (id: number) => api<void>(`/admin/ingredients/${id}`, { method: 'DELETE' })
