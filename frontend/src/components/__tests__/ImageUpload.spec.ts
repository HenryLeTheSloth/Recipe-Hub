import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { uploadImage } from '@/api/media'
import { ApiError } from '@/api/http'
import ImageUpload from '../ImageUpload.vue'

vi.mock('@/api/media', () => ({ uploadImage: vi.fn<typeof uploadImage>() }))

async function pick(w: ReturnType<typeof mount>, file: File) {
  const input = w.find('input[type=file]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
  await flushPromises()
}

describe('ImageUpload', () => {
  beforeEach(() => vi.resetAllMocks())

  it('accepts only jpeg, png, webp', () => {
    const w = mount(ImageUpload, { props: { modelValue: null } })
    expect(w.find('input[type=file]').attributes('accept')).toBe('image/jpeg,image/png,image/webp')
  })

  it('rejects files over 5MB without calling API', async () => {
    const w = mount(ImageUpload, { props: { modelValue: null } })
    await pick(w, new File([new Uint8Array(6 * 1024 * 1024)], 'big.jpg', { type: 'image/jpeg' }))
    expect(w.text()).toContain('Ảnh tối đa 5MB')
    expect(uploadImage).not.toHaveBeenCalled()
    expect(w.emitted('update:modelValue')).toBeUndefined()
  })

  it('emits MediaRef after upload', async () => {
    vi.mocked(uploadImage).mockResolvedValue({ id: 3, url: '/api/media/3/file' })
    const w = mount(ImageUpload, { props: { modelValue: null } })
    const file = new File([new Uint8Array(10)], 'a.png', { type: 'image/png' })
    await pick(w, file)
    expect(uploadImage).toHaveBeenCalledWith(file)
    expect(w.emitted('update:modelValue')).toEqual([[{ id: 3, url: '/api/media/3/file' }]])
  })

  it('shows server error', async () => {
    vi.mocked(uploadImage).mockRejectedValue(new ApiError(415, 'x', 'MEDIA_TYPE'))
    const w = mount(ImageUpload, { props: { modelValue: null } })
    await pick(w, new File([new Uint8Array(10)], 'a.png', { type: 'image/png' }))
    expect(w.text()).toContain('Chỉ nhận ảnh JPEG, PNG hoặc WebP')
  })

  it('previews current image', () => {
    const w = mount(ImageUpload, { props: { modelValue: { id: 3, url: '/api/media/3/file' } } })
    expect(w.find('img').attributes('src')).toBe('/api/media/3/file')
  })
})
