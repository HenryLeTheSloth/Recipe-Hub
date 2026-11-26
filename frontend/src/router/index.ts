import { createRouter, createWebHistory } from 'vue-router'
import PublicLayout from '@/layouts/PublicLayout.vue'
import HomePage from '@/pages/HomePage.vue'
import { useAuthStore } from '@/stores/auth'
import { ADMIN_PERMISSIONS, guard } from './guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: PublicLayout,
      children: [
        { path: '', name: 'home', component: HomePage },
        { path: 'recipes', component: () => import('@/pages/RecipeListPage.vue') },
        { path: 'recipes/:slug', component: () => import('@/pages/RecipeDetailPage.vue') },
        { path: 'me/recipes', component: () => import('@/pages/MyRecipesPage.vue'), meta: { permission: 'RECIPE_WRITE' } },
        { path: 'me/recipes/new', component: () => import('@/pages/RecipeEditorPage.vue'), meta: { permission: 'RECIPE_WRITE' } },
        { path: 'me/recipes/:id(\\d+)/edit', component: () => import('@/pages/RecipeEditorPage.vue'),
          meta: { permission: 'RECIPE_WRITE' } },
        { path: 'me/favorites', component: () => import('@/pages/MyFavoritesPage.vue'), meta: { auth: true } },
        { path: 'login', component: () => import('@/pages/LoginPage.vue') },
        { path: 'register', component: () => import('@/pages/RegisterPage.vue') },
        { path: 'verify-email', component: () => import('@/pages/VerifyEmailPage.vue') },
        { path: 'forgot-password', component: () => import('@/pages/ForgotPasswordPage.vue') },
        { path: 'reset-password', component: () => import('@/pages/ResetPasswordPage.vue') },
        { path: 'account', component: () => import('@/pages/AccountPage.vue'), meta: { auth: true } },
        { path: '403', component: () => import('@/pages/ForbiddenPage.vue') },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { auth: true, permission: ADMIN_PERMISSIONS },
      children: [
        { path: 'users', component: () => import('@/pages/admin/AdminUsersPage.vue'), meta: { permission: 'USER_READ' } },
        { path: 'roles', component: () => import('@/pages/admin/AdminRolesPage.vue'), meta: { permission: 'ROLE_MANAGE' } },
        { path: 'categories', component: () => import('@/pages/admin/AdminCatalogPage.vue'),
          props: { kind: 'categories' }, meta: { permission: 'CATALOG_MANAGE' } },
        { path: 'tags', component: () => import('@/pages/admin/AdminCatalogPage.vue'),
          props: { kind: 'tags' }, meta: { permission: 'CATALOG_MANAGE' } },
        { path: 'ingredients', component: () => import('@/pages/admin/AdminIngredientsPage.vue'),
          meta: { permission: 'CATALOG_MANAGE' } },
        { path: 'recipes', component: () => import('@/pages/admin/AdminRecipesPage.vue'),
          meta: { permission: 'CONTENT_MODERATE' } },
      ],
    },
  ],
})

router.beforeEach((to) => guard(to, useAuthStore()))

export default router
