<template>
  <div class="placeholder">
    <div class="glass-card welcome">
      <h2>骨架已就绪 🎉</h2>
      <p>AppShell（侧边栏 + 顶栏 + 玻璃卡 + blob 背景）与三态组件已可用。各业务页将在后续 Phase 挂载到此处。</p>
    </div>

    <div class="glass-card demo">
      <div class="demo__head">
        <span>三态组件演示</span>
        <el-radio-group v-model="state" size="small">
          <el-radio-button value="normal">正常</el-radio-button>
          <el-radio-button value="loading">加载中</el-radio-button>
          <el-radio-button value="empty">空数据</el-radio-button>
          <el-radio-button value="error">报错</el-radio-button>
        </el-radio-group>
      </div>
      <div class="demo__body">
        <StateLoading v-if="state === 'loading'" :rows="4" />
        <StateEmpty v-else-if="state === 'empty'" text="还没有工单，去提交一个吧" />
        <StateError v-else-if="state === 'error'" text="加载失败，请稍后再试" @retry="onRetry" />
        <div v-else class="demo__normal">内容区正常渲染。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import StateLoading from '@/components/state/StateLoading.vue'
import StateEmpty from '@/components/state/StateEmpty.vue'
import StateError from '@/components/state/StateError.vue'

const state = ref('normal')
function onRetry() {
  ElMessage.success('已触发重试')
  state.value = 'normal'
}
</script>

<style scoped>
.placeholder {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.welcome {
  padding: var(--gap-card);
}
.welcome h2 {
  font-size: 20px;
  margin-bottom: 8px;
}
.welcome p {
  color: var(--apple-darkgray);
  font-size: 14px;
  line-height: 1.7;
}
.demo {
  padding: var(--gap-card);
}
.demo__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  font-weight: 700;
}
.demo__normal {
  padding: 24px 4px;
  color: var(--apple-darkgray);
}
</style>
