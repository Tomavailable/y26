package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE bookId = :bookId ORDER BY id ASC")
    fun getWordsForBook(bookId: String): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId")
    suspend fun getWordCountForBook(bookId: String): Int

    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId AND reviewStage > 0")
    suspend fun getLearnedCountForBook(bookId: String): Int

    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId AND isMastered = 1")
    suspend fun getMasteredCountForBook(bookId: String): Int

    @Query("SELECT COUNT(*) FROM words WHERE isMastered = 0 AND reviewStage > 0 AND nextReviewTime <= :currentTime")
    fun getDueReviewCount(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE isMastered = 0 AND reviewStage > 0 AND nextReviewTime <= :currentTime")
    suspend fun getDueReviewCountSync(currentTime: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId AND isMastered = 0 AND reviewStage > 0 AND nextReviewTime <= :currentTime")
    fun getDueReviewCountForBook(bookId: String, currentTime: Long): Flow<Int>

    @Query("SELECT * FROM words WHERE isMastered = 0 AND reviewStage > 0 AND nextReviewTime <= :currentTime ORDER BY nextReviewTime ASC LIMIT :limit")
    suspend fun getDueReviewWords(currentTime: Long, limit: Int = 50): List<WordEntity>

    @Query("SELECT * FROM words WHERE bookId = :bookId AND isMastered = 0 AND reviewStage > 0 AND nextReviewTime <= :currentTime ORDER BY nextReviewTime ASC LIMIT :limit")
    suspend fun getDueReviewWordsForBook(bookId: String, currentTime: Long, limit: Int = 50): List<WordEntity>

    @Query("SELECT * FROM words WHERE bookId = :bookId AND reviewStage = 0 AND isMastered = 0 ORDER BY id ASC LIMIT :limit")
    suspend fun getNewWordsForBook(bookId: String, limit: Int = 30): List<WordEntity>

    @Query("SELECT * FROM words WHERE word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' LIMIT 100")
    fun searchWords(query: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE isFavorite = 1 ORDER BY lastReviewTime DESC")
    fun getFavoriteWords(): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words")
    fun getTotalWordsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE reviewStage > 0")
    fun getTotalLearnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE isMastered = 1")
    fun getTotalMasteredCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Update
    suspend fun updateWord(word: WordEntity)

    @androidx.room.Delete
    suspend fun deleteWords(words: List<WordEntity>)

    @Query("SELECT * FROM words WHERE bookId = :bookId")
    suspend fun getWordsForBookSync(bookId: String): List<WordEntity>

    @Query("SELECT * FROM words")
    suspend fun getAllWordsSync(): List<WordEntity>

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteWordById(id: Long)

    @Query("DELETE FROM words WHERE bookId = :bookId")
    suspend fun deleteWordsByBook(bookId: String)
}
