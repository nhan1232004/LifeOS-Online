package com.nhan.lifeos.data.repository

import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.local.entity.MockTestEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MockTestRepository(
    private val database: LifeOSDatabase,
    private val cloudSyncRepo: CloudSyncRepository? = null
) {
    val allMockTests: Flow<List<MockTestEntity>> = database.mockTestDao().getAllMockTests()

    suspend fun insertMockTest(
        name: String,
        date: String,
        list: Double,
        read: Double,
        speak: Double,
        write: Double,
        note: String = "",
        existingId: String? = null
    ) {
        val total = list + read + speak + write
        val test = MockTestEntity(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name,
            date = date,
            list = list,
            read = read,
            speak = speak,
            write = write,
            total = total,
            note = note,
            updatedAt = System.currentTimeMillis()
        )
        database.mockTestDao().insertMockTest(test)
        cloudSyncRepo?.triggerAutoSync("mocktests")
    }

    suspend fun deleteMockTest(id: String) {
        database.mockTestDao().deleteMockTest(id)
        cloudSyncRepo?.triggerAutoSync("mocktests")
    }
}
