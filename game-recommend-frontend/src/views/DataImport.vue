<template>
  <div class="page-import">
    <page-header title="数据管理" description="数据导入 · 矩阵构建 · 画像生成 · 模型训练" />

    <!-- Tab 切换 -->
    <div class="tab-bar" role="tablist" aria-label="数据管理功能">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
        role="tab"
        :aria-selected="activeTab === tab.key"
      >
        <span v-html="tab.icon" aria-hidden="true"></span>
        <span class="tab-label">{{ tab.label }}</span>
      </button>
    </div>

    <!-- 用户导入 -->
    <panel v-if="activeTab === 'user'" title="导入用户">
      <div class="form-grid">
        <div class="form-group">
          <label for="user-id" class="form-label">用户ID</label>
          <input id="user-id" v-model.number="userForm.userId" type="number" class="form-input" placeholder="输入用户ID"/>
        </div>
        <div class="form-group">
          <label for="user-purchase" class="form-label">购买数</label>
          <input id="user-purchase" v-model.number="userForm.purchaseCount" type="number" class="form-input" min="0"/>
        </div>
        <div class="form-group">
          <label for="user-play" class="form-label">游玩数</label>
          <input id="user-play" v-model.number="userForm.playCount" type="number" class="form-input" min="0"/>
        </div>
        <div class="form-group">
          <label for="user-hours" class="form-label">总游玩时长 (h)</label>
          <input id="user-hours" v-model.number="userForm.totalPlayHours" type="number" class="form-input" min="0" step="0.1"/>
        </div>
      </div>
      <div class="form-actions">
        <button class="btn btn-primary" @click="importUser" :disabled="importing">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M8 2v10M4 8l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          {{ importing ? '导入中...' : '导入用户' }}
        </button>
      </div>
    </panel>

    <!-- 游戏导入 -->
    <panel v-if="activeTab === 'game'" title="导入游戏">
      <div class="form-grid">
        <div class="form-group">
          <label for="game-id" class="form-label">游戏ID</label>
          <input id="game-id" v-model.number="gameForm.gameId" type="number" class="form-input" placeholder="输入游戏ID"/>
        </div>
        <div class="form-group">
          <label for="game-name" class="form-label">游戏名称</label>
          <input id="game-name" v-model="gameForm.gameName" type="text" class="form-input" placeholder="英文名称"/>
        </div>
        <div class="form-group">
          <label for="game-name-cn" class="form-label">中文名</label>
          <input id="game-name-cn" v-model="gameForm.gameNameCn" type="text" class="form-input" placeholder="中文名称"/>
        </div>
        <div class="form-group">
          <label for="game-dev" class="form-label">开发商</label>
          <input id="game-dev" v-model="gameForm.developer" type="text" class="form-input" placeholder="开发商"/>
        </div>
        <div class="form-group">
          <label for="game-genres" class="form-label">游戏类型</label>
          <input id="game-genres" v-model="gameForm.genres" type="text" class="form-input" placeholder="分号分隔，如 Action;RPG"/>
        </div>
        <div class="form-group">
          <label for="game-price" class="form-label">价格 ($)</label>
          <input id="game-price" v-model.number="gameForm.price" type="number" class="form-input" min="0" step="0.01"/>
        </div>
      </div>
      <div class="form-actions">
        <button class="btn btn-primary" @click="importGame" :disabled="importing">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M8 2v10M4 8l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          {{ importing ? '导入中...' : '导入游戏' }}
        </button>
      </div>
    </panel>

    <!-- 评分导入 -->
    <panel v-if="activeTab === 'rating'" title="导入评分">
      <div class="form-grid form-grid--3">
        <div class="form-group">
          <label for="rating-user" class="form-label">用户ID</label>
          <input id="rating-user" v-model.number="ratingForm.userId" type="number" class="form-input"/>
        </div>
        <div class="form-group">
          <label for="rating-game" class="form-label">游戏ID</label>
          <input id="rating-game" v-model.number="ratingForm.gameId" type="number" class="form-input"/>
        </div>
        <div class="form-group">
          <label for="rating-hours" class="form-label">游玩时长 (h)</label>
          <input id="rating-hours" v-model.number="ratingForm.playHours" type="number" class="form-input" min="0" step="0.1"/>
        </div>
      </div>
      <div class="form-actions">
        <button class="btn btn-primary" @click="importRating" :disabled="importing">
          {{ importing ? '导入中...' : '导入评分' }}
        </button>
      </div>
    </panel>

    <!-- 批量操作 -->
    <panel v-if="activeTab === 'batch'" title="批量操作">
      <div class="batch-grid">
        <div class="batch-card" @click="buildMatrix" role="button" tabindex="0" @keydown.enter="buildMatrix" @keydown.space.prevent="buildMatrix">
          <div class="batch-card-icon batch-card-icon--brand" aria-hidden="true">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <rect x="3" y="3" width="7" height="7" rx="1" stroke="currentColor" stroke-width="1.5"/>
              <rect x="14" y="3" width="7" height="7" rx="1" stroke="currentColor" stroke-width="1.5"/>
              <rect x="3" y="14" width="7" height="7" rx="1" stroke="currentColor" stroke-width="1.5"/>
              <rect x="14" y="14" width="7" height="7" rx="1" stroke="currentColor" stroke-width="1.5"/>
            </svg>
          </div>
          <div class="batch-card-title">构建评分矩阵</div>
          <div class="batch-card-desc">{{ matrixDesc }}</div>
          <div class="batch-card-status" v-if="building" role="status"><span class="spinner" aria-hidden="true"></span> 处理中... 预计30s-2min</div>
        </div>

        <div class="batch-card" @click="saveProfiles" role="button" tabindex="0" @keydown.enter="saveProfiles" @keydown.space.prevent="saveProfiles">
          <div class="batch-card-icon batch-card-icon--warm" aria-hidden="true">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <circle cx="9" cy="7" r="4" stroke="currentColor" stroke-width="1.5"/>
              <path d="M3 21c0-3.314 2.686-6 6-6s6 2.686 6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <path d="M19 8v6M16 11h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="batch-card-title">批量生成画像</div>
          <div class="batch-card-desc">全量用户 RFM + 兴趣标签计算</div>
          <div class="batch-card-status" v-if="saving" role="status"><span class="spinner" aria-hidden="true"></span> 处理中...</div>
        </div>

        <div class="batch-card" @click="trainSVDModel" role="button" tabindex="0" @keydown.enter="trainSVDModel" @keydown.space.prevent="trainSVDModel">
          <div class="batch-card-icon batch-card-icon--accent" aria-hidden="true">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="batch-card-title">训练 SVD 模型</div>
          <div class="batch-card-desc">潜在因子: 50 · 迭代: 20 · LR: 0.005</div>
          <div class="batch-card-status" v-if="training" role="status"><span class="spinner" aria-hidden="true"></span> 训练中...</div>
        </div>
      </div>
    </panel>
  </div>
