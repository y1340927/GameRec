<template>
  <section class="panel" :class="{ 'panel--borderless': borderless }">
    <div v-if="title || $slots.header" class="panel-header">
      <h3 v-if="title && !$slots.header" class="panel-title">{{ title }}</h3>
      <slot name="header"></slot>
    </div>
    <div class="panel-body" :class="{ 'panel-body--padded': padded }">
      <slot></slot>
    </div>
  </section>
</template>

<script>
export default {
  name: 'Panel',
  props: {
    title:      { type: String,  default: '' },
    borderless: { type: Boolean, default: false },
    padded:     { type: Boolean, default: true }
  }
};
</script>

<style scoped>
.panel {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-2xl);
  overflow: hidden;
  transition: border-color var(--duration-fast) var(--ease-out), box-shadow var(--duration-fast) var(--ease-out);
}
.panel:hover {
  border-color: var(--color-border-default);
}
.panel--borderless {
  border: none;
  background: linear-gradient(135deg, var(--color-bg-elevated), var(--color-bg-overlay));
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--color-border-muted);
  background: linear-gradient(180deg, rgba(255,255,255,0.01), transparent);
}
.panel-title {
  font-size: var(--text-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  letter-spacing: var(--letter-spacing-normal);
}
.panel-body {
  overflow: hidden;
}
.panel-body--padded {
  padding: var(--space-5) var(--space-6);
}

@media (max-width: 767px) {
  .panel-header { padding: var(--space-3) var(--space-4); }
  .panel-body--padded { padding: var(--space-4); }
}
</style>
