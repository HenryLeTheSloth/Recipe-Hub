<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { resetPassword } from '@/api/auth'
import { useForm } from '@/composables/useForm'

const route = useRoute()
const token = typeof route.query.token === 'string' ? route.query.token : ''
const password = ref('')
const confirm = ref('')
const { busy, error, fieldErrors, run } = useForm()
const done = ref(false)

async function submit() {
  if (password.value !== confirm.value) {
    error.value = 'Mật khẩu nhập lại không khớp'
    return
  }
  done.value = await run(() => resetPassword(token, password.value))
}
</script>

<template>
  <section class="auth-card">
    <h1>Đặt lại mật khẩu</h1>
    <p v-if="!token" class="form-error">Liên kết thiếu mã. <RouterLink to="/forgot-password">Yêu cầu lại</RouterLink></p>
    <template v-else-if="done">
      <p class="form-ok">Đã đặt lại mật khẩu. Mọi phiên đăng nhập cũ đã bị đăng xuất.</p>
      <p><RouterLink to="/login">Đăng nhập</RouterLink></p>
    </template>
    <form v-else class="form" novalidate @submit.prevent="submit">
      <label class="field">
        Mật khẩu mới
        <input v-model="password" type="password" autocomplete="new-password" required />
        <small v-if="fieldErrors.newPassword" class="field-error">{{ fieldErrors.newPassword }}</small>
        <small v-else class="hint">Ít nhất 8 ký tự</small>
      </label>
      <label class="field">
        Nhập lại mật khẩu
        <input v-model="confirm" type="password" autocomplete="new-password" required />
      </label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button type="submit" class="btn primary" :disabled="busy">Đặt lại mật khẩu</button>
    </form>
  </section>
</template>
