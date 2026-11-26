import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import type { Permission } from '@/api/auth'
import { listComments, type Comment } from '@/api/community'
import { useAuthStore } from '@/stores/auth'
import CommentSection from '../CommentSection.vue'

vi.mock('@/api/community', () => ({ listComments: vi.fn<typeof listComments>(), postComment: vi.fn(), deleteComment: vi.fn(), hideComment: vi.fn() }))

const author = { id: 2, username: 'u', displayName: 'U', avatarUrl: null }
const tree: Comment[] = [{
  id: 1, content: 'Gốc', author, hidden: false, createdAt: '', replies: [
    { id: 2, content: 'Trả lời', author, hidden: true, createdAt: '', replies: [] },
  ],
}]

async function setup(permissions: Permission[]) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().me = { id: 9, email: 'a@b.c', username: 'a', displayName: 'A', avatarUrl: null, roles: [], permissions }
  vi.mocked(listComments).mockResolvedValue(tree)
  const w = mount(CommentSection, { props: { recipeId: 5 }, global: { plugins: [pinia], stubs: { RouterLink: true } } })
  await flushPromises()
  return w
}
const buttons = (w: Awaited<ReturnType<typeof setup>>, label: string) => w.findAll('button').filter((b) => b.text() === label)

describe('CommentSection', () => {
  beforeEach(() => vi.resetAllMocks())

  it('reply button only on root comments', async () => {
    const w = await setup(['COMMUNITY_PARTICIPATE'])
    expect(w.text()).toContain('Trả lời')
    expect(buttons(w, 'Trả lời')).toHaveLength(1)
  })

  it('moderator sees hide buttons and hidden label', async () => {
    const w = await setup(['COMMUNITY_PARTICIPATE', 'CONTENT_MODERATE'])
    expect(buttons(w, 'Ẩn')).toHaveLength(1)
    expect(buttons(w, 'Hiện')).toHaveLength(1)
    expect(w.text()).toContain('Đã ẩn')
  })

  it('regular member sees no hide buttons', async () => {
    const w = await setup(['COMMUNITY_PARTICIPATE'])
    expect(buttons(w, 'Ẩn')).toHaveLength(0)
  })
})
