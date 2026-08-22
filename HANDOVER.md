# 小肚兜 AI · 项目交接文档

> 版本：v1.0
> 交接日期：2026-05-24
> 接手对象：换工具后的自己 / 新 AI 会话 / 团队成员
> 文档约定：以下所有路径基于 Windows + WSL2 环境

> **2026-08-21 当前上线边界优先于下方历史记录**：生产 AI、社区、会员、支付、退款和营养报告均关闭。AI 客户端代码只用于开发验证，真实内容安全审核供应商未接入验收前不得开启；生产不允许 Mock 或固定结果降级。微信 AppID、真实登录、生产域名、密钥托管、内容安全与法务验收仍是外部前置。

---

## 0. 一分钟项目快照（最重要，先读这个）

| 维度 | 内容 |
|---|---|
| 一句话定位 | **中国版「母婴营养版 Notion」**——用 AI 帮中国妈妈解决"今天给宝宝/产妇做什么" |
| 当前阶段 | **M1 后半段（约 70% 完成）**，可演示，未上线 |
| 切入人群 | 月子/哺乳期妈妈（M1）→ 辅食期（M2）→ 儿童餐（M3） |
| 商业模式 | 阶段会员（599/399/299）+ 食材电商 CPS + 营养师抽佣 + B 端授权 |
| 核心 LTV | 单付费用户预计 865 元 / CAC 167 元 / 回本 3.5 月 |
| 关键凭据 | 智谱 API Key（**已暴露需吊销**，见第 6 节） |
| 当前可用核心链路 | 登录/画像（开发验证）→菜谱浏览→详情→收藏→用户确认餐次和份数后记录；生产 AI 关闭 |

---

## 1. 项目目录全景

```
D:\code\xiaodudou\                    # 主代码仓
├── HANDOVER.md                       # 本文件（最先读）
├── README.md                         # 项目快速启动
├── .gitignore                        # 已严防密钥泄露
├── xiaodudou-server\                 # Java 后端
│   ├── pom.xml
│   ├── spring-boot.log               # 运行日志（运行后产生）
│   └── src\main\
│       ├── java\ai\xiaodudou\
│       │   ├── XiaoDuDouApplication.java
│       │   ├── common\               # Result / Exception / 通用
│       │   ├── config\               # SaToken / Mybatis / WebMvc
│       │   └── module\
│       │       ├── user\             # 用户 + 阶段画像
│       │       ├── recipe\           # 食谱 + 食材
│       │       ├── action\           # 收藏 + 打卡（M1 W2 新增）
│       │       ├── ai\               # AI 接口 + 智谱客户端
│       │       └── system\           # 健康检查
│       └── resources\
│           ├── application.yml       # 主配置（active=dev,local）
│           ├── application-dev.yml   # 开发配置（占位 Key）
│           ├── application-local.yml # ★ 敏感 Key（gitignored）
│           ├── logback-spring.xml
│           └── db\migration\
│               ├── V1__init_schema.sql      # 7 张核心表
│               └── V2__seed_data.sql        # 20 食材 + 8 月子菜谱
└── xiaodudou-miniapp\                # uni-app 小程序
    ├── package.json
    ├── manifest.json                 # ★ 微信 AppID 待填
    ├── index.html
    ├── vite.config.ts / tsconfig.json
    └── src\
        ├── App.vue / main.ts / pages.json
        ├── api\                      # 5 个 API 文件
        │   ├── auth.ts / user.ts / recipe.ts
        │   ├── ai.ts / favorite.ts / checkin.ts
        ├── store\user.ts             # Pinia
        ├── utils\
        │   ├── request.ts            # HTTP 封装 + Token
        │   └── feedback.ts           # 全局 loading/toast
        └── pages\
            ├── index\index.vue       # 首页
            ├── auth\wx-login.vue     # 微信登录
            ├── profile\setup.vue     # 5 步敏感信息确认与画像
            ├── camera\
            │   ├── index.vue         # 拍照
            │   ├── result.vue        # 识别结果
            │   └── recommend.vue     # AI 推荐
            ├── recipe\
            │   ├── list.vue          # 菜谱列表
            │   └── detail.vue        # 菜谱详情
            ├── community\index.vue   # 妈妈圈（占位）
            └── me\
                ├── index.vue         # 我的
                ├── favorites.vue     # 收藏（M1 W2 新增）
                └── checkin.vue       # 打卡日历（M1 W2 新增）

D:\docker\xiaodudou\                  # Docker 编排（不入代码仓）
├── docker-compose.yml                # MySQL 8 + Redis 7
├── .env                              # ★ Docker 密码（gitignored）
├── .gitignore
├── README.md                         # Docker 操作手册
├── install-docker.sh                 # 装 dockerd（已执行过）
├── setup-mirror.sh                   # 配国内镜像源（已执行过）
├── reset-and-start.sh                # 容器重启
├── wait-and-verify.sh                # 容器健康检查
├── wait-sb-windows.sh                # 等 Spring Boot 启动
├── e2e-test.ps1                      # 主流程端到端测试
├── e2e-favorite-checkin.ps1          # 收藏打卡端到端测试
├── mysql\conf\my.cnf
├── mysql\init\01-init.sql
└── redis\conf\redis.conf

D:\code\xiaodudou-ai-docs\            # 5 份产品/方案文档（不入代码仓）
├── 01-小程序原型设计.md
├── 02-M1研发任务清单.md
├── 03-AI接口与Prompt设计.md
├── 04-财务模型与BP提纲.md
└── 05-B端合作话术与报价方案.md

C:\Users\lgt10\
├── .wslconfig                        # WSL 配置（vmIdleTimeout=-1）
├── .m2\settings.xml                  # Maven 阿里云镜像
└── env-backup\                       # JAVA_HOME 修改前的备份
```

