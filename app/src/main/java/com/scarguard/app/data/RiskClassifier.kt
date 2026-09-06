package com.scarguard.app.data

import com.scarguard.app.settings.AlertThresholds

/** Turns a temperature delta and/or redness delta into a single [RiskLevel]. */
object RiskClassifier {

    fun classify(
        temperatureDeltaC: Float?,
        rednessDelta: Float?,
        thresholds: AlertThresholds,
    ): RiskLevel {
        var level = RiskLevel.NORMAL

        temperatureDeltaC?.let { delta ->
            level = RiskLevel.higherOf(level, classifyTemp(delta, thresholds))
        }
        rednessDelta?.let { delta ->
            level = RiskLevel.higherOf(level, classifyRedness(delta, thresholds))
        }
        return level
    }

    private fun classifyTemp(deltaC: Float, thresholds: AlertThresholds): RiskLevel = when {
        deltaC >= thresholds.alertTempDeltaC -> RiskLevel.ALERT
        deltaC >= thresholds.watchTempDeltaC -> RiskLevel.WATCH
        else -> RiskLevel.NORMAL
    }

    private fun classifyRedness(delta: Float, thresholds: AlertThresholds): RiskLevel = when {
        delta >= thresholds.alertRednessDelta -> RiskLevel.ALERT
        delta >= thresholds.watchRednessDelta -> RiskLevel.WATCH
        else -> RiskLevel.NORMAL
    }

    fun adviceFor(level: RiskLevel): String = when (level) {
        RiskLevel.NORMAL -> "No signs of concern. Keep checking daily until your incision is fully healed."
        RiskLevel.WATCH -> "Some change since your baseline. Recheck in a few hours and watch for it getting worse."
        RiskLevel.ALERT -> "Noticeable increase in redness and/or temperature. Contact your care provider."
    }
}
