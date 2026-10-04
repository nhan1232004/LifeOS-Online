package com.nhan.lifeos.data.repository

import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.local.entity.VocabEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class VocabRepository(
    private val database: LifeOSDatabase,
    private val cloudSyncRepo: CloudSyncRepository? = null
) {
    private val vocabDao = database.vocabDao()

    val allVocab: Flow<List<VocabEntity>> = vocabDao.getAllVocab()

    suspend fun insertVocab(
        word: String,
        pron: String,
        type: String,
        mean: String,
        example: String
    ) = withContext(Dispatchers.IO) {
        val vocab = VocabEntity(
            id = UUID.randomUUID().toString(),
            word = word.trim(),
            pron = pron.trim(),
            type = type.trim(),
            mean = mean.trim(),
            example = example.trim(),
            srsLevel = 0,
            updatedAt = System.currentTimeMillis()
        )
        vocabDao.insertVocab(vocab)
        cloudSyncRepo?.triggerAutoSync("vocab")
    }

    suspend fun updateSrsLevel(id: String, level: Int) = withContext(Dispatchers.IO) {
        vocabDao.updateSrsLevel(id, level.coerceIn(0, 3))
        cloudSyncRepo?.triggerAutoSync("vocab")
    }

    suspend fun updateVocab(vocab: VocabEntity) = withContext(Dispatchers.IO) {
        vocabDao.updateVocab(vocab.copy(updatedAt = System.currentTimeMillis()))
        cloudSyncRepo?.triggerAutoSync("vocab")
    }

    suspend fun deleteVocab(id: String) = withContext(Dispatchers.IO) {
        cloudSyncRepo?.recordDeletedItem(id, "vocab")
        vocabDao.deleteById(id)
        cloudSyncRepo?.triggerAutoSync("vocab")
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        // Sample data disabled per user request: start completely clean
    }
}
