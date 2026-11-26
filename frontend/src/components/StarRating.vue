<script setup lang="ts">
const props = defineProps<{ modelValue: number; readonly?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [v: number] }>()

function set(v: number) {
  if (!props.readonly && v >= 1 && v <= 5 && v !== props.modelValue) emit('update:modelValue', v)
}
function onKey(e: KeyboardEvent) {
  const d = e.key === 'ArrowRight' || e.key === 'ArrowUp' ? 1 : e.key === 'ArrowLeft' || e.key === 'ArrowDown' ? -1 : 0
  if (!d) return
  e.preventDefault()
  set(Math.min(5, Math.max(1, (props.modelValue || 0) + d)))
  // Roving tabindex: giữ tiêu điểm theo sao mới chọn.
  const radios = (e.currentTarget as HTMLElement).querySelectorAll<HTMLElement>('[role=radio]')
  radios[Math.min(5, Math.max(1, (props.modelValue || 0) + d)) - 1]?.focus()
}
</script>

<template>
  <div class="stars" role="radiogroup" aria-label="Điểm đánh giá" :aria-readonly="readonly || undefined" @keydown="onKey">
    <span
      v-for="n in 5" :key="n" role="radio" :aria-checked="modelValue === n" :aria-label="`${n} sao`"
      :tabindex="readonly ? -1 : modelValue === n || (!modelValue && n === 1) ? 0 : -1"
      :class="{ on: n <= modelValue, ro: readonly }" @click="set(n)"
    ><span aria-hidden="true">★</span></span>
  </div>
</template>

<style scoped>
.stars { display: inline-flex; }
.stars [role='radio'] { display: inline-flex; align-items: center; justify-content: center; min-width: 44px; min-height: 44px; font-size: 1.75rem; line-height: 1; color: var(--rule); cursor: pointer; }
.stars .on { color: var(--red); }
.stars .ro { min-width: 0; min-height: 0; font-size: 1.1rem; cursor: default; }
.stars [role='radio']:focus-visible { outline: 2px solid var(--blue); outline-offset: -2px; }
</style>
