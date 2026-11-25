import type { Ingredient } from '@/api/catalog'
import type { MediaRef } from '@/api/media'
import type { Difficulty, RecipeDetail, RecipeRequest } from '@/api/recipes'
import type { Unit } from '@/utils/units'

export interface Line {
  ingredient: Pick<Ingredient, 'id' | 'name' | 'emoji'>
  quantity: number | null
  unit: Unit
  note: string
}

export interface EditorState {
  title: string
  summary: string
  categoryId: number | null
  servings: number
  prepMinutes: number
  cookMinutes: number
  difficulty: Difficulty
  cover: MediaRef | null
  tagIds: number[]
  ingredients: Line[]
  steps: { content: string; media: MediaRef | null }[]
}

export const emptyState = (): EditorState => ({
  title: '', summary: '', categoryId: null, servings: 2, prepMinutes: 10, cookMinutes: 15, difficulty: 'EASY',
  cover: null, tagIds: [], ingredients: [], steps: [{ content: '', media: null }],
})

const media = (id: number | null, url: string | null): MediaRef | null => (id && url ? { id, url } : null)

export const fromDetail = (d: RecipeDetail): EditorState => ({
  title: d.title,
  summary: d.summary ?? '',
  categoryId: d.category?.id ?? null,
  servings: d.servings,
  prepMinutes: d.prepMinutes,
  cookMinutes: d.cookMinutes,
  difficulty: d.difficulty,
  cover: media(d.coverMediaId, d.coverUrl),
  tagIds: d.tags.map((t) => t.id),
  ingredients: d.ingredients.map((i) => ({
    ingredient: { id: i.ingredientId, name: i.name, emoji: i.emoji }, quantity: i.quantity, unit: i.unit, note: i.note ?? '',
  })),
  steps: d.steps.map((s) => ({ content: s.content, media: media(s.mediaId, s.imageUrl) })),
})

/** Nguyên liệu đã có → trả nguyên s (server cũng chặn DUPLICATE_INGREDIENT). */
export function addIngredient(s: EditorState, ing: Pick<Ingredient, 'id' | 'name' | 'emoji'>): EditorState {
  if (s.ingredients.some((l) => l.ingredient.id === ing.id)) return s
  const line: Line = { ingredient: { id: ing.id, name: ing.name, emoji: ing.emoji }, quantity: null, unit: 'PIECE', note: '' }
  return { ...s, ingredients: [...s.ingredients, line] }
}

export function moveStep(s: EditorState, from: number, to: number): EditorState {
  const steps = [...s.steps]
  const [moved] = steps.splice(from, 1)
  if (!moved) return s
  steps.splice(to, 0, moved)
  return { ...s, steps }
}

/** Thứ tự mảng chính là position (nguyên liệu) và stepNo (bước). */
export const toRequest = (s: EditorState): RecipeRequest => ({
  title: s.title.trim(),
  summary: s.summary.trim() || null,
  categoryId: s.categoryId,
  servings: s.servings,
  prepMinutes: s.prepMinutes,
  cookMinutes: s.cookMinutes,
  difficulty: s.difficulty,
  coverMediaId: s.cover?.id ?? null,
  tagIds: s.tagIds,
  ingredients: s.ingredients.map((l) => ({
    ingredientId: l.ingredient.id, quantity: l.quantity, unit: l.unit, note: l.note.trim() || null,
  })),
  steps: s.steps.map((x) => ({ content: x.content.trim(), mediaId: x.media?.id ?? null })),
})

const LABELS: Record<string, string> = {
  title: 'Tên món', summary: 'Giới thiệu', categoryId: 'Danh mục', servings: 'Khẩu phần', prepMinutes: 'Thời gian chuẩn bị',
  cookMinutes: 'Thời gian nấu', difficulty: 'Độ khó', coverMediaId: 'Ảnh bìa', tagIds: 'Tag', ingredients: 'Nguyên liệu',
  steps: 'Các bước', quantity: 'số lượng', unit: 'đơn vị', note: 'ghi chú', content: 'nội dung', mediaId: 'ảnh',
}

/** Đường dẫn lỗi của server ("ingredients[1].quantity") → nhãn người đọc được. */
export function fieldLabel(s: EditorState, path: string): string {
  const m = /^(ingredients|steps)\[(\d+)\]\.(\w+)$/.exec(path)
  if (!m) return LABELS[path] ?? path
  const i = Number(m[2])
  const where = m[1] === 'ingredients' ? (s.ingredients[i]?.ingredient.name ?? `Nguyên liệu ${i + 1}`) : `Bước ${i + 1}`
  return `${where} – ${LABELS[m[3]!] ?? m[3]}`
}
