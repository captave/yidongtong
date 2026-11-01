package com.yidongtong.guardian

data class MessageProcessingDecision(
    val assessment: RiskAssessment,
    val shouldForward: Boolean,
) {
    val level: RiskLevel
        get() = assessment.level

    companion object {
        fun combine(localRisky: Boolean, remote: RiskAssessment?): MessageProcessingDecision {
            val local = if (localRisky) {
                RiskAssessment(RiskLevel.SUSPICIOUS, "包含验证码字样或连续六位数字，请先向家人核实")
            } else {
                RiskAssessment(RiskLevel.SAFE, "本地规则未发现明显风险")
            }
            return combine(local, remote)
        }

        fun combine(local: RiskAssessment, remote: RiskAssessment?): MessageProcessingDecision {
            val finalAssessment = if (remote != null && remote.level.ordinal > local.level.ordinal) remote else local
            return MessageProcessingDecision(
                assessment = finalAssessment,
                shouldForward = finalAssessment.level != RiskLevel.SAFE,
            )
        }
    }
}
