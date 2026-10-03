package com.nhan.lifeos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhan.lifeos.data.local.converter.Converters
import com.nhan.lifeos.data.local.dao.EventDao
import com.nhan.lifeos.data.local.dao.GoalDao
import com.nhan.lifeos.data.local.dao.HabitDao
import com.nhan.lifeos.data.local.dao.NoteDao
import com.nhan.lifeos.data.local.dao.ProjectDao
import com.nhan.lifeos.data.local.dao.TodoDao
import com.nhan.lifeos.data.local.dao.TransactionDao
import com.nhan.lifeos.data.local.entity.EventEntity
import com.nhan.lifeos.data.local.entity.GoalEntity
import com.nhan.lifeos.data.local.entity.HabitEntity
import com.nhan.lifeos.data.local.entity.NoteEntity
import com.nhan.lifeos.data.local.entity.ProjectEntity
import com.nhan.lifeos.data.local.entity.TodoEntity
import com.nhan.lifeos.data.local.entity.TransactionEntity

@Database(
    entities = [
        TodoEntity::class,
        EventEntity::class,
        TransactionEntity::class,
        NoteEntity::class,
        HabitEntity::class,
        ProjectEntity::class,
        GoalEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LifeOSDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun eventDao(): EventDao
    abstract fun transactionDao(): TransactionDao
    abstract fun noteDao(): NoteDao
    abstract fun habitDao(): HabitDao
    abstract fun projectDao(): ProjectDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: LifeOSDatabase? = null

        fun getDatabase(context: Context): LifeOSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeOSDatabase::class.java,
                    "lifeos_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
