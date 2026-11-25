import type { LocationQuery } from 'vue-router'
import { api } from './http'
import type { Page } from './admin'
import type { Category, Tag } from './catalog'
import type { Unit } from '@/utils/units'

export type { Page }
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type RecipeStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED' | 'HIDDEN'

export const DIFFICULTY_LABELS: Record<Difficulty, string> = { EASY: 'Dễ', MEDIUM: 'Vừa', HARD: 'Khó' }
export const STATUS_LABELS: Record<RecipeStatus, string> = {
  DRAFT: 'Bản nháp',
  PENDING: 'Chờ duyệt',
  PUBLISHED: 'Đã đăng',
  REJECTED: 'Bị từ chối',
  HIDDEN: 'Đã ẩn',
}

export interface RecipeCard {
  id: number
  slug: string
  title: string
  summary: string | null
  coverUrl: string | null
  categoryName: string | null
  difficulty: Difficulty
  totalMinutes: number
  authorName: string
  status: RecipeStatus
  publishedAt: string | null
}

export interface RecipeDetail {
  id: number
  slug: string
  title: string
  summary: string | null
  category: Category | null
  tags: Tag[]
  servings: number
  prepMinutes: number
  cookMinutes: number
  difficulty: Difficulty
  status: RecipeStatus
  coverUrl: string | null
  author: { id: number; username: string; displayName: string; avatarUrl: string | null }
  ingredients: { ingredientId: number; name: string; emoji: string | null; quantity: number; unit: Unit; note: string | null }[]
  steps: { stepNo: number; content: string; imageUrl: string | null }[]
  rejectionReason: string | null
  publishedAt: string | null
  createdAt: string
  updatedAt: string
}

/** page tính từ 1 (như trên URL); API Spring tính từ 0. */
export interface RecipeSearch {
  q?: string
  category?: string
  tag?: string
  ingredientIds?: number[]
  sort?: 'newest' | 'rating'
  page?: number
}

export function toQuery(s: RecipeSearch): Record<string, string> {
  const out: Record<string, string> = {}
  for (const [k, v] of Object.entries(s)) {
    const str = Array.isArray(v) ? v.join(',') : v == null ? '' : String(v)
    if (str) out[k] = str
  }
  return out
}

export function fromQuery(q: LocationQuery): RecipeSearch {
  const one = (k: string) => {
    const v = q[k]
    return (Array.isArray(v) ? v[0] : v) || undefined
  }
  const s: RecipeSearch = {}
  for (const k of ['q', 'category', 'tag'] as const) {
    const v = one(k)
    if (v) s[k] = v
  }
  const ids = (one('ingredientIds') ?? '').split(',').map(Number).filter((n) => Number.isInteger(n) && n > 0)
  if (ids.length) s.ingredientIds = ids
  const sort = one('sort')
  if (sort === 'newest' || sort === 'rating') s.sort = sort
  const page = Number(one('page'))
  if (Number.isInteger(page) && page > 0) s.page = page
  return s
}

export function searchRecipes(s: RecipeSearch): Promise<Page<RecipeCard>> {
  const p = new URLSearchParams(toQuery({ ...s, page: undefined }))
  if (s.page && s.page > 1) p.set('page', String(s.page - 1))
  return api<Page<RecipeCard>>('/recipes?' + p)
}

export const getRecipe = (slug: string) => api<RecipeDetail>(`/recipes/${encodeURIComponent(slug)}`)
