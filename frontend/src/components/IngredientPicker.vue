<script setup lang="ts">
import { ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import { GROUP_LABELS, listIngredients, suggestIngredient, type Ingredient, type IngredientGroup } from '@/api/catalog'
import { messageFor } from '@/api/messages'

const emit = defineEmits<{ pick: [Ingredient] }>()

const q = ref('')
const results = ref<Ingredient[]>([])
const searched = ref(false)
const dialog = ref(false)
const group = ref<IngredientGroup>('OTHER')
const error = ref('')
let timer: ReturnType<typeof setTimeout> | undefined

watch(q, (v) => {
  clearTimeout(timer)
  searched.value = false
  if (!v.trim()) return void (results.value = [])
  timer = setTimeout(async () => {
    try {
      results.value = await listIngredients(v.trim())
    } catch {
      results.value = []
    }
    searched.value = true
  }, 250)
})

function pick(i: Ingredient) {
  emit('pick', i)
  q.value = ''
}

async function suggest() {
  error.value = ''
  try {
    pick(await suggestIngredient({ name: q.value.trim(), group: group.value }))
    dialog.value = false
  } catch (e) {
    error.value = messageFor(e)
  }
}
</script>

<template>
  <div class="picker">
    <input v-model="q" type="search" placeholder="Tìm nguyên liệu để thêm…" aria-label="Tìm nguyên liệu" />
    <ul v-if="q.trim() && (results.length || searched)" class="results" role="listbox">
      <li v-for="i in results" :key="i.id">
        <button type="button" @click="pick(i)">{{ i.emoji }} {{ i.name }}</button>
      </li>
      <li v-if="searched && !results.length">
        <button type="button" @click="dialog = true">Thêm “{{ q.trim() }}” mới</button>
      </li>
    </ul>
    <Dialog v-model:visible="dialog" modal header="Đề xuất nguyên liệu mới" :style="{ width: 'min(420px, 95vw)' }">
      <form class="form" @submit.prevent="suggest">
        <p>“{{ q.trim() }}” sẽ chờ quản trị viên duyệt, nhưng bạn dùng được ngay.</p>
        <label class="field">Nhóm
          <select v-model="group">
            <option v-for="(label, g) in GROUP_LABELS" :key="g" :value="g">{{ label }}</option>
          </select>
        </label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="btn primary">Thêm</button>
      </form>
    </Dialog>
  </div>
</template>

<style scoped>
.picker { position: relative; }
input { font: inherit; width: 100%; min-height: 44px; padding: 0.55rem 0.75rem; border: 1px dashed var(--ink); border-radius: var(--radius); background: #fff; color: var(--ink); }
.results {
  position: absolute; z-index: 5; left: 0; right: 0; margin: 0.25rem 0 0; padding: 0; list-style: none;
  background: var(--paper); border: 1px solid var(--ink); border-top: 4px solid var(--red); border-radius: 0; max-height: 280px; overflow: auto;
}
.results li { border-bottom: 1px solid var(--rule); }
.results button { font: inherit; width: 100%; min-height: 44px; text-align: left; padding: 0.45rem 0.75rem; border: 0; background: none; color: var(--ink); cursor: pointer; }
.results button:hover, .results button:focus { background: var(--red); color: #fff; }
</style>
