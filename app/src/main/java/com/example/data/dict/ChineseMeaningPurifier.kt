package com.example.data.dict

import com.example.data.model.WordEntity

object ChineseMeaningPurifier {

    // Overrides for top multi-meaning "熟词生义" words, mapping them to Collins/Macmillan high-frequency priorities
    private val HIGH_PRIORITY_REORDER_MAP = mapOf(
        "address" to listOf("v. 处理，解决", "n. 地址", "v. 致辞，演说"),
        "state" to listOf("n. 状态，状况", "n. 国家", "v. 陈述，声明", "n. 州"),
        "plant" to listOf("n. 植物", "n. 工厂", "v. 种植，栽种"),
        "run" to listOf("v. 跑", "v. 经营，管理", "v. 运行", "n. 奔跑"),
        "table" to listOf("n. 桌子", "n. 表格，图表", "v. 搁置"),
        "record" to listOf("n. 记录，记载", "v. 记录，录音", "n. 唱片"),
        "interest" to listOf("n. 兴趣", "n. 利益", "n. 利息", "v. 使感兴趣"),
        "subject" to listOf("n. 主题", "n. 学科", "adj. 易受…影响的", "v. 使服从"),
        "present" to listOf("adj. 现在的", "v. 呈现，赠送", "n. 礼物", "n. 现在"),
        "fine" to listOf("adj. 好的，优秀的", "n. 罚款", "v. 处以罚款"),
        "draft" to listOf("n. 草稿，草案", "v. 起草", "n. 汇票"),
        "issue" to listOf("n. 问题，议题", "v. 发布，发行", "n. 期号")
    )

    /**
     * Purifies and simplifies the raw Chinese meaning of a word,
     * referencing Macmillan/Collins style: removing synonymous redundancy,
     * keeping only the top 1-2 most distinct/important translations per POS,
     * and formatting it cleanly.
     */
    fun purify(word: String, rawMeaning: String, pos: String = ""): String {
        val cleanWord = word.trim().lowercase()
        
        // If we have an expert curated high-frequency priority override, use it!
        val curated = HIGH_PRIORITY_REORDER_MAP[cleanWord]
        if (curated != null) {
            return curated.joinToString(" | ")
        }

        if (rawMeaning.isBlank()) return ""

        // 1. Parse raw meaning into parts of speech and definitions
        val parsed = EcdictFormatter.parseMeanings(rawMeaning, pos)
        if (parsed.isEmpty()) return rawMeaning

        // Group by POS tag (e.g. "n.", "v.", "adj.")
        val posGroups = parsed.groupBy { it.pos }
        val purifiedSegments = mutableListOf<String>()

        for ((posTag, items) in posGroups) {
            // Collect all translations for this POS
            val rawDefs = items.map { it.definition }
                .flatMap { it.split(Regex("[,，;；]")) }
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (rawDefs.isEmpty()) continue

            // Deduplicate and simplify synonyms
            val distinctDefs = mutableListOf<String>()
            for (def in rawDefs) {
                // Skip length <= 1 if we already have meaningful longer translations
                if (def.length <= 1 && distinctDefs.isNotEmpty() && distinctDefs.any { it.length > 1 }) {
                    continue
                }
                
                // If it's a synonym of an already added definition, or redundant, skip
                if (distinctDefs.any { isSynonymousOrRedundant(it, def) }) {
                    continue
                }
                distinctDefs.add(def)
            }

            // Keep at most top 2 distinct translations per part of speech
            val finalDefs = distinctDefs.take(2)

            if (finalDefs.isNotEmpty()) {
                val posPrefix = if (posTag.isNotBlank()) "$posTag " else ""
                purifiedSegments.add("$posPrefix${finalDefs.joinToString("，")}")
            }
        }

        return purifiedSegments.joinToString(" | ")
    }

    private fun isSynonymousOrRedundant(existing: String, newWord: String): Boolean {
        if (existing == newWord) return true
        
        // Single-char inside double-char redundancy (e.g. "国" and "国家" or "弃" and "放弃")
        if (existing.length == 1 && newWord.contains(existing)) return true
        if (newWord.length == 1 && existing.contains(newWord)) return true
        
        // Substring check
        if (existing.contains(newWord) || newWord.contains(existing)) {
            // But allow distinct meanings, e.g. "处理" and "写地址"
            if (existing.length > 2 && newWord.length > 2) {
                return false
            }
            return true
        }

        // Common synonym pairs in English-Chinese dictionaries
        val synonyms = listOf(
            setOf("放弃", "遗弃", "抛弃", "离弃", "舍弃"),
            setOf("充足", "充足的", "丰富的", "丰富", "大量", "大量的"),
            setOf("积累", "积聚", "堆积", "累积"),
            setOf("陈述", "声明", "阐明", "说明"),
            setOf("植物", "庄稼", "栽培", "种植", "栽种"),
            setOf("住所", "住址", "地址"),
            setOf("显眼", "明显", "显眼的", "明显的", "瞩目", "引人注目的"),
            setOf("主张", "提倡", "拥护", "倡导"),
            setOf("真实的", "正宗的", "地道的", "真实", "正宗")
        )

        for (synSet in synonyms) {
            if (existing in synSet && newWord in synSet) return true
        }

        return false
    }
}
