<template>
  <div class="refactor-home">
    <!-- 顶部 Hero 横幅 - 优化版 -->
    <div class="top-banner">
      <!-- 简洁背景层（去除多余动态效果） -->
      <div class="banner-bg-layer">
        <div class="banner-bg-grid"></div>
        <div class="banner-bg-glow banner-bg-glow--1"></div>
      </div>

      <div class="banner-inner">
        <div class="banner-left">
          <div class="banner-eyebrow">
            <span class="eyebrow-dot"></span>
            数据智能 · 一目了然
            <span class="eyebrow-pulse"></span>
          </div>
          <h1 class="banner-title">
            <span class="title-text">基于玩家画像的游戏推荐</span>
            <span class="title-system">SYSTEM</span>
          </h1>
          <div class="banner-tags">
            <span class="banner-tag">
              <span class="tag-bullet"></span>实时概览
            </span>
            <span class="banner-tag">
              <span class="tag-bullet"></span>智能推荐
            </span>
            <span class="banner-tag">
              <span class="tag-bullet"></span>多维分析
            </span>
          </div>
        </div>
        <div class="banner-right">
          <div class="status-cluster">
            <div class="status-block">
              <div class="status-num">{{ stats.gameCount || '---' }}</div>
              <div class="status-label">在库游戏</div>
              <div class="status-bar"><span class="status-bar-fill" style="width: 92%"></span></div>
            </div>
            <div class="status-divider"></div>
            <div class="status-block">
              <div class="status-num">{{ stats.userCount || '---' }}</div>
              <div class="status-label">玩家用户</div>
              <div class="status-bar"><span class="status-bar-fill" style="width: 78%"></span></div>
            </div>
            <div class="status-divider"></div>
            <div class="status-block">
              <div class="status-num">{{ stats.ratingCount || '---' }}</div>
              <div class="status-label">交互评价</div>
              <div class="status-bar"><span class="status-bar-fill" style="width: 85%"></span></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 4 个统计卡片（KPI 大卡 · 渐变 + 图标 + 数字滚动 + 迷你图） -->
      <div class="stat-cards">
        <div
          class="stat-card"
          v-for="(s, i) in statCards"
          :key="i"
          :class="['stat-card--' + s.tone, { 'is-loaded': cardLoaded[i] }]"
          :style="{ '--accent': s.accent, '--accent-soft': s.accentSoft, '--accent-bg': s.accentBg }"
        >
          <!-- 卡片装饰：角落L形角标 + 内部光晕（去除扫描线） -->
          <span class="card-corner card-corner--tl"></span>
          <span class="card-corner card-corner--tr"></span>
          <span class="card-corner card-corner--bl"></span>
          <span class="card-corner card-corner--br"></span>
          <span class="card-glow"></span>

          <div class="stat-card-top">
            <div class="stat-card-icon">
              <span class="icon-ring"></span>
              <span class="icon-glyph" v-html="s.icon"></span>
            </div>
            <div class="stat-card-meta">
              <div class="stat-card-label">{{ s.label }}</div>
              <div class="stat-card-sub">{{ s.subLabel }}</div>
            </div>
            <div
              v-if="s.trend != null"
              class="stat-card-trend"
              :class="s.trend >= 0 ? 'is-up' : 'is-down'"
            >
              <svg v-if="s.trend >= 0" width="10" height="10" viewBox="0 0 10 10" fill="none">
                <path d="M5 1.5L8.5 6.5h-7L5 1.5z" fill="currentColor"/>
              </svg>
              <svg v-else width="10" height="10" viewBox="0 0 10 10" fill="none">
                <path d="M5 8.5L1.5 3.5h7L5 8.5z" fill="currentColor"/>
              </svg>
              <span>{{ Math.abs(s.trend) }}{{ s.trendUnit || '%' }}</span>
            </div>
          </div>

          <div class="stat-card-value-row">
            <div class="stat-card-value">
              <span class="value-num">{{ s.displayValue }}</span>
              <span class="value-unit">{{ s.unit }}</span>
            </div>
          </div>

          <!-- 迷你折线图（带渐变填充） -->
          <div class="stat-card-spark-wrap">
            <svg class="stat-card-spark" :viewBox="`0 0 ${s.svgW} ${s.svgH}`" preserveAspectRatio="none">
              <defs>
                <linearGradient :id="`stat-grad-${i}`" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" :stop-color="s.accent" stop-opacity="0.45"/>
                  <stop offset="100%" :stop-color="s.accent" stop-opacity="0"/>
                </linearGradient>
                <linearGradient :id="`stat-line-${i}`" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0%" :stop-color="s.accent" stop-opacity="0.6"/>
                  <stop offset="100%" :stop-color="s.accent" stop-opacity="1"/>
                </linearGradient>
              </defs>
              <!-- 网格线 -->
              <line
                v-for="g in 3" :key="'g' + g"
                :x1="0" :x2="s.svgW" :y1="(s.svgH / 4) * g" :y2="(s.svgH / 4) * g"
                stroke="currentColor" stroke-opacity="0.06" stroke-dasharray="2 4"
              />
              <path :d="s.areaPath" :fill="`url(#stat-grad-${i})`"/>
              <path :d="s.linePath" :stroke="`url(#stat-line-${i})`" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" class="stat-spark-line"/>
              <!-- 末端高亮点 -->
              <circle
                v-if="s.lastPoint"
                :cx="s.lastPoint.x" :cy="s.lastPoint.y" r="3.5"
                :fill="s.accent" class="stat-spark-dot"
              />
              <circle
                v-if="s.lastPoint"
                :cx="s.lastPoint.x" :cy="s.lastPoint.y" r="7"
                :fill="s.accent" fill-opacity="0.18" class="stat-spark-pulse"
              />
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 6 个图标导航 tab - 优化版 -->
    <div class="nav-tabs">
      <div class="nav-tabs-inner">
        <router-link
          v-for="t in navTabs"
          :key="t.path"
          :to="t.path"
          class="nav-tab"
          :class="{ 'nav-tab--active': isActiveTab(t.path) }"
        >
          <span class="nav-tab-icon" v-html="t.icon"></span>
          <span class="nav-tab-label">{{ t.label }}</span>
          <span class="nav-tab-indicator"></span>
        </router-link>
      </div>
    </div>

    <!-- 主内容区：价格分布 + 近期上市游戏 -->
    <div class="content-row">
      <div class="content-card chart-card">
        <div class="card-head">
          <div class="card-head-left">
            <h2 class="card-title">游戏价格分布</h2>
            <span class="card-subtitle">Price Range Distribution</span>
          </div>
          <div class="card-legend">
            <span class="legend-dot" :style="{ background: 'var(--chart-1)' }"></span>
            <span>价格区间内游戏数量</span>
          </div>
        </div>
        <div ref="priceChart" class="time-chart" aria-label="按价格区间统计游戏分布"></div>
      </div>

      <div class="content-card recent-card">
        <div class="card-head">
          <div class="card-head-left">
            <h2 class="card-title">近期上市游戏</h2>
            <span class="card-subtitle">Recently Released</span>
          </div>
          <span class="card-meta">最近 20 款</span>
        </div>
        <div class="recent-list" v-if="recentGames.length > 0">
          <div
            v-for="(g, idx) in recentGames"
            :key="g.gameId"
            class="recent-item"
            :style="{ '--idx': idx }"
            @click="goGame(g.gameId)"
          >
            <div class="recent-rank">{{ String(idx + 1).padStart(2, '0') }}</div>
            <div class="recent-cover" :style="{ background: coverStyle(g) }">
              <img
                v-if="hasImage(g)"
                :src="getImageUrl(g)"
                :alt="g.gameNameCn || g.gameName"
                @error="markImageFailed(g)"
                class="recent-cover-img"
              />
              <span v-else class="recent-cover-letter">{{ getInitial(g) }}</span>
            </div>
            <div class="recent-info">
              <div class="recent-name" :title="g.gameNameCn || g.gameName">{{ g.gameNameCn || g.gameName }}</div>
              <div class="recent-meta">
                <span class="recent-price">价格 <b :class="g.price > 0 ? 'price--paid' : 'price--free'">{{ g.price > 0 ? g.price.toFixed(2) + '元' : '免费' }}</b></span>
                <span class="recent-date"><b>{{ formatDate(g.releaseDate) }}</b></span>
              </div>
            </div>
            <div class="recent-arrow">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M5 3l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
          </div>
        </div>
        <div v-else class="loading-tip">加载中…</div>
      </div>
    </div>

    <!-- AI 助手区已移至全局浮窗按钮 (App.vue) -->
  </div>
