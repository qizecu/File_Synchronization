#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== 编译打包 ==="
mvn clean package -DskipTests

echo "=== 启动应用 ==="
java -jar target/*.jar
