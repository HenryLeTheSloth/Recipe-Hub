import { describe, expect, it } from 'vitest'
import { ApiError } from '../http'
import { messageFor } from '../messages'

describe('messageFor', () => {
  it('keeps the specific FORBIDDEN reason from the server', () => {
    expect(messageFor(new ApiError(403, 'Ảnh không thuộc về bạn', 'FORBIDDEN'))).toBe('Ảnh không thuộc về bạn')
  })
})
