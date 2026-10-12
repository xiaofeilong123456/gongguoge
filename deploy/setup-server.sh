#!/bin/bash
# ============================================================
# 功过格 · 服务器环境初始化（CentOS 7.9）
#
# 用法：以 root 执行
#   chmod +x setup-server.sh && ./setup-server.sh
#
# 做四件事：安装 JDK17 / MySQL8 / Redis，并创建应用用户与目录
# 每一步都可以单独复制出去跑，不必整脚本执行。
#
# 修订记录：
#   v2 - 修掉 JDK 安装的两个坑：
#        ① 原来 `curl | rpm --import` 写法错误（rpm 需要文件参数，不吃管道），
#           会报 "rpmkeys: 没有指定参数" 和 "Failed writing body"；
#        ② 官方 Adoptium 源在国内服务器上常连不上，改为优先从国内镜像
#           直接下载 tar.gz 解压安装，源不可达时再回退官方 API。
# ============================================================
set -e

echo "=========================================="
echo " 功过格 · CentOS 7.9 环境初始化"
echo "=========================================="

# ------------------------------------------------------------
# 0. 修复 yum 源
#    CentOS 7 已于 2024-06-30 EOL，官方 mirrorlist 全部下线，
#    不修的话任何 yum 命令都会报 Could not resolve host: mirrorlist.centos.org
# ------------------------------------------------------------
echo ""
echo "[0/5] 修复 CentOS 7 yum 源（指向 vault 归档站）..."
sed -i 's|^mirrorlist=|#mirrorlist=|g' /etc/yum.repos.d/CentOS-*.repo
sed -i 's|^#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*.repo
yum clean all >/dev/null 2>&1 || true
yum makecache -y >/dev/null 2>&1 || true
echo "      done"

# ------------------------------------------------------------
# 1. 安装 JDK 17（解压安装，不依赖第三方 yum 源）
# ------------------------------------------------------------
echo ""
echo "[1/5] 安装 JDK 17..."

JDK_DIR=/usr/local/jdk17
JDK_TARBALL=/tmp/jdk17.tar.gz

if [ -x "$JDK_DIR/bin/java" ]; then
  echo "      已存在，跳过：$("$JDK_DIR/bin/java" -version 2>&1 | head -1)"
else
  # 从「目录型」镜像里找出最新的 17 版 tar.gz 并下载
  # 用法：try_mirror_dir <镜像目录URL>
  try_mirror_dir() {
    local dir="$1"
    local file
    file=$(curl -fsSL --connect-timeout 10 -m 40 "$dir" 2>/dev/null \
           | grep -oE 'OpenJDK17U-jdk_x64_linux_hotspot_[0-9._]+\.tar\.gz' \
           | sort -V | tail -1) || true
    if [ -z "$file" ]; then
      echo "        未在该镜像找到文件，跳过"
      return 1
    fi
    echo "        找到: $file"
    rm -f "$JDK_TARBALL"
    if curl -fL --connect-timeout 15 -m 900 -o "$JDK_TARBALL" "$dir$file"; then
      return 0
    fi
    return 1
  }

  GOT=0
  # 已验证可用的国内镜像（2026-10 实测）
  echo "      尝试国内镜像（清华 TUNA）..."
  if try_mirror_dir "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/linux/"; then GOT=1; fi

  if [ "$GOT" -eq 0 ]; then
    echo "      尝试国内镜像（CERNET 教育网联合镜像）..."
    if try_mirror_dir "https://mirrors.cernet.edu.cn/Adoptium/17/jdk/x64/linux/"; then GOT=1; fi
  fi

  if [ "$GOT" -eq 0 ]; then
    echo "      国内镜像都不可用，回退 Adoptium 官方 API（国内常连不上）..."
    rm -f "$JDK_TARBALL"
    if curl -fL --connect-timeout 15 -m 900 -o "$JDK_TARBALL" \
        "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"; then
      GOT=1
    fi
  fi

  if [ "$GOT" -eq 0 ]; then
    echo ""
    echo "  ✗ JDK 下载失败：所有镜像都不可达。"
    echo "    请在服务器上手动测试网络："
    echo "      curl -sI -m 10 https://mirrors.tuna.tsinghua.edu.cn/Adoptium/ | head -1"
    echo "    把结果发给开发者。"
    exit 1
  fi

  # 校验下载的确实是 gzip（避免把错误页面当成安装包）
  if ! gzip -t "$JDK_TARBALL" 2>/dev/null; then
    echo "  ✗ 下载的文件不是有效的 tar.gz（可能下到了错误页面）"
    echo "    文件大小：$(du -h "$JDK_TARBALL" | cut -f1)"
    head -c 200 "$JDK_TARBALL"
    exit 1
  fi
  echo "      下载完成（$(du -h "$JDK_TARBALL" | cut -f1)），正在解压..."

  mkdir -p "$JDK_DIR"
  tar -xzf "$JDK_TARBALL" -C "$JDK_DIR" --strip-components=1
  rm -f "$JDK_TARBALL"

  # 让 java 命令全局可用（systemd 的 ExecStart 用的是 /usr/bin/java）
  ln -sf "$JDK_DIR/bin/java" /usr/bin/java
  ln -sf "$JDK_DIR/bin/jar"  /usr/bin/jar
  ln -sf "$JDK_DIR/bin/javac" /usr/bin/javac

  # 写入 JAVA_HOME 环境变量
  cat > /etc/profile.d/jdk17.sh <<'EOF'
