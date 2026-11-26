<script setup lang="ts">
import { DIFFICULTY_LABELS, type RecipeCard } from '@/api/recipes'
import BrandMark from '@/components/BrandMark.vue'

defineProps<{ recipe: RecipeCard }>()
</script>

<template>
  <RouterLink :to="`/recipes/${recipe.slug}`" class="card">
    <div class="media">
      <img v-if="recipe.coverUrl" :src="recipe.coverUrl" alt="" loading="lazy" />
      <!-- Không có ảnh: cùng khung 4:3, nền xanh nhạt với biểu tượng bát, chỉ khác mỗi bức ảnh. -->
      <div v-else class="noimg" aria-hidden="true"><BrandMark /></div>
      <span class="time num">{{ recipe.totalMinutes }} phút</span>
    </div>
    <div class="body">
      <h3>{{ recipe.title }}</h3>
      <p class="by">bởi {{ recipe.authorName }}</p>
      <ul class="badges">
        <li class="diff">{{ DIFFICULTY_LABELS[recipe.difficulty] }}</li>
        <li v-if="recipe.categoryName" class="cat">{{ recipe.categoryName }}</li>
      </ul>
      <p v-if="recipe.ratingCount" class="rate num">
        <span class="star" aria-hidden="true">★</span> {{ recipe.avgRating?.toFixed(1) }}
        <span class="cnt">({{ recipe.ratingCount }} đánh giá)</span>
      </p>
    </div>
  </RouterLink>
</template>

<style scoped>
.card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  color: var(--text);
  text-decoration: none;
  transition: box-shadow 0.2s, translate 0.2s var(--ease-out);
}
.card:hover { color: var(--text); box-shadow: var(--shadow-lift); translate: 0 -2px; }
@media (prefers-reduced-motion: reduce) { .card:hover { translate: none; } }
.media { position: relative; aspect-ratio: 4 / 3; background: var(--sage); }
img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.noimg { display: grid; place-items: center; height: 100%; color: var(--accent); opacity: 0.5; }
.noimg :deep(.brand-mark) { width: 56px; height: 56px; }
.time {
  position: absolute; top: 0.75rem; left: 0.75rem;
  padding: 0.15rem 0.6rem; border-radius: 999px;
  background: var(--surface); color: var(--text); font-size: 0.8rem; font-weight: 600;
}
.body { display: flex; flex-direction: column; gap: 0.4rem; flex: 1; padding: 0.9rem 1rem 1rem; }
h3 {
  margin: 0; font-size: 1.0625rem; font-weight: 700; line-height: 1.3;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.by { margin: 0; color: var(--muted); font-size: 0.875rem; }
.badges { display: flex; flex-wrap: wrap; gap: 0.35rem; margin: 0.15rem 0 0; padding: 0; list-style: none; }
.badges li { padding: 0.1rem 0.5rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
.diff { background: var(--lime-soft); color: var(--accent); }
.cat { background: var(--sage); color: var(--text); }
.rate { margin: auto 0 0; padding-top: 0.6rem; border-top: 1px solid var(--border); font-size: 0.875rem; font-weight: 700; }
.star { color: var(--star); }
.cnt { color: var(--muted); font-weight: 400; }
</style>
