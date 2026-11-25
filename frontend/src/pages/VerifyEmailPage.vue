<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { verifyEmail } from '@/api/auth'
import { useForm } from '@/composables/useForm'

const route = useRoute()
const { busy, error, run } = useForm()
const done = ref(false)

onMounted(async () => {
  const token = route.query.token
  if (typeof token !== 'string' || !token) {
    error.value = 'Liên kết xác thực thiếu mã.'
    return
  }
  done.value = await run(() => verifyEmail(token))
})
</script>

<template>
  <section class="auth-card">
    <h1>Xác thực email</h1>
    <p v-if="busy">Đang xác thực…</p>
    <template v-else-if="done">
      <p class="form-ok">Email đã được xác thực.</p>
      <p><RouterLink to="/login">Đăng nhập ngay</RouterLink></p>
    </template>
    <template v-else-if="error">
      <p class="form-error" role="alert">{{ error }}</p>
      <p class="links">Đăng nhập để yêu cầu gửi lại email xác thực. <RouterLink to="/login">Đăng nhập</RouterLink></p>
    </template>
  </section>
</template>
