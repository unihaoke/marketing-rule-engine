<script setup>
/**
 * ECharts 封装组件：传入 option 即渲染，option 变化自动更新，窗口缩放自适应。
 */
import * as echarts from 'echarts'
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  /** ECharts option */
  option: { type: Object, required: true },
  /** 图表高度 */
  height: { type: String, default: '320px' }
})

const el = ref(null)
let chart = null

function render() {
  if (!chart) return
  chart.setOption(props.option || {}, true)
}

function resize() {
  chart && chart.resize()
}

watch(() => props.option, render, { deep: true })

onMounted(() => {
  chart = echarts.init(el.value)
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<template>
  <div ref="el" :style="{ width: '100%', height }" />
</template>
