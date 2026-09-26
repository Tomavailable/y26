package com.example.data.model

/**
 * 当前会话类型：学习新词还是艾宾浩斯复习
 */
enum class SessionType {
    LEARN,
    REVIEW
}

/**
 * Available learning modes for the user to choose freely
 */
enum class LearningMode(val title: String, val badge: String, val description: String) {
    SCIENTIFIC_COMPREHENSIVE(
        title = "科学综合模式",
        badge = "推荐",
        description = "首次盲听发音，二检听音选义，多错题随机抽取听音选词/听写/看义说词/例句完形"
    ),
    LISTENING_IMMERSION(
        title = "纯听力强化模式",
        badge = "听力",
        description = "全程侧重听觉输入，二检以纯听音选义与听写为主"
    ),
    SPEED_RECOGNITION(
        title = "快速选义模式",
        badge = "极速",
        description = "适合快速刷词冲刺，以中英释义快速匹配为主"
    )
}

/**
 * 学习强度选项
 */
enum class LearningIntensity(
    val title: String,
    val subtitle: String,
    val description: String,
    val targetCorrectCount: Int
) {
    EASY("轻松", "当前状态", "抽选 2 个核心题型，快速轮转刷词", 2),
    MEDIUM("中等", "答对4次过关", "需累积答对 4 次核心/强化题型方可过关", 4),
    STRONG("强者", "全部训练", "完整打卡已开启的全部题型，形成硬核肌肉记忆", 7)
}

/**
 * Enabled modes in the learning cycle (switches configurable in settings)
 * 罗列所有6种学习方式，每个方式提供开关按钮，最少保留一个方式
 */
data class LearningModeSwitches(
    val enableBlindListenMeaning: Boolean = true,       // 1. 盲听选义 (原听音选义)
    val enableLookChooseMeaning: Boolean = true,        // 2. 看词选义
    val enableRecallByMeaning: Boolean = true,          // 3. 看义说词
    val enableListenChooseWord: Boolean = true,         // 4. 听音选词
    val enableListenSentenceChooseWord: Boolean = true, // 5. 听句选词
    val enableEnglishMeaningChooseWord: Boolean = true, // 6. 英文释义选词
    val enableBlindSpelling: Boolean = true             // 7. 盲听拼写 (原听写填空)
) {
    val enabledCount: Int
        get() = listOf(
            enableBlindListenMeaning,
            enableLookChooseMeaning,
            enableRecallByMeaning,
            enableListenChooseWord,
            enableListenSentenceChooseWord,
            enableEnglishMeaningChooseWord,
            enableBlindSpelling
        ).count { it }
}

/**
 * 随身听播放与展示内容选项 (支持多选：单词、中释、英释、例句1、例句2、例句3)
 */
enum class PortableContentItem(val id: String, val label: String) {
    WORD("word", "单词"),
    CHINESE_MEANING("cn_meaning", "中释"),
    ENGLISH_DEFINITION("en_def", "英释"),
    SENTENCE_1("sentence_1", "例句1"),
    SENTENCE_2("sentence_2", "例句2"),
    SENTENCE_3("sentence_3", "例句3");

    companion object {
        fun fromId(id: String): PortableContentItem? = values().find { it.id == id }
        val DEFAULT_SELECTION = setOf(WORD, CHINESE_MEANING)
    }
}

/**
 * Portable player (随身听) playback legacy compatibility modes
 */
enum class PortableAudioMode(val title: String, val description: String) {
    ONLY_WORD("仅单词发音", "朗读英文单词发音"),
    WORD_AND_SENTENCE("单词 + 原声双语例句", "朗读英文单词与原声例句"),
    WORD_MEANING_SENTENCE("单词 + 中文释义 + 例句", "朗读单词、中文释义与双语例句")
}

/**
 * Current playback sub-phase for highlight in lyrics/teleprompter
 */
enum class PortablePlayPhase {
    IDLE,
    WORD,
    MEANING, // backward compat alias for CHINESE_MEANING
    CHINESE_MEANING,
    ENGLISH_DEFINITION,
    SENTENCE, // backward compat alias
    SENTENCE_1,
    SENTENCE_2,
    SENTENCE_3,
    PAUSE
}

/**
 * Portable player voice config (Support US, UK, TTS multi-selection)
 */
data class PortableVoiceSelection(
    val enableUS: Boolean = true,
    val enableUK: Boolean = true,
    val enableTTS: Boolean = false
)

/**
 * Accent/timbre selection for the first listen step
 */
enum class FirstListenVoicePreference(val title: String, val flag: String) {
    DEFAULT("跟随默认发音库", "⭐"),
    US("美式发音 (US)", "🇺🇸"),
    UK("英式发音 (UK)", "🇬🇧"),
    ALTERNATING("双音色交替循环", "🔄")
}

/**
 * Stages in a learning item's lifecycle within a session
 */
