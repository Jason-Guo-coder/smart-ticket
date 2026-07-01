import request from './request'

/** 生成幂等 token（防重复提交，B9） */
function idemKey() {
  return 'eval-' + Date.now() + '-' + Math.random().toString(16).slice(2)
}

/** 提交评价，data: { score, tags?, comment? } */
export function evaluateTicket(ticketId, data) {
  return request.post(`/evaluations/${ticketId}`, data, {
    headers: { 'Idempotency-Key': idemKey() }
  })
}
