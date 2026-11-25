<script setup lang="ts">
import { ref } from 'vue'
import { listNamed, type NamedItem } from '@/api/catalog'
import { searchRecipes, type RecipeCard as Card } from '@/api/recipes'
import RecipeCard from '@/components/RecipeCard.vue'

const latest = ref<Card[]>([])
const categories = ref<NamedItem[]>([])
searchRecipes({}).then((p) => (latest.value = p.content.slice(0, 8)), () => {})
listNamed('categories').then((c) => (categories.value = c), () => {})
</script>

<template>
  <div class="home">
    <h1>Recipe Hub</h1>
    <p v-if="categories.length" class="cats">
      <RouterLink v-for="c in categories" :key="c.id" :to="{ path: '/recipes', query: { category: c.slug } }" class="cat">
        {{ c.name }}</RouterLink>
    </p>
    <h2>Mới nhất</h2>
    <p v-if="!latest.length" class="hint">Chưa có công thức nào.</p>
    <div class="grid">
      <RecipeCard v-for="r in latest" :key="r.id" :recipe="r" />
    </div>
    <p><RouterLink to="/recipes">Xem tất cả công thức →</RouterLink></p>
  </div>
</template>

<style scoped>
.home { max-width: 1200px; margin: 1.5rem auto; }
.cats { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.cat { border: 1px solid var(--border); border-radius: 999px; padding: 0.3rem 0.8rem; background: var(--surface); color: var(--text); text-decoration: none; }
.grid { display: grid; gap: 1rem; grid-template-columns: 1fr; }
@media (min-width: 600px) { .grid { grid-template-columns: repeat(2, 1fr); } }
@media (min-width: 900px) { .grid { grid-template-columns: repeat(4, 1fr); } }
</style>
