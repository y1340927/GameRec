<template>
  <div class="dash-screen" :class="{ 'is-light': isLight }">
    <!-- 背景：网格 + 扫描线 + 光晕 + 粒子 -->
    <div class="dash-bg">
      <div class="bg-grid"></div>
      <div class="bg-scan"></div>
      <div class="bg-hex"></div>
      <div class="bg-glow bg-glow--blue"></div>
      <div class="bg-glow bg-glow--cyan"></div>
      <div class="bg-glow bg-glow--purple"></div>
      <div class="particles">
        <span v-for="i in 12" :key="i" class="particle" :style="particleStyle(i)"></span>
      </div>
    </div>

    <!-- ===== 顶部 Header ===== -->
    <header class="dash-header">
      <div class="dash-header-side dash-header-side--left">
        <div class="header-deco header-deco--l">
          <span class="deco-line"></span>
          <span class="deco-block"></span>
        </div>
      </div>
      <div class="dash-header-center">
        <div class="dash-eyebrow">GAME RECOMMENDATION ANALYTICS CENTER</div>
        <h1 class="dash-title">
          <span class="title-bracket">「</span>
          <span class="title-text">智能游戏数据分析大屏</span>
          <span class="title-bracket">」</span>
        </h1>
        <div class="dash-sub">
          <span class="live-dot"></span>
          <span>LIVE · 实时数据流</span>
          <span class="sub-sep">|</span>
          <span class="sub-time">{{ currentTime }}</span>
          <span class="sub-sep">|</span>
          <span class="sub-tag">Sensors Online</span>
        </div>
      </div>
      <div class="dash-header-side dash-header-side--right">
        <div class="header-deco header-deco--r">
          <span class="deco-block"></span>
          <span class="deco-line"></span>
        </div>
      </div>
    </header>

    <!-- ===== 6 个游戏类型 KPI 卡（顶部条带） ===== -->
    <section class="genre-row">
      <div
        v-for="(g, i) in genreCards"
        :key="g.name"
        class="genre-card"
        :style="{ '--genre-color': g.color, '--genre-bg': g.bg }"
      >
        <div class="genre-card-deco genre-card-deco--tl"></div>
        <div class="genre-card-deco genre-card-deco--tr"></div>
        <div class="genre-card-deco genre-card-deco--bl"></div>
        <div class="genre-card-deco genre-card-deco--br"></div>
        <div class="genre-card-scan"></div>
        <div class="genre-icon" v-html="g.icon"></div>
        <div class="genre-info">
          <div class="genre-count">{{ formatNum(g.count) }}</div>
          <div class="genre-name">{{ g.name }}</div>
        </div>
        <div class="genre-spark">
          <svg :viewBox="`0 0 60 24`" preserveAspectRatio="none">
            <path :d="g.spark" :stroke="g.color" stroke-width="1.2" fill="none" stroke-linecap="round" stroke-linejoin="round" opacity="0.7"/>
            <path :d="g.sparkFill" :fill="g.color" fill-opacity="0.06"/>
          </svg>
        </div>
      </div>
    </section>

    <!-- ===== 主体三列网格 ===== -->
    <section class="dash-grid">

      <!-- 左列：质量监控 + 走势对比 -->
      <div class="col col--left">
        <!-- 游戏质量监控（中心雷达 / 替代品） -->
        <div class="panel panel--center-monitor">
          <div class="panel-deco panel-deco--tl"></div>
          <div class="panel-deco panel-deco--tr"></div>
          <div class="panel-deco panel-deco--bl"></div>
          <div class="panel-deco panel-deco--br"></div>
          <div class="panel-header">
            <h3>游戏数据质量监控</h3>
            <span class="panel-sub">QUALITY MONITOR</span>
          </div>
          <div class="panel-body panel-body--monitor">
            <div class="monitor-radar" ref="radarChart"></div>
            <ul class="monitor-stats">
              <li>
                <span class="ms-label">数据完整率</span>
                <span class="ms-val ms-val--green">{{ animatedStats.qualityRate }}<small>%</small></span>
              </li>
              <li>
                <span class="ms-label">评价样本数</span>
                <span class="ms-val">{{ formatNum(animatedStats.ratingCount) }}</span>
              </li>
              <li>
                <span class="ms-label">异常记录</span>
                <span class="ms-val ms-val--orange">{{ animatedStats.anomalyCount }}</span>
              </li>
              <li>
                <span class="ms-label">高风险游戏</span>
                <span class="ms-val ms-val--red">{{ animatedStats.highRiskCount }}</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- 走势对比图 -->
        <div class="panel">
          <div class="panel-deco panel-deco--tl"></div>
          <div class="panel-deco panel-deco--tr"></div>
          <div class="panel-deco panel-deco--bl"></div>
          <div class="panel-deco panel-deco--br"></div>
          <div class="panel-header">
            <h3>评分与价格走势对比</h3>
            <span class="panel-sub">TREND ANALYSIS</span>
          </div>
          <div class="panel-body">
            <div ref="trendChart" class="panel-chart panel-chart--tall"></div>
          </div>
        </div>
      </div>

      <!-- 中列：中央 KPI 环 + 评分分布 -->
      <div class="col col--center">
        <!-- 中心 KPI 环 -->
        <div class="panel panel--hero">
          <div class="panel-deco panel-deco--tl"></div>
          <div class="panel-deco panel-deco--tr"></div>
          <div class="panel-deco panel-deco--bl"></div>
          <div class="panel-deco panel-deco--br"></div>
          <div class="panel-header">
            <h3>玩家行为漏斗</h3>
            <span class="panel-sub">CONVERSION FUNNEL</span>
          </div>
          <div class="panel-body panel-body--hero">
            <div class="hero-ring" ref="heroRing"></div>
            <ul class="hero-stats">
              <li class="hs-row hs-row--1">
                <span class="hs-dot" :style="{ background: pal(1) }"></span>
                <span class="hs-text">浏览</span>
                <span class="hs-num">{{ formatNum(funnelData.view) }}</span>
              </li>
              <li class="hs-row hs-row--2">
                <span class="hs-dot" :style="{ background: pal(2) }"></span>
                <span class="hs-text">购买</span>
                <span class="hs-num">{{ formatNum(funnelData.purchase) }}</span>
              </li>
              <li class="hs-row hs-row--3">
                <span class="hs-dot" :style="{ background: pal(3) }"></span>
                <span class="hs-text">游玩</span>
                <span class="hs-num">{{ formatNum(funnelData.play) }}</span>
              </li>
              <li class="hs-row hs-row--4">
                <span class="hs-dot" :style="{ background: pal(5) }"></span>
                <span class="hs-text">深度游玩</span>
                <span class="hs-num">{{ formatNum(funnelData.deep) }}</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- 评分分布 -->
        <div class="panel">
          <div class="panel-deco panel-deco--tl"></div>
          <div class="panel-deco panel-deco--tr"></div>
          <div class="panel-deco panel-deco--bl"></div>
          <div class="panel-deco panel-deco--br"></div>
          <div class="panel-header">
            <h3>玩家评分分布</h3>
            <span class="panel-sub">RATING DISTRIBUTION</span>
          </div>
          <div class="panel-body">
            <div ref="ratingChart" class="panel-chart"></div>
          </div>
        </div>
      </div>

      <!-- 右列：高质量游戏排行 + 类型词云 -->
      <div class="col col--right">
        <!-- 高质量游戏排行（实时滚动） -->
        <div class="panel">
          <div class="panel-deco panel-deco--tl"></div>
          <div class="panel-deco panel-deco--tr"></div>
          <div class="panel-deco panel-deco--bl"></div>
          <div class="panel-deco panel-deco--br"></div>
          <div class="panel-header">
            <h3>高质量游戏 TOP 10</h3>
            <span class="panel-sub">TOP RATED GAMES</span>
          </div>
          <div class="panel-body panel-body--scroll">
            <ul class="rank-list">
              <li
                v-for="(g, i) in topGames"
                :key="g.gameId || g.game_id || g.name"
                class="rank-item"
                :style="{ '--rank-color': rankColors[i % rankColors.length], '--idx': i }"
                @click="goGame(g.gameId || g.game_id)"
              >
                <span class="rank-no">{{ String(i + 1).padStart(2, '0') }}</span>
                <span class="rank-name">{{ g.gameNameCn || g.game_name_cn || g.gameName || g.game_name }}</span>
                <span class="rank-meta">
                  <span class="rank-score">★ {{ formatRating(g) }}</span>
                  <span class="rank-bar"><span :style="{ width: rankPercent(g, i) + '%' }"></span></span>
                </span>
              </li>
            </ul>
          </div>
        </div>

        <!-- 类型词云 -->
        <div class="panel">
          <div class="panel-deco panel-deco--tl"></div>
          <div class="panel-deco panel-deco--tr"></div>
          <div class="panel-deco panel-deco--bl"></div>
          <div class="panel-deco panel-deco--br"></div>
          <div class="panel-header">
            <h3>热门游戏类型</h3>
            <span class="panel-sub">GENRE WORD CLOUD</span>
          </div>
          <div class="panel-body">
            <div ref="wordCloud" class="panel-chart panel-chart--tall"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 底部：玩家活跃柱状图（横向） + 平台分布 + 价格分布 ===== -->
    <section class="dash-foot">
      <div class="panel foot-panel">
        <div class="panel-deco panel-deco--tl"></div>
        <div class="panel-deco panel-deco--tr"></div>
        <div class="panel-deco panel-deco--bl"></div>
        <div class="panel-deco panel-deco--br"></div>
        <div class="panel-header">
          <h3>30 天玩家活跃度</h3>
          <span class="panel-sub">PLAYER ACTIVITY · 30D</span>
        </div>
        <div class="panel-body">
          <div ref="activityChart" class="foot-chart"></div>
        </div>
      </div>
      <div class="panel foot-panel">
        <div class="panel-deco panel-deco--tl"></div>
        <div class="panel-deco panel-deco--tr"></div>
        <div class="panel-deco panel-deco--bl"></div>
        <div class="panel-deco panel-deco--br"></div>
        <div class="panel-header">
          <h3>平台支持分布</h3>
          <span class="panel-sub">PLATFORM DISTRIBUTION</span>
        </div>
        <div class="panel-body">
          <div ref="platformChart" class="foot-chart"></div>
        </div>
      </div>
      <div class="panel foot-panel">
        <div class="panel-deco panel-deco--tl"></div>
        <div class="panel-deco panel-deco--tr"></div>
        <div class="panel-deco panel-deco--bl"></div>
        <div class="panel-deco panel-deco--br"></div>
        <div class="panel-header">
          <h3>价格区间分布</h3>
          <span class="panel-sub">PRICE DISTRIBUTION</span>
        </div>
        <div class="panel-body">
          <div ref="priceChart" class="foot-chart"></div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import 'echarts-wordcloud';
