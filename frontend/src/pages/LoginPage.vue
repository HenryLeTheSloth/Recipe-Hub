<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { resendVerification } from '@/api/auth'
import { useForm } from '@/composables/useForm'
import { useAuthStore } from '@/stores/auth'
import { safeRedirect } from '@/utils/safeRedirect'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const email = ref('')
const password = ref('')
const { busy, error, code, fieldErrors, run } = useForm()
const resend = useForm()
const resent = ref(false)

async function submit() {
  resent.value = false
  if (await run(() => auth.login(email.value, password.value))) {
    await router.push(safeRedirect(route.query.redirect))
  }
}

async function resendMail() {
  resent.value = await resend.run(() => resendVerification(email.value))
}
</script>

<template>
  <section class="auth-card">
    <h1>Đăng nhập</h1>
    <form class="form" novalidate @submit.prevent="submit">
      <label class="field">
        Email
        <input v-model.trim="email" type="email" autocomplete="email" required />
        <small v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</small>
      </label>
      <label class="field">
        Mật khẩu
        <input v-model="password" type="password" autocomplete="current-password" required />
        <small v-if="fieldErrors.password" class="field-error">{{ fieldErrors.password }}</small>
      </label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <template v-if="code === 'EMAIL_NOT_VERIFIED'">
        <button type="button" class="btn" :disabled="resend.busy.value" @click="resendMail">Gửi lại email xác thực</button>
        <p v-if="resent" class="form-ok">Đã gửi lại, hãy kiểm tra hộp thư.</p>
        <p v-if="resend.error.value" class="form-error">{{ resend.error.value }}</p>
      </template>
      <button type="submit" class="btn primary" :disabled="busy">{{ busy ? 'Đang đăng nhập…' : 'Đăng nhập' }}</button>
    </form>
    <p class="links">
      <RouterLink to="/forgot-password">Quên mật khẩu?</RouterLink>
      <RouterLink to="/register">Tạo tài khoản</RouterLink>
    </p>
  </section>
</template>
