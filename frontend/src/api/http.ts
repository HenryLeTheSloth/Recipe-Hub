export class ApiError extends Error {
  status: number // 0 = không kết nối được
  code?: string
  fieldErrors?: Record<string, string>

  constructor(status: number, message: string, code?: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

function xsrfToken(): string | undefined {
  const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
  return m ? decodeURIComponent(m[1]!) : undefined
}

function parse(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

export async function api<T>(path: string, init: RequestInit & { json?: unknown } = {}): Promise<T> {
  const { json, ...rest } = init
  const headers = new Headers(rest.headers)
  if (json !== undefined) headers.set('Content-Type', 'application/json')
  const method = (rest.method ?? 'GET').toUpperCase()
  const token = method === 'GET' || method === 'HEAD' ? undefined : xsrfToken()
  if (token) headers.set('X-XSRF-TOKEN', token)

  let res: Response
  try {
    res = await fetch('/api' + path, {
      ...rest,
      headers,
      body: json !== undefined ? JSON.stringify(json) : rest.body,
      credentials: 'same-origin',
    })
  } catch {
    throw new ApiError(0, 'Không kết nối được máy chủ')
  }

  // Đọc text thay vì res.json(): 204 và 200 rỗng đều trả undefined.
  const text = await res.text()
  // Proxy Vite trả 502 rỗng khi backend tắt; 502 thật của backend luôn có body ProblemDetail.
  if (res.status === 502 && !text) throw new ApiError(0, 'Không kết nối được máy chủ')
  const body = text ? parse(text) : undefined
  if (!res.ok) {
    const p = (body ?? {}) as { detail?: string; code?: string; errors?: Record<string, string> }
    // Phiên hết hạn hoặc bị admin xóa; main.ts nghe sự kiện này để đưa về trạng thái chưa đăng nhập.
    if (p.code === 'UNAUTHENTICATED') window.dispatchEvent(new Event('api:unauthenticated'))
    throw new ApiError(res.status, p.detail ?? `Lỗi máy chủ (${res.status})`, p.code, p.errors)
  }
  return body as T
}
