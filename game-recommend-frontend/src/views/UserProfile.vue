<template>
  <div class="page-profile">
    <page-header title="玩家画像" description="RFM 价值分析 · 活跃度评估 · 兴趣标签" />

    <!-- 查询栏 -->
    <query-bar>
      <div class="query-input-wrap">
        <svg class="query-input-icon" width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
          <circle cx="9" cy="5.25" r="3" stroke="currentColor" stroke-width="1.5"/>
          <path d="M3 15.75c0-3.314 2.686-6 6-6s6 2.686 6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        <input
          v-model="userId"
          type="number"
          placeholder="输入用户ID (1-56789)"
          class="query-input"
          aria-label="用户ID"
          @keyup.enter="loadProfile"
        />
      </div>
      <div class="btn-group">
        <button class="btn btn-primary" @click="loadProfile" :disabled="loading">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="6" cy="6" r="4.25" stroke="currentColor" stroke-width="1.5"/>
            <path d="M9.25 9.25L14.25 14.25" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span class="btn-text">{{ loading ? '查询中...' : '查询画像' }}</span>
        </button>
        <button class="btn btn-ghost" @click="goRecommend" :disabled="!userId">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M8 1l1.87 3.974L14 5.57l-3 2.987.733 4.243L8 11.01l-3.733 1.79L5 8.557 2 5.57l4.13-.596L8 1z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
          </svg>
          <span class="btn-text">查看推荐</span>
        </button>
        <button class="btn btn-accent" @click="randomUser" :disabled="loading">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M8 2v2M8 12v2M2 8h2M12 8h2M4.93 4.93l1.41 1.41M9.66 9.66l1.41 1.41M4.93 11.07l1.41-1.41M9.66 6.34l1.41-1.41" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span class="btn-text">随机玩家</span>
        </button>
      </div>
    </query-bar>

    <!-- ===== 默认状态：系统总览 ===== -->
    <template v-if="!profile && !loading && !profileLoaded">
      <!-- 欢迎区 -->
      <div class="welcome-card">
        <div class="welcome-glow"></div>
        <div class="welcome-grid welcome-grid--1"></div>
        <div class="welcome-grid welcome-grid--2"></div>
        <div class="welcome-content">
          <div class="welcome-icon">
            <svg width="40" height="40" viewBox="0 0 40 40" fill="none" aria-hidden="true">
              <circle cx="16" cy="14" r="8" stroke="currentColor" stroke-width="1.5"/>
              <path d="M6 36c0-5.523 4.477-10 10-10s10 4.477 10 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <circle cx="32" cy="10" r="4" fill="var(--color-brand-500)" opacity="0.3"/>
              <path d="M32 8v4M30 10h4" stroke="var(--color-brand-400)" stroke-width="1.2" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="welcome-text">
            <h3 class="welcome-title">玩家画像分析</h3>
            <p class="welcome-desc">输入用户ID查看详细画像，包含 RFM 价值分层、活跃度评估和兴趣标签云</p>
          </div>
          <div class="welcome-actions">
            <button class="welcome-btn" @click="randomUser">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M7 1v2M7 11v2M1 7h2M11 7h2M4.11 4.11l1.42 1.42M8.47 8.47l1.42 1.42M4.11 9.89l1.42-1.42M8.47 5.53l1.42-1.42" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              随机查看一位玩家
            </button>
            <span class="welcome-or">或</span>
            <div class="sample-users">
              <span class="sample-label">试试这些：</span>
              <button v-for="id in sampleIds" :key="id" class="sample-chip" @click="userId = String(id); loadProfile()">
                #{{ id }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 系统统计 -->
      <div class="section-label">系统玩家概况</div>
      <div class="overview-stats" v-if="sysOverview">
        <div class="overview-item">
          <div class="overview-item-icon overview-icon--primary">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="8" cy="6" r="4" stroke="currentColor" stroke-width="1.5"/><path d="M3 18c0-2.761 2.239-5 5-5s5 2.239 5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
          <div class="overview-item-info">
            <span class="overview-item-val">{{ sysOverview.userCount }}</span>
            <span class="overview-item-label">注册玩家</span>
          </div>
        </div>
        <div class="overview-item">
          <div class="overview-item-icon overview-icon--cyan">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><rect x="2" y="3" width="16" height="12" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M7 18h6M10 14v4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
          <div class="overview-item-info">
            <span class="overview-item-val">{{ sysOverview.gameCount }}</span>
            <span class="overview-item-label">游戏库</span>
          </div>
        </div>
        <div class="overview-item">
          <div class="overview-item-icon overview-icon--gold">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M10 2l2.09 4.472 4.66.632-3.375 3.362.825 4.784L10 12.952l-4.2 2.296.825-4.784L2.25 7.104l4.66-.632L10 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
          </div>
          <div class="overview-item-info">
            <span class="overview-item-val">{{ sysOverview.ratingCount }}</span>
            <span class="overview-item-label">评分记录</span>
          </div>
        </div>
        <div class="overview-item">
          <div class="overview-item-icon overview-icon--green">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M7 10l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><rect x="2" y="2" width="16" height="16" rx="3" stroke="currentColor" stroke-width="1.5"/></svg>
          </div>
          <div class="overview-item-info">
            <span class="overview-item-val">{{ sysOverview.avgRating }}</span>
            <span class="overview-item-label">平均评分</span>
          </div>
        </div>
      </div>

      <!-- 画像分析维度预览 -->
      <div class="preview-grid">
        <div class="preview-card">
          <div class="preview-card-icon preview-icon--primary">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M10 2l2.09 4.472 4.66.632-3.375 3.362.825 4.784L10 12.952l-4.2 2.296.825-4.784L2.25 7.104l4.66-.632L10 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
          </div>
          <div class="preview-card-content">
            <h4 class="preview-card-title">RFM 价值分层</h4>
            <p class="preview-card-desc">基于最近游玩时间（Recency）、频率（Frequency）、评分（Monetary）三维评估玩家价值</p>
            <div class="preview-dimensions">
              <span class="preview-dim">R 最近游玩</span>
              <span class="preview-dim">F 游玩频率</span>
              <span class="preview-dim">M 平均评分</span>
            </div>
          </div>
        </div>
        <div class="preview-card">
          <div class="preview-card-icon preview-icon--cyan">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M10 5v5l3 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="preview-card-content">
            <h4 class="preview-card-title">活跃度评估</h4>
            <p class="preview-card-desc">基于时间衰减模型评估玩家活跃程度，区分高热/中热/低热/沉默用户</p>
            <div class="preview-dimensions">
              <span class="preview-dim preview-dim--cyan">高热玩家</span>
              <span class="preview-dim preview-dim--cyan">中热玩家</span>
              <span class="preview-dim preview-dim--cyan">低热玩家</span>
              <span class="preview-dim preview-dim--dim">沉默用户</span>
            </div>
          </div>
        </div>
        <div class="preview-card">
          <div class="preview-card-icon preview-icon--gold">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="6" cy="6" r="3" stroke="currentColor" stroke-width="1.5"/><path d="M2 15c0-2.21 1.79-4 4-4s4 1.79 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M15 6v6M12 9h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
          <div class="preview-card-content">
            <h4 class="preview-card-title">玩家类型</h4>
            <p class="preview-card-desc">基于游玩行为聚类分析，识别硬核玩家、休闲玩家、探索型玩家等类型</p>
            <div class="preview-dimensions">
              <span class="preview-dim preview-dim--gold">硬核玩家</span>
              <span class="preview-dim preview-dim--gold">休闲玩家</span>
              <span class="preview-dim preview-dim--gold">探索型</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state" aria-busy="true" role="status">
      <span class="sr-only">加载中...</span>
      <div class="skeleton" style="height: 100px; border-radius: var(--radius-xl); margin-bottom: var(--space-6);"></div>
      <div class="skeleton-grid">
        <div class="skeleton" style="height: 200px; border-radius: var(--radius-xl);"></div>
        <div class="skeleton" style="height: 200px; border-radius: var(--radius-xl);"></div>
      </div>
    </div>

    <!-- 画像内容 -->
    <template v-if="profile && !loading && profileLoaded">
      <!-- 用户基本信息 Hero 条 -->
      <div class="user-hero">
        <div class="user-hero-bg"></div>
        <div class="user-hero-deco user-hero-deco--tl"></div>
        <div class="user-hero-deco user-hero-deco--br"></div>
        <div class="user-hero-inner">
          <div class="user-hero-avatar" :style="{ background: avatarGradient }">
            <span class="avatar-letter">{{ String(userId).charAt(0) }}</span>
            <span class="avatar-ring"></span>
          </div>
          <div class="user-hero-info">
            <div class="user-hero-name-row">
              <h2 class="user-hero-name">玩家 #{{ userId }}</h2>
              <div class="user-hero-type" :class="'type--' + playerTypeClass">
                <span class="type-dot"></span>
                {{ profile.playerType || '未知类型' }}
              </div>
            </div>
            <div class="user-hero-meta">
              <div class="meta-item">
                <span class="meta-icon">
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><circle cx="6" cy="6" r="4" stroke="currentColor" stroke-width="1.2"/><path d="M6 3.5v2.5l1.5 1.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
                </span>
                <span class="meta-value">{{ profile.totalPlayHours || 0 }}<small>h</small></span>
                <span class="meta-label">总时长</span>
              </div>
              <div class="meta-item">
                <span class="meta-icon">
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><rect x="2" y="2" width="8" height="8" rx="1.5" stroke="currentColor" stroke-width="1.2"/><path d="M4 6l1.5 1.5L8 4.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </span>
                <span class="meta-value">{{ profile.purchaseCount || 0 }}</span>
                <span class="meta-label">购买</span>
              </div>
              <div class="meta-item">
                <span class="meta-icon">
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><polygon points="2,1.5 10,6 2,10.5" fill="currentColor"/></svg>
                </span>
                <span class="meta-value">{{ profile.playCount || 0 }}</span>
                <span class="meta-label">游玩</span>
              </div>
            </div>
          </div>
          <div class="user-hero-score">
            <div class="score-ring" :style="scoreRingStyle">
              <div class="score-ring-inner">
                <span class="score-num">{{ rfm.totalScore || 0 }}</span>
                <span class="score-unit">/ 12</span>
              </div>
            </div>
            <span class="score-label">RFM 总分</span>
          </div>
        </div>
      </div>

      <!-- 核心指标卡片 -->
      <div class="stats-grid">
        <div class="stat-block stat-block--gold">
          <div class="stat-block-deco"></div>
          <div class="stat-block-icon">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M10 2l2.09 4.472 4.66.632-3.375 3.362.825 4.784L10 12.952l-4.2 2.296.825-4.784L2.25 7.104l4.66-.632L10 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
          </div>
          <div class="stat-block-info">
            <span class="stat-block-label">价值等级</span>
            <span class="stat-block-value" :style="{ color: valueLevelColor }">{{ rfm.valueLevel || '--' }}</span>
            <span class="stat-block-sub">RFM 总分 {{ rfm.totalScore || 0 }}/12</span>
          </div>
        </div>

        <div class="stat-block stat-block--cyan">
          <div class="stat-block-deco"></div>
          <div class="stat-block-icon">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M10 17.5a7.5 7.5 0 1 0 0-15 7.5 7.5 0 0 0 0 15z" stroke="currentColor" stroke-width="1.5"/><path d="M10 5.83V10l2.5 2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="stat-block-info">
            <span class="stat-block-label">活跃度等级</span>
            <span class="stat-block-value" :style="{ color: activityLevelColor }">{{ activity.activityLevel || '--' }}</span>
            <span class="stat-block-sub">归一化得分 {{ ((activity.normalizedScore || 0) * 100).toFixed(0) }}%</span>
          </div>
        </div>

        <div class="stat-block stat-block--primary">
          <div class="stat-block-deco"></div>
          <div class="stat-block-icon">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="7" cy="7" r="4" stroke="currentColor" stroke-width="1.5"/><path d="M3 17c0-2.21 1.79-4 4-4s4 1.79 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M16 5v6M13 8h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
          <div class="stat-block-info">
            <span class="stat-block-label">玩家类型</span>
            <span class="stat-block-value">{{ profile.playerType || '未知' }}</span>
            <span class="stat-block-sub">总游玩 {{ profile.totalPlayHours || 0 }}h</span>
          </div>
        </div>

        <div class="stat-block stat-block--green">
          <div class="stat-block-deco"></div>
          <div class="stat-block-icon">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M4 6l4-4 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M8 2v10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M3 18h14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
          <div class="stat-block-info">
            <span class="stat-block-label">购买-游玩转化率</span>
            <span class="stat-block-value">{{ ((profile.purchasePlayRatio || 0) * 100).toFixed(1) }}<small>%</small></span>
            <span class="stat-block-sub">购买 {{ profile.purchaseCount || 0 }} / 游玩 {{ profile.playCount || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- RFM + 活跃度详情 -->
      <div class="dual-panel">
        <div class="panel-card">
          <div class="panel-deco panel-deco--tl"></div>
          <div class="panel-deco panel-deco--br"></div>
          <div class="panel-card-header">
            <h3 class="panel-card-title">RFM 价值分层</h3>
            <span class="panel-card-sub">RFM Value Tier</span>
          </div>
          <div class="rfm-list">
            <div class="rfm-item" v-for="item in rfmTable" :key="item.dimension">
              <div class="rfm-dim-badge" :style="{ '--c1': scoreColor(item.score), '--c2': scoreColor(item.score) }">
                {{ item.dimension }}
              </div>
              <div class="rfm-content">
                <div class="rfm-meta">
                  <span class="rfm-name">{{ item.name }}</span>
                  <span class="rfm-value">{{ item.value }}</span>
                </div>
                <div class="rfm-score-row">
                  <div class="rfm-score-bar" role="progressbar" :aria-valuenow="item.score" aria-valuemin="0" aria-valuemax="4" :aria-label="item.name + '得分'">
                    <div class="rfm-score-fill" :style="{ width: (item.score / 4 * 100) + '%', background: scoreColor(item.score) }"></div>
                  </div>
                  <span class="rfm-score-num" :style="{ color: scoreColor(item.score) }">{{ item.score }}<small>/4</small></span>
                </div>
              </div>
            </div>
            <!-- RFM 总览条 -->
            <div class="rfm-summary">
              <div class="rfm-summary-header">
                <span class="rfm-summary-label">综合价值</span>
                <div class="rfm-summary-meta">
                  <span class="rfm-summary-val" :style="{ color: valueLevelColor }">{{ rfm.totalScore || 0 }}<small>/12</small></span>
                  <span class="rfm-summary-tag" :style="{ background: valueLevelGradient }">{{ rfm.valueLevel || '未知' }}</span>
                </div>
              </div>
              <div class="rfm-summary-bar">
                <div class="rfm-summary-fill" :style="{ width: ((rfm.totalScore || 0) / 12 * 100) + '%', background: valueLevelGradient }"></div>
              </div>
            </div>
          </div>
        </div>

        <div class="panel-card">
          <div class="panel-deco panel-deco--tl"></div>
          <div class="panel-deco panel-deco--br"></div>
          <div class="panel-card-header">
            <h3 class="panel-card-title">活跃度评估</h3>
            <span class="panel-card-sub">Activity Assessment</span>
          </div>
          <div class="activity-body">
            <!-- 中心大环 -->
            <div class="activity-ring">
              <div class="activity-ring-svg">
                <svg viewBox="0 0 120 120">
                  <circle cx="60" cy="60" r="50" fill="none" stroke="var(--color-bg-subtle)" stroke-width="8"/>
                  <circle
                    cx="60" cy="60" r="50"
                    fill="none"
                    :stroke="progressGradient"
                    stroke-width="8"
                    stroke-linecap="round"
                    :stroke-dasharray="314.16"
                    :stroke-dashoffset="314.16 - 314.16 * (activity.normalizedScore || 0)"
                    transform="rotate(-90 60 60)"
                    class="activity-ring-progress"
                  />
                </svg>
                <div class="activity-ring-content">
                  <span class="ring-num">{{ ((activity.normalizedScore || 0) * 100).toFixed(0) }}<small>%</small></span>
                  <span class="ring-label">{{ activity.activityLevel || '--' }}</span>
                </div>
              </div>
            </div>
            <!-- 数据列表 -->
            <div class="activity-list">
              <div class="activity-row">
                <span class="activity-row-label">原始得分</span>
                <span class="activity-row-value">{{ activity.rawScore || 0 }}</span>
              </div>
              <div class="activity-row">
                <span class="activity-row-label">评分记录数</span>
                <span class="activity-row-value">{{ activity.ratingCount || 0 }}</span>
              </div>
              <div class="activity-row">
                <span class="activity-row-label">总游玩时长</span>
                <span class="activity-row-value">{{ profile.totalPlayHours || 0 }}h</span>
              </div>
            </div>
            <!-- 活跃度解读 -->
            <div class="activity-insight">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><circle cx="7" cy="7" r="6" stroke="currentColor" stroke-width="1.2"/><path d="M7 4v3.5M7 10.5v.01" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
              <span>{{ activityInsight }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 兴趣标签 -->
      <div v-if="interestTags && interestTags.length > 0" class="panel-card tags-panel">
        <div class="panel-deco panel-deco--tl"></div>
        <div class="panel-deco panel-deco--br"></div>
        <div class="panel-card-header">
          <h3 class="panel-card-title">兴趣标签</h3>
          <span class="panel-card-sub">{{ interestTags.length }} 个标签 · 越深越热门</span>
        </div>
        <div class="tags-cloud">
          <span
            v-for="tag in interestTags"
            :key="tag.tag"
            class="tag-chip"
            :class="tagClass(tag.avgScore)"
            :title="tag.tag + ': ' + tag.count + '次游玩, 均分' + tag.avgScore"
            :style="{ '--tag-size': 0.95 + Math.min(tag.count / Math.max(...interestTags.map(t => t.count)) * 0.5, 0.6) }"
          >
            <span class="tag-name">{{ tag.tag }}</span>
            <span class="tag-meta">
              <span class="tag-count">{{ tag.count }}次</span>
              <span class="tag-dot">·</span>
              <span class="tag-score">★{{ tag.avgScore }}</span>
            </span>
          </span>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { getFullProfile, getOverview, getTopActiveUsers } from '@/api/index';
import PageHeader from '@/components/PageHeader.vue';
import QueryBar from '@/components/QueryBar.vue';

export default {
  name: 'UserProfile',
  components: { PageHeader, QueryBar },
  data() {
    return {
      userId: '',
      loading: false,
      profile: null,
      profileLoaded: false,
      rfm: {},
      activity: {},
      interestTags: [],
      rfmTable: [],
      sysOverview: null,
      sampleIds: [1, 42, 128, 777, 2048, 9999, 23456, 56789]
    };
  },
  mounted() {
    this.loadSampleIds();
    this.loadSystemOverview();
    if (this.$route.query.userId) {
      this.userId = this.$route.query.userId;
      this.loadProfile();
    }
  },
  computed: {
    valueLevelColor() {
      const l = this.rfm.valueLevel || '';
      if (l.includes('高')) return 'var(--color-warm-500)';
      if (l.includes('中')) return 'var(--color-accent-500)';
      return 'var(--color-text-tertiary)';
    },
    valueLevelGradient() {
      const s = this.rfm.totalScore || 0;
      if (s >= 9) return 'linear-gradient(90deg, var(--color-warm-500), var(--color-warm-400))';
      if (s >= 6) return 'linear-gradient(90deg, var(--color-brand-500), var(--color-accent-500))';
      return 'linear-gradient(90deg, var(--color-text-tertiary), var(--color-text-secondary))';
    },
    activityLevelColor() {
      const l = this.activity.activityLevel || '';
      if (l.includes('高热')) return 'var(--color-warm-500)';
      if (l.includes('中热')) return 'var(--color-accent-500)';
      if (l.includes('低热')) return 'var(--color-brand-500)';
      return 'var(--color-text-tertiary)';
    },
    progressGradient() {
      const s = this.activity.normalizedScore || 0;
      if (s >= 0.7) return 'url(#progress-grad-high)';
      if (s >= 0.4) return 'url(#progress-grad-mid)';
      return 'url(#progress-grad-low)';
    },
    scoreRingStyle() {
      const total = this.rfm.totalScore || 0;
      const pct = Math.min(100, (total / 12) * 100);
      return { '--pct': pct + '%' };
    },
    activityInsight() {
      const s = this.activity.normalizedScore || 0;
      const l = this.activity.activityLevel || '';
      const raw = this.activity.rawScore || 0;
      const rc = this.activity.ratingCount || 0;
      const ph = this.profile ? this.profile.totalPlayHours || 0 : 0;

      if (l.includes('高热')) {
        return `该玩家活跃度极高（归一化得分 ${(s*100).toFixed(0)}%），${rc} 条评分记录，累计游玩 ${ph}h，属于核心用户群体，建议优先推送新游戏和社区活动。`;
      }
      if (l.includes('中热')) {
        return `该玩家活跃度良好（归一化得分 ${(s*100).toFixed(0)}%），${rc} 条评分记录，累计游玩 ${ph}h，` +
          (ph > 50 ? '近期仍有参与，可通过个性化推荐和限时活动进一步提升粘性。' : '有一定参与度，建议推送热门游戏和折扣信息提升活跃度。');
      }
      if (l.includes('低热')) {
        return `该玩家活跃度偏低（归一化得分 ${(s*100).toFixed(0)}%），${rc} 条评分记录，累计游玩 ${ph}h，` +
          (rc > 0 ? '近期参与较少，建议通过回归奖励和个性化推荐重新激活。' : '仅有少量历史记录，建议推送热门游戏吸引回归。');
      }
      if (l.includes('冰封')) {
        if (rc === 0) return '该玩家暂无评分记录，属于冰封用户（归一化得分 0%），建议推送入门推荐和新手引导信息。';
        return `该玩家活跃度极低（归一化得分 ${(s*100).toFixed(0)}%），${rc} 条评分记录但均已过期，建议通过回归奖励和大幅折扣信息尝试激活。`;
      }
      return `该玩家活跃度较低（归一化得分 ${(s*100).toFixed(0)}%），建议通过回归奖励和热门游戏推荐重新激活。`;
    },
    avatarGradient() {
      const hash = String(this.userId).split('').reduce((a, c) => a + c.charCodeAt(0), 0);
      const hues = [260, 190, 35, 160, 330, 210];
      const h = hues[hash % hues.length];
      return `linear-gradient(135deg, hsl(${h}, 70%, 50%), hsl(${(h + 30) % 360}, 70%, 40%))`;
    },
    playerTypeClass() {
      const t = (this.profile && this.profile.playerType) || '';
      if (t.includes('硬核')) return 'hardcore';
      if (t.includes('休闲')) return 'casual';
      if (t.includes('探索')) return 'explorer';
      return 'default';
    }
  },
  methods: {
    async loadSystemOverview() {
      try {
        const { data } = await getOverview();
        if (data.code === 200) {
          const d = data.data;
          this.sysOverview = {
            userCount: (d.userCount || 0).toLocaleString(),
            gameCount: (d.gameCount || 0).toLocaleString(),
            ratingCount: (d.ratingCount || 0).toLocaleString(),
            avgRating: Number(d.avgRating || 0).toFixed(1)
          };
        }
      } catch (e) { console.error('加载系统概况失败:', e); }
    },
    async loadProfile() {
      if (!this.userId) { this.$message.warning('请输入用户ID'); return; }
      this.loading = true;
      this.profileLoaded = false;
      try {
        const { data } = await getFullProfile(this.userId);
        if (data.code === 200) {
          this.profile = data.data;
          this.profileLoaded = true;
          this.rfm = data.data.rfm || {};
          this.activity = data.data.activity || {};
          this.interestTags = data.data.interestTags || [];
          this.rfmTable = [
            { dimension: 'R', name: '最近游玩', value: (this.rfm.recency || 0) + '天前', score: this.rfm.rScore || 0 },
            { dimension: 'F', name: '游玩频率', value: (this.rfm.frequency || 0) + '款', score: this.rfm.fScore || 0 },
            { dimension: 'M', name: '平均评分', value: this.rfm.monetary || '--', score: this.rfm.mScore || 0 }
          ];
        } else {
          this.$message.error(data.message || '查询失败');
          this.profileLoaded = false;
        }
      } catch (e) {
        this.$message.error('获取画像失败，请检查用户ID是否存在');
        this.profileLoaded = false;
      } finally {
        this.loading = false;
      }
    },
    randomUser() {
      this.userId = String(Math.floor(Math.random() * 56789) + 1);
      this.loadProfile();
    },
    async loadSampleIds() {
      try {
        const { data } = await getTopActiveUsers(8);
        if (data.code === 200 && Array.isArray(data.data) && data.data.length > 0) {
          this.sampleIds = data.data.map(u => u.user_id).filter(id => id != null);
        }
      } catch (e) { /* 使用默认sampleIds */ }
    },
    scoreColor(score) {
      if (score >= 4) return 'var(--color-warm-500)';
      if (score >= 3) return 'var(--color-accent-500)';
      if (score >= 2) return 'var(--color-brand-500)';
      return 'var(--color-text-tertiary)';
    },
    tagClass(score) {
      if (score >= 4) return 'tag--hot';
      if (score >= 3) return 'tag--warm';
      return 'tag--cool';
    },
    goRecommend() {
      this.$router.push({ path: '/recommend', query: { userId: this.userId } });
    }
  }
};
</script>

<style scoped>
.page-profile { max-width: var(--content-max-width); margin: 0 auto; }

/* Query Input */
.query-input-wrap { position: relative; flex: 1; min-width: 180px; max-width: 320px; }
.query-input-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--color-text-tertiary); pointer-events: none; }
.query-input { width: 100%; padding: 10px 12px 10px 38px; border: 1px solid var(--color-border-default); border-radius: var(--radius-xl); background: var(--color-bg-subtle); color: var(--color-text-primary); font-family: var(--font-family-mono); font-size: var(--text-sm); transition: all var(--duration-fast) var(--ease-out); }
.query-input:focus { outline: none; border-color: var(--color-brand-500); box-shadow: 0 0 0 3px var(--color-brand-a15); }
.query-input::placeholder { color: var(--color-text-tertiary); }

.btn-group { display: flex; gap: var(--space-2); }
.btn { display: inline-flex; align-items: center; gap: var(--space-2); padding: 10px 16px; border: none; border-radius: var(--radius-xl); font-size: var(--text-sm); font-weight: var(--font-weight-medium); cursor: pointer; transition: all var(--duration-fast) var(--ease-out); white-space: nowrap; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: linear-gradient(135deg, var(--color-brand-500), var(--color-brand-600)); color: #fff; box-shadow: 0 2px 8px var(--color-brand-a25); }
.btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px var(--color-brand-a35); }
.btn-accent { background: var(--color-accent-a12); color: var(--color-accent-400); border: 1px solid var(--color-accent-a20); }
.btn-accent:hover:not(:disabled) { background: var(--color-accent-a20); }
.btn-ghost { background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-border-default); }
.btn-ghost:hover:not(:disabled) { background: var(--color-bg-subtle); color: var(--color-text-primary); border-color: var(--color-border-strong); }

