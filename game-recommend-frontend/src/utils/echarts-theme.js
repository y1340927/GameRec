/**
 * echarts-theme.js — 统一 ECharts 双主题配色
 *
 * 设计原则：所有颜色从 design-tokens.css 的 CSS 变量读取（getComputedStyle），
 * JS 端零硬编码 hex。切换 data-theme 后调用方重新 setOption 即可。
 *
 * 用法：
 *   import { chartTheme, chartPalette, mcColor } from '@/utils/echarts-theme';
 *   const t = chartTheme();
 *   chart.setOption({ ... , color: chartPalette() });
 */

/** 读取单个 CSS 变量（带 fallback） */
function cssVar(name, fallback) {
  if (typeof window === 'undefined') return fallback;
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return v || fallback;
}

/** 当前是否为浅色主题 */
export function isLightTheme() {
  return document.documentElement.getAttribute('data-theme') === 'light';
}

/** 10 色图表色板（来自 --chart-1 ~ --chart-10） */
export function chartPalette() {
  return [
    cssVar('--chart-1', '#818cf8'),
    cssVar('--chart-2', '#22d3ee'),
    cssVar('--chart-3', '#a78bfa'),
    cssVar('--chart-4', '#fbbf24'),
    cssVar('--chart-5', '#f472b6'),
    cssVar('--chart-6', '#34d399'),
    cssVar('--chart-7', '#fb923c'),
    cssVar('--chart-8', '#60a5fa'),
    cssVar('--chart-9', '#2dd4bf'),
    cssVar('--chart-10', '#e879f9')
  ];
}

/** 单个图表色（1-based） */
export function chartColor(i) {
  const p = chartPalette();
  return p[(i - 1 + p.length) % p.length];
}

/** 通用 ECharts 主题配置（坐标轴 / tooltip / 网格） */
export function chartTheme() {
  return {
    color: chartPalette(),
    axisColor: cssVar('--chart-axis-color', 'rgba(148,163,184,0.65)'),
    axisLine: cssVar('--chart-axis-line', 'rgba(129,140,248,0.14)'),
    gridColor: cssVar('--chart-grid-color', 'rgba(129,140,248,0.07)'),
    tooltip: {
      confine: true,
      backgroundColor: cssVar('--chart-tooltip-bg', 'rgba(15,20,27,0.97)'),
      borderColor: cssVar('--chart-tooltip-border', 'rgba(129,140,248,0.22)'),
      borderWidth: 1,
      textStyle: {
        color: cssVar('--chart-tooltip-fg', '#e2e8f0'),
        fontSize: 12
      },
      extraCssText: 'box-shadow: 0 8px 24px rgba(0,0,0,0.28); border-radius: 8px; padding: 10px 12px;'
    },
    textColor: cssVar('--color-text-primary', '#eceff4'),
    subTextColor: cssVar('--color-text-tertiary', '#626d7d')
  };
}

/** Metacritic 评分四色 */
export function mcColor(level) {
  const map = {
    mustplay: cssVar('--color-mc-mustplay', '#ffd43b'),
    positive: cssVar('--color-mc-positive', '#69db7c'),
    mixed: cssVar('--color-mc-mixed', '#ffa94d'),
    negative: cssVar('--color-mc-negative', '#ff8787')
  };
  return map[level] || map.mixed;
}

/** 构造带主题色的 tooltip（快速复用） */
export function themedTooltip(extra) {
  const t = chartTheme();
  return Object.assign({ trigger: 'axis' }, t.tooltip, extra || {});
}

/** 构造主题坐标轴 */
export function themedAxis(extra) {
  const t = chartTheme();
  return Object.assign({
    axisLine: { lineStyle: { color: t.axisLine } },
    axisTick: { show: false },
    axisLabel: { color: t.axisColor, fontSize: 11 },
    splitLine: { lineStyle: { color: t.gridColor } }
  }, extra || {});
}

export default {
  isLightTheme,
  chartPalette,
  chartColor,
  chartTheme,
  mcColor,
  themedTooltip,
  themedAxis
};
