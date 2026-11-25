<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { register } from '@/api/auth'
import { useForm } from '@/composables/useForm'

const form = reactive({ email: '', username: '', displayName: '', password: '' })
const { busy, error, fieldErrors, run } = useForm()
const done = ref(false)

async function submit() {
  done.value = await run(() => register(form))
}
</script>

<template>
  <section class="auth-card">
    <h1>Tạo tài khoản</h1>
    <div v-if="done">
      <p class="form-ok">Đã gửi email xác thực tới <strong>{{ form.email }}</strong>.</p>
      <p>Mở liên kết trong email (hiệu lực 24 giờ) rồi <RouterLink to="/login">đăng nhập</RouterLink>.</p>
    </div>
    <form v-else class="form" novalidate @submit.prevent="submit">
      <label class="field">
        Email
        <input v-model.trim="form.email" type="email" autocomplete="email" required />
        <small v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</small>
      </label>
      <label class="field">
        Tên đăng nhập
        <input v-model.trim="form.username" autocomplete="username" required maxlength="30" />
        <small v-if="fieldErrors.username" class="field-error">{{ fieldErrors.username }}</small>
        <small v-else class="hint">3–30 ký tự: chữ, số hoặc _</small>
      </label>
      <label class="field">
        Tên hiển thị
        <input v-model="form.displayName" autocomplete="nickname" required maxlength="50" />
        <small v-if="fieldErrors.displayName" class="field-error">{{ fieldErrors.displayName }}</small>
      </label>
      <label class="field">
        Mật khẩu
        <input v-model="form.password" type="password" autocomplete="new-password" required />
        <small v-if="fieldErrors.password" class="field-error">{{ fieldErrors.password }}</small>
        <small v-else class="hint">Ít nhất 8 ký tự</small>
      </label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button type="submit" class="btn primary" :disabled="busy">{{ busy ? 'Đang tạo…' : 'Tạo tài khoản' }}</button>
    </form>
    <p class="links">Đã có tài khoản? <RouterLink to="/login">Đăng nhập</RouterLink></p>
  </section>
</template>
