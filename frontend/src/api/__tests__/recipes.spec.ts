import { describe, expect, it } from 'vitest'
import { fromQuery, toQuery } from '../recipes'

describe('recipe search query', () => {
  it('round-trips search to URL query', () => {
    const s = { q: 'trứng', ingredientIds: [1, 2], page: 1 }
    expect(toQuery(s)).toEqual({ q: 'trứng', ingredientIds: '1,2', page: '1' })
    expect(fromQuery(toQuery(s))).toEqual(s)
  })

  it('ignores junk ids and empty values', () => {
    expect(fromQuery({ ingredientIds: '3,x,,4', q: '' })).toEqual({ ingredientIds: [3, 4] })
  })
})