---

## 2. 已完成（按时间倒序）

### 2.1 M1 W2 - 收藏 + 打卡闭环 ✅
- **后端**：6 接口（4 收藏 + 3 打卡），1 实体 + 1 Mapper + 1 Controller
- **前端**：收藏列表、打卡日历与详情页收藏；打卡仅允许在详情页由用户确认“已完成/已食用”、餐次、份数后提交，推荐页不直接产生打卡
- **测试**：端到端 12/12 通过（`D:\docker\xiaodudou\e2e-favorite-checkin.ps1`）

### 2.2 核心体验与 AI 安全收口 ✅
- 首页、菜谱、详情、收藏、打卡区分加载/空/错误并提供重试，远程图片有失败占位
- 识别结果页 0 食材完整空状态 + 8 个快速添加常用食材
- 推荐页只展示上架候选强类型结果；生产结构异常明确失败
- 步骤倒计时支持暂停、继续、重置、关闭与离页清理
- 全局 `feedback.ts`（loading/toast/confirm 统一）

### 2.3 AI 开发客户端与生产闸门 ✅
- 仓库保留智谱视觉/文本客户端用于开发环境显式验证，不代表生产选型和合规验收完成
- 生产总闸门强制关闭 AI；控制器后端拒绝，前端隐藏入口，旧直达页诚实显示暂未开放
- 图片上限 5MB，只接受魔数匹配的 JPEG/PNG，并限制尺寸与总像素
- 生产外部失败、超时或结构异常明确失败，绝不自动降级 Mock；开发 Mock 必须显式开启并标注

### 2.4 后端业务模块 ✅
**用户/画像**（A 闭环）
- POST `/api/v1/auth/wx-login`（M1 Mock，待接真实微信 jscode2session）
- POST `/api/v1/auth/logout`
- GET `/api/v1/user/me`
- GET/POST `/api/v1/user/profile`
- DELETE `/api/v1/user/me`（事务清理、账号匿名化、财务记录保留）
- 用户与画像接口使用 DTO 白名单；画像新写入仅保存 AES-GCM 密文，并以 userId 作为附加认证数据防止跨用户替换

**食谱/食材**（B+D 闭环）
- GET `/api/v1/recipes`（分页 + stageTag 筛选 + keyword 搜索）
- GET `/api/v1/recipes/{id}`（含食材关联 + 步骤）
- 种子数据：20 食材 + 8 道月子菜谱

