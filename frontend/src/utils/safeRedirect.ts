/** Chỉ cho phép đường dẫn nội bộ ("/x"); chặn "//host", "/\host" và URL tuyệt đối (open redirect). */
export function safeRedirect(r: unknown): string {
  return typeof r === 'string' && r.startsWith('/') && !r.startsWith('//') && !r.startsWith('/\\') ? r : '/'
}
