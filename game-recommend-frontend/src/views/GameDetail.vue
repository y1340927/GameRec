<template>
  <div class="page-detail">
    <page-header :title="game ? game.gameName : '游戏详情'" :description="game && game.gameNameCn ? game.gameNameCn : ''">
      <template #before-title>
        <button class="back-btn" @click="$router.back()" aria-label="返回上一页">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M10 3L5 8l5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          返回
        </button>
      </template>
    </page-header>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state" aria-busy="true">
      <div class="skeleton" style="height: 300px; border-radius: var(--radius-xl); margin-bottom: var(--space-4);"></div>
      <div class="skeleton" style="height: 400px; border-radius: var(--radius-xl);"></div>
    </div>

    <!-- 游戏详情 -->
    <div class="detail-layout" v-if="game && !loading">
      <!-- ===== 顶部：游戏封面 ===== -->
      <div class="detail-hero" v-if="game.headerImage || game._hasLocalImage">
        <div class="hero-image-wrap">
          <img :src="getHeroImage(game)" :alt="game.gameName" class="hero-image" @error="onHeroError" />
          <div class="hero-overlay">
            <div class="hero-info">
              <h1 class="hero-title">{{ game.gameName }}</h1>
              <p class="hero-subtitle" v-if="game.gameNameCn">{{ game.gameNameCn }}</p>
              <div class="hero-tags" v-if="getTags(game.genresCn || game.genres).length">
                <span class="hero-tag" v-for="tag in getTags(game.genresCn || game.genres).slice(0, 4)" :key="tag">{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="hero-placeholder" v-else>
        <div class="hero-placeholder-cover" :style="{ background: coverColor }">
          <span class="hero-placeholder-letter">{{ getGameInit }}</span>
        </div>
        <h1 class="hero-title-noimg">{{ game.gameName }}</h1>
        <p class="hero-subtitle-noimg" v-if="game.gameNameCn">{{ game.gameNameCn }}</p>
      </div>

      <!-- ===== 关键评分卡（一目了然的数据） ===== -->
      <div class="score-row">
        <div class="score-card" :class="'score-card--' + metacriticLevel">
          <div class="score-card-icon" aria-hidden="true">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.56 5.82 22 7 14.14 2 9.27l6.91-1.01L12 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="score-card-value">{{ game.metacritic || '---' }}</div>
          <div class="score-card-label">Metacritic</div>
          <div class="score-card-sub" v-if="game.metacritic">{{ metacriticLabel }}</div>
        </div>

        <div class="score-card score-card--primary">
          <div class="score-card-icon" aria-hidden="true">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M14 9l-5 5M9 9l.01.01M15 15l.01.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <rect x="3" y="3" width="18" height="18" rx="3" stroke="currentColor" stroke-width="1.5"/>
            </svg>
          </div>
          <div class="score-card-value">{{ hasReviewData() ? (posPercent + '%') : '---' }}</div>
          <div class="score-card-label">好评率</div>
          <div class="score-card-sub">{{ getReviewDescCn() || '暂无评价' }}</div>
        </div>

        <div class="score-card score-card--warm">
          <div class="score-card-icon" aria-hidden="true">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <circle cx="9" cy="7" r="4" stroke="currentColor" stroke-width="1.5"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="score-card-value">{{ ownerDisplay.value }}</div>
          <div class="score-card-label">{{ ownerDisplay.label }}</div>
        </div>

        <div class="score-card score-card--accent" v-if="hasReviewData()">
          <div class="score-card-icon" aria-hidden="true">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="score-card-value">{{ (game.totalReviews || 0).toLocaleString() }}</div>
          <div class="score-card-label">Steam 评价数</div>
        </div>
      </div>

      <!-- ===== 主内容：AI 助手主导（所有详情、简介、标签、评价均通过 AI 展示） ===== -->
      <div class="ai-driven-section">
        <div class="ai-driven-header">
          <div class="ai-driven-title">
            <span class="ai-driven-icon" aria-hidden="true">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                <path d="M12 2l1.85 4.6L19 7.5l-3.7 3.3.95 5L12 13.6 7.75 15.8l.95-5L5 7.5l5.15-.9L12 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" fill="currentColor" fill-opacity="0.18"/>
                <circle cx="18" cy="5" r="1" fill="currentColor"/>
                <circle cx="5" cy="18" r="0.8" fill="currentColor"/>
                <circle cx="19" cy="17" r="0.6" fill="currentColor"/>
              </svg>
            </span>
            <div>
              <h2 class="ai-driven-h">AI 智能解读</h2>
              <p class="ai-driven-sub">以下信息全部由 GameRec AI 助手基于系统数据库实时生成</p>
            </div>
          </div>
          <div class="ai-driven-tags">
            <span class="ai-driven-tag">游戏简介</span>
            <span class="ai-driven-tag">详细信息</span>
            <span class="ai-driven-tag">评价分析</span>
            <span class="ai-driven-tag">分类标签</span>
            <span class="ai-driven-tag">适合人群</span>
          </div>
        </div>

        <ai-assistant
          :context="'game'"
          :context-id="game ? game.gameId : null"
          :welcome-message="defaultWelcome"
          :show-quick="true"
          :style="{ height: '560px' }"
        />
      </div>
    </div>

    <empty-state v-if="!game && !loading" text="游戏不存在或加载失败" />
  </div>
</template>

<script>
import { getGameById } from '@/api/index';
import PageHeader from '@/components/PageHeader.vue';
import AiAssistant from '@/components/AiAssistant.vue';
import EmptyState from '@/components/EmptyState.vue';

const COVER_COLORS = [
  'var(--color-brand-a20)', 'var(--color-accent-a15)', 'var(--color-success-bg)',
  'var(--color-warm-a15)', 'var(--color-brand-a15)', 'var(--color-info-bg)',
];

export default {
  name: 'GameDetail',
  components: { PageHeader, AiAssistant, EmptyState },
  data() {
    return {
      game: null,
      loading: true,
      heroError: false
    };
  },
  computed: {
    posPercent() {
      if (!this.game) return 0;
      const p = this.game.positivePercentual;
      if (p != null) return (p * 100).toFixed(0);
      const pos = this.game.positiveRatings || 0;
      const neg = this.game.negativeRatings || 0;
      const total = pos + neg;
      return total > 0 ? ((pos / total) * 100).toFixed(0) : 0;
    },
    metacriticLevel() {
      if (!this.game || !this.game.metacritic) return 'none';
      if (this.game.metacritic >= 90) return 'must-play';
      if (this.game.metacritic >= 75) return 'positive';
      if (this.game.metacritic >= 50) return 'mixed';
      return 'negative';
    },
    metacriticLabel() {
      if (!this.game || !this.game.metacritic) return '';
      if (this.game.metacritic >= 90) return '必玩神作';
      if (this.game.metacritic >= 75) return '好评';
      if (this.game.metacritic >= 50) return '褒贬不一';
      return '差评';
    },
    coverColor() {
      let hash = 0;
      const s = (this.game.genres || this.game.gameName || '');
      for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash);
      return COVER_COLORS[Math.abs(hash) % COVER_COLORS.length];
    },
    getGameInit() {
      const n = this.game.gameNameCn || this.game.gameName || '?';
      return n.charAt(0).toUpperCase();
    },
    /** 拥有者数据显示（多级回退） */
    ownerDisplay() {
      if (!this.game) return { value: '---', label: '加载中' };
      // 1. Steam 拥有者（SteamSpy 估算范围）
      const ownersFmt = this.formatOwners(this.game.owners);
      if (ownersFmt) return { value: ownersFmt, label: 'Steam 拥有者' };
      // 2. 系统游玩人数
      if (this.game.playCount > 0) return { value: this.game.playCount.toLocaleString(), label: '系统游玩人数' };
      // 3. Steam 好评数
      if (this.game.positiveRatings > 0) return { value: this.game.positiveRatings.toLocaleString(), label: 'Steam 好评数' };
      // 4. Steam 评价总数
      if (this.game.totalReviews > 0) return { value: this.game.totalReviews.toLocaleString(), label: 'Steam 评价数' };
      return { value: '---', label: '暂无数据' };
    },
    defaultWelcome() {
      if (!this.game) return '你好！我是 GameRec AI 助手。';
      const name = this.game.gameNameCn || this.game.gameName;
      return `你好！我是 GameRec AI 助手。当前已为你加载「${name}」的系统完整数据。\n\n你可以问我：\n- 介绍一下这款游戏的核心玩法和适合人群\n- 这款游戏值不值得入手\n- 系统里玩家对它的评价如何\n- 它的开发商和发行商是谁\n- 总结这款游戏的优缺点`;
    }
  },
  mounted() {
    this.loadGame();
  },
  methods: {
    async loadGame() {
      const gameId = this.$route.params.gameId;
      try {
        const { data } = await getGameById(gameId);
        if (data.code === 200) {
          const g = data.data;
          if (!g.headerImage) {
            g._hasLocalImage = true;
            g.headerImage = `/images/games/${g.gameId}.jpg`;
          }
          this.game = g;
        }
      } catch (e) {
        this.$message.error('加载游戏详情失败');
      } finally {
        this.loading = false;
      }
    },
    getHeroImage(game) {
      if (game.headerImage) return game.headerImage;
      return `/images/games/${game.gameId}.jpg`;
    },
    onHeroError() {
      this.heroError = true;
    },
    getReviewDescCn() {
      if (this.game.reviewScoreDescCn) return this.game.reviewScoreDescCn;
      const desc = this.game.reviewScoreDesc;
      if (!desc) return '';
      const map = {
        'Overwhelmingly Positive': '好评如潮',
        'Very Positive': '特别好评',
        'Positive': '好评',
        'Mostly Positive': '多半好评',
        'Mixed': '褒贬不一',
        'Mostly Negative': '多半差评',
        'Negative': '差评',
        'Very Negative': '特别差评',
        'Overwhelmingly Negative': '差评如潮',
        'No user reviews': '暂无评价'
      };
      return map[desc] || desc;
    },
    hasReviewData() {
      if (this.game.totalReviews && this.game.totalReviews > 0) return true;
      if (this.game.totalPositive && this.game.totalPositive > 0) return true;
      if ((this.game.positiveRatings || 0) + (this.game.negativeRatings || 0) > 0) return true;
      return false;
    },
    getTags(str) {
      if (!str) return [];
      const arr = Array.isArray(str) ? str : String(str).split(/[,;]/);
      return arr.map(t => t.trim()).filter(Boolean);
    },
    /**
     * 格式化 Steam 拥有者范围为中文可读格式
     * "50000000-100000000" → "5000万-1亿"
     * "10000000-20000000" → "1000万-2000万"
     */
    formatOwners(ownersStr) {
      if (!ownersStr || ownersStr === '0-0') return '';
      const parts = String(ownersStr).split('-');
      if (parts.length !== 2) return ownersStr;
      const lo = parseInt(parts[0], 10);
      const hi = parseInt(parts[1], 10);
      if (isNaN(lo) || isNaN(hi) || (lo === 0 && hi === 0)) return '';
      const fmt = (n) => {
        if (n >= 100000000) {
          return (n / 100000000).toFixed(n % 100000000 === 0 ? 0 : 1) + '亿';
        } else if (n >= 10000) {
          return (n / 10000).toFixed(n % 10000 === 0 ? 0 : 0) + '万';
        } else {
          return n.toLocaleString();
        }
      };
      return fmt(lo) + '-' + fmt(hi);
    }
  }
};
</script>

