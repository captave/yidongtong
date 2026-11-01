# 易懂通 Android

面向老年用户的本地短信语音与安全提醒应用。

当前 v1.2.0 是本地安装测试版本。每位用户可以在 App 内填写自己的 DeepSeek API Key，Key 使用 Android Keystore 保护的 AES-GCM 密钥加密后保存在应用私有空间，不写入源码或 Git。

## 已实现

- 收到短信或已授权应用通知后显示高优先级提醒，并提供“朗读内容”按钮。
- 短信出现“验证码”或连续六位数字时，先由本地规则立即显示可疑提醒。
- 可由用户明确开启或关闭 DeepSeek 云端分析；关闭时不会上传消息，开启后由统一后台判断为安全、可疑或危险。
- 用户明确开启自动同步并授予发短信权限后，真正通过 `SMS_RECEIVED` 收到的高危短信会通知已保存的紧急联系人；普通第三方应用通知不会触发自动短信。
- 自动短信使用系统的发送结果回执记录“正在发送 / 已发送 / 发送失败”，不会仅因调用了 `SmsManager` 就显示发送成功。
- 风险信息可从通知或应用内打开系统拨号器联系紧急联系人。
- 大字号、高对比度、单列布局、大尺寸按钮，并支持浅色/深色主题和普通话语速调节。
- 信息记录保存在应用私有 SQLite 数据库中，只保留近 90 天；支持按号码、正文、判断理由查询，支持删除单条或清空全部。
- DeepSeek API Key 由用户在 App 内自行配置，经 Android Keystore 加密后保存在本机，不写入源码或日志。
- 语音服务只选择不需要网络的普通话声音；未安装离线普通话语音包时拒绝朗读并提示安装。
- 锁屏通知隐藏短信正文，解锁后才显示内容。

云端分析和危险信息短信同步是两个独立开关，均默认关闭。未配置后台时，本地验证码规则和原有提醒仍可正常工作。

## 用 Android Studio 运行

1. 在 Android Studio 中打开 `D:\易懂通-Android`。
2. 等待 Gradle Sync 完成。
3. 连接开启 USB 调试的 Android 手机。
4. 点击 Run 安装。
5. 首次启动后点击“开启短信守护”，授予短信和通知权限。
6. 填写并保存一位可信任的紧急联系人。
7. 如需智能判断，在“安全守护”中填写并保存自己的 DeepSeek API Key，然后开启 DeepSeek 分析。
8. 如需同步危险信息，再开启“危险信息自动短信通知联系人”并授予发短信权限。

应用不读取通讯录，也不申请直接拨号权限。点击紧急联系人按钮只打开系统拨号器，仍需用户确认拨出。仅在用户保存自己的 API Key、阅读确认说明并开启云端分析后，后续收到的短信以及“通知使用权”读取到的通知正文才会直接发送到 DeepSeek。

## 可选后台代理参考

`backend` 目录仍保留零第三方依赖的 Node.js 20 后台代理参考，但当前 Android 版本不使用它。若未来改回统一后台，真实 DeepSeek Key 只应设置在服务器环境变量中：

```powershell
$env:DEEPSEEK_API_KEY = '在服务器上填写真实 Key'
$env:YIDONGTONG_PROXY_TOKEN = '一段足够长的随机访问令牌'
$env:PORT = '8787'
node .\backend\server.mjs
```

正式环境必须通过 HTTPS 反向代理或云函数对外提供服务，并增加用户/设备身份、短期可撤销令牌、设备证明、主体限流和监控。

## 命令行验证

工程所在上级目录包含中文字符。在部分 Windows/Gradle 组合中，单元测试工作进程无法解析中文类路径。测试时可把构建输出临时放到纯英文目录：

```powershell
$env:JAVA_HOME = 'D:\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'D:\Android\GradleHome'
$env:YIDONGTONG_BUILD_DIR = 'D:\Android\builds\yidongtong'
.\gradlew.bat testDebugUnitTest lintDebug
```

正常 APK 构建不需要设置 `YIDONGTONG_BUILD_DIR`：

```powershell
$env:JAVA_HOME = 'D:\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'D:\Android\GradleHome'
$env:YIDONGTONG_BUILD_DIR = 'D:\Android\builds\yidongtong'
.\gradlew.bat assembleDebug
```

Debug APK 输出位置：`D:\Android\builds\yidongtong\outputs\apk\debug\app-debug.apk`。

## 使用限制

- 用户在系统设置中强行停止应用后，Android 会暂停向应用投递短信广播，重新打开一次应用即可恢复。
- 不同手机厂商可能允许用户关闭通知弹窗，请在系统通知设置中保持“短信提醒”渠道开启。
- 手机系统可能保留通知历史，这部分记录由系统通知设置管理，不由易懂通保存。
- DeepSeek 是第三方云端服务，分析准确率不能达到 100%，高风险操作仍应联系家人、银行或警方核实。
- 当前仓库和 APK 均不包含预置 DeepSeek API Key；用户未在本机保存有效 Key 时，App 会阻止开启 DeepSeek。
- 本机加密可以避免明文落盘，但无法防御已经完全控制手机、解锁系统或动态注入应用进程的高级攻击者，适合当前本地安装测试。
- 自动同步要求设备具备 SIM 卡和短信能力；仅有 Wi-Fi 的平板不能实际发送短信。
- 该版本使用 `RECEIVE_SMS` 和 `SEND_SMS` 权限，适合本地安装测试。发布 Google Play 前必须重新评估短信权限政策。
