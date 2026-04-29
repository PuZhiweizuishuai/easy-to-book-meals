import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './app.css'
import HttpFetch from './utils/fetch'

const app = createApp(App)

app.config.globalProperties.SERVER_API_URL = '/api'

console.log('%c不挂高数出品 https://www.buguagaoshu.com', 'color: #1976d2;font-size:2em')
console.log('%cPowered by buguagaosh', 'color: #1976d2;font-size:1em')

app.use(createPinia())
app.use(router)
app.use(HttpFetch)
app.mount('#app')
