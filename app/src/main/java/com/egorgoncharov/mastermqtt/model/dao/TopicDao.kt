package com.egorgoncharov.mastermqtt.model.dao

import androidx.room.Dao
import androidx.room.Query
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao : BaseDao<TopicEntity> {
    @Query("SELECT * FROM topics")
    suspend fun findAll(): List<TopicEntity>;

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun findById(id: String): TopicEntity?

    @Query("SELECT * FROM topics WHERE id IN (:ids)")
    suspend fun findById(ids: List<String>): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE brokerId = :brokerId")
    suspend fun findByBroker(brokerId: String): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE removed = 0")
    fun streamTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE removed = 0 AND brokerId = :brokerId")
    fun streamTopicsByBroker(brokerId: String): Flow<List<TopicEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM topics WHERE brokerId = :brokerId AND topic = :topic)")
    suspend fun existsByTopic(brokerId: String, topic: String): Boolean

    @Query("DELETE FROM topics WHERE id NOT IN (:ids)")
    suspend fun exclusiveDelete(ids: List<String>)
    @Query("DELETE FROM topics")
    suspend fun deleteAll()

    @Query("DELETE FROM topics WHERE brokerId = :brokerId")
    suspend fun deleteByBroker(brokerId: String)
}