<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Column from 'primevue/column'
import DataTable, { type DataTablePageEvent } from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import { listRoles, listUsers, setUserLocked, setUserRoles, type AdminUser, type Role } from '@/api/admin'
import { useForm } from '@/composables/useForm'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const canManage = auth.can('USER_MANAGE')
// GET /admin/roles cần ROLE_MANAGE, nên đổi vai trò cần cả hai quyền.
const canAssignRoles = canManage && auth.can('ROLE_MANAGE')

const users = ref<AdminUser[]>([])
const total = ref(0)
const q = ref('')
const pageNo = ref(0)
const size = ref(12)
const page = useForm()

async function load() {
  await page.run(async () => {
    const res = await listUsers(q.value.trim(), pageNo.value, size.value)
    users.value = res.content
    total.value = res.page.totalElements
  })
}
onMounted(load)

function onPage(e: DataTablePageEvent) {
  pageNo.value = e.page
  size.value = e.rows
  load()
}

function search() {
  pageNo.value = 0
  load()
}

function replace(u: AdminUser) {
  users.value = users.value.map((x) => (x.id === u.id ? u : x))
}

async function toggleLock(u: AdminUser) {
  if (u.locked || window.confirm(`Khóa ${u.email}? Người này bị đăng xuất ngay.`)) {
    await page.run(async () => replace(await setUserLocked(u.id, !u.locked)))
  }
}

const dialog = ref(false)
const target = ref<AdminUser | null>(null)
const allRoles = ref<Role[]>([])
const chosen = ref<string[]>([])
const save = useForm()

async function openRoles(u: AdminUser) {
  target.value = u
  chosen.value = [...u.roles]
  save.error.value = ''
  dialog.value = true
  if (!allRoles.value.length) await save.run(async () => (allRoles.value = await listRoles()))
}

async function saveRoles() {
  const u = target.value!
  if (await save.run(async () => replace(await setUserRoles(u.id, chosen.value)))) dialog.value = false
}
</script>

<template>
  <section>
    <header class="bar">
      <h1>Người dùng</h1>
      <form class="search" @submit.prevent="search">
        <InputText v-model="q" placeholder="Email, tên đăng nhập, tên hiển thị" aria-label="Tìm người dùng" />
        <Button type="submit" label="Tìm" severity="secondary" />
      </form>
    </header>
    <Message v-if="page.error.value" severity="error" :closable="false">{{ page.error.value }}</Message>

    <DataTable
      :value="users" data-key="id" lazy paginator :rows="size" :total-records="total" :first="pageNo * size"
      :rows-per-page-options="[12, 25, 50]" :loading="page.busy.value" @page="onPage"
    >
      <Column header="Người dùng">
        <template #body="{ data }">
          <div>{{ data.displayName }} <span class="muted">@{{ data.username }}</span></div>
          <div class="muted">{{ data.email }}</div>
        </template>
      </Column>
      <Column header="Vai trò">
        <template #body="{ data }">
          <Tag v-for="r in data.roles" :key="r" :value="r" severity="secondary" class="mr" />
        </template>
      </Column>
      <Column header="Trạng thái">
        <template #body="{ data }">
          <Tag v-if="data.locked" value="Đã khóa" severity="danger" />
          <Tag v-else-if="!data.emailVerified" value="Chưa xác thực" severity="warn" />
          <Tag v-else value="Hoạt động" severity="success" />
        </template>
      </Column>
      <Column header="Ngày tạo">
        <template #body="{ data }">{{ new Date(data.createdAt).toLocaleDateString('vi-VN') }}</template>
      </Column>
      <Column v-if="canManage" header="">
        <template #body="{ data }">
          <div class="actions">
            <Button
              :label="data.locked ? 'Mở khóa' : 'Khóa'" size="small" :severity="data.locked ? 'secondary' : 'danger'"
              text :disabled="data.id === auth.me?.id" @click="toggleLock(data)"
            />
            <Button
              v-if="canAssignRoles" label="Vai trò" size="small" severity="secondary"
              :disabled="data.id === auth.me?.id" @click="openRoles(data)"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="dialog" modal :header="`Vai trò của ${target?.email}`" :style="{ width: 'min(480px, 95vw)' }">
      <form class="form" @submit.prevent="saveRoles">
        <div v-for="r in allRoles" :key="r.code" class="perm">
          <Checkbox v-model="chosen" :value="r.code" :input-id="`role-${r.code}`" />
          <label :for="`role-${r.code}`"><code>{{ r.code }}</code> — {{ r.name }}</label>
        </div>
        <p class="hint">Người dùng sẽ bị đăng xuất để nhận quyền mới.</p>
        <Message v-if="save.error.value" severity="error" :closable="false">{{ save.error.value }}</Message>
        <div class="actions end">
          <Button type="button" label="Hủy" severity="secondary" text @click="dialog = false" />
          <Button type="submit" label="Lưu" :loading="save.busy.value" :disabled="!chosen.length" />
        </div>
      </form>
    </Dialog>
  </section>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; margin-bottom: 1rem; }
.bar h1 { margin: 0; }
.search { display: flex; gap: 0.5rem; }
.search :deep(input) { min-width: 260px; }
.muted { color: var(--muted); font-size: 0.875rem; }
.mr { margin-right: 0.25rem; }
.actions { display: flex; gap: 0.5rem; }
.actions.end { justify-content: flex-end; }
.perm { display: flex; gap: 0.5rem; align-items: center; }
</style>
