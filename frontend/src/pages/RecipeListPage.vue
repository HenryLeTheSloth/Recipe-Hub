<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { GROUP_LABELS, listIngredients, listNamed, type Ingredient, type IngredientGroup, type NamedItem } from '@/api/catalog'
import { fromQuery, searchRecipes, toQuery, type Page, type RecipeCard as Card, type RecipeSearch } from '@/api/recipes'
import RecipeCard from '@/components/RecipeCard.vue'

const route = useRoute()
const router = useRouter()
const search = computed(() => fromQuery(route.query))
const result = ref<Page<Card> | null>(null)
const error = ref('')
const q = ref('')
const categories = ref<NamedItem[]>([])
const tags = ref<NamedItem[]>([])
const ingredients = ref<Ingredient[]>([])

listNamed('categories').then((c) => (categories.value = c), () => {})
listNamed('tags').then((t) => (tags.value = t), () => {})
listIngredients().then((i) => (ingredients.value = i), () => {})

const byGroup = computed(() => {
  const groups = new Map<IngredientGroup, Ingredient[]>()
  for (const i of ingredients.value) groups.set(i.group, [...(groups.get(i.group) ?? []), i])
  return groups
})
const chosen = computed(() =>
  (search.value.ingredientIds ?? []).map((id) => ingredients.value.find((i) => i.id === id) ?? { id, name: `#${id}`, emoji: null }),
)

watch(
  search,
  async (s) => {
    q.value = s.q ?? ''
    error.value = ''
    try {
      result.value = await searchRecipes(s)
    } catch {
      error.value = 'Không tải được danh sách công thức'
    }
  },
  { immediate: true },
)

/** Mọi thay đổi bộ lọc đi qua URL; đổi bộ lọc thì về trang 1. */
function apply(patch: Partial<RecipeSearch>) {
  router.push({ query: toQuery({ ...search.value, page: undefined, ...patch }) })
}
const addIngredient = (e: Event) => {
  const el = e.target as HTMLSelectElement
  const id = Number(el.value)
  el.value = ''
  const ids = search.value.ingredientIds ?? []
  if (id && !ids.includes(id)) apply({ ingredientIds: [...ids, id] })
}
const removeIngredient = (id: number) => apply({ ingredientIds: (search.value.ingredientIds ?? []).filter((x) => x !== id) })
const page = computed(() => search.value.page ?? 1)
</script>

<template>
  <div class="list">
    <h1>Công thức</h1>
    <form class="filters" @submit.prevent="apply({ q: q.trim() || undefined })">
      <input v-model="q" type="search" placeholder="Tìm theo tên món…" aria-label="Tìm theo tên món" />
      <select :value="search.category ?? ''" aria-label="Danh mục"
        @change="apply({ category: ($event.target as HTMLSelectElement).value || undefined })">
        <option value="">Mọi danh mục</option>
        <option v-for="c in categories" :key="c.id" :value="c.slug">{{ c.name }}</option>
      </select>
      <select :value="search.tag ?? ''" aria-label="Tag"
        @change="apply({ tag: ($event.target as HTMLSelectElement).value || undefined })">
        <option value="">Mọi tag</option>
        <option v-for="t in tags" :key="t.id" :value="t.slug">{{ t.name }}</option>
      </select>
      <select aria-label="Thêm nguyên liệu" @change="addIngredient">
        <option value="">+ Có nguyên liệu…</option>
        <optgroup v-for="[g, items] in byGroup" :key="g" :label="GROUP_LABELS[g]">
          <option v-for="i in items" :key="i.id" :value="i.id">{{ i.emoji }} {{ i.name }}</option>
        </optgroup>
      </select>
      <button class="btn primary">Tìm</button>
    </form>
    <p v-if="chosen.length" class="chips">
      <button v-for="i in chosen" :key="i.id" type="button" class="chip" :aria-label="`Bỏ ${i.name}`"
        @click="removeIngredient(i.id)">{{ i.emoji }} {{ i.name }} ✕</button>
    </p>

    <p v-if="error" class="form-error">{{ error }}</p>
    <template v-else-if="result">
      <p v-if="!result.content.length" class="hint">Không có công thức nào khớp.</p>
      <div class="grid">
        <RecipeCard v-for="r in result.content" :key="r.id" :recipe="r" />
      </div>
      <nav v-if="result.page.totalPages > 1" class="pager">
        <button class="btn" :disabled="page <= 1" @click="apply({ ...search, page: page - 1 })">‹ Trước</button>
        <span>Trang {{ page }} / {{ result.page.totalPages }}</span>
        <button class="btn" :disabled="page >= result.page.totalPages" @click="apply({ ...search, page: page + 1 })">Sau ›</button>
      </nav>
    </template>
  </div>
</template>

<style scoped>
.list { max-width: 1200px; margin: 1.5rem auto; }
.filters { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 0.75rem; }
.filters input, .filters select {
  font: inherit; padding: 0.55rem 0.7rem; border: 1px solid var(--border); border-radius: 8px;
  background: var(--surface); color: var(--text);
}
.filters input { flex: 1 1 220px; }
.chips { display: flex; flex-wrap: wrap; gap: 0.4rem; }
.chip { font: inherit; border: 1px solid var(--accent); border-radius: 999px; padding: 0.25rem 0.7rem; background: var(--surface); color: var(--text); cursor: pointer; }
.grid { display: grid; gap: 1rem; grid-template-columns: 1fr; }
@media (min-width: 600px) { .grid { grid-template-columns: repeat(2, 1fr); } }
@media (min-width: 900px) { .grid { grid-template-columns: repeat(3, 1fr); } }
@media (min-width: 1200px) { .grid { grid-template-columns: repeat(4, 1fr); } }
.pager { display: flex; align-items: center; justify-content: center; gap: 1rem; margin: 1.5rem 0; }
</style>
