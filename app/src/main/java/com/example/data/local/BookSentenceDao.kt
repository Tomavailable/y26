package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BookEntity
import com.example.data.model.BookSentenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookSentenceDao {
    @Query("SELECT * FROM custom_books ORDER BY createdAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM custom_books ORDER BY createdAt DESC")
    suspend fun getAllBooksSync(): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentences(sentences: List<BookSentenceEntity>)

    @Query("SELECT * FROM book_sentences WHERE LOWER(word) = LOWER(:word)")
    fun getSentencesForWord(word: String): Flow<List<BookSentenceEntity>>

    @Query("SELECT * FROM book_sentences WHERE LOWER(word) = LOWER(:word)")
    suspend fun getSentencesForWordSync(word: String): List<BookSentenceEntity>

    @Query("SELECT * FROM book_sentences WHERE LOWER(word) = LOWER(:word) AND bookId = :bookId")
    suspend fun getSentencesForWordAndBook(word: String, bookId: String): List<BookSentenceEntity>

    @Query("DELETE FROM custom_books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query("DELETE FROM book_sentences WHERE bookId = :bookId")
    suspend fun deleteSentencesByBook(bookId: String)
}
