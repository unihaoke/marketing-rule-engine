<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listEvents, createEvent, updateEvent, deleteEvent, setEventEnabled } from '../api/modules'
import ParamSchemaEditor from '../components/ParamSchemaEditor.vue'
import EventSimulator from '../components/EventSimulator.vue'
import { formatTime } from '../utils'

const loading = ref(false)
const rows = ref([])

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)
const form = ref(emptyForm())

/** 模拟触发弹窗 */
const simVisible = ref(false)
const simEventCode = ref('')
const simEvent = ref(null)

function openSimulate(row) {
  simEventCode.value = row.eventCode
  simEvent.value = row // 直接传入完整事件定义，免二次拉取
  simVisible.value = true
}

function emptyForm() {
  return {
    eventCode: '',
    eventName: '',
    description: '',
    enabled: true,
    params: []
  }
}

function openCreate() {
  isEdit.value = false
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = {
    eventCode: row.eventCode,
    eventName: row.eventName || '',
    description: row.description || '',
    enabled: row.enabled !== false,
    params: (row.params || []).map((p) => ({ ...p }))
  }
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  try {
    rows.value = (await listEvents()) || []
  } catch (e) {
    ElMessage.error(`事件列表加载失败：${e.message}`)
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.value.eventCode.trim()) {
    ElMessage.warning('请填写事件编码')
    return
  }
  if (!form.value.eventName.trim()) {
    ElMessage.warning('请填写事件名称')
    return
  }
  dialogLoading.value = true
  try {
    if (isEdit.value) {
      await updateEvent(form.value.eventCode, form.value)
      ElMessage.success('事件更新成功')
    } else {
      await createEvent(form.value)
      ElMessage.success('事件创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(`保存失败：${e.message}`)
  } finally {
    dialogLoading.value = false
  }
}

async function onToggleEnabled(row, val) {
  try {
    await setEventEnabled(row.eventCode, val)
    ElMessage.success(val ? '事件已启用' : '事件已停用')
  } catch (e) {
    row.enabled = !val
    ElMessage.error(`操作失败：${e.message}`)
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除事件「${row.eventCode}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteEvent(row.eventCode)
    ElMessage.success('事件已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) {
      ElMessage.error(`删除失败：${e.message}`)
    }
  }
}

onMounted(load)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="table-toolbar" style="margin-bottom: 0">
        <span style="font-weight: 600">事件管理</span>
        <el-button type="primary" @click="openCreate">
          <el-icon style="margin-right: 4px"><Plus /></el-icon>新建事件
        </el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="eventCode" label="事件编码" min-width="140" />
      <el-table-column prop="eventName" label="事件名称" min-width="140" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="入参数量" width="100" align="center">
        <template #default="{ row }">{{ (row.params || []).length }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled !== false"
            :loading="loading"
            @change="(val) => onToggleEnabled(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建人" width="110">
        <template #default="{ row }">{{ row.createdBy || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" link @click="openSimulate(row)">
            <el-icon style="margin-right: 2px"><CaretRight /></el-icon>模拟触发
          </el-button>
          <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" link @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 新建 / 编辑弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑事件' : '新建事件'"
    width="860px"
    :close-on-click-modal="false"
    destroy-on-close
  >    <el-form :model="form" label-width="90px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="事件编码" required>
            <el-input v-model="form.eventCode" :disabled="isEdit" placeholder="如 AD_CLICK" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="事件名称" required>
            <el-input v-model="form.eventName" placeholder="如 广告点击" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="事件说明" />
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
      <el-form-item label="入参定义">
        <div style="width: 100%">
          <ParamSchemaEditor v-model="form.params" with-default />
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="dialogLoading" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <!-- 模拟触发弹窗（事件管理内测试效果） -->
  <el-dialog
    v-model="simVisible"
    :title="`事件模拟触发${simEvent ? ' · ' + simEvent.eventName : ''}`"
    width="1100px"
    top="6vh"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <EventSimulator v-if="simVisible" :event-code="simEventCode" :event="simEvent" />
  </el-dialog>
</template>
