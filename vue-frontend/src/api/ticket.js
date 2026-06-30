import request from './request'

/** 提交工单，返回 { ticketId, ticketNo, ai } */
export function submitTicket(data) {
  return request.post('/tickets/submit', data)
}
