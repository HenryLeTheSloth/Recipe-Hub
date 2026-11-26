<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listNamed, type NamedItem } from '@/api/catalog'
import { ApiError } from '@/api/http'
import { messageFor } from '@/api/messages'
import {
  createRecipe, DIFFICULTY_LABELS, getMyRecipe, STATUS_LABELS, submitRecipe, updateRecipe,
  type RecipeDetail, type RecipeStatus,
} from '@/api/recipes'
import ArrowUpIcon from '@primeicons/vue/arrow-up'
import ArrowDownIcon from '@primeicons/vue/arrow-down'
import TimesIcon from '@primeicons/vue/times'
import PlusIcon from '@primeicons/vue/plus'
import ImageUpload from '@/components/ImageUpload.vue'
import IngredientPicker from '@/components/IngredientPicker.vue'
import { addIngredient, emptyState, fieldLabel, fromDetail, moveStep, toRequest } from '@/features/recipe-editor/editorState'
import { UNIT_LABELS } from '@/utils/units'

const route = useRoute()
const router = useRouter()
const state = ref(emptyState())
const id = ref<number | null>(null)
const status = ref<RecipeStatus | null>(null)
const busy = ref(false)
const error = ref('')
const fieldErrors = ref<Record<string, string>>({})
const saved = ref(false)
const categories = ref<NamedItem[]>([])
const tags = ref<NamedItem[]>([])
listNamed('categories').then((c) => (categories.value = c), () => {})
listNamed('tags').then((t) => (tags.value = t), () => {})

const locked = computed(() => status.value === 'PENDING' || status.value === 'HIDDEN')
const loadFailed = ref(false)
const readOnly = computed(() => locked.value || loadFailed.value)
// Khóa ổn định cho từng bước: tải ảnh dở dang vẫn bám đúng bước khi bước phía trên bị xóa/đổi chỗ.
const stepKeys = new WeakMap<object, number>()
let nextKey = 0
const stepKey = (s: object) => stepKeys.get(s) ?? (stepKeys.set(s, ++nextKey), nextKey)
const published = computed(() => status.value === 'PUBLISHED')

watch(
  () => route.params.id as string | undefined,
  async (param) => {
    const n = param ? Number(param) : null
    if (n === id.value) return // vừa tạo xong rồi đổi URL: state đang đúng
    id.value = n
    status.value = null
    loadFailed.value = false
    state.value = emptyState()
    if (!n) return
    try {
      const d = await getMyRecipe(n)
      state.value = fromDetail(d)
      status.value = d.status
    } catch (e) {
      loadFailed.value = true
      error.value = e instanceof ApiError && e.status === 404 ? 'Không tìm thấy công thức' : messageFor(e)
    }
  },
  { immediate: true },
)

async function save(): Promise<RecipeDetail | null> {
  busy.value = true
  error.value = ''
  fieldErrors.value = {}
  saved.value = false
  try {
    const req = toRequest(state.value)
    const d = id.value ? await updateRecipe(id.value, req) : await createRecipe(req)
    if (!id.value) {
      id.value = d.id
      router.replace(`/me/recipes/${d.id}/edit`)
    }
    status.value = d.status
    saved.value = true
    return d
  } catch (e) {
    error.value = messageFor(e)
    if (e instanceof ApiError && e.fieldErrors) fieldErrors.value = e.fieldErrors
    return null
  } finally {
    busy.value = false
  }
}

/** Lưu rồi gửi duyệt; bản đã đăng thì lưu là tự về chờ duyệt. */
async function saveAndSubmit() {
  const d = await save()
  if (!d) return
  saved.value = false
  try {
    if (d.status === 'DRAFT' || d.status === 'REJECTED') await submitRecipe(d.id)
    router.push('/me/recipes')
  } catch (e) {
    error.value = messageFor(e)
  }
}

const removeAt = <T,>(arr: T[], i: number) => arr.splice(i, 1)
</script>

