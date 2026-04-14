package com.egorgoncharov.mastermqtt.model.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrokerDao : BaseDao<BrokerEntity> {
    @Query("SELECT * FROM brokers ORDER BY displayIndex")
    suspend fun findAll(): List<BrokerEntity>

    @Query("SELECT * FROM brokers WHERE id = :id ORDER BY displayIndex")
    suspend fun findById(id: String): BrokerEntity?

    @Query("SELECT * FROM brokers WHERE id IN (:ids) ORDER BY displayIndex")
    suspend fun findById(ids: List<String>): List<BrokerEntity>

    @Query("SELECT * FROM brokers WHERE ip || ':' || port = :address")
    suspend fun findByAddress(address: String): BrokerEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM brokers WHERE ip = :ip AND port = :port)")
    suspend fun existsByAddress(ip: String, port: Int): Boolean

    @Query("SELECT * FROM brokers WHERE removed = 0 ORDER BY displayIndex")
    fun streamBrokers(): Flow<List<BrokerEntity>>

    @Query("SELECT * FROM brokers WHERE removed = 0 AND connected = 1 ORDER BY displayIndex")
    fun streamActiveBrokers(): Flow<List<BrokerEntity>>

    @Query("DELETE FROM brokers WHERE id NOT IN (:ids)")
    suspend fun exclusiveDelete(ids: List<String>)

    @Query("DELETE FROM brokers")
    suspend fun deleteAll()

    @Transaction
    @Query(
        """
        UPDATE brokers 
        SET displayIndex = CASE 
            WHEN id = :id1 THEN (SELECT displayIndex FROM brokers WHERE id = :id2)
            WHEN id = :id2 THEN (SELECT displayIndex FROM brokers WHERE id = :id1)
        END 
        WHERE id IN (:id1, :id2)
    """
    )
    suspend fun swapBrokers(id1: String, id2: String)
}