<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRules,
  getRule,
  listVersions,
  getVersionContent,
  rollbackVersion,
  setGray
} from '../api/modules'
import GrayConfigForm from '../components/GrayConfigForm.vue'
import { formatTime } from '../utils'

const route = useRoute()

const ruleOptions = ref([])
const rulesLoading = ref(false)
const selectedRuleCode = ref('')

const versionsLoading = ref(false)
const versions = ref([])

const grayVisible = ref(false)
const grayLoading = ref(false)
const grayForm = ref(emptyGray())

const contentVisible = ref(false)
const contentLoading = ref(false)
const contentText = ref('')

const rollbackVisible = ref(false)
const rollbackLoading = ref(false)
const rollbackTarget = ref(null)
const rollbackForm = ref({ changeLog: '', operator: 'admin' })

function emptyGray() {
  return { enabled: false, strategy: 'OFF', percent: 0, channels: [], bucketKey: 'userId' }
}

const statusMap = {
  DRAFT: { text: '草稿', type: 'info' },
  PUBLISHED: { text: '线上', type: 'success' },
  OFFLINE: { text: '已下线', type: 'warning' },
  ARCHIVED: { text: '归档', type: 'info' }
}

function statusOf(row) {
  return statusMap[row.status] || { text: row.status || '-', type: 'info' }
}

async function loadRules() {
  rulesLoading.value = true
  try {
    ruleOptions.value = (await listRules()) || []
    const queryCode = route.query.ruleCode
    if (queryCode && ruleOptions.value.some((r) => r.ruleCode === queryCode)) {
      selectedRuleCode.value = String(queryCode)
    } else if (!selectedRuleCode.value && ruleOptions.value.length) {
      selectedRuleCode.value = ruleOptions.value[0].ruleCode
    }
  } catch (e) {
    ElMessage.error(`规则列表加载失败：${e.message}`)
  } finally {
    rulesLoading.value = false
  }
}

async function loadVersions() {
  if (!selectedRuleCode.value) {
    versions.value = []
    return
  }
  versionsLoading.value = true
  try {
    versions.value = (await listVersions(selectedRuleCode.value)) || []
    // 同步当前灰度配置（用于灰度弹窗默认值）
    try {
      const rule = await getRule(selectedRuleCode.value)
      grayForm.value = {
        enabled: !!rule.gray?.enabled,
        strategy: rule.gray?.strategy || 'OFF',
        percent: rule.gray?.percent ?? 0,
        channels: rule.gray?.channels || [],
        bucketKey: rule.gray?.bucketKey || 'userId'
      }
    } catch (e) {
      // 忽略：规则详情失败时灰度弹窗使用默认配置
    }
  } catch (e) {
    ElMessage.error(`版本列表加载失败：${e.message}`)
  } finally {
    versionsLoading.value = false
  }
}

watch(selectedRuleCode, () => {
  if (selectedRuleCode.value) loadVersions()
})

async function openContent(row) {
  contentVisible.value = true
  contentLoading.value = true
  contentText.value = ''
  try {
    const data = await getVersionContent(row.id)
    contentText.value = typeof data === 'string' ? data : JSON.stringify(data ?? {}, null, 2)
  } catch (e) {
    contentText.value = `版本内容加载失败：${e.message}`
  } finally {
    contentLoading.value = false
  }
}

function openRollback(row) {
  rollbackTarget.value = row
  rollbackForm.value = { changeLog: '', operator: 'admin' }
  rollbackVisible.value = true
}

async function submitRollback() {
  rollbackLoading.value = true
  try {
    await rollbackVersion(rollbackTarget.value.id, {
      changeLog: rollbackForm.value.changeLog,
      operator: rollbackForm.value.operator
    })
    ElMessage.success(`版本 v${rollbackTarget.value.versionNo} 回溯发布成功`)
    rollbackVisible.value = false
    await loadVersions()
  } catch (e) {
    ElMessage.error(`回溯失败：${e.message}`)
  } finally {
    rollbackLoading.value = false
  }
}

