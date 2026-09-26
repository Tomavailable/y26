package com.example.data.repository

import android.content.Context
import com.example.data.dict.CocaFrequencyDictionary
import com.example.data.importer.BookTextProcessor
import com.example.data.local.BookSentenceDao
import com.example.data.local.WordBookDao
import com.example.data.local.WordDao
import com.example.data.model.BookEntity
import com.example.data.model.BookSentenceEntity
import com.example.data.model.ReviewQuality
import com.example.data.model.SpacedRepetitionHelper
import com.example.data.model.WordBookEntity
import com.example.data.model.WordEntity
import com.example.data.seed.SeedWords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class WordRepository(
    private val wordBookDao: WordBookDao,
    private val wordDao: WordDao,
    private val bookSentenceDao: BookSentenceDao,
    private val context: Context
) {
    val allBooks: Flow<List<WordBookEntity>> = wordBookDao.getAllBooks()
    val allCustomBooks: Flow<List<BookEntity>> = bookSentenceDao.getAllBooks()
    val totalWordsCount: Flow<Int> = wordDao.getTotalWordsCount()
    val totalLearnedCount: Flow<Int> = wordDao.getTotalLearnedCount()
    val totalMasteredCount: Flow<Int> = wordDao.getTotalMasteredCount()
    val favoriteWords: Flow<List<WordEntity>> = wordDao.getFavoriteWords()

    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        // Initialize COCA frequency dictionary
        CocaFrequencyDictionary.init(context)

        // Delete legacy built-in books if they still exist
        val legacyIds = listOf("cet4", "cet6", "kaoyan", "toefl", "daily")
        for (legacyId in legacyIds) {
            val existing = wordBookDao.getBookById(legacyId)
            if (existing != null) {
                wordDao.deleteWordsByBook(legacyId)
                wordBookDao.deleteBookById(legacyId)
            }
        }

        val books = SeedWords.BUILT_IN_BOOKS
        wordBookDao.insertBooks(books)

        for (book in books) {
            val existingWordsMap = wordDao.getWordsForBookSync(book.id).associateBy { it.word.trim().lowercase() }
            val freshWords = CocaFrequencyDictionary.loadWordsForBook(context, book.id)
            if (freshWords.isNotEmpty()) {
                val mergedWords = freshWords.map { fresh ->
                    val old = existingWordsMap[fresh.word.trim().lowercase()]
                    if (old != null) {
                        val original = fresh.originalMeaning.ifBlank { old.originalMeaning.ifBlank { old.meaning } }
                        val conciseMeaning = fresh.meaning.ifBlank { old.meaning }
                        fresh.copy(
                            id = old.id,
                            reviewStage = old.reviewStage,
                            nextReviewTime = old.nextReviewTime,
                            lastReviewTime = old.lastReviewTime,
                            isMastered = old.isMastered,
                            isFavorite = old.isFavorite,
                            reviewCount = old.reviewCount,
                            mistakeCount = old.mistakeCount,
                            originalMeaning = original,
                            meaning = conciseMeaning
                        )
                    } else {
                        fresh
                    }
                }
                wordDao.insertWords(mergedWords)
                wordBookDao.updateWordCount(book.id, mergedWords.size)
            }
        }
    }

    fun getWordsForBook(bookId: String): Flow<List<WordEntity>> =
        wordDao.getWordsForBook(bookId)

    suspend fun getAllWordsSync(): List<WordEntity> = withContext(Dispatchers.IO) {
        wordDao.getAllWordsSync()
    }

    fun getDueReviewCount(currentTime: Long): Flow<Int> =
        wordDao.getDueReviewCount(currentTime)

    fun getDueReviewCountForBook(bookId: String, currentTime: Long): Flow<Int> =
        wordDao.getDueReviewCountForBook(bookId, currentTime)

    suspend fun getDueReviewCountSync(currentTime: Long): Int = withContext(Dispatchers.IO) {
        wordDao.getDueReviewCountSync(currentTime)
    }

    suspend fun getDueReviewWords(currentTime: Long, limit: Int = 50): List<WordEntity> =
        withContext(Dispatchers.IO) {
            wordDao.getDueReviewWords(currentTime, limit)
        }

    suspend fun getDueReviewWordsForBook(bookId: String, currentTime: Long, limit: Int = 50): List<WordEntity> =
        withContext(Dispatchers.IO) {
            wordDao.getDueReviewWordsForBook(bookId, currentTime, limit)
        }

    suspend fun getNewWordsForBook(bookId: String, limit: Int = 30): List<WordEntity> =
        withContext(Dispatchers.IO) {
            wordDao.getNewWordsForBook(bookId, limit)
        }

    suspend fun recordReview(word: WordEntity, quality: ReviewQuality, now: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            val updated = SpacedRepetitionHelper.calculateNextReview(word, quality, now)
            wordDao.updateWord(updated)
        }

    suspend fun decrementMistakeCount(word: WordEntity) = withContext(Dispatchers.IO) {
        if (word.mistakeCount > 0) {
            val updated = word.copy(mistakeCount = word.mistakeCount - 1)
            wordDao.updateWord(updated)
        }
    }

    suspend fun markMastered(word: WordEntity, isMastered: Boolean) = withContext(Dispatchers.IO) {
        val updated = word.copy(
            isMastered = isMastered,
            reviewStage = if (isMastered) 8 else word.reviewStage
        )
        wordDao.updateWord(updated)
    }

    suspend fun batchMarkMastered(wordsToMark: List<String>, bookId: String? = null): Int = withContext(Dispatchers.IO) {
        val targetWords = if (bookId != null) {
            wordDao.getWordsForBookSync(bookId)
        } else {
            wordDao.getAllWordsSync()
        }
        val markSet = wordsToMark.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        val matched = targetWords.filter { it.word.trim().lowercase() in markSet && !it.isMastered }
        val updated = matched.map { it.copy(isMastered = true, reviewStage = 8) }
        if (updated.isNotEmpty()) {
            wordDao.insertWords(updated)
        }
        updated.size
    }

    suspend fun toggleFavorite(word: WordEntity) = withContext(Dispatchers.IO) {
        val updated = word.copy(isFavorite = !word.isFavorite)
        wordDao.updateWord(updated)
    }

    suspend fun createCustomBook(
        title: String,
        description: String,
        words: List<WordEntity>
    ): String = withContext(Dispatchers.IO) {
        val bookId = "custom_" + UUID.randomUUID().toString().take(8)
        val bookEntity = WordBookEntity(
            id = bookId,
            title = title.ifBlank { "我的导入单词书" },
            description = description.ifBlank { "导入于本地自定义单词书" },
            isCustom = true,
            createdAt = System.currentTimeMillis(),
            totalWords = words.size
        )
        wordBookDao.insertBook(bookEntity)

        val wordsToInsert = words.map {
            it.copy(bookId = bookId)
        }
        wordDao.insertWords(wordsToInsert)
        wordBookDao.updateWordCount(bookId, wordsToInsert.size)
        bookId
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        wordDao.deleteWordsByBook(bookId)
        wordBookDao.deleteBookById(bookId)
    }

    fun searchWords(query: String): Flow<List<WordEntity>> =
        wordDao.searchWords(query)

    suspend fun getBookStats(bookId: String): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val total = wordDao.getWordCountForBook(bookId)
        val learned = wordDao.getLearnedCountForBook(bookId)
        val mastered = wordDao.getMasteredCountForBook(bookId)
        Triple(total, learned, mastered)
    }

    fun getSentencesForWord(word: String): Flow<List<BookSentenceEntity>> =
        bookSentenceDao.getSentencesForWord(word)

    suspend fun getSentencesForWordAndBook(word: String, bookId: String): List<BookSentenceEntity> =
        withContext(Dispatchers.IO) {
            bookSentenceDao.getSentencesForWordAndBook(word, bookId)
        }

    suspend fun importCustomBookText(title: String, fileName: String, rawText: String): BookTextProcessor.ProcessResult =
        withContext(Dispatchers.IO) {
            val result = BookTextProcessor.processBookText(title, fileName, rawText)
            bookSentenceDao.insertBook(result.book)
            if (result.sentences.isNotEmpty()) {
                bookSentenceDao.insertSentences(result.sentences)
            }
            result
        }

    suspend fun deleteCustomBook(bookId: String) = withContext(Dispatchers.IO) {
        bookSentenceDao.deleteSentencesByBook(bookId)
        bookSentenceDao.deleteBook(bookId)
    }
}
