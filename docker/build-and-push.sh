#!/bin/bash
# ============================================
# Knife4j 镜像构建与推送脚本
# 用法: 
#   ./build-and-push.sh                    # 构建并推送 latest 版本
#   ./build-and-push.sh 1.0.0              # 构建并推送指定版本
#   ./build-and-push.sh 1.0.0 build        # 仅构建，不推送
#   ./build-and-push.sh 1.0.0 build china  # 使用国内镜像源构建
# ============================================
set -e

# ============================================
# 切换到项目根目录（脚本所在目录的父目录）
# ============================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"
echo "工作目录: $(pwd)"

# ============================================
# 配置
# ============================================
DOCKER_USERNAME="${DOCKER_USERNAME:-zhanglongjun}"
VERSION="${1:-latest}"
ACTION="${2:-push}"  # push 或 build
USE_CHINA_MIRROR="${3:-}"  # china 表示使用国内镜像源

# 国内镜像源配置
if [ "${USE_CHINA_MIRROR}" = "china" ]; then
    # 阿里云镜像源
    REGISTRY_MIRROR="registry.cn-hangzhou.aliyuncs.com/"
    BUILD_ARGS="--build-arg REGISTRY_MIRROR=${REGISTRY_MIRROR}"
    echo "使用国内镜像源: ${REGISTRY_MIRROR}"
else
    BUILD_ARGS=""
fi

# 镜像名称
DOC_AGGREGATOR_IMAGE="${DOCKER_USERNAME}/knife4j-doc-aggregator:${VERSION}"
AI_SERVICE_IMAGE="${DOCKER_USERNAME}/knife4j-ai-service:${VERSION}"

echo "=========================================="
echo "Knife4j 镜像构建工具"
echo "=========================================="
echo "Docker 用户: ${DOCKER_USERNAME}"
echo "版本: ${VERSION}"
echo "操作: ${ACTION}"
echo "=========================================="

# ============================================
# 检查 Docker 是否运行
# ============================================
if ! docker info > /dev/null 2>&1; then
    echo "❌ 错误: Docker 未运行，请先启动 Docker"
    exit 1
fi

# ============================================
# 构建 doc-aggregator 镜像
# ============================================
echo ""
echo "[1/2] 构建 doc-aggregator 镜像..."
echo "镜像名: ${DOC_AGGREGATOR_IMAGE}"
echo "说明: 使用独立 pom-docker.xml 构建，不依赖父项目"
docker build ${BUILD_ARGS} -f knife4j-doc-aggregator/Dockerfile -t ${DOC_AGGREGATOR_IMAGE} .

if [ $? -eq 0 ]; then
    echo "✅ doc-aggregator 镜像构建成功"
else
    echo "❌ doc-aggregator 镜像构建失败"
    exit 1
fi

# ============================================
# 构建 knife4j-ai-service 镜像
# ============================================
echo ""
echo "[2/2] 构建 knife4j-ai-service 镜像..."
echo "镜像名: ${AI_SERVICE_IMAGE}"
docker build ${BUILD_ARGS} -f knife4j-ai-service/Dockerfile -t ${AI_SERVICE_IMAGE} ./knife4j-ai-service

if [ $? -eq 0 ]; then
    echo "✅ knife4j-ai-service 镜像构建成功"
else
    echo "❌ knife4j-ai-service 镜像构建失败"
    exit 1
fi

# ============================================
# 推送镜像（如果指定）
# ============================================
if [ "${ACTION}" = "push" ]; then
    echo ""
    echo "=========================================="
    echo "推送镜像到 DockerHub"
    echo "=========================================="
    
    # 检查是否已登录
    if ! docker info 2>/dev/null | grep -q "Username"; then
        echo "⚠️  请先登录 DockerHub: docker login"
        read -p "是否现在登录? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker login
        else
            echo "跳过推送步骤"
            exit 0
        fi
    fi
    
    echo ""
    echo "推送 doc-aggregator..."
    docker push ${DOC_AGGREGATOR_IMAGE}
    
    echo ""
    echo "推送 knife4j-ai-service..."
    docker push ${AI_SERVICE_IMAGE}
    
    # 如果不是 latest，同时更新 latest 标签
    if [ "${VERSION}" != "latest" ]; then
        echo ""
        echo "更新 latest 标签..."
        docker tag ${DOC_AGGREGATOR_IMAGE} ${DOCKER_USERNAME}/knife4j-doc-aggregator:latest
        docker tag ${AI_SERVICE_IMAGE} ${DOCKER_USERNAME}/knife4j-ai-service:latest
        docker push ${DOCKER_USERNAME}/knife4j-doc-aggregator:latest
        docker push ${DOCKER_USERNAME}/knife4j-ai-service:latest
    fi
    
    echo ""
    echo "=========================================="
    echo "✅ 镜像推送完成!"
    echo "=========================================="
else
    echo ""
    echo "=========================================="
    echo "✅ 镜像构建完成（未推送）"
    echo "=========================================="
fi

echo ""
echo "镜像列表:"
echo "  - ${DOC_AGGREGATOR_IMAGE}"
echo "  - ${AI_SERVICE_IMAGE}"
echo ""
echo "本地测试命令:"
echo "  docker run -d -p 9090:9090 ${DOC_AGGREGATOR_IMAGE}"
echo "  docker run -d -p 9100:9100 ${AI_SERVICE_IMAGE}"
