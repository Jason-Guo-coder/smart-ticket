<template>
  <div class="my-tickets">
    <!-- 筛选栏 -->
    <div class="glass-card filter">
      <el-select v-model="query.status" placeholder="全部状态" clearable class="filter__status" @change="reload">
        <el-option v-for="o in statusOpts" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="搜索标题 / 工单号"
        clearable
        class="filter__kw"
        @keyup.enter="reload"
        @clear="reload"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="reload">查询</el-button>
    </div>

    <!-- 加载中 -->
    <StateLoading v-if="status === 'loading'" :rows="6" />

    <!-- 报错 -->
    <StateError v-else-if="status === 'error'" text="工单加载失败，请稍后再试" @retry="load" />

    <!-- 空态 -->
    <StateEmpty v-else-if="!list.length" text="还没有工单">
      <template #action>
        <el-button type="primary" @click="$router.push('/app/submit')">去提交工单</el-button>
      </template>
    </StateEmpty>

    <!-- 主流程：工单列表 -->
    <template v-else>
      <div class="list">
        <div
          v-for="t in list"
          :key="t.id"
          class="glass-card hover-lift card"
          @click="goDetail(t.id)"
        >
          <div class="card__main">
            <div class="card__top">
              <span class="badge" :style="{ background: statusMeta(t.status).color }">
                {{ statusMeta(t.status).label }}
              </span>
              <span v-if="t.priorityLabel" class="pri" :class="'pri--' + t.priority">
                {{ t.priorityLabel }}
              </span>
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
          <el-icon class="card__arrow"><ArrowRight /></el-icon>
        </div>
      </div>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          @current-change="onPage"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { myTickets } from '@/api/ticket'
import { statusMeta, statusOptions, slaMeta } from '@/utils/ticket'
import StateLoading from '@/components/state/StateLoading.vue'
import StateEmpty from '@/components/state/StateEmpty.vue'
import StateError from '@/components/state/StateError.vue'

const router = useRouter()
const statusOpts = statusOptions()

const query = reactive({ pageNum: 1, pageSize: 10, status: '', keyword: '' })
const list = ref([])
const total = ref(0)
const status = ref('loading') // loading | ready | error

async function load() {
  status.value = 'loading'
  try {
    const data = await myTickets({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status || undefined,
      keyword: query.keyword || undefined
    })
    list.value = data.list || []
    total.value = data.total || 0
    status.value = 'ready'
  } catch (e) {
    status.value = 'error'
  }
}

function reload() {
  query.pageNum = 1
  load()
}

function onPage(p) {
  query.pageNum = p
  load()
}

function goDetail(id) {
  router.push(`/app/tickets/${id}`)
}

function fmtTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : '—'
}

load()
</script>

<style scoped>
.my-tickets {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter {
  display: flex;
  gap: 12px;
  padding: 16px 18px;
  align-items: center;
}
.filter__status {
  width: 150px;
}
.filter__kw {
  width: 280px;
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
  cursor: pointer;
}
.card__main {
  flex: 1;
  min-width: 0;
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
.card__arrow {
  color: #c8c8cc;
  font-size: 18px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 4px 0 8px;
}
</style>