/* Section Label */
.section-label {
  font-size: var(--text-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-4);
  margin-top: var(--space-6);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.section-label::before {
  content: '';
  width: 3px;
  height: 16px;
  border-radius: var(--radius-full);
  background: var(--color-brand-500);
}

/* ---- Welcome Card ---- */
.welcome-card {
  position: relative;
  background: linear-gradient(135deg, var(--color-brand-a06), var(--color-accent-a04));
  border: 1px solid var(--color-brand-a12);
  border-radius: var(--radius-2xl);
  padding: var(--space-8);
  overflow: hidden;
  animation: fade-up 0.5s var(--ease-out-expo);
  margin-bottom: var(--space-4);
}
@keyframes fade-up {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}
.welcome-glow {
  position: absolute;
  top: -60%;
  right: -20%;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--color-brand-a12), transparent 70%);
  pointer-events: none;
  animation: welcome-glow-drift 10s ease-in-out infinite;
}
@keyframes welcome-glow-drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50%      { transform: translate(-20px, 20px) scale(1.05); }
}
.welcome-grid {
  position: absolute;
  background-image:
    linear-gradient(var(--color-brand-a06) 1px, transparent 1px),
    linear-gradient(90deg, var(--color-brand-a06) 1px, transparent 1px);
  background-size: 32px 32px;
  pointer-events: none;
  opacity: 0.5;
}
.welcome-grid--1 { top: 0; left: 0; right: 0; bottom: 50%; mask-image: linear-gradient(180deg, rgba(0,0,0,0.6), transparent); -webkit-mask-image: linear-gradient(180deg, rgba(0,0,0,0.6), transparent); }
.welcome-grid--2 { bottom: 0; left: 0; right: 0; top: 50%; mask-image: linear-gradient(0deg, rgba(0,0,0,0.6), transparent); -webkit-mask-image: linear-gradient(0deg, rgba(0,0,0,0.6), transparent); }
.welcome-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.welcome-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-brand-a15), var(--color-accent-a10));
  color: var(--color-brand-400);
  margin-bottom: var(--space-4);
  position: relative;
  border: 1px solid var(--color-brand-a20);
}
.welcome-icon::after {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 1px dashed var(--color-brand-a35);
  animation: ring-rotate 16s linear infinite;
}
@keyframes ring-rotate {
  to { transform: rotate(360deg); }
}
.welcome-title {
  font-size: 20px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);
}
.welcome-desc {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  max-width: 480px;
  line-height: var(--line-height-relaxed);
  margin-bottom: var(--space-6);
}
.welcome-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}
.welcome-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, var(--color-brand-500), var(--color-brand-600));
  color: #fff;
  font-size: var(--text-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  box-shadow: 0 2px 8px var(--color-brand-a35);
}
.welcome-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 16px var(--color-brand-a40); }
.welcome-or { font-size: var(--text-xs); color: var(--color-text-tertiary); }
.sample-users { display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; justify-content: center; }
.sample-label { font-size: var(--text-xs); color: var(--color-text-tertiary); }
.sample-chip {
  padding: 4px 12px;
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--color-accent-400);
  font-family: var(--font-family-mono);
  font-size: var(--text-xs);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.sample-chip:hover { background: var(--color-accent-a10); border-color: var(--color-accent-500); transform: translateY(-1px); }

