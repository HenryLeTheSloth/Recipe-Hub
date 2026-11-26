<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { deleteComment, hideComment, listComments, postComment, type Comment } from '@/api/community'
import { messageFor } from '@/api/messages'
import { useForm } from '@/composables/useForm'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ recipeId: number }>()
const auth = useAuthStore()
const comments = ref<Comment[]>([])
const loadError = ref('')
const content = ref('')
const replyTo = ref<number | null>(null)
const replyText = ref('')
const { busy, error, run } = useForm()

const canPost = computed(() => auth.can('COMMUNITY_PARTICIPATE'))
const moderator = computed(() => auth.can('CONTENT_MODERATE'))
const canDelete = (c: Comment) => c.author.id === auth.me?.id || moderator.value

async function load() {
  loadError.value = ''
  try {
    comments.value = await listComments(props.recipeId)
  } catch (e) {
    loadError.value = messageFor(e)
  }
}
watch(() => props.recipeId, load, { immediate: true })

const send = (text: string, parentId: number | null, done: () => void) =>
  text.trim() && run(async () => { await postComment(props.recipeId, text.trim(), parentId); done(); await load() })
const sendRoot = () => send(content.value, null, () => (content.value = ''))
const sendReply = () => send(replyText.value, replyTo.value, () => { replyTo.value = null; replyText.value = '' })
const remove = (c: Comment) => confirm('Xóa bình luận này?') && run(async () => { await deleteComment(c.id); await load() })
const toggleHidden = (c: Comment) => run(async () => { await hideComment(c.id, !c.hidden); await load() })
</script>

<template>
  <section class="comments" aria-labelledby="cmt-title">
    <h2 id="cmt-title">Bình luận</h2>

    <form v-if="canPost" class="form" @submit.prevent="sendRoot">
      <label class="field">Viết bình luận
        <textarea v-model="content" rows="3" maxlength="1000"></textarea>
      </label>
      <p v-if="error && replyTo === null" class="form-error" role="alert">{{ error }}</p>
      <div><button class="btn primary" :disabled="busy || !content.trim()">Gửi bình luận</button></div>
    </form>
    <p v-else-if="!auth.me" class="hint"><RouterLink to="/login">Đăng nhập</RouterLink> để bình luận.</p>

    <p v-if="loadError" class="form-error">{{ loadError }}</p>
    <p v-else-if="!comments.length" class="hint">Chưa có bình luận nào.</p>

    <ul class="list">
      <li v-for="c in comments" :key="c.id">
        <template v-for="item in [c, ...c.replies]" :key="item.id">
          <div :class="['item', { reply: item !== c, hidden: item.hidden }]">
            <div class="meta">
              <strong>{{ item.author.displayName }}</strong>
              <span v-if="item.hidden" class="stamp" data-status="HIDDEN">Đã ẩn</span>
            </div>
            <p class="text">{{ item.content }}</p>
            <div class="actions">
              <button v-if="item === c && canPost" type="button" class="link-btn" @click="replyTo = c.id; replyText = ''">Trả lời</button>
              <button v-if="moderator" type="button" class="link-btn" :disabled="busy" @click="toggleHidden(item)">{{ item.hidden ? 'Hiện' : 'Ẩn' }}</button>
              <button v-if="canDelete(item)" type="button" class="link-btn danger" :disabled="busy" @click="remove(item)">Xóa</button>
            </div>
          </div>
        </template>
        <form v-if="replyTo === c.id" class="form reply-form" @submit.prevent="sendReply">
          <label class="field">Trả lời {{ c.author.displayName }}
            <textarea v-model="replyText" rows="2" maxlength="1000"></textarea>
          </label>
          <p v-if="error" class="form-error" role="alert">{{ error }}</p>
          <div class="row">
            <button class="btn primary" :disabled="busy || !replyText.trim()">Gửi trả lời</button>
            <button type="button" class="link-btn" @click="replyTo = null">Hủy</button>
          </div>
        </form>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.comments { margin-top: 2.5rem; }
h2 { font-weight: 800; font-size: 1.5rem; margin: 0 0 0.5rem; }
.row, .actions { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
.list { list-style: none; padding: 0; margin: 1rem 0 0; border-top: 1px solid var(--border); }
.list > li { padding: 0.6rem 0; border-bottom: 1px solid var(--border); }
.item.reply { margin: 0.5rem 0 0 1.5rem; padding-left: 0.75rem; border-left: 2px solid var(--border); }
.text { margin: 0.2rem 0; white-space: pre-line; }
.hidden .text { color: var(--muted); }
.reply-form { margin: 0.5rem 0 0 1.5rem; }
</style>
