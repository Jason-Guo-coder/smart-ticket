<template>
  <div class="handle">
    <div class="back" @click="$router.push('/app/todo')">
      <el-icon><ArrowLeft /></el-icon><span>返回待办</span>
    </div>

    <StateLoading v-if="status === 'loading'" :rows="8" />
    <StateError v-else-if="status === 'error'" :text="errText" @retry="load" />

    <div v-else class="grid">
      <!-- 左：工单 + 处理 -->
      <div class="col">
        <div class="glass-card block">
          <div class="head__top">
            <span class="badge" :style="{ background: sMeta.color }">{{ sMeta.label }}</span>
            <span v-if="d.priorityLabel" class="pri" :class="'pri--' + d.priority">{{ d.priorityLabel }}</span>
            <span v-if="d.categoryLabel" class="cat">{{ d.categoryLabel }}</span>
            <span class="no">{{ d.ticketNo }}</span>
          </div>
          <h2 class="title">{{ d.title }}</h2>
          <p class="content">{{ d.content }}</p>
          <a v-if="d.imageUrl" class="img-link" :href="d.imageUrl" target="_blank" rel="noopener">查看附件图片</a>
        </div>

        <!-- 处理中：解决方案表单 -->
        <div v-if="d.status === 'PROCESSING'" class="glass-card block">
          <h3>填写解决方案</h3>
          <el-input
            v-model="form.solutionText"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="记录排查过程与最终解决方案…"
          />
          <el-input v-model="form.imageUrl" class="mt" placeholder="处理截图 URL（可选）" />
          <div class="acts">
            <el-button type="primary" :loading="submitting" @click="onSubmit">提交完成</el-button>
            <el-button :loading="reassigning" @click="onReassign">转派</el-button>
          </div>
        </div>

        <!-- 待验收：只读提示 -->
        <div v-else-if="d.status === 'ACCEPTING'" class="glass-card block tip">
          <el-icon><Clock /></el-icon>
          <span>已提交解决方案，等待报修人验收。</span>
        </div>

        <!-- 时间线 -->
        <div class="glass-card block">
          <h3>处理时间线</h3>
          <el-timeline v-if="d.timeline && d.timeline.length">
            <el-timeline-item
              v-for="(l, i) in d.timeline"
              :key="i"
              :timestamp="fmtTime(l.createTime)"
              placement="top"
              :hollow="i !== 0"
              :color="i === 0 ? 'var(--apple-blue)' : ''"
            >
              <div class="tl">
                <span class="tl__action">{{ l.action }}</span>
                <span class="tl__op">{{ l.operatorName || '系统' }}</span>
              </div>
              <div v-if="l.remark" class="tl__remark">{{ l.remark }}</div>
            </el-timeline-item>
          </el-timeline>
          <StateEmpty v-else text="暂无处理记录" />
        </div>
      </div>

      <!-- 右：AI 参考 -->
      <div class="col">
        <div class="ai-card">
          <div class="ai-head">
            <div class="ai-spark"><el-icon><MagicStick /></el-icon></div>
            <div class="ai-title"><strong>AI 参考</strong><span>同类别历史解决方案</span></div>
          </div>
          <StateEmpty v-if="!similar.length" text="暂无相似历史工单" />
          <div v-for="s in similar" :key="s.ticketNo" class="sim">
            <div class="sim__top">
              <span class="sim__title">{{ s.title }}</span>
              <span class="sim__no">{{ s.ticketNo }}</span>
            </div>
            <div class="sim__sol">解决方案：{{ s.solution }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ticketDetail, handleSimilar, submitSolution, reassignTicket } from '@/api/ticket'
import { statusMeta } from '@/utils/ticket'
import StateLoading from '@/components/state/StateLoading.vue'
import StateEmpty from '@/components/state/StateEmpty.vue'
import StateError from '@/components/state/StateError.vue'

const route = useRoute()
const router = useRouter()
const id = route.params.id

