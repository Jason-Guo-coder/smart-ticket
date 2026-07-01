import request from './request'

/** 提交工单，返回 { ticketId, ticketNo, ai } */
export function submitTicket(data) {
  return request.post('/tickets/submit', data)
}

/** 我的工单分页查询，params: { pageNum, pageSize, status?, keyword? } → { total, list } */
export function myTickets(params) {
  return request.get('/tickets/my', { params })
}

/** 工单详情（表头 + 时间线 + 解决方案 + SLA） */
export function ticketDetail(id) {
  return request.get(`/tickets/${id}`)
}

/** 工程师待办页数据（统计条 + 待办卡片） */
export function engineerTodo() {
  return request.get('/tickets/todo')
}

/** 处理页 AI 参考：同类别历史已解决工单 */
export function handleSimilar(id) {
  return request.get(`/tickets/${id}/handle/similar`)
}

/** 接单：待派单→处理中 */
export function acceptTicket(id) {
  return request.post(`/tickets/${id}/handle/accept`)
}

/** 提交完成：处理中→待验收，data: { solutionText, imageUrl? } */
export function submitSolution(id, data) {
  return request.post(`/tickets/${id}/handle/submit`, data)
}

/** 转派：处理中→待派单，data: { reason? } */
export function reassignTicket(id, data) {
  return request.post(`/tickets/${id}/handle/reassign`, data)
}

/** 验收通过：待验收→已完成 */
export function verifyPass(id) {
  return request.post(`/tickets/${id}/verify/pass`)
}

/** 验收驳回：待验收→处理中，data: { reason? } */
export function verifyReject(id, data) {
  return request.post(`/tickets/${id}/verify/reject`, data)
}
