<template>
  <div class="dispatch">
    <div class="bar">
      <div class="bar__title">待派单 <span>{{ pending.length }}</span> 单</div>
      <el-button
        type="primary"
        :loading="autoAllLoading"
        :disabled="!pending.length"
        @click="onAutoAll"
      >一键智能分派全部</el-button>
    </div>

    <div class="layout">
      <!-- 左：待派单卡片 -->
      <div class="col-main">
        <StateLoading v-if="status === 'loading'" :rows="6" />
        <StateError v-else-if="status === 'error'" text="派单数据加载失败" @retry="load" />
        <StateEmpty v-else-if="!pending.length" text="暂无待派单工单，全部已分派 🎉" />
        <div v-else class="cards">
          <div v-for="t in pending" :key="t.id" class="glass-card card">
            <div class="card__top">
              <span class="badge">待派单</span>
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

            <div class="card__ai">
              <el-icon><MagicStick /></el-icon>
              <span v-if="t.suggestedEngineerName">AI 建议派给 <b>{{ t.suggestedEngineerName }}</b></span>
              <span v-else class="muted">暂无 AI 建议（未识别类别）</span>
            </div>

            <div class="card__acts">
              <el-button
                type="primary"
                size="small"
                :loading="busyId === t.id"
                :disabled="!t.suggestedEngineerId"
                @click="onAdopt(t)"
              >采纳建议</el-button>
              <el-button size="small" :loading="busyId === t.id" @click="onAuto(t)">自动分配</el-button>
              <el-dropdown trigger="click" @command="(eid) => onManual(t, eid)">
                <el-button size="small" plain>
                  手动指派<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="e in engineers" :key="e.userId" :command="e.userId">
                      {{ e.realName }}（负载 {{ e.currentLoad }}）
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>

      <!-- 右：工程师负载面板 -->
      <div class="col-side">
        <div class="glass-card panel">
          <h3>工程师负载</h3>
          <StateEmpty v-if="!engineers.length" text="暂无工程师" />
          <div v-for="e in engineers" :key="e.userId" class="eng">
            <div class="eng__head">
              <span class="eng__name">{{ e.realName }}</span>
              <span class="eng__load">{{ e.currentLoad }}</span>
            </div>
            <div class="eng__bar">
              <div class="eng__fill" :style="{ width: barWidth(e.currentLoad) }"></div>
            </div>
            <div class="eng__skills">
              <span v-for="c in skillList(e.categorySkills)" :key="c" class="skill">{{ c }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dispatchBoard, assignTicket, autoAssign, autoAssignAll } from '@/api/dispatch'
import { slaMeta } from '@/utils/ticket'
import StateLoading from '@/components/state/StateLoading.vue'
import StateEmpty from '@/components/state/StateEmpty.vue'
import StateError from '@/components/state/StateError.vue'

const CAT = { NETWORK: '网络', HARDWARE: '硬件', ACCOUNT: '账号', SOFTWARE: '软件', LOGISTICS: '后勤' }

const pending = ref([])
const engineers = ref([])
const status = ref('loading') // loading | ready | error
const busyId = ref(null)
const autoAllLoading = ref(false)

const maxLoad = computed(() => Math.max(1, ...engineers.value.map((e) => e.currentLoad || 0)))

async function load() {
  status.value = 'loading'
  try {
    const data = await dispatchBoard()
    pending.value = data.pending || []
    engineers.value = data.engineers || []
    status.value = 'ready'
  } catch (e) {
    status.value = 'error'
  }
}

async function onAdopt(t) {
  await doAssign(t, () => assignTicket(t.id, t.suggestedEngineerId), `已采纳建议，派给 ${t.suggestedEngineerName}`)
}
async function onManual(t, engineerId) {
  await doAssign(t, () => assignTicket(t.id, engineerId), '已手动指派')
}
async function onAuto(t) {
  await doAssign(t, async () => {
    const name = await autoAssign(t.id)
    ElMessage.success(`已自动分配给 ${name}`)
  })
}

async function doAssign(t, fn, okMsg) {
  busyId.value = t.id
  try {
    await fn()
    if (okMsg) ElMessage.success(okMsg)
    await load()
  } catch (e) {
    await load() // 并发被抢/状态已变 → 刷新
  } finally {
    busyId.value = null
  }
}

async function onAutoAll() {
  autoAllLoading.value = true
  try {
    const r = await autoAssignAll()
    ElMessage.success(`分派完成：成功 ${r.success} / 失败 ${r.failed}，共 ${r.total}`)
    await load()
  } catch (e) {
    await load()
  } finally {
    autoAllLoading.value = false
  }
}

function barWidth(load) {
  return Math.round(((load || 0) / maxLoad.value) * 100) + '%'
}
function skillList(s) {
  return (s || '').split(',').filter(Boolean).map((c) => CAT[c] || c)
}
function fmtTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : '—'
}

load()
</script>

<style scoped>
.dispatch {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.bar__title {
  font-size: 16px;
  font-weight: 600;
}
.bar__title span {
  color: var(--apple-blue);
}
.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 16px;
  align-items: start;
}
.cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.card {
  padding: 16px 18px;
}
.card__top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.badge {
  color: #fff;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: var(--radius-badge);
  font-weight: 500;
  background: var(--status-pending);
}
.pri {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--radius-badge);
}
.pri--HIGH { color: var(--priority-high); background: rgba(255, 59, 48, 0.1); }
.pri--MID { color: var(--priority-mid); background: rgba(245, 166, 35, 0.12); }
.pri--LOW { color: var(--priority-low); background: rgba(52, 199, 89, 0.12); }
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
  margin: 10px 0 6px;
}
.card__meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--apple-darkgray);
}
.sla {
  font-weight: 500;
}
.card__ai {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 12px 0;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(140, 102, 240, 0.08);
  color: #6b4fd0;
  font-size: 13px;
}
.card__ai .muted {
  color: var(--apple-darkgray);
}
.card__acts {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
/* 负载面板 */
.panel {
  padding: 18px 20px;
}
.panel h3 {
  font-size: 15px;
  margin-bottom: 14px;
}
.eng {
  margin-bottom: 16px;
}
.eng__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.eng__name {
  font-size: 14px;
  font-weight: 500;
}
.eng__load {
  font-size: 13px;
  color: var(--apple-blue);
  font-weight: 600;
}
.eng__bar {
  height: 6px;
  border-radius: 3px;
  background: var(--apple-gray);
  overflow: hidden;
}
.eng__fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #4d80f0, #8c66f0);
  transition: width 0.3s ease;
}
.eng__skills {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.skill {
  font-size: 11px;
  color: var(--apple-darkgray);
  background: var(--apple-gray);
  padding: 1px 7px;
  border-radius: 6px;
}
</style>
