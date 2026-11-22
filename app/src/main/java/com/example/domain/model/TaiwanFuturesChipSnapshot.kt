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
)
