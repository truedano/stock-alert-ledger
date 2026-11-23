package com.example.domain.model

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
}
