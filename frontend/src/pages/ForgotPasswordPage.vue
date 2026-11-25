<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { forgotPassword } from '@/api/auth'
import { useForm } from '@/composables/useForm'

const email = ref('')
const { busy, error, fieldErrors, run } = useForm()
const done = ref(false)

async function submit() {
  done.value = await run(() => forgotPassword(email.value))
}
</script>

<template>
  <section class="auth-card">
    <h1>Quên mật khẩu</h1>
    <p v-if="done" class="form-ok">
      Nếu <strong>{{ email }}</strong> có tài khoản, chúng tôi đã gửi liên kết đặt lại mật khẩu (hiệu lực 30 phút).
    </p>
    <form v-else class="form" novalidate @submit.prevent="submit">
      <label class="field">
        Email
        <input v-model.trim="email" type="email" autocomplete="email" required />
        <small v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</small>
      </label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button type="submit" class="btn primary" :disabled="busy">Gửi liên kết</button>
    </form>
    <p class="links"><RouterLink to="/login">Quay lại đăng nhập</RouterLink></p>
  </section>
</template>
