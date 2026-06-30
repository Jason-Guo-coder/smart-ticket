import request from './request'

/** 登录，返回 { token, username, realName, role } */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 登出（后端将当前 token 拉黑） */
export function logout() {
  return request.post('/auth/logout')
}

/** 当前登录用户信息 */
export function getMe() {
  return request.get('/auth/me')
}
