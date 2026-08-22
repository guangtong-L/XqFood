# 小肚兜 AI - 小程序

> uni-app + Vue 3 + TypeScript

## 环境准备

| 软件 | 版本 |
|---|---|
| Node.js | 18+ |
| HBuilderX | 最新版（推荐） |
| 微信开发者工具 | 最新版 |

## 启动步骤（推荐：HBuilderX）

1. 打开 HBuilderX → 文件 → 导入 → 从本地目录导入
2. 选择 `D:\code\xiaodudou\xiaodudou-miniapp`
3. 工具栏「运行」→ 运行到小程序模拟器 → 微信开发者工具
4. 首次会拉起微信开发者工具

> ⚠️ **务必**先在 `manifest.json` 中填入真实的微信小程序 `appid`，否则微信开发者工具会报错。

## 启动步骤（命令行）

```bash
cd D:\code\xiaodudou\xiaodudou-miniapp

# 安装依赖
npm install

# 开发模式（微信小程序）
npm run dev:mp-weixin

# 构建产物在 dist/dev/mp-weixin
# 用微信开发者工具打开该目录
```

## 目录结构

```
xiaodudou-miniapp/
├── package.json
├── manifest.json              # 应用配置（含 appid）
├── pages.json                 # 页面路由 + TabBar
├── vite.config.ts
├── tsconfig.json
├── App.vue
├── main.ts
├── pages/                     # 页面
│   ├── index/                 # 首页
│   ├── camera/                # 拍照 → 识别 → 推荐
│   ├── recipe/                # 菜谱列表 + 详情
│   ├── community/             # 暂未开放的隐私安全占位页
│   ├── me/                    # 我的
│   └── profile/setup.vue      # 阶段画像录入
├── store/                     # Pinia 状态
│   └── user.ts
├── utils/                     # 工具
│   └── request.ts             # 统一请求封装
└── static/                    # 静态资源（图标等，需要补充）
```

## 关键约定

- **API base url**：由 `VITE_API_BASE_URL` 注入；开发可使用 localhost，生产必须是非 localhost 的 HTTPS 地址
- **登录态**：通过响应头 `x-token` 存到 storage，请求时自动注入
- **路由**：除 TabBar 页用 `switchTab`，其它用 `navigateTo`
- **样式**：使用 SCSS + rpx，主色 `#FF8866`（暖橙）

## 待办（按 M1 计划）

- [ ] 补充 static/tabbar/*.png 图标（5 个 TabBar 图标 × 2 态）
- [ ] W2 微信登录闭环
- [ ] W2 阶段画像录入完整表单
- [ ] 完成真实内容安全审核供应商接入与验收后，按变更评审开放生产 AI（当前必须关闭）
- [ ] W3 识别结果增删改 + 推荐结果展示
- [ ] W3 菜谱详情完整页（含步骤计时器）
- [ ] W4 我的页 + 设置完善

## 调试常见问题

| 问题 | 解决 |
|---|---|
| 提示 appid 错误 | manifest.json 填入真实 appid |
| 接口跨域 | 后端使用可配置白名单；生产需配置小程序/H5实际来源并使用 HTTPS |
| 真机请求失败 | 微信后台「服务器域名」必须配置后端域名 |
| TabBar 不显示 | static/tabbar 下图标缺失 |
