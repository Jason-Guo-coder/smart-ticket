#!/usr/bin/env bash
# ============================================================
# SmartTicket 一键启动脚本
#   - 自动确保 MySQL / Redis 就绪（未运行才启动）
#   - 启动后端(Spring Boot:8080) 与 前端(Vite:5173)
#   - 前台常驻；Ctrl+C / 关窗(SIGHUP) 时：
#       1) 安全清理构建产物 / 缓存 / 日志（不动源码、依赖、数据库）
#       2) 关闭本脚本启动的前后端（及自己启动的 MySQL/Redis）
# 用法：./start.sh
# ============================================================
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND="$ROOT/backend"
FRONTEND="$ROOT/vue-frontend"
RUN_DIR="$ROOT/.run"
mkdir -p "$RUN_DIR"

BACK_LOG="$RUN_DIR/backend.log"
FRONT_LOG="$RUN_DIR/frontend.log"
BACK_PID=""
FRONT_PID=""
STARTED_MYSQL=0
STARTED_REDIS=0
CLEANED=0

c_green() { printf "\033[32m%s\033[0m\n" "$1"; }
c_blue()  { printf "\033[34m%s\033[0m\n" "$1"; }
c_yellow(){ printf "\033[33m%s\033[0m\n" "$1"; }

port_pids() { lsof -nP -iTCP:"$1" -sTCP:LISTEN -t 2>/dev/null; }

kill_port() {
  for p in $(port_pids "$1"); do kill "$p" 2>/dev/null || true; done
}

cleanup() {
  [ "$CLEANED" = "1" ] && return
  CLEANED=1
  echo
  c_yellow "▶ 正在清理并关闭 SmartTicket …"

  # 1) 关闭前后端进程（含 mvn 派生的 java：按端口兜底）
  [ -n "${TAIL_PID:-}" ] && kill "$TAIL_PID" 2>/dev/null || true
  [ -n "$FRONT_PID" ] && kill "$FRONT_PID" 2>/dev/null || true
  [ -n "$BACK_PID" ]  && kill "$BACK_PID"  2>/dev/null || true
  pkill -P $$ 2>/dev/null || true
  kill_port 5173
  kill_port 8080

  # 2) 清理本应用的 Redis 缓存键（仅本应用前缀，绝不 FLUSHALL，不动其它库）
  if command -v redis-cli >/dev/null 2>&1 && redis-cli ping >/dev/null 2>&1; then
    for pat in 'ai:*' 'rl:*' 'jwt:blacklist:*' 'ticket:no:*' 'idem:*'; do
      keys=$(redis-cli --scan --pattern "$pat" 2>/dev/null)
      [ -n "$keys" ] && echo "$keys" | xargs redis-cli del >/dev/null 2>&1 || true
    done
  fi

  # 3) 安全清理构建产物 / 缓存 / 运行日志（白名单删除，绝不碰源码/依赖/数据库/密钥）
  rm -rf "$BACKEND/target" 2>/dev/null || true
  rm -rf "$FRONTEND/dist" "$FRONTEND/node_modules/.vite" 2>/dev/null || true
  rm -rf "$RUN_DIR" 2>/dev/null || true

  # 4) 仅关闭“本脚本启动”的 MySQL/Redis（之前已在运行的保持不动）
  [ "$STARTED_REDIS" = "1" ] && { c_yellow "  停止 Redis（本脚本启动）"; brew services stop redis >/dev/null 2>&1 || true; }
  [ "$STARTED_MYSQL" = "1" ] && { c_yellow "  停止 MySQL（本脚本启动）"; brew services stop mysql >/dev/null 2>&1 || true; }

  c_green "✓ 已清理并关闭。再见 👋"
  exit 0
}
trap cleanup INT TERM HUP EXIT

# ---------- 依赖检查 ----------
for cmd in mvn npm lsof; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "缺少命令: $cmd"; exit 1; }
done

# ---------- 1. Redis ----------
if command -v redis-cli >/dev/null 2>&1 && redis-cli ping >/dev/null 2>&1; then
  c_green "✓ Redis 已在运行"
else
  c_blue "→ 启动 Redis …"; brew services start redis >/dev/null 2>&1 && STARTED_REDIS=1
  for i in {1..10}; do redis-cli ping >/dev/null 2>&1 && break; sleep 1; done
fi

# ---------- 2. MySQL ----------
if [ -n "$(port_pids 3306)" ]; then
  c_green "✓ MySQL 已在运行"
else
  c_blue "→ 启动 MySQL …"; brew services start mysql >/dev/null 2>&1 && STARTED_MYSQL=1
  for i in {1..20}; do [ -n "$(port_pids 3306)" ] && break; sleep 1; done
fi

# ---------- 3. 后端 ----------
kill_port 8080
c_blue "→ 启动后端 (Spring Boot) …"
( cd "$BACKEND" && mvn -q -DskipTests spring-boot:run > "$BACK_LOG" 2>&1 ) &
BACK_PID=$!
printf "  等待后端就绪"
for i in {1..60}; do
  grep -q "Started SmartTicketApplication" "$BACK_LOG" 2>/dev/null && break
  grep -q "APPLICATION FAILED" "$BACK_LOG" 2>/dev/null && { echo; c_yellow "  后端启动失败，详见 $BACK_LOG"; break; }
  printf "."; sleep 1
done
echo
[ -n "$(port_pids 8080)" ] && c_green "✓ 后端就绪 http://localhost:8080" || c_yellow "! 后端未监听 8080，详见 $BACK_LOG"

# ---------- 4. 前端 ----------
kill_port 5173
c_blue "→ 启动前端 (Vite) …"
if [ ! -d "$FRONTEND/node_modules" ]; then
  c_blue "  首次运行，安装前端依赖 …"; ( cd "$FRONTEND" && npm install ) >/dev/null 2>&1
fi
( cd "$FRONTEND" && npm run dev > "$FRONT_LOG" 2>&1 ) &
FRONT_PID=$!
for i in {1..20}; do [ -n "$(port_pids 5173)" ] && break; sleep 1; done
[ -n "$(port_pids 5173)" ] && c_green "✓ 前端就绪 http://localhost:5173" || c_yellow "! 前端未监听 5173，详见 $FRONT_LOG"

# ---------- 常驻 ----------
echo
c_green "========================================"
c_green " SmartTicket 已启动"
c_green "  前端  http://localhost:5173"
c_green "  后端  http://localhost:8080"
c_green "  演示账号  liming / zhangwei / wangfang  (密码 123456)"
c_green "  按 Ctrl+C 退出（将自动清理并关闭）"
c_green "========================================"
echo "实时日志（后端）：$BACK_LOG"

# 前台常驻：跟随后端日志；Ctrl+C/关窗触发 cleanup
tail -f "$BACK_LOG" 2>/dev/null &
TAIL_PID=$!
# 可中断的常驻循环（sleep 为子进程，信号可及时打断并触发 trap）
while true; do sleep 1; done
