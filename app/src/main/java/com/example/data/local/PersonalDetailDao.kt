package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalDetailDao {
    @Query("SELECT * FROM personal_details ORDER BY date DESC, id DESC")
    fun getAllPersonalDetails(): Flow<List<PersonalDetailEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalDetail(personalDetail: PersonalDetailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(personalDetails: List<PersonalDetailEntity>)

    @Query("DELETE FROM personal_details WHERE id = :id")
    suspend fun deletePersonalDetailById(id: Int)

    @Query("DELETE FROM personal_details")
    suspend fun deleteAll()
}
