<template>
  <!-- 登录页面：独立全屏渲染，不带 App Shell -->
  <div v-if="isLoginPage" id="app" class="app-login-mode">
    <transition name="page" mode="out-in">
      <router-view :key="$route.fullPath" />
    </transition>
  </div>

  <!-- 主应用外壳 -->
  <div v-else id="app" class="app-shell">
    <!-- 顶部导航栏 -->
    <header class="app-header" :class="{ 'header--scrolled': headerScrolled }">
      <div class="header-left">
        <button class="hamburger-btn" @click="toggleMobileMenu" :aria-expanded="mobileMenuOpen" aria-label="切换导航菜单">
          <span class="hamburger-line" :class="{ open: mobileMenuOpen }"></span>
          <span class="hamburger-line" :class="{ open: mobileMenuOpen }"></span>
          <span class="hamburger-line" :class="{ open: mobileMenuOpen }"></span>
        </button>
        <router-link to="/" class="header-brand">
          <div class="brand-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none" aria-hidden="true">
              <defs><linearGradient id="brandGrad" x1="0" y1="0" x2="28" y2="28"><stop offset="0%" stop-color="var(--color-brand-400)"/><stop offset="100%" stop-color="var(--color-accent-500)"/></linearGradient></defs>
              <rect width="28" height="28" rx="8" fill="url(#brandGrad)" opacity="0.2"/>
              <path d="M14 5.6l8 4-8 4-8-4 8-4z" fill="url(#brandGrad)"/>
              <path d="M6 16.6l8 4 8-4" stroke="url(#brandGrad)" stroke-width="1.8" stroke-linecap="round"/>
              <path d="M6 12l8 4 8-4" stroke="url(#brandGrad)" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="brand-text">
            <span class="brand-name">GameRec</span>
            <span class="brand-dot">·</span>
            <span class="brand-subtitle">智能推荐</span>
          </div>
        </router-link>
      </div>
      <div class="header-actions">
        <span class="current-page-label">{{ currentPageLabel }}</span>
        <button class="header-btn theme-toggle" @click="toggleTheme" :title="isDarkTheme ? '切换为浅色主题' : '切换为深色主题'" aria-label="切换主题">
          <svg v-if="isDarkTheme" width="16" height="16" viewBox="0 0 16 16" fill="none">
            <circle cx="8" cy="8" r="3" stroke="currentColor" stroke-width="1.5"/>
            <path d="M8 1v2M8 13v2M1 8h2M13 8h2M2.93 2.93l1.41 1.41M11.66 11.66l1.41 1.41M2.93 13.07l1.41-1.41M11.66 4.34l1.41-1.41" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M12.5 10.5A6.5 6.5 0 015.5 3.5 6.5 6.5 0 0012.5 10.5z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="header-badge" title="系统状态">
          <span class="badge-dot badge-dot--live"></span>
          <span class="badge-text">运行中</span>
        </div>
        <button class="header-btn" title="刷新数据" @click="refreshData" aria-label="刷新数据">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" :class="{ 'spin': isRefreshing }">
            <path d="M2 8a6 6 0 0110.47-4M14 8a6 6 0 01-10.47 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M14 2v4h-4M2 14v-4h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
        <button class="header-btn header-btn--desktop" @click="handleLogout" title="退出登录" aria-label="退出登录">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M5 1.5H2.5A1 1 0 001.5 2.5v9A1 1 0 002.5 12.5H5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M5.5 7h7m0 0L10 4.5M12.5 7L10 9.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <button class="header-btn header-btn--desktop" @click="toggleSidebar" :title="sidebarCollapsed ? '展开菜单' : '收起菜单'" aria-label="切换侧边栏">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path v-if="sidebarCollapsed" d="M10 3L5 8l5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <path v-else d="M6 3l5 5-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
    </header>

    <div class="app-body">
      <!-- 侧边栏 — 折叠分组设计 -->
      <nav class="app-sidebar" :class="{ collapsed: sidebarCollapsed && !isMobile, 'mobile-open': mobileMenuOpen }" role="navigation" aria-label="主导航">
        <div class="sidebar-inner">
          <!-- 分组1：核心导航 -->
          <div class="sidebar-group" :class="{ 'sidebar-group--collapsed': collapsedGroups.nav }">
            <button class="sidebar-group-header" @click="toggleGroup('nav')">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none" class="group-chevron" :class="{ rotated: !collapsedGroups.nav }"><path d="M4 3l3 3-3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              <span class="group-header-label">主菜单</span>
              <span class="group-header-count">{{ navItems.length }}</span>
            </button>
            <div class="sidebar-group-body" v-show="!collapsedGroups.nav">
              <ul class="nav-list">
                <li v-for="item in navItems" :key="item.path">
                  <router-link :to="item.path" class="nav-item" :class="{ active: isActive(item.path) }" :title="item.label" @click.native="closeMobileMenu">
                    <span class="nav-icon" v-html="item.icon"></span>
                    <span class="nav-label">{{ item.label }}</span>
                  </router-link>
                </li>
              </ul>
            </div>
          </div>

          <!-- 分组2：分类导航 -->
          <div class="sidebar-group" :class="{ 'sidebar-group--collapsed': collapsedGroups.categories }">
            <button class="sidebar-group-header" @click="toggleGroup('categories')">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none" class="group-chevron" :class="{ rotated: !collapsedGroups.categories }"><path d="M4 3l3 3-3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              <span class="group-header-label">游戏分类</span>
              <span class="group-header-count">{{ categoryItems.length }}</span>
            </button>
            <div class="sidebar-group-body" v-show="!collapsedGroups.categories">
              <div class="sidebar-categories">
                <button v-for="cat in categoryItems" :key="cat.label" class="sidebar-cat-item" :title="cat.label" @click="navigateToGames(cat.label)">
                  <span class="cat-dot" :style="{ background: cat.color }"></span>
                  <span class="cat-label">{{ cat.label }}</span>
                </button>
              </div>
            </div>
          </div>

          <!-- 分组3：热门排行 -->
          <div class="sidebar-group" :class="{ 'sidebar-group--collapsed': collapsedGroups.hot }">
            <button class="sidebar-group-header" @click="toggleGroup('hot')">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none" class="group-chevron" :class="{ rotated: !collapsedGroups.hot }"><path d="M4 3l3 3-3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              <span class="group-header-label">热门排行</span>
              <span class="group-header-count" v-if="hotGames.length">{{ hotGames.length }}</span>
            </button>
            <div class="sidebar-group-body" v-show="!collapsedGroups.hot">
              <div class="sidebar-hot" v-if="hotGames.length > 0">
                <div v-for="(game, idx) in hotGames.slice(0, 5)" :key="game.gameId" class="sidebar-hot-item" @click="goGame(game.gameId)">
                  <span class="hot-rank" :class="{ 'hot-rank--top': idx < 3 }">{{ idx + 1 }}</span>
                  <span class="hot-name" :title="game.gameName">{{ game.gameName }}</span>
                  <span class="hot-score" v-if="game.hotScore">{{ formatShortNum(game.hotScore) }}</span>
                </div>
              </div>
              <div class="sidebar-hot-empty" v-else><span class="sidebar-empty-text">暂无数据</span></div>
            </div>
          </div>

          <!-- 底部：系统信息 -->
          <div class="sidebar-footer">
            <div class="sidebar-stats">
              <div class="sidebar-stat"><span class="stat-num">{{ stats.gameCount || '---' }}</span><span class="stat-label">游戏</span></div>
              <div class="sidebar-stat"><span class="stat-num">{{ stats.userCount || '---' }}</span><span class="stat-label">玩家</span></div>
              <div class="sidebar-stat"><span class="stat-num">{{ stats.ratingCount || '---' }}</span><span class="stat-label">评价</span></div>
            </div>
          </div>
        </div>
      </nav>

      <transition name="overlay-fade">
        <div v-if="mobileMenuOpen" class="mobile-overlay" @click="closeMobileMenu" @touchstart.prevent="closeMobileMenu"></div>
      </transition>

      <!-- 主内容区 -->
      <main class="app-main" id="main-content">
        <transition name="page" mode="out-in">
          <router-view :key="$route.fullPath" />
        </transition>
        <transition name="fade-up">
          <button v-if="showBackTop" class="back-top-btn" @click="scrollToTop" aria-label="返回顶部">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M9 15V3M9 3l-4.5 4.5M9 3l4.5 4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </transition>
      </main>

      <!-- AI 助手浮窗按钮 + 对话面板 -->
      <button
        class="ai-fab"
        :class="{ 'ai-fab--close': aiFloatingOpen }"
        @click="aiFloatingOpen = !aiFloatingOpen"
        :aria-label="aiFloatingOpen ? '关闭 AI 助手' : '打开 AI 助手'"
      >
        <svg v-if="aiFloatingOpen" width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <template v-else>
          <svg class="ai-fab-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M12 2l1.85 4.6L19 7.5l-3.7 3.3.95 5L12 13.6 7.75 15.8l.95-5L5 7.5l5.15-.9L12 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" fill="currentColor" fill-opacity="0.25"/>
            <circle cx="18" cy="5" r="1" fill="currentColor"/>
            <circle cx="5" cy="18" r="0.8" fill="currentColor"/>
            <circle cx="19" cy="17" r="0.6" fill="currentColor"/>
          </svg>
          <span class="ai-fab-label">AI 助手</span>
        </template>
      </button>
      <transition name="slide-up">
        <div v-if="aiFloatingOpen" class="ai-floating-panel">
          <AiAssistant mode="floating" context="system" @close="aiFloatingOpen = false" />
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
import { getOverview, getPopular } from '@/api/index';
import AiAssistant from '@/components/AiAssistant.vue';
export default {
  name: 'App',
  components: { AiAssistant },
  data() {
    return {
      systemStatus: '系统运行中',
      isRefreshing: false,
      sidebarCollapsed: false,
      mobileMenuOpen: false,
      isMobile: false,
      showBackTop: false,
      headerScrolled: false,
      isDarkTheme: false,
      scrollTimer: null,
      resizeTimer: null,
      hotGames: [],
      aiFloatingOpen: false,
      stats: { gameCount: '---', userCount: '---', ratingCount: '---' },
      collapsedGroups: { nav: false, categories: false, hot: false },
      navItems: [
        { path: '/', label: '首页概览', icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M3 8l7-5.25L17 8v8.25a.75.75 0 01-.75.75h-3.5v-4.5a.75.75 0 00-.75-.75H8a.75.75 0 00-.75.75V17H3.75A.75.75 0 013 16.25V8z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>' },
        { path: '/profile', label: '玩家画像', icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="6" r="3.5" stroke="currentColor" stroke-width="1.5"/><path d="M3 17.5c0-3.866 3.134-7 7-7s7 3.134 7 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' },
        { path: '/recommend', label: '智能推荐', icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M10 2l2.317 4.96 5.183.71-3.75 3.735.917 5.345L10 14.3l-4.667 2.55.917-5.345L2.5 7.67l5.183-.71L10 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>' },
        { path: '/analysis', label: '数据分析', icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><rect x="2.5" y="14" width="3.5" height="4.5" rx="0.5" stroke="currentColor" stroke-width="1.5"/><rect x="8.25" y="8" width="3.5" height="10.5" rx="0.5" stroke="currentColor" stroke-width="1.5"/><rect x="14" y="4" width="3.5" height="14.5" rx="0.5" stroke="currentColor" stroke-width="1.5"/></svg>' },
        { path: '/evaluation', label: '算法评估', icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="7.5" stroke="currentColor" stroke-width="1.5"/><path d="M10 6v4l2.5 2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' },
        { path: '/games', label: '游戏搜索', icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="8.5" cy="8.5" r="5.75" stroke="currentColor" stroke-width="1.5"/><path d="M12.5 12.5L17.5 17.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' },
        { path: '/data-import', label: '数据管理', icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M2.5 14l2.5 2.5 5-5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M4 4h12M4 8h8M4 12h3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' },
        { path: '/settings', label: '系统设置', icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="2.6" stroke="currentColor" stroke-width="1.5"/><path d="M10 2.5v2M10 15.5v2M2.5 10h2M15.5 10h2M4.5 4.5l1.5 1.5M14 14l1.5 1.5M4.5 15.5L6 14M14 6l1.5-1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' }
      ],
      categoryItems: [
        { label: '动作', color: '#ef4444' }, { label: '冒险', color: '#f97316' },
        { label: '角色扮演', color: '#8b5cf6' }, { label: '策略', color: '#06b6d4' },
        { label: '模拟', color: '#22c55e' }, { label: '射击', color: '#eab308' },
        { label: '独立', color: '#ec4899' }, { label: '休闲', color: '#14b8a6' },
        { label: '竞速', color: '#f59e0b' }, { label: '体育', color: '#3b82f6' }
      ]
    };
  },
  computed: {
    isLoginPage() {
      return this.$route.path === '/login';
    },
    isLoggedIn() {
      try {
        const auth = localStorage.getItem('gamerec-auth');
        return !!auth;
      } catch (e) {
        return false;
      }
    },
    currentPageLabel() {
      const route = this.$route;
      if (route.meta && route.meta.title) return route.meta.title;
      const item = this.navItems.find(n => this.isActive(n.path));
      return item ? item.label : '';
    }
  },
  watch: {
    '$route'() { this.closeMobileMenu(); this.updatePageTitle(); }
  },
  mounted() {
    if (this.isLoginPage) return;
    this.initTheme();
    this.checkMobile();
    this.updatePageTitle();
    this.loadSidebarData();
    window.addEventListener('resize', this.handleResize);
    this.$nextTick(() => {
      const main = document.getElementById('main-content');
      if (main) main.addEventListener('scroll', this.handleScroll, { passive: true });
    });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    if (this.scrollTimer) clearTimeout(this.scrollTimer);
    if (this.resizeTimer) clearTimeout(this.resizeTimer);
    const main = document.getElementById('main-content');
    if (main) main.removeEventListener('scroll', this.handleScroll);
  },
  methods: {
    initTheme() {
      const saved = localStorage.getItem('gamerec-theme');
      if (saved === 'light' || saved === 'dark') {
        this.isDarkTheme = saved === 'dark';
      } else {
        this.isDarkTheme = true; // 默认深色
      }
      document.documentElement.setAttribute('data-theme', this.isDarkTheme ? 'dark' : 'light');
      this._themeMedia = window.matchMedia('(prefers-color-scheme: light)');
      this._themeMedia.addEventListener('change', this._onSystemThemeChange);
    },
    _onSystemThemeChange(e) {
      if (!localStorage.getItem('gamerec-theme')) {
        this.isDarkTheme = !e.matches;
        document.documentElement.setAttribute('data-theme', this.isDarkTheme ? 'dark' : 'light');
      }
    },
    toggleTheme() {
      this.isDarkTheme = !this.isDarkTheme;
      document.documentElement.setAttribute('data-theme', this.isDarkTheme ? 'dark' : 'light');
      localStorage.setItem('gamerec-theme', this.isDarkTheme ? 'dark' : 'light');
    },
    toggleGroup(key) {
      this.collapsedGroups[key] = !this.collapsedGroups[key];
    },
    isActive(path) {
      if (path === '/') return this.$route.path === '/';
      return this.$route.path.startsWith(path);
    },
    checkMobile() {
      this.isMobile = window.innerWidth < 768;
      if (this.isMobile) this.sidebarCollapsed = true;
    },
    handleResize() {
      if (this.resizeTimer) clearTimeout(this.resizeTimer);
      this.resizeTimer = setTimeout(() => this.checkMobile(), 150);
    },
    handleScroll(e) {
      if (this.scrollTimer) clearTimeout(this.scrollTimer);
      this.scrollTimer = setTimeout(() => {
        this.showBackTop = e.target.scrollTop > 400;
        this.headerScrolled = e.target.scrollTop > 20;
      }, 100);
    },
    toggleSidebar() { if (this.isMobile) this.mobileMenuOpen = !this.mobileMenuOpen; else this.sidebarCollapsed = !this.sidebarCollapsed; },
    toggleMobileMenu() { this.mobileMenuOpen = !this.mobileMenuOpen; },
    closeMobileMenu() { this.mobileMenuOpen = false; },
    refreshData() {
      this.isRefreshing = true;
      this.loadSidebarData();
      setTimeout(() => { this.isRefreshing = false; if (this.$message) this.$message.success('数据已刷新'); }, 800);
    },
    scrollToTop() {
      const main = document.getElementById('main-content');
      if (main) main.scrollTo({ top: 0, behavior: 'smooth' });
    },
    updatePageTitle() {
      document.title = (this.$route.meta && this.$route.meta.title) ? `${this.$route.meta.title} - GameRec` : 'GameRec - 基于玩家画像的游戏推荐系统';
    },
    async loadSidebarData() {
      try {
        const { data: overviewData } = await getOverview();
        if (overviewData.code === 200) {
          const d = overviewData.data;
          this.stats.gameCount = d.gameCount || 0;
          this.stats.userCount = d.userCount || 0;
          this.stats.ratingCount = d.ratingCount || 0;
        }
      } catch (e) { /* API不可用，保持 '---' */ }
      try {
        const { data: hotData } = await getPopular(5);
        if (hotData.code === 200) this.hotGames = hotData.data || [];
      } catch (e) { /* silent */ }
    },
    navigateToGames(category) {
      this.closeMobileMenu();
      this.$router.push({ path: '/games', query: { tag: category } });
    },
    goGame(gameId) { this.$router.push(`/game/${gameId}`); },
    handleLogout() {
      localStorage.removeItem('gamerec-auth');
      sessionStorage.removeItem('gamerec-session');
      // 保留记住的登录信息（由用户手动清除）
      if (this.aiFloatingOpen) this.aiFloatingOpen = false;
      this.$router.replace('/login');
    },
    formatShortNum(n) {
      if (!n) return '';
      if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
      return n.toString();
    }
  }
};
</script>

<style>
@import './styles/design-tokens.css';
</style>

<style scoped>
/* 登录模式：全屏无外壳 */
.app-login-mode {
  width: 100vw;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
}

.app-shell { display: flex; flex-direction: column; height: 100vh; height: 100dvh; overflow: hidden; background: var(--color-bg-base); }

/* ===== Header ===== */
.app-header { display: flex; align-items: center; justify-content: space-between; height: var(--header-height); padding: 0 var(--space-5); background: var(--color-bg-elevated); border-bottom: 1px solid var(--color-border-muted); flex-shrink: 0; z-index: 110; backdrop-filter: blur(var(--blur-xl)); transition: box-shadow var(--duration-normal), border-color var(--duration-normal); }
.header--scrolled { border-bottom-color: var(--color-border-default); box-shadow: 0 1px 3px rgba(0,0,0,0.3); }
.header-left { display: flex; align-items: center; gap: var(--space-3); }
.hamburger-btn { display: none; flex-direction: column; justify-content: center; gap: 5px; width: 36px; height: 36px; padding: 6px; border: none; border-radius: var(--radius-md); background: transparent; cursor: pointer; }
.hamburger-btn:hover { background: var(--color-bg-subtle); }
.hamburger-line { display: block; width: 20px; height: 2px; border-radius: 2px; background: var(--color-text-secondary); transition: all var(--duration-normal) var(--ease-out-expo); }
.hamburger-line.open:nth-child(1) { transform: translateY(7px) rotate(45deg); }
.hamburger-line.open:nth-child(2) { opacity: 0; transform: scaleX(0); }
.hamburger-line.open:nth-child(3) { transform: translateY(-7px) rotate(-45deg); }
.header-brand { display: flex; align-items: center; gap: var(--space-3); text-decoration: none; }
.brand-icon { display: flex; align-items: center; }
.brand-text { display: flex; align-items: baseline; gap: var(--space-2); }
.brand-name { font-family: var(--font-family-display); font-size: var(--text-lg); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }
.brand-dot { color: var(--color-brand-500); font-weight: var(--font-weight-bold); }
.brand-subtitle { font-size: var(--text-xs); color: var(--color-text-tertiary); font-weight: var(--font-weight-medium); }
.header-actions { display: flex; align-items: center; gap: var(--space-3); }
.current-page-label { display: none; font-size: var(--text-xs); color: var(--color-text-tertiary); max-width: 100px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.header-badge { display: flex; align-items: center; gap: var(--space-2); padding: var(--space-1) var(--space-3); background: var(--color-success-bg); border: 1px solid var(--color-success-border); border-radius: var(--radius-full); font-size: var(--text-xs); color: var(--color-success); }
.badge-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--color-success); box-shadow: 0 0 6px var(--color-success-border); animation: pulse-dot 2s ease-in-out infinite; }
@keyframes pulse-dot { 0%,100% { box-shadow: 0 0 4px var(--color-success-border); } 50% { box-shadow: 0 0 12px var(--color-success); } }
.header-btn { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border: none; border-radius: var(--radius-md); background: transparent; color: var(--color-text-secondary); cursor: pointer; transition: all var(--duration-fast); }
.header-btn:hover { background: var(--color-bg-subtle); color: var(--color-text-primary); }
.header-btn:active { transform: scale(0.92); }
.header-btn--desktop { display: flex; }
.spin { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.app-body { display: flex; flex: 1; overflow: hidden; position: relative; }

/* ===== Sidebar — Collapsible Group Design ===== */
.app-sidebar { width: var(--sidebar-width); flex-shrink: 0; background: var(--color-bg-elevated); border-right: 1px solid var(--color-border-muted); transition: width var(--duration-normal) var(--ease-out-expo), transform var(--duration-normal) var(--ease-out-expo); overflow: hidden; z-index: 100; }
.app-sidebar.collapsed { width: var(--sidebar-collapsed-width); }
.sidebar-inner { display: flex; flex-direction: column; height: 100%; padding: var(--space-2) var(--space-1); overflow-y: auto; scrollbar-width: thin; }
.sidebar-inner::-webkit-scrollbar { width: 3px; }
.sidebar-inner::-webkit-scrollbar-thumb { background: var(--color-border-muted); border-radius: 10px; }

/* ---- Collapsible Group ---- */
.sidebar-group { margin-bottom: 2px; border-radius: var(--radius-lg); background: var(--color-bg-base); padding: 2px; }
.sidebar-group-header { display: flex; align-items: center; gap: var(--space-2); width: 100%; padding: var(--space-2) var(--space-2); border: none; border-radius: var(--radius-md); background: transparent; color: var(--color-text-tertiary); font-size: var(--text-2xs); font-weight: var(--font-weight-semibold); cursor: pointer; transition: all var(--duration-fast); text-transform: uppercase; letter-spacing: 0.3px; }
.sidebar-group-header:hover { background: var(--color-bg-subtle); color: var(--color-text-secondary); }
.group-chevron { flex-shrink: 0; transition: transform var(--duration-fast); }
.group-chevron.rotated { transform: rotate(90deg); }
.group-header-label { flex: 1; text-align: left; }
.group-header-count { padding: 0 5px; border-radius: var(--radius-full); background: var(--color-bg-subtle); font-size: var(--text-2xs); font-weight: var(--font-weight-normal); font-family: var(--font-family-mono); }
.sidebar-group-body { padding: var(--space-1) 0; }
.sidebar-group--collapsed .sidebar-group-body { display: none; }

/* ---- Main Nav ---- */
.nav-list { list-style: none; display: flex; flex-direction: column; gap: 1px; margin: 0; padding: 0; }
.nav-item { display: flex; align-items: center; gap: var(--space-2); padding: var(--space-1_5) var(--space-2); border-radius: var(--radius-md); color: var(--color-text-secondary); text-decoration: none; font-size: var(--text-xs); font-weight: var(--font-weight-medium); transition: all var(--duration-fast); cursor: pointer; border: none; background: none; width: 100%; text-align: left; }
.nav-item:hover { color: var(--color-text-primary); background: var(--color-bg-overlay); }
.nav-item.active { color: var(--color-brand-400); background: linear-gradient(135deg, var(--color-brand-a12), var(--color-brand-a04)); }
.nav-item.active::after { content: ''; position: absolute; inset: 0; border-radius: var(--radius-md); border: 1px solid var(--color-brand-a15); pointer-events: none; }
.nav-icon { display: flex; align-items: center; flex-shrink: 0; width: 16px; height: 16px; }
.nav-label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: var(--text-xs); }

/* ---- Category Grid ---- */
.sidebar-categories { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; }
.sidebar-cat-item { display: flex; align-items: center; gap: var(--space-1_5); padding: var(--space-1) var(--space-2); border: none; border-radius: var(--radius-sm); background: transparent; color: var(--color-text-secondary); font-size: var(--text-2xs); cursor: pointer; transition: all var(--duration-fast); }
.sidebar-cat-item:hover { background: var(--color-bg-overlay); color: var(--color-text-primary); }
.cat-dot { width: 5px; height: 5px; border-radius: 50%; flex-shrink: 0; }
.cat-label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ---- Hot List ---- */
.sidebar-hot { display: flex; flex-direction: column; gap: 1px; }
.sidebar-hot-item { display: flex; align-items: center; gap: var(--space-1_5); padding: var(--space-1) var(--space-2); border-radius: var(--radius-sm); cursor: pointer; transition: all var(--duration-fast); }
.sidebar-hot-item:hover { background: var(--color-bg-overlay); }
.hot-rank { flex-shrink: 0; width: 16px; height: 16px; display: flex; align-items: center; justify-content: center; border-radius: var(--radius-sm); font-family: var(--font-family-mono); font-size: 10px; color: var(--color-text-tertiary); background: var(--color-bg-subtle); }
.hot-rank--top { background: var(--color-warm-a12); color: var(--color-warm-400); font-weight: var(--font-weight-bold); }
.hot-name { flex: 1; font-size: var(--text-2xs); color: var(--color-text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hot-score { font-family: var(--font-family-mono); font-size: 10px; color: var(--color-text-tertiary); }
.sidebar-hot-empty { padding: var(--space-2); text-align: center; }
.sidebar-empty-text { font-size: var(--text-2xs); color: var(--color-text-tertiary); }

/* ---- Footer Stats ---- */
.sidebar-footer { margin-top: auto; padding: var(--space-2) var(--space-2); border-top: 1px solid var(--color-border-muted); }
.sidebar-stats { display: flex; justify-content: space-around; }
.sidebar-stat { text-align: center; }
.stat-num { display: block; font-family: var(--font-family-mono); font-size: var(--text-xs); color: var(--color-text-primary); font-weight: var(--font-weight-semibold); }
.stat-label { font-size: var(--text-2xs); color: var(--color-text-tertiary); }

/* Collapsed state */
.app-sidebar.collapsed .sidebar-group-header,
.app-sidebar.collapsed .sidebar-group-body,
.app-sidebar.collapsed .sidebar-footer { display: none; }

/* ===== Overlay ===== */
.mobile-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(2px); z-index: 95; }
.overlay-fade-enter-active,.overlay-fade-leave-active { transition: opacity var(--duration-normal) var(--ease-in-out); }
.overlay-fade-enter,.overlay-fade-leave-to { opacity: 0; }

/* ===== Main ===== */
.app-main { flex: 1; overflow-y: auto; overflow-x: hidden; padding: var(--space-8) var(--content-gutter); background: var(--color-bg-base); -webkit-overflow-scrolling: touch; scroll-behavior: smooth; }
.page-enter-active { animation: page-in var(--duration-page) var(--ease-out-expo); }
.page-leave-active { animation: page-out var(--duration-fast) var(--ease-in-out); }
.back-top-btn { position: sticky; bottom: var(--space-6); float: right; margin-right: -8px; width: 42px; height: 42px; display: flex; align-items: center; justify-content: center; border: 1px solid var(--color-border-default); border-radius: var(--radius-full); background: var(--color-bg-elevated); color: var(--color-text-secondary); cursor: pointer; box-shadow: var(--shadow-lg); transition: all var(--duration-fast); z-index: 50; }
.back-top-btn:hover { color: var(--color-text-primary); border-color: var(--color-brand-500); transform: translateY(-2px); }
.back-top-btn:active { transform: scale(0.92); }
.fade-up-enter-active,.fade-up-leave-active { transition: all var(--duration-normal) var(--ease-out-expo); }
.fade-up-enter,.fade-up-leave-to { opacity: 0; transform: translateY(16px); }

/* ===== AI 浮窗按钮 ===== */
.ai-fab {
  position: fixed;
  right: var(--space-6);
  bottom: 80px;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 11px 18px;
  background: linear-gradient(135deg, var(--color-brand-500) 0%, #8b5cf6 100%);
  color: #fff;
  border: none;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 24px var(--color-brand-a35), 0 2px 6px rgba(0,0,0,0.2);
  transition: all var(--duration-normal) var(--ease-out-expo);
}
.ai-fab--close {
  width: 48px;
  height: 48px;
  padding: 0;
  border-radius: 50%;
}
.ai-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px var(--color-brand-a50), 0 4px 8px rgba(0,0,0,0.25);
}
.ai-fab--close:hover { transform: translateY(-2px) rotate(90deg); }
.ai-fab:active { transform: scale(0.96); }
.ai-fab--close:active { transform: scale(0.96) rotate(90deg); }
.ai-fab-icon { display: block; flex-shrink: 0; }
.ai-fab-label { line-height: 1; }

.ai-floating-panel {
  position: fixed;
  right: var(--space-5);
  bottom: 80px;
  width: 600px;
  max-width: calc(100vw - var(--space-10));
  height: 680px;
  max-height: calc(100vh - 110px);
  z-index: 95;
  border-radius: var(--radius-2xl);
  overflow: hidden;
  box-shadow: var(--shadow-2xl);
}
.slide-up-enter-active, .slide-up-leave-active { transition: all var(--duration-normal) var(--ease-out-expo); }
.slide-up-enter, .slide-up-leave-to { opacity: 0; transform: translateY(20px) scale(0.96); }

@media (max-width: 768px) {
  .ai-fab { right: var(--space-4); bottom: 70px; padding: 8px 12px; font-size: 12px; }
  .ai-floating-panel {
    right: var(--space-2);
    left: var(--space-2);
    width: auto;
    bottom: 130px;
    height: 78vh;
  }
}

/* ===== Responsive ===== */
@media (max-width: 1024px) { .app-main { padding: var(--space-6) var(--space-5); } .brand-subtitle { display: none; } }
@media (max-width: 767px) {
  .hamburger-btn { display: flex; }
  .header-btn--desktop,.header-badge { display: none; }
  .current-page-label { display: block; }
  .brand-dot,.brand-subtitle { display: none; }
  .brand-name { font-size: var(--text-md); }
  .app-sidebar { position: fixed; top: var(--header-height); left: 0; bottom: 0; width: 280px; transform: translateX(-100%); box-shadow: var(--shadow-2xl); z-index: 105; }
  .app-sidebar.mobile-open { transform: translateX(0); }
  .app-sidebar.collapsed { width: 280px; }
  .app-sidebar.collapsed .sidebar-group-header,
  .app-sidebar.collapsed .sidebar-group-body,
  .app-sidebar.collapsed .sidebar-footer { display: flex; }
  .app-main { padding: var(--space-4) var(--space-3); }
}
@media (max-width: 480px) { .app-header { padding: 0 var(--space-3); } .app-main { padding: var(--space-3) var(--space-2); } }
</style>
