<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { deleteMyRating, getMyRating, hideRating, listRatings, putMyRating, type Rating } from '@/api/community'
import { messageFor } from '@/api/messages'
import { useForm } from '@/composables/useForm'
import { useAuthStore } from '@/stores/auth'
import StarRating from './StarRating.vue'

const props = defineProps<{ recipeId: number; authorId: number }>()
const emit = defineEmits<{ changed: [] }>()
const auth = useAuthStore()
const ratings = ref<Rating[]>([])
const total = ref(0)
const loadError = ref('')
const stars = ref(0)
const review = ref('')
const { busy, error, run } = useForm()

const canRate = computed(() => !!auth.me && auth.can('COMMUNITY_PARTICIPATE') && auth.me.id !== props.authorId)
const mine = ref<Rating | null>(null)
const moderator = computed(() => auth.can('CONTENT_MODERATE'))

async function load() {
  loadError.value = ''
  try {
    // ponytail: chỉ trang đầu (20 đánh giá mới nhất); thêm "Xem thêm" khi cần.
    const [p, own] = await Promise.all([listRatings(props.recipeId), canRate.value ? getMyRating(props.recipeId).catch(() => null) : null])
    ratings.value = p.content
    total.value = p.page.totalElements
    mine.value = own
    stars.value = own?.stars ?? 0
    review.value = own?.review ?? ''
  } catch (e) {
    loadError.value = messageFor(e)
  }
}
watch(() => [props.recipeId, auth.me?.id], load, { immediate: true })

const after = async () => { await load(); emit('changed') }
const submit = () => stars.value && run(async () => { await putMyRating(props.recipeId, stars.value, review.value.trim()); await after() })
const remove = () => run(async () => { await deleteMyRating(props.recipeId); await after() })
const toggleHidden = (r: Rating) => run(async () => { await hideRating(r.id, !r.hidden); await after() })
</script>

<template>
  <section class="ratings" aria-labelledby="rate-title">
    <h2 id="rate-title">Đánh giá <span class="num count">{{ total }}</span></h2>

    <form v-if="canRate" class="form" @submit.prevent="submit">
      <StarRating v-model="stars" />
      <label class="field">Nhận xét (không bắt buộc)
        <textarea v-model="review" rows="3" maxlength="1000"></textarea>
      </label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <div class="row">
        <button class="btn primary" :disabled="busy || !stars">{{ mine ? 'Cập nhật đánh giá' : 'Gửi đánh giá' }}</button>
        <button v-if="mine" type="button" class="link-btn danger" :disabled="busy" @click="remove">Xóa đánh giá</button>
      </div>
    </form>
    <p v-else-if="!auth.me" class="hint"><RouterLink to="/login">Đăng nhập</RouterLink> để chấm điểm công thức này.</p>
    <p v-else-if="auth.me.id === props.authorId" class="hint">Bạn là tác giả nên không tự chấm điểm được.</p>

    <p v-if="loadError" class="form-error">{{ loadError }}</p>
    <p v-else-if="!ratings.length" class="hint">Chưa có đánh giá nào.</p>
    <ul class="list">
      <li v-for="r in ratings" :key="r.id" :class="{ hidden: r.hidden }">
        <div class="meta">
          <strong>{{ r.author.displayName }}</strong>
          <StarRating :model-value="r.stars" readonly />
          <span v-if="r.hidden" class="stamp" data-status="HIDDEN">Đã ẩn</span>
          <button v-if="moderator" type="button" class="link-btn" :disabled="busy" @click="toggleHidden(r)">{{ r.hidden ? 'Hiện' : 'Ẩn' }}</button>
        </div>
        <p v-if="r.review" class="text">{{ r.review }}</p>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.ratings { margin-top: 2.5rem; }
h2 { font-weight: 800; font-size: 1.5rem; margin: 0 0 0.5rem; }
.count { color: var(--accent); }
.row { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
.list { list-style: none; padding: 0; margin: 1rem 0 0; border-top: 1px solid var(--border); }
.list li { padding: 0.6rem 0; border-bottom: 1px solid var(--border); }
.meta { display: flex; align-items: center; flex-wrap: wrap; gap: 0.25rem 0.75rem; }
.text { margin: 0.25rem 0 0; white-space: pre-line; }
.hidden .text { color: var(--muted); }
</style>
