package com.yidongtong.guardian

data class RiskRule(val id: String, val title: String, val level: RiskLevel, val description: String,
    val example: String, val pattern: Regex)

object RiskRuleLibrary {
    private fun rule(id: String, title: String, level: RiskLevel, description: String, example: String, pattern: String) =
        RiskRule(id, title, level, description, example, Regex(pattern, RegexOption.IGNORE_CASE))
    private fun danger(id: String, title: String, example: String, pattern: String) =
        rule(id, title, RiskLevel.DANGEROUS, "命中此规则即标记危险，DeepSeek 不能降低等级。", example, pattern)
    private fun suspicious(id: String, title: String, example: String, pattern: String) =
        rule(id, title, RiskLevel.SUSPICIOUS, "需要核实来源；AI 可以提高风险等级。", example, pattern)

    val rules = listOf(
        danger("D01", "验证码与领取凭证", "验证码1234；OTP A7B9；取件码A-258", "验证码|校验码|验证代码|动态密码|动态口令|短信口令|一次性密码|\\bOTP\\b|取件码|提货码|领取码|兑换码|登录代码|确认码"),
        danger("D02", "链接与网址", "请勿点击陌生链接；访问example.cn", "链接|連結|https?://|www\\.|(?:[a-z0-9-]+\\.)+(?:com|cn|net|org|xyz|top|vip|info|io|app|shop|cc|co|me|在线|中国)(?:\\b|/|$)|(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d+)?|[a-z][a-z0-9+.-]*://"),
        danger("D03", "转账与汇款", "转账已到账；不要向陌生人汇款", "转账|轉賬|转帐|汇款|匯款|打款|转入.{0,12}(账户|帐户|资金)|向.{0,20}(账户|帐户).{0,8}(付款|转入)"),
        danger("D04", "索取账户秘密", "把支付密码告诉客服", "(告诉|提供|发送|回复|告知).{0,20}(密码|恢复码)|把.{0,12}(密码|恢复码).{0,12}(告诉|发给|提供)"),
        danger("D05", "安全账户", "将存款转入安全账户", "安全账[户号]|安全帐[户号]|资金.{0,10}(核验|调查).{0,15}(存款|转移)"),
        danger("D06", "先付费再领取", "先交保证金才能放款", "保证金|解冻费|激活费|先.{0,12}(交|付|缴).{0,20}(提现|退款|领奖|放款|领取)|(?:提现|退款|领奖|放款).{0,20}(手续费|税费|缴费)"),
        danger("D07", "刷单与垫资", "垫资做任务返佣", "刷单|垫资|补单|连单|充值.{0,20}(返本|返佣|返还本金)"),
        danger("D08", "高收益投资诱导", "保本高收益，立即充值", "稳赚|保本高收益|内部.{0,8}(投资|项目)|高收益.{0,20}(充值|付款|投资)"),
        danger("D09", "冒充熟人阻止核实", "妈妈急需交钱，别打电话", "(交钱|付款|借钱|急需).{0,30}(别打电话|不要打电话|保密|别告诉)|(?:保密|别打电话).{0,30}(交钱|付款|借钱)"),
        danger("D10", "远控金融操作", "开启屏幕共享，操作银行账户", "(屏幕共享|共享屏幕|远控|远程控制).{0,40}(银行|账户|支付|金融)|(?:银行|支付).{0,40}(共享屏幕|屏幕共享|远控)"),
        danger("D11", "威胁交钱", "不交钱就曝光照片", "(不交钱|不给钱|不付款).{0,30}(曝光|拘捕|伤害|照片)|(?:曝光|拘捕).{0,20}(交钱|付款)"),
        danger("D12", "付费消除记录", "付费删除不良征信", "(付费|交钱|花钱).{0,20}(征信|违法记录|贷款记录)|(?:删除|消除|修复).{0,12}征信"),
        suspicious("S01", "账户异常催促", "账户异常，今天不处理将停用", "账户.{0,12}(异常|冻结|停用|限制)|银行卡.{0,12}(异常|冻结|限制)"),
        suspicious("S02", "个人资料核验", "回复身份证号码办理认证", "(回复|提供|上传|发送).{0,15}(身份证|银行卡|住址|个人资料)"),
        suspicious("S03", "陌生换号", "爸，我换号码了", "换号码|换号了|新号码|这是我的新号"),
        suspicious("S04", "意外奖励与补贴", "获得补贴3000元，联系专员", "中奖|获奖|领取红包|获得补贴|领取礼品|领取补贴"),
        suspicious("S05", "脱离原平台", "订单异常，添加私人微信处理", "(添加|加|联系).{0,12}(私人微信|私人客服|专员|QQ)|订单异常"),
        suspicious("S06", "医保社保异常", "养老金资格未认证，即将停发", "(医保|社保|养老金|待遇).{0,25}(停发|暂停|过期|认证|失效)"),
        suspicious("S07", "停供催办", "燃气账户异常，立即办理避免停供", "(水费|电费|燃气|话费|物业).{0,25}(停供|停机|异常|立即|紧急)"),
        suspicious("S08", "自动扣费催促", "年度会员即将扣费，立即联系取消", "(会员|订阅|保险).{0,30}(扣费|取消)|自动扣费"),
        suspicious("S09", "执法身份待核实", "您涉及案件，请联系办案人员", "涉嫌|涉案|办案人员|公安机关|法院传票"),
        suspicious("S10", "陌生交友金融话题", "我有赚钱渠道，带你一起做", "赚钱渠道|带你赚钱|博彩|投资内幕"),
        suspicious("S11", "模糊领取与操作", "最后通知，立即扫码领取福利", "扫码|扫描二维码|立即下载|立即领取|最后通知"),
        suspicious("S12", "网址操作引导", "打开网页完成认证", "打开网页|访问网站|网址|网站认证"),
    )

