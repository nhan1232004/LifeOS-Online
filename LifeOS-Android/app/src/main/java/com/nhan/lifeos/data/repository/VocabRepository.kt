package com.nhan.lifeos.data.repository

import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.local.entity.VocabEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class VocabRepository(private val database: LifeOSDatabase) {
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
    }

    suspend fun updateSrsLevel(id: String, level: Int) = withContext(Dispatchers.IO) {
        vocabDao.updateSrsLevel(id, level.coerceIn(0, 3))
    }

    suspend fun deleteVocab(id: String) = withContext(Dispatchers.IO) {
        vocabDao.deleteById(id)
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val current = vocabDao.getAllVocab().first()
        if (current.isEmpty()) {
            val sampleWords = listOf(
                VocabEntity(
                    id = "v-1",
                    word = "Perseverance",
                    pron = "/ˌpɜː.sɪˈvɪə.rəns/",
                    type = "n",
                    mean = "Sự kiên trì, bền chí theo đuổi mục tiêu",
                    example = "Success in building complex software requires discipline and perseverance.",
                    srsLevel = 2
                ),
                VocabEntity(
                    id = "v-2",
                    word = "Robust",
                    pron = "/rəʊˈbʌst/",
                    type = "adj",
                    mean = "Vững chắc, mạnh mẽ, có khả năng chịu đựng lỗi",
                    example = "The Clean Architecture provides a robust foundation for native mobile apps.",
                    srsLevel = 1
                ),
                VocabEntity(
                    id = "v-3",
                    word = "Serendipity",
                    pron = "/ˌser.ənˈdɪp.ə.ti/",
                    type = "n",
                    mean = "Sự may mắn bất ngờ, tình cờ gặp được điều tốt lành",
                    example = "Discovering this productive system was pure serendipity.",
                    srsLevel = 0
                ),
                VocabEntity(
                    id = "v-4",
                    word = "Ubiquitous",
                    pron = "/juːˈbɪk.wɪ.təs/",
                    type = "adj",
                    mean = "Phổ biến, ở đâu cũng có",
                    example = "Smartphones have become ubiquitous in modern everyday life.",
                    srsLevel = 3
                ),
                VocabEntity(
                    id = "v-5",
                    word = "Meticulous",
                    pron = "/məˈtɪk.jə.ləs/",
                    type = "adj",
                    mean = "Tỉ mỉ, cẩn thận từng chi tiết nhỏ",
                    example = "He is meticulous about tracking every single expense in LifeOS.",
                    srsLevel = 1
                )
            )
            vocabDao.insertAll(sampleWords)
        }
    }
}
