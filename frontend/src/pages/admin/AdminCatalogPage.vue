<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import { deleteNamed, listNamed, saveNamed, type NamedItem, type NamedKind } from '@/api/catalog'
import { useForm } from '@/composables/useForm'
import { slugify } from '@/utils/slugify'

// Một trang cho cả danh mục và tag: cùng luật tên/slug, danh mục có thêm thứ tự.
const props = defineProps<{ kind: NamedKind }>()
const isCategory = computed(() => props.kind === 'categories')
const noun = computed(() => (isCategory.value ? 'danh mục' : 'tag'))

const items = ref<NamedItem[]>([])
const page = useForm()

async function load() {
  await page.run(async () => (items.value = await listNamed(props.kind)))
}
watch(() => props.kind, load, { immediate: true })

const dialog = ref(false)
const editing = ref<NamedItem | null>(null)
const form = ref({ name: '', sortOrder: 0 })
const save = useForm()

function open(item: NamedItem | null) {
  editing.value = item
  form.value = { name: item?.name ?? '', sortOrder: item && 'sortOrder' in item ? item.sortOrder : 0 }
  save.error.value = ''
  save.fieldErrors.value = {}
  dialog.value = true
}

async function submit() {
  const body = isCategory.value ? form.value : { name: form.value.name }
  if (await save.run(() => saveNamed(props.kind, editing.value?.id ?? null, body))) {
    dialog.value = false
    await load()
  }
}

async function remove(item: NamedItem) {
  if (!window.confirm(`Xóa ${noun.value} "${item.name}"?`)) return
  if (await page.run(() => deleteNamed(props.kind, item.id))) await load()
}
</script>

<template>
  <section>
    <header class="bar">
      <h1>{{ isCategory ? 'Danh mục' : 'Tag' }}</h1>
      <Button :label="`Thêm ${noun}`" icon="pi pi-plus" @click="open(null)" />
    </header>
    <Message v-if="page.error.value" severity="error" :closable="false">{{ page.error.value }}</Message>

    <DataTable :value="items" data-key="id" :loading="page.busy.value">
      <Column field="name" header="Tên" />
      <Column header="Slug"><template #body="{ data }"><code>{{ data.slug }}</code></template></Column>
      <Column v-if="isCategory" field="sortOrder" header="Thứ tự" />
      <Column header="">
        <template #body="{ data }">
          <div class="actions">
            <Button :aria-label="`Sửa ${data.name}`" label="Sửa" size="small" severity="secondary" @click="open(data)" />
            <Button :aria-label="`Xóa ${data.name}`" label="Xóa" size="small" severity="danger" text @click="remove(data)" />
          </div>
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="dialog" modal :header="editing ? `Sửa ${noun}` : `Thêm ${noun}`"
            :style="{ width: 'min(440px, 95vw)' }">
      <form class="form" novalidate @submit.prevent="submit">
        <label class="field">
          Tên
          <InputText v-model="form.name" maxlength="50" />
          <small v-if="save.fieldErrors.value.name" class="field-error">{{ save.fieldErrors.value.name }}</small>
          <small v-else class="hint">Slug: <code>{{ slugify(form.name) || '—' }}</code> (thêm -2, -3… nếu trùng)</small>
        </label>
        <label v-if="isCategory" class="field">
          Thứ tự (nhỏ đứng trước)
          <InputNumber v-model="form.sortOrder" :use-grouping="false" />
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
.bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.bar h1 { margin: 0; }
.actions { display: flex; gap: 0.5rem; }
.actions.end { justify-content: flex-end; }
</style>