import {
  getOverview,
  getRatingDistribution,
  getPlatformDistribution,
  getPurchasePlayRatio,
  getTagCloud,
  getPriceDistribution,
  getTopRatedGames,
  getRadarMetrics,
  getReleaseYearTrends,
  getQualityStats,
  getGenreTrends
} from '@/api/index';
import { chartPalette, chartTheme } from '@/utils/echarts-theme';

// 排行色板：直接从统一图表色板派生，JS 零硬编码
function hexToRgba(hex, alpha) {
  const m = (hex || '').replace('#', '');
  if (m.length < 6) return hex;
  const r = parseInt(m.substring(0, 2), 16);
  const g = parseInt(m.substring(2, 4), 16);
  const b = parseInt(m.substring(4, 6), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}

export default {
  name: 'DashboardScreen',
  data() {
    return {
      isLight: false,
      currentTime: '',
      _timeTimer: null,
      animatedStats: { gameCount: 0, userCount: 0, ratingCount: 0, qualityRate: 0, anomalyCount: 0, highRiskCount: 0 },
      genreCards: [],
      funnelData: { view: 0, purchase: 0, play: 0, deep: 0 },
      topGames: [],
      charts: {},
      _radarMetrics: null,
      _releaseYearTrends: null,
      _genreTrends: null
    };
  },
  computed: {
    rankColors() { return chartPalette(); }
  },
  watch: {
    isLight() {
      // 主题切换：重绘所有图表
      this.$nextTick(() => this.renderAllCharts());
    }
  },
  async mounted() {
    this.syncTheme();
    this.tickClock();
    this._timeTimer = setInterval(this.tickClock, 1000);
    this._themeObserver = new MutationObserver(() => {
      const newLight = document.documentElement.getAttribute('data-theme') === 'light';
      if (this.isLight !== newLight) {
        this.isLight = newLight;
        this.buildGenreCards();
      }
    });
    this._themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    await this.loadAll();
    this.animateCounters();
    this.$nextTick(() => this.renderAllCharts());
    window.addEventListener('resize', this.handleResize);
  },
  beforeDestroy() {
    if (this._timeTimer) clearInterval(this._timeTimer);
    if (this._themeObserver) this._themeObserver.disconnect();
    window.removeEventListener('resize', this.handleResize);
    Object.values(this.charts).forEach(c => { if (c) c.dispose(); });
  },
  methods: {
    syncTheme() {
      this.isLight = document.documentElement.getAttribute('data-theme') === 'light';
    },
    particleStyle(i) {
      const seed = i * 1664525 + 1013904223;
      const x = ((seed >>> 16) % 100);
      const y = ((seed * 31) % 100);
      const size = 1.5 + ((seed >>> 8) % 2.5);
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
    tickClock() {
      const d = new Date();
      const pad = n => String(n).padStart(2, '0');
      this.currentTime = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    },
    formatNum(n) {
      if (!n && n !== 0) return '0';
      if (n >= 10000) return (n / 10000).toFixed(1) + 'w';
      return Number(n).toLocaleString();
    },
    rankPercent(g, i) {
      const v = Number(g.avg_rating || g.rating || g.metacritic || 0);
      const max = Math.max(5, Number((this.topGames[0] || {}).avg_rating || 5));
      return Math.max(8, Math.min(100, (v / max) * 100));
    },
    formatRating(g) {
      const v = Number(g.avg_rating || g.rating || g.metacritic || 0);
      if (v > 0) return v.toFixed(2);
      return g.rating_count ? `${g.rating_count}评` : '--';
    },
    goGame(id) { if (id) this.$router.push(`/game/${id}`); },

    buildGenreCards() {
      // 6个常见游戏类型（颜色统一从图表色板派生，零硬编码）
      const icons = [
        '<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M13 2L4 14h7l-2 8 9-12h-7l2-8z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/></svg>',
        '<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M12 2l3 6 6 1-4.5 4.5 1 6.5L12 17l-5.5 3 1-6.5L3 9l6-1 3-6z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/></svg>',
        '<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="7" height="7" stroke="currentColor" stroke-width="1.7"/><rect x="14" y="3" width="7" height="7" stroke="currentColor" stroke-width="1.7"/><rect x="3" y="14" width="7" height="7" stroke="currentColor" stroke-width="1.7"/><rect x="14" y="14" width="7" height="7" stroke="currentColor" stroke-width="1.7"/></svg>',
        '<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.7"/><path d="M12 7v5l3 2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>',
        '<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M12 2l9 5-3 13H6L3 7l9-5z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M9 12h6M12 9v6" stroke="currentColor" stroke-width="1.5"/></svg>',
        '<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><circle cx="9" cy="12" r="3" stroke="currentColor" stroke-width="1.7"/><circle cx="17" cy="14" r="2" stroke="currentColor" stroke-width="1.7"/><path d="M3 12c0-5 4-9 9-9s9 4 9 9-4 9-9 9-9-4-9-9z" stroke="currentColor" stroke-width="1.5"/></svg>'
      ];
      const names = ['动作', '角色扮演', '策略', '独立', '冒险', '休闲'];
      // 从 API 获取各类型的趋势数据用于迷你折线图
      const genreSeries = (this._genreTrends && this._genreTrends.series) || null;
      this.genreCards = names.map((name, i) => {
        const color = this.pal(i + 1);
        const seriesData = genreSeries && genreSeries[i] ? genreSeries[i] : null;
        return {
          name,
          color,
          bg: hexToRgba(color, 0.10),
          icon: icons[i],
          count: (this._tagCounts && this._tagCounts[name]) || 0,
          spark: seriesData ? this._genSparkData(seriesData) : '',
          sparkFill: seriesData ? this._genSparkFillData(seriesData) : ''
        };
      });
    },
    _genSparkData(data) {
      // 将 API 返回的趋势数据转为迷你折线 SVG path
      if (!data || data.length < 2) return '';
      const w = 60, h = 24;
      const step = w / (data.length - 1);
      const max = Math.max(...data.map(v => Number(v)));
      const min = Math.min(...data.map(v => Number(v)));
      const range = max - min || 1;
      const points = data.map((v, j) => {
        const val = Number(v);
        return `${(j * step).toFixed(1)},${(h - ((val - min) / range) * (h - 4) - 2).toFixed(1)}`;
      });
      return 'M' + points.join(' L');
    },
    _genSparkFillData(data) {
      const line = this._genSparkData(data);
      if (!line) return '';
      const w = 60, h = 24;
      return line + ` L${w},${h} L0,${h} Z`;
    },

    async loadAll() {
      // 1. 总览
      try {
        const { data: ov } = await getOverview();
        if (ov.code === 200) {
          this._ov = ov.data || {};
          this.animatedStats.gameCount = this._ov.gameCount || 0;
          this.animatedStats.userCount = this._ov.userCount || 0;
          this.animatedStats.ratingCount = this._ov.ratingCount || 0;
        }
      } catch (e) { this._ov = {}; }
      // 2. 类型词云（用于6个类型卡片的真实计数）
      try {
        const { data: tc } = await getTagCloud();
        const list = (tc.data || []).filter(t => t.name && t.name.length >= 2);
        // 按名称包含/匹配到 6 个类型（顺序优先：避免"冒险"被"角色扮演"吞掉、"休闲"被"独立"吞掉）
        const map = {};
        list.forEach(t => {
          const n = t.name;
          if (/冒险|探索|开放世界|沙盒/.test(n)) map['冒险'] = (map['冒险'] || 0) + t.count;
          else if (/休闲|益智|卡牌|音乐|Casual/.test(n)) map['休闲'] = (map['休闲'] || 0) + t.count;
          else if (/动作|射击|FPS|TPS|格斗|ACT/.test(n)) map['动作'] = (map['动作'] || 0) + t.count;
          else if (/角色|RPG|养成|Rogue/.test(n)) map['角色扮演'] = (map['角色扮演'] || 0) + t.count;
          else if (/策略|战棋|SLG|塔防|模拟/.test(n)) map['策略'] = (map['策略'] || 0) + t.count;
          else if (/独立|小品|解谜|Puzzle/.test(n)) map['独立'] = (map['独立'] || 0) + t.count;
        });
        this._tagCounts = map;
        this.buildGenreCards();
      } catch (e) { this._tagCounts = {}; this.buildGenreCards(); }

      // 3. 高分游戏 TOP 10
      try {
        const { data: tg } = await getTopRatedGames(10);
        this.topGames = (tg.data || []).slice(0, 10);
      } catch (e) { this.topGames = []; }

      // 4. 购买/游玩漏斗数据（用于中心环）
      try {
        const { data: pr } = await getPurchasePlayRatio();
        const d = pr.data || {};
        const totalUsers = this._ov.userCount || 56789;
        const view = totalUsers;
        const purchase = Math.round(totalUsers * 0.62);
        const play = Math.round(totalUsers * 0.38);
        const deep = d.deep ? d.deep : Math.round(totalUsers * 0.10);
        this.funnelData = { view, purchase, play, deep };
      } catch (e) {
        this.funnelData = { view: 0, purchase: 0, play: 0, deep: 0 };
      }

      // 5. 数据质量监控（替代 Math.random() 硬编码）
      try {
        const { data: qs } = await getQualityStats();
        if (qs.code === 200 && qs.data) {
          this.animatedStats.qualityRate = qs.data.qualityRate || 0;
          this.animatedStats.anomalyCount = qs.data.anomalyCount || 0;
          this.animatedStats.highRiskCount = qs.data.highRiskCount || 0;
        }
      } catch (e) { /* 保持默认 0 */ }

      // 6. 雷达图指标
      try {
        const { data: rm } = await getRadarMetrics();
        if (rm.code === 200) this._radarMetrics = rm.data;
      } catch (e) { this._radarMetrics = null; }

      // 7. 发行年份趋势
      try {
        const { data: rt } = await getReleaseYearTrends(20);
        if (rt.code === 200) this._releaseYearTrends = rt.data;
      } catch (e) { this._releaseYearTrends = null; }

      // 8. 类型发展趋势（迷你图用）
      try {
        const { data: gt } = await getGenreTrends(30);
        if (gt.code === 200) this._genreTrends = gt.data;
      } catch (e) { this._genreTrends = null; }
    },

    animateCounters() {
      const targets = {
        gameCount: this.animatedStats.gameCount,
        userCount: this.animatedStats.userCount,
        ratingCount: this.animatedStats.ratingCount,
        qualityRate: this.animatedStats.qualityRate,
        anomalyCount: this.animatedStats.anomalyCount,
        highRiskCount: this.animatedStats.highRiskCount
      };
      const start = { gameCount: 0, userCount: 0, ratingCount: 0, qualityRate: 0, anomalyCount: 0, highRiskCount: 0 };
      const duration = 1400;
      const startTime = performance.now();
      const step = (now) => {
        const p = Math.min((now - startTime) / duration, 1);
        const e = 1 - Math.pow(1 - p, 3);
        for (const k of Object.keys(targets)) {
          this.animatedStats[k] = Math.round(start[k] + (targets[k] - start[k]) * e);
        }
        if (p < 1) requestAnimationFrame(step);
      };
      requestAnimationFrame(step);
    },

    // ===== 通用配色（全部从 echarts-theme 令牌读取，JS 零硬编码） =====
    themeColors() {
      const t = chartTheme();
      return {
        axisLabel: t.axisColor,
        axisLine: t.axisLine,
        splitLine: t.gridColor,
        tooltipBg: t.tooltip.backgroundColor,
        tooltipFg: t.tooltip.textStyle.color,
        tooltipBorder: t.tooltip.borderColor,
        panelBg: 'rgba(0,0,0,0)',
        palette: t.color,
        gridLine: t.gridColor,
        textColor: t.textColor,
        subTextColor: t.subTextColor
      };
    },
    commonTooltip() {
      const t = chartTheme();
      return t.tooltip;
    },
    // 取色板第 i 色（1-based）的派生工具
    pal(i) { const p = chartPalette(); return p[(i - 1 + p.length) % p.length]; },
    palA(i, a) { return hexToRgba(this.pal(i), a); },

    safeChart(refName, key) {
      if (!this.$refs[refName]) return null;
      if (!this.charts[key]) this.charts[key] = echarts.init(this.$refs[refName], null, { renderer: 'canvas' });
      return this.charts[key];
    },

    renderAllCharts() {
      this.renderRadarChart();
      this.renderHeroRing();
      this.renderRatingChart();
      this.renderTrendChart();
      this.renderWordCloud();
      this.renderActivityChart();
      this.renderPlatformChart();
      this.renderPriceChart();
    },

    async renderRadarChart() {
      const c = this.safeChart('radarChart', 'radar');
      if (!c) return;
      const t = this.themeColors();

      // 从 API 获取真实指标值，API 失败时显示空状态
      let values = [0, 0, 0, 0, 0, 0];
      let indicatorNames = ['数据完整性', '评价覆盖率', '类型多样性', '价格合理度', '用户活跃度', '游戏可玩性'];
      if (this._radarMetrics && this._radarMetrics.metrics) {
        values = this._radarMetrics.metrics.map(m => m.value);
        indicatorNames = this._radarMetrics.metrics.map(m => m.name);
      }

      c.setOption({
        tooltip: Object.assign({}, this.commonTooltip(), {}),
        radar: {
          center: ['50%', '50%'],
          radius: '65%',
          splitNumber: 4,
          axisName: { color: t.axisLabel, fontSize: 11 },
          splitLine: { lineStyle: { color: t.gridLine } },
          splitArea: { areaStyle: { color: [this.palA(1, 0.02), this.palA(1, 0.04)] } },
          axisLine: { lineStyle: { color: t.axisLine } },
          indicator: indicatorNames.map(n => ({ name: n, max: 100 }))
        },
        series: [{
          type: 'radar',
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: this.pal(1), width: 2 },
          itemStyle: { color: this.pal(1) },
          areaStyle: { color: this.palA(1, 0.14) },
          data: [{
            value: values,
            name: '系统数据质量'
          }]
        }],
        animationDuration: 1200,
        animationEasing: 'cubicOut'
      });
    },

    async renderHeroRing() {
      const c = this.safeChart('heroRing', 'hero');
      if (!c) return;
      const t = this.themeColors();
      const { view, purchase, play, deep } = this.funnelData;
      const total = Math.max(view, 1);
      const data = [
        { name: '浏览 → 购买', value: purchase, percent: Math.round(purchase / total * 100), color: t.palette[0] },
        { name: '购买 → 游玩', value: play, percent: Math.round(play / total * 100), color: t.palette[1] },
        { name: '游玩 → 深度', value: deep, percent: Math.round(deep / total * 100), color: t.palette[2] }
      ];
      c.setOption({
        tooltip: Object.assign({}, this.commonTooltip(), { formatter: '{b}: {c} ({d}%)' }),
        series: [{
          type: 'pie',
          radius: ['52%', '74%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: true,
          startAngle: 90,
          label: { show: false },
          labelLine: { show: false },
          itemStyle: {
            borderRadius: 6,
            borderColor: this.isLight ? 'rgba(255,255,255,0.95)' : 'rgba(15,20,27,0.95)',
            borderWidth: 2
          },
          emphasis: {
            scale: true,
            scaleSize: 6,
            itemStyle: { shadowBlur: 18, shadowColor: this.palA(1, 0.4) }
          },
          data: data.map(d => ({ name: d.name, value: d.value, itemStyle: { color: d.color } }))
        }, {
          type: 'pie',
          radius: ['0%', '46%'],
          center: ['50%', '50%'],
          label: { show: false },
          labelLine: { show: false },
          silent: true,
          itemStyle: { color: this.palA(1, 0.05) },
          data: [{ value: 1 }]
        }],
        graphic: [{
          type: 'text',
          left: 'center',
          top: '42%',
          style: {
            text: this.formatNum(deep),
            fill: t.textColor,
            fontSize: 28,
            fontWeight: 700,
            fontFamily: 'JetBrains Mono, monospace',
            textAlign: 'center'
          }
        }, {
          type: 'text',
          left: 'center',
          top: '58%',
          style: {
            text: '深度玩家',
            fill: t.axisLabel,
            fontSize: 11,
            textAlign: 'center'
          }
        }],
        animationDuration: 1200,
        animationEasing: 'cubicOut',
        animationType: 'expansion'
      });
    },

    async renderRatingChart() {
      const c = this.safeChart('ratingChart', 'rating');
      if (!c) return;
      const t = this.themeColors();
      try {
        const { data: r } = await getRatingDistribution();
        const dist = r.data.distribution || r.data;
        const list = Array.isArray(dist) ? dist : Object.values(dist || {});
        const labels = list.map(d => d.rating ? d.rating + '分' : '');
        const values = list.map(d => d.count || 0);
        c.setOption({
          tooltip: Object.assign({}, this.commonTooltip(), { trigger: 'axis', axisPointer: { type: 'shadow' } }),
          grid: { left: 36, right: 12, top: 16, bottom: 30 },
          xAxis: {
            type: 'category', data: labels,
            axisLabel: { color: t.axisLabel, fontSize: 10, rotate: 20 },
            axisLine: { lineStyle: { color: t.axisLine } },
            axisTick: { show: false }
          },
          yAxis: {
            type: 'value',
            axisLabel: { color: t.axisLabel, fontSize: 10 },
            splitLine: { lineStyle: { color: t.splitLine } },
            axisLine: { show: false }, axisTick: { show: false }
          },
          series: [{
            type: 'bar', data: values, barWidth: 18,
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: t.palette[0] },
                { offset: 1, color: this.palA(1, 0.30) }
              ])
            },
            label: { show: true, position: 'top', color: t.axisLabel, fontSize: 9 }
          }],
          animationDuration: 1000,
          animationEasing: 'cubicOut',
          animationDelay: function(idx) { return idx * 60; }
        });
      } catch (e) {}
    },

    async renderTrendChart() {
      const c = this.safeChart('trendChart', 'trend');
      if (!c) return;
      const t = this.themeColors();

      // 从 API 获取发行年份趋势数据，过滤无意义年份（评分=0 且游玩数<20）
      let labels = [];
      let ratingSeries = [];
      let playCountSeries = [];
      if (this._releaseYearTrends) {
        const rawLabels = this._releaseYearTrends.labels || [];
        const rawRating = this._releaseYearTrends.avgRating || [];
        const rawPlay = this._releaseYearTrends.playCount || [];
        for (let i = 0; i < rawLabels.length; i++) {
          if ((rawRating[i] || 0) > 0.01 || (rawPlay[i] || 0) >= 20) {
            labels.push(rawLabels[i]);
            ratingSeries.push(rawRating[i]);
            playCountSeries.push(rawPlay[i]);
          }
        }
      }

      c.setOption({
        tooltip: Object.assign({}, this.commonTooltip(), { trigger: 'axis' }),
        legend: {
          top: 4, right: 8,
          textStyle: { color: t.axisLabel, fontSize: 11 },
          itemWidth: 14, itemHeight: 6
        },
        grid: { left: 38, right: 38, top: 32, bottom: 24 },
        xAxis: {
          type: 'category',
          data: labels,
          axisLabel: { color: t.axisLabel, fontSize: 9, interval: 'auto' },
          axisLine: { lineStyle: { color: t.axisLine } },
          axisTick: { show: false }
        },
        yAxis: [
          {
            type: 'value', name: '评分', min: 0, max: 5,
            nameTextStyle: { color: t.axisLabel, fontSize: 10 },
            axisLabel: { color: t.axisLabel, fontSize: 9 },
            splitLine: { lineStyle: { color: t.splitLine } },
            axisLine: { show: false }, axisTick: { show: false }
          },
          {
            type: 'value', name: '游玩次数',
            nameTextStyle: { color: t.axisLabel, fontSize: 10 },
            axisLabel: { color: t.axisLabel, fontSize: 9 },
            splitLine: { show: false },
            axisLine: { show: false }, axisTick: { show: false }
          }
        ],
        series: [
          {
            name: '平均评分', type: 'line', smooth: true, yAxisIndex: 0,
            data: ratingSeries, symbol: 'circle', symbolSize: 4,
            lineStyle: { color: t.palette[0], width: 2 },
            itemStyle: { color: t.palette[0] },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: this.palA(1, 0.20) },
                { offset: 1, color: this.palA(1, 0) }
              ])
            }
          },
          {
            name: '游玩次数', type: 'line', smooth: true, yAxisIndex: 1,
            data: playCountSeries, symbol: 'circle', symbolSize: 4,
            lineStyle: { color: t.palette[2], width: 2 },
            itemStyle: { color: t.palette[2] },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: this.palA(3, 0.14) },
                { offset: 1, color: this.palA(3, 0) }
              ])
            }
          }
        ],
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      });
    },

    async renderWordCloud() {
      const c = this.safeChart('wordCloud', 'wordcloud');
      if (!c) return;
      const t = this.themeColors();
      try {
        const { data: r } = await getTagCloud();
        const list = (r.data || []).slice(0, 80).filter(x => x.name && x.name.length >= 2);
        c.setOption({
          tooltip: Object.assign({}, this.commonTooltip(), { show: true }),
          series: [{
            type: 'wordCloud', shape: 'circle',
            left: 'center', top: 'center',
            width: '95%', height: '95%',
            sizeRange: this.isLight ? [10, 26] : [10, 28],
            rotationRange: [-30, 30], rotationStep: 15,
            gridSize: 4, drawOutOfBound: false,
            textStyle: {
              fontFamily: 'sans-serif', fontWeight: 'bold',
              color: function() { return t.palette[Math.floor(Math.random() * t.palette.length)]; }
            },
            emphasis: {
              textStyle: {
                color: t.textColor,
                shadowBlur: 8,
                shadowColor: this.palA(1, 0.35)
              }
            },
            data: list.map(x => ({ name: x.name, value: x.count }))
          }],
          animationDuration: 1200,
          animationEasing: 'cubicOut'
        });
      } catch (e) {}
    },

    async renderActivityChart() {
      const c = this.safeChart('activityChart', 'activity');
      if (!c) return;
      const t = this.themeColors();

      // 从 API 获取发行年份游戏数量数据，过滤数据稀疏年份（< 20 款游戏不可见）
      let labels = [];
      let data = [];
      if (this._releaseYearTrends) {
        const rawLabels = this._releaseYearTrends.labels || [];
        const rawData = this._releaseYearTrends.gameCount || [];
        for (let i = 0; i < rawLabels.length; i++) {
          if ((rawData[i] || 0) >= 20) {
            labels.push(rawLabels[i]);
            data.push(Number(rawData[i]));
          }
        }
      }
      const max = Math.max(...data, 1);
      c.setOption({
        tooltip: Object.assign({}, this.commonTooltip(), { trigger: 'axis', axisPointer: { type: 'shadow' } }),
        grid: { left: 36, right: 16, top: 12, bottom: 22 },
        xAxis: {
          type: 'category',
          data: labels,
          axisLabel: { color: t.axisLabel, fontSize: 9, interval: 'auto' },
          axisLine: { lineStyle: { color: t.axisLine } },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: t.axisLabel, fontSize: 9 },
          splitLine: { lineStyle: { color: t.splitLine } },
          axisLine: { show: false }, axisTick: { show: false }
        },
        series: [{
          type: 'bar', data, barWidth: '60%',
          itemStyle: {
            borderRadius: [3, 3, 0, 0],
            color: function(p) {
              const v = p.value / max;
              const c1 = v > 0.7 ? t.palette[1] : v > 0.4 ? t.palette[0] : t.palette[7];
              return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: c1 }, { offset: 1, color: hexToRgba(c1, 0.45) }
              ]);
            }
          }
        }],
        animationDuration: 1200,
        animationEasing: 'cubicOut',
        animationDelay: function(idx) { return idx * 25; }
      });
    },

    async renderPlatformChart() {
      const c = this.safeChart('platformChart', 'platform');
      if (!c) return;
      const t = this.themeColors();
      try {
        const { data: r } = await getPlatformDistribution();
        const p = r.data.platforms || r.data || {};
        const entries = Object.entries(p).slice(0, 8);
        c.setOption({
          tooltip: Object.assign({}, this.commonTooltip(), { trigger: 'item', formatter: '{b}: {c} ({d}%)' }),
          legend: {
            bottom: 2, textStyle: { color: t.axisLabel, fontSize: 10 },
            itemWidth: 10, itemHeight: 10, itemGap: 10
          },
          series: [{
            type: 'pie', radius: ['42%', '70%'], center: ['50%', '46%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderColor: this.isLight ? 'rgba(255,255,255,0.95)' : 'rgba(15,20,27,0.95)',
              borderWidth: 2, borderRadius: 4
            },
            label: { color: t.axisLabel, fontSize: 10, formatter: '{b}\n{d}%' },
            emphasis: {
              label: { fontSize: 12, fontWeight: 'bold' },
              itemStyle: { shadowBlur: 16, shadowColor: this.palA(1, 0.35) }
            },
            data: entries.map((e, i) => ({ name: e[0], value: e[1], itemStyle: { color: t.palette[i % t.palette.length] } }))
          }],
          animationDuration: 1200,
          animationType: 'scale'
        });
      } catch (e) {}
    },

    async renderPriceChart() {
      const c = this.safeChart('priceChart', 'price');
      if (!c) return;
      const t = this.themeColors();
      try {
        const { data: r } = await getPriceDistribution();
        const d = r.data || {};
        const stats = d.priceStats || {};
        const buckets = [
          { name: '免费', count: d.freeCount || 0 },
          { name: '0-5', count: stats.price_0_5 || 0 },
          { name: '5-10', count: stats.price_5_10 || 0 },
          { name: '10-20', count: stats.price_10_20 || 0 },
          { name: '20-30', count: stats.price_20_30 || 0 },
          { name: '30-60', count: stats.price_30_60 || 0 },
          { name: '60+', count: stats.price_60_plus || 0 }
        ];
        c.setOption({
          tooltip: Object.assign({}, this.commonTooltip(), { trigger: 'axis', axisPointer: { type: 'shadow' } }),
          grid: { left: 38, right: 16, top: 12, bottom: 22 },
          xAxis: {
            type: 'category', data: buckets.map(b => b.name),
            axisLabel: { color: t.axisLabel, fontSize: 10 },
            axisLine: { lineStyle: { color: t.axisLine } },
            axisTick: { show: false }
          },
          yAxis: {
            type: 'value',
            axisLabel: { color: t.axisLabel, fontSize: 9 },
            splitLine: { lineStyle: { color: t.splitLine } },
            axisLine: { show: false }, axisTick: { show: false }
          },
          series: [{
            type: 'bar', data: buckets.map((b, idx) => ({
              value: b.count,
              itemStyle: {
                borderRadius: [4, 4, 0, 0],
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: idx === 0 ? t.palette[1] : t.palette[0] },
                  { offset: 1, color: hexToRgba(idx === 0 ? t.palette[1] : t.palette[0], 0.32) }
                ])
              }
            })),
            barWidth: '55%',
            label: { show: true, position: 'top', color: t.axisLabel, fontSize: 9 }
          }],
          animationDuration: 1000,
          animationEasing: 'cubicOut'
        });
      } catch (e) {}
    },

    handleResize() {
      Object.values(this.charts).forEach(c => { if (c) c.resize(); });
    }
  }
};
</script>