<style scoped>
.page-detail { max-width: var(--content-max-width); margin: 0 auto; }

.back-btn {
  display: inline-flex; align-items: center; gap: var(--space-1);
  padding: var(--space-1) var(--space-2); margin-bottom: var(--space-2);
  background: none; border: none; color: var(--color-text-tertiary);
  font-size: var(--text-sm); cursor: pointer; border-radius: var(--radius-md);
  transition: color var(--duration-fast);
}
.back-btn:hover { color: var(--color-text-primary); }

/* ===== Hero / 封面区域 ===== */
.detail-hero { margin-bottom: var(--space-5); border-radius: var(--radius-2xl); overflow: hidden; }
.hero-image-wrap { position: relative; width: 100%; aspect-ratio: 460/215; max-height: 320px; overflow: hidden; background: var(--color-bg-subtle); }
.hero-image { width: 100%; height: 100%; object-fit: cover; display: block; }
.hero-overlay {
  position: absolute; bottom: 0; left: 0; right: 0;
  padding: var(--space-6); background: linear-gradient(transparent, rgba(0,0,0,0.85));
}
.hero-info { display: flex; flex-direction: column; gap: 6px; }
.hero-title { font-size: 26px; font-weight: var(--font-weight-bold); color: #fff; margin: 0; text-shadow: 0 2px 8px rgba(0,0,0,0.6); letter-spacing: -0.3px; }
.hero-subtitle { font-size: var(--text-sm); color: rgba(255,255,255,0.85); margin: 0; font-weight: 500; }
.hero-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; }
.hero-tag {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid rgba(255,255,255,0.25);
}

