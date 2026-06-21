<template>
  <div class="state-loading">
    <el-skeleton :rows="rows" animated />
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 5 }
})
</script>

<style scoped>
.state-loading {
  padding: 8px 4px;
}
</style>