    fun matches(body: String, disabled: Set<String> = emptySet()): List<RiskRule> =
        rules.filter { it.id !in disabled && it.pattern.containsMatchIn(body.take(IncomingMessage.MAX_SCAN_LENGTH)) }

    fun assess(body: String, disabled: Set<String> = emptySet()): RiskAssessment {
        val hits = matches(body, disabled)
        val level = hits.maxByOrNull { it.level.ordinal }?.level ?: RiskLevel.SAFE
        val reason = if (hits.isEmpty()) "暂未发现明显风险（本地规则）" else
            "命中您的规则：" + hits.joinToString("、") { "${it.id} ${it.title}" }.take(130)
        return RiskAssessment(level, reason)
    }

    val scenarios = listOf(
        "快递取件" to "快递已到驿站，取件码A-258。", "购物订单" to "订单已发货，请在购买平台查看。",
        "退款售后" to "把验证码发给客服才能退款。", "银行到账" to "尾号1234账户到账200元。",
        "信用卡账单" to "本期账单已出，请在官方App查看。", "贷款还款" to "先交保证金才能放款。",
        "投资理财" to "保本高收益，立即充值。", "招聘兼职" to "垫资做任务返佣。",
        "中奖礼品" to "您中奖了，请联系专员领奖。", "医保社保" to "养老金资格未认证，即将停发。",
        "补贴退税" to "您的申请已受理。", "公安法院" to "向安全账户汇款。",
        "医院挂号" to "预约就诊时间为明日上午9点。", "水电燃气" to "燃气异常，立即办理避免停供。",
        "通信话费" to "本月套餐剩余流量2GB。", "家人朋友" to "爸，我换号码了。",
        "学校培训" to "周五下午召开家长会。", "工作合同" to "领导要求保密汇款。",
        "出行票务" to "先付解冻费办理赔偿。", "酒店住宿" to "访问example.cn确认预订。",
        "会员保险" to "年度会员即将扣费，请立即取消。", "婚恋交友" to "我有赚钱渠道，带你一起做。",
        "社区通知" to "周六上午开展社区义诊。", "天气营销" to "明日有雨，请注意出行。",
    )
}
