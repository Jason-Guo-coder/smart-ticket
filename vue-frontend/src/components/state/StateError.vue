<template>
  <div class="state-error">
    <el-icon class="state-error__icon"><WarningFilled /></el-icon>
    <p class="state-error__text">{{ text }}</p>
    <el-button class="state-error__btn" plain type="danger" @click="$emit('retry')">
      重试
    </el-button>
  </div>
</template>

<script setup>
defineProps({
  text: { type: String, default: '加载失败，请稍后再试' }
})
defineEmits(['retry'])
</script>

<style scoped>
.state-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  gap: 12px;
}
.state-error__icon {
  font-size: 44px;
  color: var(--status-rejected);
}
.state-error__text {
  font-size: 14px;
  color: var(--apple-darkgray);
}
</style>
