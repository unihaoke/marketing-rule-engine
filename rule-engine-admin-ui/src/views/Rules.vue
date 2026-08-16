<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRules,
  createRule,
  publishRule,
  onlineRule,
  offlineRule,
  setGray,
  listEvents
} from '../api/modules'
import GrayConfigForm from '../components/GrayConfigForm.vue'
import { formatTime } from '../utils'

const router = useRouter()

const loading = ref(false)
const rows = ref([])
const eventOptions = ref([])

// 新建规则弹窗
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = ref({
  ruleCode: '',
  ruleName: '',
  eventCode: '',
  description: '',
  priority: 100,
  enabled: false
})

// 发布弹窗
const publishVisible = ref(false)
const publishLoading = ref(false)
const publishTarget = ref(null)
const publishForm = ref({ changeLog: '', operator: 'admin' })

// 灰度弹窗
const grayVisible = ref(false)
const grayLoading = ref(false)
const grayTarget = ref(null)
const grayForm = ref(emptyGray())

function emptyGray() {
  return { enabled: false, strategy: 'OFF', percent: 0, channels: [], bucketKey: 'userId' }
}

function statusOf(row) {
  if (row.status === 'OFFLINE') return { text: '已下线', type: 'info' }
  if (row.status === 'PUBLISHED' || row.enabled) return { text: '线上运行', type: 'success' }
  return { text: '草稿', type: 'warning' }
}

async function load() {
  loading.value = true
  try {
    rows.value = (await listRules()) || []
  } catch (e) {
    ElMessage.error(`规则列表加载失败：${e.message}`)
  } finally {
    loading.value = false
  }
}

async function loadEvents() {
  try {
    eventOptions.value = (await listEvents()) || []
  } catch (e) {
    eventOptions.value = []
  }
}

function openCreate() {
  createForm.value = {
    ruleCode: '',
    ruleName: '',
    eventCode: '',
    description: '',
    priority: 100,
    enabled: false
  }
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.value.ruleCode.trim()) {
    ElMessage.warning('请填写规则编码')
    return
  }
  if (!createForm.value.ruleName.trim()) {
    ElMessage.warning('请填写规则名称')
    return
  }
  if (!createForm.value.eventCode) {
    ElMessage.warning('请选择绑定事件')
    return
  }
  createLoading.value = true
  try {
    await createRule({ ...createForm.value, priority: Number(createForm.value.priority) || 100 })
    ElMessage.success('规则创建成功（已生成初始草稿版本 v1）')
    createVisible.value = false
    await load()
    router.push(`/rules/canvas/${createForm.value.ruleCode}`)
  } catch (e) {
    ElMessage.error(`创建失败：${e.message}`)
  } finally {
    createLoading.value = false
  }
}

function openPublish(row) {
  publishTarget.value = row
  publishForm.value = { changeLog: '', operator: 'admin' }
  publishVisible.value = true
}

async function submitPublish() {
  publishLoading.value = true
  try {
    await publishRule(publishTarget.value.ruleCode, {
      changeLog: publishForm.value.changeLog,
      operator: publishForm.value.operator
    })
    ElMessage.success('发布成功，新版本已上线')
    publishVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(`发布失败：${e.message}`)
  } finally {
    publishLoading.value = false
  }
}

async function onOnline(row) {
  try {
    await onlineRule(row.ruleCode)
    ElMessage.success('规则已上线')
    await load()
  } catch (e) {
    ElMessage.error(`上线失败：${e.message}`)
  }
}

async function onOffline(row) {
  try {
    await ElMessageBox.confirm(`确定下线规则「${row.ruleName}」吗？下线后线上不再生效。`, '下线确认', {
      type: 'warning',
      confirmButtonText: '下线',
      cancelButtonText: '取消'
    })
    await offlineRule(row.ruleCode)
    ElMessage.success('规则已下线')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) {
      ElMessage.error(`下线失败：${e.message}`)
    }
  }
}

function openGray(row) {
  grayTarget.value = row
  grayForm.value = {
    enabled: !!row.gray?.enabled,
    strategy: row.gray?.strategy || 'OFF',
    percent: row.gray?.percent ?? 0,
    channels: row.gray?.channels || [],
    bucketKey: row.gray?.bucketKey || 'userId'
  }
  grayVisible.value = true
}

