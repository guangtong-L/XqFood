# 小肚兜 AI - 后端服务

## 环境准备

| 软件 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | LTS |
| Maven | 3.8+ | |
| MySQL | 8.0+ | 创建数据库 `xiaodudou` |
| Redis | 7.0+ | 默认无密码 |

## 启动步骤

### 1. 创建数据库

```sql
CREATE DATABASE xiaodudou DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 修改配置

打开 `src/main/resources/application-dev.yml`，修改：

```yaml
spring:
  datasource:
    password: 改为你的本地 MySQL 密码
```

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
            └── V1__init_schema.sql       # M1 schema
```

## 关键约定

- **接口前缀**：所有业务接口以 `/api/v1/` 开头
- **错误码**：见 `common/result/ResultCode.java`，按 1xxxx~7xxxx 分段
- **鉴权**：默认所有 `/api/**` 都需登录，免登录接口在 `SaTokenConfig` 中显式放行
- **DB 迁移**：新增表用 `V{n}__xxx.sql`，**禁止直接改 V1**

## 待办（按 M1 计划）

- [ ] W2 用户/档案接口
- [ ] W2 食谱基础库（300 条月子菜谱）
- [ ] W3 AI 识别接口
- [ ] W3 AI 推荐接口
- [ ] W4 内容审核 + 监控告警
