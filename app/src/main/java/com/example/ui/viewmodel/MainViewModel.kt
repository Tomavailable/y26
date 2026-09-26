package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SentenceVoiceType
import com.example.audio.WordPronouncer
import com.example.data.dict.ExtractedDictItem
import com.example.data.dict.MdxDictionaryManager
import com.example.data.dict.VoiceAccent
import com.example.data.dict.VoiceProfile
import com.example.data.dict.VoiceSourceManager
import com.example.data.importer.WordImporter
import com.example.data.local.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookSentenceEntity
import com.example.data.model.FirstListenVoicePreference
import com.example.data.model.GroupPlanStatus
import kotlinx.coroutines.flow.Flow
import com.example.data.model.LearningIntensity
import com.example.data.model.LearningMode
import com.example.data.model.LearningModeSwitches
import com.example.data.model.SessionType
import com.example.data.dict.VoiceConnectivityReport
import com.example.data.model.LearnItemStage
import com.example.data.model.LearnSessionItem
import com.example.data.model.PortableAudioMode
import com.example.data.model.PortableContentItem
import com.example.data.model.PortablePlayPhase
import com.example.data.model.PortableVoiceSelection
import com.example.data.model.ReviewQuality
import com.example.data.model.ReviewQuestionMode
import com.example.data.model.ReviewModeSwitches
import com.example.data.model.ReviewWordProgress
import com.example.data.model.ReviewActiveQuestion
import com.example.data.model.ReviewGroupSlice
import com.example.data.model.WordBookEntity
import com.example.data.model.WordEntity
import com.example.data.model.WordFixedGroup
import com.example.data.repository.WordRepository
import androidx.compose.ui.graphics.Color
import com.example.reminder.ReviewReminderManager
import com.example.ui.navigation.AppScreen
import com.example.ui.theme.AppDayTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CustomColorConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = WordRepository(database.wordBookDao(), database.wordDao(), database.bookSentenceDao(), application)
    val voiceManager = VoiceSourceManager(application)
    val mdxManager = MdxDictionaryManager(application)
    val pronouncer = WordPronouncer(application, voiceManager)

    // Custom Example Books & Sentences
    val allCustomBooks: StateFlow<List<BookEntity>> = repository.allCustomBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _defaultExampleSource = MutableStateFlow("builtin") // "builtin" or bookId
    val defaultExampleSource: StateFlow<String> = _defaultExampleSource.asStateFlow()

    private val _selectedBuiltInDictionary = MutableStateFlow(com.example.data.dict.BuiltInDictionary.DEFAULT)
    val selectedBuiltInDictionary: StateFlow<com.example.data.dict.BuiltInDictionary> = _selectedBuiltInDictionary.asStateFlow()

    // 当前会话类型：学习新词还是艾宾浩斯复习
    private val _currentSessionType = MutableStateFlow(SessionType.LEARN)
    val currentSessionType: StateFlow<SessionType> = _currentSessionType.asStateFlow()

    // 答对自动进入下一个（学习模式默认关闭 false，答对后也自动进入详情界面查看例句和全量释义）
    private val _autoAdvanceOnCorrectAnswer = MutableStateFlow(false)
    val autoAdvanceOnCorrectAnswer: StateFlow<Boolean> = _autoAdvanceOnCorrectAnswer.asStateFlow()

    // 答对自动进入下一个（复习模式专用开关，默认开启 true，答对极速切题，仅在答错或忘记时才进入详情页）
    private val _reviewAutoAdvanceOnCorrectAnswer = MutableStateFlow(true)
    val reviewAutoAdvanceOnCorrectAnswer: StateFlow<Boolean> = _reviewAutoAdvanceOnCorrectAnswer.asStateFlow()

    val activeAutoAdvanceOnCorrect: Boolean
        get() = if (_currentSessionType.value == SessionType.REVIEW) _reviewAutoAdvanceOnCorrectAnswer.value else _autoAdvanceOnCorrectAnswer.value

    fun setAutoAdvanceOnCorrectAnswer(enabled: Boolean) {
        _autoAdvanceOnCorrectAnswer.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_advance_on_correct", enabled).apply()
        showMessage(if (enabled) "已开启学习切题：答对自动进入下一个" else "已关闭学习切题：答对后进入详情界面")
    }

    fun setReviewAutoAdvanceOnCorrectAnswer(enabled: Boolean) {
        _reviewAutoAdvanceOnCorrectAnswer.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("review_auto_advance_on_correct", enabled).apply()
        showMessage(if (enabled) "已开启复习切题：答对极速进入下一个" else "已关闭复习切题：答对后进入详情界面")
    }

    fun setBuiltInDictionary(dict: com.example.data.dict.BuiltInDictionary) {
        _selectedBuiltInDictionary.value = dict
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("selected_builtin_dict", dict.id).apply()
        showMessage("已切换内置词典为：${dict.displayName}")
    }

    fun formatWordForCurrentDictionary(word: WordEntity): WordEntity {
        return com.example.data.dict.BuiltInDictionaryManager.formatForDictionary(word, _selectedBuiltInDictionary.value)
    }

    fun setDefaultExampleSource(source: String) {
        _defaultExampleSource.value = source
    }

    fun importCustomBookText(
        title: String,
        fileName: String,
        rawText: String,
        onComplete: (sentenceCount: Int, wordCount: Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = repository.importCustomBookText(title, fileName, rawText)
            onComplete(result.sentences.size, result.matchedWordCount)
        }
    }

    fun deleteCustomBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteCustomBook(bookId)
            if (_defaultExampleSource.value == bookId) {
                _defaultExampleSource.value = "builtin"
            }
        }
    }

    fun getSentencesForWord(word: String): Flow<List<BookSentenceEntity>> {
        return repository.getSentencesForWord(word)
    }

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active WordBook
    private val _activeBookId = MutableStateFlow("coca_1_3500")
    val activeBookId: StateFlow<String> = _activeBookId.asStateFlow()

    val allBooks: StateFlow<List<WordBookEntity>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wordsForActiveBook: StateFlow<List<WordEntity>> = _activeBookId
        .flatMapLatest { bookId -> repository.getWordsForBook(bookId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWordsCount: StateFlow<Int> = repository.totalWordsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalLearnedCount: StateFlow<Int> = repository.totalLearnedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMasteredCount: StateFlow<Int> = repository.totalMasteredCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val dueReviewCount: StateFlow<Int> = _currentTime
        .flatMapLatest { now -> repository.getDueReviewCount(now) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favoriteWords: StateFlow<List<WordEntity>> = repository.favoriteWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 已学习天数统计与持久化
    private val _learnedDaysCount = MutableStateFlow(1)
    val learnedDaysCount: StateFlow<Int> = _learnedDaysCount.asStateFlow()

    fun recordLearningDayActivity() {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet("learned_dates", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (currentSet.add(todayStr)) {
            prefs.edit().putStringSet("learned_dates", currentSet).apply()
            _learnedDaysCount.value = currentSet.size
        } else if (_learnedDaysCount.value < currentSet.size) {
            _learnedDaysCount.value = currentSet.size
        }
    }

    // 固定组划分设置（每组词数，默认 10 词/组）
    private val _dailyWordsPerGroup = MutableStateFlow(10)
    val dailyWordsPerGroup: StateFlow<Int> = _dailyWordsPerGroup.asStateFlow()

    fun setDailyWordsPerGroup(count: Int) {
        val safeCount = count.coerceIn(5, 100)
        _dailyWordsPerGroup.value = safeCount
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt("daily_words_per_group", safeCount).apply()
        showMessage("已设置固定分组为每组 $safeCount 词")
    }

    // 每日学习数量设置（几组，默认 1 组）
    private val _dailyTargetGroups = MutableStateFlow(1)
    val dailyTargetGroups: StateFlow<Int> = _dailyTargetGroups.asStateFlow()

    fun setDailyTargetGroups(count: Int) {
        val safeCount = count.coerceIn(1, 100)
        _dailyTargetGroups.value = safeCount
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt("daily_target_groups", safeCount).apply()
        showMessage("已设置每日学习数量为 $safeCount 组")
    }

    // 每日复习数量设置（按组为单位，默认 5 组/天，支持自定义）
    private val _dailyReviewTargetGroups = MutableStateFlow(5)
    val dailyReviewTargetGroups: StateFlow<Int> = _dailyReviewTargetGroups.asStateFlow()

    fun setDailyReviewTargetGroups(count: Int) {
        val safeCount = count.coerceIn(1, 100)
        _dailyReviewTargetGroups.value = safeCount
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt("daily_review_target_groups", safeCount).apply()
        showMessage("已设置每日复习数量为 $safeCount 组")
    }

    // 主题与色彩设置（支持跟随系统/日间/夜间模式，默认采用经典黑红主题，全自由自定义）
    private val _appThemeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    private val _appDayTheme = MutableStateFlow(AppDayTheme.CRIMSON_OBSIDIAN)
    val appDayTheme: StateFlow<AppDayTheme> = _appDayTheme.asStateFlow()

    private val _customColorConfig = MutableStateFlow(CustomColorConfig())
    val customColorConfig: StateFlow<CustomColorConfig> = _customColorConfig.asStateFlow()

    fun setAppThemeMode(mode: AppThemeMode) {
        _appThemeMode.value = mode
    }

    fun setAppDayTheme(theme: AppDayTheme) {
        _appDayTheme.value = theme
        when (theme) {
            AppDayTheme.DEFAULT -> {
                _customColorConfig.value = _customColorConfig.value.copy(isCustomEnabled = false)
                showMessage("已切换至【默认经典】配色（夜间模式已应用沉稳暗夜默认配色）")
            }
            AppDayTheme.CRIMSON_OBSIDIAN -> {
                _customColorConfig.value = _customColorConfig.value.copy(isCustomEnabled = false)
                showMessage("已开启【经典黑红】独立主题（大厂高反差黑曜绯红美学）")
            }
            AppDayTheme.CUSTOM -> {
                _customColorConfig.value = _customColorConfig.value.copy(isCustomEnabled = true)
                showMessage("已开启【全自由自定义配色】，可自由调整各部位色彩与透明度")
            }
        }
    }

    fun toggleCrimsonTheme(enabled: Boolean) {
        if (enabled) {
            setAppDayTheme(AppDayTheme.CRIMSON_OBSIDIAN)
        } else {
            setAppDayTheme(AppDayTheme.DEFAULT)
        }
    }

    fun updateCustomColor(part: String, color: Color) {
        val current = _customColorConfig.value
        val updated = when (part) {
            "background" -> current.copy(customBackground = color, isCustomEnabled = true)
            "surface" -> current.copy(customSurface = color, isCustomEnabled = true)
            "surfaceVariant" -> current.copy(customSurfaceVariant = color, isCustomEnabled = true)
            "primary" -> current.copy(customPrimary = color, isCustomEnabled = true)
            "primaryContainer" -> current.copy(customPrimaryContainer = color, isCustomEnabled = true)
            "onSurface" -> current.copy(customOnSurface = color, isCustomEnabled = true)
            "onSurfaceVariant" -> current.copy(customOnSurfaceVariant = color, isCustomEnabled = true)
            else -> current
        }
        _customColorConfig.value = updated
        _appDayTheme.value = AppDayTheme.CUSTOM
    }

    fun resetCustomColors() {
        _customColorConfig.value = CustomColorConfig()
        _appDayTheme.value = AppDayTheme.DEFAULT
        showMessage("已恢复为系统默认经典配色与原生暗夜夜间模式")
    }

    // 固定组（根据设定的每日学习数量切分，被标熟的单词移出该组，根据艾宾浩斯学习与复习）
    val fixedWordGroups: StateFlow<List<WordFixedGroup>> = combine(
        wordsForActiveBook,
        _currentTime,
        _dailyWordsPerGroup,
        _dailyTargetGroups
    ) { words, now, groupSize, targetGroupsCount ->
        val unmastered = words.filter { !it.isMastered }
        val chunks = unmastered.chunked(groupSize)
        var taskCount = 0
        chunks.mapIndexed { index, chunk ->
            val groupIndex = index + 1
            val unlearned = chunk.count { it.reviewStage == 0 }
            val due = chunk.count { it.nextReviewTime in 1..now }
            val learned = chunk.count { it.reviewStage > 0 }
            val isTask = unlearned > 0 && taskCount < targetGroupsCount
            if (isTask) {
                taskCount++
            }
            WordFixedGroup(
                groupIndex = groupIndex,
                tag = "L$groupIndex",
                listLabel = "List$groupIndex",
                words = chunk,
                unlearnedCount = unlearned,
                dueCount = due,
                learnedCount = learned,
                isTaskGroup = isTask,
                isReviewGroup = due > 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dictionary & Voice Management States
    val attachedMdxName: StateFlow<String> = mdxManager.attachedMdxName
    val attachedMdxPath: StateFlow<String?> = mdxManager.attachedMdxPath
    val voiceProfiles: StateFlow<List<VoiceProfile>> = voiceManager.voiceProfiles
    val multiVoiceSequentialPlay: StateFlow<Boolean> = voiceManager.multiVoiceSequentialPlay
    val voiceDownloadState = voiceManager.downloadState
    private var batchDownloadJob: Job? = null
    private var spellingHideJob: Job? = null

    // Learning Settings & Mode Switches
    private val _learningMode = MutableStateFlow(LearningMode.SCIENTIFIC_COMPREHENSIVE)
    val learningMode: StateFlow<LearningMode> = _learningMode.asStateFlow()

    private val _learningIntensity = MutableStateFlow(LearningIntensity.EASY)
    val learningIntensity: StateFlow<LearningIntensity> = _learningIntensity.asStateFlow()

    private val _learningModeSwitches = MutableStateFlow(LearningModeSwitches())
    val learningModeSwitches: StateFlow<LearningModeSwitches> = _learningModeSwitches.asStateFlow()

    // 复习模式专用的学习方式开关：默认只开启 3 种（盲听选义、看词选义、看义说词），其余 4 种默认关闭
    private val _reviewLearningModeSwitches = MutableStateFlow(
        LearningModeSwitches(
            enableBlindListenMeaning = true,
            enableLookChooseMeaning = true,
            enableRecallByMeaning = true,
            enableListenChooseWord = false,
            enableListenSentenceChooseWord = false,
            enableEnglishMeaningChooseWord = false,
            enableBlindSpelling = false
        )
    )
    val reviewLearningModeSwitches: StateFlow<LearningModeSwitches> = _reviewLearningModeSwitches.asStateFlow()

    val activeLearningModeSwitches: LearningModeSwitches
        get() = if (_currentSessionType.value == SessionType.REVIEW) _reviewLearningModeSwitches.value else _learningModeSwitches.value

    private val _firstListenRepeatTimes = MutableStateFlow(1)
    val firstListenRepeatTimes: StateFlow<Int> = _firstListenRepeatTimes.asStateFlow()

    private val _firstListenVoicePref = MutableStateFlow(FirstListenVoicePreference.DEFAULT)
    val firstListenVoicePref: StateFlow<FirstListenVoicePreference> = _firstListenVoicePref.asStateFlow()

    // Portable Player (随身听) Settings & State: 默认多选播放内容 (单词, 中释, 英释, 例句1, 例句2, 例句3)
    private val _portableAudioMode = MutableStateFlow(PortableAudioMode.ONLY_WORD)
    val portableAudioMode: StateFlow<PortableAudioMode> = _portableAudioMode.asStateFlow()

    private val _portableSelectedContentItems = MutableStateFlow<Set<PortableContentItem>>(
        PortableContentItem.DEFAULT_SELECTION
    )
    val portableSelectedContentItems: StateFlow<Set<PortableContentItem>> = _portableSelectedContentItems.asStateFlow()

    private val _portableVoiceSelection = MutableStateFlow(PortableVoiceSelection(enableUS = true, enableUK = true, enableTTS = true))
    val portableVoiceSelection: StateFlow<PortableVoiceSelection> = _portableVoiceSelection.asStateFlow()

    private val _portableEnabledProfileIds = MutableStateFlow<Set<String>>(
        setOf("voice_longman_us")
    )
    val portableEnabledProfileIds: StateFlow<Set<String>> = _portableEnabledProfileIds.asStateFlow()

    fun togglePortableVoiceProfile(profileId: String) {
        val current = _portableEnabledProfileIds.value
        val validWordProfileIds = setOf("voice_longman_us", "voice_youdao_us", "voice_youdao_uk")
        if (profileId in current) {
            val remaining = (current - profileId).filter { it in validWordProfileIds }
            if (remaining.isNotEmpty()) {
                _portableEnabledProfileIds.value = current - profileId
            } else {
                showMessage("单词发音最少选择 1 个")
            }
        } else {
            _portableEnabledProfileIds.value = current + profileId
        }
    }

    private val _portableEnabledSentenceVoiceTypes = MutableStateFlow<Set<SentenceVoiceType>>(
        setOf(SentenceVoiceType.TTS_1)
    )
    val portableEnabledSentenceVoiceTypes: StateFlow<Set<SentenceVoiceType>> = _portableEnabledSentenceVoiceTypes.asStateFlow()

    fun togglePortableSentenceVoiceType(type: SentenceVoiceType) {
        val current = _portableEnabledSentenceVoiceTypes.value
        if (type in current) {
            if (current.size > 1) {
                _portableEnabledSentenceVoiceTypes.value = current - type
            } else {
                showMessage("例句发音最少选择 1 个")
            }
        } else {
            _portableEnabledSentenceVoiceTypes.value = current + type
        }
    }

    private val _portableRepeatTimes = MutableStateFlow(1)
    val portableRepeatTimes: StateFlow<Int> = _portableRepeatTimes.asStateFlow()

    private val _portableSentenceRepeatTimes = MutableStateFlow(1)
    val portableSentenceRepeatTimes: StateFlow<Int> = _portableSentenceRepeatTimes.asStateFlow()

    private val _isPortableRandomTTS = MutableStateFlow(false)
    val isPortableRandomTTS: StateFlow<Boolean> = _isPortableRandomTTS.asStateFlow()

    private val _portablePauseSeconds = MutableStateFlow(0.5f) // 默认 0.5 秒
    val portablePauseSeconds: StateFlow<Float> = _portablePauseSeconds.asStateFlow()

    private val _portablePlayingCategory = MutableStateFlow("全部")
    val portablePlayingCategory: StateFlow<String> = _portablePlayingCategory.asStateFlow()

    private val _portablePlaylist = MutableStateFlow<List<WordEntity>>(emptyList())
    val portablePlaylist: StateFlow<List<WordEntity>> = _portablePlaylist.asStateFlow()

    private val _portableCurrentIndex = MutableStateFlow(0)
    val portableCurrentIndex: StateFlow<Int> = _portableCurrentIndex.asStateFlow()

    private val _isPortablePlaying = MutableStateFlow(false)
    val isPortablePlaying: StateFlow<Boolean> = _isPortablePlaying.asStateFlow()

    private val _portablePlayStatusText = MutableStateFlow("就绪")
    val portablePlayStatusText: StateFlow<String> = _portablePlayStatusText.asStateFlow()

    private val _portablePlayPhase = MutableStateFlow(PortablePlayPhase.IDLE)
    val portablePlayPhase: StateFlow<PortablePlayPhase> = _portablePlayPhase.asStateFlow()

    private var portableLoopJob: Job? = null

    // Word Matching Game State
    private val _matchingGameWords = MutableStateFlow<List<WordEntity>>(emptyList())
    val matchingGameWords: StateFlow<List<WordEntity>> = _matchingGameWords.asStateFlow()

    private val _isMatchingGameSoundEnabled = MutableStateFlow(true)
    val isMatchingGameSoundEnabled: StateFlow<Boolean> = _isMatchingGameSoundEnabled.asStateFlow()

    private val recentlyShownMatchingGameIds = mutableListOf<Long>()
    private val reducedMistakeWordIdsToday = mutableSetOf<Long>()
    private var lastReducedDateString: String = ""

    fun toggleMatchingGameSound() {
        _isMatchingGameSoundEnabled.value = !_isMatchingGameSoundEnabled.value
    }

    fun onWordMatchedInMatchingGame(word: WordEntity) {
        if (word.mistakeCount <= 0) return

        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

        if (lastReducedDateString != todayDate) {
            lastReducedDateString = todayDate
            reducedMistakeWordIdsToday.clear()
            val savedSet = prefs.getStringSet("matching_reduced_words_$todayDate", emptySet()) ?: emptySet()
            reducedMistakeWordIdsToday.addAll(savedSet.mapNotNull { it.toLongOrNull() })
        }

        if (word.id !in reducedMistakeWordIdsToday) {
            reducedMistakeWordIdsToday.add(word.id)
            prefs.edit().putStringSet(
                "matching_reduced_words_$todayDate",
                reducedMistakeWordIdsToday.map { it.toString() }.toSet()
            ).apply()

            viewModelScope.launch {
                repository.decrementMistakeCount(word)
            }
        }
    }

    fun refreshMatchingGame() {
        viewModelScope.launch {
            val allWords = wordsForActiveBook.value
            if (allWords.isEmpty()) return@launch
            val now = System.currentTimeMillis()

            val unmasteredWords = allWords.filter { !it.isMastered }
            if (unmasteredWords.isEmpty()) return@launch

            // 错词绝对第一优先权：优先全数填入错词 (哪怕近期出现过也优先，直到掌握消除)
            val allMistakes = unmasteredWords.filter { it.mistakeCount > 0 }
                .sortedWith(compareByDescending<WordEntity> { it.mistakeCount }.thenByDescending { it.lastReviewTime })

            // Filter out words recently shown in recent games for other categories
            var availablePool = unmasteredWords.filter { it.id !in recentlyShownMatchingGameIds }
            if (availablePool.size < 5) {
                // If available pool exhausted, clear history buffer
                recentlyShownMatchingGameIds.clear()
                availablePool = unmasteredWords
            }

            val favorites = availablePool.filter { it.isFavorite && it !in allMistakes }.shuffled()
            val due = availablePool.filter { it.nextReviewTime in 1..now && it !in allMistakes && it !in favorites }.shuffled()
            val learned = availablePool.filter { it.reviewStage > 0 && it !in allMistakes && it !in favorites && it !in due }.shuffled()
            val remaining = availablePool.filter { it !in allMistakes }.shuffled()

            val chosen = mutableListOf<WordEntity>()

            // 1. 第一优先权：优先全数填入错词
            for (w in allMistakes) {
                if (chosen.size >= 5) break
                if (w !in chosen) chosen.add(w)
            }

            // 2. 收藏词补充
            for (w in favorites) {
                if (chosen.size >= 5) break
                if (w !in chosen) chosen.add(w)
            }

            // 3. 到期复习词补充
            for (w in due) {
                if (chosen.size >= 5) break
                if (w !in chosen) chosen.add(w)
            }

            // 4. 已学词补充
            for (w in learned) {
                if (chosen.size >= 5) break
                if (w !in chosen) chosen.add(w)
            }

            // 5. 剩余词池补充
            for (w in remaining) {
                if (chosen.size >= 5) break
                if (w !in chosen) chosen.add(w)
            }

            // Ensure 5 words and shuffle selected ones
            val final5 = chosen.take(5).shuffled()
            _matchingGameWords.value = final5

            // Track in recent history (retain up to 30 recent word IDs)
            recentlyShownMatchingGameIds.addAll(final5.map { it.id })
            if (recentlyShownMatchingGameIds.size > 30) {
                val overflow = recentlyShownMatchingGameIds.size - 30
                repeat(overflow) {
                    if (recentlyShownMatchingGameIds.isNotEmpty()) {
                        recentlyShownMatchingGameIds.removeAt(0)
                    }
                }
            }
        }
    }

    // Cache & Data Management
    fun clearAudioCache() {
        voiceManager.clearAllCaches()
        showMessage("已清除所有音频缓存")
    }

    fun resetAppData() {
        viewModelScope.launch {
            try {
                database.clearAllTables()
                repository.checkAndSeedInitialData()
                _activeBookId.value = "coca_1_3500"
                showMessage("学习数据已重置")
                navigateTo(AppScreen.DASHBOARD)
            } catch (e: Exception) {
                showMessage("重置失败: ${e.localizedMessage}")
            }
        }
    }

    // Rich Multi-Stage Learning Session State
    private val _sessionItems = MutableStateFlow<List<LearnSessionItem>>(emptyList())
    val sessionItems: StateFlow<List<LearnSessionItem>> = _sessionItems.asStateFlow()

    private val _currentSessionIndex = MutableStateFlow(0)
    val currentSessionIndex: StateFlow<Int> = _currentSessionIndex.asStateFlow()

    private val _isWordAudioPlaying = MutableStateFlow(false)
    val isWordAudioPlaying: StateFlow<Boolean> = _isWordAudioPlaying.asStateFlow()

    private val _currentAudioListenCount = MutableStateFlow(1)
    val currentAudioListenCount: StateFlow<Int> = _currentAudioListenCount.asStateFlow()

    private val _isWordRevealedAfterAudio = MutableStateFlow(false)
    val isWordRevealedAfterAudio: StateFlow<Boolean> = _isWordRevealedAfterAudio.asStateFlow()

    private val _isLearningFinished = MutableStateFlow(false)
    val isLearningFinished: StateFlow<Boolean> = _isLearningFinished.asStateFlow()

    private val _sessionPassedCount = MutableStateFlow(0)
    val sessionPassedCount: StateFlow<Int> = _sessionPassedCount.asStateFlow()

    private val _sessionMasteredCount = MutableStateFlow(0)
    val sessionMasteredCount: StateFlow<Int> = _sessionMasteredCount.asStateFlow()

    private val _sessionReinforcedCount = MutableStateFlow(0)
    val sessionReinforcedCount: StateFlow<Int> = _sessionReinforcedCount.asStateFlow()

    // Backward compatibility for legacy callers
    val learningQueue: StateFlow<List<WordEntity>> = MutableStateFlow<List<WordEntity>>(emptyList()).asStateFlow()
    val learningIndex: StateFlow<Int> = _currentSessionIndex
    val isMeaningRevealed: StateFlow<Boolean> = _isWordRevealedAfterAudio

    // Review Session State
    private val _reviewQueue = MutableStateFlow<List<WordEntity>>(emptyList())
    val reviewQueue: StateFlow<List<WordEntity>> = _reviewQueue.asStateFlow()

    private val _reviewIndex = MutableStateFlow(0)
    val reviewIndex: StateFlow<Int> = _reviewIndex.asStateFlow()

    private val _isReviewMeaningRevealed = MutableStateFlow(false)
    val isReviewMeaningRevealed: StateFlow<Boolean> = _isReviewMeaningRevealed.asStateFlow()

    private val _isReviewFinished = MutableStateFlow(false)
    val isReviewFinished: StateFlow<Boolean> = _isReviewFinished.asStateFlow()

    private val _sessionReviewedCount = MutableStateFlow(0)
    val sessionReviewedCount: StateFlow<Int> = _sessionReviewedCount.asStateFlow()

    // 艾宾浩斯复习：方式开关、发音设置与分组切片消灭状态
    private val _reviewModeSwitches = MutableStateFlow(ReviewModeSwitches())
    val reviewModeSwitches: StateFlow<ReviewModeSwitches> = _reviewModeSwitches.asStateFlow()

    private val _reviewAutoPronounce = MutableStateFlow(true)
    val reviewAutoPronounce: StateFlow<Boolean> = _reviewAutoPronounce.asStateFlow()

    private val _reviewVoicePref = MutableStateFlow(FirstListenVoicePreference.DEFAULT)
    val reviewVoicePref: StateFlow<FirstListenVoicePreference> = _reviewVoicePref.asStateFlow()

    private val _reviewSlices = MutableStateFlow<List<ReviewGroupSlice>>(emptyList())
    val reviewSlices: StateFlow<List<ReviewGroupSlice>> = _reviewSlices.asStateFlow()

    private val _currentReviewSliceIndex = MutableStateFlow(0)
    val currentReviewSliceIndex: StateFlow<Int> = _currentReviewSliceIndex.asStateFlow()

    private val _currentSliceProgress = MutableStateFlow<Map<String, ReviewWordProgress>>(emptyMap())
    val currentSliceProgress: StateFlow<Map<String, ReviewWordProgress>> = _currentSliceProgress.asStateFlow()

    private val _activeRoundWords = MutableStateFlow<List<WordEntity>>(emptyList())
    val activeRoundWords: StateFlow<List<WordEntity>> = _activeRoundWords.asStateFlow()

    private val _currentWordInRoundIndex = MutableStateFlow(0)
    val currentWordInRoundIndex: StateFlow<Int> = _currentWordInRoundIndex.asStateFlow()

    private val _currentReviewQuestion = MutableStateFlow<ReviewActiveQuestion?>(null)
    val currentReviewQuestion: StateFlow<ReviewActiveQuestion?> = _currentReviewQuestion.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<WordEntity>> = _searchQuery
        .flatMapLatest { q -> repository.searchWords(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminder State
    private val _isReminderEnabled = MutableStateFlow(ReviewReminderManager.isReminderEnabled(application))
    val isReminderEnabled: StateFlow<Boolean> = _isReminderEnabled.asStateFlow()

    private val initialTime = ReviewReminderManager.getReminderTime(application)
    private val _reminderHour = MutableStateFlow(initialTime.first)
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(initialTime.second)
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    // Toast / Banner Message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Import Report
    private val _lastImportReport = MutableStateFlow<WordImporter.ImportReport?>(null)
    val lastImportReport: StateFlow<WordImporter.ImportReport?> = _lastImportReport.asStateFlow()

    fun clearImportReport() {
        _lastImportReport.value = null
    }

    private var countdownJob: Job? = null

    init {
        val prefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val savedDictId = prefs.getString("selected_builtin_dict", com.example.data.dict.BuiltInDictionary.DEFAULT.id)
        _selectedBuiltInDictionary.value = com.example.data.dict.BuiltInDictionary.fromId(savedDictId ?: "default")

        _dailyWordsPerGroup.value = prefs.getInt("daily_words_per_group", 10)
        _dailyTargetGroups.value = prefs.getInt("daily_target_groups", 1)
        _dailyReviewTargetGroups.value = prefs.getInt("daily_review_target_groups", 5)
        _portablePauseSeconds.value = prefs.getFloat("portable_pause_seconds", 0.5f)
        val savedContentItems = prefs.getString("portable_selected_content_items", null)
        if (savedContentItems != null) {
            val items = savedContentItems.split(",")
                .mapNotNull { PortableContentItem.fromId(it.trim()) }
                .toSet()
            if (items.isNotEmpty()) {
                _portableSelectedContentItems.value = items
            }
        }
        _autoAdvanceOnCorrectAnswer.value = prefs.getBoolean("auto_advance_on_correct", false)
        _reviewAutoAdvanceOnCorrectAnswer.value = prefs.getBoolean("review_auto_advance_on_correct", true)

        _learningModeSwitches.value = LearningModeSwitches(
            enableBlindListenMeaning = prefs.getBoolean("learn_mode_blind_listen_meaning", true),
            enableLookChooseMeaning = prefs.getBoolean("learn_mode_look_choose_meaning", true),
            enableRecallByMeaning = prefs.getBoolean("learn_mode_recall_by_meaning", true),
            enableListenChooseWord = prefs.getBoolean("learn_mode_listen_choose_word", true),
            enableListenSentenceChooseWord = prefs.getBoolean("learn_mode_listen_sentence_choose_word", true),
            enableEnglishMeaningChooseWord = prefs.getBoolean("learn_mode_english_meaning_choose_word", true),
            enableBlindSpelling = prefs.getBoolean("learn_mode_blind_spelling", true)
        )

        _reviewLearningModeSwitches.value = LearningModeSwitches(
            enableBlindListenMeaning = prefs.getBoolean("review_mode_blind_listen_meaning", true),
            enableLookChooseMeaning = prefs.getBoolean("review_mode_look_choose_meaning", true),
            enableRecallByMeaning = prefs.getBoolean("review_mode_recall_by_meaning", true),
            enableListenChooseWord = prefs.getBoolean("review_mode_listen_choose_word", false),
            enableListenSentenceChooseWord = prefs.getBoolean("review_mode_listen_sentence_choose_word", false),
            enableEnglishMeaningChooseWord = prefs.getBoolean("review_mode_english_meaning_choose_word", false),
            enableBlindSpelling = prefs.getBoolean("review_mode_blind_spelling", false)
        )

        val savedBlindListen = prefs.getBoolean("review_mode_blind_listen", true)
        val savedChineseMeaning = prefs.getBoolean("review_mode_chinese_meaning", true)
        val savedRecall = prefs.getBoolean("review_mode_recall", true)
        val savedSpelling = prefs.getBoolean("review_mode_spelling", false)
        _reviewModeSwitches.value = ReviewModeSwitches(
            blindListenMeaning = savedBlindListen,
            chineseMeaningChooseWord = savedChineseMeaning,
            recallByMeaning = savedRecall,
            spelling = savedSpelling
        )
        _reviewAutoPronounce.value = prefs.getBoolean("review_auto_pronounce", true)
        val savedVoicePref = prefs.getString("review_voice_pref", FirstListenVoicePreference.DEFAULT.name)
        _reviewVoicePref.value = try {
            FirstListenVoicePreference.valueOf(savedVoicePref ?: FirstListenVoicePreference.DEFAULT.name)
        } catch (e: Exception) {
            FirstListenVoicePreference.DEFAULT
        }

        val savedLearnedDates = prefs.getStringSet("learned_dates", emptySet()) ?: emptySet()
        _learnedDaysCount.value = savedLearnedDates.size.coerceAtLeast(if (savedLearnedDates.isNotEmpty()) 1 else 0)

        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            // 同步历史打卡天数与学习时间戳
            try {
                val allWords = repository.getAllWordsSync()
                val reviewDates = allWords.filter { it.lastReviewTime > 0 }.map {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.lastReviewTime))
                }.toSet()
                val merged = (savedLearnedDates + reviewDates).toMutableSet()
                if (merged.isNotEmpty()) {
                    prefs.edit().putStringSet("learned_dates", merged).apply()
                    _learnedDaysCount.value = merged.size
                } else if (_learnedDaysCount.value == 0 && allWords.any { it.reviewStage > 0 }) {
                    _learnedDaysCount.value = 1
                }
            } catch (e: Exception) {
                // Ignore sync errors
            }
            refreshCurrentTime()
            refreshMatchingGame()
        }
        viewModelScope.launch {
            wordsForActiveBook.collect { words ->
                if (words.isNotEmpty() && _portablePlaylist.value.isEmpty()) {
                    updatePortablePlaylist(_portablePlayingCategory.value)
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.PORTABLE_PLAYER) {
            updatePortablePlaylist(_portablePlayingCategory.value)
        }
        refreshCurrentTime()
    }

    fun setActiveBook(bookId: String) {
        _activeBookId.value = bookId
    }

    fun refreshCurrentTime() {
        _currentTime.value = System.currentTimeMillis()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // Sentence Pronunciation State
    private val _activeSentenceVoicePlaying = MutableStateFlow<SentenceVoiceType?>(null)
    val activeSentenceVoicePlaying: StateFlow<SentenceVoiceType?> = _activeSentenceVoicePlaying.asStateFlow()

    // Pronunciation with specific voice profile or default
    fun pronounceWord(word: String, profileId: String? = null) {
        _activeSentenceVoicePlaying.value = null
        pronouncer.speak(word, profileId)
    }

    fun pronounceSequence(word: String) {
        _activeSentenceVoicePlaying.value = null
        pronouncer.speakSequence(word)
    }

    private var sentenceRotationCounter = 0

    fun pronounceSentence(sentence: String, type: SentenceVoiceType? = null) {
        val cleanSentence = sentence.split(" ||| ").firstOrNull { it.isNotBlank() } ?: sentence
        val actualType = if (type != null) {
            type
        } else if (sentenceVoiceRotationEnabled.value) {
            val types = SentenceVoiceType.values()
            val chosen = types[sentenceRotationCounter % types.size]
            sentenceRotationCounter++
            chosen
        } else {
            SentenceVoiceType.TTS_1
        }

        _activeSentenceVoicePlaying.value = actualType
        pronouncer.speakSentence(
            sentence = cleanSentence,
            type = actualType,
            onStart = {
                _activeSentenceVoicePlaying.value = actualType
            },
            onCompletion = {
                if (_activeSentenceVoicePlaying.value == actualType) {
                    _activeSentenceVoicePlaying.value = null
                }
            }
        )
    }

    private var autoPronounceJob: Job? = null

    /**
     * 学习模式进入详情页时，自动依次朗读单词与第一个例句
     */
    fun pronounceWordAndFirstSentence(word: WordEntity) {
        autoPronounceJob?.cancel()
        autoPronounceJob = viewModelScope.launch {
            pronounceWord(word.word)
            // 等待单词发音播放完成（约1.1秒）
            delay(1150)
            val firstSentence = word.exampleSentence.split(" ||| ").firstOrNull { it.isNotBlank() }
            if (!firstSentence.isNullOrBlank()) {
                pronounceSentence(firstSentence, SentenceVoiceType.TTS_1)
            }
        }
    }

    // Dictionary & voice settings
    fun attachMdx(name: String, path: String) {
        mdxManager.attachMdxFile(name, path)
        showMessage("已成功挂载 MDX 外部词典：$name")
    }

    fun detachMdx() {
        mdxManager.detachMdxFile()
        showMessage("已卸载外部 MDX 词典")
    }

    fun setMultiVoiceSequentialPlay(enabled: Boolean) {
        voiceManager.setMultiVoiceSequentialPlay(enabled)
        showMessage(if (enabled) "已开启单词多音源轮流发音模式" else "已切换为仅播放默认单词发音方案")
    }

    val sentenceVoiceRotationEnabled: StateFlow<Boolean> = voiceManager.sentenceVoiceRotationEnabled

    fun setSentenceVoiceRotationEnabled(enabled: Boolean) {
        voiceManager.setSentenceVoiceRotationEnabled(enabled)
        showMessage(if (enabled) "已开启例句与释义轮播发音模式" else "已切换为标准例句发音")
    }

    fun toggleVoiceProfile(profileId: String, enabled: Boolean) {
        val success = voiceManager.toggleVoice(profileId, enabled)
        val profile = voiceManager.voiceProfiles.value.find { it.id == profileId }
        val name = profile?.name ?: "发音库"
        if (!success) {
            showMessage("【$name】当前是默认音源，须取消默认或切换其它为默认后才能关闭！")
        } else {
            showMessage(if (enabled) "已开启 $name" else "已关闭 $name")
        }
    }

    fun addLocalTtsVoice(name: String, accent: VoiceAccent, ttsVoiceName: String?, pitch: Float, speechRate: Float) {
        voiceManager.addLocalTtsVoiceProfile(name, accent, ttsVoiceName, pitch, speechRate)
        showMessage("已添加自定义本地 TTS 音源【$name】")
    }

    fun updateTtsVoiceProfile(profileId: String, name: String, accent: VoiceAccent, ttsVoiceName: String?, pitch: Float, speechRate: Float) {
        voiceManager.updateTtsVoiceProfile(profileId, name, accent, ttsVoiceName, pitch, speechRate)
        showMessage("已更新本地 TTS 发音人配置【$name】")
    }

    fun getAvailableTtsVoices(): List<com.example.data.dict.TtsVoiceInfo> {
        return pronouncer.getAvailableTtsVoices()
    }



    fun testVoiceConnectivity(profile: VoiceProfile, onReport: (VoiceConnectivityReport) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val report = pronouncer.testVoiceConnectivity(profile)
            launch(Dispatchers.Main) {
                onReport(report)
            }
        }
    }

    fun setDefaultVoiceProfile(profileId: String) {
        voiceManager.setDefaultVoice(profileId)
        val profile = voiceManager.voiceProfiles.value.find { it.id == profileId }
        showMessage("已设【${profile?.name}】为默认主音源，自动升至第一优先级")
    }

    fun clearVoiceCache(profile: VoiceProfile) {
        voiceManager.clearVoiceCache(profile)
        showMessage("已清空【${profile.name}】的本地音频缓存")
    }

    fun clearAllAudioCaches() {
        voiceManager.clearAllCaches()
        showMessage("已清空所有发音库的本地缓存文件夹")
    }

    fun startBatchAudioDownload(profileId: String, wordList: List<String>? = null) {
        val profile = voiceManager.voiceProfiles.value.find { it.id == profileId } ?: return
        val bookWords = wordsForActiveBook.value.map { it.word }
        val targetWords: List<String> = if (!wordList.isNullOrEmpty()) {
            wordList
        } else if (bookWords.isNotEmpty()) {
            bookWords
        } else {
            listOf("serendipity", "fascinating", "aesthetic", "ubiquitous", "ephemeral", "resilience")
        }

        if (targetWords.isEmpty()) {
            showMessage("当前词库暂无可下载单词")
            return
        }

        batchDownloadJob?.cancel()
        batchDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            val total = targetWords.size
            var success = 0
            var fail = 0
            val targetDir = voiceManager.getVoiceCacheDir(profile)

            voiceManager.updateDownloadProgress(
                profileId = profile.id,
                profileName = profile.name,
                isDownloading = true,
                currentWord = "准备开始快速连续下载...",
                completed = 0,
                total = total,
                success = 0,
                fail = 0,
                rateLimitTriggered = false,
                statusMessage = "高速连续下载中"
            )

            for ((index, w) in targetWords.withIndex()) {
                val cleanWord = w.trim().lowercase()
                if (cleanWord.isBlank()) continue

                val targetFile = java.io.File(targetDir, "$cleanWord.mp3")
                if (targetFile.exists() && targetFile.length() > 500) {
                    success++
                } else {
                    val downloaded = pronouncer.downloadWordAudioForProfile(cleanWord, targetFile, profile)
                    if (downloaded && targetFile.exists() && targetFile.length() > 500) {
                        success++
                    } else {
                        fail++
                    }
                }

                voiceManager.updateDownloadProgress(
                    profileId = profile.id,
                    profileName = profile.name,
                    isDownloading = true,
                    currentWord = cleanWord,
                    completed = index + 1,
                    total = total,
                    success = success,
                    fail = fail,
                    rateLimitTriggered = false,
                    statusMessage = "正在拉取: $cleanWord"
                )

                delay(10)
            }

            voiceManager.updateDownloadProgress(
                profileId = profile.id,
                profileName = profile.name,
                isDownloading = false,
                currentWord = "下载完成",
                completed = total,
                total = total,
                success = success,
                fail = fail,
                rateLimitTriggered = false,
                statusMessage = "离线完成"
            )
            showMessage("【${profile.name}】离线音频已就绪！成功: $success, 失败: $fail")
        }
    }

    fun cancelBatchAudioDownload() {
        batchDownloadJob?.cancel()
        batchDownloadJob = null
        voiceManager.resetDownloadProgress()
        showMessage("已停止离线音频批量下载")
    }

    fun lookupMdxDetail(word: String): ExtractedDictItem? {
        return mdxManager.lookup(word)
    }

    fun setLearningMode(mode: LearningMode) {
        _learningMode.value = mode
        showMessage("已切换学习模式：${mode.title}")
    }

    fun setLearningIntensity(intensity: LearningIntensity) {
        _learningIntensity.value = intensity
        showMessage("已调整学习强度：${intensity.title} (${intensity.subtitle})")
    }

    val targetSuccessCount: Int
        get() = if (_currentSessionType.value == SessionType.REVIEW) {
            2
        } else {
            when (_learningIntensity.value) {
                LearningIntensity.EASY -> 2
                LearningIntensity.MEDIUM -> 4
                LearningIntensity.STRONG -> maxOf(4, activeLearningModeSwitches.enabledCount)
            }
        }

    // Mode Switches (罗列7种模式开关，且至少开启一种模式)
    fun updateLearningModeSwitches(switches: LearningModeSwitches) {
        if (switches.enabledCount < 1) {
            showMessage("至少需要保留开启一种学习模式！")
            return
        }
        _learningModeSwitches.value = switches
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("learn_mode_blind_listen_meaning", switches.enableBlindListenMeaning)
            .putBoolean("learn_mode_look_choose_meaning", switches.enableLookChooseMeaning)
            .putBoolean("learn_mode_recall_by_meaning", switches.enableRecallByMeaning)
            .putBoolean("learn_mode_listen_choose_word", switches.enableListenChooseWord)
            .putBoolean("learn_mode_listen_sentence_choose_word", switches.enableListenSentenceChooseWord)
            .putBoolean("learn_mode_english_meaning_choose_word", switches.enableEnglishMeaningChooseWord)
            .putBoolean("learn_mode_blind_spelling", switches.enableBlindSpelling)
            .apply()
        showMessage("已更新学习模式功能开关配置 (${switches.enabledCount}/7 开启)")

        // 立即同步当前队列中的题目类型，保证单独开启某模式（如听句选词）立即进入对应界面
        val candidateStages = getCandidateStages(switches)
        val currentList = _sessionItems.value
        if (currentList.isNotEmpty()) {
            val updated = currentList.map { item ->
                if (item.stage !in candidateStages &&
                    item.stage != LearnItemStage.CHOOSE_MEANING_EXPLAIN &&
                    item.stage != LearnItemStage.FIRST_LISTEN_EXPLAIN
                ) {
                    val targetStage = candidateStages.first()
                    val choices = when (targetStage) {
                        LearnItemStage.BLIND_LISTEN_MEANING,
                        LearnItemStage.LISTEN_CHOOSE_MEANING,
                        LearnItemStage.LOOK_CHOOSE_MEANING -> generateChoiceOptionsForMeaning(item.word)
                        LearnItemStage.LISTEN_CHOOSE_WORD,
                        LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
                        LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
                        LearnItemStage.CLOZE_SENTENCE -> generateChoiceOptionsForWord(item.word)
                        else -> emptyList()
                    }
                    item.copy(
                        stage = targetStage,
                        multipleChoiceOptions = choices,
                        selectedOptionIndex = null,
                        isAnswerCorrect = null,
                        flashingOptionIndex = null,
                        flashingIsCorrect = true,
                        isHintClicked = false,
                        isWordRevealed = false,
                        typedSpelling = "",
                        showCorrectSpelling = false,
                        spellingSuccess = false,
                        remainingSpellingAttempts = 3,
                        countdownSecondsLeft = 5
                    )
                } else item
            }
            _sessionItems.value = updated
            playCurrentLearnItemAudio()
        }
    }

    fun updateReviewLearningModeSwitches(switches: LearningModeSwitches) {
        if (switches.enabledCount < 1) {
            showMessage("复习模式至少需要保留开启一种学习方式！")
            return
        }
        _reviewLearningModeSwitches.value = switches
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("review_mode_blind_listen_meaning", switches.enableBlindListenMeaning)
            .putBoolean("review_mode_look_choose_meaning", switches.enableLookChooseMeaning)
            .putBoolean("review_mode_recall_by_meaning", switches.enableRecallByMeaning)
            .putBoolean("review_mode_listen_choose_word", switches.enableListenChooseWord)
            .putBoolean("review_mode_listen_sentence_choose_word", switches.enableListenSentenceChooseWord)
            .putBoolean("review_mode_english_meaning_choose_word", switches.enableEnglishMeaningChooseWord)
            .putBoolean("review_mode_blind_spelling", switches.enableBlindSpelling)
            .apply()
        showMessage("已更新复习模式功能开关 (${switches.enabledCount}/7 开启)")

        // 立即同步当前队列中的题目类型
        val candidateStages = getCandidateStages(switches)
        val currentList = _sessionItems.value
        if (currentList.isNotEmpty()) {
            val updated = currentList.map { item ->
                if (item.stage !in candidateStages &&
                    item.stage != LearnItemStage.CHOOSE_MEANING_EXPLAIN &&
                    item.stage != LearnItemStage.FIRST_LISTEN_EXPLAIN
                ) {
                    val targetStage = candidateStages.first()
                    val choices = when (targetStage) {
                        LearnItemStage.BLIND_LISTEN_MEANING,
                        LearnItemStage.LISTEN_CHOOSE_MEANING,
                        LearnItemStage.LOOK_CHOOSE_MEANING -> generateChoiceOptionsForMeaning(item.word)
                        LearnItemStage.LISTEN_CHOOSE_WORD,
                        LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
                        LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
                        LearnItemStage.CLOZE_SENTENCE -> generateChoiceOptionsForWord(item.word)
                        else -> emptyList()
                    }
                    item.copy(
                        stage = targetStage,
                        multipleChoiceOptions = choices,
                        selectedOptionIndex = null,
                        isAnswerCorrect = null,
                        flashingOptionIndex = null,
                        flashingIsCorrect = true,
                        isHintClicked = false,
                        isWordRevealed = false,
                        typedSpelling = "",
                        showCorrectSpelling = false,
                        spellingSuccess = false,
                        remainingSpellingAttempts = 3,
                        countdownSecondsLeft = 5
                    )
                } else item
            }
            _sessionItems.value = updated
            playCurrentLearnItemAudio()
        }
    }

    fun setFirstListenRepeatTimes(times: Int) {
        _firstListenRepeatTimes.value = if (times == 3) 3 else 1
        showMessage("已设置首次发音朗读 ${_firstListenRepeatTimes.value} 遍")
    }

    fun setFirstListenVoicePref(pref: FirstListenVoicePreference) {
        _firstListenVoicePref.value = pref
        showMessage("已设置盲听发音偏好：${pref.title}")
    }

    // Portable Player Settings
    fun setPortableAudioMode(mode: PortableAudioMode) {
        _portableAudioMode.value = mode
        showMessage("已设置随身听模式：${mode.title}")
    }

    fun togglePortableContentItem(item: PortableContentItem) {
        val current = _portableSelectedContentItems.value.toMutableSet()
        if (current.contains(item)) {
            current.remove(item)
        } else {
            current.add(item)
        }
        _portableSelectedContentItems.value = current
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("portable_selected_content_items", current.joinToString(",") { it.id }).apply()
    }

    fun setPortableSelectedContentItems(items: Set<PortableContentItem>) {
        _portableSelectedContentItems.value = items
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("portable_selected_content_items", items.joinToString(",") { it.id }).apply()
    }

    fun updatePortableVoiceSelection(selection: PortableVoiceSelection) {
        _portableVoiceSelection.value = selection
    }

    fun setPortableRepeatTimes(times: Int) {
        _portableRepeatTimes.value = times
    }

    fun setPortableSentenceRepeatTimes(times: Int) {
        _portableSentenceRepeatTimes.value = times
    }

    fun togglePortableRandomTTS() {
        _isPortableRandomTTS.value = !_isPortableRandomTTS.value
    }

    fun setPortableRandomTTS(enabled: Boolean) {
        _isPortableRandomTTS.value = enabled
    }

    fun setPortablePauseSeconds(seconds: Float) {
        _portablePauseSeconds.value = seconds
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putFloat("portable_pause_seconds", seconds).apply()
    }

    // Helpers to generate distractors for multiple-choice modes
    private fun generateChoiceOptionsForMeaning(target: WordEntity): List<String> {
        val pool = wordsForActiveBook.value.filter { it.word != target.word && it.meaning.isNotBlank() }
        val distractors = pool.shuffled().take(3).map { it.meaning }.toMutableList()
        val defaultDistractors = listOf("adj. 基础的；初级的", "n. 机会；转折点", "v. 探索；发现", "adv. 极其；非常")
        var i = 0
        while (distractors.size < 3 && i < defaultDistractors.size) {
            val d = defaultDistractors[i++]
            if (!distractors.contains(d) && d != target.meaning) {
                distractors.add(d)
            }
        }
        return (distractors + target.meaning).shuffled()
    }

    private fun generateChoiceOptionsForWord(target: WordEntity): List<String> {
        val pool = wordsForActiveBook.value.filter { it.word != target.word }
        val distractors = pool.shuffled().take(3).map { it.word }.toMutableList()
        val defaultDistractors = listOf("accomplish", "perceive", "venture", "dilemma")
        var i = 0
        while (distractors.size < 3 && i < defaultDistractors.size) {
            val d = defaultDistractors[i++]
            if (!distractors.contains(d) && d != target.word) {
                distractors.add(d)
            }
        }
        return (distractors + target.word).shuffled()
    }

    // Start Learning Session (默认为当前任务组所有待学新词)
    fun startLearning(limit: Int = 20) {
        val groups = fixedWordGroups.value
        val taskGroups = groups.filter { it.isTaskGroup }
        if (taskGroups.isNotEmpty()) {
            val wordsToLearn = taskGroups.flatMap { it.words.filter { w -> w.reviewStage == 0 && !w.isMastered } }
            if (wordsToLearn.isNotEmpty()) {
                val groupIndicesStr = taskGroups.joinToString(",") { "List ${it.groupIndex}" }
                startLearningWithWords(wordsToLearn, "【$groupIndicesStr】今日任务")
                return
            } else {
                // If all unlearned in task groups are learned but we still have words
                val fallbackWords = taskGroups.flatMap { it.words }.filter { !it.isMastered }
                if (fallbackWords.isNotEmpty()) {
                    val groupIndicesStr = taskGroups.joinToString(",") { "List ${it.groupIndex}" }
                    startLearningWithWords(fallbackWords, "【$groupIndicesStr】任务组")
                    return
                }
            }
        }
        val taskGroup = groups.firstOrNull { it.unlearnedCount > 0 } ?: groups.firstOrNull()
        if (taskGroup != null && taskGroup.words.isNotEmpty()) {
            startLearningGroup(taskGroup)
        } else {
            viewModelScope.launch {
                val words = repository.getNewWordsForBook(_activeBookId.value, limit)
                val selectedWords = if (words.isEmpty()) {
                    val fallbackWords = wordsForActiveBook.value.filter { !it.isMastered }.take(limit)
                    if (fallbackWords.isEmpty()) {
                        _userMessage.value = "当前单词书单词已全部学完或标熟！可以导入新单词书或进行复习。"
                        return@launch
                    }
                    fallbackWords
                } else {
                    words
                }
                startLearningWithWords(selectedWords, "日常任务")
            }
        }
    }

    fun startNextGroupLearning() {
        val groups = fixedWordGroups.value
        val nextGroup = groups.firstOrNull { it.unlearnedCount > 0 }
        if (nextGroup != null) {
            startLearningGroup(nextGroup)
        } else {
            val remainingNewWords = wordsForActiveBook.value.filter { it.reviewStage == 0 && !it.isMastered }
            if (remainingNewWords.isNotEmpty()) {
                startLearningWithWords(remainingNewWords.take(15), "下一组新词")
            } else {
                showMessage("当前单词书的新词已全部学完！可以开启复习或学习新书。")
            }
        }
    }
    // 根据艾宾浩斯学习指定的固定词组 (List X)
    fun startLearningGroup(group: WordFixedGroup) {
        val wordsToLearn = group.words.filter { it.reviewStage == 0 }.ifEmpty { group.words }
        if (wordsToLearn.isEmpty()) {
            showMessage("该组已全部标熟！")
            return
        }
        startLearningWithWords(wordsToLearn, "【List ${group.groupIndex} / ${group.tag}】任务组")
    }

    // 根据艾宾浩斯复习指定的固定词组 (List X)
    fun startReviewGroup(group: WordFixedGroup) {
        val now = System.currentTimeMillis()
        val dueWords = group.words.filter { it.nextReviewTime in 1..now }
        val wordsToReview = if (dueWords.isNotEmpty()) dueWords else group.words.filter { it.reviewStage > 0 }.ifEmpty { group.words }
        if (wordsToReview.isEmpty()) {
            showMessage("该组暂无需要复习的单词！")
            return
        }
        viewModelScope.launch {
            startReviewWithWords(wordsToReview, "【List ${group.groupIndex} / ${group.tag}】艾宾浩斯复习")
            showMessage("开始【List ${group.groupIndex} / ${group.tag}】艾宾浩斯复习 (共${wordsToReview.size}词)")
        }
    }

    fun startLearnGroupByNumber(groupIndex: Int) {
        val group = fixedWordGroups.value.find { it.groupIndex == groupIndex }
        if (group != null) {
            startLearningGroup(group)
        } else {
            showMessage("未找到第 $groupIndex 组")
        }
    }

    fun startReviewGroupByNumber(groupIndex: Int) {
        val group = fixedWordGroups.value.find { it.groupIndex == groupIndex }
        if (group != null) {
            startReviewGroup(group)
        } else {
            showMessage("未找到第 $groupIndex 组")
        }
    }

    fun playGroupInPortablePlayer(group: WordFixedGroup) {
        playFixedGroupInPortablePlayer(group)
    }

    fun getGroupPlanStatus(group: WordFixedGroup): GroupPlanStatus {
        return if (group.unlearnedCount == 0 && group.words.isNotEmpty()) {
            GroupPlanStatus.LEARNED
        } else if (group.isTaskGroup || (group.learnedCount > 0 && group.unlearnedCount > 0)) {
            GroupPlanStatus.IN_PROGRESS
        } else {
            GroupPlanStatus.UNLEARNED
        }
    }

    private var allPendingSessionWords = listOf<WordEntity>()
    private var currentSessionLabel = ""

    fun getEnglishDefinitions(word: WordEntity): List<String> {
        if (word.definition.isNotBlank()) {
            val defs = word.definition.split(Regex("[\\n;]"))
                .map { it.trim() }
                .map { it.replace(Regex("^([\\(（]?[0-9]+[\\)）\\.\\、\\s]*)+"), "").trim() }
                .filter { it.isNotBlank() && !it.contains(Regex("[\\u4e00-\\u9fa5]")) }
            if (defs.isNotEmpty()) {
                return defs.take(3)
            }
        }
        val posLabel = if (word.pos.isNotBlank()) "[${word.pos}]" else ""
        return listOf("The word meaning '$posLabel ${word.meaning}'")
    }

    private fun loadNextBatchOfSessionWords() {
        val batchWords = allPendingSessionWords.take(5)
        allPendingSessionWords = allPendingSessionWords.drop(5)
        
        if (batchWords.isEmpty()) {
            _isLearningFinished.value = true
            return
        }
        
        val switches = activeLearningModeSwitches
        val initialStage = getInitialStageForSwitches(switches)
        val items = batchWords.map { word ->
            val choices = when (initialStage) {
                LearnItemStage.BLIND_LISTEN_MEANING,
                LearnItemStage.LISTEN_CHOOSE_MEANING,
                LearnItemStage.LOOK_CHOOSE_MEANING -> generateChoiceOptionsForMeaning(word)
                LearnItemStage.LISTEN_CHOOSE_WORD,
                LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
                LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
                LearnItemStage.CLOZE_SENTENCE -> generateChoiceOptionsForWord(word)
                else -> emptyList()
            }
            LearnSessionItem(
                word = word,
                stage = initialStage,
                recognizedSuccessCount = 0,
                failOrHintCount = 0,
                totalAppearCount = 1,
                multipleChoiceOptions = choices
            )
        }
        
        _sessionItems.value = items
        _currentSessionIndex.value = 0
        playCurrentLearnItemAudio()
    }

    private fun checkSessionFinishedOrLoadNext(list: MutableList<LearnSessionItem>) {
        if (list.isEmpty()) {
            if (allPendingSessionWords.isNotEmpty()) {
                loadNextBatchOfSessionWords()
            } else {
                _isLearningFinished.value = true
            }
        } else {
            if (_currentSessionIndex.value >= list.size) {
                _currentSessionIndex.value = 0
            }
            playCurrentLearnItemAudio()
        }
    }

    private fun getCandidateStages(switches: LearningModeSwitches): List<LearnItemStage> {
        val list = mutableListOf<LearnItemStage>()
        if (switches.enableBlindListenMeaning) list.add(LearnItemStage.BLIND_LISTEN_MEANING)
        if (switches.enableLookChooseMeaning) list.add(LearnItemStage.LOOK_CHOOSE_MEANING)
        if (switches.enableRecallByMeaning) list.add(LearnItemStage.RECALL_BY_MEANING)
        if (switches.enableListenChooseWord) list.add(LearnItemStage.LISTEN_CHOOSE_WORD)
        if (switches.enableListenSentenceChooseWord) list.add(LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD)
        if (switches.enableEnglishMeaningChooseWord) list.add(LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD)
        if (switches.enableBlindSpelling) list.add(LearnItemStage.BLIND_SPELLING)
        if (list.isEmpty()) list.add(LearnItemStage.BLIND_LISTEN_MEANING)
        return list
    }

    private fun getInitialStageForSwitches(switches: LearningModeSwitches): LearnItemStage {
        return when {
            switches.enableBlindListenMeaning -> LearnItemStage.BLIND_LISTEN_MEANING
            switches.enableLookChooseMeaning -> LearnItemStage.LOOK_CHOOSE_MEANING
            switches.enableRecallByMeaning -> LearnItemStage.RECALL_BY_MEANING
            switches.enableListenChooseWord -> LearnItemStage.LISTEN_CHOOSE_WORD
            switches.enableListenSentenceChooseWord -> LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD
            switches.enableEnglishMeaningChooseWord -> LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD
            switches.enableBlindSpelling -> LearnItemStage.BLIND_SPELLING
            else -> LearnItemStage.BLIND_LISTEN_MEANING
        }
    }

    fun startLearningWithWords(words: List<WordEntity>, sessionLabel: String = "") {
        viewModelScope.launch {
            val selectedWords = words.filter { !it.isMastered }
            if (selectedWords.isEmpty()) {
                _userMessage.value = "所选单词已全部标熟！"
                return@launch
            }
            
            _currentSessionType.value = SessionType.LEARN
            allPendingSessionWords = selectedWords
            currentSessionLabel = sessionLabel

            _isLearningFinished.value = false
            _sessionPassedCount.value = 0
            _sessionMasteredCount.value = 0
            _sessionReinforcedCount.value = 0
            _currentScreen.value = AppScreen.LEARN

            loadNextBatchOfSessionWords()
        }
    }

    fun startReviewWithWords(words: List<WordEntity>, sessionLabel: String = "【艾宾浩斯复习】") {
        viewModelScope.launch {
            val selectedWords = words.filter { !it.isMastered }
            if (selectedWords.isEmpty()) {
                _userMessage.value = "所选单词已全部标熟！"
                return@launch
            }
            
            _currentSessionType.value = SessionType.REVIEW
            allPendingSessionWords = selectedWords
            currentSessionLabel = sessionLabel

            _isLearningFinished.value = false
            _sessionPassedCount.value = 0
            _sessionMasteredCount.value = 0
            _sessionReinforcedCount.value = 0
            _currentScreen.value = AppScreen.LEARN

            loadNextBatchOfSessionWords()
        }
    }

    fun playCurrentLearnItemAudio() {
        countdownJob?.cancel()
        val list = _sessionItems.value
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        // 详情解释界面有专属的 ExplanationCard 自带 LaunchedEffect 处理单词+例句发音，在此不作多余播放
        if (currentItem.stage == LearnItemStage.CHOOSE_MEANING_EXPLAIN || currentItem.stage == LearnItemStage.FIRST_LISTEN_EXPLAIN) {
            _isWordAudioPlaying.value = false
            _isWordRevealedAfterAudio.value = true
            return
        }

        _isWordAudioPlaying.value = true
        _isWordRevealedAfterAudio.value = false
        _currentAudioListenCount.value = 1

        val voiceProfileId = when (_firstListenVoicePref.value) {
            FirstListenVoicePreference.DEFAULT -> null
            FirstListenVoicePreference.US -> getPreferredProfileIdForAccent(VoiceAccent.US)
            FirstListenVoicePreference.UK -> getPreferredProfileIdForAccent(VoiceAccent.UK)
            FirstListenVoicePreference.ALTERNATING -> {
                val enabledProfiles = voiceProfiles.value.filter { it.isEnabled }
                if (enabledProfiles.isNotEmpty()) {
                    enabledProfiles[index % enabledProfiles.size].id
                } else null
            }
        }

        if (currentItem.stage == LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD || currentItem.stage == LearnItemStage.CLOZE_SENTENCE) {
            _isWordAudioPlaying.value = true
            _isWordRevealedAfterAudio.value = false
            if (currentItem.word.exampleSentence.isNotBlank()) {
                pronounceSentence(currentItem.word.exampleSentence, SentenceVoiceType.TTS_1)
            } else {
                pronounceWord(currentItem.word.word)
            }
            return
        }

        if (currentItem.stage == LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD) {
            _isWordAudioPlaying.value = true
            _isWordRevealedAfterAudio.value = false
            val defs = getEnglishDefinitions(currentItem.word)
            if (defs.isNotEmpty()) {
                pronounceSentence(defs.first(), SentenceVoiceType.TTS_1)
            } else {
                pronounceWord(currentItem.word.word)
            }
            return
        }

        if (currentItem.stage == LearnItemStage.RECALL_BY_MEANING) {
            // Start 3-second countdown for "看义说词"
            _isWordAudioPlaying.value = false
            _isWordRevealedAfterAudio.value = false
            startRecallCountdown()
            return
        }

        if (multiVoiceSequentialPlay.value) {
            val enabledProfiles = voiceManager.getEnabledProfiles()
            if (enabledProfiles.size > 1) {
                pronouncer.speakSequential(
                    word = currentItem.word.word,
                    profiles = enabledProfiles,
                    onCompletion = {
                        _isWordAudioPlaying.value = false
                        _isWordRevealedAfterAudio.value = true
                    }
                )
                return
            }
        }

        pronouncer.speakWordTimes(
            text = currentItem.word.word,
            times = _firstListenRepeatTimes.value,
            profileId = voiceProfileId,
            onEachStart = { count ->
                _currentAudioListenCount.value = count
            },
            onCompletion = {
                _isWordAudioPlaying.value = false
                _isWordRevealedAfterAudio.value = true
            }
        )
    }

    private fun startRecallCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (sec in 3 downTo 1) {
                updateItemCountdown(sec)
                delay(1000)
            }
            updateItemCountdown(0)
            _isWordRevealedAfterAudio.value = true
            // Play pronunciation when revealed
            val list = _sessionItems.value
            val index = _currentSessionIndex.value
            if (index < list.size) {
                pronounceWord(list[index].word.word)
            }
        }
    }

    private fun updateItemCountdown(sec: Int) {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index < list.size) {
            list[index] = list[index].copy(countdownSecondsLeft = sec)
            _sessionItems.value = list
        }
    }

    fun forceRevealSpelling() {
        countdownJob?.cancel()
        _isWordRevealedAfterAudio.value = true
    }

    // 1. Action: 熟悉 (标熟 / Mastered directly)
    fun onActionMarkFamiliar() {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val item = list[index]

        viewModelScope.launch {
            repository.markMastered(item.word, true)
            allPendingSessionWords = allPendingSessionWords.filter { it.id != item.word.id && it.word != item.word.word }
            _sessionMasteredCount.value += 1
            // 点击熟悉按钮后不弹出提示

            val updatedList = list.filter { it.word.id != item.word.id && it.word.word != item.word.word }.toMutableList()
            _sessionItems.value = updatedList
            checkSessionFinishedOrLoadNext(updatedList)
            refreshCurrentTime()
        }
    }

    // 2. Action: 认识 (Know)
    fun onActionKnow() {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        val updatedSuccessCount = currentItem.recognizedSuccessCount + 1

        if (updatedSuccessCount >= 2) {
            // Reached 2 recognitions: Session completed for this word!
            viewModelScope.launch {
                repository.recordReview(currentItem.word, ReviewQuality.KNOW)
                _sessionPassedCount.value += 1
                list.removeAt(index)
                _sessionItems.value = list
                checkSessionFinishedOrLoadNext(list)
                refreshCurrentTime()
            }
        } else {
            // First recognition: choose secondary confirmation mode based on switches
            val switches = activeLearningModeSwitches
            val candidateStages = getCandidateStages(switches).filter { it != currentItem.stage }.ifEmpty { getCandidateStages(switches) }
            val nextStage = candidateStages.randomOrNull() ?: LearnItemStage.BLIND_LISTEN_MEANING

            val choices = when (nextStage) {
                LearnItemStage.BLIND_LISTEN_MEANING,
                LearnItemStage.LISTEN_CHOOSE_MEANING,
                LearnItemStage.LOOK_CHOOSE_MEANING -> generateChoiceOptionsForMeaning(currentItem.word)
                LearnItemStage.LISTEN_CHOOSE_WORD,
                LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
                LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
                LearnItemStage.CLOZE_SENTENCE -> generateChoiceOptionsForWord(currentItem.word)
                else -> emptyList()
            }

            val confirmationItem = currentItem.copy(
                stage = nextStage,
                recognizedSuccessCount = 1,
                totalAppearCount = currentItem.totalAppearCount + 1,
                multipleChoiceOptions = choices,
                selectedOptionIndex = null,
                isAnswerCorrect = null,
                isHintClicked = false,
                isWordRevealed = false,
                flashingOptionIndex = null
            )
            list.removeAt(index)
            list.add(confirmationItem)
            _sessionItems.value = list

            if (_currentSessionIndex.value >= list.size) {
                _currentSessionIndex.value = 0
            }
            playCurrentLearnItemAudio()
        }
    }

    // 3. Action: 模糊 (Fuzzy)
    fun onActionFuzzy() {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        _sessionReinforcedCount.value += 1
        val updatedItem = currentItem.copy(stage = LearnItemStage.FIRST_LISTEN_EXPLAIN)
        list[index] = updatedItem
        _sessionItems.value = list
    }

    // 4. Action: 提示 (Hint / Forgot)
    fun onActionHint() {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        _sessionReinforcedCount.value += 1
        val updatedItem = currentItem.copy(
            stage = LearnItemStage.FIRST_LISTEN_EXPLAIN,
            failOrHintCount = currentItem.failOrHintCount + 1
        )
        list[index] = updatedItem
        _sessionItems.value = list
    }

    // Continue after reading explanation
    fun onContinueFromExplanation() {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        list.removeAt(index)

        // 若当前题是答对后进入详情页的：
        if (currentItem.isAnswerCorrect == true) {
            if (currentItem.recognizedSuccessCount >= targetSuccessCount) {
                // 已达成两次正确，记为掌握并通关
                viewModelScope.launch {
                    repository.recordReview(currentItem.word, ReviewQuality.KNOW)
                }
                _sessionPassedCount.value += 1
                _sessionItems.value = list
                checkSessionFinishedOrLoadNext(list)
                refreshCurrentTime()
                return
            } else {
                // 答对1次，排到队列中等待后续轮次强化验证
                val switches = activeLearningModeSwitches
                val candidateStages = getCandidateStages(switches)
                val nextStage: LearnItemStage = candidateStages.randomOrNull() ?: currentItem.stage
                val choices = when (nextStage) {
                    LearnItemStage.BLIND_LISTEN_MEANING,
                    LearnItemStage.LISTEN_CHOOSE_MEANING,
                    LearnItemStage.LOOK_CHOOSE_MEANING -> generateChoiceOptionsForMeaning(currentItem.word)
                    LearnItemStage.LISTEN_CHOOSE_WORD,
                    LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
                    LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
                    LearnItemStage.CLOZE_SENTENCE -> generateChoiceOptionsForWord(currentItem.word)
                    else -> emptyList()
                }

                val cleanRescheduledItem = currentItem.copy(
                    stage = nextStage,
                    recognizedSuccessCount = currentItem.recognizedSuccessCount,
                    selectedOptionIndex = null,
                    isAnswerCorrect = null,
                    flashingOptionIndex = null,
                    flashingIsCorrect = true,
                    isHintClicked = false,
                    isWordRevealed = false,
                    typedSpelling = "",
                    showCorrectSpelling = false,
                    spellingSuccess = false,
                    remainingSpellingAttempts = 3,
                    countdownSecondsLeft = 5,
                    multipleChoiceOptions = choices,
                    totalAppearCount = currentItem.totalAppearCount + 1
                )
                list.add(cleanRescheduledItem)
                _sessionItems.value = list
                if (_currentSessionIndex.value >= list.size) {
                    _currentSessionIndex.value = 0
                }
                playCurrentLearnItemAudio()
                return
            }
        }

        val switches = activeLearningModeSwitches
        val candidateStages = getCandidateStages(switches)
        val nextStage: LearnItemStage = candidateStages.randomOrNull() ?: LearnItemStage.BLIND_LISTEN_MEANING

        val choices = when (nextStage) {
            LearnItemStage.BLIND_LISTEN_MEANING,
            LearnItemStage.LISTEN_CHOOSE_MEANING,
            LearnItemStage.LOOK_CHOOSE_MEANING -> generateChoiceOptionsForMeaning(currentItem.word)
            LearnItemStage.LISTEN_CHOOSE_WORD,
            LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
            LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
            LearnItemStage.CLOZE_SENTENCE -> generateChoiceOptionsForWord(currentItem.word)
            else -> emptyList()
        }

        val rescheduledItem = currentItem.copy(
            stage = nextStage,
            totalAppearCount = currentItem.totalAppearCount + 1,
            multipleChoiceOptions = choices,
            selectedOptionIndex = null,
            isAnswerCorrect = null,
            isHintClicked = false,
            isWordRevealed = false,
            flashingOptionIndex = null,
            typedSpelling = "",
            countdownSecondsLeft = 5
        )

        // Reschedule offset: 3 for fuzzy, 4 for hint
        val offset = if (currentItem.failOrHintCount > 0) 4 else 3
        val targetIndex = (index + offset).coerceAtMost(list.size)
        list.add(targetIndex, rescheduledItem)

        _sessionItems.value = list
        if (_currentSessionIndex.value >= list.size) {
            _currentSessionIndex.value = 0
        }
        playCurrentLearnItemAudio()
    }

    // Hint & See Answer interaction in choice-based modes
    fun onHintClicked() {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        list[index] = list[index].copy(
            isHintClicked = true,
            isWordRevealed = true
        )
        _sessionItems.value = list
    }

    fun onSeeAnswerClicked() {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        val correctIndex = when (currentItem.stage) {
            LearnItemStage.BLIND_LISTEN_MEANING,
            LearnItemStage.LISTEN_CHOOSE_MEANING,
            LearnItemStage.LOOK_CHOOSE_MEANING -> {
                currentItem.multipleChoiceOptions.indexOf(currentItem.word.meaning)
            }
            LearnItemStage.LISTEN_CHOOSE_WORD,
            LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
            LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
            LearnItemStage.CLOZE_SENTENCE -> {
                currentItem.multipleChoiceOptions.indexOfFirst { it.equals(currentItem.word.word, ignoreCase = true) }
            }
            else -> currentItem.multipleChoiceOptions.indexOf(currentItem.word.meaning)
        }.let { if (it >= 0) it else 0 }

        viewModelScope.launch {
            // 点击看答案时，正确选项高亮绿框提示 0.1 秒，然后平滑进入详情
            val l1 = _sessionItems.value.toMutableList()
            if (index < l1.size) {
                l1[index] = l1[index].copy(
                    flashingOptionIndex = correctIndex,
                    flashingIsCorrect = true
                )
                _sessionItems.value = l1
            }
            delay(100L)

            val curList = _sessionItems.value.toMutableList()
            if (index < curList.size) {
                _sessionReinforcedCount.value += 1
                val updatedItem = curList[index].copy(
                    stage = LearnItemStage.CHOOSE_MEANING_EXPLAIN,
                    failOrHintCount = curList[index].failOrHintCount + 1,
                    selectedOptionIndex = null,
                    flashingOptionIndex = null,
                    isAnswerCorrect = false
                )
                curList[index] = updatedItem
                _sessionItems.value = curList
                repository.recordReview(currentItem.word, ReviewQuality.FORGOT)
            }
        }
    }

    fun onDontKnowClicked() {
        stopPronunciation()
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        _sessionReinforcedCount.value += 1
        val updatedItem = currentItem.copy(
            stage = LearnItemStage.CHOOSE_MEANING_EXPLAIN,
            failOrHintCount = currentItem.failOrHintCount + 1,
            selectedOptionIndex = null,
            flashingOptionIndex = null,
            isAnswerCorrect = false
        )
        list[index] = updatedItem
        _sessionItems.value = list
        viewModelScope.launch {
            repository.recordReview(currentItem.word, ReviewQuality.FORGOT)
        }
    }

    // Select option in 听音选义, 听音选词, or 例句完形
    fun onSelectChoiceOption(optionIndex: Int) {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        val selectedText = currentItem.multipleChoiceOptions.getOrNull(optionIndex) ?: return
        val isCorrect = when (currentItem.stage) {
            LearnItemStage.BLIND_LISTEN_MEANING,
            LearnItemStage.LISTEN_CHOOSE_MEANING,
            LearnItemStage.LOOK_CHOOSE_MEANING -> selectedText == currentItem.word.meaning
            LearnItemStage.LISTEN_CHOOSE_WORD,
            LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
            LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
            LearnItemStage.CLOZE_SENTENCE -> selectedText.equals(currentItem.word.word, ignoreCase = true)
            else -> selectedText == currentItem.word.meaning
        }

        if (isCorrect) {
            val updatedSuccessCount = currentItem.recognizedSuccessCount + 1
            val isMeaningStage = currentItem.stage == LearnItemStage.BLIND_LISTEN_MEANING ||
                    currentItem.stage == LearnItemStage.LISTEN_CHOOSE_MEANING ||
                    currentItem.stage == LearnItemStage.LOOK_CHOOSE_MEANING
            val autoAdvance = activeAutoAdvanceOnCorrect
            if (autoAdvance || !isMeaningStage) {
                // 开启答对自动进入下一个（复习模式默认开启），或者其他非释义模式下答对后自动进入下一个
                if (updatedSuccessCount >= targetSuccessCount) {
                    viewModelScope.launch {
                        val l1 = _sessionItems.value.toMutableList()
                        if (index < l1.size) {
                            l1[index] = l1[index].copy(selectedOptionIndex = optionIndex, isAnswerCorrect = true)
                            _sessionItems.value = l1
                        }

                        // 答对不等待立即进入
                        repository.recordReview(currentItem.word, ReviewQuality.KNOW)
                        _sessionPassedCount.value += 1
                        val l2 = _sessionItems.value.toMutableList()
                        if (index < l2.size) {
                            l2.removeAt(index)
                            _sessionItems.value = l2
                        }
                        checkSessionFinishedOrLoadNext(l2)
                        refreshCurrentTime()
                    }
                } else {
                    viewModelScope.launch {
                        val l1 = _sessionItems.value.toMutableList()
                        if (index < l1.size) {
                            l1[index] = l1[index].copy(selectedOptionIndex = optionIndex, isAnswerCorrect = true)
                            _sessionItems.value = l1
                        }

                        // 答对不等待立即进入
                        val switches = activeLearningModeSwitches
                        val candidateStages = getCandidateStages(switches)
                        val nextStage = candidateStages.randomOrNull() ?: currentItem.stage
                        val choices = when (nextStage) {
                            LearnItemStage.BLIND_LISTEN_MEANING,
                            LearnItemStage.LISTEN_CHOOSE_MEANING,
                            LearnItemStage.LOOK_CHOOSE_MEANING -> generateChoiceOptionsForMeaning(currentItem.word)
                            LearnItemStage.LISTEN_CHOOSE_WORD,
                            LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
                            LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
                            LearnItemStage.CLOZE_SENTENCE -> generateChoiceOptionsForWord(currentItem.word)
                            else -> emptyList()
                        }

                        val cleanRescheduledItem = currentItem.copy(
                            stage = nextStage,
                            recognizedSuccessCount = updatedSuccessCount,
                            selectedOptionIndex = null,
                            isAnswerCorrect = null,
                            flashingOptionIndex = null,
                            flashingIsCorrect = true,
                            isHintClicked = false,
                            isWordRevealed = false,
                            typedSpelling = "",
                            showCorrectSpelling = false,
                            spellingSuccess = false,
                            remainingSpellingAttempts = 3,
                            countdownSecondsLeft = 5,
                            multipleChoiceOptions = choices,
                            totalAppearCount = currentItem.totalAppearCount + 1
                        )

                        val curList = _sessionItems.value.toMutableList()
                        if (index < curList.size) {
                            curList.removeAt(index)
                            curList.add(cleanRescheduledItem)
                            _sessionItems.value = curList
                            if (_currentSessionIndex.value >= curList.size) {
                                _currentSessionIndex.value = 0
                            }
                            playCurrentLearnItemAudio()
                        }
                    }
                }
            } else {
                // 盲听选义和看词选义在答对后跳转到单词详情页
                viewModelScope.launch {
                    val l1 = _sessionItems.value.toMutableList()
                    if (index < l1.size) {
                        l1[index] = l1[index].copy(
                            selectedOptionIndex = optionIndex,
                            isAnswerCorrect = true,
                            recognizedSuccessCount = updatedSuccessCount
                        )
                        _sessionItems.value = l1
                    }

                    // 答对不等待立即进入
                    val curList = _sessionItems.value.toMutableList()
                    if (index < curList.size) {
                        curList[index] = curList[index].copy(
                            stage = LearnItemStage.CHOOSE_MEANING_EXPLAIN,
                            selectedOptionIndex = null,
                            flashingOptionIndex = null,
                            isAnswerCorrect = true,
                            recognizedSuccessCount = updatedSuccessCount
                        )
                        _sessionItems.value = curList
                    }
                }
            }
        } else {
            viewModelScope.launch {
                val l0 = _sessionItems.value.toMutableList()
                if (index < l0.size) {
                    l0[index] = l0[index].copy(selectedOptionIndex = optionIndex, isAnswerCorrect = false)
                    _sessionItems.value = l0
                }

                delay(100L) // 答错 0.1 秒后进入详情

                // 闪烁或播放完成后，自动进入释义例句详情
                val curList = _sessionItems.value.toMutableList()
                if (index < curList.size) {
                    _sessionReinforcedCount.value += 1
                    val updatedItem = curList[index].copy(
                        stage = LearnItemStage.CHOOSE_MEANING_EXPLAIN,
                        failOrHintCount = curList[index].failOrHintCount + 1,
                        selectedOptionIndex = null,
                        isAnswerCorrect = false,
                        flashingOptionIndex = null
                    )
                    curList[index] = updatedItem
                    _sessionItems.value = curList
                    repository.recordReview(currentItem.word, ReviewQuality.FORGOT)
                }
            }
        }
    }

    // Dictation Spelling (Type/Click letter)
    fun onTypeSpellingLetter(char: Char) {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        // 若正展示错误提示/正确拼法，用户再次开始拼写时自动隐藏正确拼法并从头输入
        spellingHideJob?.cancel()
        val newTyped = (if (currentItem.showCorrectSpelling) "" else currentItem.typedSpelling) + char
        list[index] = currentItem.copy(
            typedSpelling = newTyped,
            showCorrectSpelling = false
        )
        _sessionItems.value = list

        // Check if spelling complete
        if (newTyped.length >= currentItem.word.word.length) {
            checkSpellingAnswer(newTyped)
        }
    }

    fun onBackspaceSpelling() {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        spellingHideJob?.cancel()
        val newTyped = if (currentItem.showCorrectSpelling) {
            ""
        } else {
            currentItem.typedSpelling.dropLast(1)
        }
        list[index] = currentItem.copy(
            typedSpelling = newTyped,
            showCorrectSpelling = false
        )
        _sessionItems.value = list
    }

    /**
     * 盲听拼写：一键跳过，直接跳过这个单词的拼写，并且不标记为失败
     */
    fun onSkipSpelling() {
        spellingHideJob?.cancel()
        stopPronunciation()
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        val updatedSuccessCount = currentItem.recognizedSuccessCount + 1
        if (updatedSuccessCount >= targetSuccessCount) {
            viewModelScope.launch {
                repository.recordReview(currentItem.word, ReviewQuality.KNOW)
                _sessionPassedCount.value += 1
                val l2 = _sessionItems.value.toMutableList()
                if (index < l2.size) {
                    l2.removeAt(index)
                    _sessionItems.value = l2
                }
                checkSessionFinishedOrLoadNext(l2)
                refreshCurrentTime()
            }
        } else {
            val switches = activeLearningModeSwitches
            val candidateStages = getCandidateStages(switches).filter { it != LearnItemStage.BLIND_SPELLING }
                .ifEmpty { getCandidateStages(switches) }
            val nextStage = candidateStages.randomOrNull() ?: LearnItemStage.BLIND_LISTEN_MEANING
            val choices = when (nextStage) {
                LearnItemStage.BLIND_LISTEN_MEANING,
                LearnItemStage.LISTEN_CHOOSE_MEANING,
                LearnItemStage.LOOK_CHOOSE_MEANING -> generateChoiceOptionsForMeaning(currentItem.word)
                LearnItemStage.LISTEN_CHOOSE_WORD,
                LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
                LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
                LearnItemStage.CLOZE_SENTENCE -> generateChoiceOptionsForWord(currentItem.word)
                else -> emptyList()
            }
            val cleanRescheduledItem = currentItem.copy(
                stage = nextStage,
                recognizedSuccessCount = updatedSuccessCount,
                selectedOptionIndex = null,
                isAnswerCorrect = null,
                flashingOptionIndex = null,
                flashingIsCorrect = true,
                isHintClicked = false,
                isWordRevealed = false,
                typedSpelling = "",
                showCorrectSpelling = false,
                spellingSuccess = false,
                remainingSpellingAttempts = 3,
                countdownSecondsLeft = 5,
                multipleChoiceOptions = choices,
                totalAppearCount = currentItem.totalAppearCount + 1
            )
            val curList = _sessionItems.value.toMutableList()
            if (index < curList.size) {
                curList.removeAt(index)
                curList.add(cleanRescheduledItem)
                _sessionItems.value = curList
                if (_currentSessionIndex.value >= curList.size) {
                    _currentSessionIndex.value = 0
                }
                playCurrentLearnItemAudio()
            }
        }
    }

    fun checkSpellingAnswer(typed: String = "") {
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]
        val answer = if (typed.isNotBlank()) typed else currentItem.typedSpelling

        val isCorrect = answer.trim().equals(currentItem.word.word.trim(), ignoreCase = true)
        if (isCorrect) {
            // 拼写正确时展示单词1秒，自动进入下一个
            viewModelScope.launch {
                spellingHideJob?.cancel()
                val l1 = _sessionItems.value.toMutableList()
                if (index < l1.size) {
                    l1[index] = l1[index].copy(
                        spellingSuccess = true,
                        isWordRevealed = true,
                        isAnswerCorrect = true,
                        showCorrectSpelling = false
                    )
                    _sessionItems.value = l1
                }

                delay(1000)

                val updatedSuccessCount = currentItem.recognizedSuccessCount + 1
                if (true) { // 拼写正确后自动进入下一个
                    if (updatedSuccessCount >= targetSuccessCount) {
                        repository.recordReview(currentItem.word, ReviewQuality.KNOW)
                        _sessionPassedCount.value += 1
                        val l2 = _sessionItems.value.toMutableList()
                        if (index < l2.size) {
                            l2.removeAt(index)
                            _sessionItems.value = l2
                        }
                        checkSessionFinishedOrLoadNext(l2)
                        refreshCurrentTime()
                    } else {
                        val switches = activeLearningModeSwitches
                        val candidateStages = getCandidateStages(switches)
                        val nextStage = candidateStages.randomOrNull() ?: currentItem.stage
                        val choices = when (nextStage) {
                            LearnItemStage.BLIND_LISTEN_MEANING,
                            LearnItemStage.LISTEN_CHOOSE_MEANING,
                            LearnItemStage.LOOK_CHOOSE_MEANING -> generateChoiceOptionsForMeaning(currentItem.word)
                            LearnItemStage.LISTEN_CHOOSE_WORD,
                            LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
                            LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD,
                            LearnItemStage.CLOZE_SENTENCE -> generateChoiceOptionsForWord(currentItem.word)
                            else -> emptyList()
                        }

                        val cleanRescheduledItem = currentItem.copy(
                            stage = nextStage,
                            recognizedSuccessCount = updatedSuccessCount,
                            selectedOptionIndex = null,
                            isAnswerCorrect = null,
                            flashingOptionIndex = null,
                            flashingIsCorrect = true,
                            isHintClicked = false,
                            isWordRevealed = false,
                            typedSpelling = "",
                            showCorrectSpelling = false,
                            spellingSuccess = false,
                            remainingSpellingAttempts = 3,
                            countdownSecondsLeft = 5,
                            multipleChoiceOptions = choices,
                            totalAppearCount = currentItem.totalAppearCount + 1
                        )

                        val curList = _sessionItems.value.toMutableList()
                        if (index < curList.size) {
                            curList.removeAt(index)
                            curList.add(cleanRescheduledItem)
                            _sessionItems.value = curList
                            if (_currentSessionIndex.value >= curList.size) {
                                _currentSessionIndex.value = 0
                            }
                            playCurrentLearnItemAudio()
                        }
                    }
                } else {
                    // 默认关闭自动进入下一个：拼写正确后也自动进入详情界面
                    val curList = _sessionItems.value.toMutableList()
                    if (index < curList.size) {
                        curList[index] = curList[index].copy(
                            stage = LearnItemStage.CHOOSE_MEANING_EXPLAIN,
                            spellingSuccess = true,
                            isAnswerCorrect = true,
                            recognizedSuccessCount = updatedSuccessCount,
                            showCorrectSpelling = false
                        )
                        _sessionItems.value = curList
                    }
                }
            }
        } else {
            // 盲听拼写给3次机会尝试拼写，错3次后自动进入详情界面
            val remaining = currentItem.remainingSpellingAttempts - 1
            if (remaining <= 0) {
                // 错3次后自动进入详情界面
                spellingHideJob?.cancel()
                _sessionReinforcedCount.value += 1
                val updatedItem = currentItem.copy(
                    stage = LearnItemStage.CHOOSE_MEANING_EXPLAIN,
                    failOrHintCount = currentItem.failOrHintCount + 1,
                    isAnswerCorrect = false,
                    showCorrectSpelling = false,
                    spellingSuccess = false,
                    remainingSpellingAttempts = 3,
                    typedSpelling = ""
                )
                list[index] = updatedItem
                _sessionItems.value = list
                viewModelScope.launch {
                    repository.recordReview(currentItem.word, ReviewQuality.FORGOT)
                }
            } else {
                // 拼写错误后展示正确拼法5秒，用户再次开始拼写时自动隐藏正确拼法
                list[index] = currentItem.copy(
                    remainingSpellingAttempts = remaining,
                    showCorrectSpelling = true,
                    typedSpelling = ""
                )
                _sessionItems.value = list

                spellingHideJob?.cancel()
                spellingHideJob = viewModelScope.launch {
                    delay(5000)
                    val l = _sessionItems.value.toMutableList()
                    if (index < l.size && l[index].word.id == currentItem.word.id) {
                        l[index] = l[index].copy(showCorrectSpelling = false)
                        _sessionItems.value = l
                    }
                }
            }
        }
    }

    // Action in "看义说词" (正确 / 忘了 / 错误)
    fun onRecallDecision(quality: ReviewQuality) {
        countdownJob?.cancel()
        val list = _sessionItems.value.toMutableList()
        val index = _currentSessionIndex.value
        if (index >= list.size) return
        val currentItem = list[index]

        if (quality == ReviewQuality.KNOW) {
            val updatedSuccessCount = currentItem.recognizedSuccessCount + 1
            if (activeAutoAdvanceOnCorrect) { // 开启自动进入下一个：看义说词正确后自动切题
                if (updatedSuccessCount >= targetSuccessCount) {
                    viewModelScope.launch {
                        repository.recordReview(currentItem.word, ReviewQuality.KNOW)
                        _sessionPassedCount.value += 1
                        list.removeAt(index)
                        _sessionItems.value = list
                        checkSessionFinishedOrLoadNext(list)
                        refreshCurrentTime()
                    }
                } else {
                    val cleanRescheduled = currentItem.copy(
                        recognizedSuccessCount = updatedSuccessCount,
                        selectedOptionIndex = null,
                        isAnswerCorrect = null,
                        flashingOptionIndex = null,
                        isHintClicked = false,
                        isWordRevealed = false,
                        typedSpelling = "",
                        showCorrectSpelling = false,
                        spellingSuccess = false,
                        remainingSpellingAttempts = 3,
                        countdownSecondsLeft = 5,
                        totalAppearCount = currentItem.totalAppearCount + 1
                    )
                    list.removeAt(index)
                    list.add(cleanRescheduled)
                    _sessionItems.value = list
                    if (_currentSessionIndex.value >= list.size) {
                        _currentSessionIndex.value = 0
                    }
                    playCurrentLearnItemAudio()
                }
            } else {
                // 默认关闭自动进入下一个：看义说词正确后也进入详情界面
                val updatedItem = currentItem.copy(
                    stage = LearnItemStage.CHOOSE_MEANING_EXPLAIN,
                    isAnswerCorrect = true,
                    recognizedSuccessCount = updatedSuccessCount,
                    isWordRevealed = true
                )
                list[index] = updatedItem
                _sessionItems.value = list
            }
        } else {
            _sessionReinforcedCount.value += 1
            val updatedItem = currentItem.copy(
                stage = LearnItemStage.CHOOSE_MEANING_EXPLAIN,
                failOrHintCount = currentItem.failOrHintCount + 1,
                selectedOptionIndex = null,
                isAnswerCorrect = false,
                flashingOptionIndex = null
            )
            list[index] = updatedItem
            _sessionItems.value = list
            viewModelScope.launch {
                repository.recordReview(currentItem.word, quality)
            }
        }
    }

    fun revealMeaning() {
        _isWordRevealedAfterAudio.value = true
    }

    // Direct master (斩词)
    fun markMastered(word: WordEntity) {
        viewModelScope.launch {
            repository.markMastered(word, true)
            allPendingSessionWords = allPendingSessionWords.filter { it.id != word.id && it.word != word.word }
            val currentList = _sessionItems.value
            val updatedList = currentList.filter { it.word.id != word.id && it.word.word != word.word }.toMutableList()
            if (updatedList.size != currentList.size) {
                _sessionItems.value = updatedList
                checkSessionFinishedOrLoadNext(updatedList)
            }
            showMessage("已将【${word.word}】标熟！直接跳过学习与复习。")
            if (_currentScreen.value == AppScreen.REVIEW && !_isReviewFinished.value) {
                recordReviewAnswer(ReviewQuality.KNOW)
            }
        }
    }

    fun toggleMastered(word: WordEntity) {
        viewModelScope.launch {
            val newStatus = !word.isMastered
            repository.markMastered(word, newStatus)
            if (newStatus) {
                allPendingSessionWords = allPendingSessionWords.filter { it.id != word.id && it.word != word.word }
                val currentList = _sessionItems.value
                val updatedList = currentList.filter { it.word.id != word.id && it.word.word != word.word }.toMutableList()
                if (updatedList.size != currentList.size) {
                    _sessionItems.value = updatedList
                    checkSessionFinishedOrLoadNext(updatedList)
                }
            }
            showMessage(if (newStatus) "已将【${word.word}】标记为已掌握并移出学习组" else "已取消【${word.word}】标熟状态")
            if (newStatus && _portablePlaylist.value.any { it.id == word.id }) {
                _portablePlaylist.value = _portablePlaylist.value.filter { it.id != word.id }
            }
        }
    }

    fun toggleFavorite(word: WordEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(word)
            val updatedState = !word.isFavorite
            showMessage(if (updatedState) "已收藏【${word.word}】" else "已取消收藏【${word.word}】")
        }
    }

    // ==========================================
    // 艾宾浩斯复习：固定分组切片与组内“清剿消灭”闭环流转
    // ==========================================
    fun startReview(limit: Int = -1) {
        val targetLimit = if (limit > 0) limit else (_dailyReviewTargetGroups.value * _dailyWordsPerGroup.value)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dueWords = repository.getDueReviewWords(now, targetLimit)
            if (dueWords.isEmpty()) {
                _userMessage.value = "目前暂无到期需复习的单词！根据艾宾浩斯曲线，记忆状态良好。"
                return@launch
            }
            startReviewWithWords(dueWords, "【今日艾宾浩斯复习】")
        }
    }

    fun initReviewSession(wordsToReview: List<WordEntity>) {
        if (wordsToReview.isEmpty()) return
        val groupSize = _dailyWordsPerGroup.value.coerceAtLeast(1)
        val chunks = wordsToReview.chunked(groupSize)
        val slices = chunks.mapIndexed { idx, sliceWords ->
            ReviewGroupSlice(
                sliceIndex = idx,
                totalSlices = chunks.size,
                words = sliceWords
            )
        }
        _reviewSlices.value = slices
        _currentReviewSliceIndex.value = 0
        _reviewQueue.value = wordsToReview
        _reviewIndex.value = 0
        _isReviewMeaningRevealed.value = false
        _isReviewFinished.value = false
        _sessionReviewedCount.value = 0
        _currentScreen.value = AppScreen.REVIEW
        loadReviewSlice(0)
    }

    private fun loadReviewSlice(sliceIdx: Int) {
        val slices = _reviewSlices.value
        if (sliceIdx >= slices.size) {
            _isReviewFinished.value = true
            _currentReviewQuestion.value = null
            return
        }
        _currentReviewSliceIndex.value = sliceIdx
        val slice = slices[sliceIdx]
        val progressMap = slice.words.associate { it.word to ReviewWordProgress(it, 0, 0) }.toMutableMap()
        _currentSliceProgress.value = progressMap
        _activeRoundWords.value = slice.words
        _currentWordInRoundIndex.value = 0
        prepareReviewQuestion(slice.words.first())
    }

    private fun prepareReviewQuestion(word: WordEntity) {
        val activeModes = _reviewModeSwitches.value.getActiveModes()
        val mode = activeModes.random()
        val options = when (mode) {
            ReviewQuestionMode.BLIND_LISTEN_MEANING -> generateChoiceOptionsForMeaning(word)
            ReviewQuestionMode.CHINESE_MEANING_CHOOSE_WORD -> generateChoiceOptionsForWord(word)
            else -> emptyList()
        }
        _currentReviewQuestion.value = ReviewActiveQuestion(
            word = word,
            mode = mode,
            multipleChoiceOptions = options,
            selectedOptionIndex = null,
            isAnswerCorrect = null,
            isWordRevealed = false,
            typedSpelling = "",
            isShowingDetail = false
        )
        if (_reviewAutoPronounce.value) {
            pronounceReviewWord(word.word)
        }
    }

    fun pronounceReviewWord(wordText: String) {
        when (_reviewVoicePref.value) {
            FirstListenVoicePreference.US -> pronounceWord(wordText, "built_in_us")
            FirstListenVoicePreference.UK -> pronounceWord(wordText, "built_in_uk")
            FirstListenVoicePreference.ALTERNATING -> pronounceSequence(wordText)
            FirstListenVoicePreference.DEFAULT -> pronounceWord(wordText)
        }
    }

    // 1 & 2. 盲听选义 或 中文释义选词 选择选项
    fun onReviewSelectChoiceOption(optionIndex: Int) {
        val q = _currentReviewQuestion.value ?: return
        if (q.isShowingDetail) return
        val isCorrect = when (q.mode) {
            ReviewQuestionMode.BLIND_LISTEN_MEANING -> {
                q.multipleChoiceOptions.getOrNull(optionIndex) == q.word.meaning
            }
            ReviewQuestionMode.CHINESE_MEANING_CHOOSE_WORD -> {
                q.multipleChoiceOptions.getOrNull(optionIndex) == q.word.word
            }
            else -> false
        }
        if (isCorrect) {
            handleReviewCorrect(q.word)
        } else {
            handleReviewWrong(q.word)
        }
    }

    // 点击“不认识”
    fun onReviewDontKnowClicked() {
        val q = _currentReviewQuestion.value ?: return
        if (q.isShowingDetail) return
        handleReviewWrong(q.word)
    }

    // 3. 看义说词：揭晓答案
    fun revealReviewRecallAnswer() {
        val q = _currentReviewQuestion.value ?: return
        _currentReviewQuestion.value = q.copy(isWordRevealed = true)
        pronounceReviewWord(q.word.word)
    }

    // 3. 看义说词：结果确认 (答对 / 记错)
    fun onReviewRecallDecision(isCorrect: Boolean) {
        val q = _currentReviewQuestion.value ?: return
        if (isCorrect) {
            handleReviewCorrect(q.word)
        } else {
            handleReviewWrong(q.word)
        }
    }

    // 4. 拼写：提交验证
    fun onReviewSubmitSpelling(spelling: String) {
        val q = _currentReviewQuestion.value ?: return
        if (q.isShowingDetail) return
        val isCorrect = spelling.trim().equals(q.word.word.trim(), ignoreCase = true)
        if (isCorrect) {
            handleReviewCorrect(q.word)
        } else {
            handleReviewWrong(q.word)
        }
    }

    private fun handleReviewCorrect(word: WordEntity) {
        viewModelScope.launch {
            repository.recordReview(word, ReviewQuality.KNOW)
            recordLearningDayActivity()
            refreshCurrentTime()
        }
        val progressMap = _currentSliceProgress.value.toMutableMap()
        val current = progressMap[word.word] ?: ReviewWordProgress(word)
        val updated = current.copy(correctCount = current.correctCount + 1)
        progressMap[word.word] = updated
        _currentSliceProgress.value = progressMap
        _sessionReviewedCount.value += 1
        // 答对直接进入下一个!
        advanceToNextReviewWord()
    }

    private fun handleReviewWrong(word: WordEntity) {
        val updatedWord = word.copy(mistakeCount = word.mistakeCount + 1, isMastered = false)
        viewModelScope.launch {
            repository.recordReview(word, ReviewQuality.FORGOT)
            recordLearningDayActivity()
            refreshCurrentTime()
            refreshMatchingGame()
        }
        val progressMap = _currentSliceProgress.value.toMutableMap()
        val current = progressMap[word.word] ?: ReviewWordProgress(word)
        val updated = current.copy(
            failCount = current.failCount + 1,
            word = updatedWord
        )
        progressMap[word.word] = updated
        _currentSliceProgress.value = progressMap
        // 答错进入详情页!
        val q = _currentReviewQuestion.value
        if (q != null) {
            _currentReviewQuestion.value = q.copy(
                isShowingDetail = true,
                word = updatedWord
            )
            pronounceReviewWord(word.word)
        }
    }

    // 详情页学习完毕点击“继续复习”
    fun onReviewContinueFromDetail() {
        advanceToNextReviewWord()
    }

    // 轮转推进到组内下一个待复习单词
    private fun advanceToNextReviewWord() {
        val currentRound = _activeRoundWords.value
        val nextIndex = _currentWordInRoundIndex.value + 1
        if (nextIndex < currentRound.size) {
            _currentWordInRoundIndex.value = nextIndex
            prepareReviewQuestion(currentRound[nextIndex])
        } else {
            // 当前单轮遍历结束，检查本切片组内所有未消灭单词 (correctCount < 2)
            val progressMap = _currentSliceProgress.value
            val uneliminated = progressMap.values.filter { !it.isEliminated }.map { it.word }
            if (uneliminated.isNotEmpty()) {
                // 开启新一轮轮转清剿
                _activeRoundWords.value = uneliminated.shuffled()
                _currentWordInRoundIndex.value = 0
                prepareReviewQuestion(_activeRoundWords.value.first())
            } else {
                // 本组所有单词均累计答对2次，组内清剿消灭完毕！
                val nextSliceIdx = _currentReviewSliceIndex.value + 1
                if (nextSliceIdx < _reviewSlices.value.size) {
                    showMessage("第 ${nextSliceIdx} 组已清剿消灭！开始第 ${nextSliceIdx + 1} 组复习")
                    loadReviewSlice(nextSliceIdx)
                } else {
                    _isReviewFinished.value = true
                    _currentReviewQuestion.value = null
                }
            }
        }
    }

    // 在详情页一键标熟
    fun markCurrentReviewWordMastered() {
        val q = _currentReviewQuestion.value ?: return
        viewModelScope.launch {
            toggleMastered(q.word)
            val progressMap = _currentSliceProgress.value.toMutableMap()
            val current = progressMap[q.word.word] ?: ReviewWordProgress(q.word)
            progressMap[q.word.word] = current.copy(correctCount = 2)
            _currentSliceProgress.value = progressMap
            advanceToNextReviewWord()
        }
    }

    // 设置项更新
    fun updateReviewModeSwitches(switches: ReviewModeSwitches) {
        if (switches.enabledCount == 0) {
            showMessage("请至少开启一种复习模式！")
            return
        }
        _reviewModeSwitches.value = switches
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("review_mode_blind_listen", switches.blindListenMeaning)
            .putBoolean("review_mode_chinese_meaning", switches.chineseMeaningChooseWord)
            .putBoolean("review_mode_recall", switches.recallByMeaning)
            .putBoolean("review_mode_spelling", switches.spelling)
            .apply()
        // 若当前题目模式被关闭，重新生成题目
        val q = _currentReviewQuestion.value
        if (q != null && !q.isShowingDetail && q.mode !in switches.getActiveModes()) {
            prepareReviewQuestion(q.word)
        }
    }

    fun setReviewAutoPronounce(enabled: Boolean) {
        _reviewAutoPronounce.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("review_auto_pronounce", enabled).apply()
    }

    fun setReviewVoicePref(pref: FirstListenVoicePreference) {
        _reviewVoicePref.value = pref
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("review_voice_pref", pref.name).apply()
    }

    // 兼容旧接口
    fun revealReviewMeaning() {
        revealReviewRecallAnswer()
    }

    fun recordReviewAnswer(quality: ReviewQuality) {
        if (quality == ReviewQuality.KNOW) {
            val q = _currentReviewQuestion.value
            if (q != null) handleReviewCorrect(q.word)
        } else {
            val q = _currentReviewQuestion.value
            if (q != null) handleReviewWrong(q.word)
        }
    }

    // Search
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ==========================================
    // Portable Player (随身听 / 磨耳朵) Logic
    // ==========================================
    fun openPortablePlayer(category: String = "全部") {
        _portablePlayingCategory.value = category
        updatePortablePlaylist(category)
        _portableCurrentIndex.value = 0
        _currentScreen.value = AppScreen.PORTABLE_PLAYER
        if (_portablePlaylist.value.isNotEmpty()) {
            startPortablePlayback()
        } else {
            pausePortablePlayback()
            _portablePlayStatusText.value = "当前分类暂无单词，不播放"
        }
    }

    fun setPortableCategory(category: String) {
        _portablePlayingCategory.value = category
        updatePortablePlaylist(category)
        _portableCurrentIndex.value = 0
        if (_portablePlaylist.value.isNotEmpty()) {
            if (_isPortablePlaying.value) {
                startPortablePlayback()
            }
        } else {
            pausePortablePlayback()
            _portablePlayStatusText.value = "当前分类暂无单词，不播放"
        }
    }

    private fun updatePortablePlaylist(category: String) {
        val words = wordsForActiveBook.value
        val now = System.currentTimeMillis()
        val groups = fixedWordGroups.value

        val filtered = when {
            category.startsWith("任务组") -> {
                val taskGroups = groups.filter { it.isTaskGroup }
                if (taskGroups.isNotEmpty()) {
                    taskGroups.flatMap { it.words }
                } else {
                    val taskGroup = groups.find { it.isTaskGroup } ?: groups.firstOrNull()
                    taskGroup?.words ?: emptyList()
                }
            }
            category.startsWith("复习组") -> {
                val due = words.filter { !it.isMastered && it.nextReviewTime in 1..now }
                if (due.isNotEmpty()) due else words.filter { !it.isMastered && it.reviewStage > 0 }.take(20)
            }
            category.startsWith("L") || category.contains("L") || groups.any { category.contains(it.tag) } -> {
                val tag = if (category.contains(" (")) category.substringBefore(" (") else category.trim()
                val grp = groups.find { it.tag == tag || category.contains(it.tag) }
                grp?.words ?: emptyList()
            }
            category == "未学" -> words.filter { it.reviewStage == 0 && !it.isMastered }
            category == "待复习" -> words.filter { it.nextReviewTime in 1..now && !it.isMastered }
            category == "已标熟" || category == "标熟" -> words.filter { it.isMastered }
            category == "已收藏" -> words.filter { it.isFavorite }
            else -> words.filter { !it.isMastered }
        }
        _portablePlaylist.value = filtered
    }

    // 随身听：一键收听任务组单词
    fun playTaskGroupInPortablePlayer() {
        val groups = fixedWordGroups.value
        val taskGroups = groups.filter { it.isTaskGroup }
        if (taskGroups.isNotEmpty()) {
            val words = taskGroups.flatMap { it.words }
            val taskTagsText = taskGroups.joinToString(",") { it.tag }
            _portablePlayingCategory.value = "任务组 ($taskTagsText)"
            _portablePlaylist.value = words
            _portableCurrentIndex.value = 0
            _currentScreen.value = AppScreen.PORTABLE_PLAYER
            startPortablePlayback()
            showMessage("一键收听任务组【$taskTagsText】(共${words.size}词)")
        } else {
            val taskGroup = groups.find { it.isTaskGroup } ?: groups.firstOrNull { it.unlearnedCount > 0 } ?: groups.firstOrNull()
            if (taskGroup != null && taskGroup.words.isNotEmpty()) {
                _portablePlayingCategory.value = "任务组 (${taskGroup.tag})"
                _portablePlaylist.value = taskGroup.words
                _portableCurrentIndex.value = 0
                _currentScreen.value = AppScreen.PORTABLE_PLAYER
                startPortablePlayback()
                showMessage("一键收听任务组【${taskGroup.tag}】(共${taskGroup.words.size}词)")
            } else {
                showMessage("当前没有待学习的任务组单词！")
            }
        }
    }

    // 随身听：一键收听复习组单词（艾宾浩斯到期）
    fun playReviewGroupInPortablePlayer() {
        val now = System.currentTimeMillis()
        val dueWords = wordsForActiveBook.value.filter { !it.isMastered && it.nextReviewTime in 1..now }
        val playlistWords = if (dueWords.isNotEmpty()) {
            dueWords
        } else {
            wordsForActiveBook.value.filter { !it.isMastered && it.reviewStage > 0 }.take(20)
        }
        if (playlistWords.isNotEmpty()) {
            _portablePlayingCategory.value = "复习组"
            _portablePlaylist.value = playlistWords
            _portableCurrentIndex.value = 0
            _currentScreen.value = AppScreen.PORTABLE_PLAYER
            startPortablePlayback()
            showMessage(if (dueWords.isNotEmpty()) "一键收听复习组 (共${dueWords.size}个艾宾浩斯到期词)" else "当前暂无到期词，正在复习已学单词")
        } else {
            showMessage("当前暂无已学或到期复习单词！")
        }
    }

    // 随身听：自由选择多组单词进行收听
    fun playMultipleGroupsInPortablePlayer(groups: List<WordFixedGroup>) {
        val allWords = groups.flatMap { it.words }
        if (allWords.isEmpty()) {
            showMessage("选中的单词组均无单词！")
            return
        }
        val tags = groups.map { it.tag }
        _portablePlayingCategory.value = if (tags.size <= 2) tags.joinToString(" + ") else "${tags.first()}..${tags.last()} 等${tags.size}组"
        _portablePlaylist.value = allWords
        _portableCurrentIndex.value = 0
        _currentScreen.value = AppScreen.PORTABLE_PLAYER
        startPortablePlayback()
        showMessage("已将选中单词组推送至随身听 (共${allWords.size}词)")
    }

    // 随身听：自由选择某组单词进行收听
    fun playFixedGroupInPortablePlayer(group: WordFixedGroup) {
        if (group.words.isEmpty()) {
            showMessage("该组暂无单词")
            return
        }
        _portablePlayingCategory.value = group.tag
        _portablePlaylist.value = group.words
        _portableCurrentIndex.value = 0
        _currentScreen.value = AppScreen.PORTABLE_PLAYER
        startPortablePlayback()
        showMessage("开始收听【${group.tag}】(共${group.words.size}词)")
    }

    fun startPortablePlayback() {
        portableLoopJob?.cancel()
        if (_portablePlaylist.value.isEmpty()) {
            updatePortablePlaylist(_portablePlayingCategory.value)
        }
        val currentList = _portablePlaylist.value
        if (currentList.isEmpty()) {
            _isPortablePlaying.value = false
            _portablePlayPhase.value = PortablePlayPhase.IDLE
            _portablePlayStatusText.value = "当前分类暂无单词，不播放"
            return
        }
        _isPortablePlaying.value = true
        portableLoopJob = viewModelScope.launch {
            try {
                while (_isPortablePlaying.value) {
                    val list = _portablePlaylist.value
                    if (list.isEmpty()) {
                        _portablePlayStatusText.value = "当前分类暂无单词，不播放"
                        _isPortablePlaying.value = false
                        _portablePlayPhase.value = PortablePlayPhase.IDLE
                        break
                    }
                    val index = _portableCurrentIndex.value.coerceIn(0, list.size - 1)
                    val currentWord = list[index]

                    val selectedItems = _portableSelectedContentItems.value
                    val pauseSec = _portablePauseSeconds.value

                    // Step 1: Speak English Word (if selected)
                    if (selectedItems.contains(PortableContentItem.WORD) && currentWord.word.isNotBlank()) {
                        if (_isPortablePlaying.value) {
                            _portablePlayPhase.value = PortablePlayPhase.WORD
                            val enabledWordIds = _portableEnabledProfileIds.value
                            val wordProfileOrder = listOf("voice_longman_us", "voice_youdao_us", "voice_youdao_uk")
                            val allProfiles = voiceProfiles.value
                            val activeWordProfiles = wordProfileOrder.filter { it in enabledWordIds }
                                .mapNotNull { id -> allProfiles.find { it.id == id } }
                            val profilesToPlay = if (activeWordProfiles.isNotEmpty()) activeWordProfiles else allProfiles.filter { it.id == "voice_longman_us" }

                            for (profile in profilesToPlay) {
                                if (!_isPortablePlaying.value) break
                                _portablePlayStatusText.value = "朗读单词 (${profile.name})"
                                var audioDone = false
                                pronouncer.speakWordWithCallback(currentWord.word, profile.id) {
                                    audioDone = true
                                }
                                val startT = System.currentTimeMillis()
                                while (!audioDone && (System.currentTimeMillis() - startT) < 3000) {
                                    delay(60)
                                }
                                delay(200)
                            }
                        }
                    }

                    // Step 2: Speak Chinese Meaning (if selected)
                    if (selectedItems.contains(PortableContentItem.CHINESE_MEANING) && currentWord.meaning.isNotBlank()) {
                        if (_isPortablePlaying.value) {
                            _portablePlayPhase.value = PortablePlayPhase.CHINESE_MEANING
                            _portablePlayStatusText.value = "朗读中文释义"
                            var ttsDone = false
                            pronouncer.speakChinese(currentWord.meaning) {
                                ttsDone = true
                            }
                            val ttsStart = System.currentTimeMillis()
                            while (!ttsDone && (System.currentTimeMillis() - ttsStart) < 3500) {
                                delay(60)
                            }
                            delay(200)
                        }
                    }

                    // Step 3: Speak English Definition (if selected)
                    if (selectedItems.contains(PortableContentItem.ENGLISH_DEFINITION) && currentWord.definition.isNotBlank()) {
                        if (_isPortablePlaying.value) {
                            _portablePlayPhase.value = PortablePlayPhase.ENGLISH_DEFINITION
                            _portablePlayStatusText.value = "朗读英文释义"
                            val cleanDef = currentWord.definition.replace("\n", "; ").take(150)
                            var enDefDone = false
                            pronouncer.speakSentence(cleanDef, SentenceVoiceType.TTS_1, onCompletion = {
                                enDefDone = true
                            })
                            val enDefStart = System.currentTimeMillis()
                            while (!enDefDone && (System.currentTimeMillis() - enDefStart) < 5000) {
                                delay(60)
                            }
                            delay(200)
                        }
                    }

                    // Step 4: Speak Example Sentences (例句1, 例句2, 例句3)
                    val sents = if (currentWord.exampleSentence.isNotBlank()) {
                        currentWord.exampleSentence.split(" ||| ").filter { it.isNotBlank() }
                    } else emptyList()

                    val sentenceConfigList = listOf(
                        Pair(PortableContentItem.SENTENCE_1, PortablePlayPhase.SENTENCE_1),
                        Pair(PortableContentItem.SENTENCE_2, PortablePlayPhase.SENTENCE_2),
                        Pair(PortableContentItem.SENTENCE_3, PortablePlayPhase.SENTENCE_3)
                    )

                    for ((sIdx, config) in sentenceConfigList.withIndex()) {
                        val (itemEnum, phaseEnum) = config
                        if (selectedItems.contains(itemEnum) && sIdx < sents.size && _isPortablePlaying.value) {
                            val sentText = sents[sIdx]
                            _portablePlayPhase.value = phaseEnum
                            val sentenceVoiceOrder = listOf(SentenceVoiceType.TTS_1, SentenceVoiceType.TTS_2, SentenceVoiceType.TTS_3)
                            val enabledSentenceTypes = _portableEnabledSentenceVoiceTypes.value
                            val sentenceVoicesToPlay = sentenceVoiceOrder.filter { it in enabledSentenceTypes }
                                .ifEmpty { listOf(SentenceVoiceType.TTS_1) }

                            for (sentenceVoice in sentenceVoicesToPlay) {
                                if (!_isPortablePlaying.value) break
                                _portablePlayStatusText.value = "朗读例句${sIdx + 1} (${sentenceVoice.title})"
                                var sentenceDone = false
                                pronouncer.speakSentence(sentText, sentenceVoice, onCompletion = {
                                    sentenceDone = true
                                })
                                val sentenceStart = System.currentTimeMillis()
                                while (!sentenceDone && (System.currentTimeMillis() - sentenceStart) < 5500) {
                                    delay(60)
                                }
                                delay(200)
                            }
                        }
                    }

                    // Step 5: Interval Pause before next word (if enabled)
                    if (pauseSec > 0f) {
                        _portablePlayPhase.value = PortablePlayPhase.PAUSE
                        val pauseLabel = if (pauseSec == pauseSec.toInt().toFloat()) "${pauseSec.toInt()}" else "$pauseSec"
                        _portablePlayStatusText.value = "停顿 ${pauseLabel}秒..."
                        delay((pauseSec * 1000L).toLong())
                    }

                    // Auto Next Word
                    val nextIndex = (index + 1) % list.size
                    _portableCurrentIndex.value = nextIndex
                }
            } catch (e: CancellationException) {
                _portablePlayPhase.value = PortablePlayPhase.IDLE
                _portablePlayStatusText.value = "已暂停"
            } finally {
                if (!_isPortablePlaying.value) {
                    _portablePlayPhase.value = PortablePlayPhase.IDLE
                }
            }
        }
    }

    fun stopPronunciation() {
        pronouncer.stop()
    }

    fun getPreferredProfileIdForAccent(accent: VoiceAccent): String? {
        val profiles = voiceProfiles.value
        return profiles.find { it.accent == accent && it.isDefault && it.isEnabled }?.id
            ?: profiles.find { it.accent == accent && it.isEnabled && it.sourceType == "MDD" }?.id
            ?: profiles.find { it.accent == accent && it.isEnabled }?.id
            ?: profiles.find { it.accent == accent }?.id
    }

    fun pronounceWordUK(word: String) {
        val ukProfileId = getPreferredProfileIdForAccent(VoiceAccent.UK)
        pronounceWord(word, ukProfileId)
    }

    fun pausePortablePlayback() {
        _isPortablePlaying.value = false
        _portablePlayPhase.value = PortablePlayPhase.IDLE
        portableLoopJob?.cancel()
        pronouncer.stop()
        _portablePlayStatusText.value = "已暂停"
    }

    fun togglePortablePlayPause() {
        if (_isPortablePlaying.value) {
            pausePortablePlayback()
        } else {
            startPortablePlayback()
        }
    }

    fun nextPortableWord() {
        val list = _portablePlaylist.value
        if (list.isNotEmpty()) {
            _portableCurrentIndex.value = (_portableCurrentIndex.value + 1) % list.size
            if (_isPortablePlaying.value) {
                startPortablePlayback()
            }
        }
    }

    fun prevPortableWord() {
        val list = _portablePlaylist.value
        if (list.isNotEmpty()) {
            _portableCurrentIndex.value = if (_portableCurrentIndex.value - 1 < 0) list.size - 1 else _portableCurrentIndex.value - 1
            if (_isPortablePlaying.value) {
                startPortablePlayback()
            }
        }
    }

    fun seekPortableWord(index: Int) {
        val list = _portablePlaylist.value
        if (index in list.indices) {
            _portableCurrentIndex.value = index
            if (_isPortablePlaying.value) {
                startPortablePlayback()
            }
        }
    }

    // 随身听点击单词窗口：临时发音点击的内容，播放完毕后不跳转下一个，只有点击播放按钮才开始循环播放
    fun temporaryPlayWord(index: Int) {
        val list = _portablePlaylist.value
        if (index in list.indices) {
            portableLoopJob?.cancel()
            _isPortablePlaying.value = false
            _portablePlayPhase.value = PortablePlayPhase.IDLE
            _portableCurrentIndex.value = index
            val word = list[index]
            _portablePlayStatusText.value = "点击试听: ${word.word}"
            pronounceWord(word.word)
        }
    }

    // Import Custom WordBook with MDX enrichment
    fun importCustomBook(
        title: String,
        description: String,
        rawContent: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val (parsedWords, report) = WordImporter.parseWithReport(rawContent, mdxManager = mdxManager)
                if (parsedWords.isEmpty()) {
                    _lastImportReport.value = report
                    onComplete(false, "未能解析到有效单词。已过滤 ${report.duplicateCount} 个重复词，${report.invalidSpellingCount} 个无效词。")
                    return@launch
                }
                val newBookId = repository.createCustomBook(
                    title = title.ifBlank { "我的自定义单词书" },
                    description = description.ifBlank { "共导入 ${parsedWords.size} 个单词" },
                    words = parsedWords
                )
                _lastImportReport.value = report
                _activeBookId.value = newBookId
                showMessage("成功导入单词书【$title】，共 ${parsedWords.size} 个单词！")
                onComplete(true, "导入成功")
            } catch (e: Exception) {
                onComplete(false, "导入失败: ${e.localizedMessage}")
            }
        }
    }

    // Import Words To Mark As Mastered (批量上传以标熟单词)
    fun importMasteredWords(
        rawContent: String,
        applyToActiveBookOnly: Boolean = false,
        onComplete: (Boolean, Int) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val lines = rawContent.lines()
                val wordTokens = lines.flatMap { line ->
                    line.split(Regex("[,;\\t\\r\\n]+"))
                        .map { it.trim() }
                        .filter { token ->
                            token.isNotBlank() && token.all { ch -> ch.isLetter() || ch == '-' || ch == '\'' || ch == ' ' }
                        }
                }.distinct()

                if (wordTokens.isEmpty()) {
                    showMessage("未能提取到有效英文单词，请检查格式")
                    onComplete(false, 0)
                    return@launch
                }

                val targetBookId = if (applyToActiveBookOnly) _activeBookId.value else null
                val markedCount = repository.batchMarkMastered(wordTokens, targetBookId)
                showMessage("成功识别 ${wordTokens.size} 个词，其中 $markedCount 个词已标熟！")
                onComplete(true, markedCount)
            } catch (e: Exception) {
                showMessage("标熟导入失败: ${e.localizedMessage}")
                onComplete(false, 0)
            }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
            if (_activeBookId.value == bookId) {
                _activeBookId.value = "cet4"
            }
            showMessage("已删除单词书")
        }
    }

    // Reminder settings
    fun setReminderEnabled(enabled: Boolean) {
        _isReminderEnabled.value = enabled
        ReviewReminderManager.setReminderEnabled(getApplication(), enabled)
        showMessage(if (enabled) "已开启每日艾宾浩斯复习提醒" else "已关闭复习提醒")
    }

    fun setReminderTime(hour: Int, minute: Int) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        ReviewReminderManager.setReminderTime(getApplication(), hour, minute)
        showMessage("已更新复习提醒时间为 %02d:%02d".format(hour, minute))
    }

    fun triggerTestNotification() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val due = repository.getDueReviewCountSync(now)
            val title = "艾宾浩斯复习提醒 (测试) 📚"
            val text = if (due > 0) {
                "检测到当前有 $due 个单词等待复习！点击立即巩固。"
            } else {
                "提醒系统运行正常！保持良好的每日记词节律。"
            }
            ReviewReminderManager.showNotification(getApplication(), title, text)
            showMessage("已发送测试通知，请查看系统通知栏！")
        }
    }

    override fun onCleared() {
        super.onCleared()
        portableLoopJob?.cancel()
        countdownJob?.cancel()
        pronouncer.release()
    }
}
