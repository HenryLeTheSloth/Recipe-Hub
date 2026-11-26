<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addFavorite, removeFavorite } from '@/api/community'
import { messageFor } from '@/api/messages'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ recipeId: number; favorited: boolean }>()
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const on = ref(props.favorited)
const error = ref('')
const pending = ref(false)
watch(() => props.favorited, (v) => (on.value = v))

async function toggle() {
  if (!auth.me) return router.push('/login?redirect=' + encodeURIComponent(route.fullPath))
  if (pending.value) return
  pending.value = true
  const before = on.value
  on.value = !before // lạc quan; lỗi thì trả về trạng thái cũ
  error.value = ''
  try {
    await (before ? removeFavorite : addFavorite)(props.recipeId)
  } catch (e) {
    on.value = before
    error.value = messageFor(e)
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <span class="fav">
    <button type="button" class="btn" :aria-pressed="on" @click="toggle">
      <span aria-hidden="true">{{ on ? '♥' : '♡' }}</span> {{ on ? 'Đã thích' : 'Yêu thích' }}
    </button>
    <span v-if="error" role="alert" class="form-error">{{ error }}</span>
  </span>
</template>

<style scoped>
.fav { display: inline-flex; align-items: center; gap: 0.75rem; }
.btn[aria-pressed='true'] { color: var(--accent-deep); background: var(--lime-soft); }
</style>
