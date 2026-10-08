<template>
  <div class="empty-state">
    <div class="empty-icon">
      <slot name="icon">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none" aria-hidden="true">
          <rect x="6" y="6" width="36" height="36" rx="10" stroke="currentColor" stroke-width="1.5" opacity="0.2"/>
          <circle cx="24" cy="24" r="5" stroke="currentColor" stroke-width="1.5" opacity="0.3"/>
        </svg>
      </slot>
    </div>
    <p class="empty-title">{{ text }}</p>
    <p v-if="hint" class="empty-hint">{{ hint }}</p>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action"></slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EmptyState',
  props: {
    text:    { type: String, default: '暂无数据' },
    hint:    { type: String, default: '' },
    loaded:  { type: Boolean, default: false },
    hasData: { type: Boolean, default: false }
  }
};
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) var(--space-6);
  text-align: center;
  animation: empty-in 0.5s var(--ease-out-expo);
}
@keyframes empty-in {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}
.empty-icon {
  margin-bottom: var(--space-5);
  color: var(--color-text-tertiary);
  opacity: 0.5;
}
.empty-title {
  font-size: var(--text-md);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
}
.empty-hint {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  max-width: 320px;
  line-height: var(--line-height-relaxed);
}
.empty-action {
  margin-top: var(--space-6);
}

@media (max-width: 640px) {
  .empty-state { padding: var(--space-10) var(--space-4); }
}
</style>