enum class LearnItemStage {
    BLIND_LISTEN_MEANING,         // 1. 盲听选义 (原听音选义)
    LOOK_CHOOSE_MEANING,          // 2. 看词选义
    RECALL_BY_MEANING,            // 3. 看义说词 (5秒倒计时)
    LISTEN_CHOOSE_WORD,           // 4. 听音选词 (4个拼写相似干扰项)
    LISTEN_SENTENCE_CHOOSE_WORD,  // 5. 听句选词 (例句语境)
    ENGLISH_MEANING_CHOOSE_WORD,  // 6. 英文释义选词
    BLIND_SPELLING,               // 7. 盲听拼写 (原听写填空)
    CHOOSE_MEANING_EXPLAIN,       // 详情解释界面
    FIRST_LISTEN,                 // 兼容保留
    FIRST_LISTEN_EXPLAIN,         // 兼容保留
    LISTEN_CHOOSE_MEANING,        // 兼容保留别名
    DICTATION_SPELLING,           // 兼容保留别名
    CLOZE_SENTENCE                // 兼容保留别名
}

/**
 * Represents an item in the active learning queue
 */
data class LearnSessionItem(
    val word: WordEntity,
    val stage: LearnItemStage = LearnItemStage.BLIND_LISTEN_MEANING,
    val recognizedSuccessCount: Int = 0, // 成功认识或答对次数
    val failOrHintCount: Int = 0,         // 点击提示或答错次数
    val totalAppearCount: Int = 1,        // 在本次学习中出现的次数
    val multipleChoiceOptions: List<String> = emptyList(), // 4 个选项
    val selectedOptionIndex: Int? = null,
    val isAnswerCorrect: Boolean? = null,
    val typedSpelling: String = "",        // 用于拼写/听写模式
    val countdownSecondsLeft: Int = 5,     // 用于看义说词模式 (5秒)
    val remainingSpellingAttempts: Int = 3, // 盲听拼写剩余机会 (共3次)
    val isWordRevealed: Boolean = false,   // 提示后是否显示单词
    val isHintClicked: Boolean = false,    // 是否已点击提示按钮 (点击后变为看答案)
    val flashingOptionIndex: Int? = null,  // 正在闪烁边框的选项索引
    val flashingIsCorrect: Boolean = true, // 闪烁框颜色 (true: 绿框, false: 红框)
    val showCorrectSpelling: Boolean = false, // 拼写错误后展示正确拼法5秒
    val spellingSuccess: Boolean = false,  // 拼写正确庆祝状态
    val showSentenceText: Boolean = false, // 听句选词是否显示句子
    val showSentenceTranslation: Boolean = false // 听句选词是否显示中文翻译
)

/**
 * 固定组结构（根据设置的每日学习数量/每组词数切分，被标熟的单词自动移出该组）
 */
data class WordFixedGroup(
    val groupIndex: Int,                 // 1, 2, 3...
    val tag: String,                     // "1#", "2#", "3#"...
    val listLabel: String = "List$groupIndex", // "List1", "List2"...
    val words: List<WordEntity>,         // 固定组内单词（未标熟）
    val unlearnedCount: Int = 0,         // 未学词数
    val dueCount: Int = 0,               // 待复习词数 (艾宾浩斯到期)
    val learnedCount: Int = 0,           // 已学词数
    val isTaskGroup: Boolean = false,    // 是否为当前任务组
    val isReviewGroup: Boolean = false   // 是否包含待复习词
)

/**
 * 组学习状态（用于全部计划表格中的颜色标注）
 */
enum class GroupPlanStatus(val title: String) {
    LEARNED("已学习"),      // 绿色标注 + 对勾
    IN_PROGRESS("正学习"),  // 橙色/高亮主色标注
    UNLEARNED("未学习")     // 灰色中性标注
}

/**
 * 艾宾浩斯单日学习与复习计划行（参考杨鹏17天艾宾浩斯曲线，每天记忆1组，最多复习5组）
 */
data class EbbinghausDayPlan(
    val dayNumber: Int,                     // Day 1, Day 2...
    val memorizeGroupIndex: Int?,           // 记忆的组 (List D，超过总组数则为 null)
    val reviewGroupIndices: List<Int>       // 复习1..复习5 对应的组序号 (最多5组)
)

object EbbinghausPlanGenerator {
    /**
     * 生成完整的艾宾浩斯学习与复习计划表
     * 艾宾浩斯经典复习间隔：1天后、2天后、4天后、7天后、15天后
     * 对任意 Day D，复习列表为在 D-15, D-7, D-4, D-2, D-1 天所背诵的有效组（最多 5 组）
     */
    fun generatePlan(totalGroups: Int): List<EbbinghausDayPlan> {
        if (totalGroups <= 0) return emptyList()
        val totalDays = totalGroups + 15 // 最后一组背完后还需复习至第15天
        val intervals = listOf(15, 7, 4, 2, 1)

        return (1..totalDays).map { day ->
            val memorize = if (day <= totalGroups) day else null
            val reviews = intervals
                .map { interval -> day - interval }
                .filter { it in 1..totalGroups }
                .sorted() // 按组号从小到大排列，即对应 复习1, 复习2, 复习3, 复习4, 复习5
            EbbinghausDayPlan(
                dayNumber = day,
                memorizeGroupIndex = memorize,
                reviewGroupIndices = reviews
            )
        }
    }
}
