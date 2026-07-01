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
