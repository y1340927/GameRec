<template>
  <div
    class="game-row"
    :class="{ 'game-row--clickable': !!$listeners.click }"
    :style="{ animationDelay: animationDelay + 'ms' }"
    @click="$emit('click', game)"
    role="option"
    :aria-label="'游戏: ' + (game.gameNameCn || game.gameName)"
  >
    <div class="game-rank" v-if="rank !== null && rank !== undefined">
      <span class="rank-badge" :class="rankClass">{{ rank }}</span>
    </div>
    <div class="game-info">
      <div class="game-name-row">
        <span class="game-name" :title="game.gameName">{{ game.gameName }}</span>
        <span v-if="game.gameNameCn" class="game-name-cn">{{ game.gameNameCn }}</span>
      </div>
      <div class="game-meta" v-if="showTags || showMeta">
        <span v-if="showTags && game.genres" class="game-tags">
          <span v-for="tag in getTags(game.genres)" :key="tag" class="game-tag">{{ tag }}</span>
        </span>
        <span v-if="showMeta && (game.purchaseCount || game.playCount)" class="game-stats">
          <span v-if="game.purchaseCount" class="stat-item">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M6 1.5v9M1.5 6h9" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
            购买 {{ formatNum(game.purchaseCount) }}
          </span>
          <span v-if="game.playCount" class="stat-item">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><polygon points="2,1 10,6 2,11" fill="currentColor"/></svg>
            游玩 {{ formatNum(game.playCount) }}
          </span>
        </span>
      </div>
    </div>
    <div class="game-score" v-if="showScore">
      <div class="score-bar">
        <div class="score-fill" :style="{ width: scorePercent + '%' }"></div>
      </div>
      <span class="score-label">{{ scoreLabel || (scorePercent + '%') }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GameRow',
  props: {
    game:           { type: Object, required: true },
    rank:           { type: Number, default: null },
    animationDelay: { type: Number, default: 0 },
    showTags:       { type: Boolean, default: true },
    showMeta:       { type: Boolean, default: true },
    showScore:      { type: Boolean, default: false },
    scorePercent:   { type: Number, default: 0 },
    scoreLabel:     { type: String, default: '' }
  },
  computed: {
    rankClass() {
      if (this.rank === 1) return 'rank-gold';
      if (this.rank === 2) return 'rank-silver';
      if (this.rank === 3) return 'rank-bronze';
      return '';
    }
  },
  methods: {
    getTags(tags) {
      if (!tags) return [];
      if (Array.isArray(tags)) return tags.slice(0, 3);
      return String(tags).split(',').slice(0, 3).map(t => t.trim());
    },
    formatNum(n) {
      if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿';
      if (n >= 10000) return (n / 10000).toFixed(1) + '万';
      return n.toLocaleString();
    }
  }
};
</script>

<style scoped>
.game-row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  transition: all var(--duration-fast) var(--ease-out);
  animation: row-in 0.4s var(--ease-out-expo) both;
  animation-delay: inherit;
}
.game-row--clickable { cursor: pointer; }
.game-row--clickable:hover { background: var(--color-bg-subtle); }
@keyframes row-in {
  from { opacity: 0; transform: translateX(-8px); }
  to   { opacity: 1; transform: translateX(0); }
}

.game-rank { flex-shrink: 0; width: 28px; text-align: center; }
.rank-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px;
  border-radius: var(--radius-sm);
  font-size: var(--text-2xs);
  font-weight: var(--font-weight-bold);
  background: var(--color-bg-subtle);
  color: var(--color-text-tertiary);
}
.rank-gold   { background: rgba(245,158,11,0.15); color: var(--color-warm-400); }
.rank-silver { background: rgba(161,161,170,0.15); color: var(--color-text-secondary); }
.rank-bronze { background: rgba(251,146,60,0.15); color: #fb923c; }

.game-info { flex: 1; min-width: 0; }
.game-name-row { display: flex; align-items: center; gap: var(--space-2); }
.game-name {
  font-size: var(--text-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.game-name-cn {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.game-meta { display: flex; align-items: center; gap: var(--space-3); margin-top: var(--space-1); }
.game-tags { display: flex; gap: var(--space-1); }
.game-tag {
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--color-bg-subtle);
  color: var(--color-text-tertiary);
  font-size: var(--text-2xs);
}
.game-stats { display: flex; gap: var(--space-3); }
.stat-item {
  display: flex; align-items: center; gap: var(--space-1);
  font-size: var(--text-2xs);
  color: var(--color-text-tertiary);
}

.game-score { display: flex; align-items: center; gap: var(--space-2); flex-shrink: 0; }
.score-bar {
  width: 80px; height: 4px;
  border-radius: var(--radius-full);
  background: var(--color-bg-subtle);
  overflow: hidden;
}
.score-fill {
  height: 100%;
  border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--color-brand-500), var(--color-accent-500));
  transition: width 0.6s var(--ease-out-expo);
}
.score-label {
  font-size: var(--text-xs);
  font-family: var(--font-family-mono);
  color: var(--color-text-secondary);
}

@media (max-width: 640px) {
  .game-tags,.game-meta { display: none; }
  .game-name-cn { display: none; }
  .score-bar { width: 50px; }
}
</style>
