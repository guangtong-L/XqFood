# 小肚兜 AI - 后端服务

## 环境准备

| 软件 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | LTS |
| Maven | 3.8+ | |
| MySQL | 8.0+ | 创建数据库 `xiaodudou` |
| Redis | 7.0+ | 必须配置认证，凭据由环境安全注入 |

## 启动步骤

### 1. 创建数据库

```sql
CREATE DATABASE xiaodudou DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 修改配置

通过环境变量或本地忽略配置注入数据库凭据，不要修改并提交仓库配置：

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:}
```

敏感画像要求注入 `XDD_DATA_ENCRYPTION_KEY`（32 字节随机密钥的 Base64 编码）并妥善管理密钥版本；AI 最小运行日志通过 `AI_LOG_RETENTION_DAYS` 配置 1 至 365 天保留期。密钥不得写入日志、命令输出或前端产物。

本地开发首次启用画像前，应使用系统安全随机数生成器生成 32 字节密钥，例如 `openssl rand -base64 32`，并只写入已被 Git 忽略的本地 `.env` 或 `application-local.yml`。不得在每次启动时重新生成，也不要把生成结果粘贴到聊天、工单或测试日志。至少制作一份加密离线备份，并实际验证能够恢复；本地密钥丢失后，已经写入的画像密文无法解密，只能在确认全部为测试数据后清理并重建。

普通 Spring Boot 进程不会自动加载任意目录下的 `.env`；本地启动时必须通过 IDE 的 env-file 支持、容器 `--env-file` 或不打印变量值的本地启动脚本，将同一份 `XDD_DATA_ENCRYPTION_KEY` 注入进程。可检查“变量存在”和“Base64 解码后为 32 字节”，但禁止打印变量内容。

生产环境必须由密钥管理系统生成、托管和审计密钥，并保存受控的灾备副本。生产密钥丢失属于不可逆数据事故：所有对应版本画像将永久无法解密，不能用“生成新密钥”修复。轮换时必须保留旧版本密钥，完成全量重加密和恢复演练后才能退役旧密钥。

### 3. 启动 Redis

```bash
# Windows
redis-server.exe
# macOS
brew services start redis
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

启动后：
- 应用地址：http://localhost:8080
- 健康检查：http://localhost:8080/api/v1/health
- API 文档：http://localhost:8080/doc.html

### 5. 验证

```bash
curl http://localhost:8080/api/v1/health
# 期望返回：{"code":0,"message":"成功","data":{"status":"UP",...}}
```

## 目录结构

```
xiaodudou-server/
├── pom.xml
└── src/main/
    ├── java/ai/xiaodudou/
    │   ├── XiaoDuDouApplication.java     # 启动类
    │   ├── common/                       # 通用模块
    │   │   ├── result/                   # 统一返回
    │   │   └── exception/                # 异常处理
    │   ├── config/                       # 配置类
    │   │   ├── SaTokenConfig.java        # 鉴权
    │   │   └── WebMvcConfig.java         # 跨域
    │   └── module/                       # 业务模块
    │       └── system/                   # 系统接口（health）
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── logback-spring.xml
        └── db/migration/                 # Flyway 迁移
            ├── V1__init_schema.sql       # 不可变历史 schema
            └── V6__encrypt_profiles_and_minimize_ai_logs.sql
```

## 关键约定

- **接口前缀**：所有业务接口以 `/api/v1/` 开头
- **错误码**：见 `common/result/ResultCode.java`，按 1xxxx~7xxxx 分段
- **鉴权**：默认所有 `/api/**` 都需登录，免登录接口在 `SaTokenConfig` 中显式放行
- **DB 迁移**：新增变更使用下一版本迁移，**禁止修改任何已发布迁移**

## 当前安全边界

- 用户画像通过统一服务加密读写；历史明文画像由生产启动迁移器分批转换。
- 妈妈圈、会员、支付和退款仍保持关闭。
- AI 生产总闸门强制关闭，开发环境也必须同时显式开启功能与真实/Mock 开关才可测试。
- 真实内容安全审核供应商与生产监控尚未完成；完成输入/输出审核、故障失败关闭、安全/法务验收和灰度回归前不得开启 AI，也不能宣称已经具备。
