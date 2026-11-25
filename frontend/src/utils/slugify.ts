/** Cùng luật với Slugs.slugify (Java): bỏ dấu, đ/Đ → d, chữ thường, ký tự khác [a-z0-9] → '-'. */
export function slugify(text: string): string {
  return text
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}
