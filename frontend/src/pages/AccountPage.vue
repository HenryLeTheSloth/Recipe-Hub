<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '@/api/auth'
import { setAvatar, type MediaRef } from '@/api/media'
import ImageUpload from '@/components/ImageUpload.vue'
import { useForm } from '@/composables/useForm'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const current = ref('')
const next = ref('')
const confirm = ref('')
const { busy, error, fieldErrors, run } = useForm()
const done = ref(false)

// Server chỉ trả avatarUrl; id chỉ để thỏa kiểu MediaRef, ImageUpload chỉ đọc url.
const avatar = computed<MediaRef | null>(() => (auth.me?.avatarUrl ? { id: 0, url: auth.me.avatarUrl } : null))
const avatarForm = useForm()
async function changeAvatar(m: MediaRef) {
  await avatarForm.run(async () => (auth.me = await setAvatar(m.id)))
}

async function submit() {
  done.value = false
  if (next.value !== confirm.value) {
    error.value = 'Mật khẩu nhập lại không khớp'
    return
  }
  done.value = await run(() => changePassword(current.value, next.value))
  if (done.value) current.value = next.value = confirm.value = ''
}

async function logout() {
  await auth.logout().catch(() => {})
  await router.push('/')
}
</script>

<template>
  <section v-if="auth.me" class="auth-card">
    <h1>Tài khoản</h1>
    <dl class="info">
      <dt>Tên hiển thị</dt><dd>{{ auth.me.displayName }}</dd>
      <dt>Tên đăng nhập</dt><dd>@{{ auth.me.username }}</dd>
      <dt>Email</dt><dd>{{ auth.me.email }}</dd>
      <dt>Vai trò</dt><dd>{{ auth.me.roles.join(', ') }}</dd>
    </dl>

    <h2>Ảnh đại diện</h2>
    <div class="avatar">
      <ImageUpload :model-value="avatar" @update:model-value="(m) => m && changeAvatar(m)" />
      <p v-if="avatarForm.error.value" class="form-error" role="alert">{{ avatarForm.error.value }}</p>
    </div>

    <h2>Đổi mật khẩu</h2>
    <form class="form" novalidate @submit.prevent="submit">
      <label class="field">
        Mật khẩu hiện tại
        <input v-model="current" type="password" autocomplete="current-password" required />
        <small v-if="fieldErrors.currentPassword" class="field-error">{{ fieldErrors.currentPassword }}</small>
      </label>
      <label class="field">
        Mật khẩu mới
        <input v-model="next" type="password" autocomplete="new-password" required />
        <small v-if="fieldErrors.newPassword" class="field-error">{{ fieldErrors.newPassword }}</small>
      </label>
      <label class="field">
        Nhập lại mật khẩu mới
        <input v-model="confirm" type="password" autocomplete="new-password" required />
      </label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <p v-if="done" class="form-ok">Đã đổi mật khẩu.</p>
      <button type="submit" class="btn primary" :disabled="busy">Đổi mật khẩu</button>
    </form>

    <p class="links"><button type="button" class="btn" @click="logout">Đăng xuất</button></p>
  </section>
</template>

<style scoped>
h2 { font-size: 1.1rem; margin: 1.5rem 0 0.75rem; }
.info { display: grid; grid-template-columns: auto 1fr; gap: 0.35rem 1rem; margin: 0; }
.info dt { color: var(--muted); }
.info dd { margin: 0; overflow-wrap: anywhere; }
.avatar :deep(.preview) { border-radius: 50%; }
</style>