/* ---- System Overview ---- */
.overview-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}
.overview-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-xl);
  transition: all var(--duration-fast);
}
.overview-item:hover { border-color: var(--color-border-default); transform: translateY(-1px); }
.overview-item-icon {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-lg);
  flex-shrink: 0;
}
.overview-icon--primary { background: var(--color-brand-a12); color: var(--color-brand-400); }
.overview-icon--cyan    { background: var(--color-accent-a12);  color: var(--color-accent-500); }
.overview-icon--gold    { background: var(--color-warm-a12); color: var(--color-warm-400); }
.overview-icon--green   { background: var(--color-success-bg); color: var(--color-success); }
.overview-item-info { display: flex; flex-direction: column; }
.overview-item-val {
  font-family: var(--font-family-mono);
  font-size: var(--text-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}
.overview-item-label {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

/* ---- Preview Cards ---- */
.preview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}
.preview-card {
  display: flex;
  gap: var(--space-3);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  transition: all var(--duration-fast);
  position: relative;
  overflow: hidden;
}
.preview-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-border-default), transparent);
  opacity: 0;
  transition: opacity 0.2s;
}
.preview-card:hover {
  border-color: var(--color-border-default);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.preview-card:hover::before { opacity: 1; }
.preview-card-icon {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-lg);
  flex-shrink: 0;
}
.preview-icon--primary { background: var(--color-brand-a12); color: var(--color-brand-400); }
.preview-icon--cyan    { background: var(--color-accent-a12);  color: var(--color-accent-500); }
.preview-icon--gold    { background: var(--color-warm-a12); color: var(--color-warm-400); }
.preview-card-content { flex: 1; min-width: 0; }
.preview-card-title {
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 6px;
}
.preview-card-desc {
  font-size: 12px;
  color: var(--color-text-tertiary);
  line-height: var(--line-height-relaxed);
  margin: 0 0 var(--space-3);
}
.preview-dimensions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.preview-dim {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-brand-a08);
  color: var(--color-brand-400);
  font-size: 11px;
  font-weight: 500;
}
.preview-dim--cyan { color: var(--color-accent-500); background: var(--color-accent-a08); }
.preview-dim--gold { color: var(--color-warm-400); background: var(--color-warm-a10); }
.preview-dim--dim  { color: var(--color-text-tertiary); background: var(--color-bg-subtle); }