</template>

<script>
import { importUsers, importGames, importRatings, buildMatrix, saveAllProfiles, trainSVD, getOverview } from '@/api/index';
import PageHeader from '@/components/PageHeader.vue';
import Panel from '@/components/Panel.vue';

export default {
  name: 'DataImport',
  components: { PageHeader, Panel },
  data() {
    return {
      activeTab: 'user',
      importing: false, building: false, saving: false, training: false,
      tabs: [
        { key: 'user', label: '用户导入', icon: '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="6" cy="4.5" r="2.5" stroke="currentColor" stroke-width="1.5"/><path d="M2 14c0-2.21 1.79-4 4-4s4 1.79 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' },
        { key: 'game', label: '游戏导入', icon: '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="1.5" y="2.5" width="13" height="9" rx="1.5" stroke="currentColor" stroke-width="1.5"/><path d="M5 14h6M8 11.5V14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>' },
        { key: 'rating', label: '评分导入', icon: '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 1.5l1.87 3.974L14 5.57l-3 2.987.733 4.243L8 11.01l-3.733 1.79L5 8.557 2 5.57l4.13-.596L8 1.5z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>' },
        { key: 'batch', label: '批量操作', icon: '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="2" y="2" width="5" height="5" rx="0.5" stroke="currentColor" stroke-width="1.5"/><rect x="9" y="2" width="5" height="5" rx="0.5" stroke="currentColor" stroke-width="1.5"/><rect x="2" y="9" width="5" height="5" rx="0.5" stroke="currentColor" stroke-width="1.5"/><rect x="9" y="9" width="5" height="5" rx="0.5" stroke="currentColor" stroke-width="1.5"/></svg>' }
      ],
      userForm: { userId: null, purchaseCount: 0, playCount: 0, totalPlayHours: 0 },
      gameForm: { gameId: null, gameName: '', gameNameCn: '', developer: '', genres: '', price: 0 },
      ratingForm: { userId: null, gameId: null, playHours: 0 },
      dbStats: { userCount: 0, gameCount: 0, ratingCount: 0 }
    };
  },
  computed: {
    matrixDesc() {
      if (!this.dbStats.userCount && !this.dbStats.gameCount) return '暂无数据（请先启动后端服务）';
      const users = (this.dbStats.userCount || 0).toLocaleString();
      const games = (this.dbStats.gameCount || 0).toLocaleString();
      const ratings = this.dbStats.ratingCount || 0;
      const ratingsStr = ratings >= 10000 ? (ratings / 10000).toFixed(1) + '万' : ratings.toLocaleString();
      return `${users}用户 × ${games}游戏 (~${ratingsStr}评分)`;
    }
  },
  mounted() {
    this.loadOverview();
  },
  methods: {
    async importUser() {
      if (!this.userForm.userId) { this.$message.warning('请输入用户ID'); return; }
      this.importing = true;
      try {
        const { data } = await importUsers([{ ...this.userForm }]);
        if (data.code === 200) this.$message.success(`导入完成: 新增${data.data.inserted}, 更新${data.data.updated}`);
      } catch (e) { this.$message.error('导入失败'); }
      finally { this.importing = false; }
    },
    async importGame() {
      if (!this.gameForm.gameId) { this.$message.warning('请输入游戏ID'); return; }
      this.importing = true;
      try {
        const { data } = await importGames([{ ...this.gameForm }]);
        if (data.code === 200) this.$message.success(`导入完成: 新增${data.data.inserted}, 更新${data.data.updated}`);
      } catch (e) { this.$message.error('导入失败'); }
      finally { this.importing = false; }
    },
    async importRating() {
      if (!this.ratingForm.userId || !this.ratingForm.gameId) { this.$message.warning('请输入用户ID和游戏ID'); return; }
      this.importing = true;
      try {
        const { data } = await importRatings([{ ...this.ratingForm }]);
        if (data.code === 200) this.$message.success(`导入完成: 新增${data.data.inserted}, 更新${data.data.updated}`);
      } catch (e) { this.$message.error('导入失败'); }
      finally { this.importing = false; }
    },
    async loadOverview() {
      try {
        const { data } = await getOverview();
        if (data.code === 200) this.dbStats = data.data;
      } catch (e) { /* 静默加载失败，使用默认值 */ }
    },
    async buildMatrix() {
      this.building = true;
      try {
        const { data } = await buildMatrix();
        if (data.code === 200) this.$message.success(`矩阵构建完成: ${data.data.userCount}用户 × ${data.data.gameCount}游戏, 稀疏度${data.data.sparsity}`);
      } catch (e) { this.$message.error('构建失败'); }
      finally { this.building = false; }
    },
    async saveProfiles() {
      this.saving = true;
      try {
        const { data } = await saveAllProfiles();
        if (data.code === 200) this.$message.success(`画像生成完成: ${data.data.savedCount} 条`);
      } catch (e) { this.$message.error('生成失败'); }
      finally { this.saving = false; }
    },
    async trainSVDModel() {
      this.training = true;
      try {
        const { data } = await trainSVD(50, 20, 0.005, 0.02);
        if (data.code === 200) this.$message.success(`SVD训练完成: finalLoss=${data.data.finalLoss}, 耗时${data.data.elapsedMs}ms`);
      } catch (e) { this.$message.error('训练失败'); }
      finally { this.training = false; }
    }
  }
};
</script>

