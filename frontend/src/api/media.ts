import { api } from './http'
import type { Me } from './auth'

export interface MediaRef {
  id: number
  url: string
}

export function uploadImage(file: File): Promise<MediaRef> {
  const body = new FormData()
  body.append('file', file)
  return api<MediaRef>('/media', { method: 'POST', body })
}

export const setAvatar = (mediaId: number) => api<Me>('/me/avatar', { method: 'PUT', json: { mediaId } })