<template>
  <section class="sheet editor">
    <header class="head">
      <h1>{{ id ? 'Sửa công thức' : 'Viết công thức mới' }}</h1>
      <span v-if="status" class="stamp" :data-status="status">{{ STATUS_LABELS[status] }}</span>
    </header>
    <p v-if="published" class="warn">Lưu thay đổi sẽ gửi công thức đi duyệt lại.</p>
    <p v-if="locked" class="warn">Công thức đang {{ status === 'PENDING' ? 'chờ duyệt' : 'bị ẩn' }} nên chưa sửa được.</p>

    <form class="form" @submit.prevent>
      <fieldset :disabled="readOnly || busy">
        <label class="field">Tên món
          <input v-model="state.title" name="title" maxlength="120" required />
        </label>
        <label class="field">Giới thiệu ngắn
          <textarea v-model="state.summary" maxlength="500" rows="2" />
        </label>
        <div class="row">
          <label class="field">Danh mục
            <select v-model="state.categoryId">
              <option :value="null">Chọn danh mục</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </label>
          <label class="field">Độ khó
            <select v-model="state.difficulty">
              <option v-for="(label, d) in DIFFICULTY_LABELS" :key="d" :value="d">{{ label }}</option>
            </select>
          </label>
          <label class="field">Khẩu phần
            <input v-model.number="state.servings" type="number" min="1" max="50" />
          </label>
          <label class="field">Chuẩn bị (phút)
            <input v-model.number="state.prepMinutes" type="number" min="0" max="1440" />
          </label>
          <label class="field">Nấu (phút)
            <input v-model.number="state.cookMinutes" type="number" min="0" max="1440" />
          </label>
        </div>
        <div v-if="tags.length" class="field">Tag
          <div class="tags">
            <label v-for="t in tags" :key="t.id" class="tag">
              <input v-model="state.tagIds" type="checkbox" :value="t.id" /> {{ t.name }}
            </label>
          </div>
        </div>
        <div class="field">Ảnh bìa
          <ImageUpload v-model="state.cover" clearable />
        </div>

        <h2>Nguyên liệu</h2>
        <ul class="lines">
          <li v-for="(l, i) in state.ingredients" :key="l.ingredient.id">
            <span class="name">{{ l.ingredient.emoji }} {{ l.ingredient.name }}</span>
            <input v-model.number="l.quantity" type="number" min="0" step="0.01" aria-label="Số lượng" />
            <select v-model="l.unit" aria-label="Đơn vị">
              <option v-for="(label, u) in UNIT_LABELS" :key="u" :value="u">{{ label }}</option>
            </select>
            <input v-model="l.note" maxlength="100" placeholder="Ghi chú" aria-label="Ghi chú" />
            <button type="button" class="btn icon" :aria-label="`Bỏ ${l.ingredient.name}`" @click="removeAt(state.ingredients, i)"><TimesIcon aria-hidden="true" /></button>
          </li>
        </ul>
        <IngredientPicker @pick="(ing) => (state = addIngredient(state, ing))" />

        <h2>Các bước</h2>
        <ol class="steps">
          <li v-for="(s, i) in state.steps" :key="stepKey(s)">
            <span class="no num" aria-hidden="true">{{ i + 1 }}</span>
            <textarea v-model="s.content" rows="3" maxlength="2000" :aria-label="`Bước ${i + 1}`" />
            <ImageUpload v-model="s.media" clearable />
            <div class="step-actions">
              <button type="button" class="btn icon" :disabled="i === 0" aria-label="Lên" @click="state = moveStep(state, i, i - 1)"><ArrowUpIcon aria-hidden="true" /></button>
              <button type="button" class="btn icon" :disabled="i === state.steps.length - 1" aria-label="Xuống"
                @click="state = moveStep(state, i, i + 1)"><ArrowDownIcon aria-hidden="true" /></button>
              <button type="button" class="btn icon" aria-label="Xóa bước" @click="removeAt(state.steps, i)"><TimesIcon aria-hidden="true" /></button>
            </div>
          </li>
        </ol>
        <button type="button" class="btn add" @click="state.steps.push({ content: '', media: null })"><PlusIcon aria-hidden="true" />Thêm bước</button>
      </fieldset>

      <div v-if="error" class="form-error" role="alert">
        <p>{{ error }}</p>
        <ul v-if="Object.keys(fieldErrors).length">
          <li v-for="(msg, f) in fieldErrors" :key="f">{{ fieldLabel(state, String(f)) }}: {{ msg }}</li>
        </ul>
      </div>
      <p v-if="saved" class="form-ok">Đã lưu.</p>
      <div v-if="!readOnly" class="actions">
        <template v-if="published">
          <button type="button" class="btn primary" :disabled="busy" @click="saveAndSubmit">Lưu và gửi duyệt lại</button>
        </template>
        <template v-else>
          <button type="button" class="btn" :disabled="busy" @click="save">Lưu nháp</button>
          <button type="button" class="btn primary" :disabled="busy" @click="saveAndSubmit">Gửi duyệt</button>
        </template>
      </div>
    </form>
  </section>
