import Vue from 'vue';
import VueRouter from 'vue-router';

Vue.use(VueRouter);

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '用户登录', noAuth: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/UserProfile.vue'),
    meta: { title: '玩家画像' }
  },
  {
    path: '/recommend',
    name: 'Recommend',
    component: () => import('@/views/Recommend.vue'),
    meta: { title: '推荐结果' }
  },
  {
    path: '/analysis',
    name: 'Analysis',
    component: () => import('@/views/Analysis.vue'),
    meta: { title: '数据分析' }
  },
  {
    path: '/evaluation',
    name: 'Evaluation',
    component: () => import('@/views/Evaluation.vue'),
    meta: { title: '算法评估' }
  },
  {
    path: '/table',
    name: 'RefactorTable',
    component: () => import('@/views/GameSearch.vue'),
    meta: { title: '数据表格' }
  },
  {
    path: '/search',
    name: 'RefactorSearch',
    component: () => import('@/views/Search.vue'),
    meta: { title: '游戏搜索' }
  },
  {
    path: '/games',
    name: 'GameSearch',
    component: () => import('@/views/GameSearch.vue'),
    meta: { title: '游戏搜索' }
  },
  {
    path: '/game/:gameId',
    name: 'GameDetail',
    component: () => import('@/views/GameDetail.vue'),
    meta: { title: '游戏详情' }
  },
  {
    path: '/data-import',
    name: 'DataImport',
    component: () => import('@/views/DataImport.vue'),
    meta: { title: '数据导入' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { title: '系统设置' }
  }
];

const router = new VueRouter({
  mode: 'hash',
  routes
});

export default router;
