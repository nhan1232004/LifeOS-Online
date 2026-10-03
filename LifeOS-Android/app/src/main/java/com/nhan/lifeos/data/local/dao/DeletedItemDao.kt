package com.nhan.lifeos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhan.lifeos.data.local.entity.DeletedItemEntity

@Dao
interface DeletedItemDao {
    @Query("SELECT * FROM deleted_items WHERE collectionName = :col")
    suspend fun getByCollection(col: String): List<DeletedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DeletedItemEntity)

    @Query("DELETE FROM deleted_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM deleted_items WHERE id = :id AND collectionName = :col")
    suspend fun deleteByIdAndCollection(id: String, col: String)

    @Query("DELETE FROM deleted_items WHERE collectionName = :col")
    suspend fun clearCollection(col: String)
}
