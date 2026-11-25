<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="auth-card">
    <h1>403 — Không có quyền</h1>
    <p>Tài khoản của bạn không có quyền truy cập trang này.</p>
    <p><RouterLink to="/">Về trang chủ</RouterLink></p>
  </section>
</template>