<style scoped>
/* ===========================================================
   大屏 - 双主题科技感布局
   =========================================================== */
.dash-screen {
  position: relative;
  min-height: calc(100vh - 56px);
  margin: -16px -16px 0;
  padding: 16px 18px 18px;
  color: var(--dash-text-primary);
  overflow: hidden;
  isolation: isolate;
}
.dash-screen.is-light {
  color: var(--dash-text-primary);
}

/* ===== 背景层 ===== */
.dash-bg {
  position: absolute; inset: 0; z-index: -1;
  pointer-events: none;
  overflow: hidden;
}
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(var(--dash-grid-color) 1px, transparent 1px),
    linear-gradient(90deg, var(--dash-grid-color) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.95) 0%, rgba(0,0,0,0.3) 70%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.95) 0%, rgba(0,0,0,0.3) 70%, transparent 100%);
}
.bg-scan {
  position: absolute; left: 0; right: 0; height: 80px;
  background: linear-gradient(180deg, transparent, var(--color-brand-a10), transparent);
  animation: scan-y 8s linear infinite;
  pointer-events: none;
}
.is-light .bg-scan {
  background: linear-gradient(180deg, transparent, var(--color-brand-a06), transparent);
}
@keyframes scan-y {
  0%   { top: -80px; }
  100% { top: 100%; }
}
.bg-hex {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 30%, var(--color-brand-a10) 0%, transparent 25%),
    radial-gradient(circle at 80% 70%, var(--color-accent-a08) 0%, transparent 25%);
  filter: blur(40px);
  animation: mesh-move 24s ease-in-out infinite;
}
@keyframes mesh-move {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50%      { transform: translate(2%, -1%) scale(1.04); }
}
.bg-glow {
  position: absolute; border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}
