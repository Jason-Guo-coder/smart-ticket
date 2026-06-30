<template>
  <div class="submit">
    <!-- 左：表单 -->
    <div class="glass-card form-card">
      <h3>工单信息</h3>
      <el-form label-position="top">
        <el-form-item label="工单标题">
          <el-input v-model="form.title" placeholder="例如：我的电脑连不上公司 WiFi" maxlength="128" />
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请详细描述故障现象、发生时间、影响范围…"
          />
        </el-form-item>
        <el-form-item label="图片（可选，填写图片 URL）">
          <el-input v-model="form.imageUrl" placeholder="https://…（MVP 仅存 URL）" />
        </el-form-item>
        <el-button
          class="submit-btn"
          type="primary"
          size="large"
          :loading="loading"
          @click="onSubmit"
        >
          {{ loading ? 'AI 智能分析中…' : '提交并智能分析' }}
        </el-button>
      </el-form>
    </div>

    <!-- 右：AI 智能分析面板 -->
    <div class="ai-card" :class="{ 'ai-card--idle': state === 'idle' }">
      <div class="ai-head">
        <div class="ai-spark"><el-icon><MagicStick /></el-icon></div>
        <div class="ai-title">
          <strong>AI 智能分析</strong>
          <span>基于工单描述自动生成</span>
        </div>
        <el-tag v-if="state === 'done'" type="success" size="small" round>已完成</el-tag>
        <el-tag v-else-if="state === 'loading'" type="primary" size="small" round>分析中</el-tag>
        <el-tag v-else-if="state === 'degraded'" type="info" size="small" round>已降级</el-tag>
      </div>

      <!-- 待分析 -->
      <div v-if="state === 'idle'" class="ai-empty">提交工单后，AI 将自动给出类别、优先级、派单建议与相似历史工单。</div>

      <!-- 分析中 -->
      <div v-else-if="state === 'loading'" class="ai-loading">
        <el-icon class="spin"><Loading /></el-icon>
        <span>AI 正在分析工单…</span>
      </div>

      <!-- 已降级 -->
      <div v-else-if="state === 'degraded'" class="ai-degraded">
        <el-icon><WarningFilled /></el-icon>
        <p>AI 暂时不可用，已转人工派单。工单已正常提交（{{ ticketNo }}），不影响处理。</p>
      </div>

      <!-- 已完成 -->
      <template v-else-if="state === 'done'">
        <div class="ai-rows">
          <div class="ai-row">
            <span>问题类别</span><el-tag round effect="light">{{ ai.categoryLabel }}</el-tag>
          </div>
          <div class="ai-row">
            <span>建议优先级</span>
            <el-tag round effect="light" :type="priType">{{ ai.priorityLabel }}</el-tag>
          </div>
          <div class="ai-row">
            <span>建议工程师</span>
            <el-tag round effect="light" type="success">{{ ai.suggestedEngineerName || '—' }}</el-tag>
          </div>
        </div>

        <div class="ai-similar">
          <div class="ai-similar__title">相似历史工单（{{ ai.similarTickets.length }}）</div>
          <StateEmpty v-if="!ai.similarTickets.length" text="暂无相似历史工单" />
          <div v-for="s in ai.similarTickets" :key="s.ticketNo" class="ai-sim">
            <div class="ai-sim__top">
              <span class="ai-sim__title">{{ s.title }}</span>
              <span class="ai-sim__no">{{ s.ticketNo }}</span>
            </div>
            <div class="ai-sim__sol">解决方案：{{ s.solution }}</div>
          </div>
        </div>

        <div class="ai-done-tip">工单 {{ ticketNo }} 已提交，等待管理员派单。</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { submitTicket } from '@/api/ticket'
import StateEmpty from '@/components/state/StateEmpty.vue'

const form = reactive({ title: '', content: '', imageUrl: '' })
const loading = ref(false)
const state = ref('idle') // idle | loading | done | degraded
const ai = ref({})
const ticketNo = ref('')

const priType = computed(() => {
  const p = ai.value.priority
  return p === 'HIGH' ? 'danger' : p === 'MID' ? 'warning' : 'success'
})

async function onSubmit() {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和问题描述')
    return
  }
  loading.value = true
  state.value = 'loading'
  try {
    const data = await submitTicket({ ...form })
    ticketNo.value = data.ticketNo
    ai.value = data.ai
    state.value = data.ai.degraded ? 'degraded' : 'done'
    ElMessage.success(`工单 ${data.ticketNo} 提交成功`)
  } catch (e) {
    state.value = 'idle'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.submit {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}
.form-card {
  padding: 26px;
}
.form-card h3 {
  font-size: 18px;
  margin-bottom: 16px;
}
.submit-btn {
  width: 100%;
  border-radius: var(--radius-control);
  margin-top: 8px;
}
/* AI 面板（蓝紫渐变，DESIGN §2.6） */
.ai-card {
  padding: 24px;
  border-radius: var(--radius-card);
  border: 1px solid rgba(140, 120, 240, 0.35);
  background: linear-gradient(160deg, rgba(237, 242, 255, 0.9), rgba(245, 237, 255, 0.85));
  box-shadow: var(--shadow-card);
}
.ai-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.ai-spark {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #8c66f0, #4d80ff);
}
.ai-title {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  flex: 1;
}
.ai-title span {
  font-size: 11px;
  color: var(--apple-darkgray);
}
.ai-empty,
.ai-degraded p {
  font-size: 13px;
  color: var(--apple-darkgray);
  line-height: 1.7;
}
.ai-degraded {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  color: #b5852a;
}
.ai-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 0;
  color: var(--apple-blue);
}
.spin {
  animation: rot 1s linear infinite;
}
@keyframes rot {
  to {
    transform: rotate(360deg);
  }
}
.ai-rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ai-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  color: var(--apple-darkgray);
}
.ai-similar {
  margin-top: 16px;
}
.ai-similar__title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
}
.ai-sim {
  padding: 11px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.65);
  margin-bottom: 8px;
}
.ai-sim__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ai-sim__title {
  font-size: 13px;
  font-weight: 500;
}
.ai-sim__no {
  font-size: 11px;
  color: var(--apple-darkgray);
}
.ai-sim__sol {
  font-size: 12px;
  color: var(--apple-darkgray);
  margin-top: 4px;
}
.ai-done-tip {
  margin-top: 14px;
  font-size: 12px;
  color: var(--apple-blue);
}
</style>
