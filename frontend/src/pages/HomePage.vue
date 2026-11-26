<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { GROUP_LABELS, listIngredients, listNamed, type Ingredient, type IngredientGroup, type NamedItem } from '@/api/catalog'
import { fromQuery, searchRecipes, toQuery, type RecipeCard as Card } from '@/api/recipes'
import RecipeCard from '@/components/RecipeCard.vue'

const route = useRoute()
const router = useRouter()
const ingredients = ref<Ingredient[]>([])
const categories = ref<NamedItem[]>([])
const recipes = ref<Card[]>([])
const total = ref<number | null>(null)
const error = ref('')

listIngredients().then((i) => (ingredients.value = i), () => {})
listNamed('categories').then((c) => (categories.value = c), () => {})

// Lựa chọn nằm trên URL để bấm Back từ trang món vẫn còn nguyên.
const selected = computed(() => fromQuery(route.query).ingredientIds ?? [])
const isPicked = (id: number) => selected.value.includes(id)
function setSelected(ids: number[]) {
  router.replace({ query: toQuery({ ingredientIds: ids.length ? ids : undefined }) })
}
const toggle = (id: number) => setSelected(isPicked(id) ? selected.value.filter((x) => x !== id) : [...selected.value, id])

const byGroup = computed(() => {
  const groups = new Map<IngredientGroup, Ingredient[]>()
  for (const g of Object.keys(GROUP_LABELS) as IngredientGroup[]) groups.set(g, [])
  for (const i of ingredients.value) groups.get(i.group)?.push(i)
  return [...groups].filter(([, items]) => items.length)
})
const chosen = computed(() => ingredients.value.filter((i) => isPicked(i.id)))

let latest = 0
watch(
  selected,
  async (ids) => {
    const ticket = ++latest
    error.value = ''
    try {
      const page = await searchRecipes({ ingredientIds: ids.length ? ids : undefined })
      if (ticket !== latest) return
      total.value = page.page.totalElements
      recipes.value = page.content.slice(0, 8)
    } catch {
      if (ticket === latest) error.value = 'Không tải được công thức. Kiểm tra máy chủ rồi tải lại trang.'
    }
  },
  { immediate: true },
)

const listLink = computed(() => ({ path: '/recipes', query: toQuery({ ingredientIds: selected.value.length ? selected.value : undefined }) }))

