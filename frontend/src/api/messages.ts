import { ApiError } from './http'

// Chỉ những mã cần câu khác với `detail` của backend.
const MESSAGES: Record<string, string> = {
  BAD_CREDENTIALS: 'Email hoặc mật khẩu không đúng',
  EMAIL_NOT_VERIFIED: 'Email chưa được xác thực. Hãy mở liên kết trong hộp thư của bạn.',
  ACCOUNT_LOCKED: 'Tài khoản đã bị khóa. Liên hệ quản trị viên.',
  INVALID_TOKEN: 'Liên kết không hợp lệ hoặc đã hết hạn',
  UNAUTHENTICATED: 'Phiên đăng nhập đã hết, hãy đăng nhập lại',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này',
  MEDIA_TYPE: 'Chỉ nhận ảnh JPEG, PNG hoặc WebP',
  MEDIA_TOO_LARGE: 'Ảnh tối đa 5MB',
  NAME_TAKEN: 'Tên này đã có, hãy chọn tên khác',
  IN_USE: 'Mục này đang được dùng nên không xóa được',
}

export function messageFor(e: unknown): string {
  if (e instanceof ApiError) return (e.code && MESSAGES[e.code]) || e.message
  return e instanceof Error ? e.message : 'Đã có lỗi xảy ra'
}
