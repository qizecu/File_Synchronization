#!/bin/bash
# =================================================
# 文件同步管理系统 - 一键部署脚本
# =================================================
# 用法：
#   ./deploy.sh local    -- 本地开发环境部署
#   ./deploy.sh prod     -- 服务器生产环境部署
#   ./deploy.sh stop     -- 停止所有服务
#   ./deploy.sh restart  -- 重启所有服务
#   ./deploy.sh logs     -- 查看日志
# =================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 Docker 环境
check_prerequisites() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    if ! command -v docker compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
}

# 部署
do_deploy() {
    local env=$1
    local env_file

    case $env in
        local)
            env_file=".env.local"
            ;;
        prod)
            env_file=".env.prod"
            ;;
        *)
            print_error "未知环境: $env，只支持 local 和 prod"
            exit 1
            ;;
    esac

    print_info "部署环境: $env (配置文件: $env_file)"

    if [ ! -f "$env_file" ]; then
        print_error "配置文件不存在: $env_file"
        exit 1
    fi

    # 创建 .env 软链接，docker compose 默认读取 .env
    rm -f .env
    cp "$env_file" .env
    print_info "已复制 $env_file -> .env"

    # 构建并启动
    print_info "正在构建镜像..."
    docker compose build --no-cache

    print_info "正在启动服务..."
    docker compose up -d

    print_info "等待服务就绪..."
    sleep 5

    # 健康检查
    print_info "服务状态："
    docker compose ps

    echo ""
    print_info "========================================="
    print_info "部署完成！"
    print_info "前端地址: http://localhost:$(grep FRONTEND_PORT "$env_file" | cut -d= -f2)"
    print_info "后端地址: http://localhost:$(grep BACKEND_PORT "$env_file" | cut -d= -f2)"
    print_info "默认账号: admin / admin123"
    print_info "========================================="
}

# 停止服务
do_stop() {
    print_info "正在停止所有服务..."
    docker compose down
    print_info "服务已停止"
}

# 重启服务
do_restart() {
    print_info "正在重启所有服务..."
    docker compose restart
    print_info "服务已重启"
}

# 查看日志
do_logs() {
    docker compose logs -f --tail=100
}

# 主入口
check_prerequisites

case "${1:-}" in
    local|prod)
        do_deploy "$1"
        ;;
    stop)
        do_stop
        ;;
    restart)
        do_restart
        ;;
    logs)
        do_logs
        ;;
    *)
        echo "用法: $0 {local|prod|stop|restart|logs}"
        echo ""
        echo "  local   - 本地开发环境部署 (TEST 模式, 限制 10 个文件)"
        echo "  prod    - 服务器生产环境部署 (FULL 模式)"
        echo "  stop    - 停止所有服务"
        echo "  restart - 重启所有服务"
        echo "  logs    - 查看实时日志"
        exit 1
        ;;
esac
