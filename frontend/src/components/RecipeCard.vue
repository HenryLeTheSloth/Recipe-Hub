<script setup lang="ts">
import { DIFFICULTY_LABELS, type RecipeCard } from '@/api/recipes'

defineProps<{ recipe: RecipeCard }>()
</script>

<template>
  <RouterLink :to="`/recipes/${recipe.slug}`" class="card">
    <img v-if="recipe.coverUrl" :src="recipe.coverUrl" alt="" loading="lazy" />
    <div v-else class="noimg" aria-hidden="true">🍲</div>
    <div class="body">
      <h3>{{ recipe.title }}</h3>
      <p class="meta">
        <span v-if="recipe.categoryName">{{ recipe.categoryName }} · </span>
        {{ recipe.totalMinutes }} phút · {{ DIFFICULTY_LABELS[recipe.difficulty] }}
      </p>
      <p class="meta">bởi {{ recipe.authorName }}</p>
    </div>
  </RouterLink>
</template>

<style scoped>
.card {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  color: var(--text);
  text-decoration: none;
}
.card:hover { border-color: var(--accent); }
img, .noimg { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; }
.noimg { display: grid; place-items: center; font-size: 3rem; background: var(--bg); }
.body { padding: 0.75rem 0.9rem; }
h3 { margin: 0 0 0.35rem; font-size: 1.05rem; }
.meta { margin: 0; color: var(--muted); font-size: 0.875rem; }
</style>
