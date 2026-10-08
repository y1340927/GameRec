<template>
  <div class="page-recommend">
    <page-header title="推荐结果" description="6 种算法 · 混合推荐 · 个性化推荐" />

    <!-- 查询栏 -->
    <query-bar>
      <div class="query-input-wrap">
        <svg class="query-input-icon" width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
          <circle cx="9" cy="5.25" r="3" stroke="currentColor" stroke-width="1.5"/>
          <path d="M3 15.75c0-3.314 2.686-6 6-6s6 2.686 6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        <input v-model="userId" type="number" placeholder="输入用户ID (1-56789)" class="query-input" aria-label="用户ID" @keyup.enter="loadRecommend"/>
      </div>
      <div class="query-select-wrap query-select-wrap--sm">
        <select id="topn-select" v-model="topN" class="query-select" aria-label="推荐数量">
          <option :value="10">10条</option>
          <option :value="20">20条</option>
          <option :value="30">30条</option>
          <option :value="50">50条</option>
        </select>
      </div>
      <div class="btn-group">
        <button class="btn btn-primary" @click="loadRecommend" :disabled="loading">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M8 1l1.87 3.974L14 5.57l-3 2.987.733 4.243L8 11.01l-3.733 1.79L5 8.557 2 5.57l4.13-.596L8 1z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
          </svg>
          <span class="btn-text">获取推荐</span>
        </button>
        <button class="btn btn-ghost" @click="goProfile" :disabled="!userId">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="6" cy="5" r="3.25" stroke="currentColor" stroke-width="1.5"/><path d="M2 14c0-2.21 1.79-4 4-4s4 1.79 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          <span class="btn-text">查看画像</span>
        </button>
      </div>
    </query-bar>

    <!-- 算法选择 — 简洁行内选择器 -->
    <div class="section-label">推荐算法</div>
    <div class="algo-row">
      <button
        v-for="algo in algorithms"
        :key="algo.key"
        class="algo-btn"
        :class="{ 'algo-btn--active': algorithm === algo.key }"
        @click="selectAlgo(algo.key)"
      >
        <span class="algo-btn-dot" :style="{ background: (algorithm === algo.key) ? 'var(--color-brand-500)' : 'var(--color-border-strong)' }"></span>
        <span class="algo-btn-label">{{ algo.label }}</span>
        <span class="algo-btn-desc">{{ algo.desc }}</span>
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state" aria-busy="true">
      <div class="skeleton" style="height: 300px; border-radius: var(--radius-xl);"></div>
    </div>

    <!-- 推荐结果 — 简洁列表 -->
    <template v-if="recommendations.length > 0 && !loading">
      <div class="section-label" style="margin-top: var(--space-6);">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M7 1l1.87 3.974L14 5.57l-3 2.987.733 4.243L7 11.01l-3.733 1.79L5 8.557 2 5.57l4.13-.596L7 1z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
        推荐结果
        <span class="label-badge">{{ algorithmLabel }}</span>
        <span class="label-count">Top-{{ topN }} · {{ recommendations.length }} 款</span>
      </div>

      <div class="rec-list" role="list">
        <div
          v-for="(game, idx) in recommendations"
          :key="game.gameId"
          class="rec-item"
          :style="{ animationDelay: idx * 40 + 'ms' }"
          role="listitem"
          @click="goGameDetail(game.gameId)"
        >
          <div class="rec-item-pos">
            <span class="rec-item-num" :class="getNumClass(idx)">{{ idx + 1 }}</span>
          </div>
          <div class="rec-item-cover" :style="{ background: coverColor(game) }">
            <img
              :src="`/images/games/${game.gameId}.jpg`"
              :alt="game.gameName"
              class="rec-item-cover-img"
              @error="hideRecCover($event)"
            />
            <span class="rec-item-initial">{{ getGameInit(game) }}</span>
          </div>
          <div class="rec-item-body">
            <div class="rec-item-title-row">
              <span class="rec-item-name" :title="game.gameName">{{ game.gameName }}</span>
              <span class="rec-item-cn" v-if="game.gameNameCn">{{ game.gameNameCn }}</span>
            </div>
            <div class="rec-item-tags" v-if="game.genres">
              <span class="rec-item-tag" v-for="tag in getFirstTags(game.genres, 2)" :key="tag">{{ tag }}</span>
            </div>
          </div>
          <div class="rec-item-score">
            <div class="rec-item-bar">
              <div class="rec-item-bar-fill" :style="{ width: scorePercent(game) + '%' }"></div>
            </div>
            <span class="rec-item-score-val">{{ scoreValue(game).toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- 默认：全站热门 -->
    <template v-if="!recommendations.length && !loading && !hasQueried">
      <div class="section-label" style="margin-top: var(--space-6);">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M7 13c-2-1.3-4-3.5-4-6.5 0-1.8 1-3 2.5-3.8C6.5 4 7.5 6 8 7c.5-1 1.5-3 2.5-4.8 1.5.8 2.5 2 2.5 3.8 0 3-2 5.2-4 6.5z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
        全站热门
        <span class="label-badge">流行度排序</span>
      </div>

      <!-- 热门卡片网格 — 简洁风格 -->
      <div class="hot-grid" v-if="defaultHotGames.length > 0" role="list">
        <div
          v-for="(game, idx) in defaultHotGames"
          :key="game.gameId"
          class="hot-card"
          :style="{ animationDelay: idx * 40 + 'ms' }"
          role="listitem"
          @click="goGameDetail(game.gameId)"
        >
          <div class="hot-card-cover" :style="{ background: coverColor(game) }">
            <img
              :src="`/images/games/${game.gameId}.jpg`"
              :alt="game.gameName"
              class="hot-card-cover-img"
              @error="hideHotCover($event)"
            />
            <span class="hot-card-initial">{{ getGameInit(game) }}</span>
          </div>
          <div class="hot-card-body">
            <div class="hot-card-genres" v-if="game.genres">
              <span class="hot-card-genre" v-for="tag in getFirstTags(game.genres, 2)" :key="tag">{{ tag }}</span>
            </div>
            <span class="hot-card-name" :title="game.gameName">{{ game.gameName }}</span>
            <span class="hot-card-cn" v-if="game.gameNameCn">{{ game.gameNameCn }}</span>
            <div class="hot-card-meta">
              <span v-if="game.purchaseCount" class="hot-card-meta-item">购买 {{ formatNum(game.purchaseCount) }}</span>
              <span v-if="game.playCount" class="hot-card-meta-item">游玩 {{ formatNum(game.playCount) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 快速入门 -->
      <div class="quick-guide">
        <div class="quick-guide-inner">
          <svg class="quick-guide-icon" width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M12 4l4.2 9h9.8l-8 6.4 2.8 9.6L12 22.4l-8.8 5.6 2.8-9.6-8-6.4h9.8L12 4z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
          <span class="quick-guide-text">输入用户ID获取个性化推荐，或试试 <button class="quick-guide-link" @click="randomUser">随机用户</button></span>
        </div>
      </div>
    </template>

    <!-- 无结果 -->
    <empty-state
      v-if="!recommendations.length && !loading && hasQueried"
      text="未找到推荐结果"
      hint="该用户数据可能不足，尝试更换算法或用户ID"
    />
  </div>
</template>

<script>
import { getHybrid, getUserCF, getItemCF, getContentBased, getSVD, getPopular } from '@/api/index';
import PageHeader from '@/components/PageHeader.vue';
import QueryBar from '@/components/QueryBar.vue';
import EmptyState from '@/components/EmptyState.vue';

const COVER_COLORS = [
  'rgba(99,102,241,0.15)', 'rgba(6,182,212,0.12)', 'rgba(16,185,129,0.11)',
  'rgba(245,158,11,0.11)', 'rgba(139,92,246,0.12)', 'rgba(59,130,246,0.12)',
];

export default {
  name: 'Recommend',
  components: { PageHeader, QueryBar, EmptyState },
  data() {
    return {
      userId: '', algorithm: 'hybrid', topN: 10, loading: false,
      recommendations: [], defaultHotGames: [], hasQueried: false,
      algorithms: [
        { key: 'hybrid', label: '混合推荐', desc: '多算法融合' },
        { key: 'usercf', label: 'User-CF', desc: '相似用户协同过滤' },
        { key: 'itemcf', label: 'Item-CF', desc: '物品相似度' },
        { key: 'cb', label: 'Content-Based', desc: '内容特征匹配' },
        { key: 'svd', label: 'SVD', desc: '矩阵分解' },
        { key: 'popular', label: '流行度', desc: '全站热门' },
      ]
    };
  },
  computed: {
    algorithmLabel() {
      const a = this.algorithms.find(x => x.key === this.algorithm);
      return a ? a.label : this.algorithm;
    },
    maxScore() {
      if (!this.recommendations.length) return 1;
      return Math.max(...this.recommendations.map(g => this.scoreValue(g)), 0.01);
    }
  },
  mounted() {
    this.loadPopularDefault();
    if (this.$route.query.userId) { this.userId = this.$route.query.userId; this.loadRecommend(); }
  },
  methods: {
    coverColor(game) {
      let hash = 0; const s = (game.genres || game.gameName || '');
      for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash);
      return COVER_COLORS[Math.abs(hash) % COVER_COLORS.length];
    },
    getGameInit(game) {
      const name = game.gameNameCn || game.gameName || '?';
      return name.charAt(0).toUpperCase();
    },
    getFirstTags(genres, n) {
      if (!genres) return [];
      const arr = Array.isArray(genres) ? genres : String(genres).split(',');
      return arr.slice(0, n).map(t => String(t).trim()).filter(Boolean);
    },
    getNumClass(idx) { return idx < 3 ? 'rec-item-num--' + (idx + 1) : ''; },
    scoreValue(game) {
      const map = { hybrid: 'hybridScore', usercf: 'score', itemcf: 'score', cb: 'score', svd: 'predictedRating', popular: 'popularity' };
      return game[map[this.algorithm] || 'score'] || 0;
    },
    scorePercent(game) {
      return this.maxScore > 0 ? Math.min((this.scoreValue(game) / this.maxScore) * 100, 100) : 0;
    },
    selectAlgo(key) { this.algorithm = key; if (this.userId) this.loadRecommend(); },
    async loadPopularDefault() {
      try { const { data } = await getPopular(12); if (data.code === 200) this.defaultHotGames = data.data; }
      catch (e) { /* */ }
    },
    async loadRecommend() {
      if (!this.userId) { this.$message.warning('请输入用户ID'); return; }
      this.loading = true; this.hasQueried = true;
      try {
        let res;
        switch (this.algorithm) {
          case 'usercf': res = await getUserCF(this.userId, 10, this.topN); break;
          case 'itemcf': res = await getItemCF(this.userId, this.topN); break;
          case 'cb': res = await getContentBased(this.userId, this.topN); break;
          case 'svd': res = await getSVD(this.userId, this.topN); break;
          case 'popular': res = await getPopular(this.topN); break;
          default: res = await getHybrid(this.userId, this.topN);
        }
        if (res.data.code === 200) this.recommendations = res.data.data;
        else this.$message.error(res.data.message || '获取失败');
      } catch (e) { this.$message.error('获取推荐失败'); }
      finally { this.loading = false; }
    },
    randomUser() { this.userId = String(Math.floor(Math.random() * 56789) + 1); this.loadRecommend(); },
    formatNum(n) { if (!n) return '0'; if (n >= 10000) return (n / 10000).toFixed(1) + '万'; return n.toLocaleString(); },
    hideRecCover(event) {
      const img = event.target;
      img.style.display = 'none';
    },
    hideHotCover(event) {
      const img = event.target;
      img.style.display = 'none';
    },
    goProfile() { this.$router.push({ path: '/profile', query: { userId: this.userId } }); },
    goGameDetail(gameId) { this.$router.push(`/game/${gameId}`); }
  }
};
</script>

<style scoped>
.page-recommend { max-width: var(--content-max-width); margin: 0 auto; }

.query-input-wrap { position: relative; flex: 1; min-width: 160px; max-width: 260px; }
.query-input-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--color-text-tertiary); pointer-events: none; }
.query-input { width: 100%; padding: 10px 12px 10px 38px; border: 1px solid var(--color-border-default); border-radius: var(--radius-xl); background: var(--color-bg-subtle); color: var(--color-text-primary); font-family: var(--font-family-mono); font-size: var(--text-sm); transition: border-color var(--duration-fast); }
.query-input:focus { outline: none; border-color: var(--color-border-strong); }
.query-input::placeholder { color: var(--color-text-tertiary); }

.query-select-wrap--sm { width: 85px; }
.query-select { width: 100%; padding: 10px 28px 10px 10px; border: 1px solid var(--color-border-default); border-radius: var(--radius-xl); background: var(--color-bg-subtle); color: var(--color-text-primary); font-size: var(--text-sm); cursor: pointer; appearance: none; background-image: url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%2371717a' stroke-width='1.5'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 8px center; }
.query-select:focus { outline: none; border-color: var(--color-border-strong); }

.btn-group { display: flex; gap: var(--space-2); }
.btn { display: inline-flex; align-items: center; gap: var(--space-2); padding: 10px 16px; border: none; border-radius: var(--radius-xl); font-size: var(--text-sm); font-weight: var(--font-weight-medium); cursor: pointer; transition: all var(--duration-fast); white-space: nowrap; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: var(--color-bg-subtle); color: var(--color-text-primary); border: 1px solid var(--color-border-strong); }
.btn-primary:hover:not(:disabled) { background: var(--color-bg-overlay); }
.btn-ghost { background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-border-default); }
.btn-ghost:hover:not(:disabled) { background: var(--color-bg-subtle); color: var(--color-text-primary); }

/* Section Label */
.section-label { font-size: var(--text-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); margin-bottom: var(--space-4); display: flex; align-items: center; gap: var(--space-2); }
.section-label::before { content: ''; width: 3px; height: 16px; border-radius: var(--radius-full); background: var(--color-text-tertiary); }
.label-badge { padding: 1px 8px; border-radius: var(--radius-full); background: var(--color-bg-subtle); color: var(--color-text-tertiary); font-size: var(--text-2xs); }
.label-count { font-size: var(--text-xs); color: var(--color-text-tertiary); margin-left: auto; font-weight: var(--font-weight-normal); }

/* ---- Algorithm Row ---- */
.algo-row { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-bottom: var(--space-6); }
.algo-btn { display: flex; align-items: center; gap: var(--space-2); padding: 8px 14px; border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl); background: var(--color-bg-elevated); cursor: pointer; transition: all var(--duration-fast); }
.algo-btn:hover { border-color: var(--color-border-default); }
.algo-btn--active { border-color: var(--color-border-strong); background: var(--color-bg-subtle); }
.algo-btn-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; transition: background var(--duration-fast); }
.algo-btn-label { font-size: var(--text-xs); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }
.algo-btn-desc { font-size: var(--text-2xs); color: var(--color-text-tertiary); }

