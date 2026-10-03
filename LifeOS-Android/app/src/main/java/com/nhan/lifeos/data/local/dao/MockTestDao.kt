package com.nhan.lifeos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhan.lifeos.data.local.entity.MockTestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MockTestDao {
    @Query("SELECT * FROM mock_tests ORDER BY date DESC")
    fun getAllMockTests(): Flow<List<MockTestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTest(test: MockTestEntity)

    @Query("DELETE FROM mock_tests WHERE id = :id")
    suspend fun deleteMockTest(id: String)

    @Query("DELETE FROM mock_tests")
    suspend fun clearAll()
}