/* ---- User Hero (新) ---- */
.user-hero {
  position: relative;
  background: linear-gradient(135deg,
    var(--color-bg-elevated) 0%,
    color-mix(in srgb, var(--color-bg-elevated) 85%, var(--color-brand-500) 8%) 100%);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-2xl);
  padding: var(--space-6) var(--space-6);
  margin-bottom: var(--space-5);
  overflow: hidden;
  animation: fade-up 0.4s var(--ease-out-expo);
}
.user-hero-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 20% 0%, var(--color-brand-a10) 0%, transparent 50%),
              radial-gradient(ellipse at 100% 100%, var(--color-accent-a08) 0%, transparent 50%);
  pointer-events: none;
}
.user-hero-deco {
  position: absolute;
  width: 16px; height: 16px;
  border-color: var(--color-brand-500);
  opacity: 0.6;
  z-index: 2;
}
.user-hero-deco--tl { top: 8px; left: 8px; border-top: 2px solid; border-left: 2px solid; }
.user-hero-deco--br { bottom: 8px; right: 8px; border-bottom: 2px solid; border-right: 2px solid; }
.user-hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--space-5);
}
.user-hero-avatar {
  position: relative;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-xl);
  flex-shrink: 0;
  box-shadow: 0 8px 24px rgba(0,0,0,0.20);
}
.avatar-letter {
  font-family: var(--font-family-display);
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}
.avatar-ring {
  position: absolute;
  inset: -4px;
  border-radius: calc(var(--radius-xl) + 4px);
  border: 2px dashed rgba(255,255,255,0.4);
  animation: ring-rotate 14s linear infinite;
}
.user-hero-info {
  flex: 1;
  min-width: 0;
}
.user-hero-name-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.user-hero-name {
  font-family: var(--font-family-display);
  font-size: 20px;
  font-weight: 800;
  color: var(--color-text-primary);
  margin: 0;
  letter-spacing: -0.3px;
}
.user-hero-type {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: var(--font-weight-semibold);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-default);
  color: var(--color-text-secondary);
}
.type--hardcore { background: var(--color-danger-bg); color: var(--color-danger); border: 1px solid var(--color-danger-border); }
.type--casual   { background: var(--color-success-bg); color: var(--color-success); border: 1px solid var(--color-success-border); }
.type--explorer { background: var(--color-brand-a10); color: var(--color-brand-400); border: 1px solid var(--color-brand-a25); }
.type--default  { background: var(--color-bg-subtle); color: var(--color-text-secondary); border: 1px solid var(--color-border-default); }
.type-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 6px currentColor;
  animation: type-pulse 1.4s ease-in-out infinite;
}
@keyframes type-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.5; transform: scale(0.7); }
}

