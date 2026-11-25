<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ApiError } from '@/api/http'
import { DIFFICULTY_LABELS, getRecipe, STATUS_LABELS, type RecipeDetail } from '@/api/recipes'
import { formatQuantity } from '@/utils/units'

const route = useRoute()
const recipe = ref<RecipeDetail | null>(null)
const error = ref('')

watch(
  () => route.params.slug as string,
  async (slug) => {
    recipe.value = null
    error.value = ''
    try {
      recipe.value = await getRecipe(slug)
    } catch (e) {
      error.value = e instanceof ApiError && e.status === 404 ? 'Không tìm thấy công thức' : 'Không tải được công thức'
    }
  },
  { immediate: true },
)
</script>

<template>
  <p v-if="error" class="form-error">{{ error }}</p>
  <article v-else-if="recipe" class="recipe">
    <!-- API chỉ trả bản chưa đăng cho tác giả hoặc người kiểm duyệt. -->
    <div v-if="recipe.status !== 'PUBLISHED'" class="notice">
      <strong>{{ STATUS_LABELS[recipe.status] }}</strong>
      <span v-if="recipe.rejectionReason"> — Lý do: {{ recipe.rejectionReason }}</span>
    </div>
    <img v-if="recipe.coverUrl" :src="recipe.coverUrl" alt="" class="cover" />
    <h1>{{ recipe.title }}</h1>
    <p v-if="recipe.summary" class="summary">{{ recipe.summary }}</p>
    <p class="meta">
      <RouterLink v-if="recipe.category" :to="{ path: '/recipes', query: { category: recipe.category.slug } }">
        {{ recipe.category.name }}</RouterLink>
      · {{ recipe.servings }} người · Chuẩn bị {{ recipe.prepMinutes }} phút · Nấu {{ recipe.cookMinutes }} phút
      · {{ DIFFICULTY_LABELS[recipe.difficulty] }} · bởi {{ recipe.author.displayName }}
    </p>
    <p v-if="recipe.tags.length" class="tags">
      <RouterLink v-for="t in recipe.tags" :key="t.id" :to="{ path: '/recipes', query: { tag: t.slug } }">
        #{{ t.name }}</RouterLink>
    </p>
    <div class="cols">
      <section>
        <h2>Nguyên liệu</h2>
        <ul class="ingredients">
          <li v-for="i in recipe.ingredients" :key="i.ingredientId">
            <span>{{ i.emoji }} {{ i.name }}<small v-if="i.note"> ({{ i.note }})</small></span>
            <span>{{ formatQuantity(i.quantity, i.unit) }}</span>
          </li>
        </ul>
      </section>
      <section>
        <h2>Cách làm</h2>
        <ol class="steps">
          <li v-for="s in recipe.steps" :key="s.stepNo">
            <p>{{ s.content }}</p>
            <img v-if="s.imageUrl" :src="s.imageUrl" alt="" loading="lazy" />
          </li>
        </ol>
      </section>
    </div>
  </article>
</template>

<style scoped>
.recipe { max-width: 960px; margin: 1.5rem auto; }
.notice { padding: 0.75rem 1rem; border: 1px solid var(--danger); border-radius: var(--radius); margin-bottom: 1rem; }
.cover { width: 100%; max-height: 420px; object-fit: cover; border-radius: var(--radius); margin-bottom: 1rem; }
.summary { font-size: 1.05rem; }
.meta { color: var(--muted); }
.tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.cols { display: grid; gap: 1.5rem; }
@media (min-width: 768px) { .cols { grid-template-columns: 1fr 2fr; } }
.ingredients { list-style: none; padding: 0; margin: 0; }
.ingredients li { display: flex; justify-content: space-between; gap: 1rem; padding: 0.45rem 0; border-bottom: 1px solid var(--border); }
.steps { padding-left: 1.25rem; }
.steps li { margin-bottom: 1rem; }
.steps p { margin: 0 0 0.5rem; }
.steps img { max-width: 100%; border-radius: var(--radius); }
</style>
