package com.nhan.lifeos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nhan.lifeos.data.local.entity.VocabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocab ORDER BY srsLevel ASC, updatedAt DESC")
    fun getAllVocab(): Flow<List<VocabEntity>>

    @Query("SELECT * FROM vocab WHERE id = :id")
    suspend fun getVocabById(id: String): VocabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocab(vocab: VocabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vocabList: List<VocabEntity>)

    @Update
    suspend fun updateVocab(vocab: VocabEntity)

    @Delete
    suspend fun deleteVocab(vocab: VocabEntity)

    @Query("DELETE FROM vocab WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE vocab SET srsLevel = :level, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSrsLevel(id: String, level: Int, updatedAt: Long = System.currentTimeMillis())
}