async function submitGray() {
  grayLoading.value = true
  try {
    await setGray(grayTarget.value.ruleCode, grayForm.value)
    ElMessage.success('灰度配置已保存并即时生效')
    grayVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(`灰度配置失败：${e.message}`)
  } finally {
    grayLoading.value = false
  }
}

function goCanvas(row) {
  router.push(`/rules/canvas/${row.ruleCode}`)
}

function goVersions(row) {
  router.push({ path: '/versions', query: { ruleCode: row.ruleCode } })
}

onMounted(() => {
  load()
  loadEvents()
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="table-toolbar" style="margin-bottom: 0">
        <span style="font-weight: 600">规则管理</span>
        <el-button type="primary" @click="openCreate">
          <el-icon style="margin-right: 4px"><Plus /></el-icon>新建规则
        </el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="ruleCode" label="规则编码" min-width="170" />
      <el-table-column prop="ruleName" label="规则名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="eventCode" label="绑定事件" width="120" />
      <el-table-column prop="priority" label="优先级" width="90" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusOf(row).type" size="small">{{ statusOf(row).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本" width="80" align="center">
        <template #default="{ row }">
          {{ row.versionNo ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="330" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="goCanvas(row)">编辑</el-button>
          <el-button size="small" type="success" link @click="openPublish(row)">发布</el-button>
          <el-button
            v-if="row.enabled"
            size="small"
            type="warning"
            link
            @click="onOffline(row)"
          >
            下线
          </el-button>
          <el-button v-else size="small" type="success" link @click="onOnline(row)">上线</el-button>
          <el-button size="small" type="info" link @click="openGray(row)">灰度</el-button>
          <el-button size="small" link @click="goVersions(row)">版本</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 新建规则 -->
  <el-dialog v-model="createVisible" title="新建规则" width="560px" :close-on-click-modal="false">
    <el-form :model="createForm" label-width="90px">
      <el-form-item label="规则编码" required>
        <el-input v-model="createForm.ruleCode" placeholder="如 SIGN_IN_STREAK_REWARD" />
      </el-form-item>
      <el-form-item label="规则名称" required>
        <el-input v-model="createForm.ruleName" placeholder="如 签到连续打卡返积分" />
      </el-form-item>
      <el-form-item label="绑定事件" required>
        <el-select v-model="createForm.eventCode" placeholder="选择事件" style="width: 100%">
          <el-option
            v-for="ev in eventOptions"
            :key="ev.eventCode"
            :label="`${ev.eventName}（${ev.eventCode}）`"
            :value="ev.eventCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级">
        <el-input-number v-model="createForm.priority" :min="0" :max="99999" />
        <span style="margin-left: 8px; color: #909399; font-size: 12px">数值越小优先级越高</span>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="规则说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="createLoading" @click="submitCreate">
        创建并进入编辑
      </el-button>
    </template>
  </el-dialog>

  <!-- 发布 -->
  <el-dialog v-model="publishVisible" title="发布为线上版本" width="480px" :close-on-click-modal="false">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 14px"
      title="发布后将生成新的线上版本并立即生效，请确认画布草稿内容。"
    />
    <el-form :model="publishForm" label-width="90px">
      <el-form-item label="变更说明">
        <el-input v-model="publishForm.changeLog" type="textarea" :rows="2" placeholder="本次发布变更内容" />
      </el-form-item>
      <el-form-item label="操作人">
        <el-input v-model="publishForm.operator" placeholder="发布人" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="publishVisible = false">取消</el-button>
      <el-button type="primary" :loading="publishLoading" @click="submitPublish">确认发布</el-button>
    </template>
  </el-dialog>

  <!-- 灰度配置 -->
  <el-dialog v-model="grayVisible" title="灰度配置" width="520px" :close-on-click-modal="false">
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 14px"
      title="灰度配置保存后即时生效，可用于线上规则逐步放量。"
    />
    <GrayConfigForm v-model="grayForm" />
    <template #footer>
      <el-button @click="grayVisible = false">取消</el-button>
      <el-button type="primary" :loading="grayLoading" @click="submitGray">保存灰度配置</el-button>
    </template>
  </el-dialog>
</template>
