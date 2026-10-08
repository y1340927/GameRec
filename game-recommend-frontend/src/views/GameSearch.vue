<template>
  <div class="refactor-table">
    <!-- 简洁搜索 Hero 区（去除动态效果） -->
    <div class="search-hero">
      <div class="search-hero-inner">
        <div class="search-hero-text">
          <div class="hero-eyebrow">
            <span class="eyebrow-dot"></span>
            GAME SEARCH
          </div>
          <h1 class="hero-title">探索海量游戏</h1>
          <p class="hero-desc">在 26,000+ 款游戏中搜索你感兴趣的内容</p>
        </div>
        <div class="search-bar-wrap">
          <div class="search-bar" :class="{ 'is-focused': searchFocused, 'has-value': keyword }">
            <span class="search-bar-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <circle cx="9" cy="9" r="6.25" stroke="currentColor" stroke-width="1.7"/>
                <path d="M13.5 13.5L17.5 17.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
              </svg>
            </span>
            <input
              v-model="keyword"
              @keyup.enter="loadGames(1)"
              @focus="searchFocused = true"
              @blur="searchFocused = false"
              placeholder="输入游戏名、类型或标签..."
              class="search-bar-input"
              ref="searchInput"
            />
            <span class="search-bar-kbd" v-if="!keyword">Ctrl + K</span>
            <button v-if="keyword" class="search-bar-clear" @click="clearSearch" title="清除">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M3.5 3.5l7 7M10.5 3.5l-7 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
            <button class="search-bar-go" @click="loadGames(1)" title="搜索">
              <span>搜索</span>
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M3 7h8M8 4l3 3-3 3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
          <!-- 热门标签快速筛选 -->
          <div class="quick-tags">
            <span class="quick-tag-label">热门筛选：</span>
            <button
              v-for="t in quickTags"
              :key="t.label"
              class="quick-tag"
              :class="{ 'is-active': tags === t.value }"
              :style="{ '--tag-color': t.color }"
              @click="toggleTag(t)"
            >
              <span class="quick-tag-dot" :style="{ background: t.color }"></span>
              {{ t.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="content-row">
      <!-- 左侧：近期游戏列表 -->
      <div class="content-card table-card">
        <div class="card-head table-card-head">
          <div class="card-head-left">
            <h2 class="card-title">近期游戏报表</h2>
            <span class="card-subtitle">Recent Games Report</span>
          </div>
          <div class="table-controls">
            <div class="entries-control">
              <span class="ctrl-label">每页</span>
              <select v-model.number="pageSize" @change="loadGames(1)">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
              <span class="ctrl-label">条</span>
            </div>
          </div>
        </div>

        <div class="data-table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th class="th-num">编号</th>
                <th>游戏名</th>
                <th class="th-cover">图标</th>
                <th>上市时间</th>
                <th>折扣价</th>
                <th>类型</th>
                <th>近期评价</th>
                <th>整体评价</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(g, i) in games" :key="g.gameId" @click="goGame(g.gameId)" class="game-row-clickable">
                <td class="th-num">{{ (page - 1) * pageSize + i + 1 }}</td>
                <td class="td-name">
                  <div class="name-cell">
                    <span class="name-text" :title="g.gameNameCn || g.gameName">{{ g.gameNameCn || g.gameName }}</span>
                  </div>
                </td>
                <td>
                  <div class="cover-mini cover-mini--img">
                    <img
                      :src="`/images/games/${g.gameId}.jpg`"
                      :alt="g.gameNameCn || g.gameName"
                      @error="hideCoverMini($event, g)"
                      class="cover-mini-img"
                    />
                    <span class="cover-mini-fallback" :style="{ background: coverStyle(g) }">{{ getInitial(g) }}</span>
                  </div>
                </td>
                <td class="td-date">{{ formatDate(g.releaseDate) }}</td>
                <td>
                  <span v-if="g.priceInitial && g.priceInitial > g.price" class="discount-badge">
                    {{ ((g.price / g.priceInitial) * 10).toFixed(0) }}折
                  </span>
                  <span v-else class="no-discount">—</span>
                </td>
                <td class="td-genres">
                  <div class="genres-tags">
                    <span v-for="t in (g.genresCn || g.genres || '').split(/[;；,]/).filter(Boolean).slice(0,3)" :key="t" class="g-tag">{{ t.trim() }}</span>
                  </div>
                </td>
                <td>
                  <span class="review-pill" :class="reviewClass(g.reviewScoreDescCn)">{{ g.reviewScoreDescCn || '—' }}</span>
                </td>
                <td>
                  <span class="review-pill" :class="reviewClass(g.reviewScoreDescCn)">{{ g.reviewScoreDescCn || '—' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar">
          <span class="page-info">共 <b>{{ total }}</b> 条</span>
          <div class="pager">
            <button class="pager-btn pager-btn--nav" :disabled="page === 1" @click="loadGames(page - 1)">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M7.5 3L4.5 6l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button v-for="p in pageList" :key="p" class="pager-btn" :class="{ 'pager-btn--active': p === page }" @click="typeof p === 'number' && loadGames(p)">{{ p }}</button>
            <button class="pager-btn pager-btn--nav" :disabled="page >= totalPages" @click="loadGames(page + 1)">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M4.5 3L7.5 6l-3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧：图表区 -->
      <div class="right-col">
        <div class="content-card chart-card chart-card--user">
          <div class="card-head">
            <div class="card-head-left">
              <h2 class="card-title">用户个类占比</h2>
              <span class="card-subtitle">User Activity Distribution</span>
            </div>
            <div class="pager-mini">
              <button class="pager-mini-btn" :disabled="userPage === 1" @click="loadUserChart(userPage - 1)">
                <svg width="10" height="10" viewBox="0 0 12 12" fill="none"><path d="M7.5 3L4.5 6l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </button>
              <span class="pager-mini-text">{{ userPage }}<span class="pager-mini-divider">/</span>{{ userTotalPages }}</span>
              <button class="pager-mini-btn" :disabled="userPage >= userTotalPages" @click="loadUserChart(userPage + 1)">
                <svg width="10" height="10" viewBox="0 0 12 12" fill="none"><path d="M4.5 3L7.5 6l-3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </button>
            </div>
          </div>
          <div ref="userChart" class="user-chart" aria-label="用户类型占比环形图"></div>
        </div>

        <div class="content-card chart-card chart-card--genre">
          <div class="card-head">
            <div class="card-head-left">
              <h2 class="card-title">游戏类型占比</h2>
              <span class="card-subtitle">Genre Distribution Top 10</span>
            </div>
            <div class="chart-tools">
              <button class="tool-btn" @click="refreshGenreChart" title="刷新">
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                  <path d="M2 7a5 5 0 0110-2M12 7a5 5 0 01-10 2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                  <path d="M12 2v3h-3M2 12V9h3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                </svg>
              </button>
              <button class="tool-btn" @click="downloadGenreChart" title="下载">
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                  <path d="M7 1v8M4 6l3 3 3-3M2 11h10" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </div>
          </div>
          <div ref="genreChart" class="genre-chart" aria-label="游戏类型占比饼图"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import { listGames, getTopActiveUsers } from '@/api/index';
import { chartPalette, chartTheme } from '@/utils/echarts-theme';

export default {
  name: 'RefactorTable',
  data() {
    return {
      page: 1,
      pageSize: 10,
      total: 0,
      totalPages: 1,
      keyword: '',
      tags: '',
      games: [],
      userChart: null,
      genreChart: null,
      userPage: 1,
      userTotalPages: 1,
      userPageSize: 12,
      searchFocused: false,
      isLight: false,
      quickTags: [
        { label: '动作', value: '动作' },
        { label: '角色扮演', value: '角色扮演' },
        { label: '策略', value: '策略' },
        { label: '独立', value: '独立' },
        { label: '冒险', value: '冒险' },
        { label: '休闲', value: '休闲' }
      ].map((t, i) => ({ ...t, color: chartPalette()[i % chartPalette().length] }))
    };
  },
  computed: {
    pageList() {
      const out = [];
      const max = this.totalPages;
      const cur = this.page;
      // 智能分页：当前页前后各2个
      const start = Math.max(1, cur - 2);
      const end = Math.min(max, cur + 2);
      if (start > 1) {
        out.push(1);
        if (start > 2) out.push('…');
      }
      for (let i = start; i <= end; i++) out.push(i);
      if (end < max) {
        if (end < max - 1) out.push('…');
        out.push(max);
      }
      return out;
    }
  },
  async mounted() {
    this.syncTheme();
    // Ctrl+K 快捷键聚焦搜索
    document.addEventListener('keydown', this.handleKeydown);
    // 读取路由参数：标签筛选
    if (this.$route.query.tag) {
      this.tags = this.$route.query.tag;
    }
    await this.loadGames(1);
    this.$nextTick(() => {
      this.initUserChart();
      this.initGenreChart();
    });
    window.addEventListener('resize', this.handleResize);
    this._themeObserver = new MutationObserver(() => {
      const newLight = document.documentElement.getAttribute('data-theme') === 'light';
      if (this.isLight !== newLight) {
        this.isLight = newLight;
        this.$nextTick(() => {
          this.initUserChart();
          this.initGenreChart();
        });
      }
    });
    this._themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.handleKeydown);
    window.removeEventListener('resize', this.handleResize);
    if (this.userChart) this.userChart.dispose();
    if (this.genreChart) this.genreChart.dispose();
    if (this._themeObserver) this._themeObserver.disconnect();
  },
  watch: {
    '$route.query.tag': {
      handler(newTag) {
        this.tags = newTag || '';
        this.loadGames(1);
      }
    }
  },
  methods: {
    syncTheme() {
      this.isLight = document.documentElement.getAttribute('data-theme') === 'light';
    },
    handleKeydown(e) {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        this.$refs.searchInput && this.$refs.searchInput.focus();
      }
    },
    clearSearch() {
      this.keyword = '';
      this.loadGames(1);
    },
    toggleTag(t) {
      if (this.tags === t.value) {
        this.tags = '';
      } else {
        this.tags = t.value;
      }
      this.loadGames(1);
    },
    async loadGames(p) {
      this.page = p;
      try {
        const { data: r } = await listGames(p, this.pageSize, this.keyword, 'releaseDate', this.tags);
        if (r.code === 200) {
          this.games = r.data.records || [];
          this.total = r.data.total || 0;
          this.totalPages = Math.max(1, Math.ceil(this.total / this.pageSize));
        }
      } catch (e) { /* fallback */ }
    },
    themeChartColors() {
      const t = chartTheme();
      return {
        text: t.textColor,
        textSecondary: t.axisColor,
        textTertiary: t.subTextColor,
        splitLine: t.gridColor,
        axisLine: t.axisLine,
        tooltipBg: t.tooltip.backgroundColor,
        tooltipFg: t.tooltip.textStyle.color,
        tooltipBorder: t.tooltip.borderColor,
        paneBg: 'rgba(0,0,0,0)'
      };
    },
    chartPalette() {
      return chartPalette();
    },
    async initUserChart() {
      if (!this.$refs.userChart) return;
      this.userChart = echarts.init(this.$refs.userChart);
      const palette = this.chartPalette();
      const tc = this.themeChartColors();
      try {
        // 从后端获取活跃用户数据，按活跃度分组展示
        const { data: r } = await getTopActiveUsers(12);
        if (r.code === 200 && Array.isArray(r.data) && r.data.length > 0) {
          const users = r.data;
          const data = users.map((u, i) => ({
            name: '用户#' + u.user_id,
            value: u.play_count || u.total_hours || 1
          }));
          this.userChart.setOption({
            color: palette,
            tooltip: {
              trigger: 'item',
              formatter: '{b}<br/>活跃度: <b>{c}</b> ({d}%)',
              backgroundColor: tc.tooltipBg,
              borderColor: tc.tooltipBorder,
              borderWidth: 1,
              textStyle: { color: tc.tooltipFg, fontSize: 12 },
              extraCssText: 'box-shadow: 0 8px 24px rgba(0,0,0,0.28); border-radius: 8px; padding: 10px 12px;'
            },
            legend: {
              type: 'scroll',
              orient: 'vertical',
              right: 4,
              top: 'middle',
              itemWidth: 8,
              itemHeight: 8,
              itemGap: 8,
              textStyle: { fontSize: 10, color: tc.textSecondary }
            },
            series: [{
              type: 'pie',
              radius: ['44%', '70%'],
              center: ['36%', '50%'],
              avoidLabelOverlap: true,
              minShowLabelAngle: 8,
              itemStyle: {
                borderRadius: 4,
                borderColor: tc.tooltipBg,
                borderWidth: 2
              },
              label: {
                show: true,
                position: 'outside',
                formatter: '{d}%',
                fontSize: 10,
                color: tc.textSecondary
              },
              labelLine: { show: true, length: 6, length2: 4, lineStyle: { color: tc.textTertiary } },
              data
            }]
          });
          return;
        }
      } catch (e) { /* API不可用 */ }
      // 无数据时显示空状态
      this.userChart.setOption({
        title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: tc.textTertiary, fontSize: 13 } }
      });
    },
    initGenreChart() {
      if (!this.$refs.genreChart) return;
      this.genreChart = echarts.init(this.$refs.genreChart);
      // 实际：从已有数据聚合
      const buckets = {};
      this.games.forEach(g => {
        const tags = (g.genresCn || g.genres || '').split(/[;；,]/);
        tags.forEach(t => {
          const k = t.trim();
          if (k) buckets[k] = (buckets[k] || 0) + 1;
        });
      });
      const top = Object.entries(buckets).sort((a, b) => b[1] - a[1]).slice(0, 10);
      const palette = this.chartPalette();
      const tc = this.themeChartColors();
      const data = top.map(([n, v], i) => ({ name: n, value: v, itemStyle: { color: palette[i % palette.length] } }));

      this.genreChart.setOption({
        color: palette,
        tooltip: {
          trigger: 'item',
          formatter: '{b}: <b>{c}</b> ({d}%)',
          backgroundColor: tc.tooltipBg,
          borderColor: tc.tooltipBorder,
          borderWidth: 1,
          textStyle: { color: tc.tooltipFg, fontSize: 12 },
          extraCssText: 'box-shadow: 0 8px 24px rgba(0,0,0,0.28); border-radius: 8px; padding: 10px 12px;'
        },
        legend: {
          type: 'scroll', orient: 'vertical', right: 4, top: 'middle',
          itemWidth: 8, itemHeight: 8, itemGap: 8,
          textStyle: { fontSize: 10, color: tc.textSecondary }
        },
        series: [{
          type: 'pie',
          radius: ['44%', '72%'],
          center: ['36%', '50%'],
          avoidLabelOverlap: true,
          minShowLabelAngle: 10,
          itemStyle: {
            borderRadius: 4,
            borderColor: tc.tooltipBg,
            borderWidth: 2
          },
          label: { show: false },
          labelLine: { show: false },
          data
        }]
      });
    },
    refreshGenreChart() {
      if (this.genreChart) this.initGenreChart();
    },
    downloadGenreChart() {
      if (!this.genreChart) return;
      const url = this.genreChart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: chartTheme().tooltip.backgroundColor });
      const a = document.createElement('a');
      a.href = url;
      a.download = '游戏类型占比.png';
      a.click();
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
    reviewClass(s) {
      if (!s) return 'r-null';
      if (s.includes('好评') || s.includes('特别') || s.includes('Very') || s.includes('Positive')) return 'r-good';
      if (s.includes('差评') || s.includes('Negative')) return 'r-bad';
      return 'r-mid';
    },
    formatDate(s) {
      if (!s) return '-';
      const d = new Date(s);
      if (isNaN(d)) return s;
      return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
    },
    hideCoverMini(event, g) {
      const img = event.target;
      const fallback = img.parentElement.querySelector('.cover-mini-fallback');
      if (fallback) {
        img.style.display = 'none';
        fallback.style.display = 'flex';
      }
    },
    handleResize() {
      if (this.userChart) this.userChart.resize();
      if (this.genreChart) this.genreChart.resize();
    },
    goGame(id) { if (id) this.$router.push(`/game/${id}`); }
  }
};
</script>

