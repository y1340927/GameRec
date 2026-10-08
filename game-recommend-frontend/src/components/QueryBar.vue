<template>
  <div class="query-bar">
    <slot></slot>
  </div>
</template>

<script>
export default { name: 'QueryBar' };
</script>

<style scoped>
.query-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  margin-bottom: var(--space-6);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-2xl);
}
@media (max-width: 640px) {
  .query-bar {
    flex-direction: column;
    align-items: stretch;
    padding: var(--space-3) var(--space-4);
  }
}
</style>
