package com.spendwise.app.repository

import com.spendwise.app.database.dao.ExchangeRateDao
import com.spendwise.app.database.entity.ExchangeRateEntity
import com.spendwise.app.model.ExchangeRate
import com.spendwise.app.network.ExchangeRateApi
import com.spendwise.app.network.safeApiCall
import com.spendwise.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CurrencyRepository(
    private val api: ExchangeRateApi,
    private val dao: ExchangeRateDao
) {

    /** Các ngoại tệ hiển thị: mã → (tên, mã quốc gia để lấy cờ). */
    private val currencies = linkedMapOf(
        "USD" to ("Đô la Mỹ" to "us"),
        "EUR" to ("Euro" to "eu"),
        "JPY" to ("Yên Nhật" to "jp"),
        "KRW" to ("Won Hàn Quốc" to "kr"),
        "CNY" to ("Nhân dân tệ" to "cn"),
        "GBP" to ("Bảng Anh" to "gb"),
        "AUD" to ("Đô la Úc" to "au"),
        "SGD" to ("Đô la Singapore" to "sg"),
        "THB" to ("Baht Thái" to "th"),
        "CAD" to ("Đô la Canada" to "ca"),
        "TWD" to ("Đô la Đài Loan" to "tw"),
        "HKD" to ("Đô la Hồng Kông" to "hk")
    )

    /**
     * Loading → gọi API → Success (đồng thời lưu cache Room)
     *                   ↘ Error (kèm dữ liệu cache cũ nếu có để vẫn hiển thị offline).
     */
    fun getRates(base: String = BASE): Flow<Resource<List<ExchangeRate>>> = flow {
        emit(Resource.Loading)
        when (val result = safeApiCall { api.latest(base) }) {
            is Resource.Success -> {
                val body = result.data
                val rates = body?.rates
                if (body?.result != "success" || rates == null) {
                    emit(Resource.Error("Máy chủ tỷ giá trả về lỗi", data = cached(base)))
                } else {
                    val updatedAt = (body.updatedAt ?: 0L) * 1000
                    dao.insertAll(rates.map { (code, rate) -> ExchangeRateEntity(code, base, rate, updatedAt) })
                    emit(Resource.Success(toModels(dao.getRates(base))))
                }
            }
            is Resource.Error -> emit(Resource.Error(result.message, result.code, cached(base)))
            Resource.Loading -> Unit
        }
    }

    private suspend fun cached(base: String): List<ExchangeRate>? =
        toModels(dao.getRates(base)).takeIf { it.isNotEmpty() }

    private fun toModels(entities: List<ExchangeRateEntity>): List<ExchangeRate> {
        val byCode = entities.associateBy { it.code }
        return currencies.mapNotNull { (code, info) ->
            val e = byCode[code] ?: return@mapNotNull null
            ExchangeRate(code, info.first, info.second, e.rate, e.updatedAt)
        }
    }

    companion object {
        const val BASE = "VND"
    }
}
