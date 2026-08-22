# 小肚兜 AI

> 母婴阶段菜谱浏览、收藏与饮食记录工具；AI 能力尚未达到生产开放条件。

## 项目状态

- 🚧 当前阶段：**上线可靠性加固，生产外部前置检查仍未通过**
- 📅 启动日期：2026-05-23
- 🎯 当前可用主链路：菜谱浏览、收藏、打卡和账号注销

## 技术栈

### 后端 (xiaodudou-server)
- Java 17 + Spring Boot 3.5.16
- MyBatis-Plus 3.5.17 + MySQL 8 + Redis 7
- Sa-Token 1.46.0 鉴权 + springdoc（仅非生产环境）
- Flyway DB 迁移 + Hutool + Lombok

### 前端 (xiaodudou-miniapp)
- uni-app + Vue 3 + TypeScript
- Pinia 状态管理
- wot-design-uni UI 库
- Vite 构建

### AI 服务
- 生产环境由后端安全闸门强制关闭，当前不可作为上线能力。
- 开发环境可显式开启真实或带明确标识的 Mock 进行测试。
- 恢复生产 AI 前必须完成真实内容安全审核供应商接入与专项验收。

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

> 设计与历史规划文档位于仓库 `docs/xiaodudou-ai-docs/`。标记为历史草案的内容不代表已交付能力。

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
# 通过环境变量或本地忽略配置注入 DB_PASSWORD、REDIS_PASSWORD 等凭据
mvn spring-boot:run -Dspring-boot.run.profiles=dev
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

- [ ] 由法务确认适用的 AI、算法、备案、类目及个人信息保护要求
- [ ] 确认真实运营主体、有效联系方式和平台审核材料
- [ ] 敏感画像加密已实现；密钥托管/轮换、加密灾备、恢复演练、最小权限和访问审计仍需生产验收。生产密钥丢失会导致对应画像永久不可解密，禁止临时重生成替代。
- [ ] 内容安全审核接入并完成失败降级、拦截测试
- [ ] AI 日志最小化和自动清理已实现；保留周期仍需法务确认并完成生产配置与审计验收
- [ ] 真实微信 AppID、合法域名、登录和图片上传真机验收

## 上线安全检查

```powershell
# 扫描 Git 跟踪及待提交文本文件，不输出命中的敏感内容
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\repository-safety-check.ps1

# 验证凭据泄漏与虚假加密承诺两类规则本身有效
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\repository-safety-check.ps1 -SelfTest
```

## 项目责任人

真实责任人和有效联系方式尚未在仓库确认，正式上线前必须补齐，不在文档中编造。
