<template>
  <div class="search-page">
    <!-- 搜索头部 -->
    <div class="search-hero">
      <div class="search-hero-bg"></div>
      <div class="search-hero-content">
        <div class="search-icon-big">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8"/>
            <path d="M16.5 16.5L21 21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <h1 class="search-hero-title">探索游戏世界</h1>
        <p class="search-hero-sub">搜索超过 {{ totalGames || '27,000' }} 款游戏，发现你的下一个最爱</p>

        <!-- 搜索框 -->
        <div class="search-box-wrapper" ref="searchBoxWrapper">
          <div class="search-box" :class="{ 'search-box--focused': isFocused, 'search-box--has-value': keyword }">
            <svg class="search-box-icon" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8"/>
              <path d="M16.5 16.5L21 21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
            <input
              ref="searchInput"
              v-model="keyword"
              @focus="onFocus"
              @blur="onBlur"
              @input="onInput"
              @keydown="onKeydown"
              placeholder="输入游戏名称、类型或标签..."
              class="search-box-input"
              autocomplete="off"
            />
            <button
              v-if="keyword"
              class="search-box-clear"
              @mousedown.prevent="clearSearch"
              title="清除"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.2" fill="var(--color-bg-subtle)"/>
                <path d="M5.5 5.5l5 5M10.5 5.5l-5 5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
              </svg>
            </button>
            <div class="search-box-shortcut" v-if="!isFocused && !keyword">
              <kbd>Ctrl</kbd><span>+</span><kbd>K</kbd>
            </div>
          </div>

          <!-- 自动补全下拉 -->
          <transition name="dropdown-fade">
            <div class="suggestions-dropdown" v-if="showSuggestions && suggestions.length > 0">
              <div class="suggestions-header">搜索建议</div>
              <div
                v-for="(s, idx) in suggestions"
                :key="s.gameId"
                class="suggestion-item"
                :class="{ 'suggestion-item--active': idx === activeSuggestionIdx }"
                @mousedown.prevent="selectSuggestion(s)"
                @mouseenter="activeSuggestionIdx = idx"
              >
                <div class="suggestion-cover">
                  <img
                    :src="`/images/games/${s.gameId}.jpg`"
                    :alt="s.gameNameCn || s.gameName"
                    @error="onSuggestionImgError($event)"
                    class="suggestion-cover-img"
                  />
                  <span class="suggestion-cover-fallback" :style="{ background: suggestionColor(s) }">{{ getSuggestionInit(s) }}</span>
                </div>
                <div class="suggestion-info">
                  <div class="suggestion-name">{{ s.gameNameCn || s.gameName }}</div>
                  <div class="suggestion-meta">
                    <span v-if="s.genresCn || s.genres">{{ getFirstTag(s.genresCn || s.genres) }}</span>
                    <span v-if="s.totalReviews" class="suggestion-reviews">{{ (s.totalReviews || 0).toLocaleString() }} 评价</span>
                  </div>
                </div>
                <span class="suggestion-arrow">↵</span>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>

    <!-- 搜索结果区域 -->
    <div class="search-results-section">
      <!-- 筛选标签 -->
      <div class="filter-bar" v-if="results.length > 0 || keyword">
        <div class="filter-left">
          <span class="filter-count" v-if="totalResults > 0">
            找到 <strong>{{ totalResults.toLocaleString() }}</strong> 个结果
            <template v-if="keyword"> — "<em>{{ keyword }}</em>"</template>
          </span>
          <span class="filter-count" v-else-if="!loading && searched">未找到相关游戏</span>
        </div>
        <div class="filter-right">
          <span class="filter-label">排序：</span>
          <button
            v-for="opt in sortOptions"
            :key="opt.value"
            class="filter-sort-btn"
            :class="{ 'filter-sort-btn--active': sortBy === opt.value }"
            @click="changeSort(opt.value)"
          >{{ opt.label }}</button>
        </div>
      </div>

      <!-- 加载态 -->
      <div class="results-grid" v-if="loading">
        <div v-for="i in 6" :key="i" class="result-card result-card--skeleton">
          <div class="skeleton-img"></div>
          <div class="skeleton-body">
            <div class="skeleton-line skeleton-line--title"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line skeleton-line--short"></div>
          </div>
        </div>
      </div>

      <!-- 搜索结果网格 -->
      <div class="results-grid" v-else-if="results.length > 0">
        <div
          v-for="g in results"
          :key="g.gameId"
          class="result-card"
          @click="goGame(g.gameId)"
          tabindex="0"
          @keydown.enter="goGame(g.gameId)"
          role="link"
          :aria-label="'查看 ' + (g.gameNameCn || g.gameName) + ' 详情'"
        >
          <div class="result-card-img-wrap">
            <img
              :src="`/images/games/${g.gameId}.jpg`"
              :alt="g.gameNameCn || g.gameName"
              class="result-card-img"
              @error="onResultImgError($event, g)"
              loading="lazy"
            />
            <div class="result-card-img-fallback" :style="{ background: cardColor(g) }">
              <span class="img-fallback-letter">{{ getGameInit(g) }}</span>
            </div>
            <!-- 价格标签 -->
            <div class="result-card-price" :class="{ 'price-free': g.isFree || g.price === 0 }">
              {{ g.isFree || g.price === 0 ? '免费' : ('¥' + ((g.priceInitial || g.price) * 7.2).toFixed(0)) }}
            </div>
            <!-- 评分徽章 -->
            <div class="result-card-score" v-if="g.metacritic" :class="'mc-' + metacriticLevel(g.metacritic)">
              {{ g.metacritic }}
            </div>
          </div>
          <div class="result-card-body">
            <h3 class="result-card-title" :title="g.gameNameCn || g.gameName">
              {{ g.gameNameCn || g.gameName }}
            </h3>
            <div class="result-card-genres" v-if="g.genresCn || g.genres">
              <span
                v-for="(tag, ti) in getTags(g.genresCn || g.genres).slice(0, 3)"
                :key="ti"
                class="genre-badge"
              >{{ tag }}</span>
            </div>
            <div class="result-card-desc" v-if="g.shortDescription">
              {{ g.shortDescription }}
            </div>
            <div class="result-card-desc result-card-desc--fallback" v-else>
              {{ (g.genresCn || g.genres || '') }} 类型 · {{ g.developer || '未知开发商' }}
            </div>
            <div class="result-card-footer">
              <span class="result-card-review" :class="reviewClass(g.reviewScoreDescCn)">
                <span class="review-dot"></span>
                {{ g.reviewScoreDescCn || '暂无评价' }}
              </span>
              <span class="result-card-date" v-if="g.releaseDate">{{ formatDate(g.releaseDate) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空结果 -->
      <div class="empty-result" v-else-if="!loading && searched">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
            <circle cx="28" cy="28" r="16" stroke="currentColor" stroke-width="2.5" opacity="0.4"/>
            <path d="M40 40l14 14" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.4"/>
            <path d="M22 28h12M28 22v12" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.2"/>
          </svg>
        </div>
        <h3>未找到匹配的游戏</h3>
        <p>试试其他关键词，或浏览下方热门游戏</p>
      </div>

      <!-- 默认热门游戏 -->
      <div v-if="!keyword && !loading && defaultGames.length > 0">
        <div class="section-header">
          <h2 class="section-title">热门游戏</h2>
          <span class="section-meta">按评价数排序</span>
        </div>
        <div class="results-grid">
          <div
            v-for="g in defaultGames"
            :key="g.gameId"
            class="result-card"
            @click="goGame(g.gameId)"
            tabindex="0"
            @keydown.enter="goGame(g.gameId)"
          >
            <div class="result-card-img-wrap">
              <img
                :src="`/images/games/${g.gameId}.jpg`"
                :alt="g.gameNameCn || g.gameName"
                class="result-card-img"
                @error="onResultImgError($event, g)"
                loading="lazy"
              />
              <div class="result-card-img-fallback" :style="{ background: cardColor(g) }">
                <span class="img-fallback-letter">{{ getGameInit(g) }}</span>
              </div>
              <div class="result-card-price" :class="{ 'price-free': g.isFree || g.price === 0 }">
                {{ g.isFree || g.price === 0 ? '免费' : ('¥' + ((g.priceInitial || g.price) * 7.2).toFixed(0)) }}
              </div>
              <div class="result-card-score" v-if="g.metacritic" :class="'mc-' + metacriticLevel(g.metacritic)">
                {{ g.metacritic }}
              </div>
            </div>
            <div class="result-card-body">
              <h3 class="result-card-title">{{ g.gameNameCn || g.gameName }}</h3>
              <div class="result-card-genres" v-if="g.genresCn || g.genres">
                <span v-for="(tag, ti) in getTags(g.genresCn || g.genres).slice(0, 3)" :key="ti" class="genre-badge">{{ tag }}</span>
              </div>
              <div class="result-card-desc" v-if="g.shortDescription">{{ g.shortDescription }}</div>
              <div class="result-card-footer">
                <span class="result-card-review" :class="reviewClass(g.reviewScoreDescCn)">
                  <span class="review-dot"></span>{{ g.reviewScoreDescCn || '暂无评价' }}
                </span>
                <span class="result-card-date" v-if="g.releaseDate">{{ formatDate(g.releaseDate) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-row" v-if="totalPages > 1">
        <button class="page-nav-btn" :disabled="page <= 1" @click="loadPage(page - 1)">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M10 3L5 8l5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          上一页
        </button>
        <div class="page-numbers">
          <button
            v-for="p in pageList"
            :key="p"
            class="page-num-btn"
            :class="{ 'page-num-btn--active': p === page }"
            @click="typeof p === 'number' && loadPage(p)"
          >{{ p }}</button>
        </div>
        <button class="page-nav-btn" :disabled="page >= totalPages" @click="loadPage(page + 1)">
          下一页
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M6 3l5 5-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { listGames, getGameSuggestions } from '@/api/index';

const SORT_OPTIONS = [
  { label: '综合排序', value: 'totalReviews' },
  { label: '最新发布', value: 'releaseDate' },
  { label: '价格从低到高', value: 'price' },
  { label: '好评数', value: 'rating' },
  { label: '名称 A-Z', value: 'name' }
];

export default {
  name: 'RefactorSearch',
  data() {
    return {
      keyword: '',
      results: [],
      defaultGames: [],
      suggestions: [],
      loading: false,
      searched: false,
      isFocused: false,
      showSuggestions: false,
      activeSuggestionIdx: -1,
      page: 1,
      pageSize: 20,
      totalResults: 0,
      totalPages: 1,
      sortBy: 'totalReviews',
      sortOptions: SORT_OPTIONS,
      totalGames: '',
      debounceTimer: null
    };
  },
  computed: {
    pageList() {
      const out = [];
      const max = this.totalPages;
      const current = this.page;
      if (max <= 7) {
        for (let i = 1; i <= max; i++) out.push(i);
        return out;
      }
      out.push(1);
      if (current > 3) out.push('…');
      for (let i = Math.max(2, current - 1); i <= Math.min(max - 1, current + 1); i++) out.push(i);
      if (current < max - 2) out.push('…');
      out.push(max);
      return out;
    }
  },
  async mounted() {
    await this.loadDefaultGames();
    document.addEventListener('keydown', this.onGlobalKeydown);
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.onGlobalKeydown);
  },
  methods: {
    onGlobalKeydown(e) {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        this.$refs.searchInput && this.$refs.searchInput.focus();
      }
    },
    async loadDefaultGames() {
      try {
        const { data: r } = await listGames(1, 12, '', 'totalReviews');
        if (r.code === 200) {
          this.defaultGames = r.data.records || [];
          this.totalGames = (r.data.total || 0).toLocaleString();
        }
      } catch (e) { /* ignore */ }
    },
    async search() {
      this.loading = true;
      this.searched = true;
      this.showSuggestions = false;
      try {
        const { data: r } = await listGames(this.page, this.pageSize, this.keyword.trim(), this.sortBy);
        if (r.code === 200) {
          this.results = r.data.records || [];
          this.totalResults = r.data.total || 0;
          this.totalPages = Math.max(1, Math.ceil(this.totalResults / this.pageSize));
        }
      } catch (e) {
        this.results = [];
        this.totalResults = 0;
      }
      this.loading = false;
    },
    async fetchSuggestions(query) {
      if (!query || query.trim().length < 1) {
        this.suggestions = [];
        this.showSuggestions = false;
        return;
      }
      try {
        const { data: r } = await getGameSuggestions(query.trim(), 8);
        if (r.code === 200 && Array.isArray(r.data)) {
          this.suggestions = r.data;
          this.showSuggestions = this.suggestions.length > 0 && this.isFocused;
          this.activeSuggestionIdx = -1;
        }
      } catch (e) {
        this.suggestions = [];
        this.showSuggestions = false;
      }
    },
    onInput() {
      clearTimeout(this.debounceTimer);
      this.debounceTimer = setTimeout(() => {
        this.fetchSuggestions(this.keyword);
      }, 200);
    },
    onFocus() {
      this.isFocused = true;
      if (this.suggestions.length > 0) {
        this.showSuggestions = true;
      } else if (this.keyword) {
        this.fetchSuggestions(this.keyword);
      }
    },
    onBlur() {
      setTimeout(() => {
        this.isFocused = false;
        this.showSuggestions = false;
      }, 200);
    },
    onKeydown(e) {
      if (this.showSuggestions && this.suggestions.length > 0) {
        if (e.key === 'ArrowDown') {
          e.preventDefault();
          this.activeSuggestionIdx = Math.min(this.activeSuggestionIdx + 1, this.suggestions.length - 1);
        } else if (e.key === 'ArrowUp') {
          e.preventDefault();
          this.activeSuggestionIdx = Math.max(this.activeSuggestionIdx - 1, -1);
        } else if (e.key === 'Enter') {
          if (this.activeSuggestionIdx >= 0) {
            e.preventDefault();
            this.selectSuggestion(this.suggestions[this.activeSuggestionIdx]);
          } else {
            this.page = 1;
            this.search();
          }
        } else if (e.key === 'Escape') {
          this.showSuggestions = false;
          this.$refs.searchInput.blur();
        }
      } else if (e.key === 'Enter') {
        this.page = 1;
        this.search();
      }
    },
    selectSuggestion(s) {
      this.keyword = s.gameNameCn || s.gameName;
      this.showSuggestions = false;
      this.page = 1;
      this.search();
    },
    clearSearch() {
      this.keyword = '';
      this.results = [];
      this.suggestions = [];
      this.showSuggestions = false;
      this.searched = false;
      this.page = 1;
      this.totalResults = 0;
      this.$refs.searchInput.focus();
    },
    changeSort(val) {
      if (this.sortBy === val) return;
      this.sortBy = val;
      this.page = 1;
      this.search();
    },
    loadPage(p) {
      this.page = p;
      this.search();
      this.$nextTick(() => {
        window.scrollTo({ top: 400, behavior: 'smooth' });
      });
    },
    goGame(id) {
      if (!id) return;
      this.$router.push(`/game/${id}`);
    },
    // --- helpers ---
    getTags(str) {
      if (!str) return [];
      return String(str).split(/[;；,]/).map(t => t.trim()).filter(Boolean);
    },
    getFirstTag(str) {
      const tags = this.getTags(str);
      return tags.length > 0 ? tags[0] : '';
    },
    cardColor(g) {
      const hash = ((g.gameId || 0) * 2654435761) >>> 0;
      const h1 = hash % 360;
      const h2 = (h1 + 40) % 360;
      return `linear-gradient(135deg, hsl(${h1},55%,45%) 0%, hsl(${h2},60%,38%) 100%)`;
    },
    suggestionColor(s) {
      const hash = ((s.gameId || 0) * 2654435761) >>> 0;
      const h = hash % 360;
      return `linear-gradient(135deg, hsl(${h},50%,48%) 0%, hsl(${(h+30)%360},55%,40%) 100%)`;
    },
    getGameInit(g) {
      const n = g.gameNameCn || g.gameName || '?';
      return n.trim().charAt(0).toUpperCase();
    },
    getSuggestionInit(s) {
      const n = s.gameNameCn || s.gameName || '?';
      return n.trim().charAt(0).toUpperCase();
    },
    metacriticLevel(score) {
      if (score >= 90) return 'mustplay';
      if (score >= 75) return 'positive';
      if (score >= 50) return 'mixed';
      return 'negative';
    },
    reviewClass(s) {
      if (!s) return 'rev-none';
      if (s.includes('好评') || s.includes('特别') || s.includes('Very') || s.includes('Positive')) return 'rev-good';
      if (s.includes('差评') || s.includes('Negative')) return 'rev-bad';
      return 'rev-mid';
    },
    formatDate(s) {
      if (!s) return '';
      const d = new Date(s);
      if (isNaN(d)) return '';
      const now = new Date();
      const diff = now - d;
      if (diff < 7 * 86400000) return '本周';
      if (diff < 30 * 86400000) return Math.ceil(diff / 86400000) + '天前';
      return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`;
    },
    onSuggestionImgError(e) {
      e.target.style.display = 'none';
      const fb = e.target.parentElement.querySelector('.suggestion-cover-fallback');
      if (fb) fb.style.display = 'flex';
    },
    onResultImgError(e, g) {
      e.target.style.display = 'none';
      const fb = e.target.parentElement.querySelector('.result-card-img-fallback');
      if (fb) fb.style.display = 'flex';
    }
  }
};
</script>

<style scoped>
/* ===== Search Hero（双主题令牌驱动） ===== */
.search-page { min-height: 100vh; }
.search-hero {
  position: relative;
  padding: var(--space-8) var(--space-8) var(--space-6);
  background: var(--banner-bg-gradient-2, var(--banner-bg-gradient));
  overflow: hidden;
  color: var(--banner-text-color);
}
.search-hero-bg {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background:
    radial-gradient(ellipse 600px 400px at 30% 20%, var(--color-brand-a10) 0%, transparent 70%),
    radial-gradient(ellipse 400px 500px at 80% 80%, var(--color-accent-a08) 0%, transparent 70%);
  pointer-events: none;
}
.search-hero-content {
  position: relative; z-index: 1;
  display: flex; flex-direction: column; align-items: center;
  max-width: 640px; margin: 0 auto;
}
.search-icon-big {
  width: 72px; height: 72px; border-radius: 20px;
  background: var(--color-brand-a12); color: var(--color-brand-400);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: var(--space-4); backdrop-filter: blur(8px);
  border: 1px solid var(--color-brand-a20);
}
.search-hero-title {
  font-size: 28px; font-weight: 700; color: var(--banner-text-color); margin: 0 0 8px;
  letter-spacing: 0.5px;
}
.search-hero-sub { font-size: 14px; color: var(--banner-eyebrow-color); margin: 0 0 var(--space-5); }

/* ===== Search Box ===== */
.search-box-wrapper { width: 100%; position: relative; }
.search-box {
  display: flex; align-items: center;
  background: var(--banner-card-bg); border: 1.5px solid var(--banner-card-border);
  border-radius: 14px; padding: 4px;
  transition: all 0.25s ease;
  backdrop-filter: blur(12px);
}
.search-box--focused {
  background: var(--banner-card-bg); border-color: var(--color-brand-a50);
  box-shadow: 0 0 0 3px var(--color-brand-a15);
}
.search-box--has-value { border-color: var(--color-brand-a35); }
.search-box-icon {
  flex-shrink: 0; color: var(--banner-eyebrow-color);
  margin: 0 4px 0 12px;
}
.search-box--focused .search-box-icon { color: var(--color-brand-400); }
.search-box-input {
  flex: 1; border: none; background: transparent; padding: 12px 8px;
  font-size: 15px; color: var(--banner-text-color); outline: none; min-width: 0;
}
.search-box-input::placeholder { color: var(--banner-eyebrow-color); opacity: 0.6; }
.search-box-clear {
  flex-shrink: 0; background: none; border: none; cursor: pointer;
  padding: 6px; margin-right: 4px; color: var(--banner-eyebrow-color);
  border-radius: 8px; display: flex; align-items: center;
  transition: all 0.15s;
}
.search-box-clear:hover { color: var(--banner-text-color); background: var(--banner-tag-bg); }
.search-box-shortcut {
  flex-shrink: 0; display: flex; align-items: center; gap: 3px;
  padding: 4px 10px; margin-right: 8px; border-radius: 8px;
  background: var(--banner-tag-bg); font-size: 11px; color: var(--banner-eyebrow-color);
}
.search-box-shortcut kbd {
  font-family: inherit; font-size: 11px; padding: 1px 4px;
  background: var(--banner-tag-bg); border-radius: 4px; color: var(--banner-tag-color);
}

/* ===== Suggestions Dropdown ===== */
.suggestions-dropdown {
  position: absolute; top: calc(100% + 6px); left: 0; right: 0;
  background: var(--color-bg-elevated); border: 1px solid var(--color-border-default);
  border-radius: 12px; overflow: hidden; z-index: 100;
  box-shadow: var(--shadow-xl);
}
.suggestions-header {
  padding: 10px 16px; font-size: 11px; color: var(--color-text-tertiary);
  text-transform: uppercase; letter-spacing: 0.5px;
  border-bottom: 1px solid var(--color-border-muted);
}
.suggestion-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 16px; cursor: pointer; transition: background 0.12s;
}
.suggestion-item:hover, .suggestion-item--active { background: var(--color-brand-a10); }
.suggestion-cover {
  width: 48px; height: 30px; border-radius: 4px; overflow: hidden;
  flex-shrink: 0; position: relative; background: var(--color-bg-subtle);
}
.suggestion-cover-img {
  width: 100%; height: 100%; object-fit: cover; display: block;
  position: absolute; top: 0; left: 0;
}
.suggestion-cover-fallback {
  width: 100%; height: 100%; display: flex; align-items: center;
  justify-content: center; color: #fff; font-weight: 700; font-size: 13px;
}
.suggestion-info { flex: 1; min-width: 0; }
.suggestion-name {
  font-size: 13px; color: var(--color-text-primary); font-weight: 500;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.suggestion-meta { font-size: 11px; color: var(--color-text-tertiary); margin-top: 2px; }
.suggestion-reviews { margin-left: 8px; }
.suggestion-arrow { color: var(--color-text-tertiary); font-size: 14px; }
.dropdown-fade-enter-active { transition: all 0.18s ease-out; }
.dropdown-fade-leave-active { transition: all 0.12s ease-in; }
.dropdown-fade-enter-from, .dropdown-fade-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== Results Section ===== */
.search-results-section { padding: var(--space-6) var(--space-8); }
.filter-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--space-5); flex-wrap: wrap; gap: 12px;
}
.filter-left { }
.filter-count { font-size: 14px; color: var(--color-text-secondary); }
.filter-count strong { color: var(--color-text-primary); }
.filter-count em { font-style: normal; color: var(--color-text-link); }
.filter-right { display: flex; align-items: center; gap: 4px; }
.filter-label { font-size: 12px; color: var(--color-text-tertiary); margin-right: 4px; }
.filter-sort-btn {
  padding: 5px 12px; border: 1px solid var(--color-border-muted);
  border-radius: 6px; background: var(--color-bg-elevated);
  color: var(--color-text-tertiary); font-size: 12px;
  cursor: pointer; transition: all 0.15s;
}
.filter-sort-btn:hover { color: var(--color-text-secondary); border-color: var(--color-border-default); }
.filter-sort-btn--active {
  color: var(--color-text-link); border-color: var(--color-brand-a40);
  background: var(--color-brand-a10);
}

/* ===== Results Grid ===== */
.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-5);
  margin-bottom: var(--space-6);
}
.result-card {
  background: var(--color-bg-elevated); border: 1px solid var(--color-border-muted);
  border-radius: 12px; overflow: hidden; cursor: pointer;
  transition: all 0.22s ease;
}
.result-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-border-default);
}
.result-card:focus-visible {
  outline: 2px solid var(--color-text-link); outline-offset: 2px;
}
.result-card-img-wrap {
  position: relative; width: 100%; aspect-ratio: 460/215;
  overflow: hidden; background: var(--color-bg-subtle);
}
.result-card-img {
  width: 100%; height: 100%; object-fit: cover; display: block;
}
.result-card-img-fallback {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  display: none; align-items: center; justify-content: center;
}
.img-fallback-letter { font-size: 2.4rem; font-weight: 700; color: rgba(255,255,255,0.5); }
.result-card-price {
  position: absolute; top: 10px; left: 10px;
  padding: 3px 10px; border-radius: 6px; background: rgba(0,0,0,0.65);
  color: var(--color-mc-mustplay); font-size: 12px; font-weight: 600;
  backdrop-filter: blur(6px);
}
.result-card-price.price-free { color: var(--color-mc-positive); }
.result-card-score {
  position: absolute; top: 10px; right: 10px;
  min-width: 32px; height: 24px; border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; backdrop-filter: blur(6px);
}
.mc-mustplay { background: var(--color-mc-mustplay-bg); color: var(--color-mc-mustplay); }
.mc-positive { background: var(--color-mc-positive-bg); color: var(--color-mc-positive); }
.mc-mixed { background: var(--color-mc-mixed-bg); color: var(--color-mc-mixed); }
.mc-negative { background: var(--color-mc-negative-bg); color: var(--color-mc-negative); }

.result-card-body { padding: 14px 16px 16px; }
.result-card-title {
  font-size: 15px; font-weight: 600; color: var(--color-text-primary);
  margin: 0 0 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.result-card-genres { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
.genre-badge {
  padding: 2px 8px; border-radius: 4px; font-size: 11px;
  background: var(--color-brand-a08); color: var(--color-brand-400);
  border: 1px solid var(--color-brand-a12);
}
.result-card-desc {
  font-size: 12px; color: var(--color-text-secondary);
  line-height: 1.5; margin-bottom: 10px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
}
.result-card-desc--fallback { color: var(--color-text-tertiary); }
.result-card-footer {
  display: flex; align-items: center; justify-content: space-between;
}
.result-card-review {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; font-weight: 600;
}
.review-dot { width: 6px; height: 6px; border-radius: 50%; }
.rev-good { color: var(--color-success); }
.rev-good .review-dot { background: var(--color-success); }
.rev-mid { color: var(--color-warning); }
.rev-mid .review-dot { background: var(--color-warning); }
.rev-bad { color: var(--color-danger); }
.rev-bad .review-dot { background: var(--color-danger); }
.rev-none { color: var(--color-text-tertiary); }
.rev-none .review-dot { background: var(--color-text-tertiary); }
.result-card-date { font-size: 11px; color: var(--color-text-tertiary); }

/* ===== Skeleton ===== */
.result-card--skeleton { cursor: default; pointer-events: none; }
.result-card--skeleton:hover { transform: none; box-shadow: none; }
.skeleton-img {
  width: 100%; aspect-ratio: 460/215;
  background: linear-gradient(90deg, var(--color-bg-subtle) 25%, var(--color-bg-elevated) 50%, var(--color-bg-subtle) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.skeleton-body { padding: 14px 16px 16px; }
.skeleton-line {
  height: 12px; border-radius: 4px; margin-bottom: 8px;
  background: linear-gradient(90deg, var(--color-bg-subtle) 25%, var(--color-bg-elevated) 50%, var(--color-bg-subtle) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.skeleton-line--title { height: 16px; width: 70%; }
.skeleton-line--short { width: 40%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* ===== Empty ===== */
.empty-result {
  text-align: center; padding: var(--space-10);
}
.empty-icon { color: var(--color-text-tertiary); margin-bottom: var(--space-4); }
.empty-result h3 { font-size: 16px; color: var(--color-text-secondary); margin: 0 0 8px; }
.empty-result p { font-size: 13px; color: var(--color-text-tertiary); margin: 0; }

/* ===== Section Header ===== */
.section-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--space-4);
}
.section-title { font-size: 18px; font-weight: 600; color: var(--color-text-primary); margin: 0; }
.section-meta { font-size: 12px; color: var(--color-text-tertiary); }

/* ===== Pagination ===== */
.pagination-row {
  display: flex; align-items: center; justify-content: center;
  gap: 8px; padding: var(--space-4) 0;
}
.page-nav-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 8px 16px; border: 1px solid var(--color-border-muted);
  border-radius: 8px; background: var(--color-bg-elevated);
  color: var(--color-text-secondary); font-size: 13px;
  cursor: pointer; transition: all 0.15s;
}
.page-nav-btn:hover:not(:disabled) { border-color: var(--color-text-link); color: var(--color-text-link); }
.page-nav-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.page-numbers { display: flex; gap: 4px; }
.page-num-btn {
  min-width: 36px; height: 36px; border: 1px solid var(--color-border-muted);
  border-radius: 8px; background: var(--color-bg-elevated);
  color: var(--color-text-secondary); font-size: 13px;
  cursor: pointer; transition: all 0.15s; display: flex; align-items: center; justify-content: center;
}
.page-num-btn:hover { border-color: var(--color-text-link); color: var(--color-text-link); }
.page-num-btn--active {
  background: var(--color-text-link); color: var(--color-text-inverse); border-color: var(--color-text-link);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .search-hero { padding: var(--space-5) var(--space-4) var(--space-4); }
  .search-hero-title { font-size: 22px; }
  .search-results-section { padding: var(--space-4); }
  .results-grid { grid-template-columns: 1fr; }
  .filter-bar { flex-direction: column; align-items: flex-start; }
  .search-box-shortcut { display: none; }
}
@media (max-width: 480px) {
  .search-hero-title { font-size: 18px; }
  .search-icon-big { width: 52px; height: 52px; border-radius: 14px; }
  .search-icon-big svg { width: 24px; height: 24px; }
}
</style>
