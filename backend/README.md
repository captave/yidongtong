# 易懂通 DeepSeek 后台

DeepSeek API Key 只存放在后台服务器的环境变量中，不进入 Android APK，也不提交到 Git。

## 本地启动

需要 Node.js 20 或更高版本。设置 `DEEPSEEK_API_KEY`、`YIDONGTONG_PROXY_TOKEN` 和可选的 `PORT` 环境变量，然后运行：

```powershell
node .\server.mjs
```

生产环境必须放在 HTTPS 反向代理或云函数之后。服务只监听 `127.0.0.1`，不要直接暴露 HTTP 端口。Android Debug 构建可通过 `YIDONGTONG_PROXY_URL` 和 `YIDONGTONG_PROXY_TOKEN` 注入测试地址与令牌；Release 构建强制禁用该测试代理。共享令牌只能降低受控测试中的误用风险；正式上线必须增加用户登录、短期令牌、设备证明、可信代理配置、共享限流存储和监控。
