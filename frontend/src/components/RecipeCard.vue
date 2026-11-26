<script setup lang="ts">
import { DIFFICULTY_LABELS, type RecipeCard } from '@/api/recipes'

defineProps<{ recipe: RecipeCard }>()
</script>

<template>
  <RouterLink :to="`/recipes/${recipe.slug}`" class="card">
    <img v-if="recipe.coverUrl" :src="recipe.coverUrl" alt="" loading="lazy" />
    <!-- Không có ảnh: tờ lịch in to số phút, vẫn là một mặt thẻ có chủ ý. -->
    <div v-else class="noimg" aria-hidden="true">
      <span class="mins num">{{ recipe.totalMinutes }}</span><span class="unit">phút</span>
    </div>
    <div class="body">
      <h3>{{ recipe.title }}</h3>
      <ul class="facts">
        <li v-if="recipe.coverUrl" class="num">{{ recipe.totalMinutes }} phút</li>
        <li>{{ DIFFICULTY_LABELS[recipe.difficulty] }}</li>
        <li v-if="recipe.categoryName">{{ recipe.categoryName }}</li>
      </ul>
      <p class="by">bởi {{ recipe.authorName }}</p>
    </div>
  </RouterLink>
</template>

<style scoped>
.card {
  display: flex;
  flex-direction: column;
  background: var(--paper);
  border-top: 5px solid var(--red);
  box-shadow: 0 3px 0 -1px var(--paper), 0 3px 0 0 var(--rule);
  color: var(--ink);
  text-decoration: none;
}
.card:hover { color: var(--ink); }
.card:hover h3 { color: var(--red); }
img, .noimg { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; display: block; }
.noimg {
  display: flex;
  align-items: flex-end;
  gap: 0.4rem;
  padding: 0 1rem 0.5rem;
  border-bottom: 1px solid var(--rule);
  color: var(--red);
}
.mins { font-family: var(--font-display); font-weight: 900; font-size: 6.5rem; line-height: 0.8; letter-spacing: -0.03em; }
.unit { font-weight: 700; padding-bottom: 0.2rem; }
.body { padding: 0.85rem 1rem 1rem; display: flex; flex-direction: column; gap: 0.5rem; flex: 1; }
h3 { margin: 0; font-size: 1.15rem; transition: color 0.15s; }
.facts { list-style: none; margin: 0; padding: 0; display: flex; flex-wrap: wrap; border-top: 1px solid var(--rule); border-bottom: 1px solid var(--rule); font-size: 0.875rem; }
.facts li { padding: 0.3rem 0.6rem; border-right: 1px solid var(--rule); }
.facts li:first-child { padding-left: 0; }
.facts li:last-child { border-right: 0; }
.by { margin: auto 0 0; color: var(--muted); font-size: 0.875rem; }
</style>
