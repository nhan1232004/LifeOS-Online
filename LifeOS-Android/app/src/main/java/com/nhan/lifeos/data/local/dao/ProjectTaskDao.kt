package com.nhan.lifeos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhan.lifeos.data.local.entity.ProjectTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectTaskDao {
    @Query("SELECT * FROM project_tasks ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<ProjectTaskEntity>>

    @Query("SELECT * FROM project_tasks WHERE projId = :projId ORDER BY updatedAt DESC")
    fun getTasksByProject(projId: String): Flow<List<ProjectTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ProjectTaskEntity)

    @Query("UPDATE project_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM project_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM project_tasks WHERE projId = :projId")
    suspend fun deleteTasksByProject(projId: String)

    @Query("DELETE FROM project_tasks")
    suspend fun clearAll()
}
