# 小肚兜 AI

> 中国版「母婴营养版 Notion」—— 用 AI 帮中国妈妈解决「今天给宝宝/产妇做什么」

## 项目状态

- 🚀 当前阶段：**M1 开发期**（4 周 / 63 人日）
- 📅 启动日期：2026-05-23
- 🎯 M1 目标：AI 拍食材出菜谱 + 月子阶段画像 + 100 种子用户内测

## 技术栈

### 后端 (xiaodudou-server)
- Java 17 + Spring Boot 3.2.x
- MyBatis-Plus 3.5 + MySQL 8 + Redis 7
- Sa-Token 鉴权 + Knife4j API 文档
- Flyway DB 迁移 + Hutool + Lombok

### 前端 (xiaodudou-miniapp)
- uni-app + Vue 3 + TypeScript
- Pinia 状态管理
- wot-design-uni UI 库
- Vite 构建

### AI 服务
- 智谱 GLM-4V（食材识别 + 菜谱推荐）
- 通义千问（兜底）
- 腾讯云内容安全（输出审核）

## 目录结构

```
xiaodudou/
├── README.md
├── .gitignore
├── docs/                    # 产品/设计/方案文档（见下方索引）
├── xiaodudou-server/        # 后端项目
└── xiaodudou-miniapp/       # 小程序项目
```

## 文档索引

> 完整设计文档位于 `D:\code\xiaodudou-ai-docs\`（独立目录，避免污染代码仓）

| 文档 | 内容 |
|---|---|
| 01-小程序原型设计.md | 5 大核心页面线框图 + IA + User Journey |
| 02-M1研发任务清单.md | 4 周 WBS + 工时表 + 测试策略 |
| 03-AI接口与Prompt设计.md | AI 接口 schema + Prompt + 降级方案 |
| 04-财务模型与BP提纲.md | LTV/CAC 模型 + 18 月 P&L + 融资 BP |
| 05-B端合作话术与报价方案.md | 4 档报价 + 销售 SOP + 异议处理 |

## 启动指南

### 后端启动
```bash
cd xiaodudou-server
# 准备 MySQL（创建数据库 xiaodudou）和 Redis
# 修改 src/main/resources/application-dev.yml 中的数据库密码
mvn spring-boot:run
# 访问 http://localhost:8080/doc.html 看 API 文档
```

### 小程序启动
```bash
cd xiaodudou-miniapp
npm install
# 用 HBuilderX 打开项目 -> 运行 -> 微信开发者工具
# 或命令行：npm run dev:mp-weixin
```

## 环境要求

| 工具 | 版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |
| Node.js | 18+ |
| HBuilderX / 微信开发者工具 | 最新版 |

## 关键风险与合规清单

- [ ] **AI 服务备案**（生成式 AI + 算法备案）—— 30-45 天，**今天启动**
- [ ] **ICP 备案** —— 20 天
- [ ] **微信小程序类目**（母婴 + 餐饮，需营业执照）
- [ ] **食品经营许可证**（涉及"营养建议"可能需要）
- [ ] **个人信息保护法合规**（健康数据加密 + 最小化采集）
- [ ] **内容审核接入**（腾讯云内容安全）
- [ ] **AI 调用日志留存**（≥ 180 天）

## 团队联系

- 创始人：TBD
- 产品负责人：TBD
- 技术负责人：TBD
- 营养师顾问：招募中
