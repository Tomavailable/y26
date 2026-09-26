package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WordBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordBookDao {
    @Query("SELECT * FROM word_books ORDER BY createdAt ASC")
    fun getAllBooks(): Flow<List<WordBookEntity>>

    @Query("SELECT * FROM word_books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): WordBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: WordBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<WordBookEntity>)

    @Update
    suspend fun updateBook(book: WordBookEntity)

    @Query("UPDATE word_books SET totalWords = :count WHERE id = :bookId")
    suspend fun updateWordCount(bookId: String, count: Int)

    @Query("DELETE FROM word_books WHERE id = :id")
    suspend fun deleteBookById(id: String)
}
