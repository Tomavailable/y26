package com.example.data.dict

import java.util.regex.Pattern

data class ExtractedDictItem(
    val word: String,
    val phonetic: String = "",
    val pos: String = "",
    val conciseMeaning: String = "",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val cleanedHtml: String = "",
    val notes: String = ""
)

object MdxExtractor {

    /**
     * Intelligently extracts essential learning fields from raw MDX HTML or text:
     * - Standard IPA phonetic symbols
     * - Part of speech (pos)
     * - Primary 1-2 concise meanings (filtering out hundreds of obscure derivatives/grammar tables)
     * - Selected representative bilingual example sentence
     */
    fun extract(rawWord: String, rawMdxContent: String): ExtractedDictItem {
        val word = rawWord.trim()
        if (rawMdxContent.isBlank()) {
            return ExtractedDictItem(word = word)
        }

        // 1. Remove dangerous or noisy script/style blocks
        var content = rawMdxContent
            .replace(Regex("(?s)<script.*?>.*?</script>"), "")
            .replace(Regex("(?s)<style.*?>.*?</style>"), "")
            .replace(Regex("<!--.*?-->"), "")

        // 2. Extract IPA phonetic
        val phonetic = extractPhonetic(content)

        // 3. Extract POS (Part of Speech)
        val pos = extractPos(content)

        // 4. Extract concise core meaning (1-2 main definitions)
        val meaning = extractMeaning(content)

        // 5. Extract best example sentence and translation
        val (exampleSentence, exampleTranslation) = extractExample(content)

        // 6. Generate simplified clean HTML for optional full dictionary view
        val cleanedHtml = sanitizeForDisplay(content)

        return ExtractedDictItem(
            word = word,
            phonetic = phonetic,
            pos = pos,
            conciseMeaning = meaning,
            exampleSentence = exampleSentence,
            exampleTranslation = exampleTranslation,
            cleanedHtml = cleanedHtml,
            notes = "【MDX专业词典】已智能提取核心释义与真题语境"
        )
    }

    private fun extractPhonetic(content: String): String {
        // Look for common phonetic tags: class="phon", class="ipa", class="pron", <phonetic>
        val tagRegexes = listOf(
            Regex("""<[^>]*class=["'][^"']*(?:phon|ipa|pron)[^"']*["'][^>]*>(.*?)</[^>]+>""", RegexOption.IGNORE_CASE),
            Regex("""<phonetic[^>]*>(.*?)</phonetic>""", RegexOption.IGNORE_CASE)
        )
        for (regex in tagRegexes) {
            val match = regex.find(content)
            if (match != null) {
                val clean = stripTags(match.groupValues[1]).trim()
                if (clean.isNotEmpty()) {
                    return if (clean.startsWith("/") || clean.startsWith("[")) clean else "/$clean/"
                }
            }
        }

        // Fallback: search for pattern /.../ or [...]
        val pattern = Pattern.compile("""(/[a-zA-Zˈˌːθðʃʒŋæʌɒɛɪʊuːəeɪaɪɔɪəʊaʊɪəeəʊə\s\-']{2,25}/)""")
        val matcher = pattern.matcher(content)
        if (matcher.find()) {
            return matcher.group(1) ?: ""
        }

        return ""
    }

    private fun extractPos(content: String): String {
        val posTagRegex = Regex("""<[^>]*class=["'][^"']*(?:pos|p-o-s|part-of-speech)[^"']*["'][^>]*>(.*?)</[^>]+>""", RegexOption.IGNORE_CASE)
        val match = posTagRegex.find(content)
        if (match != null) {
            val rawPos = stripTags(match.groupValues[1]).trim().lowercase()
            return normalizePos(rawPos)
        }

        // Fallback text pos markers
        val textPosRegex = Regex("""\b(vt\.|vi\.|v\.|n\.|adj\.|adv\.|prep\.|conj\.|pron\.|num\.|art\.)\b""", RegexOption.IGNORE_CASE)
        val textMatch = textPosRegex.find(content)
        if (textMatch != null) {
            return textMatch.groupValues[1].lowercase()
        }

        return ""
    }

