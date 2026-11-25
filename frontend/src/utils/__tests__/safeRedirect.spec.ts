import { describe, expect, it } from 'vitest'
import { safeRedirect } from '../safeRedirect'

describe('safeRedirect', () => {
  it.each([
    ['/recipes', '/recipes'],
    ['/recipes?x=1#a', '/recipes?x=1#a'],
    ['//evil.com', '/'],
    ['/\\evil.com', '/'],
    ['https://evil.com', '/'],
    ['javascript:alert(1)', '/'],
    ['', '/'],
    [undefined, '/'],
    [['/a'], '/'],
  ])('safeRedirect(%s) = %s', (i, o) => expect(safeRedirect(i)).toBe(o))
})
