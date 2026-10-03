package com.nhan.lifeos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhan.lifeos.data.local.entity.ProjectMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectMessageDao {
    @Query("SELECT * FROM project_messages WHERE projId = :projId ORDER BY timestamp ASC")
    fun getMessagesForProject(projId: String): Flow<List<ProjectMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ProjectMessageEntity)

    @Query("DELETE FROM project_messages WHERE projId = :projId")
    suspend fun deleteMessagesForProject(projId: String)
}