export JAVA_HOME=/usr/local/jdk17
export PATH=$JAVA_HOME/bin:$PATH
EOF
  chmod +x /etc/profile.d/jdk17.sh
  # 让当前 shell 也生效
  export JAVA_HOME=/usr/local/jdk17
  export PATH="$JAVA_HOME/bin:$PATH"

  echo "      安装完成：$(java -version 2>&1 | head -1)"
fi

# 兜底检查：确认 java 可用
if ! command -v java >/dev/null 2>&1; then
  echo "  ✗ java 命令不可用，请检查 /usr/local/jdk17"
  exit 1
fi

# ------------------------------------------------------------
# 2. 安装 MySQL 8
# ------------------------------------------------------------
echo ""
echo "[2/5] 安装 MySQL 8..."

if systemctl list-unit-files 2>/dev/null | grep -q '^mysqld.service'; then
  echo "      已安装，跳过"
else
  yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-11.noarch.rpm
  yum install -y mysql-community-server
fi

# ------------------------------------------------------------
# 处理 MariaDB 残留数据目录（CentOS 7 的常见坑）
#
# CentOS 7 默认自带 MariaDB，安装 MySQL 8 时 MariaDB 会被卸载，
# 但 /var/lib/mysql 里会残留 MariaDB 的数据文件（aria_log* 等）。
# MySQL 8 不认识这种目录，会直接启动失败。
#
# 判断依据：MySQL 8 的数据目录一定含 mysql.ibd（数据字典表空间），
# 没有它就不是 MySQL 8 的目录 —— 此时把它整体移走备份（不删除）后重建。
# ------------------------------------------------------------
if [ -d /var/lib/mysql ] && [ -n "$(ls -A /var/lib/mysql 2>/dev/null)" ] && [ ! -f /var/lib/mysql/mysql.ibd ]; then
  MYSQL_BAK="/var/lib/mysql.bak.$(date +%Y%m%d-%H%M%S)"
  echo "      检测到非 MySQL 8 的数据目录（很可能是 MariaDB 残留）"
  echo "      已备份到：$MYSQL_BAK   （原目录未删除）"
  mv /var/lib/mysql "$MYSQL_BAK"
  mkdir -p /var/lib/mysql
  chown mysql:mysql /var/lib/mysql
fi

rm -f /var/log/mysqld.log
systemctl enable --now mysqld
echo "      等待 MySQL 首次初始化（约 30 秒）..."
for i in $(seq 1 30); do
  if systemctl is-active --quiet mysqld; then break; fi
  sleep 2
done

# 启动失败时把日志打出来，不要只报一句 "control process exited"
if ! systemctl is-active --quiet mysqld; then
  echo ""
  echo "  ✗ mysqld 启动失败，最近日志如下："
  echo "  ------------------------------------------"
  tail -30 /var/log/mysqld.log 2>/dev/null || journalctl -u mysqld -n 30 --no-pager
  echo "  ------------------------------------------"
  echo "  把上面这段发给开发者。"
  exit 1
fi

TEMP_PWD=$(grep 'temporary password' /var/log/mysqld.log 2>/dev/null | tail -1 | awk '{print $NF}' || true)
if [ -n "$TEMP_PWD" ]; then
  echo ""
  echo "      ============================================"
  echo "      MySQL 初始 root 密码：$TEMP_PWD"
  echo "      ⚠️ 立刻复制记下来！下一步要用，只出现这一次"
  echo "      ============================================"
  echo ""
else
  echo "      未取到初始密码（可能之前已初始化过）"
  echo "      若忘记了，可用 mysqld --skip-grant-tables 重置"
fi

# ------------------------------------------------------------
# 3. 安装 Redis
# ------------------------------------------------------------
echo ""
echo "[3/5] 安装 Redis..."
if command -v redis-server >/dev/null 2>&1; then
  echo "      已安装，跳过"
else
  yum install -y epel-release
  yum install -y redis
fi
# 只监听本机，避免 Redis 暴露到公网（Redis 未授权访问是高危漏洞）
sed -i 's|^bind .*|bind 127.0.0.1|' /etc/redis.conf 2>/dev/null || true
systemctl enable --now redis
echo "      redis 版本：$(redis-server --version | awk '{print $3}')"

# ------------------------------------------------------------
# 4. 创建应用专用用户与目录
# ------------------------------------------------------------
echo ""
echo "[4/5] 创建应用用户与目录..."
id gongguoge >/dev/null 2>&1 || useradd -r -m -d /opt/gongguoge -s /sbin/nologin gongguoge
mkdir -p /opt/gongguoge/uploads
chown -R gongguoge:gongguoge /opt/gongguoge
echo "      /opt/gongguoge 已就绪"

# ------------------------------------------------------------
# 5. 完成
# ------------------------------------------------------------
echo ""
echo "[5/5] 环境安装完成"
echo ""
echo "=========================================="
echo " 环境自检："
echo "   java  : $(java -version 2>&1 | head -1)"
echo "   mysql : $(systemctl is-active mysqld)"
echo "   redis : $(systemctl is-active redis)"
echo "=========================================="
echo ""
echo " 下一步（按 DEPLOY.md 操作）："
echo "   1) 设置 MySQL root 密码并创建业务库与用户"
echo "   2) 导入表结构 database/init/01_schema.sql"
echo "   3) 生成 JWT_SECRET：openssl rand -base64 48"
echo "   4) 上传 jar 到 /opt/gongguoge/"
echo "   5) 配置并启动 gongguoge.service"
echo "=========================================="
