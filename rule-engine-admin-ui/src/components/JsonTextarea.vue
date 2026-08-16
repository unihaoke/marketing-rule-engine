<script setup>
import { ref, watch } from 'vue'

/**
 * JSON 文本域：实时校验 JSON 合法性，通过 valid 事件上报。
 */
const props = defineProps({
  modelValue: { type: String, default: '{}' },
  placeholder: { type: String, default: '请输入 JSON 对象，如 {"key": "value"}' },
  rows: { type: Number, default: 4 },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'valid'])

const text = ref(props.modelValue ?? '{}')
const error = ref('')

watch(
  () => props.modelValue,
  (v) => {
    text.value = v ?? '{}'
  }
)

watch(text, (v) => {
  const trimmed = String(v).trim()
  if (!trimmed) {
    error.value = ''
    emit('valid', true)
    emit('update:modelValue', v)
    return
  }
  try {
    JSON.parse(trimmed)
    error.value = ''
    emit('valid', true)
  } catch (e) {
    error.value = `JSON 格式错误：${e.message}`
    emit('valid', false)
  }
  emit('update:modelValue', v)
})
</script>

<template>
  <div style="width: 100%">
    <el-input
      v-model="text"
      type="textarea"
      :rows="rows"
      :placeholder="placeholder"
      :disabled="disabled"
      class="mono"
    />
    <div v-if="error" style="color: #f56c6c; font-size: 12px; line-height: 1.5; margin-top: 2px">
      {{ error }}
    </div>
  </div>
</template>