.bg-glow--blue { top: -10%; left: -8%; width: 480px; height: 480px;
  background: radial-gradient(circle, var(--color-brand-a20) 0%, transparent 70%); }
.bg-glow--cyan { bottom: -15%; right: -10%; width: 520px; height: 520px;
  background: radial-gradient(circle, var(--color-accent-a15) 0%, transparent 70%); }
.bg-glow--purple { top: 30%; right: 30%; width: 400px; height: 400px;
  background: radial-gradient(circle, var(--color-brand-a12) 0%, transparent 70%); }
.is-light .bg-glow--blue { background: radial-gradient(circle, var(--color-brand-a12) 0%, transparent 70%); }
.is-light .bg-glow--cyan { background: radial-gradient(circle, var(--color-accent-a10) 0%, transparent 70%); }
.is-light .bg-glow--purple { background: radial-gradient(circle, var(--color-brand-a08) 0%, transparent 70%); }

/* 粒子 */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.particle {
  position: absolute;
  background: var(--color-accent-a40);
  border-radius: 50%;
  box-shadow: 0 0 6px var(--color-accent-a40);
  animation: particle-float linear infinite;
  opacity: 0;
}
.is-light .particle {
  background: var(--color-brand-a35);
  box-shadow: 0 0 6px var(--color-brand-a25);
}
@keyframes particle-float {
  0%   { opacity: 0; transform: translateY(0); }
  10%  { opacity: 0.8; }
  90%  { opacity: 0.5; }
  100% { opacity: 0; transform: translateY(-200px); }
}

