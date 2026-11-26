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

const today = new Date()
const sunday = today.getDay() === 0
const solar = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(today)
// ponytail: lịch âm theo lịch Trung Quốc (UTC+8), hiếm khi lệch một ngày so với âm lịch Việt (UTC+7).
const lunar = (() => {
  const parts = new Intl.DateTimeFormat('vi-VN-u-ca-chinese', { day: 'numeric', month: 'numeric' }).formatToParts(today)
  const day = parts.find((p) => p.type === 'day')?.value
  const month = parts.find((p) => p.type === 'month')?.value ?? ''
  return day ? `Âm lịch ${day} tháng ${month.replace(/\D/g, '')}${/\D/.test(month) ? ' nhuận' : ''}` : ''
})()
</script>

<template>
  <div class="home">
    <section class="sheet today" aria-labelledby="today-title">
      <header class="head">
        <p class="solar" :class="{ sunday }">{{ solar }}</p>
        <p v-if="lunar" class="lunar">{{ lunar }}</p>
      </header>

      <div class="body">
        <div class="count">
          <div class="pad">
            <Transition name="tear">
              <span :key="total ?? -1" class="numeral num" :style="{ '--d': String(total ?? 0).length }" aria-hidden="true">{{ total ?? '–' }}</span>
            </Transition>
          </div>
          <p class="caption" aria-live="polite">
            <template v-if="error">{{ error }}</template>
            <template v-else-if="total === null">Đang đếm món…</template>
            <template v-else-if="!selected.length">
              <span class="sr-only">{{ total }}</span> món trong sổ. Chạm vào những thứ đang có trong bếp để đếm số món nấu được.
            </template>
            <template v-else-if="total === 0">
              Chưa có món nào dùng đủ {{ chosen.length }} thứ này. Bỏ bớt một nguyên liệu thử xem.
            </template>
            <template v-else>
              <span class="sr-only">{{ total }}</span> món nấu được với
              <span v-for="(i, n) in chosen" :key="i.id" class="with">{{ i.emoji }} {{ i.name }}{{ n < chosen.length - 1 ? ', ' : '.' }}</span>
            </template>
          </p>
        </div>

        <div class="pantry">
          <h1 id="today-title">Trong bếp có gì?</h1>
          <p v-if="!ingredients.length && !error" class="hint">Đang lấy danh sách nguyên liệu…</p>
          <div class="table" role="group" aria-label="Nguyên liệu đang có">
            <div v-for="[g, items] in byGroup" :key="g" class="row">
              <span class="group">{{ GROUP_LABELS[g] }}</span>
              <div class="cells">
                <button v-for="i in items" :key="i.id" type="button" class="cell" :aria-pressed="isPicked(i.id)"
                  @click="toggle(i.id)"><span class="emo" aria-hidden="true">{{ i.emoji }}</span>{{ i.name }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <footer class="tear-line">
        <RouterLink :to="listLink" class="btn primary go">
          {{ selected.length ? `Xem ${total ?? ''} món nấu được` : 'Mở cả sổ công thức' }}
        </RouterLink>
        <button v-if="selected.length" type="button" class="link-btn" @click="setSelected([])">Bỏ chọn</button>
      </footer>
    </section>

    <section v-if="recipes.length" class="next" aria-labelledby="next-title">
      <h2 id="next-title">{{ selected.length ? 'Nấu được ngay' : 'Món mới lên lịch' }}</h2>
      <div class="grid">
        <RecipeCard v-for="r in recipes" :key="r.id" :recipe="r" />
      </div>
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
.today { padding-bottom: 0; }

.head { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 0.25rem 1rem; padding-bottom: 0.75rem; border-bottom: 1px solid var(--rule); }
.head p { margin: 0; }
.solar { font-weight: 700; }
.solar::first-letter { text-transform: uppercase; }
.sunday { color: var(--red); }
.lunar { color: var(--muted); }

.body { display: grid; gap: 1.5rem; padding: 1.25rem 0 1.5rem; }
@media (min-width: 900px) { .body { grid-template-columns: minmax(0, 5fr) minmax(0, 7fr); gap: 2.5rem; } }

/* Số to theo chiều cao màn hình và theo bề rộng cột chia cho số chữ số (--d), nên 1 chữ số to hết cỡ mà 3 chữ số vẫn không tràn.
   Mobile chặn ở 34svh để hàng nguyên liệu đầu tiên vẫn lộ trên màn hình đầu. */
.count { container-type: inline-size; }
/* Mobile: chú thích nằm cạnh số, căn đáy; số 3 chữ số chiếm hết bề ngang thì chú thích tự xuống dòng. */
@media (max-width: 899px) {
  .count { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 0.75rem 1rem; }
  .count .caption { flex: 1 1 9rem; margin: 0; }
}
/* Desktop: bảng nguyên liệu dài, con số dính lại để luôn thấy nó đổi khi chạm. */
@media (min-width: 900px) {
  .body { align-items: start; }
  .count { position: sticky; top: 1.5rem; padding: 1rem 2.5rem 1rem 0; border-right: 1px solid var(--rule); }
}
/* fit-content: tờ đang xé chỉ che đúng ô chữ số, không che đường kẻ bên cạnh. */
.numeral {
  display: block;
  width: fit-content;
  font-family: var(--font-display);
  font-weight: 900;
  font-size: min(34svh, calc(205cqi / var(--d, 3)), 26rem);
  line-height: 0.78;
  letter-spacing: -0.03em;
  color: var(--red);
  transform-origin: 0 0;
  background: var(--paper);
}
@media (min-width: 900px) { .numeral { font-size: min(48svh, calc(205cqi / var(--d, 3)), 26rem); } }
.pad { display: grid; }
.pad > * { grid-area: 1 / 1; }
.caption { margin: 1rem 0 0; font-size: 1.05rem; max-width: 34ch; }
.with { white-space: nowrap; font-weight: 600; }

/* Xé tờ: số mới đã nằm sẵn bên dưới; tờ cũ (nền giấy che số mới) bị giật lên khỏi gáy, xoay quanh mép trên. */
.tear-leave-active { z-index: 1; transition: transform 0.38s var(--ease-out), opacity 0.38s ease-in, clip-path 0.38s var(--ease-out); }
.tear-leave-to { transform: translateY(-22%) rotate(-9deg); opacity: 0; clip-path: inset(0 0 55% 0); }

.pantry h1 { margin-bottom: 0.75rem; }
.table { border-top: 2px solid var(--ink); }
.row { display: grid; grid-template-columns: 1fr; border-bottom: 1px solid var(--rule); }
@media (min-width: 600px) { .row { grid-template-columns: 7.5rem 1fr; } }
.group { padding: 0.55rem 0 0; font-size: 0.85rem; font-weight: 700; color: var(--muted); }
@media (min-width: 600px) { .group { padding: 0.8rem 0.75rem 0.8rem 0; border-right: 1px solid var(--rule); } }
.cells { display: flex; flex-wrap: wrap; margin-inline: -0.25rem; }
@media (min-width: 600px) { .cells { margin: 0; padding-left: 0.25rem; } }
.cell {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 44px;
  margin: 0.2rem 0.25rem;
  padding: 0.3rem 0.7rem;
  font: inherit;
  font-size: 0.95rem;
  color: var(--ink);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius);
  cursor: pointer;
  white-space: nowrap;
}
.cell:hover { border-color: var(--rule); }
.cell[aria-pressed='true'] { background: var(--red); color: #fff; font-weight: 600; }
.emo { font-size: 1.15em; line-height: 1; }

.tear-line {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 1rem;
  margin: 0 -1.25rem;
  padding: 0.9rem 1.25rem 1.1rem;
  background: var(--paper);
  border-top: 2px dotted var(--rule);
}
@media (min-width: 720px) { .tear-line { margin: 0 -2rem; padding: 1rem 2rem 1.25rem; } }
.go { flex: 1 1 auto; min-width: 0; font-size: 1.05rem; min-height: 52px; }
.tear-line .link-btn { flex: none; white-space: nowrap; }
@media (min-width: 720px) { .go { flex: 0 1 auto; padding-inline: 1.75rem; } }

.next h2, .cats h2 { font-family: var(--font-display); font-weight: 800; font-size: 2rem; letter-spacing: -0.01em; }
.grid { display: grid; gap: 1.5rem 1.25rem; grid-template-columns: repeat(auto-fill, minmax(min(100%, 15rem), 1fr)); }

.cats ul { list-style: none; margin: 0; padding: 0; display: flex; flex-wrap: wrap; border-top: 2px solid var(--ink); }
.cats li { border-bottom: 1px solid var(--rule); border-right: 1px solid var(--rule); }
.cats a { display: block; padding: 0.75rem 1.25rem; color: var(--ink); font-weight: 600; text-decoration: none; }
.cats a:hover { background: var(--red); color: #fff; }

.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
</style>
