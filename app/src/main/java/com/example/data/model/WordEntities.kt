package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "word_books")
data class WordBookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val totalWords: Int = 0
)

@Entity(tableName = "custom_books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val fileName: String = "",
    val sentenceCount: Int = 0,
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "book_sentences",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["word"])
    ]
)
data class BookSentenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val word: String,
    val sentence: String,
    val translation: String = ""
)

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["word"]),
        Index(value = ["nextReviewTime"]),
        Index(value = ["reviewStage"])
    ]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val word: String,
    val phonetic: String = "",
    val meaning: String = "",
    val originalMeaning: String = "",
    val pos: String = "",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val notes: String = "",
    val definition: String = "",
    val reviewStage: Int = 0,
    val nextReviewTime: Long = 0L,
    val lastReviewTime: Long = 0L,
    val reviewCount: Int = 0,
    val mistakeCount: Int = 0,
    val isMastered: Boolean = false,
    val isFavorite: Boolean = false,
    val frequencyRank: Int? = null
)

enum class ReviewQuality {
    FORGOT,  // 不认识 (忘记了)
    FUZZY,   // 模糊 (有点印象)
    KNOW     // 认识 (记得)
}

object SpacedRepetitionHelper {
    // 艾宾浩斯经典复习间隔：8小时/第二天、2天(48h)、4天、7天、15天、30天 (1个月)
    val INTERVALS_MS = longArrayOf(
        0L,                           // 0: 未学习
        8 * 60 * 60 * 1000L,          // 1: 8小时后 (或第二天)
        2 * 24 * 60 * 60 * 1000L,     // 2: 2天后 (48小时)
        4 * 24 * 60 * 60 * 1000L,     // 3: 4天后 (4天)
        7 * 24 * 60 * 60 * 1000L,     // 4: 7天后 (1周)
        15 * 24 * 60 * 60 * 1000L,    // 5: 15天后 (半月)
        30 * 24 * 60 * 60 * 1000L     // 6: 30天后 (1个月，完成艾宾浩斯全周期标熟)
    )

    fun calculateNextReview(current: WordEntity, quality: ReviewQuality, now: Long = System.currentTimeMillis()): WordEntity {
        val newReviewCount = current.reviewCount + 1
        return when (quality) {
            ReviewQuality.KNOW -> {
                val newStage = if (current.reviewStage == 0) 1 else (current.reviewStage + 1).coerceAtMost(6)
                val isMastered = newStage >= 6 || current.isMastered
                val interval = INTERVALS_MS[newStage]
                current.copy(
                    reviewStage = newStage,
                    nextReviewTime = now + interval,
                    lastReviewTime = now,
                    reviewCount = newReviewCount,
                    isMastered = isMastered
                )
            }
            ReviewQuality.FUZZY -> {
                val newStage = if (current.reviewStage <= 1) 1 else current.reviewStage - 1
                val interval = 12 * 60 * 60 * 1000L // 半天后再次巩固
                current.copy(
                    reviewStage = newStage,
                    nextReviewTime = now + interval,
                    lastReviewTime = now,
                    reviewCount = newReviewCount
                )
            }
            ReviewQuality.FORGOT -> {
                val newStage = 1
                val interval = INTERVALS_MS[1] // 重回阶段1 (1天后)
                current.copy(
                    reviewStage = newStage,
                    nextReviewTime = now + interval,
                    lastReviewTime = now,
                    reviewCount = newReviewCount,
                    mistakeCount = current.mistakeCount + 1,
                    isMastered = false
                )
            }
        }
    }
}