<style scoped>
.refactor-table { padding: 0 0 var(--space-6); }

/* ===== Search Hero ===== */
.search-hero {
  position: relative;
  margin: 0;
  padding: var(--space-5) var(--space-8) var(--space-4);
  background: var(--banner-bg-gradient-2, var(--banner-bg-gradient));
  color: var(--banner-text-color);
  overflow: hidden;
  isolation: isolate;
}
.search-hero-glow { display: none; }
.search-hero-inner {
  position: relative;
  z-index: 1;
  max-width: var(--content-max-width);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-8);
  flex-wrap: wrap;
}
.search-hero-text { flex-shrink: 0; }
.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  letter-spacing: 3px;
  color: var(--banner-eyebrow-color);
  font-weight: 600;
  margin-bottom: 8px;
  font-family: var(--font-family-mono);
}
.eyebrow-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--banner-shape-1);
  box-shadow: 0 0 6px currentColor;
}
.hero-title {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 800;
  color: var(--banner-text-color);
  letter-spacing: -0.5px;
  line-height: 1.2;
  background: linear-gradient(90deg, var(--banner-text-color) 0%, var(--banner-shape-1) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.hero-desc {
  margin: 0;
  font-size: 13px;
  color: var(--banner-eyebrow-color);
  font-weight: 400;
}
.search-bar-wrap {
  flex: 1;
  min-width: 380px;
  max-width: 720px;
}
.search-bar {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 5px 5px 16px;
  background: var(--banner-card-bg, rgba(255,255,255,0.10));
  border: 1px solid var(--banner-card-border, rgba(255,255,255,0.18));
  border-radius: 12px;
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 10px rgba(0,0,0,0.08);
  transition: all 0.25s var(--ease-out-expo);
}
.search-bar:hover {
  border-color: var(--banner-shape-1);
  box-shadow: 0 4px 14px rgba(0,0,0,0.12);
}
.search-bar.is-focused {
  border-color: var(--banner-shape-1);
  background: var(--banner-card-bg);
  box-shadow: 0 0 0 3px var(--color-brand-a20), 0 4px 14px rgba(0,0,0,0.15);
}
.search-bar-icon {
  display: flex;
  align-items: center;
  color: var(--banner-shape-1);
  flex-shrink: 0;
}
.search-bar-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 9px 4px;
  font-size: 14px;
  color: var(--banner-text-color);
  outline: none;
  min-width: 0;
  font-family: var(--font-family-body);
  font-weight: 500;
}
.search-bar-input::placeholder {
  color: var(--banner-eyebrow-color);
  font-weight: 400;
}
.search-bar-kbd {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
  background: var(--banner-tag-bg);
  border: 1px solid var(--banner-card-border, rgba(255,255,255,0.15));
  border-radius: 6px;
  font-family: var(--font-family-mono);
  font-size: 11px;
  color: var(--banner-eyebrow-color);
  letter-spacing: 0.5px;
  flex-shrink: 0;
}
.search-bar-clear {
  width: 24px; height: 24px;
  display: flex; align-items: center; justify-content: center;
  border: none;
  border-radius: 6px;
  background: var(--banner-tag-bg);
  color: var(--banner-eyebrow-color);
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s;
}
.search-bar-clear:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}
.search-bar-go {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--banner-shape-1) 0%, var(--banner-shape-3) 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--ease-out-expo);
  flex-shrink: 0;
  box-shadow: 0 4px 12px var(--color-brand-a35);
}
.search-bar-go:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px var(--color-brand-a50);
}
.search-bar-go:active { transform: scale(0.96); }

