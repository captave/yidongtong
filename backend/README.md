# 易懂通 DeepSeek 后台

此目录是可选代理参考。v1.2.0 Android 应用使用用户自行配置的 API Key 直接调用 DeepSeek，不依赖此服务，也没有代理地址或令牌的构建配置。

DeepSeek API Key 只存放在后台服务器的环境变量中，不进入 Android APK，也不提交到 Git。

## 本地启动

需要 Node.js 20 或更高版本。设置 `DEEPSEEK_API_KEY`、`YIDONGTONG_PROXY_TOKEN` 和可选的 `PORT` 环境变量，然后运行：

```powershell
node .\server.mjs
```

如未来接入此代理，必须通过 HTTPS 提供服务。服务只监听 `127.0.0.1`。示例中的共享令牌仅适合受控测试；生产部署还需要用户身份、短期令牌、设备证明、可信代理配置、共享限流存储和监控。
