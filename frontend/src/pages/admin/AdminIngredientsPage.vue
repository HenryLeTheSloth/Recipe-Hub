<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable, { type DataTablePageEvent } from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import {
  adminListIngredients, deleteIngredient, GROUP_LABELS, updateIngredient,
  type Ingredient, type IngredientGroup, type IngredientStatus,
} from '@/api/catalog'
import { useForm } from '@/composables/useForm'

const GROUPS = Object.entries(GROUP_LABELS) as [IngredientGroup, string][]

const items = ref<Ingredient[]>([])
const total = ref(0)
const status = ref<IngredientStatus | ''>('')
const q = ref('')
const pageNo = ref(0)
const size = ref(20)
const page = useForm()

async function load() {
  await page.run(async () => {
    const res = await adminListIngredients(status.value, q.value.trim(), pageNo.value, size.value)
    items.value = res.content
    total.value = res.page.totalElements
  })
}
onMounted(load)

function search() {
  pageNo.value = 0
  load()
}
function onPage(e: DataTablePageEvent) {
  pageNo.value = e.page
  size.value = e.rows
  load()
}

const input = (i: Ingredient, s: IngredientStatus) => ({ name: i.name, group: i.group, emoji: i.emoji, status: s })

async function approve(i: Ingredient) {
  if (await page.run(() => updateIngredient(i.id, input(i, 'APPROVED')))) await load()
}

async function remove(i: Ingredient) {
  if (!window.confirm(`Xóa nguyên liệu "${i.name}"?`)) return
  if (await page.run(() => deleteIngredient(i.id))) return load()
  // GĐ3 có chức năng gộp; xóa chỉ dành cho nguyên liệu chưa dùng ở đâu.
  if (page.code.value === 'IN_USE') page.error.value = 'Nguyên liệu đang được dùng, hãy gộp thay vì xóa'
}

const dialog = ref(false)
const editing = ref<Ingredient | null>(null)
const form = ref({ name: '', group: 'OTHER' as IngredientGroup, emoji: '' as string | null, status: 'PENDING' as IngredientStatus })
const save = useForm()

function open(i: Ingredient) {
  editing.value = i
  form.value = input(i, i.status)
  save.error.value = ''
  save.fieldErrors.value = {}
  dialog.value = true
}

async function submit() {
  if (await save.run(() => updateIngredient(editing.value!.id, form.value))) {
    dialog.value = false
    await load()
  }
}
</script>

<template>
  <section>
    <header class="bar">
      <h1>Nguyên liệu</h1>
      <form class="search" @submit.prevent="search">
        <select v-model="status" class="native" aria-label="Lọc trạng thái" @change="search">
          <option value="">Tất cả</option>
          <option value="PENDING">Chờ duyệt</option>
          <option value="APPROVED">Đã duyệt</option>
        </select>
        <InputText v-model="q" placeholder="Tìm (gõ có hoặc không dấu)" aria-label="Tìm nguyên liệu" />
        <Button type="submit" label="Tìm" severity="secondary" />
      </form>
    </header>
    <Message v-if="page.error.value" severity="error" :closable="false">{{ page.error.value }}</Message>

    <DataTable
      :value="items" data-key="id" lazy paginator :rows="size" :total-records="total" :first="pageNo * size"
      :rows-per-page-options="[20, 50]" :loading="page.busy.value" @page="onPage"
    >
      <Column header="Nguyên liệu">
        <template #body="{ data }">
          <span class="emoji" aria-hidden="true">{{ data.emoji ?? '·' }}</span> {{ data.name }}
          <div class="muted"><code>{{ data.slug }}</code></div>
        </template>
      </Column>
      <Column header="Nhóm">
        <template #body="{ data }">{{ GROUP_LABELS[data.group as IngredientGroup] }}</template>
      </Column>
      <Column header="Trạng thái">
        <template #body="{ data }">
          <Tag :value="data.status === 'PENDING' ? 'Chờ duyệt' : 'Đã duyệt'"
               :severity="data.status === 'PENDING' ? 'warn' : 'success'" />
        </template>
      </Column>
      <Column header="">
        <template #body="{ data }">
          <div class="actions">
            <Button v-if="data.status === 'PENDING'" :aria-label="`Duyệt ${data.name}`" label="Duyệt" size="small"
                    @click="approve(data)" />
            <Button :aria-label="`Sửa ${data.name}`" label="Sửa" size="small" severity="secondary" @click="open(data)" />
            <Button :aria-label="`Xóa ${data.name}`" label="Xóa" size="small" severity="danger" text @click="remove(data)" />
          </div>
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="dialog" modal :header="`Sửa ${editing?.name ?? ''}`" :style="{ width: 'min(480px, 95vw)' }">
      <form class="form" novalidate @submit.prevent="submit">
        <label class="field">
          Tên
          <InputText v-model="form.name" />
          <small v-if="save.fieldErrors.value.name" class="field-error">{{ save.fieldErrors.value.name }}</small>
        </label>
        <label class="field">
          Nhóm
          <select v-model="form.group" class="native">
            <option v-for="[g, label] in GROUPS" :key="g" :value="g">{{ label }}</option>
          </select>
        </label>
        <label class="field">
          Emoji
          <InputText v-model="form.emoji" maxlength="16" />
        </label>
        <label class="field">
          Trạng thái
          <select v-model="form.status" class="native">
            <option value="PENDING">Chờ duyệt</option>
            <option value="APPROVED">Đã duyệt</option>
          </select>
        </label>
        <Message v-if="save.error.value" severity="error" :closable="false">{{ save.error.value }}</Message>
        <div class="actions end">
          <Button type="button" label="Hủy" severity="secondary" text @click="dialog = false" />
          <Button type="submit" label="Lưu" :loading="save.busy.value" />
        </div>
      </form>
    </Dialog>
  </section>
</template>

<style scoped>
.bar { display: flex; flex-wrap: wrap; gap: 0.75rem; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.bar h1 { margin: 0; }
.search { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.actions { display: flex; gap: 0.5rem; }
.actions.end { justify-content: flex-end; }
.emoji { font-size: 1.2rem; }
.muted { color: var(--muted); font-size: 0.85rem; }
.native {
  padding: 0.5rem 0.6rem; border: 1px solid var(--border); border-radius: var(--radius);
  background: var(--surface); color: var(--text); font: inherit;
}
.native:focus { outline: 2px solid var(--accent); outline-offset: 1px; }
</style>
