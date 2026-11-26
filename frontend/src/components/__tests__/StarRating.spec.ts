import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import StarRating from '../StarRating.vue'

describe('StarRating', () => {
  it('changes with arrow keys and clamps at 1', async () => {
    const w = mount(StarRating, { props: { modelValue: 3 }, attachTo: document.body })
    expect(w.find('[role=radiogroup]').exists()).toBe(true)
    expect(w.findAll('[role=radio]')[2]!.attributes('aria-label')).toBe('3 sao')
    await w.find('[role=radiogroup]').trigger('keydown', { key: 'ArrowRight' })
    expect(w.emitted('update:modelValue')![0]).toEqual([4])
    await w.setProps({ modelValue: 1 })
    await w.find('[role=radiogroup]').trigger('keydown', { key: 'ArrowLeft' })
    expect(w.emitted('update:modelValue')).toHaveLength(1)
    w.unmount()
  })

  it('readonly ignores clicks', async () => {
    const w = mount(StarRating, { props: { modelValue: 2, readonly: true } })
    await w.findAll('[role=radio]')[4]!.trigger('click')
    expect(w.emitted('update:modelValue')).toBeUndefined()
  })
})