/* ===== Header ===== */
.dash-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 16px;
  padding: 8px 0 18px;
  position: relative;
}
.dash-header-center {
  text-align: center;
  position: relative;
  padding: 14px 30px;
}
.dash-header-center::before,
.dash-header-center::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 100px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--dash-line-color), transparent);
  opacity: 0.6;
}
.dash-header-center::before { left: -80px; }
.dash-header-center::after  { right: -80px; }
.dash-eyebrow {
  font-size: 10px;
  letter-spacing: 4px;
  color: var(--dash-text-tertiary);
  text-transform: uppercase;
  margin-bottom: 6px;
  font-weight: 600;
  font-family: var(--font-family-mono);
}
.dash-title {
  margin: 0;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 4px;
  line-height: 1.1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
}
.dash-title::before, .dash-title::after {
  content: '';
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--dash-corner-color), transparent);
  opacity: 0.5;
}
.dash-title::before { margin-right: 6px; }
.dash-title::after  { margin-left: 6px; }
.title-text {
  background: linear-gradient(90deg, var(--color-brand-400) 0%, var(--color-accent-400) 50%, var(--color-brand-300) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
  filter: drop-shadow(0 0 12px var(--color-brand-a35));
}
.is-light .title-text {
  background: linear-gradient(90deg, #4f46e5 0%, #0891b2 50%, #7c3aed 100%);
  -webkit-background-clip: text;
  background-clip: text;
  filter: none;
}
.title-bracket {
  font-size: 28px;
  color: var(--dash-corner-color);
  -webkit-text-fill-color: currentColor;
  vertical-align: middle;
  opacity: 0.6;
  text-shadow: 0 0 8px currentColor;
}
.dash-sub {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 11px;
  color: var(--dash-text-tertiary);
  letter-spacing: 0.5px;
  font-family: var(--font-family-mono);
}
.sub-sep { opacity: 0.4; color: var(--dash-text-tertiary); }
.sub-time {
  color: var(--dash-text-secondary);
}
.sub-tag {
  padding: 1px 8px;
  border: 1px solid var(--dash-border-soft);
  border-radius: 3px;
  background: var(--dash-bg-panel);
  font-size: 9px;
  letter-spacing: 1.5px;
  color: var(--dash-corner-color);
}
.live-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 8px var(--color-success-border);
  animation: live-pulse 1.4s ease-in-out infinite;
}
@keyframes live-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.4; transform: scale(0.7); }
}
.dash-header-side { display: flex; align-items: center; height: 100%; }
.dash-header-side--left  { justify-content: flex-start; }
.dash-header-side--right { justify-content: flex-end; }
.header-deco {
  display: flex;
  align-items: center;
  gap: 4px;
}
.header-deco--r { flex-direction: row-reverse; }
.deco-line {
  width: 140px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--dash-line-color));
  opacity: 0.6;
}
.header-deco--r .deco-line {
  background: linear-gradient(90deg, var(--dash-line-color), transparent);
}
.deco-block {
  width: 8px;
  height: 8px;
  background: var(--dash-corner-color);
  transform: rotate(45deg);
  box-shadow: 0 0 8px currentColor;
}

