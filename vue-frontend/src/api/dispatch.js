import request from './request'

/** 派单页数据：待派单卡片（含 AI 建议）+ 工程师负载 */
export function dispatchBoard() {
  return request.get('/dispatch/board')
}

/** 手动指派 / 采纳 AI 建议 */
export function assignTicket(id, engineerId) {
  return request.post(`/dispatch/${id}/assign`, { engineerId })
}

/** 自动分配（按类别+负载），返回被指派工程师姓名 */
export function autoAssign(id) {
  return request.post(`/dispatch/${id}/auto`)
}

/** 一键智能分派全部，返回 { total, success, failed } */
export function autoAssignAll() {
  return request.post('/dispatch/auto-all')
}
