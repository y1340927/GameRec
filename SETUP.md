# GameRec - 基于玩家画像的游戏推荐系统

> **跨平台部署指南** — 在新电脑上从零搭建运行环境

---

## 目录

1. [环境要求](#1-环境要求)
2. [快速开始（5分钟部署）](#2-快速开始5分钟部署)
3. [详细安装步骤](#3-详细安装步骤)
   - [3.1 安装 JDK 17](#31-安装-jdk-17)
   - [3.2 安装 Node.js](#32-安装-nodejs)
   - [3.3 安装 MySQL](#33-安装-mysql)
   - [3.4 初始化数据库](#34-初始化数据库)
   - [3.5 安装 Maven（可选）](#35-安装-maven可选)
   - [3.6 安装 Python（可选）](#36-安装-python可选)
4. [项目结构说明](#4-项目结构说明)
5. [启动系统](#5-启动系统)
6. [配置说明](#6-配置说明)
7. [文件传输方案](#7-文件传输方案)
8. [常见问题](#8-常见问题)

---

## 1. 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | **17+**（推荐 LibericaJDK 17 或 Adoptium Temurin 17） | 后端运行必需 |
| Node.js | **18+**（推荐 22.x LTS） | 前端运行必需 |
| MySQL | **8.0+** | 数据库必需 |
| Maven | 3.8+（可选） | 仅后端代码修改后需要重新打包时使用 |
| Python | 3.8+（可选） | 仅数据库导出/初始化时使用 |

---

## 2. 快速开始（5分钟部署）

**前提**：已从原电脑复制完整项目目录到本机。

```powershell
# 1. 检查环境是否就绪
.\start.ps1 -Setup

# 2. 初始化数据库（需要 MySQL 已运行且能连接）
.\start.ps1 -InitDB

# 3. 启动系统
.\start.ps1

# 后续启动可跳过构建：
.\start.ps1 -Quick
```

> 也可以用 `start.bat` 打开图形界面启动（双击即可）。

---

## 3. 详细安装步骤

### 3.1 安装 JDK 17

**推荐下载**：[Adoptium Temurin 17 LTS](https://adoptium.net/temurin/releases/?version=17)

- Windows：下载 `.msi` 安装包，默认安装即可
- 安装后验证：
  ```powershell
  java -version
  # 应输出：openjdk version "17.0.x" ...
  ```
- 如需手动指定 Java 路径，设置环境变量：
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
  ```

### 3.2 安装 Node.js

**推荐下载**：[Node.js 22 LTS](https://nodejs.org/)

- Windows：下载 `.msi` 安装包，勾选"Add to PATH"
- 安装后验证：
  ```powershell
  node --version
  # 应输出：v22.x.x
  npm --version
  # 应输出：10.x.x
  ```

### 3.3 安装 MySQL

**推荐下载**：[MySQL Community Server 8.0+](https://dev.mysql.com/downloads/mysql/)

**安装步骤**：
1. 下载并运行 MySQL Installer
2. 选择"Server only"或"Developer Default"
3. 设置 root 用户密码（**建议设为 `123456`**，或后续通过环境变量配置）
4. 确保 MySQL 服务已启动：
   ```powershell
   net start MySQL80  # 或通过 services.msc 启动
   ```

**验证连接**：
```powershell
mysql -u root -p
# 输入密码后应进入 MySQL 命令行
```

### 3.4 初始化数据库

**方法一：从 SQL Dump 导入（推荐）**

从原电脑导出数据库：
```powershell
# 在原电脑上执行
.\start.ps1 -ExportDB
# 生成文件：sql/dump_game_recommend_db.sql
```

将生成的 `sql/dump_game_recommend_db.sql` 复制到新电脑的同位置，然后：
```powershell
# 在新电脑上执行
.\start.ps1 -InitDB
```

**方法二：手动导入**

如果没有 dump 文件，也可以先执行 DDL 建表，再使用 Python 脚本导入原始 CSV 数据：
```powershell
# 首先初始化 DDL
python scripts/init_database.py

# 然后导入原始数据（需要 CSV 文件在 data/ 目录下）
python scripts/data_import.py
```

### 3.5 安装 Maven（可选）

**推荐下载**：[Apache Maven 3.9+](https://maven.apache.org/download.cgi)

1. 下载 `apache-maven-3.9.x-bin.zip`
2. 解压到 `C:\tools\apache-maven-3.9.x`
3. 添加 `bin` 目录到 PATH

**验证**：
```powershell
mvn --version
```

### 3.6 安装 Python（可选）

**推荐下载**：[Python 3.13+](https://www.python.org/downloads/)

安装时勾选"Add Python to PATH"。

安装后安装 pymysql：
```powershell
pip install pymysql
```

---

## 4. 项目结构说明

```
智能游戏推荐与玩家行为分析系统/
├── start.bat                          # 启动入口（双击）
├── start.ps1                          # PowerShell 启动脚本（核心）
├── SETUP.md                           # 本部署文档
├── .gitignore                         # Git 忽略规则
│
├── game-recommend-system/             # 后端（Spring Boot + MyBatis-Plus）
│   ├── pom.xml                        # Maven 构建配置
│   ├── src/main/resources/
│   │   ├── application.yml            # 主配置（DB 连接、端口等）
│   │   └── mapper/                    # MyBatis XML 映射
│   ├── src/main/java/                 # Java 源代码
│   └── target/                        # 构建产物（git 忽略）
│       └── game-recommend-system-1.0.0.jar  # 可执行 JAR
│
├── game-recommend-frontend/           # 前端（Vue 2 + Element UI + ECharts）
│   ├── package.json                   # Node 依赖
│   ├── vue.config.js                  # Vue 构建配置
│   ├── server.js                      # 生产环境静态服务器 + API 代理
│   ├── node_modules/                  # Node 依赖（git 忽略）
│   ├── src/                           # Vue 源代码
│   └── dist/                          # 构建产物（git 忽略）
│
├── sql/
│   ├── 01_create_database.sql         # 完整 DDL（8 张表 + 1 个别名表）
│   └── dump_game_recommend_db.sql     # 全库数据导出（需手动生成）
│
├── scripts/                           # Python 工具脚本
│   ├── export_database.py             # 数据库导出工具
│   ├── init_database.py               # 数据库初始化工具
│   ├── data_import.py                 # 原始 CSV 数据导入
│   └── ...                            # 其他分析/修复脚本
│
├── data/                              # 原始 CSV 数据文件
│   ├── steam.csv                      # Steam 游戏主数据（27,075 款）
│   ├── steam_games.csv                # Steam 扩展数据
│   └── simulated_user_behaviors.csv   # 模拟用户行为数据
│
├── generated-images/games/            # 游戏封面图（27,109 张，需复制）
│
├── 智能游戏推荐与玩家行为分析系统-实训项目方案.md  # 项目方案文档
└── 20天实训日志（简略版）.md                      # 开发日志
```

---

## 5. 启动系统

### 首次启动（自动构建）

```powershell
.\start.ps1
```

此命令会：
1. 检查 Java / Node.js 是否就绪
2. 构建前端（`vue-cli-service build`）
3. 构建后端（`mvn package -DskipTests`）
4. 清理端口 8080 / 8081
5. 启动后端 → 前端
6. 打开浏览器

### 快速启动（跳过构建）

```powershell
.\start.ps1 -Quick
```

适用于第二次及以后的启动，前提是 `target/` 和 `dist/` 已存在且不需要重新构建。

### 环境诊断

```powershell
.\start.ps1 -Setup
```

检查环境中 Java、Node.js、Maven、MySQL 是否就绪。

### 启动后的访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端界面 | http://localhost:8081 | 用户访问入口 |
| 后端 API | http://localhost:8080 | 供前端代理调用 |

---

## 6. 配置说明

### 数据库连接配置

后端通过 `application.yml` 中的环境变量占位符进行配置，可在启动前设置：

```powershell
# 示例：连接到远程 MySQL（可选）
$env:DB_HOST = "192.168.1.100"
$env:DB_PORT = "3306"
$env:DB_USER = "root"
$env:DB_PASSWORD = "my_password"
.\start.ps1
```

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `DB_HOST` | `localhost` | MySQL 主机地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `game_recommend_db` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户 |
| `DB_PASSWORD` | `123456` | 数据库密码 |

### 端口配置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 后端端口 | 8080 | 可在 `application.yml` 中修改 `server.port` |
| 前端端口 | 8081 | 可在 `server.js` 中修改 `PORT` 常量 |

---

## 7. 文件传输方案

将项目迁移到新电脑时，以下文件和目录是**必需**的：

### 最小必需（~60 MB，适合网络传输）

```
智能游戏推荐与玩家行为分析系统/
├── start.bat
├── start.ps1
├── SETUP.md
├── sql/01_create_database.sql
├── scripts/export_database.py
├── scripts/init_database.py
├── game-recommend-system/                # 仅源码，不含 target/
├── game-recommend-frontend/              # 仅源码 + server.js，不含 node_modules/ 和 dist/
├── data/                                 # 原始 CSV 数据（~60 MB）
```

配上数据库 SQL dump 文件（需要从原电脑导出）：
```powershell
# 在原电脑上生成
.\start.ps1 -ExportDB
# 复制 sql/dump_game_recommend_db.sql 到新电脑
```

然后新电脑上：
1. 安装 JDK 17、Node.js、MySQL
2. 初始化数据库： `.\start.ps1 -InitDB`
3. 首次启动：`.\start.ps1`（会自动安装 npm 依赖并构建）

### 完整传输（含构建产物 ~650 MB，U盘/局域网最佳）

包含 `target/`、`dist/`、`node_modules/` 和 `generated-images/` 的完整项目目录，复制后可直接 `.\start.ps1 -Quick` 启动，**完全无需构建过程**。

### 文件大小参考

| 目录/文件 | 大小 | 传输建议 |
|-----------|------|---------|
| `data/` | ~60 MB | 网络/U盘 |
| `sql/dump_game_recommend_db.sql` | ~200 MB | 导出后方可传输 |
| `generated-images/games/` | ~457 MB | 局域网/U盘 |
| `game-recommend-system/target/` | ~50 MB | Maven 构建生成 |
| `game-recommend-frontend/node_modules/` | ~200 MB | `npm install` 生成 |
| `game-recommend-frontend/dist/` | ~10 MB | `npm run build` 生成 |

---

## 8. 常见问题

### Q: MySQL 连接失败

```
[ERROR] Communications link failure
```

**排查**：
1. 确认 MySQL 服务已启动：`net start MySQL80`
2. 确认用户名密码正确
3. 尝试命令行连接：`mysql -u root -p`
4. 如需修改密码，设置环境变量：`$env:DB_PASSWORD = "新密码"`

### Q: 启动后页面空白（控制台 404）

**原因**：前端未正确构建或 dist 目录不存在。
**解决**：
```powershell
# 删除旧的 dist，重新构建
Remove-Item game-recommend-frontend/dist -Recurse -Force -ErrorAction SilentlyContinue
.\start.ps1
```

### Q: 端口 8080 被占用

```powershell
# 查看谁占用了端口
netstat -ano | findstr :8080
# 在启动脚本中会自动清理，也可以手动结束进程
taskkill /f /pid <PID>
```

### Q: 前端 build 失败（node-gyp / 权限错误）

```powershell
# 清理并重装 node_modules
cd game-recommend-frontend
Remove-Item node_modules -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item package-lock.json -ErrorAction SilentlyContinue
npm install
```

### Q: 游戏图片不显示

**原因**：`generated-images/games/` 目录不存在或图片不完整。
**解决**：将原电脑上的 `generated-images/` 目录完整复制到新电脑的项目根目录。

### Q: 后端编译时 Maven 下载依赖超时

```powershell
# 设置 Maven 使用国内镜像（在 Maven 的 conf/settings.xml 中添加）
<mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

---

> **最后更新**：2026-07-16
