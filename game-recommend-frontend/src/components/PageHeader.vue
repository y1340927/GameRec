<template>
  <div class="page-header">
    <div class="page-header-content">
      <div class="page-header-top">
        <slot name="before-title"></slot>
        <h1 class="page-title">{{ title }}</h1>
      </div>
      <p v-if="description" class="page-description">{{ description }}</p>
    </div>
    <div class="page-header-accent"></div>
  </div>
</template>

<script>
export default {
  name: 'PageHeader',
  props: {
    title:       { type: String, required: true },
    description: { type: String, default: '' }
  }
};
</script>

<style scoped>
.page-header {
  margin-bottom: var(--space-8);
  position: relative;
}
.page-header-content {
  position: relative;
  z-index: 1;
}
.page-header-top {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.page-title {
  font-family: var(--font-family-display);
  font-size: var(--text-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  letter-spacing: var(--letter-spacing-tight);
  line-height: var(--line-height-tight);
}
.page-description {
  margin-top: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  line-height: var(--line-height-relaxed);
  max-width: 600px;
}
.page-header-accent {
  position: absolute;
  top: -20px;
  left: -20px;
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(99,102,241,0.08), transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

@media (max-width: 767px) {
  .page-header { margin-bottom: var(--space-6); }
  .page-title { font-size: var(--text-xl); }
}
</style>
