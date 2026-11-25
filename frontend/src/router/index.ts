import { createRouter, createWebHistory } from 'vue-router'
import PublicLayout from '@/layouts/PublicLayout.vue'
import HomePage from '@/pages/HomePage.vue'
import { useAuthStore } from '@/stores/auth'
import { guard } from './guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: PublicLayout,
      children: [
        { path: '', name: 'home', component: HomePage },
        { path: 'login', component: () => import('@/pages/LoginPage.vue') },
        { path: 'register', component: () => import('@/pages/RegisterPage.vue') },
        { path: 'verify-email', component: () => import('@/pages/VerifyEmailPage.vue') },
        { path: 'forgot-password', component: () => import('@/pages/ForgotPasswordPage.vue') },
        { path: 'reset-password', component: () => import('@/pages/ResetPasswordPage.vue') },
        { path: 'account', component: () => import('@/pages/AccountPage.vue'), meta: { auth: true } },
        { path: '403', component: () => import('@/pages/ForbiddenPage.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => guard(to, useAuthStore()))

export default router