.user-hero-meta {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
  font-size: 12px;
  transition: all 0.15s;
}
.meta-item:hover { border-color: var(--color-border-default); }
.meta-icon { color: var(--color-text-tertiary); display: flex; }
.meta-value {
  font-family: var(--font-family-mono);
  font-weight: 700;
  color: var(--color-text-primary);
}
.meta-value small { font-size: 10px; color: var(--color-text-tertiary); margin-left: 1px; }
.meta-label { color: var(--color-text-tertiary); }

.user-hero-score {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.score-ring {
  --pct: 0%;
  --c1: var(--color-brand-500);
  --c2: var(--color-accent-500);
  position: relative;
  width: 84px; height: 84px;
  border-radius: 50%;
  background: conic-gradient(var(--c1) var(--pct), var(--color-bg-subtle) 0);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 20px color-mix(in srgb, var(--c1) 30%, transparent);
  transition: all 0.4s var(--ease-out-expo);
}
.score-ring-inner {
  width: 70px; height: 70px;
  background: var(--color-bg-elevated);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.score-num {
  font-family: var(--font-family-mono);
  font-size: 24px;
  font-weight: 800;
  color: var(--c1);
  line-height: 1;
}
.score-unit {
  font-size: 10px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}
.score-label {
  font-size: 11px;
  color: var(--color-text-tertiary);
  font-family: var(--font-family-mono);
  letter-spacing: 1px;
}

/* Loading */
.loading-state { padding: var(--space-4) 0; }
.skeleton-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-6); }

