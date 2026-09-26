package com.example.data.importer

import com.example.data.dict.CocaFrequencyDictionary
import com.example.data.dict.LocalDictionary
import com.example.data.dict.MdxDictionaryManager
import com.example.data.model.WordEntity
import org.json.JSONArray

object WordImporter {

    data class ImportReport(
        val totalProcessed: Int = 0,
        val importedCount: Int = 0,
        val duplicateCount: Int = 0,
        val invalidSpellingCount: Int = 0,
        val invalidWords: List<String> = emptyList(),
        val duplicateWords: List<String> = emptyList()
    )

    const val SAMPLE_CSV = """word,phonetic,meaning,pos,example,translation
serendipity,/ˌserənˈdɪpəti/,机缘巧合；意外发现珍宝的运气,n.,Finding this vintage book was pure serendipity.,偶然淘到这本绝版旧书纯粹是机缘巧合。
eloquent,/ˈeləkwənt/,雄辩的；有说服力的,adj.,She gave an eloquent speech at the international summit.,她在国际峰会上发表了极具说服力的雄辩演说。
epiphany,/ɪˈpɪfəni/,顿悟；突然的领悟,n.,In the middle of the lecture, he had a sudden epiphany.,在听讲座的中途，他猛然有了一种豁然顿悟的感觉。
resilience,/rɪˈrɪliəns/,韧性；复原能力,n.,True resilience is the ability to bounce back from adversity.,真正的坚韧是指在逆境中迅速触底反弹的能力。
mellifluous,/meˈlɪfluəs/,声音甜美的；悦耳动听的,adj.,The cello produced a rich, mellifluous tone.,大提琴流淌出醇厚、悠扬动听的优美乐音。"""

    const val SAMPLE_RAW_WORDS = """epiphany
resilience
catalyst
serendipity
mellifluous
ambiguous
abundant
authentic"""

    const val SAMPLE_JSON = """[
  {
    "word": "catalyst",
    "phonetic": "/ˈkætəlɪst/",
    "meaning": "催化剂；促成变化的人或事物",
    "pos": "n.",
    "exampleSentence": "The technology served as a catalyst for widespread industrial innovation.",
    "exampleTranslation": "这项新技术成为了推动广泛工业创新的强劲催化剂。"
  },
  {
    "word": "solitude",
    "phonetic": "/ˈsɒlɪtjuːd/",
    "meaning": "独处；清静，幽居",
    "pos": "n.",
    "exampleSentence": "She enjoyed the peaceful solitude of walking in the winter forest.",
    "exampleTranslation": "她享受在冬日森林中漫步的那份宁静独处。"
  }
]"""

    fun parse(
        rawText: String,
        bookId: String = "",
        mdxManager: MdxDictionaryManager? = null
    ): List<WordEntity> {
        return parseWithReport(rawText, bookId, mdxManager).first
    }

    fun parseWithReport(
        rawText: String,
        bookId: String = "",
        mdxManager: MdxDictionaryManager? = null
    ): Pair<List<WordEntity>, ImportReport> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return Pair(emptyList(), ImportReport())