.hero-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: var(--space-10); margin-bottom: var(--space-5);
  border-radius: var(--radius-2xl); border: 1px solid var(--color-border-muted);
  background: var(--color-bg-elevated);
}
.hero-placeholder-cover {
  width: 96px; height: 96px; border-radius: var(--radius-xl);
  display: flex; align-items: center; justify-content: center; margin-bottom: var(--space-4);
}
.hero-placeholder-letter {
  font-family: var(--font-family-display); font-size: 2.4rem;
  font-weight: var(--font-weight-bold); color: var(--color-text-secondary);
}
.hero-title-noimg { font-size: 26px; font-weight: var(--font-weight-bold); color: var(--color-text-primary); margin: 0; text-align: center; }
.hero-subtitle-noimg { font-size: var(--text-sm); color: var(--color-text-tertiary); margin: var(--space-1) 0 0; }

/* ===== 评分卡行 ===== */
.score-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}
.score-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  text-align: center;
  transition: all var(--duration-normal) var(--ease-out-expo);
  position: relative;
  overflow: hidden;
}
.score-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: currentColor;
  opacity: 0.5;
}
.score-card:hover {
  border-color: var(--color-border-default);
  transform: translateY(-2px);
}
.score-card-icon {
  margin-bottom: var(--space-2);
  display: inline-flex;
}
.score-card-value {
  font-family: var(--font-family-display);
  font-size: 28px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1;
  margin-bottom: 4px;
}
.score-card--primary .score-card-value { color: var(--color-brand-400); }
.score-card--accent .score-card-value { color: var(--color-accent-400); }
.score-card--warm .score-card-value { color: var(--color-warm-500); }
.score-card--must-play .score-card-value { color: var(--color-mc-mustplay); }
.score-card--positive .score-card-value { color: var(--color-mc-positive); }
.score-card--mixed .score-card-value { color: var(--color-mc-mixed); }
.score-card--negative .score-card-value { color: var(--color-mc-negative); }
.score-card--none .score-card-value { color: var(--color-text-tertiary); }
.score-card--primary .score-card-icon { color: var(--color-brand-500); }
.score-card--accent .score-card-icon { color: var(--color-accent-400); }
.score-card--warm .score-card-icon { color: var(--color-warm-500); }
.score-card-label {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: var(--space-1);
  font-weight: 600;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}
