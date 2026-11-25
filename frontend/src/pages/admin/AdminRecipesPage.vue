<script setup lang="ts">
import { ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import { messageFor } from '@/api/messages'
import {
  adminGetRecipe, adminListRecipes, moderateRecipe, STATUS_LABELS,
  type ModerationAction, type Page, type RecipeCard, type RecipeDetail, type RecipeStatus,
} from '@/api/recipes'
import { formatQuantity } from '@/utils/units'

const TABS: RecipeStatus[] = ['PENDING', 'PUBLISHED', 'HIDDEN']
const status = ref<RecipeStatus>('PENDING')
const page = ref(0)
const result = ref<Page<RecipeCard> | null>(null)
const error = ref('')
const preview = ref<RecipeDetail | null>(null)
const asking = ref<{ recipe: RecipeCard; action: 'reject' | 'hide' } | null>(null)
const reason = ref('')

async function load() {
  error.value = ''
  try {
    result.value = await adminListRecipes(status.value, page.value)
  } catch (e) {
    error.value = messageFor(e)
  }
}
watch(status, () => (page.value = 0))
watch([status, page], load, { immediate: true })

async function open(r: RecipeCard) {
  try {
    preview.value = await adminGetRecipe(r.id)
  } catch (e) {
    error.value = messageFor(e)
  }
}

async function act(r: RecipeCard, action: ModerationAction, why?: string) {
  error.value = ''
  try {
    await moderateRecipe(r.id, action, why)
    asking.value = null
    preview.value = null
    await load()
  } catch (e) {
    error.value = messageFor(e)
  }
}

function ask(recipe: RecipeCard, action: 'reject' | 'hide') {
  reason.value = ''
  asking.value = { recipe, action }
}
</script>

<template>
  <div>
    <h1>Kiểm duyệt công thức</h1>
    <div class="tabs" role="tablist">
      <button v-for="s in TABS" :key="s" type="button" role="tab" :aria-selected="status === s"
        :class="{ active: status === s }" @click="status = s">{{ STATUS_LABELS[s] }}</button>
    </div>
    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <template v-if="result">
      <p v-if="!result.content.length" class="hint">Không có công thức nào.</p>
      <ul class="rows">
        <li v-for="r in result.content" :key="r.id">
          <button type="button" class="link title" @click="open(r)">{{ r.title }}</button>
          <span class="hint">{{ r.authorName }} · {{ r.categoryName ?? '—' }}</span>
          <span class="actions">
            <template v-if="r.status === 'PENDING'">
              <button type="button" class="btn primary" @click="act(r, 'approve')">Duyệt</button>
              <button type="button" class="btn" @click="ask(r, 'reject')">Từ chối</button>
            </template>
            <button v-else-if="r.status === 'PUBLISHED'" type="button" class="btn" @click="ask(r, 'hide')">Ẩn</button>
            <button v-else-if="r.status === 'HIDDEN'" type="button" class="btn" @click="act(r, 'unhide')">Hiện lại</button>
          </span>
        </li>
      </ul>
      <nav v-if="result.page.totalPages > 1" class="pager">
        <button class="btn" :disabled="page === 0" @click="page--">‹ Trước</button>
        <span>Trang {{ page + 1 }} / {{ result.page.totalPages }}</span>
        <button class="btn" :disabled="page + 1 >= result.page.totalPages" @click="page++">Sau ›</button>
      </nav>
    </template>

    <Dialog :visible="!!preview" modal :header="preview?.title" :style="{ width: 'min(720px, 95vw)' }"
      @update:visible="(v: boolean) => { if (!v) preview = null }">
      <template v-if="preview">
        <img v-if="preview.coverUrl" :src="preview.coverUrl" alt="" class="cover" />
        <p v-if="preview.summary">{{ preview.summary }}</p>
        <p class="hint">{{ preview.author.displayName }} · {{ preview.category?.name ?? 'Chưa có danh mục' }}
          · {{ preview.servings }} người · {{ preview.prepMinutes + preview.cookMinutes }} phút</p>
        <h3>Nguyên liệu</h3>
        <ul>
          <li v-for="i in preview.ingredients" :key="i.ingredientId">
            {{ i.emoji }} {{ i.name }} — {{ formatQuantity(i.quantity, i.unit) }}<span v-if="i.note"> ({{ i.note }})</span>
          </li>
        </ul>
        <h3>Các bước</h3>
        <ol>
          <li v-for="s in preview.steps" :key="s.stepNo">
            {{ s.content }}
            <img v-if="s.imageUrl" :src="s.imageUrl" alt="" class="step-img" />
          </li>
        </ol>
      </template>
    </Dialog>

    <Dialog :visible="!!asking" modal :header="asking?.action === 'reject' ? 'Từ chối công thức' : 'Ẩn công thức'"
      :style="{ width: 'min(480px, 95vw)' }" @update:visible="(v: boolean) => { if (!v) asking = null }">
      <form v-if="asking" class="form" @submit.prevent="act(asking.recipe, asking.action, reason.trim())">
        <p>“{{ asking.recipe.title }}”</p>
        <label class="field">Lý do (tác giả sẽ thấy)
          <textarea v-model="reason" rows="3" maxlength="500" required />
        </label>
        <button class="btn primary" :disabled="!reason.trim()">Xác nhận</button>
      </form>
    </Dialog>
  </div>
</template>

<style scoped>
.tabs { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-bottom: 1rem; }
.tabs button { font: inherit; padding: 0.35rem 0.8rem; border: 1px solid var(--border); border-radius: 999px; background: var(--surface); color: var(--text); cursor: pointer; }
.tabs button.active { border-color: var(--accent); color: var(--accent); font-weight: 600; }
.rows { list-style: none; padding: 0; margin: 0; }
.rows li { display: flex; flex-wrap: wrap; gap: 0.5rem 1rem; align-items: center; padding: 0.7rem 0; border-bottom: 1px solid var(--border); }
.title { flex: 1 1 220px; text-align: left; font-weight: 600; }
.link { font: inherit; background: none; border: 0; padding: 0; cursor: pointer; color: var(--text); }
.actions { display: flex; gap: 0.5rem; }
.actions .btn { padding: 0.4rem 0.8rem; }
.pager { display: flex; align-items: center; justify-content: center; gap: 1rem; margin: 1.5rem 0; }
.cover { width: 100%; max-height: 280px; object-fit: cover; border-radius: var(--radius); }
.step-img { display: block; max-width: 240px; margin-top: 0.4rem; border-radius: 6px; }
textarea { font: inherit; padding: 0.55rem 0.7rem; border: 1px solid var(--border); border-radius: 8px; background: var(--bg); color: var(--text); }
</style>
