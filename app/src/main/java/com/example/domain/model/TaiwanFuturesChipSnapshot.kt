package com.example.domain.model

data class TaiwanFuturesDailyPoint(
    val date: String,
    val foreignTxNetOi: Long,
    val foreignTxOiChange: Long? = null,
    val trustTxNetOi: Long = 0L,
    val dealerTxNetOi: Long = 0L,
    val mtxRetailNetOi: Long = 0L,
    val mtxTotalOi: Long = 0L,
    val mtxRetailRatio: Double? = null,
    val tmfRetailNetOi: Long = 0L,
    val tmfTotalOi: Long = 0L,
    val tmfRetailRatio: Double? = null,
) {
    val shortDateLabel: String
        get() {
            val digits = date.filter { it.isDigit() }
            return if (digits.length == 8) {
                "${digits.substring(4, 6)}/${digits.substring(6, 8)}"
            } else {
                date
            }
        }

    val totalInstitutionalTxNetOi: Long
        get() = foreignTxNetOi + trustTxNetOi + dealerTxNetOi

    val combinedRetailRatio: Double?
        get() {
            val weightedTotal = mtxTotalOi * 5L + tmfTotalOi
            if (weightedTotal <= 0L) return mtxRetailRatio ?: tmfRetailRatio
            val weightedNet = mtxRetailNetOi * 5L + tmfRetailNetOi
            return (weightedNet.toDouble() / weightedTotal.toDouble()) * 100.0
        }
}

data class TaiwanFuturesChipSnapshot(
    val date: String,
    val foreignTxNetOi: Long,
    val foreignTxTradingNet: Long,
    val trustTxNetOi: Long,
    val dealerTxNetOi: Long,
    val mtxRetailNetOi: Long,
    val mtxTotalOi: Long,
    val mtxRetailRatio: Double?,
    val tmfRetailNetOi: Long,
    val tmfTotalOi: Long,
    val tmfRetailRatio: Double?,
    val foreignTxOiChange: Long? = null,
    val trustTxTradingNet: Long = 0L,
    val dealerTxTradingNet: Long = 0L,
    val trustTxOiChange: Long? = null,
    val dealerTxOiChange: Long? = null,
    val foreignTxLongOi: Long = 0L,
    val foreignTxShortOi: Long = 0L,
    val trustTxLongOi: Long = 0L,
    val trustTxShortOi: Long = 0L,
    val dealerTxLongOi: Long = 0L,
    val dealerTxShortOi: Long = 0L,
    val historyPoints: List<TaiwanFuturesDailyPoint> = emptyList(),
) {
    val shortDateLabel: String
        get() {
            val digits = date.filter { it.isDigit() }
            return if (digits.length == 8) {
                "${digits.substring(4, 6)}/${digits.substring(6, 8)}"
            } else {
                date
            }
        }

    val formattedDate: String
        get() {
            val digits = date.filter { it.isDigit() }
            return if (digits.length == 8) {
                "${digits.substring(0, 4)}/${digits.substring(4, 6)}/${digits.substring(6, 8)}"
            } else {
                date
            }
        }

    val totalInstitutionalTxNetOi: Long
        get() = foreignTxNetOi + trustTxNetOi + dealerTxNetOi

    val totalInstitutionalTxTradingNet: Long
        get() = foreignTxTradingNet + trustTxTradingNet + dealerTxTradingNet

    val totalInstitutionalTxOiChange: Long?
        get() {
            val f = foreignTxOiChange ?: return null
            val t = trustTxOiChange ?: 0L
            val d = dealerTxOiChange ?: 0L
            return f + t + d
        }

    /** 以 1 口小台 = 5 口微台的契約價值權重換算為等值小台口數 */
    val combinedMtxEquivalentRetailNetOi: Double
        get() = mtxRetailNetOi.toDouble() + (tmfRetailNetOi.toDouble() / 5.0)

    val combinedMtxEquivalentTotalOi: Double
        get() = mtxTotalOi.toDouble() + (tmfTotalOi.toDouble() / 5.0)

    val combinedRetailRatio: Double?
        get() {
            val weightedTotal = mtxTotalOi * 5L + tmfTotalOi
            if (weightedTotal <= 0L) return mtxRetailRatio ?: tmfRetailRatio
            val weightedNet = mtxRetailNetOi * 5L + tmfRetailNetOi
            return (weightedNet.toDouble() / weightedTotal.toDouble()) * 100.0
        }
}
