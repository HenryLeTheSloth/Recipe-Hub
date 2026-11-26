<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ApiError } from '@/api/http'
import { DIFFICULTY_LABELS, getRecipe, STATUS_LABELS, type RecipeDetail } from '@/api/recipes'
import CheckIcon from '@primeicons/vue/check'
import { formatQuantity } from '@/utils/units'
import CommentSection from '@/components/CommentSection.vue'
import FavoriteButton from '@/components/FavoriteButton.vue'
import RatingSection from '@/components/RatingSection.vue'
import StarRating from '@/components/StarRating.vue'

const route = useRoute()
const recipe = ref<RecipeDetail | null>(null)
const error = ref('')
// Đánh dấu khi đang nấu: chỉ sống trong phiên xem, không lưu đi đâu.
const done = reactive({ ingredients: new Set<number>(), steps: new Set<number>() })
const flip = (set: Set<number>, id: number) => (set.has(id) ? set.delete(id) : set.add(id))

// Tải lại để điểm trung bình khớp sau khi chấm, xóa hoặc ẩn đánh giá.
async function refresh() {
  try { recipe.value = await getRecipe(route.params.slug as string) } catch { /* giữ bản đang xem */ }
}

watch(
  () => route.params.slug as string,
  async (slug) => {
    recipe.value = null
    error.value = ''
    done.ingredients.clear()
    done.steps.clear()
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
  <article v-else-if="recipe" class="sheet recipe">
    <!-- API chỉ trả bản chưa đăng cho tác giả hoặc người kiểm duyệt. -->
    <div v-if="recipe.status !== 'PUBLISHED'" class="notice">
      <span class="stamp" :data-status="recipe.status">{{ STATUS_LABELS[recipe.status] }}</span>
      <p v-if="recipe.rejectionReason">Lý do: {{ recipe.rejectionReason }}</p>
    </div>

    <header class="head">
      <h1>{{ recipe.title }}</h1>
      <p v-if="recipe.summary" class="summary">{{ recipe.summary }}</p>
      <p class="by">
        bởi {{ recipe.author.displayName }}
        <template v-if="recipe.category">trong
          <RouterLink :to="{ path: '/recipes', query: { category: recipe.category.slug } }">{{ recipe.category.name }}</RouterLink>
        </template>
      </p>
      <p v-if="recipe.status === 'PUBLISHED'" class="social">
        <template v-if="recipe.ratingCount && recipe.avgRating != null">
          <StarRating :model-value="Math.round(recipe.avgRating)" readonly />
          <span class="num"><strong>{{ recipe.avgRating.toFixed(1) }}</strong> ({{ recipe.ratingCount }} lượt)</span>
        </template>
        <span v-else class="by">Chưa có đánh giá</span>
        <FavoriteButton :recipe-id="recipe.id" :favorited="recipe.favorited" />
      </p>
    </header>

    <img v-if="recipe.coverUrl" :src="recipe.coverUrl" alt="" class="cover" />

    <dl class="facts">
      <div><dt>Khẩu phần</dt><dd><span class="num">{{ recipe.servings }}</span> người</dd></div>
      <div><dt>Chuẩn bị</dt><dd><span class="num">{{ recipe.prepMinutes }}</span> phút</dd></div>
      <div><dt>Nấu</dt><dd><span class="num">{{ recipe.cookMinutes }}</span> phút</dd></div>
      <div><dt>Độ khó</dt><dd>{{ DIFFICULTY_LABELS[recipe.difficulty] }}</dd></div>
    </dl>

    <p v-if="recipe.tags.length" class="tags">
      <RouterLink v-for="t in recipe.tags" :key="t.id" :to="{ path: '/recipes', query: { tag: t.slug } }">#{{ t.name }}</RouterLink>
    </p>

    <div class="cols">
      <section aria-labelledby="ing-title">
        <h2 id="ing-title">Nguyên liệu</h2>
        <p class="hint">Chạm để gạch những thứ đã chuẩn bị xong.</p>
        <ul class="ingredients">
          <li v-for="i in recipe.ingredients" :key="i.ingredientId">
            <button type="button" :aria-pressed="done.ingredients.has(i.ingredientId)" @click="flip(done.ingredients, i.ingredientId)">
              <span class="name"><span aria-hidden="true">{{ i.emoji }}</span> {{ i.name }}<small v-if="i.note"> ({{ i.note }})</small></span>
              <span class="qty num">{{ formatQuantity(i.quantity, i.unit) }}</span>
            </button>
          </li>
        </ul>
      </section>
      <section aria-labelledby="steps-title">
        <h2 id="steps-title">Cách làm</h2>
        <p class="hint">Xong bước nào, chạm vào bước đó để đóng dấu.</p>
        <ol class="steps">
          <li v-for="s in recipe.steps" :key="s.stepNo" :class="{ done: done.steps.has(s.stepNo) }">
            <button type="button" class="step" :aria-pressed="done.steps.has(s.stepNo)" @click="flip(done.steps, s.stepNo)">
              <span class="no num" aria-hidden="true">{{ s.stepNo }}</span>
              <span class="text">{{ s.content }}</span>
              <span v-if="done.steps.has(s.stepNo)" class="mark"><CheckIcon aria-hidden="true" />Xong</span>
            </button>
            <img v-if="s.imageUrl" :src="s.imageUrl" alt="" loading="lazy" />
          </li>
        </ol>
      </section>
    </div>

    <!-- Bản chưa đăng không có khu cộng đồng (API cũng trả 404). -->
    <template v-if="recipe.status === 'PUBLISHED'">
      <RatingSection :recipe-id="recipe.id" :author-id="recipe.author.id" @changed="refresh" />
      <CommentSection :recipe-id="recipe.id" />
    </template>
  </article>
</template>

<style scoped>
.recipe { max-width: 1040px; margin: 0.5rem auto 0; }
.notice { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem 1rem; padding: 0.75rem 0 1rem; margin-bottom: 1rem; border-bottom: 1px dashed var(--rule); }
.notice p { margin: 0; }
.head { margin-bottom: 1.25rem; }
.head h1 { font-size: clamp(2.5rem, 8vw, 4.5rem); line-height: 0.95; max-width: 18ch; }
.summary { font-size: 1.1rem; max-width: 60ch; margin: 0 0 0.5rem; }
.by { margin: 0; color: var(--muted); }
.social { display: flex; flex-wrap: wrap; align-items: center; gap: 0.25rem 1rem; margin: 0.5rem 0 0; }
.cover { display: block; width: 100%; max-height: 440px; object-fit: cover; margin-bottom: 1.25rem; }

.facts { display: grid; grid-template-columns: repeat(2, 1fr); margin: 0 0 1rem; border-top: 2px solid var(--ink); border-bottom: 1px solid var(--rule); }
@media (min-width: 640px) { .facts { grid-template-columns: repeat(4, 1fr); } }
.facts div { padding: 0.6rem 0.75rem; border-right: 1px solid var(--rule); }
.facts div:nth-child(2n) { border-right: 0; }
@media (max-width: 639px) { .facts div:nth-child(-n + 2) { border-bottom: 1px solid var(--rule); } }
@media (min-width: 640px) { .facts div:nth-child(2n) { border-right: 1px solid var(--rule); } .facts div:last-child { border-right: 0; } }
.facts dt { font-size: 0.85rem; color: var(--muted); }
.facts dd { margin: 0; font-weight: 600; }
.facts .num { font-family: var(--font-display); font-weight: 900; font-size: 2.25rem; line-height: 1; color: var(--red); }

.tags { display: flex; flex-wrap: wrap; gap: 0.25rem 1rem; }
.tags a { display: inline-block; padding: 0.6rem 0; }

.cols { display: grid; gap: 2rem; margin-top: 1rem; }
@media (min-width: 820px) { .cols { grid-template-columns: minmax(0, 2fr) minmax(0, 3fr); gap: 3rem; } }
.cols h2 { font-family: var(--font-display); font-weight: 800; font-size: 2rem; margin: 0; }
.cols .hint { margin: 0 0 0.5rem; }

.ingredients { list-style: none; padding: 0; margin: 0; border-top: 2px solid var(--ink); }
.ingredients li { border-bottom: 1px solid var(--rule); }
.ingredients button {
  display: flex; justify-content: space-between; align-items: center; gap: 1rem; width: 100%; min-height: 48px;
  padding: 0.4rem 0; font: inherit; font-size: 1.05rem; text-align: left; color: var(--ink); background: none; border: 0; cursor: pointer;
}
.ingredients .qty { font-weight: 700; white-space: nowrap; }
.ingredients button[aria-pressed='true'] { color: var(--muted); }
.ingredients button[aria-pressed='true'] .name { text-decoration: line-through; text-decoration-color: var(--red); text-decoration-thickness: 2px; }

.steps { list-style: none; padding: 0; margin: 0; border-top: 2px solid var(--ink); }
.steps li { border-bottom: 1px solid var(--rule); padding-bottom: 0.75rem; }
.step {
  display: grid; grid-template-columns: 3rem 1fr; gap: 0.5rem 0.75rem; width: 100%; padding: 0.75rem 0 0;
  font: inherit; text-align: left; color: var(--ink); background: none; border: 0; cursor: pointer;
}
.no { font-family: var(--font-display); font-weight: 900; font-size: 2.75rem; line-height: 0.85; color: var(--red); }
.text { font-size: 1.1rem; line-height: 1.6; white-space: pre-line; }
.done .text { color: var(--muted); }
.mark {
  grid-column: 2; justify-self: start; display: inline-flex; align-items: center; gap: 0.3rem;
  padding: 0.1rem 0.5rem; border: 2px solid var(--ok); border-radius: 3px; color: var(--ok); font-weight: 700; font-size: 0.85rem;
  transform: rotate(-4deg); background: var(--paper);
}
.mark svg { width: 0.8rem; height: 0.8rem; }
.steps img { display: block; margin: 0.75rem 0 0 3.75rem; max-width: calc(100% - 3.75rem); }
</style>