// Chỉ mở một nhóm một lúc để phần chọn gọn; món đã chọn ở nhóm khác vẫn hiện trong thanh trên.
const activeGroup = ref<IngredientGroup | null>(null)
const shownGroup = computed(() => byGroup.value.find(([g]) => g === activeGroup.value) ?? byGroup.value[0])
const pickedIn = (items: Ingredient[]) => items.filter((i) => isPicked(i.id)).length
</script>
<template>
  <div class="home">
    <section class="hero" aria-labelledby="hero-title">
      <h1 id="hero-title">Trong bếp có gì?</h1>
      <p class="lede">Chọn vài thứ bạn đang có, mình tìm món nấu được.</p>

      <div class="bar">
        <ul v-if="chosen.length" class="picked" aria-label="Đã chọn">
          <li v-for="i in chosen" :key="i.id">
            <button type="button" class="tag" :aria-label="`Bỏ ${i.name}`" @click="toggle(i.id)">
              <span aria-hidden="true">{{ i.emoji }}</span>{{ i.name }}<span class="x" aria-hidden="true">×</span>
            </button>
          </li>
          <li><button type="button" class="link-btn" @click="setSelected([])">Bỏ chọn</button></li>
        </ul>
        <p v-else class="placeholder">Chưa chọn nguyên liệu nào</p>
        <RouterLink v-if="total !== 0" :to="listLink" class="btn lime go">
          {{ selected.length ? `Xem ${total ?? ''} món` : `Xem tất cả ${total ?? ''} món` }}
        </RouterLink>
      </div>

      <p v-if="!ingredients.length && !error" class="loading">Đang lấy danh sách nguyên liệu…</p>
      <template v-else-if="shownGroup">
        <div class="groups" role="group" aria-label="Nhóm nguyên liệu">
          <button v-for="[g, items] in byGroup" :key="g" type="button" class="group" :aria-pressed="g === shownGroup[0]"
            aria-controls="group-cells" @click="activeGroup = g">
            {{ GROUP_LABELS[g] }}<span v-if="pickedIn(items)" class="n num">{{ pickedIn(items) }}</span>
          </button>
        </div>
        <div id="group-cells" class="cells" role="group" :aria-label="GROUP_LABELS[shownGroup[0]]">
          <button v-for="i in shownGroup[1]" :key="i.id" type="button" class="cell" :aria-pressed="isPicked(i.id)" @click="toggle(i.id)">
            <span aria-hidden="true">{{ isPicked(i.id) ? '✓' : i.emoji }}</span>{{ i.name }}
          </button>
        </div>
      </template>
    </section>

    <section class="results" aria-labelledby="results-title">
      <header class="row">
        <h2 id="results-title" aria-live="polite">
          <template v-if="error">{{ error }}</template>
          <template v-else-if="!selected.length">Món mới</template>
          <template v-else-if="total === 0">Chưa có món nào dùng đủ {{ chosen.length }} thứ này. Bỏ bớt một nguyên liệu thử xem.</template>
          <template v-else>{{ total ?? '' }} món nấu được với {{ chosen.map((i) => `${i.emoji ?? ''} ${i.name}`).join(', ') }}</template>
        </h2>
        <RouterLink v-if="recipes.length" :to="listLink" class="all">Xem tất cả</RouterLink>
      </header>
      <div v-if="recipes.length" class="grid">
        <RecipeCard v-for="r in recipes" :key="r.id" :recipe="r" />
      </div>
    </section>

    <section class="how" aria-labelledby="how-title">
      <h2 id="how-title">Cách dùng</h2>
      <ol>
        <li><span class="step num">1</span><div><h3>Chọn nguyên liệu</h3><p>Chạm vào những thứ đang có trong tủ lạnh.</p></div></li>
        <li><span class="step num">2</span><div><h3>Xem món nấu được</h3><p>Chỉ hiện món dùng đủ mọi thứ bạn chọn.</p></div></li>
        <li><span class="step num">3</span><div><h3>Nấu theo từng bước</h3><p>Đánh dấu nguyên liệu và bước đã xong ngay trên trang món.</p></div></li>
      </ol>
    </section>

    <section v-if="categories.length" class="cats" aria-labelledby="cats-title">
      <h2 id="cats-title">Theo danh mục</h2>
      <ul>
        <li v-for="c in categories" :key="c.id">
          <RouterLink :to="{ path: '/recipes', query: { category: c.slug } }">{{ c.name }}</RouterLink>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.home { display: flex; flex-direction: column; gap: 3rem; }

