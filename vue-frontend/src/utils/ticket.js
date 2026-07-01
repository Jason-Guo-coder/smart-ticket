/** 工单展示辅助：状态色 / 优先级 / SLA 剩余（DESIGN §2.3 / §6.4）。 */

const STATUS = {
  PENDING: { label: '待派单', color: 'var(--status-pending)' },
  PROCESSING: { label: '处理中', color: 'var(--status-processing)' },
  ACCEPTING: { label: '待验收', color: 'var(--status-accepting)' },
  DONE: { label: '已完成', color: 'var(--status-done)' },
  RATED: { label: '已评价', color: 'var(--status-rated)' }
}

/** 状态徽章元信息（未知状态兜底为灰）。 */
export function statusMeta(status) {
  return STATUS[status] || { label: status || '—', color: 'var(--apple-darkgray)' }
}

/** 优先级 → el-tag type。 */
export function priorityTagType(priority) {
  return priority === 'HIGH' ? 'danger' : priority === 'MID' ? 'warning' : 'success'
}

const STATUS_OPTIONS = Object.entries(STATUS).map(([value, m]) => ({ value, label: m.label }))
/** 状态下拉选项（供筛选栏）。 */
export function statusOptions() {
  return STATUS_OPTIONS
}

/**
 * SLA 剩余时间态：
 * - overdue：已超时（标记位=1 或已过截止）→ 红
 * - soon：≤4h 临近 → 橙
 * - normal：充裕 → 绿
 */
export function slaMeta(deadline, overdue) {
  if (!deadline) return { level: 'none', text: '无时限', color: 'var(--apple-darkgray)' }
  const diff = new Date(deadline).getTime() - Date.now()
  if (overdue === 1 || diff <= 0) {
    return { level: 'overdue', text: '已超时', color: 'var(--status-rejected)' }
  }
  const soon = diff <= 4 * 3600 * 1000
  return {
    level: soon ? 'soon' : 'normal',
    text: '剩 ' + fmtRemain(diff),
    color: soon ? 'var(--status-pending)' : 'var(--status-done)'
  }
}

/** 毫秒 → “1天3时” / “3时20分” / “12分”。 */
function fmtRemain(ms) {
  const totalMin = Math.floor(ms / 60000)
  const d = Math.floor(totalMin / 1440)
  const h = Math.floor((totalMin % 1440) / 60)
  const m = totalMin % 60
  if (d > 0) return `${d}天${h}时`
  if (h > 0) return `${h}时${m}分`
  return `${m}分`
}
