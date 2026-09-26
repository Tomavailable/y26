package com.example.data.dict

object EcdictFormatter {

    data class FormattedMeaning(
        val pos: String,
        val definition: String
    )

    /**
     * Splits raw ECDICT meaning strings into distinct part-of-speech definition lines.
     * Removes literal escape sequences (\n), slash separators (/), and splits into separate lines.
     * Example input: "art. 这；那\nadv. 更加（用于比较级）"
     * Output: [ FormattedMeaning("art.", "这；那"), FormattedMeaning("adv.", "更加（用于比较级）") ]
     */
    fun parseMeanings(rawMeaning: String, defaultPos: String = ""): List<FormattedMeaning> {
        if (rawMeaning.isBlank()) return emptyList()

        val cleaned = rawMeaning
            .replace("\\n", "\n")
            .replace("\\r", "")

        val rawSegments = cleaned.split(Regex("[\\n/]"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val posRegex = Regex("(?:^|[;；\\s])(n\\.|v\\.|vt\\.|vi\\.|adj\\.|adv\\.|prep\\.|conj\\.|pron\\.|art\\.|num\\.|interj\\.|aux\\.|suf\\.|pref\\.|a\\.|s\\.|r\\.|v\\. t\\.|v\\. i\\.)")

        val result = mutableListOf<FormattedMeaning>()
        var lastPos = defaultPos

        for (segment in rawSegments) {
            val matches = posRegex.findAll(segment).toList()
            if (matches.isEmpty()) {
                val subParts = segment.split(Regex("[;；]")).map { it.trim().trim('/', '\\', ' ') }.filter { it.isNotBlank() }
                for (part in subParts) {
                    result.add(FormattedMeaning(pos = lastPos, definition = part))
                }
            } else {
                for (i in matches.indices) {
                    val match = matches[i]
                    val posTag = match.groupValues[1]
                    lastPos = posTag
                    val startIndex = match.range.first + match.value.indexOf(posTag) + posTag.length
                    val endIndex = if (i < matches.size - 1) matches[i + 1].range.first else segment.length
                    val defText = segment.substring(startIndex, endIndex).trim().trim(';', '；', '/', '\\', ' ')
                    if (defText.isNotBlank()) {
                        val subParts = defText.split(Regex("[;；]")).map { it.trim().trim('/', '\\', ' ') }.filter { it.isNotBlank() }
                        for (sub in subParts) {
                            result.add(FormattedMeaning(pos = posTag, definition = sub))
                        }
                    }
                }
            }
        }

        return if (result.isNotEmpty()) result else listOf(FormattedMeaning(pos = defaultPos, definition = cleaned.replace("/", " ").replace("\n", " ").trim()))
    }

    /**
     * Formats raw ECDICT exchange string into readable Chinese inflection tags.
     * Exchange tag mapping:
     * p: past tense (过去式)
     * d: past participle (过去分词)
     * 3: 3rd person singular (三单)
     * i: present participle (现在分词)
     * s: plural (复数)
     * r: comparative (比较级)
     * t: superlative (最高级)
     * 0: lemma / prototype (原形)
     * 1: transformation (变换)
     */
    fun formatExchange(word: String, rawExchange: String = ""): String {
        val cleanWord = word.trim()
        if (cleanWord.isBlank() || rawExchange.isBlank()) return ""

        val parts = rawExchange.split("/")
        val formatted = mutableListOf<String>()
        for (p in parts) {
            val kv = p.split(":")
            if (kv.size == 2) {
                val key = kv[0].trim()
                val valStr = kv[1].trim()
                if (valStr.isNotBlank()) {
                    val label = when (key) {
                        "p" -> "过去式"
                        "d" -> "过去分词"
                        "3" -> "三单"
                        "i" -> "现在分词"
                        "s" -> "复数"
                        "r" -> "比较级"
                        "t" -> "最高级"
                        "0" -> "原形"
                        "1" -> "变换"
                        else -> null
                    }
                    if (label != null) {
                        formatted.add("$label: $valStr")
                    }
                }
            }
        }
        return if (formatted.isNotEmpty()) formatted.joinToString(" / ") else ""
    }

    /**
     * Formats raw ECDICT etymology string into readable Chinese word root/affix formula.
     * Raw format: "词根/词缀 [form] | 含义: shape, form | 源自: Latin | 类型: root ;; 词根/词缀 [-ation] | 含义: ..."
     */
    fun formatEtymology(rawEtymology: String): String {
        val trimmed = rawEtymology.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("【词根】") || trimmed.startsWith("【词源】")) return trimmed

        val parts = trimmed.split(";;")
        val roots = mutableListOf<String>()
        val origins = mutableSetOf<String>()

        val rootRegex = Regex("\\[(.*?)\\]")
        val meaningRegex = Regex("含义:\\s*([^|]+)")
        val originRegex = Regex("源自:\\s*([^|]+)")

        for (p in parts) {
            val cleanPart = p.trim()
            if (cleanPart.isBlank()) continue

            val rootMatch = rootRegex.find(cleanPart)
            val meaningMatch = meaningRegex.find(cleanPart)
            val originMatch = originRegex.find(cleanPart)

            val rootStr = rootMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
            val meaningStr = meaningMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
            val originStr = originMatch?.groupValues?.getOrNull(1)?.trim() ?: ""

            if (originStr.isNotBlank()) {
                origins.add(originStr)
            }

            if (rootStr.isNotBlank()) {
                if (meaningStr.isNotBlank()) {
                    roots.add("$rootStr ($meaningStr)")
                } else {
                    roots.add(rootStr)
                }
            }
        }

        if (roots.isEmpty()) return trimmed

        val formula = roots.joinToString(" + ")
        val originTag = if (origins.isNotEmpty()) " (源自 ${origins.sorted().joinToString(", ")})" else ""
        return "【词根】$formula$originTag"
    }
}
