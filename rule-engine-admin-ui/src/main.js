import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as Icons from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import './assets/main.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 全局注册 Element Plus 图标组件
for (const [name, comp] of Object.entries(Icons)) {
  if (name === 'default') continue
  app.component(name, comp)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} · 营销规则引擎` : '营销规则引擎 · 运营后台'
})

app.mount('#app')