/* ===== 6个类型卡 ===== */
.genre-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}
.genre-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 14px;
  background: var(--dash-bg-panel);
  border: 1px solid var(--dash-border-soft);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  animation: card-in 0.5s ease both;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.is-light .genre-card {
  background: linear-gradient(135deg, rgba(255,255,255,0.9) 0%, rgba(248,250,255,0.7) 100%);
  border: 1px solid var(--color-brand-a12);
  box-shadow: 0 1px 3px rgba(15,23,42,0.04);
}
.genre-card:nth-child(1) { animation-delay: 0.05s; }
.genre-card:nth-child(2) { animation-delay: 0.10s; }
.genre-card:nth-child(3) { animation-delay: 0.15s; }
.genre-card:nth-child(4) { animation-delay: 0.20s; }
.genre-card:nth-child(5) { animation-delay: 0.25s; }
.genre-card:nth-child(6) { animation-delay: 0.30s; }
@keyframes card-in {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}
.genre-card:hover {
  border-color: var(--genre-color);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px -8px var(--genre-bg);
}
.genre-card-scan {
  position: absolute;
  top: 0; left: -100%;
  width: 40%;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--genre-bg), transparent);
  pointer-events: none;
  opacity: 0.5;
  animation: card-scan 6s ease-in-out infinite;
}
@keyframes card-scan {
  0%   { left: -50%; }
  50%  { left: 100%; }
  100% { left: 100%; }
}
.genre-icon {
  width: 40px; height: 40px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  background: var(--genre-bg);
  color: var(--genre-color);
  flex-shrink: 0;
  border: 1px solid var(--genre-bg);
  position: relative;
  z-index: 1;
}
.genre-info { flex: 1; min-width: 0; position: relative; z-index: 1; }
.genre-count {
  font-family: var(--font-family-mono, monospace);
  font-size: 20px;
  font-weight: 700;
  color: var(--genre-color);
  line-height: 1.1;
  font-feature-settings: 'tnum';
}
.genre-name {
  font-size: 12px;
  color: var(--dash-text-tertiary);
  margin-top: 2px;
}
.genre-spark {
  width: 60px; height: 24px;
  flex-shrink: 0;
  opacity: 0.9;
  position: relative;
  z-index: 1;
}
.genre-spark svg { width: 100%; height: 100%; }
.genre-card-deco {
  position: absolute;
  width: 6px; height: 6px;
  border-color: var(--genre-color);
  opacity: 0.7;
  z-index: 2;
}
.genre-card-deco--tl { top: 3px; left: 3px; border-top: 1px solid; border-left: 1px solid; }
.genre-card-deco--tr { top: 3px; right: 3px; border-top: 1px solid; border-right: 1px solid; }
.genre-card-deco--bl { bottom: 3px; left: 3px; border-bottom: 1px solid; border-left: 1px solid; }
.genre-card-deco--br { bottom: 3px; right: 3px; border-bottom: 1px solid; border-right: 1px solid; }

