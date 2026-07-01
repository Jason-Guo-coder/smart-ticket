<template>
  <div class="detail">
    <div class="back" @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon><span>返回</span>
    </div>

    <!-- 加载中 -->
    <StateLoading v-if="status === 'loading'" :rows="8" />

    <!-- 报错 -->
    <StateError v-else-if="status === 'error'" :text="errText" @retry="load" />

    <!-- 主流程 -->
    <template v-else>
      <!-- 表头 -->
      <div class="glass-card head">
        <div class="head__top">
          <span class="badge" :style="{ background: sMeta.color }">{{ sMeta.label }}</span>
          <span v-if="d.priorityLabel" class="pri" :class="'pri--' + d.priority">{{ d.priorityLabel }}</span>
          <span v-if="d.categoryLabel" class="cat">{{ d.categoryLabel }}</span>
          <span class="no">{{ d.ticketNo }}</span>
        </div>
        <h2 class="head__title">{{ d.title }}</h2>
        <div class="head__grid">
          <div><label>报修人</label><span>{{ d.creatorName || '—' }}</span></div>
          <div><label>受理工程师</label><span>{{ d.assigneeName || '未派单' }}</span></div>
          <div><label>提交时间</label><span>{{ fmtTime(d.createTime) }}</span></div>
          <div>
            <label>SLA 时限</label>
            <span class="sla" :style="{ color: slaLive.color }">{{ slaLive.text }}</span>
          </div>
        </div>
      </div>

      <!-- 报修人验收（待验收时） -->
      <div v-if="canVerify" class="glass-card verify">
        <div class="verify__text">
          <strong>工程师已提交解决方案</strong>
          <span>请确认问题是否已解决</span>
        </div>
        <div class="verify__acts">
          <el-button :loading="verifying" @click="onReject">驳回</el-button>
          <el-button type="primary" :loading="verifying" @click="onPass">验收通过</el-button>
        </div>
      </div>

      <!-- 报修人评价入口（已完成时） -->
      <div v-if="canEvaluate" class="glass-card verify">
        <div class="verify__text">
          <strong>工单已完成</strong>
          <span>对本次服务打个分吧</span>
        </div>
        <div class="verify__acts">
          <el-button type="primary" @click="evalVisible = true">评价服务</el-button>
        </div>
      </div>

      <!-- 已评价展示 -->
      <div v-if="d.evalScore" class="glass-card block">
        <h3>服务评价</h3>
        <div class="rated">
          <el-rate :model-value="d.evalScore" disabled />
          <span class="rated__score">{{ d.evalScore }}.0</span>
        </div>
        <div v-if="evalTagList.length" class="rated__tags">
          <span v-for="t in evalTagList" :key="t" class="rated__tag">{{ t }}</span>
        </div>
        <p v-if="d.evalComment" class="rated__comment">{{ d.evalComment }}</p>
      </div>

      <!-- 问题描述 -->
      <div class="glass-card block">
        <h3>问题描述</h3>
        <p class="content">{{ d.content }}</p>
        <a v-if="d.imageUrl" class="img-link" :href="d.imageUrl" target="_blank" rel="noopener">
          查看附件图片
        </a>
      </div>

      <!-- 处理时间线 -->
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

      <!-- 解决方案 -->
      <div class="glass-card block">
        <h3>解决方案</h3>
        <template v-if="d.solutions && d.solutions.length">
          <div v-for="(s, i) in d.solutions" :key="i" class="sol">
            <div class="sol__head">
              <span class="sol__eng">{{ s.engineerName || '工程师' }}</span>
              <span class="sol__time">{{ fmtTime(s.createTime) }}</span>
            </div>
            <p class="sol__text">{{ s.solutionText }}</p>
          </div>
        </template>
        <StateEmpty v-else text="工单处理完成后将展示解决方案" />
      </div>
    </template>

    <!-- 评价弹窗 -->
    <el-dialog v-model="evalVisible" title="服务评价" width="440px">
      <div class="eval-form">
        <div class="eval-row">
          <span class="eval-label">评分</span>
          <el-rate v-model="evalForm.score" />
        </div>
        <div class="eval-row eval-row--top">
          <span class="eval-label">标签</span>
          <div class="eval-tags">
            <span
              v-for="t in TAG_OPTIONS"
              :key="t"
              class="eval-tag"
              :class="{ 'eval-tag--on': evalForm.tags.includes(t) }"
              @click="toggleTag(t)"
            >{{ t }}</span>
          </div>
        </div>
        <div class="eval-row eval-row--top">
          <span class="eval-label">评论</span>
          <el-input
            v-model="evalForm.comment"
            type="textarea"
            :rows="3"
            maxlength="512"
            show-word-limit
            placeholder="说说你的感受（可选）"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="evalVisible = false">取消</el-button>
        <el-button type="primary" :loading="evalSubmitting" @click="onEvaluate">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ticketDetail, verifyPass, verifyReject } from '@/api/ticket'
import { evaluateTicket } from '@/api/evaluation'
import { statusMeta, slaMeta } from '@/utils/ticket'
import StateLoading from '@/components/state/StateLoading.vue'
import StateEmpty from '@/components/state/StateEmpty.vue'
import StateError from '@/components/state/StateError.vue'

const TAG_OPTIONS = ['响应及时', '解决彻底', '态度好', '专业负责', '沟通顺畅']

const route = useRoute()
const role = localStorage.getItem('role') || 'EMPLOYEE'
const d = ref({})
const status = ref('loading') // loading | ready | error
const errText = ref('工单详情加载失败，请稍后再试')
const verifying = ref(false)

