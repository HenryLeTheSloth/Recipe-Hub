import { describe, expect, it } from 'vitest'
import type { Ingredient } from '@/api/catalog'
import { addIngredient, emptyState, fieldLabel, moveStep, toRequest } from '../editorState'

const egg: Ingredient = { id: 5, name: 'Trứng gà', slug: 'trung-ga', group: 'EGG_DAIRY', emoji: '🥚', status: 'APPROVED' }

describe('editorState', () => {
  it('does not add the same ingredient twice', () => {
    const s = emptyState()
    const once = addIngredient(s, egg)
    expect(addIngredient(once, egg)).toBe(once)
    expect(once.ingredients).toHaveLength(1)
    expect(s.ingredients).toHaveLength(0)
  })

  it('a new draft with only a title sends no blank step', () => {
    expect(toRequest({ ...emptyState(), title: 'Nháp' }).steps).toEqual([])
  })

  it('moveStep reorders and toRequest keeps order', () => {
    const s = { ...emptyState(), steps: ['A', 'B', 'C'].map((content) => ({ content, media: null })) }
    const r = toRequest(moveStep(s, 2, 0))
    expect(r.steps.map((x) => x.content)).toEqual(['C', 'A', 'B'])
  })

  it('toRequest sends ids and media ids', () => {
    const s = {
      ...addIngredient(emptyState(), egg),
      title: 'Trứng chiên',
      cover: { id: 9, url: '/api/media/9/file' },
      steps: [{ content: 'Chiên', media: { id: 10, url: '/api/media/10/file' } }],
    }
    const r = toRequest(s)
    expect(r.coverMediaId).toBe(9)
    expect(r.ingredients).toEqual([{ ingredientId: 5, quantity: null, unit: 'PIECE', note: null }])
    expect(r.steps).toEqual([{ content: 'Chiên', mediaId: 10 }])
  })

  it('fieldLabel names the line or step a server error points at', () => {
    const s = addIngredient(emptyState(), egg)
    expect(fieldLabel(s, 'ingredients[0].quantity')).toBe('Trứng gà – số lượng')
    expect(fieldLabel(s, 'steps[0].content')).toBe('Bước 1 – nội dung')
    expect(fieldLabel(s, 'title')).toBe('Tên món')
  })
})