/* ---- 核心指标卡片 (重设计) ---- */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}
.stat-block {
  --c1: var(--color-brand-500);
  --c2: var(--color-brand-400);
  position: relative;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  transition: all 0.2s var(--ease-out-expo);
  overflow: hidden;
  isolation: isolate;
}
/* 语义色变体（令牌驱动，替代原 inline 硬编码） */
.stat-block--gold    { --c1: var(--color-warm-500);  --c2: var(--color-warm-400); }
.stat-block--cyan    { --c1: var(--color-accent-500); --c2: var(--color-accent-400); }
.stat-block--primary { --c1: var(--color-brand-500); --c2: var(--color-brand-400); }
.stat-block--green   { --c1: var(--color-success);   --c2: var(--color-success); }
.stat-block::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg,
    color-mix(in srgb, var(--c1) 8%, transparent) 0%,
    transparent 60%);
  z-index: -1;
  opacity: 0.6;
  transition: opacity 0.2s;
}
.stat-block:hover {
  border-color: var(--c1);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px -8px color-mix(in srgb, var(--c1) 30%, transparent);
}
.stat-block:hover::before { opacity: 1; }
.stat-block-deco {
  position: absolute;
  top: 0; left: 0;
  width: 60%;
  height: 2px;
  background: linear-gradient(90deg, var(--c1), transparent);
  opacity: 0.7;
}
.stat-block-icon {
  width: 44px; height: 44px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg,
    color-mix(in srgb, var(--c1) 12%, transparent),
    color-mix(in srgb, var(--c2) 8%, transparent));
  color: var(--c1);
  flex-shrink: 0;
  border: 1px solid color-mix(in srgb, var(--c1) 20%, transparent);
}
.stat-block-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.stat-block-label {
  font-size: 11px;
  color: var(--color-text-tertiary);
  letter-spacing: 0.5px;
  text-transform: uppercase;
  font-weight: 600;
  margin-bottom: 4px;
  display: block;
  width: 100%;
}
.stat-block-value {
  font-family: var(--font-family-display);
  font-size: 22px;
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.1;
  letter-spacing: -0.5px;
  margin-bottom: 4px;
  display: block;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-block-value small {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-tertiary);
  margin-left: 2px;
}
.stat-block-sub {
  font-size: 11px;
  color: var(--color-text-tertiary);
  font-family: var(--font-family-mono);
  display: block;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---- Dual Panel ---- */
.dual-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
}
.panel-card {
  position: relative;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-2xl);
  padding: var(--space-5) var(--space-6);
  transition: all 0.2s;
  overflow: hidden;
}
.panel-card:hover { border-color: var(--color-border-default); }
.panel-deco {
  position: absolute;
  width: 12px; height: 12px;
  border-color: var(--color-brand-500);
  opacity: 0.5;
  z-index: 1;
}
.panel-deco--tl { top: 0; left: 0; border-top: 1.5px solid; border-left: 1.5px solid; }
.panel-deco--br { bottom: 0; right: 0; border-bottom: 1.5px solid; border-right: 1.5px solid; }