<style scoped>
.page-import { max-width: var(--content-max-width); margin: 0 auto; }

/* Tab Bar */
.tab-bar { display: flex; gap: var(--space-1); padding: var(--space-1); background: var(--color-bg-elevated); border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl); margin-bottom: var(--space-6); }
.tab-btn {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: var(--space-2);
  padding: var(--space-3) var(--space-4); border: none; border-radius: var(--radius-lg);
  background: transparent; color: var(--color-text-tertiary); font-size: var(--text-sm); font-weight: var(--font-weight-medium);
  cursor: pointer; transition: all var(--duration-fast) var(--ease-out-expo);
}
.tab-btn:hover { color: var(--color-text-secondary); }
.tab-btn.active { background: var(--color-bg-subtle); color: var(--color-text-primary); box-shadow: var(--shadow-sm); }

/* Form */
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-4); padding: var(--space-5); }
.form-grid--3 { grid-template-columns: 1fr 1fr 1fr; }
.form-group { display: flex; flex-direction: column; gap: var(--space-2); }
.form-label { font-size: var(--text-xs); color: var(--color-text-secondary); font-weight: var(--font-weight-medium); }
.form-input {
  padding: 10px 12px; border: 1px solid var(--color-border-default); border-radius: var(--radius-lg);
  background: var(--color-bg-subtle); color: var(--color-text-primary); font-size: var(--text-sm);
  transition: border-color var(--duration-fast);
}
.form-input:focus { outline: none; border-color: var(--color-brand-500); box-shadow: 0 0 0 3px rgba(92,124,250,0.15); }
.form-input::placeholder { color: var(--color-text-tertiary); }