/* ---- Rec List ---- */
.rec-list { border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl); overflow: hidden; background: var(--color-bg-elevated); }
.rec-item {
  display: flex; align-items: center; gap: var(--space-3); padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border-muted); cursor: pointer;
  transition: background var(--duration-fast); animation: row-in 0.3s var(--ease-out) both;
}
.rec-item:last-child { border-bottom: none; }
.rec-item:hover { background: var(--color-bg-subtle); }
@keyframes row-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.rec-item-pos { width: 22px; flex-shrink: 0; text-align: center; }
.rec-item-num { font-family: var(--font-family-mono); font-size: var(--text-xs); color: var(--color-text-tertiary); }
.rec-item-num--1 { color: #f59e0b; font-weight: var(--font-weight-bold); }
.rec-item-num--2 { color: #a1a1aa; font-weight: var(--font-weight-semibold); }
.rec-item-num--3 { color: #f97316; font-weight: var(--font-weight-semibold); }

.rec-item-cover { width: 36px; height: 36px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden; position: relative; }
.rec-item-cover-img { width: 100%; height: 100%; object-fit: cover; display: block; position: absolute; top: 0; left: 0; }
.rec-item-initial { font-family: var(--font-family-display); font-size: var(--text-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); }

.rec-item-body { flex: 1; min-width: 0; }
.rec-item-title-row { display: flex; align-items: baseline; gap: var(--space-2); }
.rec-item-name { font-size: var(--text-sm); font-weight: var(--font-weight-medium); color: var(--color-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rec-item-cn { font-size: var(--text-2xs); color: var(--color-text-tertiary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rec-item-tags { display: flex; gap: 4px; margin-top: 2px; }
.rec-item-tag { padding: 0 6px; border-radius: var(--radius-full); background: var(--color-bg-subtle); color: var(--color-text-tertiary); font-size: var(--text-2xs); }

.rec-item-score { display: flex; align-items: center; gap: var(--space-2); flex-shrink: 0; width: 120px; }
.rec-item-bar { flex: 1; height: 3px; border-radius: var(--radius-full); background: var(--color-bg-subtle); overflow: hidden; }
.rec-item-bar-fill { height: 100%; border-radius: var(--radius-full); background: var(--color-text-tertiary); transition: width 0.6s var(--ease-out); }
.rec-item-score-val { font-family: var(--font-family-mono); font-size: var(--text-2xs); color: var(--color-text-secondary); min-width: 36px; text-align: right; }

/* ---- Hot Grid ---- */
.hot-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: var(--space-3); margin-bottom: var(--space-6); }
.hot-card { display: flex; background: var(--color-bg-elevated); border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl); overflow: hidden; cursor: pointer; transition: background var(--duration-fast); animation: row-in 0.3s var(--ease-out) both; }
.hot-card:hover { background: var(--color-bg-subtle); }
.hot-card-cover { width: 64px; min-height: 100%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden; position: relative; }
.hot-card-cover-img { width: 100%; height: 100%; object-fit: cover; display: block; position: absolute; top: 0; left: 0; }
.hot-card-initial { font-family: var(--font-family-display); font-size: 1.2rem; font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); }
.hot-card-body { flex: 1; padding: var(--space-3); display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.hot-card-genres { display: flex; gap: 4px; }
.hot-card-genre { padding: 0 6px; border-radius: var(--radius-full); background: var(--color-bg-subtle); color: var(--color-text-tertiary); font-size: var(--text-2xs); }
.hot-card-name { font-size: var(--text-xs); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hot-card-cn { font-size: var(--text-2xs); color: var(--color-text-tertiary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hot-card-meta { display: flex; gap: var(--space-3); margin-top: auto; padding-top: 2px; }
.hot-card-meta-item { font-size: var(--text-2xs); color: var(--color-text-tertiary); }

/* ---- Quick Guide ---- */
.quick-guide { margin-top: var(--space-6); }
.quick-guide-inner { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-4); background: var(--color-bg-elevated); border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl); font-size: var(--text-sm); color: var(--color-text-secondary); }
.quick-guide-icon { color: var(--color-text-tertiary); flex-shrink: 0; opacity: 0.5; }
.quick-guide-link { border: none; background: none; color: var(--color-brand-400); font-size: inherit; cursor: pointer; text-decoration: underline; text-underline-offset: 2px; padding: 0; }
.quick-guide-link:hover { color: var(--color-brand-300); }

@media (max-width: 768px) {
  .algo-row { gap: var(--space-1); }
  .algo-btn-desc { display: none; }
  .rec-item-score { width: 80px; }
  .rec-item-bar { display: none; }
  .hot-grid { grid-template-columns: 1fr; }
  .query-input-wrap { max-width: 100%; width: 100%; }
}
@media (max-width: 480px) { .btn-text { display: none; } .algo-btn { padding: 6px 10px; } }
</style>
