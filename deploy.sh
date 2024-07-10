#!/bin/bash

# 使用传入的第一个参数作为版本标签
VERSION_TAG=$1

if [ -z "$VERSION_TAG" ]; then
    echo "Error: No version tag provided."
    exit 1
fi

# Maven 构建并跳过测试
./gradlew build

# 构建 Docker 镜像并设置标签
docker build -t aolifu/stirling-pdf:$VERSION_TAG .

# 推送 Docker 镜像
docker push aolifu/stirling-pdf:$VERSION_TAG