.form-actions { padding: 0 var(--space-5) var(--space-5); }

.btn { display: inline-flex; align-items: center; gap: var(--space-2); padding: 10px 16px; border: none; border-radius: var(--radius-lg); font-size: var(--text-sm); font-weight: var(--font-weight-medium); cursor: pointer; transition: all var(--duration-fast); white-space: nowrap; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: var(--color-brand-600); color: #fff; }
.btn-primary:hover:not(:disabled) { background: var(--color-brand-500); }

/* Batch Cards */
.batch-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-4); padding: var(--space-5); }
.batch-card {
  background: var(--color-bg-subtle); border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl);
  padding: var(--space-6); text-align: center; cursor: pointer; transition: all var(--duration-normal) var(--ease-out-expo);
  outline-offset: -2px;
}
.batch-card:hover { border-color: var(--color-border-accent); transform: translateY(-2px); box-shadow: var(--shadow-glow-brand); }
.batch-card:active { transform: scale(0.97); }
.batch-card-icon { display: flex; align-items: center; justify-content: center; width: 48px; height: 48px; margin: 0 auto var(--space-4); border-radius: var(--radius-lg); }
.batch-card-icon--brand { background: rgba(92,124,250,0.1); color: var(--color-brand-500); }
.batch-card-icon--warm { background: rgba(252,196,25,0.1); color: var(--color-warm-500); }
.batch-card-icon--accent { background: rgba(34,184,207,0.1); color: var(--color-accent-400); }
.batch-card-title { font-size: var(--text-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: var(--space-1); }
.batch-card-desc { font-size: var(--text-xs); color: var(--color-text-tertiary); }
.batch-card-status { margin-top: var(--space-3); font-size: var(--text-xs); color: var(--color-brand-400); display: flex; align-items: center; justify-content: center; gap: var(--space-2); }

.spinner { width: 12px; height: 12px; border: 2px solid var(--color-border-default); border-top-color: var(--color-brand-500); border-radius: 50%; animation: spin 0.6s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .form-grid, .form-grid--3 { grid-template-columns: 1fr; }
  .batch-grid { grid-template-columns: 1fr; }
  .tab-bar { flex-wrap: wrap; }
  .tab-btn { flex: 1 1 45%; }
  .tab-label { font-size: var(--text-xs); }
}
</style>