**AI**
- POST `/api/v1/ai/recognize`（multipart）
- POST `/api/v1/ai/recommend`

**收藏/打卡**
- POST/DELETE `/api/v1/favorites/{recipeId}`
- GET `/api/v1/favorites/{recipeId}/check`
- GET `/api/v1/favorites`
- POST `/api/v1/checkin`
- GET `/api/v1/checkin/today`
- GET `/api/v1/checkin/calendar?month=2026-05`

**系统**
- GET `/api/v1/health`
- OpenAPI 文档（仅非生产）：http://localhost:8080/swagger-ui.html

### 2.5 前端业务页面 ✅
| 页面 | 完成度 | 路径 |
|---|---|---|
| 首页 | 95% | `pages/index/index.vue` |
| 微信登录 | 80%（Mock） | `pages/auth/wx-login.vue` |
| 5 步敏感确认与阶段画像 | 95% | `pages/profile/setup.vue` |
| 拍照 | 95% | `pages/camera/index.vue` |
| 识别结果 | 95% | `pages/camera/result.vue` |
| AI 推荐 | 95% | `pages/camera/recommend.vue` |
| 菜谱列表 | 80%（无下拉刷新） | `pages/recipe/list.vue` |
| 菜谱详情 | 90% | `pages/recipe/detail.vue` |
| 我的 | 90% | `pages/me/index.vue` |
| 收藏列表 | 95% | `pages/me/favorites.vue` |
| 打卡日历 | 95% | `pages/me/checkin.vue` |
| 妈妈圈 | 安全关闭 | `pages/community/index.vue`（直达页不请求接口） |

### 2.6 基础设施 ✅
- ✅ JDK 17（环境变量已切换，JDK 8 保留未删）
- ✅ Maven 3.9.15 + 阿里云镜像源
- ✅ Node 24.12 + npm 11.6（淘宝镜像）
- ✅ Docker Engine 29.1.3 在 WSL2 Ubuntu 24.04
- ✅ Docker Compose 2.40.3 + 4 个国内镜像加速器
- ✅ MySQL 8.0.37 容器（端口 3307，named volume 持久化）
- ✅ Redis 7.2.5 容器（端口 6379）
- ✅ Flyway 自动迁移（已发布 V1–V7，不得修改历史迁移）
- ✅ Sa-Token 鉴权（最长 7 天、无操作最长 12 小时，均可配置）
- ✅ springdoc OpenAPI（生产环境安全闸门强制关闭）
- ✅ Lombok + MyBatis-Plus + Hutool
- ✅ WSL keepalive 进程（防止 docker 容器闲置重启）

### 2.7 文档产出 ✅
5 份产品/方案/技术文档，位于 `D:\code\xiaodudou-ai-docs\`：
| 文档 | 内容简述 |
|---|---|
| 01-小程序原型设计 | 5 大核心页面线框图 + IA + User Journey |
| 02-M1研发任务清单 | 4 周 63 人日 WBS + 测试策略 |
| 03-AI接口与Prompt设计 | 接口 schema + Prompt + 降级方案 |
| 04-财务模型与BP提纲 | LTV/CAC + 18月P&L + 融资 BP 模板 |
| 05-B端合作话术与报价方案 | 4 档报价 + 销售 SOP + 异议处理 |

---

## 3. 启动指南（一键复现）

### 3.1 必备依赖检查（按顺序）

```powershell
java -version       # 应为 17.0.12
mvn -version        # 应为 3.9.x，Java version=17
node --version      # 24.x
docker --version    # WSL 内执行：wsl docker --version
```

### 3.2 启动顺序（每次开机/换工具后）

**Step 1: 启动 WSL keepalive（关键！否则 Docker 容器会反复重启）**

```powershell
Start-Process -FilePath 'C:\Windows\System32\wsl.exe' `
  -ArgumentList '-u','root','--','bash','-c','service docker start; tail -f /dev/null' `
  -WindowStyle Hidden -PassThru