</template>

<style scoped>
.editor { max-width: 900px; margin: 0.5rem auto 0; }
.head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.5rem 1rem; }
.head h1 { margin-bottom: 0.5rem; }
fieldset { border: 0; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 1rem; min-width: 0; }
.warn { padding: 0.6rem 0 0.6rem; border-top: 1px dashed var(--accent); border-bottom: 1px dashed var(--accent); color: var(--accent); font-weight: 600; }
.lines input, .lines select, .steps textarea {
  font: inherit; min-height: 44px; padding: 0.55rem 0.7rem; border: 1px solid var(--field); border-radius: var(--radius); background: #fff; color: var(--text);
}
.row { display: grid; gap: 0.75rem; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); }
.tags { display: flex; flex-wrap: wrap; gap: 0 1rem; font-weight: 400; }
.tag { display: inline-flex; align-items: center; gap: 0.4rem; min-height: 44px; }
h2 { font-weight: 800; font-size: 1.5rem; margin: 1rem 0 0; padding-top: 0.75rem; border-top: 1px solid var(--border); }
.lines { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; }
.lines li { display: grid; gap: 0.5rem; grid-template-columns: 1fr 90px 130px; align-items: center; padding: 0.5rem 0; border-bottom: 1px solid var(--border); }
.lines .name { font-weight: 600; }
.lines li input[aria-label='Ghi chú'] { grid-column: 1 / 3; }
@media (min-width: 720px) { .lines li { grid-template-columns: 1.3fr 90px 130px 1fr auto; } .lines li input[aria-label='Ghi chú'] { grid-column: auto; } }
.steps { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; }
.steps li { display: grid; grid-template-columns: 2.75rem 1fr; gap: 0.5rem 0.75rem; padding: 0.75rem 0; border-bottom: 1px solid var(--border); }
.steps li > :not(.no) { grid-column: 2; }
.no { grid-row: 1 / span 3; font-family: var(--font-display); font-weight: 900; font-size: 2.5rem; line-height: 0.9; color: var(--accent); }
.step-actions { display: flex; gap: 0.4rem; }
.btn svg { width: 0.9rem; height: 0.9rem; }
.add { align-self: flex-start; }
.actions {
  position: sticky; bottom: 0; display: flex; flex-wrap: wrap; gap: 0.75rem; justify-content: flex-end;
  margin: 1rem -1.25rem 0; padding: 0.9rem 1.25rem; background: var(--surface); border-top: 1px solid var(--border);
}
@media (min-width: 720px) { .actions { margin-inline: -2rem; padding-inline: 2rem; } }
.form-error p { margin: 0; }
</style>
