export type Unit = 'G' | 'KG' | 'ML' | 'L' | 'TSP' | 'TBSP' | 'CUP' | 'PIECE' | 'PINCH'

export const UNIT_LABELS: Record<Unit, string> = {
  G: 'g',
  KG: 'kg',
  ML: 'ml',
  L: 'l',
  TSP: 'thìa cà phê',
  TBSP: 'thìa canh',
  CUP: 'cốc',
  PIECE: 'cái/quả',
  PINCH: 'nhúm',
}

/** 1.5, 'TBSP' → "1,5 thìa canh" (dấu thập phân kiểu Việt). */
export const formatQuantity = (q: number, u: Unit) =>
  `${Number(q).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} ${UNIT_LABELS[u]}`
