<script setup lang="ts">
import { ref, watch } from 'vue'
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
  <div class="mine">
    <div class="head">
      <h1>Công thức của tôi</h1>
      <RouterLink to="/me/recipes/new" class="btn primary">+ Viết công thức</RouterLink>
    </div>
    <div class="tabs" role="tablist">
      <button v-for="s in ['', 'DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'HIDDEN'] as const" :key="s" type="button" role="tab"
        :aria-selected="status === s" :class="{ active: status === s }" @click="status = s">
        {{ s ? STATUS_LABELS[s] : 'Tất cả' }}</button>
    </div>
    <p v-if="error" class="form-error">{{ error }}</p>
    <template v-if="result">
      <p v-if="!result.content.length" class="hint">Chưa có công thức nào.</p>
      <ul class="rows">
        <li v-for="r in result.content" :key="r.id">
          <RouterLink :to="`/recipes/${r.slug}`" class="title">{{ r.title }}</RouterLink>
          <span class="status" :data-status="r.status">{{ STATUS_LABELS[r.status] }}</span>
          <span class="actions">
            <RouterLink v-if="r.status !== 'PENDING' && r.status !== 'HIDDEN'" :to="`/me/recipes/${r.id}/edit`">Sửa</RouterLink>
            <button type="button" class="link danger" @click="remove(r)">Xóa</button>
          </span>
        </li>
      </ul>
      <nav v-if="result.page.totalPages > 1" class="pager">
        <button class="btn" :disabled="page === 0" @click="page--">‹ Trước</button>
        <span>Trang {{ page + 1 }} / {{ result.page.totalPages }}</span>
        <button class="btn" :disabled="page + 1 >= result.page.totalPages" @click="page++">Sau ›</button>
      </nav>
    </template>
  </div>
</template>

<style scoped>
.mine { max-width: 860px; margin: 1.5rem auto; }
.head { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; }
.head .btn { text-decoration: none; }
.tabs { display: flex; flex-wrap: wrap; gap: 0.4rem; margin: 0.5rem 0 1rem; }
.tabs button { font: inherit; padding: 0.35rem 0.8rem; border: 1px solid var(--border); border-radius: 999px; background: var(--surface); color: var(--text); cursor: pointer; }
.tabs button.active { border-color: var(--accent); color: var(--accent); font-weight: 600; }
.rows { list-style: none; padding: 0; margin: 0; }
.rows li { display: flex; flex-wrap: wrap; gap: 0.5rem 1rem; align-items: center; padding: 0.7rem 0; border-bottom: 1px solid var(--border); }
.title { flex: 1 1 240px; color: var(--text); font-weight: 600; text-decoration: none; }
.status { font-size: 0.85rem; color: var(--muted); }
.status[data-status='REJECTED'], .status[data-status='HIDDEN'] { color: var(--danger); }
.status[data-status='PUBLISHED'] { color: var(--ok); }
.actions { display: flex; gap: 0.75rem; }
.link { font: inherit; background: none; border: 0; padding: 0; cursor: pointer; color: var(--accent); }
.link.danger { color: var(--danger); }
.pager { display: flex; align-items: center; justify-content: center; gap: 1rem; margin: 1.5rem 0; }
</style>
