<script setup>
import { computed } from 'vue'

/**
 * 灰度配置表单（v-model 绑定 GrayConfig 对象）：
 * { enabled, strategy: OFF|PERCENT|CHANNEL|PERCENT_AND_CHANNEL, percent, channels, bucketKey }
 */
const props = defineProps({
  modelValue: { type: Object, required: true }
})

const emit = defineEmits(['update:modelValue'])

const strategies = [
  { value: 'OFF', label: '关闭（全量放量）' },
  { value: 'PERCENT', label: '按百分比' },
  { value: 'CHANNEL', label: '按渠道' },
  { value: 'PERCENT_AND_CHANNEL', label: '百分比 + 渠道' }
]

const enabled = computed({
  get: () => !!props.modelValue.enabled,
  set: (v) => emit('update:modelValue', { ...props.modelValue, enabled: v })
})
const strategy = computed({
  get: () => props.modelValue.strategy || 'OFF',
  set: (v) => emit('update:modelValue', { ...props.modelValue, strategy: v })
})
const percent = computed({
  get: () => props.modelValue.percent ?? 0,
  set: (v) => emit('update:modelValue', { ...props.modelValue, percent: v == null ? 0 : v })
})
const bucketKey = computed({
  get: () => props.modelValue.bucketKey || 'userId',
  set: (v) => emit('update:modelValue', { ...props.modelValue, bucketKey: v })
})
const channelsText = computed({
  get: () => (props.modelValue.channels || []).join(','),
  set: (v) =>
    emit('update:modelValue', {
      ...props.modelValue,
      channels: String(v).split(',').map((s) => s.trim()).filter(Boolean)
    })
})

const usePercent = computed(() => ['PERCENT', 'PERCENT_AND_CHANNEL'].includes(strategy.value))
const useChannel = computed(() => ['CHANNEL', 'PERCENT_AND_CHANNEL'].includes(strategy.value))
</script>

<template>
  <el-form label-width="90px" size="default">
    <el-form-item label="灰度开关">
      <el-switch v-model="enabled" />
      <span style="margin-left: 8px; color: #909399; font-size: 12px">
        {{ enabled ? '已开启灰度' : '全量放量' }}
      </span>
    </el-form-item>
    <el-form-item label="灰度策略">
      <el-select v-model="strategy" style="width: 100%">
        <el-option v-for="s in strategies" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
    </el-form-item>
    <el-form-item label="灰度比例">
      <el-input-number
        v-model="percent"
        :min="0"
        :max="100"
        :disabled="!usePercent"
        style="width: 180px"
      />
      <span style="margin-left: 8px; color: #909399; font-size: 12px">%</span>
    </el-form-item>
    <el-form-item label="渠道白名单">
      <el-input
        v-model="channelsText"
        :disabled="!useChannel"
        placeholder="多个渠道用英文逗号分隔，如 AD-ZHITONG, WECHAT"
      />
    </el-form-item>
    <el-form-item label="分桶键">
      <el-input v-model="bucketKey" placeholder="默认 userId，也可用 channelId 等" />
    </el-form-item>
  </el-form>
</template>
