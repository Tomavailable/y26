package com.example.data.dict

import com.example.data.model.WordEntity

object BuiltInDictionaryManager {

    /**
     * Enriches and formats a word based on the user's selected built-in dictionary:
     * - DEFAULT (朗文当代): Longman Contemporary definition and all bilingual examples.
     * - RED_STAR (红星): Red Star core definition & up to 3 high-frequency examples.
     * - CAMBRIDGE_CORE (剑桥核心): Cambridge Core definition & up to 3 essential examples.
     * All other properties (Chinese translation, phonetics, POS, notes, etymology)
     * are extracted from the Default dictionary (LocalDictionary / CocaFrequencyDictionary)
     * to eliminate duplicate content and save memory/resources.
     */
    fun formatForDictionary(
        word: WordEntity,
        dictType: BuiltInDictionary
    ): WordEntity {
        // Step 1: Base enrichment from Default offline dictionary (LocalDictionary & CocaFrequencyDictionary)
        var baseWord = LocalDictionary.enrichWord(CocaFrequencyDictionary.enrichWord(word))
        val entry = CocaFrequencyDictionary.lookup(baseWord.word) ?: LocalDictionary.lookup(baseWord.word)
        if (entry != null) {
            baseWord = baseWord.copy(
                originalMeaning = baseWord.originalMeaning.ifBlank { entry.originalMeaning.ifBlank { entry.meaning } },
                meaning = baseWord.meaning.ifBlank { entry.meaning },
                definition = baseWord.definition.ifBlank { entry.definition },
                exampleSentence = baseWord.exampleSentence.ifBlank { entry.exampleSentence },
                exampleTranslation = baseWord.exampleTranslation.ifBlank { entry.exampleTranslation },
                notes = if (baseWord.notes.isBlank()) entry.notes else baseWord.notes,
                phonetic = baseWord.phonetic.ifBlank { entry.phonetic }
            )
        }

        val rawDef = baseWord.definition.ifBlank {
            entry?.definition ?: ""
        }

        val rawSentences = baseWord.exampleSentence.split(" ||| ").filter { it.isNotBlank() }
        val rawTranslations = baseWord.exampleTranslation.split(" ||| ").filter { it.isNotBlank() }

        return when (dictType) {
            BuiltInDictionary.DEFAULT -> {
                val formattedDef = formatLongmanDefinition(rawDef, baseWord.word, baseWord.meaning)
                baseWord.copy(
                    definition = formattedDef
                )
            }
            BuiltInDictionary.RED_STAR -> {
                val redStarDef = formatRedStarDefinition(rawDef, baseWord.word, baseWord.meaning)
                val limitedSentences = rawSentences.take(3).joinToString(" ||| ")
                val limitedTranslations = rawTranslations.take(3).joinToString(" ||| ")
                baseWord.copy(
                    definition = redStarDef,
                    exampleSentence = limitedSentences,
                    exampleTranslation = limitedTranslations
                )
            }
            BuiltInDictionary.CAMBRIDGE_CORE -> {
                val cambridgeDef = formatCambridgeDefinition(rawDef, baseWord.word, baseWord.meaning)
                val limitedSentences = rawSentences.take(3).joinToString(" ||| ")
                val limitedTranslations = rawTranslations.take(3).joinToString(" ||| ")
                baseWord.copy(
                    definition = cambridgeDef,
                    exampleSentence = limitedSentences,
                    exampleTranslation = limitedTranslations
                )
            }
        }
    }

    private val DICT_NAME_PREFIX_REGEX = Regex("""^(Longman Contemporary\s*\[朗文当代\]|朗文当代·红星考点\s*\[Red Star Core\]|剑桥核心词典\s*\[Cambridge Essentials\]|\[朗文当代\]|\[Red Star Core\]|\[Cambridge Essentials\])\s*[:：]?\s*""")

    private fun formatLongmanDefinition(rawDef: String, word: String, meaning: String): String {
        if (rawDef.isNotBlank()) {
            val lines = rawDef.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            return lines.map { line ->
                val cleanLine = line.replace(DICT_NAME_PREFIX_REGEX, "")
                    .replace(Regex("""^(prep\.|v\.|vt\.|vi\.|n\.|adj\.|adv\.|conj\.)\s*"""), "")
                    .trim()
                cleanLine
            }.filter { it.isNotBlank() }.joinToString("\n")
        }
        return "Key definition for $word ($meaning)"
    }

    private fun formatRedStarDefinition(rawDef: String, word: String, meaning: String): String {
        if (rawDef.isNotBlank()) {
            val lines = rawDef.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            val takeLines = lines.take(2)
            return takeLines.map { line ->
                val cleanLine = line.replace(DICT_NAME_PREFIX_REGEX, "")
                    .replace(Regex("""^(prep\.|v\.|vt\.|vi\.|n\.|adj\.|adv\.|conj\.)\s*"""), "")
                    .trim()
                cleanLine
            }.filter { it.isNotBlank() }.joinToString("\n")
        }
        return "High-yield exam focus definition for $word ($meaning)"
    }

    private fun formatCambridgeDefinition(rawDef: String, word: String, meaning: String): String {
        if (rawDef.isNotBlank()) {
            val lines = rawDef.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            val takeLines = lines.take(2)
            return takeLines.map { line ->
                val cleanLine = line.replace(DICT_NAME_PREFIX_REGEX, "")
                    .replace(Regex("""^(prep\.|v\.|vt\.|vi\.|n\.|adj\.|adv\.|conj\.)\s*"""), "")
                    .trim()
                cleanLine
            }.filter { it.isNotBlank() }.joinToString("\n")
        }
        return "Essential plain-English definition for $word ($meaning)"
    }
}
