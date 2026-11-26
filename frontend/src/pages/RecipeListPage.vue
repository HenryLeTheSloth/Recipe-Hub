<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { GROUP_LABELS, listIngredients, listNamed, type Ingredient, type IngredientGroup, type NamedItem } from '@/api/catalog'
import { fromQuery, searchRecipes, toQuery, type Page, type RecipeCard as Card, type RecipeSearch } from '@/api/recipes'
import TimesIcon from '@primeicons/vue/times'
import ChevronLeftIcon from '@primeicons/vue/chevron-left'
import ChevronRightIcon from '@primeicons/vue/chevron-right'
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

let latest = 0
watch(
  search,
  async (s) => {
    const ticket = ++latest
    q.value = s.q ?? ''
    error.value = ''
    try {
      const r = await searchRecipes(s)
      if (ticket === latest) result.value = r
    } catch {
      if (ticket === latest) error.value = 'Không tải được danh sách công thức. Kiểm tra máy chủ rồi tải lại trang.'
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
  <section class="list" aria-labelledby="list-title">
    <header class="head">
      <h1 id="list-title">Công thức</h1>
      <p v-if="result" class="count"><span class="num">{{ result.page.totalElements }}</span> món</p>
    </header>

    <form class="filters" @submit.prevent="apply({ q: q.trim() || undefined })">
      <select class="input" :value="search.sort ?? 'newest'" aria-label="Sắp xếp"
        @change="apply({ sort: ($event.target as HTMLSelectElement).value === 'rating' ? 'rating' : undefined })">
        <option value="newest">Mới nhất</option>
        <option value="rating">Điểm cao</option>
      </select>
      <input v-model="q" type="search" class="input name" placeholder="Tìm theo tên món" aria-label="Tìm theo tên món" />
      <select class="input" :value="search.category ?? ''" aria-label="Danh mục"
        @change="apply({ category: ($event.target as HTMLSelectElement).value || undefined })">
        <option value="">Mọi danh mục</option>
        <option v-for="c in categories" :key="c.id" :value="c.slug">{{ c.name }}</option>
      </select>
      <select class="input" :value="search.tag ?? ''" aria-label="Tag"
        @change="apply({ tag: ($event.target as HTMLSelectElement).value || undefined })">
        <option value="">Mọi tag</option>
        <option v-for="t in tags" :key="t.id" :value="t.slug">{{ t.name }}</option>
      </select>
      <select class="input pantry" aria-label="Thêm nguyên liệu" @change="addIngredient">
        <option value="">Thêm nguyên liệu đang có</option>
        <optgroup v-for="[g, items] in byGroup" :key="g" :label="GROUP_LABELS[g]">
          <option v-for="i in items" :key="i.id" :value="i.id">{{ i.emoji }} {{ i.name }}</option>
        </optgroup>
      </select>
      <button class="btn primary">Tìm</button>
    </form>
    <ul v-if="chosen.length" class="chips" aria-label="Nguyên liệu đang lọc">
      <li v-for="i in chosen" :key="i.id">
        <button type="button" class="chip" :aria-label="`Bỏ ${i.name}`" @click="removeIngredient(i.id)">
          <span aria-hidden="true">{{ i.emoji }}</span>{{ i.name }}<TimesIcon class="x" aria-hidden="true" />
        </button>
      </li>
    </ul>

    <p v-if="error" class="form-error">{{ error }}</p>
    <template v-else-if="result">
      <div v-if="!result.content.length" class="empty">
        <p>Không có món nào khớp bộ lọc này. Bỏ bớt một điều kiện thử xem.</p>
        <RouterLink to="/recipes" class="btn">Bỏ hết bộ lọc</RouterLink>
      </div>
      <div class="grid">
        <RecipeCard v-for="r in result.content" :key="r.id" :recipe="r" />
      </div>
      <nav v-if="result.page.totalPages > 1" class="pager" aria-label="Phân trang">
        <button class="btn" :disabled="page <= 1" @click="apply({ ...search, page: page - 1 })"><ChevronLeftIcon aria-hidden="true" />Trước</button>
        <span class="num">Trang {{ page }} / {{ result.page.totalPages }}</span>
        <button class="btn" :disabled="page >= result.page.totalPages" @click="apply({ ...search, page: page + 1 })">Sau<ChevronRightIcon aria-hidden="true" /></button>
      </nav>
    </template>
  </section>
</template>

<style scoped>
.head { display: flex; align-items: baseline; gap: 0.75rem; margin-bottom: 1rem; }
.head h1 { margin: 0; }
.count { margin: 0; color: var(--muted); }
.filters {
  display: grid; gap: 0.5rem; grid-template-columns: repeat(2, minmax(0, 1fr));
  padding: 0.75rem; background: var(--surface); border-radius: var(--radius-lg); box-shadow: var(--shadow-card);
}
.filters .input { width: 100%; min-width: 0; border-radius: 999px; }
.filters .name, .filters .pantry { grid-column: 1 / -1; }
.filters .btn { grid-column: 1 / -1; }
@media (min-width: 900px) {
  .filters { grid-template-columns: 9rem minmax(0, 2fr) repeat(3, minmax(0, 1fr)) auto; }
  .filters .name, .filters .pantry, .filters .btn { grid-column: auto; }
}
.chips { list-style: none; display: flex; flex-wrap: wrap; gap: 0.4rem; margin: 0.75rem 0 0; padding: 0; }
.chip {
  display: inline-flex; align-items: center; gap: 0.4rem; min-height: 44px; padding: 0.3rem 0.9rem;
  font: inherit; font-weight: 600; color: var(--accent-deep); background: var(--sage); border: 0; border-radius: 999px; cursor: pointer;
}
.chip:hover { background: var(--lime-soft); }
.chip .x { width: 0.75rem; height: 0.75rem; }
.empty { display: flex; flex-wrap: wrap; align-items: center; gap: 0.75rem 1.25rem; margin: 2rem 0; }
.empty p { margin: 0; max-width: 40ch; }
.grid { display: grid; gap: 1.5rem 1.25rem; margin-top: 1.5rem; grid-template-columns: repeat(auto-fill, minmax(min(100%, 16rem), 1fr)); }
.pager .btn svg { width: 0.85rem; height: 0.85rem; }
</style>
