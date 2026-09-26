package com.example.data.dict

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class VoiceBatchDownloadState(
    val profileId: String = "",
    val profileName: String = "",
    val isDownloading: Boolean = false,
    val currentWord: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val successCount: Int = 0,
    val failCount: Int = 0,
    val rateLimitTriggered: Boolean = false,
    val statusMessage: String = ""
) {
    val progressPercent: Float
        get() = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
}

data class VoiceCacheStats(
    val fileCount: Int = 0,
    val totalSizeBytes: Long = 0L
) {
    val formattedSize: String
        get() {
            return when {
                totalSizeBytes < 1024 -> "${totalSizeBytes} B"
                totalSizeBytes < 1024 * 1024 -> String.format("%.1f KB", totalSizeBytes / 1024.0)
                else -> String.format("%.1f MB", totalSizeBytes / (1024.0 * 1024.0))
            }
        }
}

class VoiceSourceManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("voice_source_prefs", Context.MODE_PRIVATE)
    private val baseAudioSourcesDir: File = (context.getExternalFilesDir("audio_sources") ?: File(context.filesDir, "audio_sources")).apply { mkdirs() }
    private val legacyAudioSourcesDir: File = File(context.filesDir, "audio_sources")

    private val _voiceProfiles = MutableStateFlow<List<VoiceProfile>>(emptyList())
    val voiceProfiles: StateFlow<List<VoiceProfile>> = _voiceProfiles.asStateFlow()

    // 多音源轮流播放模式 (点击单词发音时，连续播放所有已勾选启用的音源)
    private val _multiVoiceSequentialPlay = MutableStateFlow(
        prefs.getBoolean(KEY_MULTI_VOICE_SEQUENTIAL, false)
    )
    val multiVoiceSequentialPlay: StateFlow<Boolean> = _multiVoiceSequentialPlay.asStateFlow()

    // 例句轮播播放模式 (点击例句发音时，在启用 TTS 方案中循环)
    private val _sentenceVoiceRotationEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SENTENCE_VOICE_ROTATION, false)
    )
    val sentenceVoiceRotationEnabled: StateFlow<Boolean> = _sentenceVoiceRotationEnabled.asStateFlow()

    // 批量下载状态
    private val _downloadState = MutableStateFlow(VoiceBatchDownloadState())
    val downloadState: StateFlow<VoiceBatchDownloadState> = _downloadState.asStateFlow()

    init {
        loadProfiles()
    }

    private fun getDefaultSeedProfiles(): List<VoiceProfile> {
        return listOf(
            VoiceProfile(
                id = "voice_longman_us",
                name = "朗文美音",
                accent = VoiceAccent.US,
                description = "朗文当代美音，地道正统，响应迅速，默认单词发音",
                sourceType = "LONGMAN_US",
                customFolderPath = null,
                isEnabled = true,
                isDefault = true,
                cacheDirName = "audio_longman_us"
            ),
            VoiceProfile(
                id = "voice_youdao_us",
                name = "有道美音",
                accent = VoiceAccent.US,
                description = "标准美音，发音清晰，响应极快，内置防封链路",
                sourceType = "YOUDAO_US",
                customFolderPath = null,
                isEnabled = true,
                isDefault = false,
                cacheDirName = "audio_youdao_ame"
            ),
            VoiceProfile(
                id = "voice_youdao_uk",
                name = "有道英音",
                accent = VoiceAccent.UK,
                description = "标准英音，地道正统，响应极快，内置防封链路",
                sourceType = "YOUDAO_UK",
                customFolderPath = null,
                isEnabled = true,
                isDefault = false,
                cacheDirName = "audio_youdao_eng"
            ),
            VoiceProfile(
                id = "voice_local_tts_1",
                name = "1美音 tts",
                accent = VoiceAccent.US,
                description = "系统美音 TTS 引擎，朗文美音发音降级替补",
                sourceType = "LOCAL_TTS",
                customFolderPath = null,
                ttsVoiceName = null,
                pitch = 1.0f,
                speechRate = 0.92f,
                isEnabled = true,
                isDefault = false,
                cacheDirName = "audio_tts_1"
            ),
            VoiceProfile(
                id = "voice_local_tts_2",
                name = "2英音 tts",
                accent = VoiceAccent.UK,
                description = "系统英音 TTS 引擎，有道美音发音降级替补",
                sourceType = "LOCAL_TTS",
                customFolderPath = null,
                ttsVoiceName = null,
                pitch = 1.0f,
                speechRate = 0.95f,
                isEnabled = true,
                isDefault = false,
                cacheDirName = "audio_tts_2"
            ),
            VoiceProfile(
                id = "voice_local_tts_3",
                name = "3澳音 tts",
                accent = VoiceAccent.NATURAL,
                description = "系统澳音/缓读 TTS 引擎，有道英音发音降级替补",
                sourceType = "LOCAL_TTS",
                customFolderPath = null,
                ttsVoiceName = null,
                pitch = 0.95f,
                speechRate = 0.85f,
                isEnabled = true,
                isDefault = false,
                cacheDirName = "audio_tts_3"
            )
        )
    }

    private fun loadProfiles() {
        val jsonString = prefs.getString(KEY_PROFILES, null)
        val defaultSeeds = getDefaultSeedProfiles()
        val obsoleteIds = setOf(
            "voice_google_neural", "voice_shanbay_us", "voice_shanbay_uk", "voice_cambridge_us", "voice_oxford_uk",
            "voice_longman_uk", "voice_dictcn_us", "voice_dictcn_uk", "voice_local_folder"
        )
        val obsoleteTypes = setOf(
            "GOOGLE_NEURAL", "SHANBAY_US", "SHANBAY_UK", "CAMBRIDGE_US", "OXFORD_UK",
            "LONGMAN_UK", "DICTCN_US", "DICTCN_UK", "LOCAL_FOLDER"
        )

        if (jsonString.isNullOrBlank()) {
            _voiceProfiles.value = defaultSeeds
            saveProfiles(defaultSeeds)
        } else {
            val list = mutableListOf<VoiceProfile>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val sourceType = obj.optString("sourceType", "")
                    if (id in obsoleteIds || sourceType in obsoleteTypes) continue

                    val accentStr = obj.optString("accent", "US")
                    val accent = try {
                        VoiceAccent.valueOf(accentStr)
                    } catch (_: Exception) {
                        VoiceAccent.US
                    }
                    val seed = defaultSeeds.find { it.id == id }
                    list.add(
                        VoiceProfile(
                            id = id,
                            name = obj.optString("name", seed?.name ?: id),
                            accent = accent,
                            description = obj.optString("description", seed?.description ?: ""),
                            sourceType = sourceType.ifBlank { seed?.sourceType ?: "YOUDAO_US" },
                            customFolderPath = obj.optString("customFolderPath", null).takeIf { !it.isNullOrBlank() },
                            ttsVoiceName = obj.optString("ttsVoiceName", null).takeIf { !it.isNullOrBlank() } ?: seed?.ttsVoiceName,
                            pitch = obj.optDouble("pitch", seed?.pitch?.toDouble() ?: 1.0).toFloat(),
                            speechRate = obj.optDouble("speechRate", seed?.speechRate?.toDouble() ?: 0.92).toFloat(),
                            isEnabled = obj.optBoolean("isEnabled", seed?.isEnabled ?: true),
                            isDefault = obj.optBoolean("isDefault", seed?.isDefault ?: false),
                            cacheDirName = obj.optString("cacheDirName", seed?.cacheDirName ?: "audio_$id")
                        )
                    )
                }
            } catch (_: Exception) {
            }

            val existingIds = list.map { it.id }.toSet()
            val missingDefaults = defaultSeeds.filter { it.id !in existingIds }
            val mergedList = (list + missingDefaults).distinctBy { it.id }

            if (mergedList.isEmpty()) {
                _voiceProfiles.value = defaultSeeds
                saveProfiles(defaultSeeds)
            } else {
                if (mergedList.none { it.isDefault }) {
                    val corrected = mergedList.map { vp ->
                        if (vp.id == "voice_longman_us") vp.copy(isDefault = true, isEnabled = true) else vp
                    }
                    _voiceProfiles.value = corrected
                    saveProfiles(corrected)
                } else {
                    _voiceProfiles.value = mergedList
                    saveProfiles(mergedList)
                }
            }
        }
    }

    fun saveProfiles(list: List<VoiceProfile>) {
        _voiceProfiles.value = list
        val array = JSONArray()
        for (p in list) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("accent", p.accent.name)
                put("description", p.description)
                put("sourceType", p.sourceType)
                put("customFolderPath", p.customFolderPath ?: "")
                put("ttsVoiceName", p.ttsVoiceName ?: "")
                put("pitch", p.pitch.toDouble())
                put("speechRate", p.speechRate.toDouble())
                put("isEnabled", p.isEnabled)
                put("isDefault", p.isDefault)
                put("cacheDirName", p.cacheDirName)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    fun setMultiVoiceSequentialPlay(enabled: Boolean) {
        _multiVoiceSequentialPlay.value = enabled
        prefs.edit().putBoolean(KEY_MULTI_VOICE_SEQUENTIAL, enabled).apply()
    }

    fun setSentenceVoiceRotationEnabled(enabled: Boolean) {
        _sentenceVoiceRotationEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SENTENCE_VOICE_ROTATION, enabled).apply()
    }

    fun toggleVoice(profileId: String, isEnabled: Boolean): Boolean {
        val target = _voiceProfiles.value.find { it.id == profileId }
        if (target != null && target.isDefault && !isEnabled) {
            return false // Default profile CANNOT be disabled!
        }
        val updated = _voiceProfiles.value.map {
            if (it.id == profileId) it.copy(isEnabled = isEnabled) else it
        }
        saveProfiles(updated)
        return true
    }

    fun setDefaultVoice(profileId: String) {
        val updated = _voiceProfiles.value.map {
            it.copy(
                isDefault = (it.id == profileId),
                isEnabled = if (it.id == profileId) true else it.isEnabled
            )
        }
        saveProfiles(updated)
    }

    fun addLocalTtsVoiceProfile(
        name: String,
        accent: VoiceAccent,
        ttsVoiceName: String?,
        pitch: Float,
        speechRate: Float
    ): VoiceProfile {
        val timestamp = System.currentTimeMillis()
        val id = "voice_tts_$timestamp"
        val cacheDirName = "audio_tts_${timestamp % 10000}"
        val newProfile = VoiceProfile(
            id = id,
            name = name,
            accent = accent,
            description = "自定义本地 TTS 引擎音源",
            sourceType = "LOCAL_TTS",
            customFolderPath = null,
            ttsVoiceName = ttsVoiceName,
            pitch = pitch,
            speechRate = speechRate,
            isEnabled = true,
            isDefault = false,
            cacheDirName = cacheDirName
        )
        val updated = _voiceProfiles.value + newProfile
        saveProfiles(updated)
        return newProfile
    }

    fun updateTtsVoiceProfile(
        profileId: String,
        name: String,
        accent: VoiceAccent,
        ttsVoiceName: String?,
        pitch: Float,
        speechRate: Float
    ) {
        val updated = _voiceProfiles.value.map {
            if (it.id == profileId) {
                it.copy(
                    name = name,
                    accent = accent,
                    ttsVoiceName = ttsVoiceName,
                    pitch = pitch,
                    speechRate = speechRate,
                    isEnabled = true
                )
            } else it
        }
        saveProfiles(updated)
    }

    fun removeVoice(profileId: String) {
        val profile = _voiceProfiles.value.find { it.id == profileId }
        if (profile != null) {
            val updated = _voiceProfiles.value.filter { it.id != profileId }
            saveProfiles(updated)
        }
    }

    fun getVoiceCacheDir(profile: VoiceProfile): File {
        return File(baseAudioSourcesDir, profile.cacheDirName).apply { mkdirs() }
    }

    fun getVoiceCacheStats(profile: VoiceProfile): VoiceCacheStats {
        val dir = getVoiceCacheDir(profile)
        if (!dir.exists() || !dir.isDirectory) return VoiceCacheStats(0, 0L)
        val files = dir.listFiles() ?: return VoiceCacheStats(0, 0L)
        var count = 0
        var totalBytes = 0L
        for (f in files) {
            if (f.isFile && f.length() > 0) {
                count++
                totalBytes += f.length()
            }
        }
        return VoiceCacheStats(count, totalBytes)
    }

    fun getVoiceCacheSizeBytes(profile: VoiceProfile): Long {
        return getVoiceCacheStats(profile).totalSizeBytes
    }

    fun clearVoiceCache(profile: VoiceProfile) {
        val dir = getVoiceCacheDir(profile)
        try {
            if (dir.exists()) {
                dir.deleteRecursively()
                dir.mkdirs()
            }
        } catch (_: Exception) {
        }
    }

    fun clearAllCaches() {
        for (p in _voiceProfiles.value) {
            clearVoiceCache(p)
        }
    }

    fun updateDownloadProgress(
        profileId: String,
        profileName: String,
        isDownloading: Boolean,
        currentWord: String,
        completed: Int,
        total: Int,
        success: Int,
        fail: Int,
        rateLimitTriggered: Boolean = false,
        statusMessage: String = ""
    ) {
        _downloadState.value = VoiceBatchDownloadState(
            profileId = profileId,
            profileName = profileName,
            isDownloading = isDownloading,
            currentWord = currentWord,
            completedCount = completed,
            totalCount = total,
            successCount = success,
            failCount = fail,
            rateLimitTriggered = rateLimitTriggered,
            statusMessage = statusMessage
        )
    }

    fun resetDownloadProgress() {
        _downloadState.value = VoiceBatchDownloadState()
    }

    fun getEnabledProfiles(): List<VoiceProfile> {
        val list = _voiceProfiles.value.filter { it.isEnabled }
        return if (list.isNotEmpty()) list else listOf(getDefaultProfile())
    }

    fun getDefaultProfile(): VoiceProfile {
        return _voiceProfiles.value.firstOrNull { it.isDefault && it.isEnabled }
            ?: _voiceProfiles.value.firstOrNull { it.isDefault }
            ?: _voiceProfiles.value.firstOrNull { it.isEnabled }
            ?: _voiceProfiles.value.first()
    }

    companion object {
        private const val KEY_PROFILES = "profiles_json"
        private const val KEY_MULTI_VOICE_SEQUENTIAL = "multi_voice_sequential"
        private const val KEY_SENTENCE_VOICE_ROTATION = "sentence_voice_rotation"
        private const val KEY_GLOBAL_CUSTOM_AUDIO_FOLDER = "global_custom_audio_folder"
        private const val KEY_PACING_MODE = "download_pacing_mode"
    }
}