```

⚠️ 这个隐藏进程必须一直在跑。关电脑或重启 WSL 后必须重新启动。

**Step 2: 启动 Docker MySQL + Redis**

```powershell
wsl -u root -- bash /mnt/d/docker/xiaodudou/full-restart.sh
```

应看到 `[READY] mysql=healthy redis=healthy`。

**Step 3: 启动 Spring Boot 后端**

```powershell
$env:JAVA_HOME = 'D:\JDK\jdk-17.0.12'
$env:Path = 'D:\JDK\jdk-17.0.12\bin;' + $env:Path
cd D:\code\xiaodudou\xiaodudou-server
cmd /c "set JAVA_HOME=D:\JDK\jdk-17.0.12 && D:\Maven\apache-maven-3.9.15\bin\mvn.cmd spring-boot:run > spring-boot.log 2>&1"
```

或者用 IDEA：打开 `xiaodudou-server` 目录 → 设 Project SDK = JDK 17 → 运行 `XiaoDuDouApplication`。

**Step 4: 启动 uni-app H5 前端**

```powershell
cd D:\code\xiaodudou\xiaodudou-miniapp
npm run dev:h5
```

启动后访问 http://localhost:5173/

### 3.3 端口表

| 服务 | 端口 | 验证 URL |
|---|---|---|
| Spring Boot 后端 | 8080 | http://localhost:8080/api/v1/health |
| OpenAPI 文档（仅非生产） | 8080 | http://localhost:8080/swagger-ui.html |
| Vite H5 前端 | 5173 | http://localhost:5173/ |
| MySQL 8（Docker） | 3307 | jdbc:mysql://localhost:3307/xiaodudou |
| Redis 7（Docker） | 6379 | `docker exec -it xiaodudou-redis redis-cli --askpass PING`（交互输入新密码） |
| 旧 MySQL 5.7（本机） | 3306 | （未启用，仅占用） |

### 3.4 一键端到端测试

```powershell
& 'D:\docker\xiaodudou\e2e-test.ps1'              # 11 步主流程
& 'D:\docker\xiaodudou\e2e-favorite-checkin.ps1'  # 12 步收藏打卡
```

---

## 4. 凭据与敏感配置

### 4.1 ⚠️ 智谱 API Key

| 项 | 值 |
|---|---|
| 文件 | `xiaodudou-server/src/main/resources/application-local.yml`（**已 gitignore**） |
| 当前 Key | **见本地文件**（曾在聊天记录暴露，必须吊销重置） |
| 吊销地址 | https://bigmodel.cn/usercenter/proj-mgmt/apikeys |
| 模型 | 视觉=glm-4v-plus，文本=glm-4-plus |
| 超时 | 60000 ms |
| 配置项 | `xiaodudou.ai.zhipu.enabled=true`；关闭时生产返回明确错误，只有 dev/local 显式开启 Mock 才允许模拟结果 |

**首次拉代码后必须做的**：

```bash
# 1. 注册智谱账号获取 Key https://bigmodel.cn
# 2. 创建文件 xiaodudou-server/src/main/resources/application-local.yml（仓库不含）
# 3. 粘贴模板：
xiaodudou:
  ai:
    zhipu:
      api-key: ${ZHIPU_API_KEY:}
      base-url: https://open.bigmodel.cn/api/paas/v4
      model-vision: glm-4v-plus
      model-chat: glm-4-plus
      timeout-ms: 60000
      enabled: true
