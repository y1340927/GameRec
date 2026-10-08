<template>
  <div class="page-eval">
    <page-header title="算法评估" description="精确率 · 召回率 · F1值 · NDCG · 离线对比" />

    <!-- 操作栏 -->
    <query-bar>
      <div class="query-select-wrap">
        <select id="k-select" v-model="topK" class="query-select" aria-label="K值选择">
          <option :value="5">K = 5</option>
          <option :value="10">K = 10</option>
          <option :value="15">K = 15</option>
          <option :value="20">K = 20</option>
        </select>
      </div>
      <div class="btn-group">
        <button class="btn btn-primary" @click="evaluate" :disabled="evaluating">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.5"/>
            <path d="M8 4.67V8l2 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          {{ evaluating ? '评估中...' : '开始评估' }}
        </button>
        <button class="btn btn-ghost" @click="loadHistory">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.5"/>
            <path d="M8 4v4l2 2M2 2l2 2M14 2l-2 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          历史结果
        </button>
      </div>
    </query-bar>

    <!-- 加载中 -->
    <div v-if="evaluating" class="loading-state" aria-busy="true">
      <div class="eval-progress">
        <div class="eval-progress-ring">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
            <circle cx="32" cy="32" r="28" stroke="var(--color-bg-subtle)" stroke-width="3"/>
            <circle cx="32" cy="32" r="28" stroke="var(--color-text-tertiary)" stroke-width="3" stroke-linecap="round" stroke-dasharray="176" stroke-dashoffset="44" class="eval-ring"/>
          </svg>
          <span class="eval-progress-text">评估中</span>
        </div>
        <p class="eval-progress-hint">正在对6种算法进行离线评估，请稍候...</p>
      </div>
    </div>

    <!-- ===== 评估指标说明（始终显示） ===== -->
    <template v-if="!evaluating">
      <div class="section-label">评估指标说明</div>
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-card-icon metric-icon--precision">P</div>
          <div class="metric-card-body">
            <h4 class="metric-card-title">精确率 Precision</h4>
            <p class="metric-card-desc">推荐结果中用户真正喜欢的游戏占比。衡量推荐准确性——推荐的东西有多少是对的。</p>
            <span class="metric-card-formula">TP / (TP + FP)</span>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-card-icon metric-icon--recall">R</div>
          <div class="metric-card-body">
            <h4 class="metric-card-title">召回率 Recall</h4>
            <p class="metric-card-desc">用户喜欢的游戏中被成功推荐出来的比例。衡量推荐全面性——喜欢的游戏推荐了多少。</p>
            <span class="metric-card-formula">TP / (TP + FN)</span>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-card-icon metric-icon--f1">F</div>
          <div class="metric-card-body">
            <h4 class="metric-card-title">F1值 F1-Score</h4>
            <p class="metric-card-desc">精确率与召回率的调和平均数，综合评估推荐质量。F1值越高表示推荐效果越均衡。</p>
            <span class="metric-card-formula">2 × P × R / (P + R)</span>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-card-icon metric-icon--ndcg">N</div>
          <div class="metric-card-body">
            <h4 class="metric-card-title">NDCG</h4>
            <p class="metric-card-desc">归一化折损累计增益，衡量排序质量。排名越靠前的好游戏贡献越大，关注推荐顺序。</p>
            <span class="metric-card-formula">DCG / IDCG</span>
          </div>
        </div>
      </div>

      <!-- 评估方法介绍 -->
      <div class="section-label" style="margin-top: var(--space-6);">评估方法</div>
      <div class="method-card">
        <div class="method-steps">
          <div class="method-step">
            <span class="method-step-num">1</span>
            <div>
              <h4 class="method-step-title">数据划分</h4>
              <p class="method-step-desc">将用户行为数据按时间序列划分为训练集(80%)和测试集(20%)，模拟线上真实场景。</p>
            </div>
          </div>
          <div class="method-step-arrow">→</div>
          <div class="method-step">
            <span class="method-step-num">2</span>
            <div>
              <h4 class="method-step-title">模型训练</h4>
              <p class="method-step-desc">使用训练集分别训练6种推荐算法：混合推荐、User-CF、Item-CF、Content-Based、SVD和流行度基线。</p>
            </div>
          </div>
          <div class="method-step-arrow">→</div>
          <div class="method-step">
            <span class="method-step-num">3</span>
            <div>
              <h4 class="method-step-title">离线评估</h4>
              <p class="method-step-desc">在测试集上计算每个用户的Top-K推荐结果，对比真实行为计算精确率、召回率、F1值和NDCG四项指标。</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 评估结果 ===== -->
    <template v-if="results.length > 0 && !evaluating">
      <!-- 概况卡片 -->
      <div class="insight-grid">
        <div class="insight-card insight-card--best">
          <div class="insight-card-row">
            <div>
              <div class="insight-card-label">最佳算法</div>
              <span class="insight-card-val">{{ bestAlgo.name }}</span>
            </div>
            <span class="insight-card-f1">F1: {{ bestAlgo.f1 }}</span>
          </div>
        </div>
        <div class="insight-card"><div class="insight-card-label">平均精确率</div><span class="insight-card-val">{{ avgPrecision }}</span></div>
        <div class="insight-card"><div class="insight-card-label">平均召回率</div><span class="insight-card-val">{{ avgRecall }}</span></div>
        <div class="insight-card"><div class="insight-card-label">平均F1值</div><span class="insight-card-val">{{ avgF1 }}</span></div>
      </div>

      <!-- 评估表格 -->
      <panel>
        <template #header>
          <h3 class="panel-title">评估结果 <span class="panel-badge">Top-{{ topK }}</span></h3>
        </template>
        <div class="table-wrap">
          <table class="data-table" role="table" aria-label="算法评估结果">
            <thead><tr><th>算法</th><th>精确率@K</th><th>召回率@K</th><th>F1值@K</th><th>NDCG@K</th><th>有效用户</th></tr></thead>
            <tbody>
              <tr v-for="row in results" :key="row.algorithmName" :class="{ 'row--highlight': row.algorithmName === bestAlgo.name }">
                <td><span class="algo-name">{{ row.algorithmName }}</span></td>
                <td><span class="metric" :class="metricClass(row.precision, 0.1)">{{ row.precision }}</span></td>
                <td><span class="metric" :class="metricClass(row.recall, 0.05)">{{ row.recall }}</span></td>
                <td><span class="metric" :class="metricClass(row.f1, 0.05)">{{ row.f1 }}</span></td>
                <td><span class="metric">{{ row.ndcg }}</span></td>
                <td><span class="metric metric--dim">{{ row.validUsers }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </panel>

      <!-- 对比图表 -->
      <panel title="算法对比图" style="margin-top: var(--space-6);">
        <div class="chart-body" ref="compareChart" role="img" aria-label="算法对比柱状图"></div>
      </panel>

      <!-- 历史结果 -->
      <panel v-if="historyResults.length > 0" title="历史评估记录" style="margin-top: var(--space-6);">
        <div class="table-wrap">
          <table class="data-table" role="table">
            <thead><tr><th>算法</th><th>K值</th><th>精确率</th><th>召回率</th><th>F1值</th><th>NDCG</th><th>评估时间</th></tr></thead>
            <tbody>
              <tr v-for="row in historyResults" :key="row.id || row.algorithmName + row.topK">
                <td><span class="algo-name">{{ row.algorithmName }}</span></td>
                <td><span class="metric">{{ row.topK }}</span></td>
                <td><span class="metric">{{ row.precisionVal }}</span></td>
                <td><span class="metric">{{ row.recallVal }}</span></td>
                <td><span class="metric">{{ row.f1Val }}</span></td>
                <td><span class="metric">{{ row.ndcgVal }}</span></td>
                <td><span class="metric metric--dim">{{ row.createTime }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </panel>
    </template>

    <!-- ===== 评估引导（仅评估前显示） ===== -->
    <template v-if="!results.length && !evaluating">
      <div class="action-hint">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="8" stroke="currentColor" stroke-width="1.5"/><path d="M10 7v5M10 4v.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        <span>选择 K 值后点击「开始评估」，对6种算法进行全面的离线对比分析。支持 K=5/10/15/20 四种规模。评估过程通常需要数分钟，请耐心等待。</span>
      </div>
    </template>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import { evaluateAll, getEvalHistory } from '@/api/index';
import PageHeader from '@/components/PageHeader.vue';
import QueryBar from '@/components/QueryBar.vue';
import Panel from '@/components/Panel.vue';
import { chartTheme, chartPalette } from '@/utils/echarts-theme';

export default {
  name: 'Evaluation',
  components: { PageHeader, QueryBar, Panel },
  data() {
    return { topK: 10, evaluating: false, results: [], historyResults: [], compareChart: null, resizeTimer: null };
  },
  computed: {
    bestAlgo() {
      if (!this.results.length) return { name: '-', f1: '-', precision: '-', ndcg: '-' };
      return this.results.reduce((b, r) => r.f1 > b.f1 ? r : b, this.results[0]);
    },
    maxPrecision() { return Math.max(...this.results.map(r => r.precision), 0.01); },
    maxRecall()    { return Math.max(...this.results.map(r => r.recall), 0.01); },
    maxF1()        { return Math.max(...this.results.map(r => r.f1), 0.01); },
    avgPrecision() { return this.results.length ? (this.results.reduce((s, r) => s + r.precision, 0) / this.results.length).toFixed(4) : '-'; },
    avgRecall()    { return this.results.length ? (this.results.reduce((s, r) => s + r.recall, 0) / this.results.length).toFixed(4) : '-'; },
    avgF1()        { return this.results.length ? (this.results.reduce((s, r) => s + r.f1, 0) / this.results.length).toFixed(4) : '-'; }
  },
  mounted() {
    window.addEventListener('resize', this.handleResize);
    this.loadHistory();
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    if (this.resizeTimer) clearTimeout(this.resizeTimer);
    if (this.compareChart) this.compareChart.dispose();
  },
  methods: {
    handleResize() {
      if (this.resizeTimer) clearTimeout(this.resizeTimer);
      this.resizeTimer = setTimeout(() => this.compareChart && this.compareChart.resize(), 150);
    },
    metricClass(val, threshold) { return val >= threshold ? 'metric--good' : 'metric--warn'; },
    async evaluate() {
      this.evaluating = true;
      try {
        const { data } = await evaluateAll(this.topK);
        if (data.code === 200) { this.results = data.data; this.$message.success('评估完成'); }
        else this.$message.error(data.message || '评估失败');
      } catch (e) { this.$message.error('评估失败: ' + (e.message || '网络错误')); }
      finally { this.evaluating = false; this.$nextTick(() => this.renderCompare()); }
    },
    async loadHistory() {
      try { const { data } = await getEvalHistory(); if (data.code === 200) this.historyResults = data.data; }
      catch (e) { /* */ }
    },
    renderCompare() {
      setTimeout(() => {
        const el = this.$refs.compareChart;
        if (!el || el.offsetWidth === 0) return;
        if (this.compareChart) { this.compareChart.dispose(); this.compareChart = null; }
        this.compareChart = echarts.init(el);
        const algs = this.results.map(r => r.algorithmName);
        const t = chartTheme();
        const p = chartPalette();
        this.compareChart.setOption({
          tooltip: { trigger: 'axis', backgroundColor: t.tooltip.backgroundColor, borderColor: t.tooltip.borderColor, borderWidth: 1, textStyle: { color: t.tooltip.textStyle.color, fontSize: 12 } },
          legend: { data: ['精确率','召回率','F1值','NDCG'], bottom: 0, textStyle: { color: t.axisColor, fontSize: 11 }, itemWidth: 12, itemHeight: 12, itemGap: 20 },
          grid: { left: '3%', right: '4%', bottom: '14%', top: '8%', containLabel: true },
          xAxis: { type: 'category', data: algs, axisLine: { lineStyle: { color: t.axisLine } }, axisLabel: { fontSize: 10, color: t.axisColor } },
          yAxis: { type: 'value', splitLine: { lineStyle: { color: t.gridColor, type: 'dashed' } }, axisLabel: { fontSize: 10, color: t.axisColor } },
          series: [
            { name: '精确率', type: 'bar', data: this.results.map(r => r.precision), itemStyle: { color: p[0], borderRadius: [4,4,0,0] }, barWidth: '18%' },
            { name: '召回率', type: 'bar', data: this.results.map(r => r.recall),    itemStyle: { color: p[1], borderRadius: [4,4,0,0] }, barWidth: '18%' },
            { name: 'F1值',   type: 'bar', data: this.results.map(r => r.f1),        itemStyle: { color: p[3], borderRadius: [4,4,0,0] }, barWidth: '18%' },
            { name: 'NDCG',   type: 'bar', data: this.results.map(r => r.ndcg),      itemStyle: { color: p[5], borderRadius: [4,4,0,0] }, barWidth: '18%' }
          ],
          animationDuration: 1000, animationEasing: 'cubicOut'
        });
      }, 200);
    }
  }
};
</script>

<style scoped>
.page-eval { max-width: var(--content-max-width); margin: 0 auto; }

.query-select-wrap { width: 140px; }
.query-select { width: 100%; padding: 10px 32px 10px 12px; border: 1px solid var(--color-border-default); border-radius: var(--radius-xl); background: var(--color-bg-subtle); color: var(--color-text-primary); font-size: var(--text-sm); cursor: pointer; appearance: none; background-image: url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%2371717a' stroke-width='1.5'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 10px center; }
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

/* Eval Progress */
.eval-progress { display: flex; flex-direction: column; align-items: center; padding: var(--space-16) var(--space-6); }
.eval-progress-ring { position: relative; display: flex; align-items: center; justify-content: center; margin-bottom: var(--space-4); }
.eval-ring { animation: ring-spin 2s linear infinite; transform-origin: center; }
@keyframes ring-spin { to { transform: rotate(360deg); } }
.eval-progress-text { position: absolute; font-size: var(--text-xs); font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); }
.eval-progress-hint { font-size: var(--text-sm); color: var(--color-text-tertiary); }

/* Insight Cards */
.insight-grid { display: grid; grid-template-columns: 1.5fr 1fr 1fr 1fr; gap: var(--space-4); margin-bottom: var(--space-6); animation: fade-up 0.4s var(--ease-out); }
@keyframes fade-up { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.insight-card { background: var(--color-bg-elevated); border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl); padding: var(--space-5); display: flex; flex-direction: column; gap: var(--space-2); }
.insight-card--best { border-color: var(--color-warm-a15); }
.insight-card-row { display: flex; align-items: center; justify-content: space-between; }
.insight-card-label { font-size: var(--text-xs); color: var(--color-text-tertiary); }
.insight-card-val { font-family: var(--font-family-mono); font-size: var(--text-xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }
.insight-card-f1 { font-family: var(--font-family-mono); font-size: var(--text-lg); font-weight: var(--font-weight-bold); color: var(--color-warm-500); }

/* Panel */
.panel-title { font-size: var(--text-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); display: flex; align-items: center; gap: var(--space-2); }
.panel-badge { padding: 2px 8px; border-radius: var(--radius-full); background: var(--color-bg-subtle); color: var(--color-text-tertiary); font-size: var(--text-xs); }

/* Table */
.table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.data-table { width: 100%; border-collapse: collapse; font-size: var(--text-sm); min-width: 600px; }
.data-table th { padding: var(--space-3) var(--space-4); text-align: left; font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border-muted); font-size: var(--text-xs); text-transform: uppercase; letter-spacing: 0.05em; }
.data-table td { padding: var(--space-3) var(--space-4); border-bottom: 1px solid var(--color-border-muted); color: var(--color-text-primary); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tbody tr { transition: background var(--duration-fast); }
.data-table tbody tr:hover { background: var(--color-bg-subtle); }
.data-table tbody tr.row--highlight { background: var(--color-warm-a04); }
.algo-name { font-weight: var(--font-weight-medium); color: var(--color-brand-400); }
.metric { font-family: var(--font-family-mono); font-size: var(--text-sm); }
.metric--good { color: var(--color-success); }
.metric--warn { color: var(--color-warm-500); }
.metric--dim { color: var(--color-text-tertiary); }
.chart-body { height: 360px; }

/* ---- Metrics Explanation Cards ---- */
.metrics-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--space-4); margin-bottom: var(--space-4); }
.metric-card { display: flex; gap: var(--space-4); padding: var(--space-5); background: var(--color-bg-elevated); border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl); transition: border-color var(--duration-fast); }
.metric-card:hover { border-color: var(--color-border-default); }
.metric-card-icon { width: 40px; height: 40px; border-radius: var(--radius-lg); display: flex; align-items: center; justify-content: center; flex-shrink: 0; font-family: var(--font-family-display); font-size: var(--text-lg); font-weight: var(--font-weight-bold); color: #fff; }
.metric-icon--precision { background: var(--color-brand-500); }
.metric-icon--recall { background: var(--color-accent-500); }
.metric-icon--f1 { background: var(--color-warm-500); }
.metric-icon--ndcg { background: var(--color-success); }
.metric-card-body { flex: 1; min-width: 0; }
.metric-card-title { font-size: var(--text-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: 4px; }
.metric-card-desc { font-size: var(--text-xs); color: var(--color-text-secondary); line-height: var(--line-height-relaxed); margin-bottom: 6px; }
.metric-card-formula { font-family: var(--font-family-mono); font-size: var(--text-2xs); color: var(--color-text-tertiary); background: var(--color-bg-subtle); padding: 2px 8px; border-radius: var(--radius-sm); display: inline-block; }

/* ---- Method Card ---- */
.method-card { background: var(--color-bg-elevated); border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl); padding: var(--space-6); margin-bottom: var(--space-4); }
.method-steps { display: flex; align-items: flex-start; gap: var(--space-3); }
.method-step { flex: 1; display: flex; gap: var(--space-3); align-items: flex-start; }
.method-step-num { width: 28px; height: 28px; border-radius: 50%; background: var(--color-bg-subtle); display: flex; align-items: center; justify-content: center; font-family: var(--font-family-mono); font-size: var(--text-xs); font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); flex-shrink: 0; }
.method-step-title { font-size: var(--text-xs); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: 4px; }
.method-step-desc { font-size: var(--text-2xs); color: var(--color-text-secondary); line-height: var(--line-height-relaxed); }
.method-step-arrow { color: var(--color-text-tertiary); font-size: var(--text-xl); flex-shrink: 0; padding-top: 4px; }

/* Action Hint */
.action-hint { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-4); background: var(--color-bg-elevated); border: 1px solid var(--color-border-muted); border-radius: var(--radius-xl); margin-top: var(--space-4); font-size: var(--text-sm); color: var(--color-text-secondary); line-height: var(--line-height-relaxed); }
.action-hint svg { color: var(--color-text-tertiary); flex-shrink: 0; }

@media (max-width: 1024px) { .insight-grid { grid-template-columns: repeat(2, 1fr); } .metrics-grid { grid-template-columns: 1fr; } .method-steps { flex-direction: column; } .method-step-arrow { display: none; } }
@media (max-width: 640px) { .insight-grid { grid-template-columns: 1fr; } .chart-body { height: 260px; } }
</style>
