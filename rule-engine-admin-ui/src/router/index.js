import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { title: '概览' }
  },
  {
    path: '/events',
    name: 'Events',
    component: () => import('../views/Events.vue'),
    meta: { title: '事件管理' }
  },
  {
    path: '/rules',
    name: 'Rules',
    component: () => import('../views/Rules.vue'),
    meta: { title: '规则管理' }
  },
  {
    path: '/rules/canvas/:ruleCode',
    name: 'RuleCanvas',
    component: () => import('../views/RuleCanvas.vue'),
    meta: { title: '规则画布' }
  },
  {
    path: '/functions',
    name: 'Functions',
    component: () => import('../views/Functions.vue'),
    meta: { title: '函数管理' }
  },
  {
    path: '/actions',
    name: 'Actions',
    component: () => import('../views/Actions.vue'),
    meta: { title: '动作配置' }
  },
  {
    path: '/versions',
    name: 'Versions',
    component: () => import('../views/Versions.vue'),
    meta: { title: '版本&灰度' }
  },
  {
    path: '/logs',
    name: 'Logs',
    component: () => import('../views/Logs.vue'),
    meta: { title: '执行日志' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
