package com.example.data.dict

enum class VoiceAccent(val displayName: String, val flag: String) {
    US("美音", "🇺🇸"),
    UK("英音", "🇬🇧"),
    NATURAL("原声", "🎙️"),
    CUSTOM("自定义", "🔊")
}

data class VoiceConnectivityReport(
    val profileId: String,
    val profileName: String,
    val isSuccess: Boolean,
    val statusCode: Int,
    val latencyMs: Long,
    val fileSizeBytes: Long,
    val message: String
)

data class VoiceProfile(
    val id: String,
    val name: String,
    val accent: VoiceAccent,
    val description: String = "",
    val sourceType: String = "YOUDAO", // YOUDAO_US, YOUDAO_UK, LONGMAN_US, LOCAL_TTS, LOCAL_FOLDER
    val customFolderPath: String? = null,
    val ttsVoiceName: String? = null,
    val pitch: Float = 1.0f,
    val speechRate: Float = 0.92f,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val cacheDirName: String
)

data class TtsVoiceInfo(
    val name: String,
    val displayName: String,
    val localeStr: String,
    val isNetworkRequired: Boolean
)

enum class BuiltInDictionary(
    val id: String,
    val displayName: String,
    val shortName: String,
    val tag: String,
    val description: String
) {
    DEFAULT(
        id = "default",
        displayName = "默认 (朗文当代)",
        shortName = "朗文当代",
        tag = "朗文当代",
        description = "标准朗文当代高级词典全量释义与完整双语例句，包含学霸级词根词源与时态变形"
    ),
    RED_STAR(
        id = "red_star",
        displayName = "红星 (朗文当代·红星)",
        shortName = "红星词典",
        tag = "红星考点",
        description = "精选红星高频考点英文释义与最多 3 条红星考点双语例句，其余项目继承自默认词典"
    ),
    CAMBRIDGE_CORE(
        id = "cambridge_core",
        displayName = "剑桥核心 (剑桥核心词典)",
        shortName = "剑桥核心",
        tag = "剑桥核心",
        description = "精炼剑桥核心通俗英文释义与最多 3 条剑桥核心双语例句，其余项目继承自默认词典"
    );

    companion object {
        fun fromId(id: String): BuiltInDictionary {
            return values().find { it.id == id } ?: DEFAULT
        }
    }
}