```

### 4.2 MySQL / Redis（开发期密码）

密码存于 `D:\docker\xiaodudou\.env`（**不入代码仓**）。

| 服务 | 账号 | 密码位置 |
|---|---|---|
| MySQL root | root | `.env: MYSQL_ROOT_PASSWORD` |
| MySQL 业务 | `xiaodudou` | `.env: MYSQL_PASSWORD` |
| Redis | - | `.env: REDIS_PASSWORD` |

首次启动需手动创建 `.env`（参考仓库 `xiaodudou-server/src/main/resources/application-dev.yml` 中的字段名）。

### 4.3 微信小程序 AppID（待申请）

| 项 | 状态 |
|---|---|
| AppID | **未申请**（manifest.json 里是 `TODO_REPLACE_WITH_REAL_APPID`） |
| 类目 | 母婴 + 餐饮（建议） |
| 注册地址 | https://mp.weixin.qq.com |
| 替换位置 | `xiaodudou-miniapp/src/manifest.json` → `mp-weixin.appid` |
| 影响 | 没有 AppID **可以跑 H5**，跑不了微信开发者工具 |

### 4.4 AI 备案（待启动）

| 项 | 状态 |
|---|---|
| 算法备案 | **未启动** |
| 生成式 AI 备案 | **未启动** |
| 周期 | 30-45 天 |
| 代办成本 | 8,000-15,000 元 |
| 必要性 | **不备案不能正式上线公测**，2024 起多家被下架 |
| 详细步骤 | 参见之前讨论的 11 步流程，搜「网信办算法备案 代办」找代办 |

---

## 5. 已知风险与遗留 bug（按等级）

### 🔴 P0 - 上线阻塞

| # | 风险 | 应对 |
|---|---|---|
| 1 | 智谱 API Key 已泄露 | **立即吊销**重置 |
| 2 | AI 备案未启动 | **本周联系代办** |
| 3 | 微信小程序 AppID 未注册 | 主体备齐后 1-3 天可下 |
| 4 | ICP 备案未做 | 域名解析前需要，20 天周期 |
| 5 | 过敏硬规则已实现前后两次服务端过滤 | 上线前补齐标签数据和真机回归 |
| 6 | AI 调用已实现 Redis 日额度和分钟级限制 | 生产验证 Redis 可用性和告警 |
| 7 | AI 日志已最小化并实现每日清理 | 上线前经法务确认 1..365 天周期并完成生产配置、审计 |
| 8 | 敏感画像已实现 AES-GCM 静态加密和历史明文迁移 | 上线前完成持久密钥托管、灾备、恢复演练、轮换、权限与访问审计；密钥丢失会使对应画像永久不可解密 |

### 🟡 P1 - 体验/运营问题

| # | 问题 | 应对 |
|---|---|---|
| 8 | 营养雷达数据写死 80/60/50 | M2 用 `t_recipe.nutrition × 打卡次数`算 |
| 9 | 计时器只 toast | 做真实倒计时弹层 |
| 10 | 打卡无防刷（1 秒打 100 次也行） | 加节流/限频 |
| 11 | `t_user_recipe_action` 表没 `meal_type` 字段 | V3 迁移加列 |
| 12 | mp-weixin 已可构建，但真实 AppID 未配置 | 配置后用微信开发者工具真机验收 |
| 13 | jscode2session 已实现，生产配置未就绪时会拒绝启动 | 注入真实 AppID/Secret，Mock 仅限 dev/local 显式开启 |
| 14 | 内容审核未接 | 接腾讯云内容安全过滤 AI 输出 |

### 🟢 P2 - 体验细节

| # | 问题 |
|---|---|
| 15 | 菜谱列表无下拉刷新/上拉加载 |
| 16 | 收藏列表无分页（>100 道才需要） |
| 17 | 阶段画像现已支持返回/稍后填写，并在采集前单独确认敏感信息用途 |
| 18 | 登录页已移除外部占位头像，真实头像授权流程仍需产品确认 |
| 19 | PowerShell 控制台显示中文乱码（GBK），实际数据是 UTF-8 |
| 20 | mvn 启动 banner 中文乱码（同上） |

---

## 6. 未完成 - 全量路线图

### 🔴 M1 收尾必做（1-3 天）

- [x] 加 AI 限流（Redis + 用户日额度）
- [x] 加过敏硬规则 Java 端二次过滤
- [x] AI 调用日志落库 `t_ai_call_log`
- [ ] 接腾讯云内容安全审核
- [ ] 微信 AppID 申请 + manifest 替换

### 🟡 M2 - MVP 完整化（5-7 天）

| 页面 | 工时 | 备注 |
|---|---|---|
| 营养报告（暂未开放） | 待专业口径验收 | 当前关闭目标百分比、雷达图与达标判断，仅提供基于主动记录的营养估算 |
| 妈妈圈动态流（只读版） | 2d | 同阶段打卡墙 |
| 引导页（首次启动 3 屏滑动） | 0.5d | 拉新 |
| 设置页（通知/缓存/版本） | 0.5d | |
| 隐私协议 + 用户协议（静态） | 0.5d | **上线必备** |
| 客服 / 帮助中心 | 0.5d | 应用商店要求 |
| 意见反馈 | 0.3d | |
| 404 / 网络异常页 | 0.3d | |

### 🔵 M3 - 商业化（5-8 天）

| 功能 | 工时 | 备注 |
|---|---|---|
| 会员中心（月子卡 599 / 辅食卡 399） | 2d | UI + 套餐展示 |
| 微信支付接入 | 2d | 商户号申请 2-4 周 |
| 订单列表 + 退款流程 | 1.5d | ⚠️ 合规重点 |
| 营养师咨询列表 + 详情 + IM | 3d | M3 中期 |

### 🟢 M4+ - 增长 + 生态

- 邀请有礼（避开微信诱导分享红线）
- 分享海报生成
- 智能家电联动（美的/苏泊尔等）
- 多孩管理（一个账号管两个娃）
- 阶段切换（月子→辅食 续费）
- 后台管理（菜谱 CRUD、用户、订单、AI 调用监控）

### 🔴 M1 后端待补能力

| 功能 | 价值 |
|---|---|
| AI 限流生产监控与压测 | 验证 Redis 故障和高并发行为 |
| AI 日志留存/删除任务 | 自动清理已实现；周期须经法务确认并支持访问审计 |
| 过敏标签数据验收 | 保证两次硬规则过滤的数据基础 |
| 食谱后台导入工具（Excel→SQL） | 让运营填 300 道库 |
| 食谱向量化召回（pgvector / Milvus） | M3 智能推荐升级 |
| 微信真实环境联调 | jscode2session 代码已完成，待真实 AppID/Secret 和真机验证 |
| 内容审核（腾讯云内容安全） | 合规 |

---

## 7. 关键技术决策（避免接手者重复踩坑）

| 决策 | 原因 | 影响 |
|---|---|---|
| WSL2 + Docker Engine（非 Docker Desktop） | 商用免费、贴近生产 | 必须保 keepalive 进程 |
| MySQL 数据卷用 named volume 而非 bind 到 D 盘 | NTFS 不支持 Linux 权限，MySQL 直接挂会启动失败 | D 盘备份用 mysqldump |
| WSL `vmIdleTimeout=-1` + keepalive 进程 | 默认 60 秒闲置就关 WSL，导致 docker 容器反复重启 | 关电脑前 keepalive 进程会丢，开机要重启 |
| WSL networkingMode=NAT（非 mirrored） | mirrored 与 Meta 软件 198.18.0.1 网卡冲突 | localhost forwarding 已正常 |
| uni-app 全家桶固定同一 `3.0.0-5020420260813003` 发行版 | DCloud 同天发布不同包版本不一致会导致 npm 失败 | 升级时全家桶必须一起升并重建锁文件 |
| 本地忽略配置或安全 `.env` 存 Key | 便于本地跨重启复用且不进入仓库 | 永远不要 git add；必须加密备份并验证恢复，丢失后已有画像不可解密 |
| MyBatis-Plus 而非 JPA | 国内事实标准 + 开发效率 | 团队招聘 Java 工程师注意 |
| Sa-Token 而非自建会话方案 | 统一登录态、停用与注销失效策略 | 最长 7 天、无操作最长 12 小时，可配置 |
| AI 失败按环境处理 | 生产返回明确错误；仅 dev/local 且显式开启时允许标注为开发 fallback | 禁止把开发模拟结果伪装成真实识别 |

---

## 8. 关键命令速查（接手必收藏）

```powershell
# === 全部启动 ===
# 1. WSL keepalive
Start-Process wsl -ArgumentList '-u','root','--','bash','-c','service docker start; tail -f /dev/null' -WindowStyle Hidden

