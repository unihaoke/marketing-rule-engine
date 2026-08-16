<script setup>
import { ref, watch } from 'vue'
import { VALUE_TYPES } from '../utils/condition'

/**
 * 参数 Schema 动态行编辑器：code / name / type / required / description / defaultValue(可选) / frontDisplay(可选)
 * 用于事件入参、函数入参、动作入参的编辑，支持增删行。
 *
 * withFrontDisplay 仅用于动作参数：控制该参数是否在规则画布动作配置中展示给运营填写。
 */
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  withDefault: { type: Boolean, default: false },
  withFrontDisplay: { type: Boolean, default: false },
  placeholderCode: { type: String, default: '如 adSlotId' }
})

const emit = defineEmits(['update:modelValue'])

const rows = ref(cloneRows(props.modelValue))

/**
 * 外部值同步：仅当外部值与内部行内容不一致时重置内部行，
 * 避免"内部编辑 → emit → 外部回写 → 重置"的更新循环（此前的 bug 导致添加参数无效/递归更新）。
 */
watch(
  () => props.modelValue,
  (v) => {
    if (JSON.stringify(cloneRows(v)) !== JSON.stringify(rows.value)) {
      rows.value = cloneRows(v)
    }
  }
)

watch(
  rows,
  () => {
    emit('update:modelValue', rows.value.map((r) => ({ ...r })))
  },
  { deep: true }
)

function cloneRows(list) {
  return (list || []).map((r) => ({
    code: r.code ?? '',
    name: r.name ?? '',
    type: r.type ?? 'STRING',
    required: !!r.required,
    description: r.description ?? '',
    defaultValue: r.defaultValue ?? null,
    frontDisplay: r.frontDisplay !== false
  }))
}

function addRow() {
  rows.value.push({
    code: '',
    name: '',
    type: 'STRING',
    required: false,
    description: '',
    defaultValue: null,
    frontDisplay: true
  })
}

function removeRow(i) {
  rows.value.splice(i, 1)
}
</script>

<template>
  <div>
    <el-table :data="rows" size="small" border>
      <el-table-column label="编码" min-width="130">
        <template #default="{ row }">
          <el-input v-model="row.code" size="small" :placeholder="placeholderCode" />
        </template>
      </el-table-column>
      <el-table-column label="名称" min-width="110">
        <template #default="{ row }">
          <el-input v-model="row.name" size="small" placeholder="如 广告位ID" />
        </template>
      </el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-select v-model="row.type" size="small" style="width: 100%">
            <el-option v-for="t in VALUE_TYPES" :key="t" :label="t" :value="t" />
            <el-option label="JSON" value="JSON" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="必填" width="70" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.required" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="描述" min-width="120">
        <template #default="{ row }">
          <el-input v-model="row.description" size="small" placeholder="说明" />
        </template>
      </el-table-column>
      <el-table-column v-if="withDefault" label="默认值" min-width="110">
        <template #default="{ row }">
          <el-input v-model="row.defaultValue" size="small" placeholder="默认值" />
        </template>
      </el-table-column>
      <el-table-column v-if="withFrontDisplay" label="前端展示" width="100" align="center">
        <template #default="{ row }">
          <el-select v-model="row.frontDisplay" size="small" style="width: 100%">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="60" align="center">
        <template #default="{ $index }">
          <el-button size="small" type="danger" link @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 8px">
      <el-button size="small" type="primary" plain @click="addRow">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>添加参数
      </el-button>
    </div>
  </div>
</template>