const evalVisible = ref(false)
const evalSubmitting = ref(false)
const evalForm = reactive({ score: 5, tags: [], comment: '' })

// 待验收且本人报修 / 管理员 → 可验收（后端再校验 creator/admin）
const canVerify = computed(
  () => d.value.status === 'ACCEPTING' && (role === 'EMPLOYEE' || role === 'ADMIN')
)
// 已完成且本人报修 / 管理员 → 可评价
const canEvaluate = computed(
  () => d.value.status === 'DONE' && (role === 'EMPLOYEE' || role === 'ADMIN')
)
const evalTagList = computed(() => (d.value.evalTags || '').split(',').filter(Boolean))

function toggleTag(t) {
  const i = evalForm.tags.indexOf(t)
  if (i >= 0) evalForm.tags.splice(i, 1)
  else evalForm.tags.push(t)
}

async function onEvaluate() {
  if (!evalForm.score) {
    ElMessage.warning('请先打分')
    return
  }
  evalSubmitting.value = true
  try {
    await evaluateTicket(route.params.id, {
      score: evalForm.score,
      tags: evalForm.tags.join(','),
      comment: evalForm.comment || undefined
    })
    ElMessage.success('感谢评价')
    evalVisible.value = false
    await load()
  } catch (e) {
    await load()
  } finally {
    evalSubmitting.value = false
  }
}

// 每秒刷新，驱动 SLA 倒计时
const nowTick = ref(Date.now())
const timer = setInterval(() => (nowTick.value = Date.now()), 1000)
onUnmounted(() => clearInterval(timer))

const sMeta = computed(() => statusMeta(d.value.status))
const slaLive = computed(() => {
  // 依赖 nowTick 触发重算
  void nowTick.value
  return slaMeta(d.value.slaDeadline, d.value.slaOverdue)
})

async function load() {
  status.value = 'loading'
  try {
    d.value = await ticketDetail(route.params.id)
    status.value = 'ready'
  } catch (e) {
    // 业务异常（数据域 403 / 404）经 request.js 拆包为 Error(message)；HTTP 层错误用兜底文案
    const m = e?.message
    errText.value = m && !/^Request failed/.test(m) ? m : '工单详情加载失败，请稍后再试'
    status.value = 'error'
  }
}

async function onPass() {
  verifying.value = true
  try {
    await verifyPass(route.params.id)
    ElMessage.success('已验收通过')
    await load()
  } catch (e) {
    // request.js 已提示；刷新以反映最新状态
    await load()
  } finally {
    verifying.value = false
  }
}

async function onReject() {
  try {
    const { value } = await ElMessageBox.prompt('请填写驳回原因（可选）', '验收驳回', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '如：问题仍未解决'
    })
    verifying.value = true
    await verifyReject(route.params.id, { reason: value || undefined })
    ElMessage.success('已驳回，退回处理中')
    await load()
  } catch (e) {
    if (e !== 'cancel') await load()
  } finally {
    verifying.value = false
  }
}

function fmtTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : '—'
}

load()
</script>

<style scoped>
.detail {
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
.head {
  padding: 22px 24px;
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
.head__title {
  font-size: 20px;
  margin: 14px 0 18px;
}
.head__grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.head__grid label {
  display: block;
  font-size: 12px;
  color: var(--apple-darkgray);
  margin-bottom: 4px;
}
.head__grid span {
  font-size: 14px;
  font-weight: 500;
}
.sla {
  font-weight: 600;
}
.block {
  padding: 20px 24px;
}
.block h3 {
  font-size: 16px;
  margin-bottom: 14px;
}
.verify {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border: 1px solid rgba(175, 82, 222, 0.3);
  background: linear-gradient(160deg, rgba(245, 237, 255, 0.9), rgba(237, 242, 255, 0.85));
}
.verify__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.verify__text strong {
  font-size: 15px;
}
.verify__text span {
  font-size: 12px;
  color: var(--apple-darkgray);
}
.verify__acts {
  display: flex;
  gap: 12px;
}
/* 已评价展示 */
.rated {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rated__score {
  font-size: 15px;
  font-weight: 600;
  color: #f5a623;
}
.rated__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.rated__tag {
  font-size: 12px;
  color: var(--apple-blue);
  background: rgba(0, 113, 227, 0.08);
  padding: 3px 10px;
  border-radius: var(--radius-badge);
}
.rated__comment {
  margin-top: 12px;
  font-size: 14px;
  line-height: 1.7;
  color: #3a3a3c;
}
/* 评价弹窗 */
.eval-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.eval-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.eval-row--top {
  align-items: flex-start;
}
.eval-label {
  width: 40px;
  font-size: 14px;
  color: var(--apple-darkgray);
  flex-shrink: 0;
  padding-top: 4px;
}
.eval-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.eval-tag {
  font-size: 13px;
  color: var(--apple-darkgray);
  background: var(--apple-gray);
  padding: 4px 12px;
  border-radius: var(--radius-badge);
  cursor: pointer;
  border: 1px solid transparent;
}
.eval-tag--on {
  color: var(--apple-blue);
  background: rgba(0, 113, 227, 0.1);
  border-color: rgba(0, 113, 227, 0.4);
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
.sol {
  padding: 14px 16px;
  border-radius: var(--radius-control);
  background: rgba(255, 255, 255, 0.6);
  margin-bottom: 10px;
}
.sol__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.sol__eng {
  font-size: 13px;
  font-weight: 600;
}
.sol__time {
  font-size: 12px;
  color: var(--apple-darkgray);
}
.sol__text {
  font-size: 14px;
  line-height: 1.7;
  color: #3a3a3c;
  white-space: pre-wrap;
}
</style>