const d = ref({})
const similar = ref([])
const status = ref('loading') // loading | ready | error
const errText = ref('工单加载失败，请稍后再试')
const form = reactive({ solutionText: '', imageUrl: '' })
const submitting = ref(false)
const reassigning = ref(false)

const sMeta = computed(() => statusMeta(d.value.status))

async function load() {
  status.value = 'loading'
  try {
    const [detail, sim] = await Promise.all([ticketDetail(id), handleSimilar(id).catch(() => [])])
    d.value = detail
    similar.value = sim || []
    status.value = 'ready'
  } catch (e) {
    const m = e?.message
    errText.value = m && !/^Request failed/.test(m) ? m : '工单加载失败，请稍后再试'
    status.value = 'error'
  }
}

async function onSubmit() {
  if (!form.solutionText.trim()) {
    ElMessage.warning('请填写解决方案')
    return
  }
  submitting.value = true
  try {
    await submitSolution(id, { solutionText: form.solutionText, imageUrl: form.imageUrl || undefined })
    ElMessage.success('已提交完成，待报修人验收')
    router.push('/app/todo')
  } catch (e) {
    load() // 状态可能已变，刷新
  } finally {
    submitting.value = false
  }
}

async function onReassign() {
  try {
    const { value } = await ElMessageBox.prompt('请填写转派原因（可选）', '转派工单', {
      confirmButtonText: '确认转派',
      cancelButtonText: '取消',
      inputPlaceholder: '如：非本人技能范围'
    })
    reassigning.value = true
    await reassignTicket(id, { reason: value || undefined })
    ElMessage.success('已转派，退回待派单')
    router.push('/app/todo')
  } catch (e) {
    if (e !== 'cancel') load()
  } finally {
    reassigning.value = false
  }
}

function fmtTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : '—'
}

load()
</script>

<style scoped>
.handle {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: fit-content;
  color: var(--apple-darkgray);
  font-size: 14px;
  cursor: pointer;
}
.back:hover {
  color: var(--apple-blue);
}
.grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
  align-items: start;
}
.col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.block {
  padding: 20px 24px;
}
.head__top {
  display: flex;
  align-items: center;
  gap: 10px;
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
.title {
  font-size: 20px;
  margin: 12px 0;
}
.content {
  font-size: 14px;
  line-height: 1.8;
  color: #3a3a3c;
  white-space: pre-wrap;
}
.img-link {
  display: inline-block;
  margin-top: 10px;
  font-size: 13px;
  color: var(--apple-blue);
}
.block h3 {
  font-size: 16px;
  margin-bottom: 14px;
}
.mt {
  margin-top: 10px;
}
.acts {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}
.tip {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--status-accepting);
  font-size: 14px;
}
.tl {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tl__action {
  font-size: 14px;
  font-weight: 500;
}
.tl__op {
  font-size: 12px;
  color: var(--apple-darkgray);
}
.tl__remark {
  font-size: 13px;
  color: var(--apple-darkgray);
  margin-top: 4px;
}
/* AI 参考卡（蓝紫渐变，与提交页一致） */
.ai-card {
  padding: 20px 22px;
  border-radius: var(--radius-card);
  border: 1px solid rgba(140, 120, 240, 0.35);
  background: linear-gradient(160deg, rgba(237, 242, 255, 0.9), rgba(245, 237, 255, 0.85));
  box-shadow: var(--shadow-card);
}
.ai-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
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
}
.ai-title span {
  font-size: 11px;
  color: var(--apple-darkgray);
}
.sim {
  padding: 11px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.65);
  margin-bottom: 8px;
}
.sim__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sim__title {
  font-size: 13px;
  font-weight: 500;
}
.sim__no {
  font-size: 11px;
  color: var(--apple-darkgray);
}
.sim__sol {
  font-size: 12px;
  color: var(--apple-darkgray);
  margin-top: 4px;
}
</style>
