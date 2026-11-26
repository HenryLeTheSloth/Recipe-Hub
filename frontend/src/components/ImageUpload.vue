<script setup lang="ts">
import { ref } from 'vue'
import { uploadImage, type MediaRef } from '@/api/media'
import { messageFor } from '@/api/messages'

const MAX_BYTES = 5 * 1024 * 1024

defineProps<{ modelValue: MediaRef | null; clearable?: boolean }>()
// MediaRef sau khi tải lên; null khi bấm "Bỏ ảnh" (chỉ có lúc clearable).
const emit = defineEmits<{ 'update:modelValue': [MediaRef | null] }>()

const busy = ref(false)
const error = ref('')

async function onChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  error.value = ''
  // Chặn sớm ở client; server vẫn kiểm tra lại cả kích thước lẫn loại ảnh.
  if (file.size > MAX_BYTES) {
    error.value = 'Ảnh tối đa 5MB'
    return
  }
  busy.value = true
  try {
    emit('update:modelValue', await uploadImage(file))
  } catch (err) {
    error.value = messageFor(err)
  } finally {
    busy.value = false
    input.value = ''
  }
}
</script>

<template>
  <div class="image-upload">
    <img v-if="modelValue" :src="modelValue.url" alt="Ảnh đã chọn" class="preview" />
    <label class="btn pick">
      {{ busy ? 'Đang tải lên…' : modelValue ? 'Đổi ảnh' : 'Chọn ảnh' }}
      <input type="file" accept="image/jpeg,image/png,image/webp" :disabled="busy" class="visually-hidden" @change="onChange" />
    </label>
    <button v-if="clearable && modelValue && !busy" type="button" class="link-btn danger" @click="emit('update:modelValue', null)">Bỏ ảnh</button>
    <small class="hint">JPEG, PNG hoặc WebP, tối đa 5MB.</small>
    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
  </div>
</template>

<style scoped>
.image-upload { display: flex; flex-direction: column; align-items: flex-start; gap: 0.5rem; }
.preview { width: 112px; height: 84px; object-fit: cover; border: 1px solid var(--rule); }
.pick { position: relative; cursor: pointer; }
.pick:focus-within { outline: 2px solid var(--blue); outline-offset: 2px; }
.visually-hidden { position: absolute; width: 1px; height: 1px; opacity: 0; overflow: hidden; }
</style>