# 2. Docker 容器
wsl -u root -- bash /mnt/d/docker/xiaodudou/full-restart.sh

# 3. 后端
cd D:\code\xiaodudou\xiaodudou-server
$env:JAVA_HOME = 'D:\JDK\jdk-17.0.12'
cmd /c "set JAVA_HOME=D:\JDK\jdk-17.0.12 && mvn spring-boot:run"

# 4. 前端
cd D:\code\xiaodudou\xiaodudou-miniapp
npm run dev:h5

# === 验证 ===
curl http://localhost:8080/api/v1/health
curl http://localhost:5173/

# === Docker 运维 ===
wsl docker ps                          # 看容器
wsl docker compose -f /mnt/d/docker/xiaodudou/docker-compose.yml logs -f mysql
wsl docker exec -it xiaodudou-mysql mysql -uxiaodudou -p xiaodudou  # 交互式输入新密码
wsl docker exec -it xiaodudou-redis redis-cli --askpass  # 交互式输入新密码

# === 端到端测试 ===
& 'D:\docker\xiaodudou\e2e-test.ps1'
& 'D:\docker\xiaodudou\e2e-favorite-checkin.ps1'

# === 数据库连接（DBeaver/Navicat 等）===
# Host: localhost  Port: 3307
# User: ${DB_USERNAME}  Pass: ${DB_PASSWORD}（从安全凭据管理系统获取）
# DB:   xiaodudou
```

---

## 9. 接手第一天该做什么

按优先级：

1. **立即轮换所有曾写入仓库或交接材料的凭据**
   - MySQL、Redis、智谱 AI 等旧凭据均按已泄露处理，必须在对应服务端吊销或改密。
   - 新凭据只允许通过环境变量、配置中心或本地忽略配置注入，禁止再次写入 Git、命令示例或聊天记录。
   - 轮换后检查旧凭据已失效，再运行仓库安全扫描与连接验证；不要在验证输出中打印凭据。

2. **本地跑通**（30 分钟）
   - 按第 3 节启动顺序，4 步全跑起来
   - 浏览器开 http://localhost:5173/ 完整走一遍
   - 跑 e2e 脚本验证 11+12 = 23 项全过

3. **熟悉核心代码**（2-3 小时）
   - 先读 `AiController.java`（最复杂）
   - 再读 `pages/camera/index.vue`（前端最复杂）
   - 数据流：登录 → SaToken → Controller → Mapper → MySQL

4. **决定下一步路线**（30 分钟）
   - 看第 6 节 Roadmap
   - 建议优先做 M1 收尾必做的 5 项（合规相关）

5. **启动并行任务**（不写代码也能推进）
   - AI 备案找代办
   - 微信小程序 AppID 申请
   - ICP 备案
   - 找营养师顾问签约

---

## 10. 给接手 AI 的提示（如果换 Claude 实例）

复制下面这段作为新会话的第一条 prompt 即可让 AI 快速进入状态：

```
我是小肚兜 AI 项目的创始人，项目是中国版母婴营养小程序，
当前项目处于上线可靠性加固阶段，技术栈是 Spring Boot 3.5.16 + uni-app + MySQL 8 + Redis 7。生产 AI、支付、社区和营养报告均保持关闭。

