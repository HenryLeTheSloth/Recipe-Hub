<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import {
  createRole, deleteRole, listPermissions, listRoles, updateRole,
  type PermissionInfo, type Role, type RoleInput,
} from '@/api/admin'
import { useForm } from '@/composables/useForm'

const roles = ref<Role[]>([])
const permissions = ref<PermissionInfo[]>([])
const page = useForm()

const dialog = ref(false)
const editing = ref<Role | null>(null)
const form = ref<RoleInput>({ code: '', name: '', description: null, permissionCodes: [] })
const save = useForm()
const lockedPerms = computed(() => editing.value?.code === 'ADMIN')

async function load() {
  await page.run(async () => {
    ;[roles.value, permissions.value] = await Promise.all([listRoles(), listPermissions()])
  })
}
onMounted(load)

function open(role: Role | null) {
  editing.value = role
  form.value = role
    ? { name: role.name, description: role.description, permissionCodes: [...role.permissionCodes] }
    : { code: '', name: '', description: null, permissionCodes: [] }
  save.error.value = ''
  save.fieldErrors.value = {}
  dialog.value = true
}

async function submit() {
  const ok = await save.run(() =>
    editing.value ? updateRole(editing.value.id, form.value) : createRole(form.value),
  )
  if (ok) {
    dialog.value = false
    await load()
  }
}

async function remove(role: Role) {
  if (!window.confirm(`Xóa vai trò ${role.code}?`)) return
  if (await page.run(() => deleteRole(role.id))) await load()
}
</script>

<template>
  <section>
    <header class="bar">
      <h1>Vai trò</h1>
      <Button label="Tạo vai trò" icon="pi pi-plus" @click="open(null)" />
    </header>
    <Message v-if="page.error.value" severity="error" :closable="false">{{ page.error.value }}</Message>

    <DataTable :value="roles" data-key="id" :loading="page.busy.value">
      <Column field="code" header="Mã">
        <template #body="{ data }">
          <code>{{ data.code }}</code>
          <Tag v-if="data.system" value="hệ thống" severity="secondary" class="ml" />
        </template>
      </Column>
      <Column field="name" header="Tên" />
      <Column header="Quyền">
        <template #body="{ data }">{{ data.permissionCodes.length }}</template>
      </Column>
      <Column field="userCount" header="Người dùng" />
      <Column header="">
        <template #body="{ data }">
          <div class="actions">
            <Button :aria-label="`Sửa ${data.code}`" label="Sửa" size="small" severity="secondary" @click="open(data)" />
            <Button
              :aria-label="`Xóa ${data.code}`" label="Xóa" size="small" severity="danger" text
              :disabled="data.system" @click="remove(data)"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="dialog" modal :header="editing ? `Sửa vai trò ${editing.code}` : 'Tạo vai trò'"
            :style="{ width: 'min(560px, 95vw)' }">
      <form class="form" novalidate @submit.prevent="submit">
        <label v-if="!editing" class="field">
          Mã
          <InputText v-model.trim="form.code" placeholder="VD: EDITOR" />
          <small v-if="save.fieldErrors.value.code" class="field-error">{{ save.fieldErrors.value.code }}</small>
          <small v-else class="hint">IN HOA, số hoặc _, 3–31 ký tự. Không đổi được sau khi tạo.</small>
        </label>
        <label class="field">
          Tên
          <InputText v-model="form.name" />
          <small v-if="save.fieldErrors.value.name" class="field-error">{{ save.fieldErrors.value.name }}</small>
        </label>
        <label class="field">
          Mô tả
          <InputText v-model="form.description" />
        </label>
        <fieldset class="perms">
          <legend>Quyền</legend>
          <small v-if="lockedPerms" class="hint">Quyền của ADMIN cố định.</small>
          <div v-for="p in permissions" :key="p.code" class="perm">
            <Checkbox v-model="form.permissionCodes" :value="p.code" :input-id="`perm-${p.code}`" :disabled="lockedPerms" />
            <label :for="`perm-${p.code}`"><code>{{ p.code }}</code> {{ p.description }}</label>
          </div>
        </fieldset>
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
.ml { margin-left: 0.5rem; }
.perms { border: 1px solid var(--border); border-radius: var(--radius); padding: 0.75rem; display: grid; gap: 0.5rem; }
.perm { display: flex; gap: 0.5rem; align-items: center; }
</style>
