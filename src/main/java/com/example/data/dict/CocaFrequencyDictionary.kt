package com.example.data.dict

import android.content.Context
import com.example.data.model.WordEntity
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

object CocaFrequencyDictionary {
    private val rankMap = mutableMapOf<String, Int>()
    private val entryMap = mutableMapOf<String, DictEntry>()
    private val longmanMeaningMap = mutableMapOf<String, String>()
    private var isLoaded = false

    private fun parseExamples(rawJson: String): Pair<String, String> {
        if (rawJson.isBlank() || !rawJson.trim().startsWith("[")) return Pair("", "")
        return try {
            val array = JSONArray(rawJson.trim())
            val enList = mutableListOf<String>()
            val cnList = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val en = obj.optString("en", "").trim()
                val cn = obj.optString("cn", "").trim()
                if (en.isNotBlank()) enList.add(en)
                if (cn.isNotBlank()) cnList.add(cn)
            }
            Pair(enList.joinToString(" ||| "), cnList.joinToString(" ||| "))
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    fun init(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                // Load cleaned Longman meanings for learning/review mode
                try {
                    context.assets.open("longman_meanings_15000.tsv").use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                            var lineIndex = 0
                            for (line in lines) {
                                lineIndex++
                                if (lineIndex == 1) continue
                                val parts = line.split('\t')
                                if (parts.size >= 2) {
                                    val word = parts[0].trim().lowercase()
                                    val trans = parts[1].replace("\\n", "\n").trim()
                                    if (word.isNotBlank()) {
                                        longmanMeaningMap[word] = trans
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Load base ECDICT COCA dictionary
                context.assets.open("ecdict_coca15000.tsv").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                        var lineIndex = 0
                        for (line in lines) {
                            lineIndex++
                            if (lineIndex == 1) continue // Skip header row
                            val parts = line.split('\t')
                            if (parts.isNotEmpty()) {
                                val rank = lineIndex - 1
                                val word = parts[0].trim().lowercase()
                                if (word.isBlank()) continue
                                val uk = parts.getOrNull(1)?.trim() ?: ""
                                val us = parts.getOrNull(2)?.trim() ?: ""
                                val definition = parts.getOrNull(3)?.replace("\\n", "\n")?.trim() ?: ""
                                val translation = parts.getOrNull(4)?.replace("\\n", "\n")?.trim() ?: ""
                                val exchange = parts.getOrNull(5)?.trim() ?: ""
                                val etymology = parts.getOrNull(6)?.replace("\\n", "\n")?.trim() ?: ""
                                val examplesJson = parts.getOrNull(7)?.trim() ?: ""

                                val phoneticStr = when {
                                    uk.isNotBlank() && us.isNotBlank() -> "🇬🇧 $uk 🇺🇸 $us"
                                    uk.isNotBlank() -> uk
                                    us.isNotBlank() -> us
                                    else -> ""
                                }
                                val (exEn, exCn) = parseExamples(examplesJson)
                                val formattedEtym = EcdictFormatter.formatEtymology(etymology)
                                val formattedExch = EcdictFormatter.formatExchange(word, exchange)
                                val notesContent = when {
                                    formattedEtym.isNotBlank() && formattedExch.isNotBlank() -> "$formattedEtym\n【词形】$formattedExch"
                                    formattedEtym.isNotBlank() -> formattedEtym
                                    formattedExch.isNotBlank() -> "【词形】$formattedExch"
                                    else -> ""
                                }

                                val originalTranslation = translation
                                val cleanMeaning = longmanMeaningMap[word] ?: originalTranslation

                                rankMap[word] = rank
                                entryMap[word] = DictEntry(
                                    word = word,
                                    phonetic = phoneticStr,
                                    pos = "",
                                    meaning = cleanMeaning,
                                    originalMeaning = originalTranslation,
                                    exampleSentence = exEn,
                                    exampleTranslation = exCn,
                                    notes = notesContent,
                                    definition = definition,
                                    exchange = exchange
                                )
                            }
                        }
                    }
                }
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Returns the COCA frequency rank (1..10000) for the given word, or null if not in top 10k.
     */
    fun getRank(word: String): Int? {
        val clean = word.trim().lowercase()
        return rankMap[clean]
    }

    /**
     * Looks up full dictionary entry from the COCA dictionary.
     */
    fun lookup(word: String): DictEntry? {
        val clean = word.trim().lowercase()
        return entryMap[clean]
    }

    /**
     * Enriches a word with COCA frequency rank and missing meanings/phonetics.
     */
    fun enrichWord(word: WordEntity): WordEntity {
        val clean = word.word.trim().lowercase()
        val rank = word.frequencyRank ?: rankMap[clean]
        val entry = entryMap[clean]
        return if (entry != null) {
            word.copy(
                frequencyRank = rank,
                phonetic = word.phonetic.ifBlank { entry.phonetic },
                pos = word.pos.ifBlank { entry.pos },
                meaning = word.meaning.ifBlank { entry.meaning },
                originalMeaning = word.originalMeaning.ifBlank { entry.originalMeaning },
                definition = word.definition.ifBlank { entry.definition },
                exampleSentence = word.exampleSentence.ifBlank { entry.exampleSentence },
                exampleTranslation = word.exampleTranslation.ifBlank { entry.exampleTranslation },
                notes = if (word.notes.isBlank() || word.notes.startsWith("p:") || word.notes.startsWith("s:")) entry.notes.ifBlank { word.notes } else word.notes
            )
        } else {
            word.copy(frequencyRank = rank)
        }
    }

    /**
     * Reads all words for a given book section directly from the asset file.
     */
    fun loadWordsForBook(context: Context, bookId: String): List<WordEntity> {
        init(context)
        val (startRank, endRank) = when (bookId) {
            "coca_1_3500" -> Pair(1, 3500)
            "coca_3500_7000" -> Pair(3501, 7000)
            "coca_7000_10000" -> Pair(7001, 10000)
            "coca_10000_15000" -> Pair(10001, 15000)
            else -> Pair(1, 15000)
        }
        val result = mutableListOf<WordEntity>()
        val seenWords = mutableSetOf<String>()
        try {
            context.assets.open("ecdict_coca15000.tsv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                    var lineIndex = 0
                    for (line in lines) {
                        lineIndex++
                        if (lineIndex == 1) continue // Skip header
                        val rank = lineIndex - 1
                        if (rank in startRank..endRank) {
                            val parts = line.split('\t')
                            if (parts.isNotEmpty()) {
                                val word = parts[0].trim()
                                if (word.isBlank() || seenWords.contains(word.lowercase())) continue
                                seenWords.add(word.lowercase())
                                val uk = parts.getOrNull(1)?.trim() ?: ""
                                val us = parts.getOrNull(2)?.trim() ?: ""
                                val definition = parts.getOrNull(3)?.replace("\\n", "\n")?.trim() ?: ""
                                val translation = parts.getOrNull(4)?.replace("\\n", "\n")?.trim() ?: ""
                                val exchange = parts.getOrNull(5)?.trim() ?: ""
                                val etymology = parts.getOrNull(6)?.replace("\\n", "\n")?.trim() ?: ""
                                val examplesJson = parts.getOrNull(7)?.trim() ?: ""

                                val phoneticStr = when {
                                    uk.isNotBlank() && us.isNotBlank() -> "🇬🇧 $uk 🇺🇸 $us"
                                    uk.isNotBlank() -> uk
                                    us.isNotBlank() -> us
                                    else -> ""
                                }
                                val (exEn, exCn) = parseExamples(examplesJson)
                                val formattedEtym = EcdictFormatter.formatEtymology(etymology)
                                val formattedExch = EcdictFormatter.formatExchange(word, exchange)
                                val notesContent = when {
                                    formattedEtym.isNotBlank() && formattedExch.isNotBlank() -> "$formattedEtym\n【词形】$formattedExch"
                                    formattedEtym.isNotBlank() -> formattedEtym
                                    formattedExch.isNotBlank() -> "【词形】$formattedExch"
                                    else -> ""
                                }

                                val originalTranslation = translation
                                val cleanMeaning = longmanMeaningMap[word.lowercase()] ?: originalTranslation

                                result.add(
                                    WordEntity(
                                        bookId = bookId,
                                        word = word,
                                        phonetic = phoneticStr,
                                        meaning = cleanMeaning,
                                        originalMeaning = originalTranslation,
                                        pos = "",
                                        exampleSentence = exEn,
                                        exampleTranslation = exCn,
                                        notes = notesContent,
                                        definition = definition,
                                        frequencyRank = rank
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