.quick-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.quick-tag-label {
  font-size: 12px;
  color: var(--banner-eyebrow-color);
  font-weight: 500;
}
.quick-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border: 1px solid var(--banner-card-border, rgba(255,255,255,0.15));
  border-radius: 999px;
  background: var(--banner-tag-bg);
  color: var(--banner-tag-color, var(--banner-text-color));
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  backdrop-filter: blur(4px);
}
.quick-tag:hover {
  background: var(--banner-card-bg, rgba(255,255,255,0.10));
  border-color: var(--tag-color);
  transform: translateY(-1px);
}
.quick-tag.is-active {
  background: var(--tag-color);
  border-color: var(--tag-color);
  color: #fff;
  box-shadow: 0 2px 8px color-mix(in srgb, var(--tag-color) 40%, transparent);
}
.quick-tag.is-active .quick-tag-dot {
  background: rgba(255,255,255,0.8) !important;
}
.quick-tag-dot {
  width: 5px; height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 4px currentColor;
}

.content-row {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: var(--space-5);
  padding: var(--space-5) var(--space-8) 0;
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
.right-col { display: flex; flex-direction: column; gap: var(--space-5); }

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
.table-card-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
.table-controls {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--space-3);
  font-size: 12px;
  color: var(--color-text-tertiary);
  flex-wrap: wrap;
}
.ctrl-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}
.entries-control {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border-muted);
  border-radius: 6px;
  padding: 4px 10px;
  transition: all var(--duration-fast);
}
.entries-control:hover { border-color: var(--color-border-default); }
.entries-control:focus-within { border-color: var(--color-brand-500); box-shadow: 0 0 0 2px var(--color-brand-a15); }
.entries-control select {
  border: none;
  background: transparent;
  padding: 2px 0;
  font-size: 12px;
  color: var(--color-text-primary);
  outline: none;
  cursor: pointer;
  min-width: 36px;
  font-weight: 600;
}