.panel-card-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border-muted);
}
.panel-card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.panel-card-title::before {
  content: '';
  width: 3px;
  height: 14px;
  background: linear-gradient(180deg, var(--color-brand-500), var(--color-accent-500));
  border-radius: 2px;
  box-shadow: 0 0 6px currentColor;
}
.panel-card-sub {
  font-size: 10px;
  color: var(--color-text-tertiary);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  font-weight: 500;
  font-family: var(--font-family-mono);
}

/* ---- RFM List ---- */
.rfm-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.rfm-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.rfm-dim-badge {
  --c1: var(--color-brand-500);
  --c2: var(--color-accent-500);
  width: 40px; height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg,
    color-mix(in srgb, var(--c1) 15%, transparent),
    color-mix(in srgb, var(--c2) 10%, transparent));
  font-family: var(--font-family-mono);
  font-size: 14px;
  font-weight: 800;
  color: var(--c1);
  flex-shrink: 0;
  border: 1px solid color-mix(in srgb, var(--c1) 25%, transparent);
}
.rfm-content { flex: 1; min-width: 0; }
.rfm-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  gap: var(--space-2);
}
.rfm-name {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
  white-space: nowrap;
}
.rfm-value {
  font-family: var(--font-family-mono);
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 600;
  white-space: nowrap;
  text-align: right;
}
.rfm-score-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.rfm-score-bar {
  flex: 1;
  min-width: 0;
  height: 6px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-full);
  overflow: hidden;
}
.rfm-score-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 0.8s var(--ease-out-expo);
  box-shadow: 0 0 8px currentColor;
}
.rfm-score-num {
  font-family: var(--font-family-mono);
  font-size: 12px;
  min-width: 40px;
  text-align: right;
  font-weight: 700;
  flex-shrink: 0;
  white-space: nowrap;
  display: inline-block;
}
.rfm-score-num small { color: var(--color-text-tertiary); font-weight: 500; font-size: 11px; }