.score-card-sub {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

/* ===== AI 驱动区 ===== */
.ai-driven-section {
  position: relative;
  background: linear-gradient(135deg,
    color-mix(in srgb, var(--color-bg-elevated) 95%, var(--color-brand-500) 5%) 0%,
    var(--color-bg-elevated) 100%);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-2xl);
  padding: var(--space-5);
  overflow: hidden;
}
.ai-driven-section::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-brand-a50), var(--color-accent-a50), transparent);
}
.ai-driven-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  gap: var(--space-3);
  flex-wrap: wrap;
}
.ai-driven-title {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.ai-driven-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  font-size: 22px;
  background: linear-gradient(135deg, var(--color-brand-a15), var(--color-accent-a10));
  border: 1px solid var(--color-brand-a25);
  flex-shrink: 0;
  color: var(--color-brand-400);
  /* 关键修复K: 静态光晕，避免 SVG 在某些 GPU 上抖动 */
  filter: drop-shadow(0 0 4px var(--color-brand-a35));
  transform: translateZ(0);
}
.ai-driven-icon svg {
  display: block;
}
.ai-driven-h {
  font-size: 18px;
  font-weight: 800;
  color: var(--color-text-primary);
  margin: 0;
  letter-spacing: -0.3px;
}
.ai-driven-sub {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin: 2px 0 0;
  font-weight: 500;
}
.ai-driven-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.ai-driven-tag {
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-brand-400);
  background: var(--color-brand-a10);
  border: 1px solid var(--color-brand-a20);
  border-radius: var(--radius-full);
}

@media (max-width: 1024px) {
  .score-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .score-row { grid-template-columns: repeat(2, 1fr); }
  .hero-title { font-size: 20px; }
  .ai-driven-header { flex-direction: column; align-items: flex-start; }
}
@media (max-width: 480px) {
  .score-row { grid-template-columns: 1fr; }
}
</style>
