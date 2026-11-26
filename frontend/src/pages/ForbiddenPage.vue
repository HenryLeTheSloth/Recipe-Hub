<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="auth-card denied">
    <p class="code num" aria-hidden="true">403</p>
    <h1>Không có quyền</h1>
    <p>Tài khoản của bạn không được mở trang này. Nếu cần, hãy nhờ quản trị viên cấp thêm quyền.</p>
    <p><RouterLink to="/" class="btn">Về trang chủ</RouterLink></p>
  </section>
</template>

<style scoped>
.code { margin: 0.5rem 0 0; font-family: var(--font-display); font-weight: 900; font-size: 8rem; line-height: 0.8; color: var(--red); }
</style>
