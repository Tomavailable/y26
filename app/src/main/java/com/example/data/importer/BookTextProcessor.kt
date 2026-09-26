package com.example.data.importer

import com.example.data.dict.CocaFrequencyDictionary
import com.example.data.model.BookEntity
import com.example.data.model.BookSentenceEntity
import java.util.UUID

object BookTextProcessor {

    data class ProcessResult(
        val book: BookEntity,
        val sentences: List<BookSentenceEntity>,
        val matchedWordCount: Int
    )

    /**
     * Processes raw text file content from a book, extracts valid sentences,
     * matches them with COCA 10,000 words, and returns structured entities.
     */
    fun processBookText(
        title: String,
        fileName: String,
        rawText: String
    ): ProcessResult {
        val bookId = "book_" + UUID.randomUUID().toString().take(8)
        val cleanTitle = title.ifBlank { fileName.substringBeforeLast(".").ifBlank { "自定义图书" } }

        if (rawText.isBlank()) {
            val emptyBook = BookEntity(id = bookId, title = cleanTitle, fileName = fileName, sentenceCount = 0, wordCount = 0)
            return ProcessResult(emptyBook, emptyList(), 0)
        }

        // Split text into candidate sentences
        val sentenceRegex = Regex("[.!?\\n]+")
        val rawSentences = rawText.split(sentenceRegex)

        val matchedSentences = mutableListOf<BookSentenceEntity>()
        val matchedWordsSet = mutableSetOf<String>()
        val wordSentenceCountMap = mutableMapOf<String, Int>()

        for (rawSent in rawSentences) {
            val sentence = rawSent.trim().replace(Regex("\\s+"), " ")
            if (sentence.length !in 15..350) continue
            if (!sentence.any { it in 'a'..'z' || it in 'A'..'Z' }) continue

            // Extract words in sentence with word boundary constraints
            val wordsInSent = Regex("\\b[a-zA-Z]+\\b")
                .findAll(sentence)
                .map { it.value.lowercase() }
                .distinct()
                .toList()

            for (w in wordsInSent) {
                if (w.length < 2) continue
                // Ensure exact word boundary match in sentence to avoid substring matches
                val matchesWholeWord = sentence.contains(Regex("\\b${Regex.escape(w)}\\b", RegexOption.IGNORE_CASE))
                if (!matchesWholeWord) continue

                val rank = CocaFrequencyDictionary.getRank(w)
                if (rank != null) {
                    val count = wordSentenceCountMap.getOrDefault(w, 0)
                    // Limit up to 5 sentences per word per book for database efficiency
                    if (count < 5) {
                        wordSentenceCountMap[w] = count + 1
                        matchedWordsSet.add(w)
                        matchedSentences.add(
                            BookSentenceEntity(
                                bookId = bookId,
                                word = w,
                                sentence = sentence
                            )
                        )
                    }
                }
            }
        }

        val bookEntity = BookEntity(
            id = bookId,
            title = cleanTitle,
            fileName = fileName,
            sentenceCount = matchedSentences.size,
            wordCount = matchedWordsSet.size,
            createdAt = System.currentTimeMillis()
        )

        return ProcessResult(bookEntity, matchedSentences, matchedWordsSet.size)
    }
}