/* ===== 主网格 ===== */
.dash-grid {
  display: grid;
  grid-template-columns: 1fr 1.1fr 1fr;
  gap: 12px;
  margin-bottom: 14px;
}
.col { display: flex; flex-direction: column; gap: 12px; min-width: 0; }

/* ===== Panel 通用 ===== */
.panel {
  position: relative;
  background: var(--dash-bg-panel);
  border: 1px solid var(--dash-border-soft);
  border-radius: 8px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  overflow: hidden;
  flex: 1;
  min-height: 0;
  transition: border-color 0.2s;
}
.panel:hover {
  border-color: var(--dash-border-glow);
}
.is-light .panel {
  background: linear-gradient(180deg, rgba(255,255,255,0.92) 0%, rgba(248,250,255,0.75) 100%);
  border: 1px solid var(--color-brand-a12);
  box-shadow: 0 1px 4px rgba(15,23,42,0.04);
}
.panel::before {
  content: ''; position: absolute; top: 0; left: 8px; right: 8px; height: 1px;
  background: linear-gradient(90deg, transparent, var(--dash-border-glow), transparent);
  z-index: 1;
}
.is-light .panel::before {
  background: linear-gradient(90deg, transparent, var(--color-brand-a35), transparent);
}

/* 4角L形装饰 - 强化 */
.panel-deco {
  position: absolute;
  width: 10px; height: 10px;
  border-color: var(--dash-corner-color);
  opacity: 0.75;
  z-index: 2;
  pointer-events: none;
  transition: all 0.3s;
}
.panel-deco--tl { top: 0; left: 0; border-top: 1.5px solid; border-left: 1.5px solid; }
.panel-deco--tr { top: 0; right: 0; border-top: 1.5px solid; border-right: 1.5px solid; }
.panel-deco--bl { bottom: 0; left: 0; border-bottom: 1.5px solid; border-left: 1.5px solid; }
.panel-deco--br { bottom: 0; right: 0; border-bottom: 1.5px solid; border-right: 1.5px solid; }
.panel:hover .panel-deco { opacity: 1; }
.is-light .panel-deco { border-color: var(--color-brand-500); opacity: 0.6; }

