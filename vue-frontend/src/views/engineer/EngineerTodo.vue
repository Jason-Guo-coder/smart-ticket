<template>
  <div class="todo">
    <!-- 统计条 -->
    <div class="stats">
      <div class="glass-card stat">
        <div class="stat__num">{{ stats.pending }}</div>
        <div class="stat__label">待接单</div>
      </div>
      <div class="glass-card stat">
        <div class="stat__num" style="color: var(--status-processing)">{{ stats.processing }}</div>
        <div class="stat__label">我处理中</div>
      </div>
      <div class="glass-card stat">
        <div class="stat__num" style="color: var(--status-done)">{{ stats.doneToday }}</div>
        <div class="stat__label">今日完成</div>
      </div>
      <div class="glass-card stat">
        <div class="stat__num">{{ stats.avgHours }}<span class="stat__unit">h</span></div>
        <div class="stat__label">平均处理时长</div>
      </div>
    </div>

    <!-- 加载中 -->
    <StateLoading v-if="status === 'loading'" :rows="6" />
    <!-- 报错 -->
    <StateError v-else-if="status === 'error'" text="待办加载失败，请稍后再试" @retry="load" />
    <!-- 空态 -->
    <StateEmpty v-else-if="!list.length" text="太棒了，暂无待办工单" />

    <!-- 待办列表 -->
    <div v-else class="list">
      <div v-for="t in list" :key="t.id" class="glass-card hover-lift card">
        <div class="card__main" @click="goHandle(t)">
          <div class="card__top">
            <span class="badge" :style="{ background: statusMeta(t.status).color }">
              {{ statusMeta(t.status).label }}
            </span>
            <span v-if="t.priorityLabel" class="pri" :class="'pri--' + t.priority">{{ t.priorityLabel }}</span>
            <span v-if="t.categoryLabel" class="cat">{{ t.categoryLabel }}</span>
            <span class="no">{{ t.ticketNo }}</span>
          </div>
          <div class="card__title">{{ t.title }}</div>
          <div class="card__meta">
            <span>提交于 {{ fmtTime(t.createTime) }}</span>
            <span class="sla" :style="{ color: slaMeta(t.slaDeadline, t.slaOverdue).color }">
              {{ slaMeta(t.slaDeadline, t.slaOverdue).text }}
            </span>
          </div>
        </div>
        <div class="card__act">
          <el-button
            v-if="t.status === 'PENDING'"
            type="primary"
            :loading="acceptingId === t.id"
            @click="onAccept(t)"
          >接单</el-button>
          <el-button v-else type="primary" plain @click="goHandle(t)">去处理</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { engineerTodo, acceptTicket } from '@/api/ticket'
import { statusMeta, slaMeta } from '@/utils/ticket'
import StateLoading from '@/components/state/StateLoading.vue'
import StateEmpty from '@/components/state/StateEmpty.vue'
import StateError from '@/components/state/StateError.vue'

const router = useRouter()
const stats = reactive({ pending: 0, processing: 0, doneToday: 0, avgHours: 0 })
const list = ref([])
const status = ref('loading') // loading | ready | error
const acceptingId = ref(null)

async function load() {
  status.value = 'loading'
  try {
    const data = await engineerTodo()
    Object.assign(stats, data.stats)
    list.value = data.list || []
    status.value = 'ready'
  } catch (e) {
    status.value = 'error'
  }
}

async function onAccept(t) {
  acceptingId.value = t.id
  try {
    await acceptTicket(t.id)
    ElMessage.success(`已接单 ${t.ticketNo}`)
    router.push(`/app/handle/${t.id}`)
  } catch (e) {
    // 并发抢单失败等 → request.js 已提示；刷新待办
    load()
  } finally {
    acceptingId.value = null
  }
}

function goHandle(t) {
  if (t.status === 'PENDING') return // 待接单需先接单
  router.push(`/app/handle/${t.id}`)
}

function fmtTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : '—'
}

load()
</script>

<style scoped>
.todo {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat {
  padding: 18px 20px;
}
.stat__num {
  font-size: 28px;
  font-weight: 700;
}
.stat__unit {
  font-size: 15px;
  font-weight: 500;
  margin-left: 2px;
  color: var(--apple-darkgray);
}
.stat__label {
  font-size: 13px;
  color: var(--apple-darkgray);
  margin-top: 4px;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
}
.card__main {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}
.card__top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.badge {
  color: #fff;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: var(--radius-badge);
  font-weight: 500;
}
.pri {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--radius-badge);
}
.pri--HIGH {
  color: var(--priority-high);
  background: rgba(255, 59, 48, 0.1);
}
.pri--MID {
  color: var(--priority-mid);
  background: rgba(245, 166, 35, 0.12);
}
.pri--LOW {
  color: var(--priority-low);
  background: rgba(52, 199, 89, 0.12);
}
.cat {
  font-size: 12px;
  color: var(--apple-darkgray);
  background: var(--apple-gray);
  padding: 2px 8px;
  border-radius: var(--radius-badge);
}
.no {
  margin-left: auto;
  font-size: 12px;
  color: var(--apple-darkgray);
}
.card__title {
  font-size: 15px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card__meta {
  display: flex;
  gap: 16px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--apple-darkgray);
}
.sla {
  font-weight: 500;
}
</style>
