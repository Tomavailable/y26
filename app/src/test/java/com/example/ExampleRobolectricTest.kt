package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.WordImporter
import com.example.data.model.ReviewQuality
import com.example.data.model.SpacedRepetitionHelper
import com.example.data.model.WordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("要背单词", appName)
    }

    @Test
    fun `test spaced repetition calculation`() {
        val word = WordEntity(
            bookId = "test",
            word = "abandon",
            meaning = "放弃",
            reviewStage = 0
        )
        val now = 1000000L

        // Review with KNOW -> advances to stage 1 (8 hours = 8 * 60 * 60 * 1000ms)
        val stage1 = SpacedRepetitionHelper.calculateNextReview(word, ReviewQuality.KNOW, now)
        assertEquals(1, stage1.reviewStage)
        assertEquals(now + 8 * 60 * 60 * 1000L, stage1.nextReviewTime)

        // Review stage 1 with KNOW -> advances to stage 2 (2 days)
        val stage2 = SpacedRepetitionHelper.calculateNextReview(stage1, ReviewQuality.KNOW, stage1.nextReviewTime)
        assertEquals(2, stage2.reviewStage)

        // Review with FORGOT -> drops back to stage 1
        val forgot = SpacedRepetitionHelper.calculateNextReview(stage2, ReviewQuality.FORGOT, stage2.nextReviewTime)
        assertEquals(1, forgot.reviewStage)
    }

    @Test
    fun `test word importer csv and json`() {
        val csv = WordImporter.SAMPLE_CSV
        val wordsFromCsv = WordImporter.parse(csv, "custom_test")
        assertTrue(wordsFromCsv.size >= 5)
        assertEquals("serendipity", wordsFromCsv[0].word)

        val json = WordImporter.SAMPLE_JSON
        val wordsFromJson = WordImporter.parse(json, "custom_test")
        assertTrue(wordsFromJson.size >= 2)
        assertEquals("catalyst", wordsFromJson[0].word)
    }

    @Test
    fun `test raw words auto enrichment from local dictionary`() {
        val rawWords = WordImporter.SAMPLE_RAW_WORDS
        val parsed = WordImporter.parse(rawWords, "raw_test")
        assertTrue(parsed.size >= 5)

        // Find "epiphany"
        val epiphany = parsed.find { it.word.equals("epiphany", ignoreCase = true) }
        assertNotNull(epiphany)
        assertEquals("/ɪˈpɪfəni/", epiphany?.phonetic)
        assertTrue(epiphany?.meaning?.contains("顿悟") == true)
        assertTrue(epiphany?.exampleSentence?.isNotBlank() == true)
    }

    @Test
    fun `test mdx intelligent extractor`() {
        val sampleMdxHtml = """
            <div class="entry">
              <span class="phon">/ɪˈpɪfəni/</span>
              <span class="pos">noun</span>
              <div class="sense">
                <span class="def">a sudden realization 顿悟；灵感</span>
                <div class="exam">
                  <span class="x">He had a sudden epiphany.</span>
                  <span class="trans">他脑海中突然浮现出一阵顿悟。</span>
                </div>
              </div>
            </div>
        """.trimIndent()

        val item = com.example.data.dict.MdxExtractor.extract("epiphany", sampleMdxHtml)
        assertEquals("epiphany", item.word)
        assertEquals("/ɪˈpɪfəni/", item.phonetic)
        assertEquals("n.", item.pos)
        assertTrue(item.conciseMeaning.contains("顿悟"))
        assertEquals("He had a sudden epiphany.", item.exampleSentence)
        assertEquals("他脑海中突然浮现出一阵顿悟。", item.exampleTranslation)
    }

    @Test
    fun `test voice source manager isolation and toggles`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.data.dict.VoiceSourceManager(context)

        val profiles = manager.voiceProfiles.value
        assertTrue(profiles.size >= 2)

        val usVoice = profiles.find { it.accent == com.example.data.dict.VoiceAccent.US }
        assertNotNull(usVoice)
        assertEquals("audio_youdao_ame", usVoice?.cacheDirName)

        val dir = manager.getVoiceCacheDir(usVoice!!)
        assertTrue(dir.path.contains("audio_sources/audio_youdao_ame"))

        // Toggle state test
        manager.toggleVoice(usVoice.id, false)
        val updated = manager.voiceProfiles.value.find { it.id == usVoice.id }
        assertEquals(false, updated?.isEnabled)
    }

    @Test
    fun `test sentence voice types and isolated folders`() {
        val types = com.example.audio.SentenceVoiceType.values()
        assertEquals(3, types.size)
        assertTrue(types.any { it == com.example.audio.SentenceVoiceType.TTS_1 })
        assertTrue(types.any { it == com.example.audio.SentenceVoiceType.TTS_2 })
        assertTrue(types.any { it == com.example.audio.SentenceVoiceType.TTS_3 })

        val context = ApplicationProvider.getApplicationContext<Context>()
        val pronouncer = com.example.audio.WordPronouncer(context)
        assertNotNull(pronouncer)
    }

    @Test
    fun `test coca tsv dictionary loading`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.data.dict.CocaFrequencyDictionary.init(context)
        val rankThe = com.example.data.dict.CocaFrequencyDictionary.getRank("the")
        assertEquals(1, rankThe)
        val entry = com.example.data.dict.CocaFrequencyDictionary.lookup("the")
        assertNotNull(entry)
        assertTrue(entry!!.meaning.isNotBlank())

        // Check high rank words in 10001-15000 range
        val stareEntry = com.example.data.dict.CocaFrequencyDictionary.lookup("stare")
        assertNotNull(stareEntry)
        assertTrue(stareEntry!!.meaning.isNotBlank())
        assertTrue(stareEntry.exampleSentence.isNotBlank())

        val words15k = com.example.data.dict.CocaFrequencyDictionary.loadWordsForBook(context, "coca_10000_15000")
        assertEquals(5000, words15k.size)
    }
}
