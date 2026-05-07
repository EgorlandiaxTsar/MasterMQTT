package com.egorgoncharov.mastermqtt.model.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.egorgoncharov.mastermqtt.model.entity.SettingsProfileEntity
import com.egorgoncharov.mastermqtt.model.types.TTSLanguage
import com.egorgoncharov.mastermqtt.model.types.ThemeOption
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsProfileDao : BaseDao<SettingsProfileEntity> {
    @Query("SELECT * FROM settingsProfiles")
    suspend fun findAll(): List<SettingsProfileEntity>

    @Query("SELECT * FROM settingsProfiles WHERE id = :id")
    suspend fun findById(id: String): SettingsProfileEntity?

    @Query("SELECT * FROM settingsProfiles WHERE id IN (:ids)")
    suspend fun findById(ids: List<String>): List<SettingsProfileEntity>

    @Query("SELECT * FROM settingsProfiles WHERE id = 'main'")
    suspend fun getMainSettingsProfile(): SettingsProfileEntity?

    @Query("SELECT * FROM settingsProfiles")
    fun streamSettingsProfiles(): Flow<List<SettingsProfileEntity>>

    @Query("SELECT * FROM settingsProfiles WHERE id = 'main'")
    fun streamMainSettingsProfile(): Flow<SettingsProfileEntity?>

    @Transaction
    suspend fun createMainSettingsProfileIfNotExists() {
        val mainProfile = getMainSettingsProfile()
        if (mainProfile == null) {
            save(
                SettingsProfileEntity(
                    id = "main",
                    ttsLanguage = TTSLanguage.RU,
                    theme = ThemeOption.SYSTEM,
                    defaultMessageAge = 604800, // 7 days
                    settingsSafetyButtonEnabled = false,
                    showTopicRouteInStream = false
                )
            )
        }
    }

    @Query("DELETE FROM settingsProfiles WHERE id NOT IN (:ids)")
    suspend fun exclusiveDelete(ids: List<String>)

    @Query("DELETE FROM settingsProfiles")
    suspend fun deleteAll()
}