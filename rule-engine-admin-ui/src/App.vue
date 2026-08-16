<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const menus = [
  { path: '/', title: '概览', icon: 'Odometer' },
  { path: '/events', title: '事件管理', icon: 'Memo' },
  { path: '/rules', title: '规则管理', icon: 'SetUp' },
  { path: '/functions', title: '函数管理', icon: 'MagicStick' },
  { path: '/actions', title: '动作配置', icon: 'Operation' },
  { path: '/versions', title: '版本&灰度', icon: 'Tickets' },
  { path: '/logs', title: '执行日志', icon: 'Document' }
]

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/rules/canvas')) return '/rules'
  return p
})

const pageTitle = computed(() => route.meta?.title || '营销规则引擎')
</script>

<template>
  <el-container class="app-layout">
    <el-aside width="220px" class="app-aside">
      <div class="app-logo">
        <div class="app-logo-title">营销规则引擎</div>
        <div class="app-logo-sub">MARKETING RULE ENGINE</div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        class="app-menu"
      >
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="app-header-title">{{ pageTitle }}</div>
        <div class="app-header-right">
          <el-tag type="success" effect="dark" size="small">运营后台</el-tag>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  height: 100%;
}

.app-aside {
  background-color: #001529;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-logo {
  height: 64px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.app-logo-title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
}

.app-logo-sub {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 2px;
  margin-top: 4px;
}

.app-menu {
  border-right: none;
  flex: 1;
}

.app-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.app-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.app-main {
  background-color: #f0f2f5;
  padding: 16px;
  overflow: auto;
}
</style>
