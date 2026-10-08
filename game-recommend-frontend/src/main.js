import Vue from 'vue';
import App from './App.vue';
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import './styles/design-tokens.css';
import axios from 'axios';
import router from './router';

Vue.config.productionTip = false;

// 使用 Element UI (保留作为后备组件库)
Vue.use(ElementUI);

// 配置 Axios — 性能优化
axios.defaults.baseURL = '/api';
axios.defaults.timeout = 120000;

// 请求拦截：添加时间戳防缓存
axios.interceptors.request.use(config => {
  // 对 GET 请求添加时间戳避免缓存
  if (config.method === 'get') {
    config.params = { ...config.params, _t: Date.now() };
  }
  return config;
});

// 响应拦截：统一错误处理
axios.interceptors.response.use(
  response => response,
  error => {
    // 网络错误统一处理（后端未启动等）
    if (!error.response) {
      console.warn('[API] 后端服务不可达:', error.message);
    } else {
      console.error('[API] 请求失败:', error.response.status, error.response.data);
    }
    return Promise.reject(error);
  }
);

Vue.prototype.$axios = axios;

// 全局 Element UI 配置
Vue.prototype.$ELEMENT = {
  size: 'small',
  zIndex: 2000
};

// 全局错误处理
Vue.config.errorHandler = (err, vm, info) => {
  console.error(`[Vue Error] ${info}:`, err);
  // 生产环境可以上报到监控系统
};

// ==================== 认证工具 ====================
const isAuthenticated = () => {
  try {
    const auth = localStorage.getItem('gamerec-auth');
    if (!auth) return false;
    const data = JSON.parse(auth);
    return !!(data && data.username);
  } catch (e) {
    return false;
  }
};

// ==================== 路由守卫 ====================
router.beforeEach((to, from, next) => {
  // 登录页面无需认证
  if (to.meta && to.meta.noAuth) {
    // 如果已登录，访问登录页则重定向到首页
    if (isAuthenticated() && to.path === '/login') {
      next('/');
      return;
    }
    next();
    return;
  }

  // 其他页面需要认证
  if (!isAuthenticated()) {
    // 未登录则跳转到登录页，传递目标路径以便登录后跳回
    next({ path: '/login', query: { redirect: to.fullPath } });
    return;
  }

  next();
});

// 页面标题更新
router.afterEach(to => {
  const title = to.meta && to.meta.title;
  document.title = title ? `${title} - GameRec` : 'GameRec - 基于玩家画像的游戏推荐系统';
});

new Vue({
  router,
  render: h => h(App)
}).$mount('#app');
