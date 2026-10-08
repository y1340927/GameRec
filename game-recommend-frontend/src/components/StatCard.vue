<template>
  <div class="stat-card" :class="`stat-card--${variant}`">
    <div class="stat-card-bg"></div>
    <div class="stat-card-content">
      <div class="stat-header">
        <div class="stat-icon" :class="iconClass">
          <slot name="icon">
            <span v-html="icon"></span>
          </slot>
        </div>
        <span class="stat-label">{{ label }}</span>
      </div>
      <div class="stat-value" :class="valueClass" :style="valueStyle">
        <span class="stat-value-number">{{ displayValue }}</span>
      </div>
      <div v-if="subText" class="stat-sub">{{ subText }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StatCard',
  props: {
    label:        { type: String,  required: true },
    displayValue: { type: [String, Number], default: '--' },
    subText:      { type: String,  default: '' },
    icon:         { type: String,  default: '' },
    variant:      { type: String,  default: 'default', validator: v => ['default','primary','gold','cyan','green','purple'].includes(v) },
    iconClass:    { type: String,  default: '' },
    valueClass:   { type: String,  default: '' },
    valueStyle:   { type: Object,  default: () => ({}) }
  }
};
</script>

<style scoped>
.stat-card {
  position: relative;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-2xl);
  padding: var(--space-5) var(--space-6);
  overflow: hidden;
  transition: all var(--duration-normal) var(--ease-out);
}
.stat-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-border-default);
  box-shadow: var(--shadow-lg);
}
.stat-card-bg {
  position: absolute;
  top: -20px; right: -20px;
  width: 100px; height: 100px;
  border-radius: 50%;
  opacity: 0.06;
  pointer-events: none;
}

.stat-card--primary .stat-card-bg { background: var(--color-brand-500); }
.stat-card--gold    .stat-card-bg { background: var(--color-warm-400); }
.stat-card--cyan    .stat-card-bg { background: var(--color-accent-500); }
.stat-card--green   .stat-card-bg { background: var(--color-success); }
.stat-card--purple  .stat-card-bg { background: #a855f7; }

.stat-card-content { position: relative; z-index: 1; }

.stat-header {
  display: flex; align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
.stat-icon {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px;
  border-radius: var(--radius-lg);
  font-size: var(--text-md);
  flex-shrink: 0;
}
.stat-card--default .stat-icon  { background: var(--color-bg-subtle);  color: var(--color-text-secondary); }
.stat-card--primary .stat-icon  { background: rgba(99,102,241,0.12);  color: var(--color-brand-400); }
.stat-card--gold .stat-icon     { background: rgba(245,158,11,0.12);  color: var(--color-warm-400); }
.stat-card--cyan .stat-icon     { background: rgba(6,182,212,0.12);   color: var(--color-accent-500); }
.stat-card--green .stat-icon    { background: var(--color-success-bg); color: var(--color-success); }
.stat-card--purple .stat-icon   { background: rgba(168,85,247,0.12);  color: #c084fc; }

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.stat-value {
  display: flex; align-items: baseline;
  gap: var(--space-1);
}
.stat-value-number {
  font-family: var(--font-family-display);
  font-size: var(--text-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: var(--line-height-tight);
  letter-spacing: var(--letter-spacing-tight);
}
.stat-sub {
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

@media (max-width: 767px) {
  .stat-card { padding: var(--space-4); }
  .stat-value-number { font-size: var(--text-xl); }
}
</style>
