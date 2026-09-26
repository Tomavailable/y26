package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.dict.TtsVoiceInfo
import com.example.data.dict.VoiceAccent
import com.example.data.dict.VoiceConnectivityReport
import com.example.data.dict.VoiceProfile
import com.example.data.dict.VoiceSourceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

enum class SentenceVoiceType(val title: String, val flag: String, val badge: String) {
    TTS_1("TTS 1", "🤖", "TTS1"),
    TTS_2("TTS 2", "🤖", "TTS2"),
    TTS_3("TTS 3", "🤖", "TTS3")
}

/**
 * 内存 Hash 索引引擎，极速检索已缓存单词音频与本地文件夹音频 (O(1) 零磁盘 I/O 延迟)
 */
class AudioCacheIndex {
    private val dirFileSetMap = ConcurrentHashMap<String, MutableSet<String>>()
    private val isDirIndexed = ConcurrentHashMap<String, Boolean>()

    fun hasCachedWord(dir: File, word: String): Boolean? {
        val path = dir.absolutePath
        if (isDirIndexed[path] != true) return null
        val set = dirFileSetMap[path] ?: return false
        return set.contains(word.trim().lowercase())
    }

    fun buildOrRefreshDirIndex(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return
        val path = dir.absolutePath
        val files = dir.listFiles() ?: return
        val set = ConcurrentHashMap.newKeySet<String>()
        for (f in files) {
            if (f.isFile && f.length() > 200) {
                set.add(f.nameWithoutExtension.lowercase())
            }
        }
        dirFileSetMap[path] = set
        isDirIndexed[path] = true
    }

    fun addCachedWord(dir: File, word: String) {
        val path = dir.absolutePath
        val clean = word.trim().lowercase()
        val set = dirFileSetMap.getOrPut(path) { ConcurrentHashMap.newKeySet() }
        set.add(clean)
        isDirIndexed[path] = true
    }

    fun invalidate(dir: File? = null) {
        if (dir != null) {
            val path = dir.absolutePath
            dirFileSetMap.remove(path)
            isDirIndexed.remove(path)
        } else {
            dirFileSetMap.clear()
            isDirIndexed.clear()
        }
    }
}