.hero {
  margin: -1rem -1rem 0;
  padding: 1.75rem 1.25rem 1.5rem;
  background: var(--accent);
  color: #fff;
}
@media (min-width: 720px) { .hero { margin: 0.5rem 0 0; padding: 2.5rem; border-radius: var(--radius-xl); } }
.hero h1 { margin: 0 0 0.35rem; color: #fff; }
.lede { margin: 0 0 1.25rem; color: #dfe9e2; font-size: 1.05rem; }

.bar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem 1rem;
  padding: 0.5rem 0.5rem 0.5rem 1rem;
  background: var(--surface); color: var(--text); border-radius: var(--radius-lg);
}
@media (min-width: 720px) { .bar { border-radius: 999px; } }
.picked { flex: 1 1 16rem; display: flex; flex-wrap: wrap; align-items: center; gap: 0.35rem; margin: 0; padding: 0; list-style: none; }
.placeholder { flex: 1 1 12rem; margin: 0; color: var(--muted); }
.tag {
  display: inline-flex; align-items: center; gap: 0.35rem; min-height: 36px; padding: 0.2rem 0.75rem;
  font: inherit; font-weight: 600; font-size: 0.95rem; color: var(--accent-deep); background: var(--lime-soft);
  border: 0; border-radius: 999px; cursor: pointer;
}
.tag:hover { background: var(--lime); }
.tag .x { font-size: 1.1em; line-height: 1; color: var(--muted); }
.picked .link-btn { min-height: 36px; padding: 0 0.5rem; color: var(--muted); }
.go { flex: 0 0 auto; margin-left: auto; min-height: 48px; padding-inline: 1.5rem; }
@media (max-width: 519px) { .go { flex: 1 1 100%; } }

.loading { margin: 1rem 0 0; color: #dfe9e2; }
.groups { display: flex; gap: 0.35rem; margin: 1.25rem -1.25rem 0.75rem; padding: 0 1.25rem; overflow-x: auto; scrollbar-width: none; }
@media (min-width: 720px) { .groups { flex-wrap: wrap; margin-inline: 0; padding: 0; overflow: visible; } }
.group {
  flex: none; display: inline-flex; align-items: center; gap: 0.4rem; min-height: 44px; padding: 0.3rem 1rem;
  font: inherit; font-weight: 600; color: #fff; background: transparent;
  border: 1px solid rgb(255 255 255 / 0.35); border-radius: 999px; cursor: pointer; white-space: nowrap;
}
.group:hover { border-color: #fff; }
.group[aria-pressed='true'] { background: #fff; color: var(--accent-deep); border-color: #fff; }
.group .n { min-width: 1.4rem; padding: 0 0.35rem; border-radius: 999px; background: var(--lime); color: var(--accent-deep); font-size: 0.8rem; text-align: center; }

.cells { display: flex; flex-wrap: wrap; gap: 0.4rem; }
/* Mobile: tối đa 2 hàng, vuốt ngang để xem hết nhóm, phần chọn không đẩy danh sách món xuống quá xa. */
@media (max-width: 719px) {
  .cells {
    display: grid; grid-auto-flow: column; grid-template-rows: repeat(2, auto); justify-content: start;
    margin-inline: -1.25rem; padding: 0 1.25rem 0.25rem; overflow-x: auto; scrollbar-width: none; scroll-snap-type: x proximity;
  }
  .cell { scroll-snap-align: start; }
}
.cell {
  display: inline-flex; align-items: center; gap: 0.4rem; min-height: 44px; padding: 0.3rem 0.9rem;
  font: inherit; font-size: 0.95rem; color: #fff; background: rgb(255 255 255 / 0.12);
  border: 1px solid transparent; border-radius: 999px; cursor: pointer; white-space: nowrap;
}
.cell:hover { background: rgb(255 255 255 / 0.2); }
.cell[aria-pressed='true'] { background: var(--lime); color: var(--accent-deep); font-weight: 700; }
.hero :focus-visible { outline-color: var(--lime); }

.row { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: 0.5rem 1rem; margin-bottom: 1.25rem; }
.row h2 { margin: 0; font-size: 1.5rem; max-width: 40ch; }
.all { font-weight: 600; }
.grid { display: grid; gap: 1.5rem 1.25rem; grid-template-columns: repeat(auto-fill, minmax(min(100%, 16rem), 1fr)); }

.how ol { display: grid; gap: 1rem; margin: 0; padding: 0; list-style: none; }
@media (min-width: 720px) { .how ol { grid-template-columns: repeat(3, 1fr); } }
.how li { display: flex; gap: 1rem; padding: 1.25rem; background: var(--sage); border-radius: var(--radius-lg); }
.how h3 { margin: 0 0 0.25rem; font-size: 1.05rem; }
.how p { margin: 0; color: var(--muted); }
.step { flex: none; display: grid; place-items: center; width: 2.25rem; height: 2.25rem; border-radius: 50%; background: var(--accent); color: #fff; font-weight: 800; }

.cats ul { list-style: none; margin: 0; padding: 0; display: flex; flex-wrap: wrap; gap: 0.5rem; }
.cats a {
  display: inline-flex; align-items: center; min-height: 44px; padding: 0.4rem 1.1rem;
  color: var(--accent-deep); font-weight: 600; text-decoration: none;
  background: var(--surface); border: 1px solid var(--border); border-radius: 999px;
}
.cats a:hover { border-color: var(--accent); background: var(--sage); }
</style>
