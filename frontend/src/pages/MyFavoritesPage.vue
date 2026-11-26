<script setup lang="ts">
import { ref, watch } from 'vue'
import ChevronLeftIcon from '@primeicons/vue/chevron-left'
import ChevronRightIcon from '@primeicons/vue/chevron-right'
import { listFavorites } from '@/api/community'
import { messageFor } from '@/api/messages'
import type { Page, RecipeCard as Card } from '@/api/recipes'
import RecipeCard from '@/components/RecipeCard.vue'

const page = ref(0)
const result = ref<Page<Card> | null>(null)
const error = ref('')

watch(page, async (p) => {
  error.value = ''
  try {
    result.value = await listFavorites(p)
  } catch (e) {
    error.value = messageFor(e)
  }
}, { immediate: true })
</script>

<template>
  <section class="sheet" aria-labelledby="fav-title">
    <h1 id="fav-title">Món yêu thích</h1>
    <p v-if="error" class="form-error">{{ error }}</p>
    <template v-else-if="result">
      <p v-if="!result.content.length" class="empty">
        Chưa có món nào. Bấm “Yêu thích” ở một công thức để giữ nó lại đây, hoặc <RouterLink to="/recipes">xem các công thức</RouterLink>.
      </p>
      <div class="grid">
        <RecipeCard v-for="r in result.content" :key="r.id" :recipe="r" />
      </div>
      <nav v-if="result.page.totalPages > 1" class="pager" aria-label="Phân trang">
        <button class="btn" :disabled="page === 0" @click="page--"><ChevronLeftIcon aria-hidden="true" />Trước</button>
        <span class="num">Trang {{ page + 1 }} / {{ result.page.totalPages }}</span>
        <button class="btn" :disabled="page + 1 >= result.page.totalPages" @click="page++">Sau<ChevronRightIcon aria-hidden="true" /></button>
      </nav>
    </template>
  </section>
</template>

<style scoped>
.grid { display: grid; gap: 1.5rem 1.25rem; margin-top: 1.5rem; grid-template-columns: repeat(auto-fill, minmax(min(100%, 15rem), 1fr)); }
.empty { color: var(--muted); }
.pager .btn svg { width: 0.85rem; height: 0.85rem; }
</style>