async function submitGray() {
  grayLoading.value = true
  try {
    await setGray(selectedRuleCode.value, grayForm.value)
    ElMessage.success('灰度配置已保存并即时生效')
    grayVisible.value = false
  } catch (e) {
    ElMessage.error(`灰度配置失败：${e.message}`)
  } finally {
    grayLoading.value = false
  }
}

async function onClearVersions() {
  try {
    await ElMessageBox.confirm('确认清空当前选择？', '提示', {
      type: 'info',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    selectedRuleCode.value = ''
    versions.value = []
  } catch (e) {
    // 取消
  }
}

onMounted(loadRules)
</script>

<template>
  <div>
    <el-card shadow="never" class="page-card">
      <div class="table-toolbar" style="margin-bottom: 0">
        <div style="display: flex; align-items: center; gap: 10px">
          <span style="font-weight: 600">选择规则</span>
          <el-select
            v-model="selectedRuleCode"
            :loading="rulesLoading"
            placeholder="请选择规则"
            filterable
            style="width: 320px"
          >
            <el-option
              v-for="r in ruleOptions"
              :key="r.ruleCode"
              :label="`${r.ruleName}（${r.ruleCode}）`"
              :value="r.ruleCode"
            />
          </el-select>
          <el-button type="info" plain @click="onClearVersions">清空</el-button>
        </div>
        <el-button
          type="warning"
          plain
          :disabled="!selectedRuleCode"
          @click="grayVisible = true"
        >
          <el-icon style="margin-right: 4px"><Setting /></el-icon>灰度配置
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <span style="font-weight: 600">
          版本列表
          <span v-if="selectedRuleCode" style="color: #909399; font-size: 12px">
            · {{ selectedRuleCode }}
          </span>
        </span>
      </template>

      <el-table v-loading="versionsLoading" :data="versions" border stripe>
        <el-table-column label="版本号" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">v{{ row.versionNo }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusOf(row).type" size="small">{{ statusOf(row).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeLog" label="变更说明" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.changeLog || '-' }}</template>
        </el-table-column>
        <el-table-column prop="publishedBy" label="发布人" width="110">
          <template #default="{ row }">{{ row.publishedBy || '-' }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatTime(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openContent(row)">内容</el-button>
            <el-button
              size="small"
              type="warning"
              link
              :disabled="row.status !== 'PUBLISHED' && row.status !== 'OFFLINE'"
              @click="openRollback(row)"
            >
              回溯
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="!versionsLoading && !versions.length"
        :description="selectedRuleCode ? '该规则暂无版本记录' : '请先选择规则'"
        :image-size="70"
      />
    </el-card>

    <!-- 版本内容 -->
    <el-dialog v-model="contentVisible" title="版本内容（画布回显）" width="720px">
      <div v-loading="contentLoading">
        <pre class="content-view">{{ contentText }}</pre>
      </div>
      <template #footer>
        <el-button @click="contentVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 回溯发布 -->
    <el-dialog v-model="rollbackVisible" title="版本回溯（重新发布）" width="480px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 14px"
        :title="rollbackTarget ? `将版本 v${rollbackTarget.versionNo} 重新发布为线上版本` : ''"
      />
      <el-form :model="rollbackForm" label-width="90px">
        <el-form-item label="变更说明">
          <el-input v-model="rollbackForm.changeLog" type="textarea" :rows="2" placeholder="回溯变更说明" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="rollbackForm.operator" placeholder="操作人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rollbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="rollbackLoading" @click="submitRollback">确认回溯</el-button>
      </template>
    </el-dialog>

    <!-- 灰度配置 -->
    <el-dialog v-model="grayVisible" title="灰度配置" width="520px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 14px"
        title="灰度配置保存后即时生效，可对当前线上规则逐步放量。"
      />
      <GrayConfigForm v-model="grayForm" />
      <template #footer>
        <el-button @click="grayVisible = false">取消</el-button>
        <el-button type="primary" :loading="grayLoading" @click="submitGray">保存灰度配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.content-view {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  max-height: 420px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
}
</style>