        val initialList = when {
            // 1. Try parsing as JSON if starts with [
            trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                val jsonResult = parseJson(trimmed, bookId)
                if (jsonResult.isNotEmpty()) jsonResult else parseDelimitedOrLines(trimmed, bookId, mdxManager)
            }
            // 2. Parse as CSV/TSV or line-by-line formatted text
            else -> parseDelimitedOrLines(trimmed, bookId, mdxManager)
        }

        // Filtering Logic
        val seenWords = mutableSetOf<String>()
        val filteredList = mutableListOf<WordEntity>()
        val duplicateWords = mutableListOf<String>()
        val invalidWords = mutableListOf<String>()

        for (item in initialList) {
            val word = item.word.trim()
            
            // 1. Spelling Validation
            // Basic check: at least 1 char, only English letters, hyphens, spaces, dots (for abbrev), apostrophes
            val isValid = word.isNotEmpty() && word.all { it.isLetter() || it == '-' || it == ' ' || it == '\'' || it == '.' }
            if (!isValid) {
                invalidWords.add(word)
                continue
            }

            // 2. Duplicate Check (Keep first, preserve tenses/different strings)
            val lowerWord = word.lowercase()
            if (seenWords.contains(lowerWord)) {
                duplicateWords.add(word)
                continue
            }

            seenWords.add(lowerWord)
            filteredList.add(item)
        }

        // Seamlessly enrich with MDX (Scheme 2) and LocalDictionary (Scheme 1)
        val enrichedList = filteredList.map { enrichWord(it, mdxManager) }

        val report = ImportReport(
            totalProcessed = initialList.size,
            importedCount = enrichedList.size,
            duplicateCount = duplicateWords.size,
            invalidSpellingCount = invalidWords.size,
            invalidWords = invalidWords,
            duplicateWords = duplicateWords
        )

        return Pair(enrichedList, report)
    }

    private fun enrichWord(word: WordEntity, mdxManager: MdxDictionaryManager?): WordEntity {
        var enriched = word

        // 1. Try MDX external dictionary lookup (if enabled)
        if (mdxManager != null) {
            val mdxItem = mdxManager.lookup(enriched.word)
            if (mdxItem != null) {
                enriched = enriched.copy(
                    phonetic = enriched.phonetic.ifBlank { mdxItem.phonetic },
                    pos = enriched.pos.ifBlank { mdxItem.pos },
                    meaning = enriched.meaning.ifBlank { mdxItem.conciseMeaning },
                    exampleSentence = enriched.exampleSentence.ifBlank { mdxItem.exampleSentence },
                    exampleTranslation = enriched.exampleTranslation.ifBlank { mdxItem.exampleTranslation },
                    notes = enriched.notes.ifBlank { mdxItem.notes }
                )
            }
        }

        // 2. Look up COCA frequency ranking and enrich
        enriched = CocaFrequencyDictionary.enrichWord(enriched)

        // 3. Fallback to LocalDictionary (Scheme 1) for missing fields
        enriched = LocalDictionary.enrichWord(enriched)

        val rawMeaning = enriched.meaning
        val purified = com.example.data.dict.ChineseMeaningPurifier.purify(enriched.word, rawMeaning, enriched.pos)
        return enriched.copy(
            originalMeaning = rawMeaning,
            meaning = purified
        )
    }

    private fun parseJson(jsonString: String, bookId: String): List<WordEntity> {
        val list = mutableListOf<WordEntity>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val word = obj.optString("word").trim()
                if (word.isNotEmpty()) {
                    val phonetic = obj.optString("phonetic", "")
                    val meaning = obj.optString("meaning", obj.optString("translation", ""))
                    val pos = obj.optString("pos", "")
                    val example = obj.optString("example", obj.optString("exampleSentence", ""))
                    val exampleTrans = obj.optString("exampleTranslation", obj.optString("sentenceTranslation", ""))
                    val notes = obj.optString("notes", "")

                    list.add(
                        WordEntity(
                            bookId = bookId,
                            word = word,
                            phonetic = phonetic,
                            meaning = meaning,
                            pos = pos,
                            exampleSentence = example,
                            exampleTranslation = exampleTrans,
                            notes = notes
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore json parse error and fallback
        }
        return list
    }

    private fun parseDelimitedOrLines(
        rawText: String,
        bookId: String,
        mdxManager: MdxDictionaryManager?
    ): List<WordEntity> {
        val list = mutableListOf<WordEntity>()
        val lines = rawText.lines()

        for ((index, originalLine) in lines.withIndex()) {
            val line = originalLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue

            // Check if first line is CSV header (word, translation...)
            if (index == 0 && line.lowercase().contains("word") && (line.contains(",") || line.contains("\t"))) {
                continue
            }

            val entity = parseSingleLine(line, bookId, mdxManager)
            if (entity != null) {
                list.add(entity)
            }
        }
        return list
    }

    private fun parseSingleLine(
        line: String,
        bookId: String,
        mdxManager: MdxDictionaryManager?
    ): WordEntity? {
        // Delimiters to test: Comma, Tab, Semicolon
        val tokens = when {
            line.contains("\t") -> line.split("\t")
            line.contains(",") -> splitCsvLine(line)
            line.contains(" - ") -> line.split(" - ", limit = 2)
            line.contains(" — ") -> line.split(" — ", limit = 2)
            line.contains("：") -> line.split("：", limit = 2)
            line.contains(":") -> line.split(":", limit = 2)
            else -> {
                // Try whitespace splitting
                val regex = Regex("""^([a-zA-Z\s\-']+?)\s+([/\[].*?[/\]])?\s*(.*)$""")
                val match = regex.find(line)
                if (match != null && match.groupValues[3].isNotBlank()) {
                    val w = match.groupValues[1].trim()
                    val p = match.groupValues.getOrNull(2)?.trim() ?: ""
                    val m = match.groupValues.getOrNull(3)?.trim() ?: ""
                    if (w.isNotEmpty()) {
                        return WordEntity(
                            bookId = bookId,
                            word = w,
                            phonetic = p,
                            meaning = m
                        )
                    }
                }
                line.split(Regex("""\s{2,}"""))
            }
        }.map { it.trim().trim('"', '\'') }

        if (tokens.isEmpty()) return null
        val word = tokens[0]
        if (word.isBlank()) return null

        var phonetic = ""
        var meaning = ""
        var pos = ""
        var example = ""
        var exampleTrans = ""

        if (tokens.size == 1) {
            // Single raw word! Will be looked up from MDX or LocalDictionary
            val mdxItem = mdxManager?.lookup(word)
            if (mdxItem != null) {
                phonetic = mdxItem.phonetic
                meaning = mdxItem.conciseMeaning
                pos = mdxItem.pos
                example = mdxItem.exampleSentence
                exampleTrans = mdxItem.exampleTranslation
            } else {
                val dictEntry = LocalDictionary.lookup(word)
                if (dictEntry != null) {
                    phonetic = dictEntry.phonetic
                    meaning = dictEntry.meaning
                    pos = dictEntry.pos
                    example = dictEntry.exampleSentence
                    exampleTrans = dictEntry.exampleTranslation
                } else {
                    meaning = "生词 (暂无本地词义，可自行编辑)"
                }
            }
        } else if (tokens.size == 2) {
            meaning = tokens[1]
        } else if (tokens.size >= 3) {
            // Can be word, phonetic, meaning...
            if (tokens[1].startsWith("/") || tokens[1].startsWith("[") || tokens[1].endsWith("/") || tokens[1].endsWith("]")) {
                phonetic = tokens[1]
                meaning = tokens[2]
                if (tokens.size >= 4) pos = tokens[3]
                if (tokens.size >= 5) example = tokens[4]
                if (tokens.size >= 6) exampleTrans = tokens[5]
            } else {
                meaning = tokens[1]
                pos = tokens[2]
                if (tokens.size >= 4) example = tokens[3]
                if (tokens.size >= 5) exampleTrans = tokens[4]
            }
        }

        // Clean up meaning if POS is prefixed like "n. 苹果"
        if (pos.isEmpty() && meaning.contains(".")) {
            val dotIndex = meaning.indexOf(".")
            if (dotIndex in 1..4) {
                pos = meaning.substring(0, dotIndex + 1).trim()
            }
        }

        return WordEntity(
            bookId = bookId,
            word = word,
            phonetic = phonetic,
            meaning = meaning,
            pos = pos,
            exampleSentence = example,
            exampleTranslation = exampleTrans
        )
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        for (ch in line) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) {
                        sb.append(ch)
                    } else {
                        result.add(sb.toString().trim())
                        sb.setLength(0)
                    }
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}
