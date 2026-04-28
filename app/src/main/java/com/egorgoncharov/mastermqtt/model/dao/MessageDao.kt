package com.egorgoncharov.mastermqtt.model.dao

import androidx.room.Dao
import androidx.room.Query
import com.egorgoncharov.mastermqtt.model.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao : BaseDao<MessageEntity> {
    @Query("SELECT * FROM messages ORDER BY date DESC")
    suspend fun findAll(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id ORDER BY date DESC")
    suspend fun findById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE id IN (:ids) ORDER BY date DESC")
    suspend fun findById(ids: List<String>): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY date DESC")
    fun streamMessages(): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE id NOT IN (:ids)")
    suspend fun exclusiveDelete(ids: List<String>)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("DELETE FROM messages WHERE topicId IN (SELECT id FROM topics WHERE brokerId = :brokerId)")
    suspend fun deleteByBroker(brokerId: String)

    @Query("DELETE FROM messages WHERE topicId IN (SELECT id FROM topics WHERE id = :topicId)")
    suspend fun deleteByTopic(topicId: String)

    @Query(
        """
    DELETE FROM messages 
    WHERE EXISTS (
        SELECT 1 FROM topics 
        WHERE topics.id = messages.topicId 
        AND (messages.date + (topics.messageAge * 1000)) < :currentTime
    )
    """
    )
    suspend fun deleteOldMessages(currentTime: Long = System.currentTimeMillis())
}