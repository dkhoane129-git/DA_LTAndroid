package com.spendwise.app.database

import androidx.room.TypeConverter
import com.spendwise.app.model.TransactionType

class Converters {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}
