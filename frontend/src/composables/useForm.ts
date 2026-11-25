import { ref } from 'vue'
import { ApiError } from '@/api/http'
import { messageFor } from '@/api/messages'

/** Trạng thái chung của form: đang gửi, lỗi chung, lỗi theo ô, mã lỗi. */
export function useForm() {
  const busy = ref(false)
  const error = ref('')
  const code = ref<string>()
  const fieldErrors = ref<Record<string, string>>({})

  async function run(fn: () => Promise<unknown>): Promise<boolean> {
    busy.value = true
    error.value = ''
    code.value = undefined
    fieldErrors.value = {}
    try {
      await fn()
      return true
    } catch (e) {
      if (e instanceof ApiError) {
        code.value = e.code
        fieldErrors.value = e.fieldErrors ?? {}
      }
      error.value = messageFor(e)
      return false
    } finally {
      busy.value = false
    }
  }

  return { busy, error, code, fieldErrors, run }
}