代码在 D:\code\xiaodudou\，Docker 在 D:\docker\xiaodudou\，
完整交接文档在 D:\code\xiaodudou\HANDOVER.md，请先读这份文档了解全貌。

接手第一件事：检查所有服务是否在线（按 HANDOVER.md 第 3 节），
然后告诉我下一步建议（参考 HANDOVER.md 第 6 节 Roadmap）。

我倾向先做：[填写已完成安全评审、具备明确数据口径和验收标准的模块]
```

---

## 11. 测试数据

### 11.1 测试账号

Mock 登录任意 `code` 都行，建议固定一个：
```bash
curl -X POST http://localhost:8080/api/v1/auth/wx-login \
  -H "Content-Type: application/json" \
  -d '{"code":"my_test_user","nickname":"测试妈妈"}'
```

### 11.2 种子菜谱 ID

| ID | 菜名 | 阶段 |
|---|---|---|
| 2001 | 番茄炒鸡蛋 | postpartum_early/lactation/weaning |
| 2002 | 红枣小米粥 | postpartum_early/postpartum_late |
| 2003 | 鲫鱼豆腐汤 | lactation |
| 2004 | 猪蹄黄豆汤 | lactation |
| 2005 | 香菇胡萝卜瘦肉粥 | postpartum_late/weaning |
| 2006 | 木耳红枣鸡汤 | postpartum_early/lactation |
| 2007 | 小米南瓜粥 | postpartum_early/weaning/child |
| 2008 | 黑芝麻糊 | lactation/postpartum_late |

### 11.3 种子食材 ID

| 范围 | 类别 |
|---|---|
| 1001-1004 | 蔬菜（番茄/白菜/胡萝卜/南瓜等） |
| 1005-1006 | 肉禽/海鲜（猪蹄/鲫鱼） |
| 1007-1008 | 主食（红枣/小米） |
| 1009-1013 | 肉类（牛腩/排骨/虾等） |
| 1014-1015 | 调味料（姜/葱花） |
| 1016-1018 | 其他（黑芝麻/南瓜/土豆） |
| 1019 | 酒酿 - **月子忌口标记** |
| 1020 | 辣椒 - **月子忌口标记** |

---

## 12. 配套产品文档

| 文档 | 路径 | 内容 |
|---|---|---|
| 01 小程序原型设计 | `D:\code\xiaodudou-ai-docs\01-小程序原型设计.md` | 5 大核心页面线框图 + IA |
| 02 M1 研发任务清单 | `D:\code\xiaodudou-ai-docs\02-M1研发任务清单.md` | 4 周 63 人日 WBS |
| 03 AI 接口与 Prompt 设计 | `D:\code\xiaodudou-ai-docs\03-AI接口与Prompt设计.md` | 接口 schema + Prompt + 降级 |
| 04 财务模型与 BP 提纲 | `D:\code\xiaodudou-ai-docs\04-财务模型与BP提纲.md` | LTV/CAC + 18 月 P&L |
| 05 B 端合作话术与报价方案 | `D:\code\xiaodudou-ai-docs\05-B端合作话术与报价方案.md` | 销售 SOP + 异议处理 |

---

## 13. 紧急联系 / 在线工具

| 工具 | 地址 | 用途 |
|---|---|---|
| 智谱 AI 后台 | https://bigmodel.cn | 吊销/创建 API Key |
| 微信公众平台 | https://mp.weixin.qq.com | 申请小程序 AppID |
| 工信部备案 | https://beian.miit.gov.cn | ICP 备案查询 |
| 网信办算法备案 | https://beian.cac.gov.cn | AI 备案 |
| 腾讯云内容安全 | https://console.cloud.tencent.com/cms | 内容审核 |
| OpenAPI 本地文档 | http://localhost:8080/swagger-ui.html | 仅非生产 API 调试 |
| uni-app 文档 | https://uniapp.dcloud.net.cn/ | 前端框架 |
| MyBatis-Plus 文档 | https://baomidou.com | 后端 ORM |

---

## 14. 版本号

| 组件 | 版本 |
|---|---|
| Java | 17.0.12 LTS |
| Spring Boot | 3.5.16 |
| MyBatis-Plus | 3.5.17 |
| Sa-Token | 1.46.0 |
| springdoc | 2.8.17（仅非生产） |
| MySQL | 8.0.37 |
| Redis | 7.2.5-alpine |
| Node | 24.12.0 |
| uni-app | 3.0.0-5020420260813003 |
| Vue | 3.4.21 |
| Vite | 5.2.8 |
| Pinia | 2.2.4 |
| AI | 生产关闭；恢复需内容安全审核专项验收 |

---

## 15. 一句话总结

**项目已经能完整跑通"登录→画像→拍冰箱→AI 识别→AI 推荐→详情→收藏→打卡"全链路；技术栈现代、文档齐全、Bug 清单清晰；最紧急的事是吊销暴露的智谱 Key 和启动 AI 备案。**

---

**📌 文档维护**：每次大改动后请更新本文件第 2 节（已完成）和第 6 节（Roadmap）。
