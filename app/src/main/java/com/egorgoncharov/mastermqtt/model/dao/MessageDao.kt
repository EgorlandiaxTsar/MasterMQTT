package com.egorgoncharov.mastermqtt.model.dao

import androidx.room.Dao
import androidx.room.Query
import com.egorgoncharov.mastermqtt.model.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao : BaseDao<MessageEntity> {
    @Query("SELECT * FROM notifications ORDER BY date DESC")
    suspend fun findAll(): List<MessageEntity>

    @Query("SELECT * FROM notifications WHERE id = :id ORDER BY date DESC")
    suspend fun findById(id: String): MessageEntity?

    @Query("SELECT * FROM notifications WHERE id IN (:ids) ORDER BY date DESC")
    suspend fun findById(ids: List<String>): List<MessageEntity>

    @Query("SELECT * FROM notifications ORDER BY date DESC")
    fun streamNotifications(): Flow<List<MessageEntity>>

    @Query("DELETE FROM notifications WHERE id NOT IN (:ids)")
    suspend fun exclusiveDelete(ids: List<String>)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}