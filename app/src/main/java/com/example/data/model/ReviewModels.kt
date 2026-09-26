package com.example.data.model

/**
 * 4 大复习学习方式
 */
enum class ReviewQuestionMode(val title: String, val description: String) {
    BLIND_LISTEN_MEANING("盲听选义", "听单词发音，在4个中文释义中选出正确含义"),
    CHINESE_MEANING_CHOOSE_WORD("中文释义选词", "看中文释义，在4个英文单词中选出正确单词"),
    RECALL_BY_MEANING("看义说词", "看中文释义在脑海中回忆并说出英文，点击核对"),
    SPELLING("拼写", "根据发音与中文释义拼写出正确英文单词 (默认关闭)")
}

/**
 * 复习方式开关配置
 */
data class ReviewModeSwitches(
    val blindListenMeaning: Boolean = true,
    val chineseMeaningChooseWord: Boolean = true,
    val recallByMeaning: Boolean = true,
    val spelling: Boolean = false // 默认关闭
) {
    val enabledCount: Int
        get() = (if (blindListenMeaning) 1 else 0) +
                (if (chineseMeaningChooseWord) 1 else 0) +
                (if (recallByMeaning) 1 else 0) +
                (if (spelling) 1 else 0)

    fun getActiveModes(): List<ReviewQuestionMode> {
        val list = mutableListOf<ReviewQuestionMode>()
        if (blindListenMeaning) list.add(ReviewQuestionMode.BLIND_LISTEN_MEANING)
        if (chineseMeaningChooseWord) list.add(ReviewQuestionMode.CHINESE_MEANING_CHOOSE_WORD)
        if (recallByMeaning) list.add(ReviewQuestionMode.RECALL_BY_MEANING)
        if (spelling) list.add(ReviewQuestionMode.SPELLING)
        return if (list.isEmpty()) listOf(ReviewQuestionMode.BLIND_LISTEN_MEANING) else list
    }
}

/**
 * 复习单词的累积状态 (累计答对 2 次即消灭)
 */
data class ReviewWordProgress(
    val word: WordEntity,
    val correctCount: Int = 0,
    val failCount: Int = 0
) {
    val isEliminated: Boolean get() = correctCount >= 2
}

/**
 * 当前正在呈现给用户的复习题目
 */
data class ReviewActiveQuestion(
    val word: WordEntity,
    val mode: ReviewQuestionMode,
    val multipleChoiceOptions: List<String> = emptyList(),
    val selectedOptionIndex: Int? = null,
    val isAnswerCorrect: Boolean? = null,
    val isWordRevealed: Boolean = false,
    val typedSpelling: String = "",
    val isShowingDetail: Boolean = false
)

/**
 * 复习切片（每组）
 */
data class ReviewGroupSlice(
    val sliceIndex: Int,
    val totalSlices: Int,
    val words: List<WordEntity>
)