    private fun normalizePos(raw: String): String {
        return when {
            raw.contains("noun") || raw == "n" || raw == "n." -> "n."
            raw.contains("verb transitive") || raw == "vt" || raw == "vt." -> "vt."
            raw.contains("verb intransitive") || raw == "vi" || raw == "vi." -> "vi."
            raw.contains("verb") || raw == "v" || raw == "v." -> "v."
            raw.contains("adjective") || raw == "adj" || raw == "adj." -> "adj."
            raw.contains("adverb") || raw == "adv" || raw == "adv." -> "adv."
            raw.contains("preposition") || raw == "prep" || raw == "prep." -> "prep."
            raw.contains("conjunction") || raw == "conj" || raw == "conj." -> "conj."
            else -> raw
        }
    }

    private fun extractMeaning(content: String): String {
        // Look for .def, .sense, .dchn, .trans, .chn tags
        val defRegex = Regex("""<[^>]*class=["'][^"']*(?:def|sense|dchn|trans|chn|meaning|definition)[^"']*["'][^>]*>(.*?)</[^>]+>""", RegexOption.IGNORE_CASE)
        val matches = defRegex.findAll(content).toList()
        if (matches.isNotEmpty()) {
            val definitions = matches.take(2).map { stripTags(it.groupValues[1]).trim() }.filter { it.isNotBlank() }
            if (definitions.isNotEmpty()) {
                return definitions.joinToString("； ")
            }
        }

        // Fallback: Chinese character sequences extraction
        val cnRegex = Regex("""[\u4e00-\u9fa5][\u4e00-\u9fa5，、；（）()：a-zA-Z0-9\s]{2,80}""")
        val cnMatches = cnRegex.findAll(stripTags(content)).toList()
        if (cnMatches.isNotEmpty()) {
            return cnMatches.take(2).map { it.value.trim() }.joinToString("； ")
        }

        return stripTags(content).take(80)
    }

    private fun extractExample(content: String): Pair<String, String> {
        // Matches typical MDX example sentence containers: class="x", class="exam", class="example"
        val exRegex = Regex("""<[^>]*class=["'][^"']*(?:exam|example|sent_eng|x)[^"']*["'][^>]*>(.*?)</[^>]+>""", RegexOption.IGNORE_CASE)
        val matchEn = exRegex.find(content)

        val transRegex = Regex("""<[^>]*class=["'][^"']*(?:trans|sent_chn|meaning|dchn)[^"']*["'][^>]*>(.*?)</[^>]+>""", RegexOption.IGNORE_CASE)
        val matchCn = transRegex.find(content)

        val englishSentence = matchEn?.let { stripTags(it.groupValues[1]).trim() } ?: ""
        val chineseTrans = matchCn?.let { stripTags(it.groupValues[1]).trim() } ?: ""

        if (englishSentence.isNotEmpty()) {
            return Pair(englishSentence, chineseTrans)
        }

        // Fallback search for English sentence with trailing Chinese translation
        val sentenceRegex = Regex("""([A-Z][^.!?\n]{8,90}[.!?])\s*([\u4e00-\u9fa5][^\n]{3,60})""")
        val sMatch = sentenceRegex.find(stripTags(content))
        if (sMatch != null) {
            return Pair(sMatch.groupValues[1].trim(), sMatch.groupValues[2].trim())
        }

        return Pair("", "")
    }

    private fun stripTags(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun sanitizeForDisplay(html: String): String {
        return html
            .replace(Regex("(?s)<script.*?>.*?</script>"), "")
            .replace(Regex("(?s)<style.*?>.*?</style>"), "")
            .replace(Regex("""href=["']sound://.*?["']"""), "href=\"#\"")
            .trim()
    }
}
