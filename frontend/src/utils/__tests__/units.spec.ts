import { describe, expect, it } from 'vitest'
import { formatQuantity } from '../units'

describe('formatQuantity', () => {
  it('formats quantity with Vietnamese labels', () => {
    expect(formatQuantity(200, 'G')).toBe('200 g')
    expect(formatQuantity(1.5, 'TBSP')).toBe('1,5 thìa canh')
  })
})