class WordPronouncer(
    private val context: Context,
    val voiceManager: VoiceSourceManager = VoiceSourceManager(context)
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null
    private var activeCompletionCallback: (() -> Unit)? = null

    val cacheIndex = AudioCacheIndex()

    private var soundPool: SoundPool? = null
    private val soundPoolCacheMap = ConcurrentHashMap<String, Int>()
    private var currentStreamId: Int = 0
    private var soundPoolCompletionJob: Job? = null

    companion object {
        private const val TAG = "WordPronouncer"

        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15",
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.64 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/122.0.6261.62 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
            "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.178 Mobile Safari/537.36",
            "Mozilla/5.0 (iPad; CPU OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Mobile/15E148 Safari/604.1"
        )
    }

    init {
        initSoundPool()
        preloadAudioIndices()
    }

    private fun initSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "SoundPool init error: ${e.message}")
        }
    }

    fun preloadAudioIndices() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                voiceManager.voiceProfiles.value.forEach { profile ->
                    if (profile.sourceType != "LOCAL_TTS") {
                        val dir = voiceManager.getVoiceCacheDir(profile)
                        cacheIndex.buildOrRefreshDirIndex(dir)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Preload audio index error: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }
            tts?.setSpeechRate(0.92f)
            isTtsInitialized = true

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    CoroutineScope(Dispatchers.Main).launch {
                        activeCompletionCallback?.invoke()
                        activeCompletionCallback = null
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    CoroutineScope(Dispatchers.Main).launch {
                        activeCompletionCallback?.invoke()
                        activeCompletionCallback = null
                    }
                }
            })
        }
    }



    fun getBackupTtsProfile(profile: VoiceProfile): VoiceProfile? {
        val ttsProfiles = voiceManager.voiceProfiles.value.filter { it.sourceType == "LOCAL_TTS" }
        return when (profile.id) {
            "voice_longman_us" -> ttsProfiles.find { it.id == "voice_local_tts_1" } ?: ttsProfiles.getOrNull(0)
            "voice_youdao_us" -> ttsProfiles.find { it.id == "voice_local_tts_2" } ?: ttsProfiles.getOrNull(1) ?: ttsProfiles.getOrNull(0)
            "voice_youdao_uk" -> ttsProfiles.find { it.id == "voice_local_tts_3" } ?: ttsProfiles.getOrNull(2) ?: ttsProfiles.getOrNull(0)
            else -> ttsProfiles.find { it.id == "voice_local_tts_1" } ?: ttsProfiles.getOrNull(0)
        }
    }

    private fun speakWithTtsFallback(text: String, profile: VoiceProfile, onCompletion: (() -> Unit)? = null) {
        val backupTts = getBackupTtsProfile(profile)
        if (backupTts != null) {
            speakWithTtsProfile(text, backupTts, onCompletion = onCompletion)
        } else {
            speakWithTts(text, profile.accent, onCompletion = onCompletion)
        }
    }

    /**
     * 发音核心入口
     */
    fun speak(text: String, profileId: String? = null) {
        val cleanWord = text.trim().lowercase()
        if (cleanWord.isBlank()) return

        if (profileId == null && voiceManager.multiVoiceSequentialPlay.value) {
            val wordProfiles = voiceManager.getEnabledProfiles().filter { it.sourceType != "LOCAL_TTS" }
            if (wordProfiles.isNotEmpty()) {
                speakSequential(cleanWord, wordProfiles)
                return
            }
        }

        val profile: VoiceProfile = if (!profileId.isNullOrBlank()) {
            val exact = voiceManager.voiceProfiles.value.find { it.id == profileId }
            if (exact != null) {
                exact
            } else if (profileId == "uk" || profileId.contains("uk", ignoreCase = true)) {
                voiceManager.voiceProfiles.value.find { it.accent == VoiceAccent.UK && it.isEnabled }
                    ?: voiceManager.voiceProfiles.value.find { it.accent == VoiceAccent.UK }
                    ?: voiceManager.getDefaultProfile()
            } else if (profileId == "us" || profileId.contains("us", ignoreCase = true)) {
                voiceManager.voiceProfiles.value.find { it.accent == VoiceAccent.US && it.isEnabled }
                    ?: voiceManager.voiceProfiles.value.find { it.accent == VoiceAccent.US }
                    ?: voiceManager.getDefaultProfile()
            } else {
                voiceManager.getDefaultProfile()
            }
        } else {
            voiceManager.getDefaultProfile()
        }

        // Handle Local TTS Profile
        if (profile.sourceType == "LOCAL_TTS") {
            speakWithTtsProfile(text, profile)
            return
        }

        val targetDir = voiceManager.getVoiceCacheDir(profile)
        val targetFile = File(targetDir, "$cleanWord.mp3")

        // 3. 内存 Hash 索引极速检测 (O(1) 零磁盘 I/O 延迟)
        val hasInCache = cacheIndex.hasCachedWord(targetDir, cleanWord)
        if (hasInCache == true) {
            playLocalAudio(targetFile) {
                speakWithTtsFallback(text, profile)
            }
            return
        } else if (hasInCache == null) {
            if (targetFile.exists() && targetFile.length() > 500) {
                cacheIndex.addCachedWord(targetDir, cleanWord)
                playLocalAudio(targetFile) {
                    speakWithTtsFallback(text, profile)
                }
                return
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            var audioFile: File? = null
            val downloaded = downloadWordAudioForProfile(cleanWord, targetFile, profile)
            if (downloaded && targetFile.exists() && targetFile.length() > 500) {
                audioFile = targetFile
                cacheIndex.addCachedWord(targetDir, cleanWord)
            }

            withContext(Dispatchers.Main) {
                if (audioFile != null && audioFile.exists() && audioFile.length() > 500) {
                    playLocalAudio(audioFile) {
                        speakWithTtsFallback(text, profile)
                    }
                } else {
                    speakWithTtsFallback(text, profile)
                }
            }
        }
    }

    /**
     * 单词发音带完成回调
     */
    fun speakWordWithCallback(
        text: String,
        profileId: String? = null,
        onCompletion: () -> Unit
    ) {
        val cleanWord = text.trim().lowercase()
        if (cleanWord.isBlank()) {
            onCompletion()
            return
        }

        val profile = if (!profileId.isNullOrBlank()) {
            voiceManager.voiceProfiles.value.find { it.id == profileId } ?: voiceManager.getDefaultProfile()
        } else {
            voiceManager.getDefaultProfile()
        }

        if (profile.sourceType == "LOCAL_TTS") {
            speakWithTtsProfile(text, profile, onCompletion = onCompletion)
            return
        }

        val targetDir = voiceManager.getVoiceCacheDir(profile)
        val targetFile = File(targetDir, "$cleanWord.mp3")

        val hasInCache = cacheIndex.hasCachedWord(targetDir, cleanWord)
        if (hasInCache == true) {
            playLocalAudio(targetFile, onCompletion = onCompletion) {
                speakWithTtsFallback(text, profile, onCompletion = onCompletion)
            }
            return
        } else if (hasInCache == null) {
            if (targetFile.exists() && targetFile.length() > 500) {
                cacheIndex.addCachedWord(targetDir, cleanWord)
                playLocalAudio(targetFile, onCompletion = onCompletion) {
                    speakWithTtsFallback(text, profile, onCompletion = onCompletion)
                }
                return
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            var audioFile: File? = null
            val downloaded = downloadWordAudioForProfile(cleanWord, targetFile, profile)
            if (downloaded && targetFile.exists() && targetFile.length() > 500) {
                audioFile = targetFile
                cacheIndex.addCachedWord(targetDir, cleanWord)
            }

            withContext(Dispatchers.Main) {
                if (audioFile != null && audioFile.exists() && audioFile.length() > 500) {
                    playLocalAudio(audioFile, onCompletion = onCompletion) {
                        speakWithTtsFallback(text, profile, onCompletion = onCompletion)
                    }
                } else {
                    speakWithTtsFallback(text, profile, onCompletion = onCompletion)
                }
            }
        }
    }

    /**
     * 重复发音 N 次 (随身听 / 学习初听模式)
     */
    fun speakWordTimes(
        text: String,
        times: Int = 1,
        profileId: String? = null,
        onEachStart: ((Int) -> Unit)? = null,
        onCompletion: (() -> Unit)? = null
    ) {
        val total = times.coerceAtLeast(1)
        fun playLoop(current: Int) {
            if (current > total) {
                onCompletion?.invoke()
                return
            }
            onEachStart?.invoke(current)
            speakWordWithCallback(text, profileId) {
                CoroutineScope(Dispatchers.Main).launch {
                    if (current < total) {
                        delay(400)
                    }
                    playLoop(current + 1)
                }
            }
        }
        playLoop(1)
    }

    /**
     * 朗读中文释义 (TTS)
     */
    fun speakChinese(chineseText: String, onCompletion: (() -> Unit)? = null) {
        if (isTtsInitialized && chineseText.isNotBlank()) {
            tts?.setLanguage(Locale.CHINESE)
            tts?.setSpeechRate(1.0f)
            val params = android.os.Bundle()
            activeCompletionCallback = onCompletion
            tts?.speak(chineseText, TextToSpeech.QUEUE_FLUSH, params, "Chinese_${System.currentTimeMillis()}")
        } else {
            onCompletion?.invoke()
        }
    }

    /**
     * 连续轮播多个音源
     */
    fun speakSequential(
        word: String,
        profiles: List<VoiceProfile>,
        onCompletion: (() -> Unit)? = null
    ) {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.isBlank() || profiles.isEmpty()) {
            onCompletion?.invoke()
            return
        }

        fun playNext(index: Int) {
            if (index >= profiles.size) {
                onCompletion?.invoke()
                return
            }
            val profile = profiles[index]
            speakWordWithCallback(cleanWord, profile.id) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(220) // 音源之间平滑缓冲
                    playNext(index + 1)
                }
            }
        }

        playNext(0)
    }

    /**
     * 例句发音 (3 个本地 TTS 发音引擎)
     */
    fun speakSentence(
        sentence: String,
        type: SentenceVoiceType = SentenceVoiceType.TTS_1,
        onStart: (() -> Unit)? = null,
        onCompletion: (() -> Unit)? = null
    ) {
        val cleanSentence = sentence.trim()
        if (cleanSentence.isBlank()) {
            onCompletion?.invoke()
            return
        }

        onStart?.invoke()
        speakSentenceWithTts(cleanSentence, type, onCompletion)
    }

    private fun speakSentenceWithTts(sentence: String, type: SentenceVoiceType, onCompletion: (() -> Unit)? = null) {
        if (isTtsInitialized && sentence.isNotBlank()) {
            val ttsProfiles = voiceManager.voiceProfiles.value.filter { it.sourceType == "LOCAL_TTS" }
            val profile = when (type) {
                SentenceVoiceType.TTS_1 -> ttsProfiles.getOrNull(0)
                SentenceVoiceType.TTS_2 -> ttsProfiles.getOrNull(1)
                SentenceVoiceType.TTS_3 -> ttsProfiles.getOrNull(2)
            }

            if (profile != null) {
                val locale = if (profile.accent == VoiceAccent.UK) Locale.UK else Locale.US
                tts?.setLanguage(locale)
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(profile.speechRate)

                if (!profile.ttsVoiceName.isNullOrBlank()) {
                    val availableVoices = tts?.voices
                    val matched = availableVoices?.find { it.name == profile.ttsVoiceName }
                    if (matched != null) {
                        tts?.voice = matched
                    }
                }
            } else {
                when (type) {
                    SentenceVoiceType.TTS_1 -> {
                        tts?.setLanguage(Locale.US)
                        tts?.setSpeechRate(0.92f)
                        tts?.setPitch(1.0f)
                    }
                    SentenceVoiceType.TTS_2 -> {
                        tts?.setLanguage(Locale.UK)
                        tts?.setSpeechRate(0.95f)
                        tts?.setPitch(1.05f)
                    }
                    SentenceVoiceType.TTS_3 -> {
                        tts?.setLanguage(Locale.US)
                        tts?.setSpeechRate(0.80f)
                        tts?.setPitch(0.95f)
                    }
                }
            }

            val params = android.os.Bundle()
            if (profile != null) {
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, profile.pitch)
            }
            activeCompletionCallback = onCompletion
            tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, "Sentence_${System.currentTimeMillis()}")
        } else {
            onCompletion?.invoke()
            activeCompletionCallback = null
        }
    }

    fun speakSequence(text: String, onFinished: (() -> Unit)? = null) {
        val enabledProfiles = voiceManager.getEnabledProfiles()
        if (enabledProfiles.isEmpty()) {
            speak(text)
            return
        }
        speakSequential(text, enabledProfiles, onFinished)
    }

    private fun playLocalAudio(
        file: File,
        onCompletion: (() -> Unit)? = null,
        onError: () -> Unit
    ) {
        if (!file.exists() || file.length() < 200) {
            onError()
            return
        }

        stop()

        val path = file.absolutePath
        val pool = soundPool

        if (pool != null && file.length() < 1200 * 1024) {
            try {
                val cachedSoundId = soundPoolCacheMap[path]
                val durationMs = getAudioDurationMs(file)

                if (cachedSoundId != null && cachedSoundId > 0) {
                    val streamId = pool.play(cachedSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                    if (streamId != 0) {
                        currentStreamId = streamId
                        soundPoolCompletionJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(durationMs)
                            onCompletion?.invoke()
                        }
                        return
                    }
                }

                pool.setOnLoadCompleteListener { sp, sampleId, status ->
                    if (status == 0) {
                        soundPoolCacheMap[path] = sampleId
                        val streamId = sp.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f)
                        if (streamId != 0) {
                            currentStreamId = streamId
                            soundPoolCompletionJob = CoroutineScope(Dispatchers.Main).launch {
                                delay(durationMs)
                                onCompletion?.invoke()
                            }
                        } else {
                            playLocalAudioWithMediaPlayer(file, onCompletion, onError)
                        }
                    } else {
                        playLocalAudioWithMediaPlayer(file, onCompletion, onError)
                    }
                }

                val loadedId = pool.load(path, 1)
                if (loadedId == 0) {
                    playLocalAudioWithMediaPlayer(file, onCompletion, onError)
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "SoundPool error, fallback to MediaPlayer: ${e.message}")
            }
        }

        playLocalAudioWithMediaPlayer(file, onCompletion, onError)
    }

    private fun playLocalAudioWithMediaPlayer(
        file: File,
        onCompletion: (() -> Unit)? = null,
        onError: () -> Unit
    ) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    onCompletion?.invoke()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    onError()
                    true
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            onError()
        }
    }

    private fun getAudioDurationMs(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            timeStr?.toLongOrNull()?.coerceIn(300L, 8000L) ?: 800L
        } catch (_: Exception) {
            800L
        }
    }

    /**
     * 下载特定音源的发音文件，支持防封链路与多候选备选源轮换
     */
    fun downloadWordAudioForProfile(word: String, targetFile: File, profile: VoiceProfile): Boolean {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.isBlank()) return false

        val urlCandidates = getAudioUrlsForProfile(cleanWord, profile)
        for (candidate in urlCandidates) {
            if (fetchUrlToFileWithAntiBan(candidate.url, targetFile, candidate.referer)) {
                return true
            }
        }
        return false
    }

    private data class UrlCandidate(val url: String, val referer: String)

    private fun getAudioUrlsForProfile(word: String, profile: VoiceProfile): List<UrlCandidate> {
        val encodedWord = try {
            URLEncoder.encode(word, "UTF-8")
        } catch (_: Exception) {
            word
        }

        return when (profile.sourceType) {
            "YOUDAO_US" -> listOf(
                UrlCandidate("https://dict.youdao.com/dictvoice?type=2&audio=$encodedWord", "https://dict.youdao.com/"),
                UrlCandidate("https://dict.youdao.com/dictvoice?audio=$encodedWord&type=2&le=eng", "https://dict.youdao.com/"),
                UrlCandidate("https://audio.dict.cn/mp3.php?type=us&q=$encodedWord", "https://dict.cn/"),
                UrlCandidate("https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en-us&q=$encodedWord", "https://translate.google.com/")
            )
            "YOUDAO_UK" -> listOf(
                UrlCandidate("https://dict.youdao.com/dictvoice?type=1&audio=$encodedWord", "https://dict.youdao.com/"),
                UrlCandidate("https://dict.youdao.com/dictvoice?audio=$encodedWord&type=1&le=eng", "https://dict.youdao.com/"),
                UrlCandidate("https://audio.dict.cn/mp3.php?type=uk&q=$encodedWord", "https://dict.cn/"),
                UrlCandidate("https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en-gb&q=$encodedWord", "https://translate.google.com/")
            )
            "LONGMAN_US" -> listOf(
                UrlCandidate("https://www.ldoceonline.com/media/english/us_pron/$word.mp3", "https://www.ldoceonline.com/"),
                UrlCandidate("https://audio.dict.cn/mp3.php?type=us&q=$encodedWord", "https://dict.cn/"),
                UrlCandidate("https://dict.youdao.com/dictvoice?type=2&audio=$encodedWord", "https://dict.youdao.com/"),
                UrlCandidate("https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en-us&q=$encodedWord", "https://translate.google.com/")
            )
            "LONGMAN_UK" -> listOf(
                UrlCandidate("https://www.ldoceonline.com/media/english/uk_pron/$word.mp3", "https://www.ldoceonline.com/"),
                UrlCandidate("https://audio.dict.cn/mp3.php?type=uk&q=$encodedWord", "https://dict.cn/"),
                UrlCandidate("https://dict.youdao.com/dictvoice?type=1&audio=$encodedWord", "https://dict.youdao.com/"),
                UrlCandidate("https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en-gb&q=$encodedWord", "https://translate.google.com/")
            )
            "DICTCN_US" -> listOf(
                UrlCandidate("https://audio.dict.cn/mp3.php?type=us&q=$encodedWord", "https://dict.cn/"),
                UrlCandidate("https://dict.cn/mp3.php?type=us&q=$encodedWord", "https://dict.cn/"),
                UrlCandidate("https://dict.youdao.com/dictvoice?type=2&audio=$encodedWord", "https://dict.youdao.com/"),
                UrlCandidate("https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en-us&q=$encodedWord", "https://translate.google.com/")
            )
            "DICTCN_UK" -> listOf(
                UrlCandidate("https://audio.dict.cn/mp3.php?type=uk&q=$encodedWord", "https://dict.cn/"),
                UrlCandidate("https://dict.cn/mp3.php?type=uk&q=$encodedWord", "https://dict.cn/"),
                UrlCandidate("https://dict.youdao.com/dictvoice?type=1&audio=$encodedWord", "https://dict.youdao.com/"),
                UrlCandidate("https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en-gb&q=$encodedWord", "https://translate.google.com/")
            )
            else -> {
                if (profile.accent == VoiceAccent.UK) {
                    listOf(
                        UrlCandidate("https://dict.youdao.com/dictvoice?type=1&audio=$encodedWord", "https://dict.youdao.com/"),
                        UrlCandidate("https://audio.dict.cn/mp3.php?type=uk&q=$encodedWord", "https://dict.cn/")
                    )
                } else {
                    listOf(
                        UrlCandidate("https://dict.youdao.com/dictvoice?type=2&audio=$encodedWord", "https://dict.youdao.com/"),
                        UrlCandidate("https://audio.dict.cn/mp3.php?type=us&q=$encodedWord", "https://dict.cn/")
                    )
                }
            }
        }
    }

    /**
     * 防封防被限制的底层 HTTP 音频下载引擎
     */
    private fun fetchUrlToFileWithAntiBan(
        urlString: String,
        targetFile: File,
        referer: String? = null,
        retryCount: Int = 0
    ): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                instanceFollowRedirects = true
                val randomUa = USER_AGENTS[Random.nextInt(USER_AGENTS.size)]
                setRequestProperty("User-Agent", randomUa)
                if (!referer.isNullOrBlank()) {
                    setRequestProperty("Referer", referer)
                }
                setRequestProperty("Accept", "audio/mpeg, audio/mp4, audio/aac, audio/*;q=0.9, */*;q=0.8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8")
                setRequestProperty("Connection", "keep-alive")
            }

            connection.connect()
            val code = connection.responseCode

            if (code == 429 || code == 403 || code == 503) {
                Log.w(TAG, "Server rate limit / anti-scraping encountered ($code) on $urlString")
                if (retryCount < 2) {
                    val backoffMs = (retryCount + 1) * 1800L + Random.nextLong(200, 600)
                    Thread.sleep(backoffMs)
                    return fetchUrlToFileWithAntiBan(urlString, targetFile, referer, retryCount + 1)
                }
                return false
            }

            if (code in 200..299) {
                val contentType = connection.contentType ?: ""
                if (contentType.contains("text/html", ignoreCase = true)) {
                    return false
                }

                val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.exists() && tempFile.length() > 600) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    true
                } else {
                    tempFile.delete()
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio fetch failed for $urlString: ${e.message}")
            false
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 音源连通性与防封诊断检测方法
     */
    suspend fun testVoiceConnectivity(profile: VoiceProfile, testWord: String = "serendipity"): VoiceConnectivityReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        val urlCandidates = getAudioUrlsForProfile(testWord, profile)
            var lastCode = 0
            var matchedCandidate: UrlCandidate? = null
            var downloadedSize = 0L

            for (cand in urlCandidates) {
                try {
                    val conn = (URL(cand.url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4500
                        readTimeout = 4500
                        setRequestProperty("User-Agent", USER_AGENTS.first())
                        setRequestProperty("Referer", cand.referer)
                        setRequestProperty("Accept", "audio/mpeg, audio/*;q=0.9, */*;q=0.8")
                    }
                    conn.connect()
                    lastCode = conn.responseCode
                    if (lastCode in 200..299) {
                        val length = conn.contentLengthLong
                        downloadedSize = if (length > 0) length else 5000L
                        matchedCandidate = cand
                        conn.disconnect()
                        break
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    lastCode = -1
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            if (matchedCandidate != null) {
                VoiceConnectivityReport(
                    profileId = profile.id,
                    profileName = profile.name,
                    isSuccess = true,
                    statusCode = lastCode,
                    latencyMs = elapsed,
                    fileSizeBytes = downloadedSize,
                    message = "链路畅通 · 延迟 ${elapsed}ms · 防封标头安全"
                )
            } else {
                VoiceConnectivityReport(
                    profileId = profile.id,
                    profileName = profile.name,
                    isSuccess = false,
                    statusCode = lastCode,
                    latencyMs = elapsed,
                    fileSizeBytes = 0L,
                    message = if (lastCode == 429) "触发频率限制 (429)，已就绪智能防封避让" else "连接超时或网络异常 (状态码: $lastCode)"
                )
            }
        }

    private fun downloadSentenceAudioFile(
        sentence: String,
        targetFile: File,
        type: SentenceVoiceType
    ): Boolean {
        return try {
            val encodedSentence = URLEncoder.encode(sentence, "UTF-8")
            val voiceType = if (type == SentenceVoiceType.TTS_2) "1" else "2"
            val url = URL("https://dict.youdao.com/dictvoice?type=$voiceType&audio=$encodedSentence")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", USER_AGENTS[Random.nextInt(USER_AGENTS.size)])
            conn.setRequestProperty("Referer", "https://dict.youdao.com/")
            conn.connect()
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 500) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    true
                } else {
                    tempFile.delete()
                    false
                }
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun speakWithTts(text: String, accent: VoiceAccent, onCompletion: (() -> Unit)? = null) {
        if (isTtsInitialized && text.isNotBlank()) {
            val locale = if (accent == VoiceAccent.UK) Locale.UK else Locale.US
            tts?.setLanguage(locale)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "WordPronounceID")
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                onCompletion?.invoke()
            }
        } else {
            onCompletion?.invoke()
        }
    }

    private fun speakWithTtsProfile(text: String, profile: VoiceProfile, onCompletion: (() -> Unit)? = null) {
        if (isTtsInitialized && text.isNotBlank()) {
            try {
                val locale = if (profile.accent == VoiceAccent.UK) Locale.UK else Locale.US
                tts?.setLanguage(locale)
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(profile.speechRate)

                if (!profile.ttsVoiceName.isNullOrBlank()) {
                    val availableVoices = tts?.voices
                    val matched = availableVoices?.find { it.name == profile.ttsVoiceName }
                    if (matched != null) {
                        tts?.voice = matched
                    }
                }
            } catch (_: Exception) {
            }

            val params = android.os.Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, profile.pitch)
            val utteranceId = "WordPronounce_${System.currentTimeMillis()}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            CoroutineScope(Dispatchers.Main).launch {
                delay(1200)
                onCompletion?.invoke()
            }
        } else {
            onCompletion?.invoke()
        }
    }

    fun getAvailableTtsVoices(): List<TtsVoiceInfo> {
        if (!isTtsInitialized) return emptyList()
        return try {
            tts?.voices?.filter { voice ->
                voice.locale.language == "en"
            }?.map { voice ->
                val flag = when {
                    voice.locale == Locale.US || voice.locale.country == "US" -> "🇺🇸 美音"
                    voice.locale == Locale.UK || voice.locale.country == "GB" -> "🇬🇧 英音"
                    voice.locale.country == "AU" -> "🇦🇺 澳音"
                    voice.locale.country == "IN" -> "🇮🇳 印音"
                    else -> "🌐 ${voice.locale.displayLanguage} (${voice.locale.country})"
                }
                val netStr = if (voice.isNetworkConnectionRequired) " [在线]" else " [本地]"
                TtsVoiceInfo(
                    name = voice.name,
                    displayName = "$flag - ${voice.name}$netStr",
                    localeStr = voice.locale.toString(),
                    isNetworkRequired = voice.isNetworkConnectionRequired
                )
            }?.sortedBy { it.displayName } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun stop() {
        try {
            soundPoolCompletionJob?.cancel()
            soundPoolCompletionJob = null
            if (currentStreamId != 0) {
                soundPool?.stop(currentStreamId)
                currentStreamId = 0
            }
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {
        }
        tts?.stop()
        activeCompletionCallback = null
    }

    fun release() {
        stop()
        try {
            soundPool?.release()
            soundPool = null
            soundPoolCacheMap.clear()
        } catch (_: Exception) {
        }
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
    }
}