.rfm-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: var(--space-4);
  margin-top: var(--space-3);
  border-top: 1px solid var(--color-border-muted);
}
.rfm-summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}
.rfm-summary-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-weight: 500;
  letter-spacing: 0.3px;
}
.rfm-summary-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rfm-summary-val {
  font-family: var(--font-family-mono);
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.3px;
  line-height: 1;
}
.rfm-summary-val small {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-tertiary);
  margin-left: 1px;
}
.rfm-summary-tag {
  display: inline-block;
  padding: 3px 9px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  border-radius: var(--radius-full);
  letter-spacing: 0.3px;
  white-space: nowrap;
  line-height: 1.2;
}
.rfm-summary-bar {
  width: 100%;
  height: 8px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-full);
  overflow: hidden;
}
.rfm-summary-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 1s var(--ease-out-expo);
  box-shadow: 0 0 10px currentColor;
}

/* ---- Activity Body ---- */
.activity-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.activity-ring {
  display: flex;
  justify-content: center;
  padding: var(--space-3) 0;
}
.activity-ring-svg {
  position: relative;
  width: 160px;
  height: 160px;
}
.activity-ring-svg svg { width: 100%; height: 100%; }
.activity-ring-progress {
  transition: stroke-dashoffset 1.4s var(--ease-out-expo);
  filter: drop-shadow(0 0 6px currentColor);
}
.activity-ring-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.ring-num {
  font-family: var(--font-family-mono);
  font-size: 32px;
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1;
  background: linear-gradient(135deg, var(--color-brand-500), var(--color-accent-500));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.ring-num small { font-size: 16px; -webkit-text-fill-color: var(--color-text-tertiary); }
.ring-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
  font-weight: 500;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-base);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
}
.activity-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.activity-row-label { color: var(--color-text-secondary); }
.activity-row-value {
  font-family: var(--font-family-mono);
  font-weight: 700;
  color: var(--color-text-primary);
}

.activity-insight {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  padding: var(--space-3);
  border-radius: var(--radius-lg);
  background: var(--color-accent-a06);
  border: 1px solid var(--color-accent-a15);
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
}
.activity-insight svg { flex-shrink: 0; margin-top: 2px; color: var(--color-accent-500); }

/* ---- Tags ---- */
.tags-panel { margin-top: var(--space-5); }
.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-chip {
  display: inline-flex;
  flex-direction: column;
  gap: 3px;
  padding: 8px 14px;
  border-radius: var(--radius-lg);
  font-size: calc(var(--text-xs) * var(--tag-size, 1));
  border: 1px solid;
  transition: all var(--duration-fast) var(--ease-out);
  cursor: default;
  position: relative;
  overflow: hidden;
}
.tag-chip::before {
  content: '';
  position: absolute;
  inset: 0;
  background: currentColor;
  opacity: 0.05;
  z-index: -1;
}
.tag-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.tag--hot {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger);
}
.tag--warm {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning);
}
.tag--cool {
  background: var(--color-brand-a06);
  border-color: var(--color-brand-a20);
  color: var(--color-brand-400);
}
.tag-name {
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.2px;
}
.tag-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0.85;
  font-family: var(--font-family-mono);
  font-size: 10px;
}
.tag-count { font-weight: 500; }
.tag-dot { opacity: 0.4; }
.tag-score { color: inherit; opacity: 0.9; }

@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .dual-panel,.skeleton-grid { grid-template-columns: 1fr; }
  .overview-stats { grid-template-columns: repeat(2, 1fr); }
  .preview-grid { grid-template-columns: 1fr; }
  .user-hero-score { display: none; }
}
@media (max-width: 640px) {
  .stats-grid { grid-template-columns: 1fr; }
  .overview-stats { grid-template-columns: 1fr; }
  .welcome-card { padding: var(--space-5); }
  .query-input-wrap { max-width: 100%; width: 100%; }
  .btn-group { flex-wrap: wrap; width: 100%; }
  .btn { flex: 1; justify-content: center; }
  .user-hero-inner { flex-wrap: wrap; }
  .user-hero-avatar { width: 56px; height: 56px; }
  .avatar-letter { font-size: 24px; }
  .stat-block { padding: var(--space-4); }
  .panel-card { padding: var(--space-4) var(--space-5); }
}
@media (max-width: 480px) {
  .btn-text { display: none; }
}
</style>
