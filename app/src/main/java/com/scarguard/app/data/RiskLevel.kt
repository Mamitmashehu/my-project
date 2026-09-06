package com.scarguard.app.data

/** Combined infection-risk classification shown throughout the UI. */
enum class RiskLevel {
    NORMAL,
    WATCH,
    ALERT;

    companion object {
        fun higherOf(a: RiskLevel, b: RiskLevel): RiskLevel =
            if (a.ordinal >= b.ordinal) a else b
    }
}
