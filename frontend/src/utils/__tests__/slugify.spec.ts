import { describe, expect, it } from 'vitest'
import { slugify } from '../slugify'

// Cùng bộ dữ liệu với SlugsTest (Java) — hai bên phải ra cùng slug.
describe('slugify', () => {
  it.each([
    ['Cà chua', 'ca-chua'],
    ['Đậu phụ chiên', 'dau-phu-chien'],
    ['  Phở   bò!! ', 'pho-bo'],
    ['Bánh mì & pate', 'banh-mi-pate'],
    ['Gà ĐÔNG TẢO', 'ga-dong-tao'],
    ['Nước mắm Phú Quốc', 'nuoc-mam-phu-quoc'],
  ])('slugify(%s)', (i, o) => expect(slugify(i)).toBe(o))

  it('symbols only give empty', () => expect(slugify('!!!')).toBe(''))
})
