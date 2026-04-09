package com.egorgoncharov.mastermqtt.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface BaseDao<T> {
    @RawQuery
    @Transaction
    suspend fun query(query: SupportSQLiteQuery): Int

    @Upsert
    suspend fun save(e: T)

    @Upsert
    suspend fun save(e: List<T>)

    @Delete
    suspend fun delete(e: T);

    @Delete
    suspend fun delete(e: List<T>)
}