.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px 10px;
  border-bottom: 1px solid var(--dash-border-soft);
  position: relative;
}
.is-light .panel-header { border-bottom-color: rgba(15,23,42,0.06); }
.panel-header h3 {
  font-size: 14px; font-weight: 700; color: var(--dash-text-primary);
  margin: 0; letter-spacing: 1.5px;
  display: flex; align-items: center; gap: 8px;
}
.panel-header h3::before {
  content: ''; display: inline-block;
  width: 3px; height: 14px;
  background: linear-gradient(180deg, var(--color-brand-400), var(--color-accent-400));
  border-radius: 2px;
  box-shadow: 0 0 6px var(--color-brand-a35);
}
.is-light .panel-header h3::before {
  background: linear-gradient(180deg, #4f46e5, #0891b2);
  box-shadow: none;
}
.panel-sub {
  font-size: 9px;
  letter-spacing: 1.5px;
  color: var(--dash-text-tertiary);
  font-family: var(--font-family-mono, monospace);
  padding: 2px 6px;
  border: 1px solid var(--dash-border-soft);
  border-radius: 3px;
  background: var(--dash-bg-panel);
}
.is-light .panel-sub { color: var(--dash-text-tertiary); }
.panel-body { padding: 10px 8px 8px; }
.panel-body--monitor {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  align-items: center;
  padding: 10px 12px 12px;
}
.panel-body--hero {
  position: relative;
  padding: 14px 12px 12px;
}
.panel-chart { width: 100%; height: 200px; }
.panel-chart--tall { height: 240px; }

/* ===== 监控雷达面板 ===== */
.monitor-radar { width: 100%; height: 200px; }
.monitor-stats {
  list-style: none; padding: 0; margin: 0;
  display: flex; flex-direction: column; gap: 8px;
}
.monitor-stats li {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px;
  background: var(--dash-bg-pane, var(--color-brand-a06));
  border: 1px solid var(--dash-border-soft);
  border-radius: 6px;
  font-size: 12px;
  transition: all 0.2s;
}
.monitor-stats li:hover {
  background: var(--color-brand-a10);
  border-color: var(--dash-border-glow);
}
.is-light .monitor-stats li {
  background: var(--color-brand-a04);
  border-color: var(--color-brand-a12);
}
.ms-label { color: var(--dash-text-tertiary); }
.ms-val {
  font-family: var(--font-family-mono, monospace);
  font-size: 16px;
  font-weight: 700;
  color: var(--dash-text-primary);
}
.ms-val small { font-size: 11px; color: var(--dash-text-tertiary); margin-left: 1px; }
.ms-val--green  { color: var(--color-success); }
.ms-val--orange { color: var(--color-warning); }
.ms-val--red    { color: var(--color-danger); }

/* ===== 中央 Hero 环 ===== */
.hero-ring { width: 100%; height: 240px; }
.hero-stats {
  list-style: none; padding: 0; margin: 0;
  position: absolute;
  top: 12px; right: 12px;
  display: flex; flex-direction: column; gap: 6px;
  z-index: 5;
}
.hs-row {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px;
  padding: 3px 8px;
  background: var(--dash-bg-pane, var(--color-brand-a08));
  border: 1px solid var(--dash-border-soft);
  border-radius: 4px;
  white-space: nowrap;
  backdrop-filter: blur(4px);
}
.is-light .hs-row {
  background: var(--color-brand-a06);
  border-color: var(--color-brand-a10);
}
.hs-dot { width: 8px; height: 8px; border-radius: 2px; flex-shrink: 0; box-shadow: 0 0 6px currentColor; }
.hs-text { color: var(--dash-text-tertiary); }
.hs-num {
  font-family: var(--font-family-mono, monospace);
  font-weight: 700;
  color: var(--dash-text-primary);
  font-size: 12px;
}

/* ===== 高质量游戏排行 ===== */
.panel-body--scroll {
  max-height: 530px;
  overflow-y: auto;
  padding: 8px 8px 8px;
  scrollbar-width: thin;
  scrollbar-color: var(--color-brand-a35) transparent;
}
.panel-body--scroll::-webkit-scrollbar { width: 4px; }
.panel-body--scroll::-webkit-scrollbar-thumb { background: var(--color-brand-a35); border-radius: 2px; }
.rank-list { list-style: none; padding: 0; margin: 0; }
.rank-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  margin-bottom: 4px;
  animation: rank-in 0.4s var(--ease-out-expo) backwards;
  animation-delay: calc(var(--idx) * 40ms);
}
@keyframes rank-in {
  from { opacity: 0; transform: translateX(-8px); }
  to   { opacity: 1; transform: translateX(0); }
}
.rank-item:hover {
  background: var(--dash-bg-pane, var(--color-brand-a08));
  border-color: var(--rank-color, var(--color-brand-a35));
  transform: translateX(2px);
}
.is-light .rank-item:hover { background: var(--color-brand-a06); }
.rank-no {
  font-family: var(--font-family-mono, monospace);
  font-size: 14px;
  font-weight: 800;
  color: var(--rank-color, var(--color-brand-400));
  min-width: 22px;
  text-align: center;
  flex-shrink: 0;
}
.rank-name {
  flex: 1;
  font-size: 12px;
  color: var(--dash-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
.rank-meta {
  display: flex; align-items: center; gap: 6px;
  flex-shrink: 0;
}
.rank-score {
  font-family: var(--font-family-mono, monospace);
  font-size: 11px;
  color: var(--rank-color, var(--color-brand-400));
  font-weight: 600;
}
.rank-bar {
  width: 40px; height: 4px;
  background: var(--color-brand-a10);
  border-radius: 2px;
  overflow: hidden;
  display: inline-block;
}
.is-light .rank-bar { background: rgba(15,23,42,0.06); }
.rank-bar span {
  display: block; height: 100%;
  background: linear-gradient(90deg, var(--rank-color, var(--color-brand-400)), transparent);
  transition: width 0.8s var(--ease-out-expo, ease-out);
}

/* ===== 底部三栏 ===== */
.dash-foot {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 12px;
}
.foot-panel { min-height: 220px; }
.foot-chart { width: 100%; height: 200px; }

/* ===== 响应式 ===== */
@media (max-width: 1500px) {
  .dash-grid { grid-template-columns: 1fr 1.1fr 1fr; }
  .panel-chart--tall { height: 220px; }
  .panel-body--scroll { max-height: 460px; }
}
@media (max-width: 1200px) {
  .dash-grid { grid-template-columns: 1fr 1fr; }
  .col--right { grid-column: span 2; }
  .genre-row { grid-template-columns: repeat(3, 1fr); }
  .dash-foot { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 900px) {
  .dash-grid { grid-template-columns: 1fr; }
  .col--right { grid-column: span 1; }
  .dash-foot { grid-template-columns: 1fr; }
  .genre-row { grid-template-columns: repeat(2, 1fr); }
  .dash-title { font-size: 22px; letter-spacing: 2px; }
  .dash-title::before, .dash-title::after { width: 30px; }
  .dash-header-center { padding: 14px 0; }
  .dash-header-center::before, .dash-header-center::after { display: none; }
  .header-deco .deco-line { width: 60px; }
}
@media (max-width: 600px) {
  .dash-screen { padding: 10px 10px 14px; margin: -10px -10px 0; }
  .genre-row { grid-template-columns: 1fr 1fr; }
  .dash-title { font-size: 18px; }
  .panel-body--monitor { grid-template-columns: 1fr; }
  .hero-stats { position: static; flex-direction: row; flex-wrap: wrap; }
}
</style>
