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
const published = computed(() => status.value === 'PUBLISHED')

watch(
  () => route.params.id as string | undefined,
  async (param) => {
    const n = param ? Number(param) : null
    if (n === id.value) return // vừa tạo xong rồi đổi URL: state đang đúng
    id.value = n
    status.value = null
    state.value = emptyState()
    if (!n) return
    try {
      const d = await getMyRecipe(n)
      state.value = fromDetail(d)
      status.value = d.status
    } catch (e) {
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
  <div class="editor">
    <h1>{{ id ? 'Sửa công thức' : 'Viết công thức mới' }}</h1>
    <p v-if="status" class="hint">Trạng thái: {{ STATUS_LABELS[status] }}</p>
    <p v-if="published" class="warn">Lưu thay đổi sẽ gửi công thức đi duyệt lại.</p>
    <p v-if="locked" class="warn">Công thức đang {{ status === 'PENDING' ? 'chờ duyệt' : 'bị ẩn' }} nên chưa sửa được.</p>

    <form class="form" @submit.prevent>
      <fieldset :disabled="locked || busy">
        <label class="field">Tên món
          <input v-model="state.title" name="title" maxlength="120" required />
        </label>
        <label class="field">Giới thiệu ngắn
          <textarea v-model="state.summary" maxlength="500" rows="2" />
        </label>
        <div class="row">
          <label class="field">Danh mục
            <select v-model="state.categoryId">
              <option :value="null">— Chọn —</option>
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
          <ImageUpload v-model="state.cover" />
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
            <button type="button" class="btn" :aria-label="`Bỏ ${l.ingredient.name}`" @click="removeAt(state.ingredients, i)">✕</button>
          </li>
        </ul>
        <IngredientPicker @pick="(ing) => (state = addIngredient(state, ing))" />

        <h2>Các bước</h2>
        <ol class="steps">
          <li v-for="(s, i) in state.steps" :key="i">
            <textarea v-model="s.content" rows="3" maxlength="2000" :aria-label="`Bước ${i + 1}`" />
            <ImageUpload v-model="s.media" />
            <div class="step-actions">
              <button type="button" class="btn" :disabled="i === 0" aria-label="Lên" @click="state = moveStep(state, i, i - 1)">↑</button>
              <button type="button" class="btn" :disabled="i === state.steps.length - 1" aria-label="Xuống"
                @click="state = moveStep(state, i, i + 1)">↓</button>
              <button type="button" class="btn" aria-label="Xóa bước" @click="removeAt(state.steps, i)">✕</button>
            </div>
          </li>
        </ol>
        <button type="button" class="btn" @click="state.steps.push({ content: '', media: null })">+ Thêm bước</button>
      </fieldset>

      <div v-if="error" class="form-error" role="alert">
        <p>{{ error }}</p>
        <ul v-if="Object.keys(fieldErrors).length">
          <li v-for="(msg, f) in fieldErrors" :key="f">{{ fieldLabel(state, String(f)) }}: {{ msg }}</li>
        </ul>
      </div>
      <p v-if="saved" class="form-ok">Đã lưu.</p>
      <div v-if="!locked" class="actions">
        <template v-if="published">
          <button type="button" class="btn primary" :disabled="busy" @click="saveAndSubmit">Lưu và gửi duyệt lại</button>
        </template>
        <template v-else>
          <button type="button" class="btn" :disabled="busy" @click="save">Lưu nháp</button>
          <button type="button" class="btn primary" :disabled="busy" @click="saveAndSubmit">Gửi duyệt</button>
        </template>
      </div>
    </form>
  </div>
</template>

<style scoped>
.editor { max-width: 860px; margin: 1.5rem auto; }
fieldset { border: 0; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.9rem; }
.warn { padding: 0.6rem 0.9rem; border: 1px solid var(--accent); border-radius: var(--radius); }
.field select, .field textarea, .lines input, .lines select, .steps textarea {
  font: inherit; padding: 0.55rem 0.7rem; border: 1px solid var(--border); border-radius: 8px; background: var(--bg); color: var(--text);
}
.row { display: grid; gap: 0.75rem; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); }
.tags { display: flex; flex-wrap: wrap; gap: 0.4rem 0.9rem; font-weight: 400; }
h2 { font-size: 1.15rem; margin: 0.5rem 0 0; }
.lines { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.5rem; }
.lines li { display: grid; gap: 0.5rem; grid-template-columns: 1fr 90px 130px; align-items: center; }
.lines li input[aria-label='Ghi chú'] { grid-column: 1 / 3; }
@media (min-width: 720px) { .lines li { grid-template-columns: 1.3fr 90px 130px 1fr auto; } .lines li input[aria-label='Ghi chú'] { grid-column: auto; } }
.steps { padding-left: 1.25rem; margin: 0; display: flex; flex-direction: column; gap: 1rem; }
.steps li { display: flex; flex-direction: column; gap: 0.5rem; }
.step-actions { display: flex; gap: 0.4rem; }
.step-actions .btn, .lines .btn { padding: 0.35rem 0.7rem; }
.actions { display: flex; gap: 0.75rem; justify-content: flex-end; }
.form-error p { margin: 0; }
</style>
