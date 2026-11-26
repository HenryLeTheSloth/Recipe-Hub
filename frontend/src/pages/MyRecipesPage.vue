<script setup lang="ts">
import { ref, watch } from 'vue'
import PlusIcon from '@primeicons/vue/plus'
import ChevronLeftIcon from '@primeicons/vue/chevron-left'
import ChevronRightIcon from '@primeicons/vue/chevron-right'
import { messageFor } from '@/api/messages'
import { deleteRecipe, listMyRecipes, STATUS_LABELS, type Page, type RecipeCard, type RecipeStatus } from '@/api/recipes'

const status = ref<RecipeStatus | ''>('')
const page = ref(0)
const result = ref<Page<RecipeCard> | null>(null)
const error = ref('')

async function load() {
  error.value = ''
  try {
    result.value = await listMyRecipes(status.value, page.value)
  } catch (e) {
    error.value = messageFor(e)
  }
}
watch(status, () => (page.value = 0))
watch([status, page], load, { immediate: true })

async function remove(r: RecipeCard) {
  if (!confirm(`Xóa “${r.title}”? Không hoàn tác được.`)) return
  try {
    await deleteRecipe(r.id)
    await load()
  } catch (e) {
    error.value = messageFor(e)
  }
}
</script>

<template>
  <section class="sheet mine" aria-labelledby="mine-title">
    <header class="head">
      <h1 id="mine-title">Công thức của tôi</h1>
      <RouterLink to="/me/recipes/new" class="btn primary"><PlusIcon aria-hidden="true" />Viết công thức</RouterLink>
    </header>
    <div class="tabs" role="tablist" aria-label="Lọc theo trạng thái">
      <button v-for="s in ['', 'DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'HIDDEN'] as const" :key="s" type="button" role="tab"
        :aria-selected="status === s" @click="status = s">
        {{ s ? STATUS_LABELS[s] : 'Tất cả' }}</button>
    </div>
    <p v-if="error" class="form-error">{{ error }}</p>
    <template v-if="result">
      <p v-if="!result.content.length" class="empty">
        Chưa có công thức nào ở đây. <RouterLink to="/me/recipes/new">Viết món đầu tiên</RouterLink> từ chính căn bếp nhà bạn.
      </p>
      <ul class="rows">
        <li v-for="r in result.content" :key="r.id">
          <RouterLink :to="`/recipes/${r.slug}`" class="title">{{ r.title }}</RouterLink>
          <span class="stamp" :data-status="r.status">{{ STATUS_LABELS[r.status] }}</span>
          <span class="actions">
            <RouterLink v-if="r.status !== 'PENDING' && r.status !== 'HIDDEN'" :to="`/me/recipes/${r.id}/edit`" class="act">Sửa</RouterLink>
            <button type="button" class="link-btn danger" @click="remove(r)">Xóa</button>
          </span>
        </li>
      </ul>
      <nav v-if="result.page.totalPages > 1" class="pager" aria-label="Phân trang">
        <button class="btn" :disabled="page === 0" @click="page--"><ChevronLeftIcon aria-hidden="true" />Trước</button>
        <span class="num">Trang {{ page + 1 }} / {{ result.page.totalPages }}</span>
        <button class="btn" :disabled="page + 1 >= result.page.totalPages" @click="page++">Sau<ChevronRightIcon aria-hidden="true" /></button>
      </nav>
    </template>
  </section>
</template>

<style scoped>
.mine { max-width: 900px; margin: 0.5rem auto 0; }
.head { display: flex; justify-content: space-between; align-items: center; gap: 0.5rem 1rem; flex-wrap: wrap; }
.head .btn svg, .pager .btn svg { width: 0.85rem; height: 0.85rem; }
.tabs { display: flex; gap: 0.4rem; overflow-x: auto; scrollbar-width: none; margin-bottom: 0.75rem; }
.tabs button {
  flex: none; min-height: 44px; padding: 0.4rem 1rem; font: inherit; font-weight: 600; color: var(--accent-deep);
  background: var(--surface); border: 1px solid var(--border); border-radius: 999px; cursor: pointer;
}
.tabs button:hover { border-color: var(--accent); background: var(--sage); }
.tabs button[aria-selected='true'] { background: var(--accent); border-color: var(--accent); color: var(--accent-text); }
.empty { margin: 1.5rem 0; color: var(--muted); }
.rows { list-style: none; padding: 0; margin: 0; }
.rows li { display: flex; flex-wrap: wrap; gap: 0.25rem 1rem; align-items: center; padding: 0.5rem 0; border-bottom: 1px solid var(--border); }
.title { flex: 1 1 240px; color: var(--text); font-weight: 600; font-size: 1.05rem; text-decoration: none; padding: 0.5rem 0; }
.title:hover { color: var(--accent); }
.actions { display: flex; gap: 1rem; }
.act { display: inline-flex; align-items: center; min-height: 44px; }
</style>