/* Data table */
.data-table-wrap { overflow-x: auto; }
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.data-table thead th {
  background: var(--color-bg-subtle);
  color: var(--color-text-primary);
  font-weight: 600;
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border-default);
  white-space: nowrap;
  font-size: 12px;
  letter-spacing: 0.3px;
}
.data-table tbody td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border-muted);
  color: var(--color-text-primary);
  vertical-align: middle;
}
.data-table tbody tr { transition: background 0.15s; }
.data-table tbody tr:hover { background: var(--color-bg-overlay); }
.game-row-clickable { cursor: pointer; }
.th-num { color: var(--color-text-tertiary); font-weight: 500; font-family: var(--font-family-mono); }
.th-cover { width: 80px; }
.td-name { max-width: 200px; }
.name-cell { display: flex; align-items: center; gap: 8px; }
.name-text {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}
.td-date { color: var(--color-text-tertiary); white-space: nowrap; font-family: var(--font-family-mono); font-size: 12px; }
.cover-mini {
  width: 60px;
  height: 36px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
  overflow: hidden;
  position: relative;
}
.cover-mini--img {
  background: transparent;
}
.cover-mini-img {
  width: 100%; height: 100%; object-fit: cover;
  display: block; position: absolute; top: 0; left: 0;
}
.cover-mini-fallback {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
}
.discount-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  background: var(--color-success-bg);
  color: var(--color-success);
  font-size: 11px;
  font-weight: 600;
  font-family: var(--font-family-mono);
}
.no-discount { color: var(--color-text-tertiary); }