</template>

<script>
import * as echarts from 'echarts';
import { getOverview, getPriceDistribution, getKpiTrends } from '@/api/index';
import { listGames } from '@/api/index';
import { getPurchasePlayRatio } from '@/api/index';
import { chartPalette, chartTheme } from '@/utils/echarts-theme';

export default {
  name: 'RefactorHome',
  data() {
    return {
      // 4个KPI统计卡 · 数据全部从DB拉取（accent 色使用统一图表色板）
      statCards: [
        {
          label: '游戏平均评分', subLabel: 'Average Rating', tone: 'blue',
          value: '---', displayValue: '0.0', unit: '/ 5', trend: 2.3, trendUnit: '%',
          chartIdx: 1,
          icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M10 2l2.317 4.96 5.183.71-3.75 3.735.917 5.345L10 14.3l-4.667 2.55.917-5.345L2.5 7.67l5.183-.71L10 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>',
          linePath: '', areaPath: '', lastPoint: null, svgW: 200, svgH: 50, points: []
        },
        {
          label: '免费游戏占比', subLabel: 'Free Ratio', tone: 'cyan',
          value: '---', displayValue: '0.0', unit: '%', trend: 1.1, trendUnit: '%',
          chartIdx: 2,
          icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="7.5" stroke="currentColor" stroke-width="1.5"/><path d="M7 10l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
          linePath: '', areaPath: '', lastPoint: null, svgW: 200, svgH: 50, points: []
        },
        {
          label: '玩家转化率', subLabel: 'Conversion', tone: 'purple',
          value: '---', displayValue: '0', unit: '%', trend: 5.6, trendUnit: '%',
          chartIdx: 3,
          icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M3 10l4.5-4.5L12 10l4.5-4.5L18 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
          linePath: '', areaPath: '', lastPoint: null, svgW: 200, svgH: 50, points: []
        },
        {
          label: '平均游玩时长', subLabel: 'Avg. Play Hours', tone: 'pink',
          value: '---', displayValue: '0.0', unit: 'h', trend: 3.2, trendUnit: '%',
          chartIdx: 5,
          icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="7.5" stroke="currentColor" stroke-width="1.5"/><path d="M10 6v4l2.5 2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
          linePath: '', areaPath: '', lastPoint: null, svgW: 200, svgH: 50, points: []
        }
      ],
      cardLoaded: [false, false, false, false],
      stats: { gameCount: '---', userCount: '---', ratingCount: '---' },
      recentGames: [],
      _kpiTrends: null,
      priceChart: null,
      imageFailed: {},
      navTabs: [
        { path: '/', label: '主页', icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M3 8l7-5.25L17 8v8.25a.75.75 0 01-.75.75h-3.5v-4.5a.75.75 0 00-.75-.75H8a.75.75 0 00-.75.75V17H3.75A.75.75 0 013 16.25V8z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>' },
        { path: '/games', label: '游戏搜索', icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><circle cx="8.5" cy="8.5" r="5.75" stroke="currentColor" stroke-width="1.5"/><path d="M12.5 12.5L17.5 17.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' },
        { path: '/analysis', label: '数据分析', icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><rect x="2.5" y="14" width="3.5" height="4.5" rx="0.5" stroke="currentColor" stroke-width="1.5"/><rect x="8.25" y="8" width="3.5" height="10.5" rx="0.5" stroke="currentColor" stroke-width="1.5"/><rect x="14" y="4" width="3.5" height="14.5" rx="0.5" stroke="currentColor" stroke-width="1.5"/></svg>' },
        { path: '/recommend', label: '智能推荐', icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M10 2l2.317 4.96 5.183.71-3.75 3.735.917 5.345L10 14.3l-4.667 2.55.917-5.345L2.5 7.67l5.183-.71L10 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>' },
        { path: '/profile', label: '玩家画像', icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="6" r="3.5" stroke="currentColor" stroke-width="1.5"/><path d="M3 17.5c0-3.866 3.134-7 7-7s7 3.134 7 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' },
        { path: '/evaluation', label: '算法评估', icon: '<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="7.5" stroke="currentColor" stroke-width="1.5"/><path d="M10 6v4l2.5 2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' }
      ],
      isLightTheme: false
    };
  },
  async mounted() {
    this.syncTheme();
    this.applyCardAccents();
    this.generateSparkLines();
    await this.loadAll();
    this.$nextTick(() => this.initPriceChart());
    window.addEventListener('resize', this.handleResize);
    // 监听主题切换
    this._themeObserver = new MutationObserver(() => {
      const newLight = document.documentElement.getAttribute('data-theme') === 'light';
      if (this.isLightTheme !== newLight) {
        this.isLightTheme = newLight;
        this.applyCardAccents();
        this.generateSparkLines();
        this.$nextTick(() => this.initPriceChart());
      }
    });
    this._themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    if (this.priceChart) this.priceChart.dispose();
    if (this._themeObserver) this._themeObserver.disconnect();
  },
  methods: {
    // 从统一图表色板派生每张 KPI 卡的 accent / accentSoft / accentBg
    applyCardAccents() {
      const palette = chartPalette();
      this.statCards.forEach((s) => {
        const accent = palette[(s.chartIdx - 1 + palette.length) % palette.length];
        s.accent = accent;
        s.accentSoft = this.hexToRgba(accent, 0.38);
        s.accentBg = this.hexToRgba(accent, 0.12);
      });
    },
    hexToRgba(hex, alpha) {
      const m = hex.replace('#', '');
      const r = parseInt(m.substring(0, 2), 16);
      const g = parseInt(m.substring(2, 4), 16);
      const b = parseInt(m.substring(4, 6), 16);
      return `rgba(${r},${g},${b},${alpha})`;
    },
    particleStyle(i) {
      // 预生成稳定的位置（基于 i）— 保留但暂未使用
      const seed = i * 1664525 + 1013904223;
      const x = ((seed >>> 16) % 100);
      const y = ((seed * 31) % 100);
      const size = 2 + ((seed >>> 8) % 3);
      const dur = 8 + (i % 7);
      const delay = (i * 0.7) % 8;
      return {
        left: x + '%',
        top: y + '%',
        width: size + 'px',
        height: size + 'px',
        animationDuration: dur + 's',
        animationDelay: delay + 's'
      };
    },
    syncTheme() {
      this.isLightTheme = document.documentElement.getAttribute('data-theme') === 'light';
    },
    isActiveTab(path) {
      if (path === '/') return this.$route.path === '/';
      return this.$route.path.startsWith(path);
    },
    generateSparkLines() {
      // 为每个统计卡片生成动态迷你折线图（数据来自数据库 API）
      // 如果有 API 数据则使用，否则显示空白（等待 loadAll 加载后重新调用）
      const apiSeries = this._kpiTrends && this._kpiTrends.series ? this._kpiTrends.series : null;
      const paths = apiSeries
        ? apiSeries.map(arr => (arr || []).map(v => Number(v)))
        : [[], [], [], []];

      this.statCards.forEach((s, i) => {
        const pts = paths[i] || [];
        if (pts.length < 2) {
          s.points = []; s.linePath = ''; s.areaPath = ''; s.lastPoint = null;
          return;
        }
        const w = s.svgW, h = s.svgH;
        const padding = 4;
        const usableH = h - padding * 2;
        const maxVal = Math.max(...pts);
        const minVal = Math.min(...pts);
        const range = maxVal - minVal || 1;
        const step = w / (pts.length - 1);
        const points = pts.map((v, j) => {
          const x = j * step;
          const y = padding + usableH - ((v - minVal) / range) * usableH;
          return { x, y };
        });
        const smoothPath = this.catmullRomToBezier(points);
        s.points = points;
        s.linePath = smoothPath.length > 0 ? 'M' + smoothPath[0] : '';
        s.areaPath = smoothPath.length > 0
          ? 'M' + smoothPath[0] + ' L' + smoothPath.slice(1).join(' L') + ` L${w},${h} L0,${h} Z`
          : '';
        s.lastPoint = points.length > 0 ? points[points.length - 1] : null;
      });
    },
    // Catmull-Rom 样条 → 三次 Bezier 控制点
    catmullRomToBezier(points) {
      if (points.length < 2) return points.map(p => `${p.x},${p.y}`);
      const result = [];
      for (let i = 0; i < points.length; i++) {
        const p0 = points[i - 1] || points[i];
        const p1 = points[i];
        const p2 = points[i + 1] || points[i];
        const p3 = points[i + 2] || p2;
        const t = 0.18;
        const cp1x = p1.x + (p2.x - p0.x) * t;
        const cp1y = p1.y + (p2.y - p0.y) * t;
        const cp2x = p2.x - (p3.x - p1.x) * t;
        const cp2y = p2.y - (p3.y - p1.y) * t;
        if (i === 0) {
          result.push(`${p1.x.toFixed(1)},${p1.y.toFixed(1)}`);
        }
        result.push(`${cp1x.toFixed(1)},${cp1y.toFixed(1)}`);
        result.push(`${cp2x.toFixed(1)},${cp2y.toFixed(1)}`);
        result.push(`${p2.x.toFixed(1)},${p2.y.toFixed(1)}`);
      }
      return result;
    },
    // 数字滚动动画（从0到目标值）
    animateCardNumber(idx, target, duration = 1100, decimals = 1) {
      const start = performance.now();
      const step = (now) => {
        const progress = Math.min((now - start) / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        const cur = target * eased;
        this.$set(this.statCards[idx], 'displayValue', decimals > 0 ? cur.toFixed(decimals) : Math.round(cur).toString());
        if (progress < 1) {
          requestAnimationFrame(step);
        } else {
          this.$set(this.statCards[idx], 'displayValue', decimals > 0 ? Number(target).toFixed(decimals) : Math.round(target).toString());
        }
      };
      requestAnimationFrame(step);
    },
    async loadAll() {
      // 1. 加载系统概览（游戏数、用户数、评价数、平均评分、平均游玩时长）
      try {
        const { data: ov } = await getOverview();
        if (ov.code === 200) {
          const d = ov.data;
          this.stats.gameCount = (d.gameCount || 0).toLocaleString();
          this.stats.userCount = (d.userCount || 0).toLocaleString();
          this.stats.ratingCount = (d.ratingCount || 0).toLocaleString();
          // 平均评分（满分5分）→ 数字滚动
          if (d.avgRating) {
            const v = Number(d.avgRating);
            this.statCards[0].value = v.toFixed(1) + ' / 5';
            this.animateCardNumber(0, v, 1100, 1);
            this.$set(this.cardLoaded, 0, true);
          }
          // 平均游玩时长（从数据库读取）
          if (d.avgPlayHours) {
            const v = Number(d.avgPlayHours);
            this.statCards[3].value = v.toFixed(1) + ' h';
            this.animateCardNumber(3, v, 1200, 1);
            this.$set(this.cardLoaded, 3, true);
          }
          // 转化率（先用概览估算，最终被 purchasePlayRatio 覆盖）
          if (d.ratingCount && d.userCount) {
            this.statCards[2].value = Math.min(99, Math.round(d.ratingCount / d.userCount * 100)) + '%';
          }
        }
      } catch (e) { /* API不可用，保持 '---' */ }
      // 2. 加载价格分布（免费游戏占比 — 使用 total 字段计算百分比）
      try {
        const { data: pd } = await getPriceDistribution();
        if (pd.code === 200 && pd.data) {
          const d = pd.data;
          if (d.freeCount && d.total && d.total > 0) {
            const v = d.freeCount / d.total * 100;
            this.statCards[1].value = v.toFixed(1) + '%';
            this.animateCardNumber(1, v, 1000, 1);
            this.$set(this.cardLoaded, 1, true);
          }
        }
      } catch (e) { /* API不可用 */ }
      // 3. 加载购买游玩转化率（精确值）
      try {
        const { data: pr } = await getPurchasePlayRatio();
        if (pr.code === 200 && pr.data && pr.data.overallRatio) {
          const v = Math.round(parseFloat(pr.data.overallRatio));
          this.statCards[2].value = v + '%';
          this.animateCardNumber(2, v, 1200, 0);
          this.$set(this.cardLoaded, 2, true);
        }
      } catch (e) { /* 使用概览估算值 */ }
      // 4. 加载最近游戏（筛选有上市日期的，按日期降序）
      try {
        const { data: r } = await listGames(1, 20, '', 'releaseDate');
        if (r.code === 200) {
          const all = r.data.records || [];
          // 筛选出有有效上市日期的游戏，按日期降序排列
          this.recentGames = all
            .filter(g => g.releaseDate && g.releaseDate !== '0000-00-00' && !isNaN(new Date(g.releaseDate)))
            .sort((a, b) => new Date(b.releaseDate) - new Date(a.releaseDate))
            .slice(0, 20);
        }
      } catch (e) { this.recentGames = []; }
      // 5. 加载 KPI 迷你趋势数据（替代硬编码折线）
      try {
        const { data: kt } = await getKpiTrends(20);
        if (kt.code === 200 && kt.data) {
          this._kpiTrends = kt.data;
          this.generateSparkLines();
        }
      } catch (e) { /* 保持默认空白 */ }
    },
    async initPriceChart() {
      if (!this.$refs.priceChart) return;
      this.priceChart = echarts.init(this.$refs.priceChart);

      // 从后端API获取价格分布数据
      try {
        const { data: r } = await getPriceDistribution();
        const d = (r && r.data) || {};
        const stats = d.priceStats || {};
        const buckets = [
          { name: '免费', count: d.freeCount || 0 },
          { name: '0-5元', count: stats.price_0_5 || 0 },
          { name: '5-10元', count: stats.price_5_10 || 0 },
          { name: '10-20元', count: stats.price_10_20 || 0 },
          { name: '20-30元', count: stats.price_20_30 || 0 },
          { name: '30-60元', count: stats.price_30_60 || 0 },
          { name: '60元以上', count: stats.price_60_plus || 0 }
        ];
        this.renderPriceChart(buckets);
      } catch (e) {
        // API不可用时显示空图表
        this.renderPriceChart([]);
      }
    },
    renderPriceChart(buckets) {
      if (!this.priceChart) return;
      const names = buckets.map(b => b.name);
      const counts = buckets.map(b => b.count);
      // 统一从 echarts-theme 读取双主题令牌（JS 零硬编码）
      const t = chartTheme();
      const palette = t.color;
      this.priceChart.setOption({
        grid: { left: 50, right: 20, top: 50, bottom: 40 },
        xAxis: {
          type: 'category',
          data: names.length > 0 ? names : ['暂无数据'],
          axisLine: { lineStyle: { color: t.axisLine } },
          axisTick: { show: false },
          axisLabel: { color: t.axisColor, fontSize: 11 }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: t.gridColor } },
          axisLabel: { color: t.axisColor, fontSize: 11 }
        },
        tooltip: Object.assign({ trigger: 'axis' }, t.tooltip),
        series: [{
          type: 'bar',
          data: counts.map((cnt, i) => ({
            value: cnt,
            itemStyle: {
              color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: palette[i % palette.length] },
                  { offset: 1, color: this.hexToRgba(palette[i % palette.length], 0.35) }
                ]
              },
              borderRadius: [6, 6, 0, 0]
            }
          })),
          barWidth: 32,
          label: {
            show: true,
            position: 'top',
            color: t.textColor,
            fontSize: 12,
            fontWeight: 700,
            distance: 6,
            textBorderColor: t.tooltip.backgroundColor,
            textBorderWidth: 2
          }
        }]
      });
    },
    hasImage(g) {
      return !this.imageFailed[g.gameId];
    },
    markImageFailed(g) {
      this.$set(this.imageFailed, g.gameId, true);
    },
    getImageUrl(g) {
      // 优先使用本地封面图
      return `/images/games/${g.gameId}.jpg`;
    },
    coverStyle(g) {
      const hash = ((g.gameId || 0) * 2654435761) >>> 0;
      const h1 = hash % 360;
      const h2 = (h1 + 50) % 360;
      return `linear-gradient(135deg, hsl(${h1},65%,55%) 0%, hsl(${h2},70%,45%) 100%)`;
    },
    getInitial(g) {
      const s = g.gameNameCn || g.gameName || '?';
      return s.trim().charAt(0).toUpperCase();
    },
    formatDate(s) {
      if (!s) return '-';
      const d = new Date(s);
      if (isNaN(d)) return s;
      return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日`;
    },
    goGame(id) { this.$router.push(`/game/${id}`); },
    handleResize() { if (this.priceChart) this.priceChart.resize(); }
  }
};
</script>

<style scoped>
.refactor-home { padding: 0 0 var(--space-6) 0; }

/* ===== Top Banner ===== */
.top-banner {
  position: relative;
  background: var(--banner-bg-gradient-2, var(--banner-bg-gradient));
  margin: 0;
  padding: var(--space-6) var(--space-8) var(--space-4);
  overflow: hidden;
  color: var(--banner-text-color);
  border-radius: 0;
  transition: background var(--duration-slow) var(--ease-out-expo);
  isolation: isolate;
}

/* 简洁背景层（无动态效果） */
.banner-bg-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}
.banner-bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.6) 0%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.6) 0%, transparent 75%);
}
:global(:root[data-theme="light"]) .banner-bg-grid {
  background-image:
    linear-gradient(rgba(99,102,241,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99,102,241,0.05) 1px, transparent 1px);
}
.banner-bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.25;
  pointer-events: none;
}
.banner-bg-glow--1 {
  width: 300px; height: 300px;
  background: var(--banner-shape-1);
  top: -80px; right: -60px;
}

.banner-inner {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
  gap: var(--space-5);
}

.banner-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--banner-eyebrow-color);
  letter-spacing: 2px;
  margin-bottom: 8px;
  text-transform: uppercase;
  font-weight: 600;
}
.eyebrow-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--banner-eyebrow-color);
  box-shadow: 0 0 6px currentColor;
}
.eyebrow-pulse {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 8px var(--color-success);
  animation: live-pulse 1.4s ease-in-out infinite;
  margin-left: 4px;
}
@keyframes live-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.4; transform: scale(0.6); }
}

.banner-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--banner-text-color);
  margin: 0 0 10px 0;
  letter-spacing: -0.3px;
  line-height: 1.2;
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  flex-wrap: wrap;
}
.title-text {
  background: linear-gradient(90deg, var(--banner-text-color) 0%, var(--banner-shape-1) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.title-system {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--banner-eyebrow-color);
  padding: 3px 8px;
  border: 1px solid var(--banner-tag-bg);
  border-radius: 4px;
  background: var(--banner-tag-bg);
  backdrop-filter: blur(4px);
  -webkit-text-fill-color: initial;
}
:global(:root[data-theme="light"]) .title-text {
  background: linear-gradient(90deg, #1e1b4b 0%, #4f46e5 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

.banner-tags { display: flex; gap: 10px; margin-top: 4px; flex-wrap: wrap; }
.banner-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  background: var(--banner-tag-bg);
  border: 1px solid var(--banner-tag-bg);
  border-radius: 999px;
  font-size: 12px;
  color: var(--banner-tag-color);
  font-weight: 500;
  letter-spacing: 0.5px;
  backdrop-filter: blur(6px);
  transition: all 0.2s;
}
.banner-tag:hover {
  transform: translateY(-1px);
  border-color: var(--banner-shape-1);
}
.tag-bullet {
  width: 4px; height: 4px;
  border-radius: 50%;
  background: var(--banner-shape-1);
  box-shadow: 0 0 4px currentColor;
}

.status-cluster {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: 18px 24px;
  background: var(--banner-card-bg);
  border: 1px solid var(--banner-card-border);
  border-radius: 14px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 24px -8px var(--banner-card-glow);
}
.status-block { text-align: center; min-width: 88px; }
.status-num {
  font-family: var(--font-family-mono, monospace);
  font-size: 24px;
  font-weight: 800;
  color: var(--banner-text-color);
  line-height: 1.1;
  letter-spacing: -0.5px;
}
.status-label {
  font-size: 11px;
  color: var(--banner-eyebrow-color);
  margin-top: 4px;
  letter-spacing: 0.5px;
}
.status-bar {
  width: 60%;
  height: 2px;
  margin: 6px auto 0;
  background: var(--banner-tag-bg);
  border-radius: 1px;
  overflow: hidden;
}
.status-bar-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--banner-shape-1), var(--banner-shape-3));
  border-radius: 1px;
  animation: bar-fill 1.5s var(--ease-out-expo) forwards;
  transform-origin: left;
}
@keyframes bar-fill {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}
.status-divider {
  width: 1px;
  height: 36px;
  background: var(--banner-tag-bg);
  flex-shrink: 0;
}

/* Stat cards row — 大型KPI卡（4列） */
.stat-cards {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-top: var(--space-2);
}
.stat-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 18px 12px;
  border-radius: 14px;
  background: var(--banner-card-bg);
  border: 1px solid var(--banner-card-border);
  color: var(--banner-text-color, #fafafa);
  overflow: hidden;
  isolation: isolate;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: transform 0.4s var(--ease-out-expo, ease-out),
              box-shadow 0.4s ease,
              border-color 0.4s ease;
  opacity: 0;
  transform: translateY(12px);
  animation: stat-card-in 0.6s var(--ease-out-expo, ease-out) forwards;
}
.stat-card:nth-child(1) { animation-delay: 0.05s; }
.stat-card:nth-child(2) { animation-delay: 0.15s; }
.stat-card:nth-child(3) { animation-delay: 0.25s; }
.stat-card:nth-child(4) { animation-delay: 0.35s; }
.stat-card.is-loaded {
  border-color: var(--accent-soft, rgba(74,144,226,0.35));
  box-shadow: 0 8px 24px -8px var(--accent-bg, rgba(74,144,226,0.15));
}
@keyframes stat-card-in {
  to { opacity: 1; transform: translateY(0); }
}
.stat-card:hover {
  transform: translateY(-4px);
  border-color: var(--accent, #4a90e2);
  box-shadow:
    0 16px 32px -8px var(--accent-bg, rgba(74,144,226,0.25)),
    0 0 0 1px var(--accent-soft, rgba(74,144,226,0.4)) inset;
}

/* 内部光晕（跟随accent色） */
.card-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 0%, var(--accent-bg, rgba(74,144,226,0.18)) 0%, transparent 60%);
  opacity: 0.7;
  pointer-events: none;
  z-index: -1;
  transition: opacity 0.4s ease;
}
.stat-card:hover .card-glow { opacity: 1; }

/* 扫描线（已移除，无需样式） */
.card-scan { display: none; }

/* 角落L形角标（科技感） */
.card-corner {
  position: absolute;
  width: 12px; height: 12px;
  border-color: var(--accent, #4a90e2);
  opacity: 0.6;
  transition: opacity 0.3s ease, width 0.3s ease, height 0.3s ease;
  pointer-events: none;
  z-index: 1;
}
.card-corner--tl { top: 6px; left: 6px;  border-top: 1.5px solid; border-left: 1.5px solid; }
.card-corner--tr { top: 6px; right: 6px; border-top: 1.5px solid; border-right: 1.5px solid; }
.card-corner--bl { bottom: 6px; left: 6px;  border-bottom: 1.5px solid; border-left: 1.5px solid; }
.card-corner--br { bottom: 6px; right: 6px; border-bottom: 1.5px solid; border-right: 1.5px solid; }
.stat-card:hover .card-corner { opacity: 1; width: 16px; height: 16px; }

/* 顶部：图标 + 标签 + 趋势 */
.stat-card-top {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  z-index: 2;
}
.stat-card-icon {
  position: relative;
  width: 38px; height: 38px;
  flex-shrink: 0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-bg, rgba(74,144,226,0.1));
  color: var(--accent, #4a90e2);
  border: 1px solid var(--accent-soft, rgba(74,144,226,0.3));
}
.icon-ring {
  position: absolute;
  inset: -3px;
  border-radius: 14px;
  border: 1px dashed var(--accent-soft, rgba(74,144,226,0.4));
  animation: ring-rotate 14s linear infinite;
  opacity: 0.55;
}
@keyframes ring-rotate {
  to { transform: rotate(360deg); }
}
.icon-glyph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px; height: 18px;
}

.stat-card-meta { flex: 1; min-width: 0; }
.stat-card-label {
  font-size: 13px;
  color: var(--banner-eyebrow-color, rgba(255,255,255,0.85));
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-card-sub {
  font-size: 10px;
  color: var(--banner-eyebrow-color, rgba(255,255,255,0.5));
  letter-spacing: 0.6px;
  text-transform: uppercase;
  margin-top: 1px;
  opacity: 0.7;
}

.stat-card-trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  font-family: var(--font-family-mono, monospace);
  white-space: nowrap;
  flex-shrink: 0;
}
.stat-card-trend.is-up {
  background: var(--color-success-bg);
  color: var(--color-success);
  border: 1px solid var(--color-success-border);
}
.stat-card-trend.is-down {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-danger-border);
}

/* 中部：超大数字 */
.stat-card-value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-top: -2px;
  position: relative;
  z-index: 2;
}
.stat-card-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  color: var(--accent, #4a90e2);
  text-shadow: 0 0 24px var(--accent-bg, rgba(74,144,226,0.3));
  line-height: 1;
}
.value-num {
  font-family: var(--font-family-mono, 'JetBrains Mono', monospace);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, var(--banner-text-color, #fff) 0%, var(--accent, #4a90e2) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: var(--banner-text-color, #fff);
}
.value-unit {
  font-size: 13px;
  font-weight: 600;
  color: var(--banner-eyebrow-color, rgba(255,255,255,0.65));
  letter-spacing: 0.5px;
}

/* 底部：迷你图 */
.stat-card-spark-wrap {
  height: 40px;
  margin: 0 -4px -4px;
  position: relative;
  color: currentColor;
  z-index: 2;
}
.stat-card-spark {
  width: 100%;
  height: 100%;
  display: block;
  color: var(--accent, #4a90e2);
  opacity: 0.85;
}
.stat-spark-line {
  stroke-dasharray: 600;
  stroke-dashoffset: 600;
  animation: spark-draw 1.4s var(--ease-out-expo, ease-out) 0.3s forwards;
}
@keyframes spark-draw {
  to { stroke-dashoffset: 0; }
}
.stat-spark-dot {
  opacity: 0;
  animation: spark-dot-in 0.4s ease 1.5s forwards;
  filter: drop-shadow(0 0 3px currentColor);
}
.stat-spark-pulse {
  display: none;
}

/* Nav tabs - 优化版 */
.nav-tabs {
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border-muted);
  overflow-x: auto;
  scrollbar-width: none;
  position: relative;
}
.nav-tabs::-webkit-scrollbar { display: none; }
.nav-tabs::before {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-border-default), transparent);
  pointer-events: none;
}
.nav-tabs-inner {
  display: flex;
  max-width: var(--content-max-width);
  margin: 0 auto;
  padding: 0 var(--space-8);
  gap: 0;
}
.nav-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 22px;
  text-decoration: none;
  color: var(--color-text-tertiary);
  font-size: 13px;
  font-weight: 500;
  position: relative;
  transition: all 0.2s;
  white-space: nowrap;
  overflow: hidden;
}
.nav-tab::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--color-brand-a08), var(--color-accent-a04));
  opacity: 0;
  transition: opacity 0.2s;
  z-index: -1;
}
.nav-tab:hover {
  color: var(--color-text-link);
}
.nav-tab:hover::before {
  opacity: 1;
}
.nav-tab--active {
  color: var(--color-text-link);
  font-weight: 600;
}
.nav-tab--active::before {
  opacity: 1;
}
.nav-tab-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--color-brand-500), var(--color-accent-500));
  transform: translateX(-50%);
  transition: width 0.3s var(--ease-out-expo);
  border-radius: 2px 2px 0 0;
  box-shadow: 0 0 8px var(--color-brand-a50);
}
.nav-tab--active .nav-tab-indicator {
  width: 60%;
}
.nav-tab-icon {
  display: flex;
  align-items: center;
  width: 18px;
  height: 18px;
}

/* Content row */
.content-row {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: var(--space-5);
  padding: var(--space-6) var(--space-8) 0;
  max-width: var(--content-max-width);
  margin: 0 auto;
}
.content-card {
  position: relative;
  background: var(--color-bg-elevated);
  border-radius: 12px;
  padding: var(--space-5);
  border: 1px solid var(--color-border-muted);
  transition: border-color 0.2s, transform 0.2s, box-shadow 0.2s;
  overflow: hidden;
}
.content-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-border-default), transparent);
  opacity: 0;
  transition: opacity 0.2s;
}
.content-card:hover {
  border-color: var(--color-border-default);
  box-shadow: var(--shadow-md);
}
.content-card:hover::before {
  opacity: 1;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  gap: var(--space-3);
}
.card-head-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
  letter-spacing: -0.2px;
}
.card-subtitle {
  font-size: 10px;
  color: var(--color-text-tertiary);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  font-weight: 500;
}
.card-meta {
  font-size: 12px;
  color: var(--color-text-tertiary);
  padding: 4px 10px;
  background: var(--color-bg-subtle);
  border-radius: 999px;
}
.card-legend {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}
.time-chart { width: 100%; height: 320px; }

/* Recent games list - 优化版 */
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 480px;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-width: thin;
}
.recent-list::-webkit-scrollbar { width: 4px; }
.recent-list::-webkit-scrollbar-thumb { background: var(--color-border-muted); border-radius: 2px; }
.recent-item {
  display: flex;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s var(--ease-out-expo);
  align-items: center;
  border: 1px solid transparent;
  position: relative;
  animation: item-in 0.4s var(--ease-out-expo) backwards;
  animation-delay: calc(var(--idx) * 30ms);
}
@keyframes item-in {
  from { opacity: 0; transform: translateX(-8px); }
  to   { opacity: 1; transform: translateX(0); }
}
.recent-item:hover {
  background: var(--color-bg-overlay);
  border-color: var(--color-border-default);
  transform: translateX(2px);
}
.recent-rank {
  font-family: var(--font-family-mono);
  font-size: 11px;
  font-weight: 700;
  color: var(--color-text-tertiary);
  width: 22px;
  text-align: center;
  flex-shrink: 0;
}
.recent-item:hover .recent-rank {
  color: var(--color-brand-500);
}
.recent-cover {
  width: 80px;
  height: 50px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-weight: 700;
  font-size: 18px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.10);
  overflow: hidden;
  position: relative;
}
.recent-cover img,
.recent-cover-img {
  width: 100%; height: 100%; object-fit: cover;
  display: block;
}
.recent-cover-letter {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0,0,0,0.4);
}
.recent-info { flex: 1; min-width: 0; }
.recent-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.recent-meta {
  font-size: 12px;
  color: var(--color-text-tertiary);
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.recent-meta b { color: var(--color-text-primary); font-weight: 600; }
.price--free { color: var(--color-success); }
.price--paid { color: var(--color-warning); }
.recent-arrow {
  color: var(--color-text-tertiary);
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.2s;
  flex-shrink: 0;
}
.recent-item:hover .recent-arrow {
  opacity: 1;
  transform: translateX(0);
  color: var(--color-brand-500);
}
.loading-tip {
  text-align: center;
  padding: 40px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

/* Responsive */
@media (max-width: 1100px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .content-row { grid-template-columns: 1fr; }
  .banner-title { font-size: 24px; }
  .title-accent { font-size: 26px; }
  .status-cluster { padding: 10px 14px; }
  .status-num { font-size: 18px; }
  .status-block { min-width: 64px; }
  .value-num { font-size: 26px; }
}
@media (max-width: 600px) {
  .top-banner { padding: var(--space-5) var(--space-4) var(--space-4); }
  .banner-inner { flex-direction: column; align-items: stretch; }
  .status-cluster { justify-content: space-around; }
  .stat-cards { grid-template-columns: 1fr 1fr; gap: var(--space-3); }
  .value-num { font-size: 22px; }
  .stat-card { padding: 14px 14px 10px; gap: 8px; }
  .stat-card-icon { width: 32px; height: 32px; }
  .nav-tabs-inner { padding: 0 var(--space-4); }
  .content-row { padding: var(--space-3) var(--space-4) 0; }
  .nav-tab { padding: 14px 14px; font-size: 13px; }
  .banner-title { font-size: 18px; }
  .title-accent { font-size: 20px; }
  .title-system { display: none; }
}
</style>
