<template>
  <div class="state-empty">
    <el-icon class="state-empty__icon"><Box /></el-icon>
    <p class="state-empty__text">{{ text }}</p>
    <slot name="action" />
  </div>
</template>

<script setup>
defineProps({
  text: { type: String, default: '暂无数据' }
})
</script>

<style scoped>
.state-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  gap: 12px;
}
.state-empty__icon {
  font-size: 48px;
  color: #c8c8cc;
}
.state-empty__text {
  font-size: 14px;
  color: var(--apple-darkgray);
}
</style>