.td-genres { max-width: 200px; }
.genres-tags {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.g-tag {
  font-size: 11px;
  color: var(--color-text-secondary);
  line-height: 1.4;
  padding: 1px 0;
}
.review-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
}
.r-good { background: var(--color-success-bg); color: var(--color-success); }
.r-mid { background: var(--color-warning-bg); color: var(--color-warning); }
.r-bad { background: var(--color-danger-bg); color: var(--color-danger); }
.r-null { background: var(--color-bg-subtle); color: var(--color-text-tertiary); }

/* Pager */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--space-4);
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.page-info b {
  color: var(--color-brand-500);
  font-weight: 700;
  font-family: var(--font-family-mono);
  font-size: 13px;
}
.pager { display: flex; gap: 4px; }
.pager-btn {
  min-width: 32px;
  height: 32px;
  border: 1px solid var(--color-border-default);
  background: var(--color-bg-base);
  color: var(--color-text-secondary);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  padding: 0 8px;
  font-weight: 500;
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.pager-btn:hover:not(:disabled) {
  border-color: var(--color-brand-500);
  color: var(--color-brand-500);
  transform: translateY(-1px);
}
.pager-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pager-btn--active {
  background: linear-gradient(135deg, var(--color-brand-500) 0%, var(--color-accent-500) 100%);
  color: #fff !important;
  border-color: transparent;
  box-shadow: 0 4px 12px var(--color-brand-a35);
}
.pager-btn--nav { padding: 0; }

/* Right column charts */
.chart-card { display: flex; flex-direction: column; }
.user-chart { width: 100%; height: 280px; }
.pager-mini {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.pager-mini-btn {
  width: 24px; height: 24px;
  border: 1px solid var(--color-border-default);
  background: var(--color-bg-base);
  border-radius: 4px;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all var(--duration-fast);
}
.pager-mini-btn:hover:not(:disabled) {
  border-color: var(--color-brand-500);
  color: var(--color-brand-500);
  background: var(--color-bg-overlay);
}
.pager-mini-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pager-mini-text {
  font-family: var(--font-family-mono);
  font-weight: 600;
  min-width: 38px;
  text-align: center;
  color: var(--color-text-primary);
}
.pager-mini-divider {
  margin: 0 1px;
  color: var(--color-text-tertiary);
}

.chart-tools { display: flex; gap: 6px; }
.tool-btn {
  width: 30px; height: 30px;
  border: 1px solid var(--color-border-muted);
  background: var(--color-bg-base);
  border-radius: 6px;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all var(--duration-fast);
}
.tool-btn:hover {
  color: var(--color-brand-500);
  border-color: var(--color-brand-500);
  background: var(--color-bg-overlay);
  transform: translateY(-1px);
}
.tool-btn:active { transform: scale(0.92); }
.genre-chart { width: 100%; height: 280px; }

@media (max-width: 1100px) {
  .content-row { grid-template-columns: 1fr; }
  .search-hero-inner { flex-direction: column; align-items: stretch; }
  .search-bar-wrap { max-width: 100%; min-width: 0; }
  .hero-title { font-size: 24px; }
}
@media (max-width: 600px) {
  .refactor-table { padding: 0 0 var(--space-3); }
  .search-hero { padding: var(--space-5) var(--space-3) var(--space-4); }
  .data-table thead th, .data-table tbody td { padding: 8px 6px; font-size: 12px; }
  .cover-mini { width: 44px; height: 28px; font-size: 12px; }
  .name-text { max-width: 120px; }
  .search-bar-input { font-size: 14px; }
  .hero-title { font-size: 22px; }
  .search-bar-kbd { display: none; }
  .search-bar-go span { display: none; }
  .search-bar-go { padding: 9px 10px; }
}
</style>
