package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SentenceVoiceType
import com.example.data.model.WordEntity
import com.example.data.model.WordFixedGroup
import com.example.ui.navigation.AppScreen
import com.example.ui.viewmodel.MainViewModel

enum class WordListFilter(val title: String) {
    DUE("待复习"),
    TASK("任务"),
    ALL("全部"),
    MISTAKE("错词"),
    UNLEARNED("未学"),
    MASTERED("已标熟"),
    FAVORITE("已收藏")
}

enum class WordSortOrder(val title: String) {
    DEFAULT("默认顺序"),
    ALPHA("A-Z 排序"),
    REVIEW_URGENT("复习优先")
}

@Composable
fun WordListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeBookId by viewModel.activeBookId.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val activeBook = allBooks.find { it.id == activeBookId }

    val activeWords by viewModel.wordsForActiveBook.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(WordListFilter.TASK) }
    var sortOrder by remember { mutableStateOf(WordSortOrder.DEFAULT) }
    var expandedWordId by remember { mutableStateOf<Long?>(null) }
    
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedGroupTags by remember { mutableStateOf(setOf<String>()) }

    // Display mode toggles: 单词、释义、例句、中文优先
    var showWord by remember { mutableStateOf(true) }
    var showMeaning by remember { mutableStateOf(false) }
    var showSentence by remember { mutableStateOf(false) }
    var showChineseFirst by remember { mutableStateOf(false) }

    val dailyWordsPerGroup by viewModel.dailyWordsPerGroup.collectAsStateWithLifecycle()
    val dailyTargetGroups by viewModel.dailyTargetGroups.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()

    val fixedWordGroups by viewModel.fixedWordGroups.collectAsStateWithLifecycle()
    val taskGroups = remember(fixedWordGroups) { fixedWordGroups.filter { it.isTaskGroup } }
    val todayTaskUnlearned = remember(taskGroups, activeWords) {
        if (taskGroups.isNotEmpty()) {
            taskGroups.flatMap { g -> g.words.filter { it.reviewStage == 0 && !it.isMastered } }
        } else {
            val fallbackGroup = fixedWordGroups.firstOrNull { it.unlearnedCount > 0 } ?: fixedWordGroups.firstOrNull()
            fallbackGroup?.words?.filter { it.reviewStage == 0 && !it.isMastered }
                ?: activeWords.filter { it.reviewStage == 0 && !it.isMastered }.take(20)
        }
    }
    val todayDueWords = remember(activeWords, now) {
        activeWords.filter { it.nextReviewTime in 1..now && !it.isMastered }
    }
    val todayTaskWords = todayTaskUnlearned
    val taskCount = todayTaskWords.size

    // Counts for chips
    val allCount = activeWords.size
    val mistakeCount = activeWords.count { it.mistakeCount > 0 }
    val unlearnedCount = activeWords.count { it.reviewStage == 0 && !it.isMastered }
    val dueCount = activeWords.count { it.nextReviewTime in 1..now && !it.isMastered }
    val masteredCount = activeWords.count { it.isMastered }
    val favCount = activeWords.count { it.isFavorite }

    var collapsedGroupTags by remember { mutableStateOf(setOf<String>()) }

    // Setup Horizontal Pager to switch tabs by swiping!
    val pagerState = rememberPagerState(
        initialPage = WordListFilter.values().indexOf(selectedFilter),
        pageCount = { WordListFilter.values().size }
    )

    // Sync swiping with selected filter tab
    LaunchedEffect(pagerState.currentPage) {
        selectedFilter = WordListFilter.values()[pagerState.currentPage]
    }

    // Sync tab clicking with pager state
    LaunchedEffect(selectedFilter) {
        val targetPage = WordListFilter.values().indexOf(selectedFilter)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        // --- 非滑动的固定顶栏 ---
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "单词表",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${activeBook?.title ?: "单词书"} · 共 $allCount 词",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 全部计划 Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { viewModel.navigateTo(AppScreen.ALL_PLAN) }
                    .testTag("wordlist_all_plan_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "全部计划",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display Toggle Buttons: [单词] [释义] [例句]
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "显示:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 「单词」按钮
                FilterChip(
                    selected = showWord,
                    onClick = { showWord = !showWord },
                    label = { Text("单词", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    leadingIcon = if (showWord) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("wordlist_toggle_word_btn")
                )

                // 「释义」按钮
                FilterChip(
                    selected = showMeaning,
                    onClick = { showMeaning = !showMeaning },
                    label = { Text("释义", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    leadingIcon = if (showMeaning) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("wordlist_toggle_meaning_btn")
                )

                // 「例句」按钮
                FilterChip(
                    selected = showSentence,
                    onClick = { showSentence = !showSentence },
                    label = { Text("例句", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    leadingIcon = if (showSentence) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("wordlist_toggle_sentence_btn")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(searchFocusRequester)
                .testTag("wordlist_search_input"),
            placeholder = { Text("搜索单词、释义或例句...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清空",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs (可横向滑动容器，任务标签置首)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WordListFilter.values().forEach { filter ->
                val count = when (filter) {
                    WordListFilter.TASK -> taskCount
                    WordListFilter.ALL -> allCount
                    WordListFilter.MISTAKE -> mistakeCount
                    WordListFilter.UNLEARNED -> unlearnedCount
                    WordListFilter.DUE -> dueCount
                    WordListFilter.MASTERED -> masteredCount
                    WordListFilter.FAVORITE -> favCount
                }
                val isSelected = selectedFilter == filter
                val chipTitle = if (filter == WordListFilter.TASK) "任务${dailyTargetGroups}×${dailyWordsPerGroup}" else filter.title
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = "$chipTitle ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("wordlist_filter_${filter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 核心滑动容器：左右滑动切换分类 ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val pageFilter = WordListFilter.values()[pageIndex]

            val masteredWordsList = remember(activeWords, searchQuery) {
                val mastered = activeWords.filter { it.isMastered }
                if (searchQuery.isNotBlank()) {
                    val query = searchQuery.trim().lowercase()
                    mastered.filter {
                        it.word.lowercase().contains(query) ||
                        it.meaning.lowercase().contains(query) || it.originalMeaning.lowercase().contains(query) ||
                        it.phonetic.lowercase().contains(query) ||
                        it.exampleSentence.lowercase().contains(query)
                    }
                } else {
                    mastered
                }
            }

            val pageGroups = remember(fixedWordGroups, pageFilter, searchQuery, masteredWordsList, now, sortOrder) {
                if (pageFilter == WordListFilter.MASTERED) {
                    if (masteredWordsList.isNotEmpty()) {
                        listOf(
                            WordFixedGroup(
                                groupIndex = 999,
                                tag = "已标熟",
                                words = masteredWordsList,
                                unlearnedCount = 0,
                                dueCount = 0,
                                learnedCount = masteredWordsList.size,
                                isTaskGroup = false,
                                isReviewGroup = false
                            )
                        )
                    } else {
                        emptyList()
                    }
                } else {
                    fixedWordGroups.mapNotNull { group ->
                        val filteredWords = group.words.filter { word ->
                            val matchesSearch = if (searchQuery.isNotBlank()) {
                                val query = searchQuery.trim().lowercase()
                                word.word.lowercase().contains(query) ||
                                word.meaning.lowercase().contains(query) || word.originalMeaning.lowercase().contains(query) ||
                                word.phonetic.lowercase().contains(query) ||
                                word.exampleSentence.lowercase().contains(query)
                            } else true

                            matchesSearch && when (pageFilter) {
                                WordListFilter.TASK -> word.reviewStage == 0 && !word.isMastered
                                WordListFilter.ALL -> true
                                WordListFilter.MISTAKE -> word.mistakeCount > 0
                                WordListFilter.UNLEARNED -> word.reviewStage == 0 && !word.isMastered
                                WordListFilter.DUE -> word.nextReviewTime in 1..now && !word.isMastered
                                WordListFilter.FAVORITE -> word.isFavorite
                                else -> true
                            }
                        }

                        if (filteredWords.isNotEmpty()) {
                            val sortedWords = when (sortOrder) {
                                WordSortOrder.DEFAULT -> filteredWords
                                WordSortOrder.ALPHA -> filteredWords.sortedBy { it.word.lowercase() }
                                WordSortOrder.REVIEW_URGENT -> filteredWords.sortedWith(
                                    compareBy<WordEntity> { if (it.nextReviewTime in 1..now) 0 else 1 }
                                        .thenBy { it.nextReviewTime }
                                )
                            }

                            if (pageFilter == WordListFilter.TASK) {
                                if (group.isTaskGroup) {
                                    WordFixedGroup(
                                        groupIndex = group.groupIndex,
                                        tag = "L${group.groupIndex}",
                                        words = sortedWords,
                                        unlearnedCount = sortedWords.size,
                                        dueCount = 0,
                                        learnedCount = group.words.size - sortedWords.size,
                                        isTaskGroup = true,
                                        isReviewGroup = false
                                    )
                                } else null
                            } else {
                                WordFixedGroup(
                                    groupIndex = group.groupIndex,
                                    tag = "L${group.groupIndex}",
                                    words = sortedWords,
                                    unlearnedCount = sortedWords.count { it.reviewStage == 0 },
                                    dueCount = sortedWords.count { it.nextReviewTime in 1..now },
                                    learnedCount = sortedWords.count { it.reviewStage > 0 },
                                    isTaskGroup = group.isTaskGroup,
                                    isReviewGroup = group.isReviewGroup
                                )
                            }
                        } else null
                    }
                }
            }

            if (pageGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "没有找到包含「$searchQuery」的单词" else "当前筛选分类暂无单词",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (pageFilter) {
                                    WordListFilter.MISTAKE -> "太棒了！当前没有任何错词记录。"
                                    WordListFilter.DUE -> "当前所有单词均未到期，艾宾浩斯记忆保持良好！"
                                    WordListFilter.MASTERED -> "已标熟的单词会移出学习组。点击复原可恢复"
                                    WordListFilter.FAVORITE -> "点击单词右侧的爱心可加入收藏"
                                    WordListFilter.UNLEARNED -> "当前单词书的新词已全部进入背诵循环！"
                                    else -> "可从单词书管理导入更多单词书"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stats & Sort Bar & Expand/Collapse All
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Batch Expand / Collapse buttons
                            if (pageGroups.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isMultiSelectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    isMultiSelectMode = !isMultiSelectMode
                                                    if (!isMultiSelectMode) selectedGroupTags = emptySet()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (isMultiSelectMode) "退出多选" else "多选",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isMultiSelectMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // 「中文」按钮：点击选中后折叠状态显示中文，展开显示音标和英文
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (showChineseFirst) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showChineseFirst = !showChineseFirst }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                                .testTag("wordlist_toggle_chinese_btn")
                                        ) {
                                            Text(
                                                text = "中文",
                                                fontSize = 11.sp,
                                                fontWeight = if (showChineseFirst) FontWeight.Bold else FontWeight.Medium,
                                                color = if (showChineseFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isMultiSelectMode) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (selectedGroupTags.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        if (selectedGroupTags.isNotEmpty()) {
                                                            val selectedGroups = pageGroups.filter { selectedGroupTags.contains(it.tag) }
                                                            if (selectedGroups.isNotEmpty()) {
                                                                viewModel.playMultipleGroupsInPortablePlayer(selectedGroups)
                                                            }
                                                            isMultiSelectMode = false
                                                            selectedGroupTags = emptySet()
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Headphones,
                                                        contentDescription = null,
                                                        tint = if (selectedGroupTags.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "随身听",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (selectedGroupTags.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { collapsedGroupTags = emptySet() }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "全部展开",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { collapsedGroupTags = pageGroups.map { it.tag }.toSet() }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "全部折叠",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Words Grouped by 20 with Collapsible Headers
                    pageGroups.forEach { group ->
                        val isCollapsed = collapsedGroupTags.contains(group.tag)

                        // Group Header Card
                        item(key = "group_header_${group.tag}") {
                            val isSelected = selectedGroupTags.contains(group.tag)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (isMultiSelectMode) {
                                            selectedGroupTags = if (isSelected) selectedGroupTags - group.tag else selectedGroupTags + group.tag
                                        } else {
                                            collapsedGroupTags = if (isCollapsed) collapsedGroupTags - group.tag else collapsedGroupTags + group.tag
                                        }
                                    }
                                    .testTag("group_header_${group.tag}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMultiSelectMode && isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                    } else if (group.isTaskGroup) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    } else if (group.isReviewGroup) {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isMultiSelectMode && isSelected) MaterialTheme.colorScheme.primary
                                    else if (group.isTaskGroup) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isMultiSelectMode) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                                contentDescription = "Select",
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(end = 12.dp).size(24.dp)
                                            )
                                        }
                                        
                                        // Tag Badge
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 10.dp)
                                        ) {
                                            Text(
                                                text = group.tag,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    // 将“第 1 组”直接改成 list号 “L1”！符合极简、专业的背词体验
                                                    text = "L${group.groupIndex}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${group.words.size} 词",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (group.isTaskGroup) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "任务组",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (group.dueCount > 0) "新词 ${group.unlearnedCount} · 待复习 ${group.dueCount} · 已学 ${group.learnedCount}" else "新词 ${group.unlearnedCount} · 已学 ${group.learnedCount}",
                                                fontSize = 11.sp,
                                                color = if (group.dueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Action buttons for group
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // 随身听此组
                                        IconButton(
                                            onClick = { viewModel.playGroupInPortablePlayer(group) },
                                            modifier = Modifier.size(36.dp).testTag("play_group_${group.tag}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Headphones,
                                                contentDescription = "随身听此组",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        // 学习此组
                                        if (group.unlearnedCount > 0) {
                                            IconButton(
                                                onClick = { viewModel.startLearningGroup(group) },
                                                modifier = Modifier.size(36.dp).testTag("learn_group_${group.tag}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Psychology,
                                                    contentDescription = "学习此组",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }

                                        // 艾宾浩斯复习此组
                                        if (group.dueCount > 0 || group.learnedCount > 0) {
                                            IconButton(
                                                onClick = { viewModel.startReviewGroup(group) },
                                                modifier = Modifier.size(36.dp).testTag("review_group_${group.tag}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "复习此组",
                                                    tint = if (group.dueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }

                                        // 折叠/展开按钮
                                        IconButton(
                                            onClick = {
                                                collapsedGroupTags = if (isCollapsed) collapsedGroupTags - group.tag else collapsedGroupTags + group.tag
                                            },
                                            modifier = Modifier.size(36.dp).testTag("toggle_fold_${group.tag}")
                                        ) {
                                            Icon(
                                                imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                contentDescription = if (isCollapsed) "展开此组" else "折叠此组",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 20个单词 (When not collapsed)
                        if (!isCollapsed) {
                            items(
                                items = group.words,
                                key = { "group_${group.tag}_${it.id}" }
                            ) { word ->
                                val isExpanded = expandedWordId == word.id
                                DetailedWordCard(
                                    word = word,
                                    showWord = showWord,
                                    showMeaning = showMeaning,
                                    showChineseFirst = showChineseFirst,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        expandedWordId = if (isExpanded) null else word.id
                                    },
                                    onPronounceWord = { viewModel.pronounceWord(word.word) },
                                    onPronounceWordUK = { viewModel.pronounceWordUK(word.word) },
                                    onToggleFavorite = { viewModel.toggleFavorite(word) },
                                    onToggleMastered = { viewModel.toggleMastered(word) }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

private fun formatTwoPhonetics(phoneticRaw: String): String {
    if (phoneticRaw.isBlank()) return ""
    val clean = phoneticRaw.trim()
    val matches = Regex("/[^/]+/\b|\\[[^\\]]+\\]").findAll(clean).map { it.value }.toList()
    if (matches.size >= 2) {
        return "${matches[0]}  ${matches[1]}"
    } else if (matches.isNotEmpty()) {
        return matches[0]
    }
    val parts = clean.split(Regex("[\\s;,]+")).filter { it.isNotBlank() }
    return parts.take(2).joinToString(" ")
}

@Composable
private fun DetailedWordCard(
    word: WordEntity,
    showWord: Boolean,
    showMeaning: Boolean,
    showChineseFirst: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPronounceWord: () -> Unit,
    onPronounceWordUK: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleMastered: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealWordPrivately by remember(word.id, showWord) { mutableStateOf(false) }
    var revealMeaningPrivately by remember(word.id, showMeaning) { mutableStateOf(false) }

    val isWordVisible = showWord || revealWordPrivately
    val isMeaningVisible = showMeaning || revealMeaningPrivately

    if (isExpanded) {
        LaunchedEffect(word.id) {
            onPronounceWordUK()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onPronounceWord() }
            .testTag("word_card_${word.word}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (word.isMastered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (word.isMastered) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Title (Word or Chinese based on showChineseFirst & fold/expand) & Right Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title Section
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rank = word.frequencyRank
                    if (rank != null && rank > 0) {
                        Text(
                            text = "$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }

                    if (showChineseFirst && !isExpanded) {
                        // 折叠状态下显示中文释义
                        Text(
                            text = word.meaning,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (word.isMastered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        // 正常模式或展开状态下显示英文单词
                        if (isWordVisible) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (word.isMastered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { revealWordPrivately = true }
                            ) {
                                Text(
                                    text = "●●●●● (点击查看)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Action Icons (Favorite, Mastered, Expand) - Speaker removed, Card click pronounces word
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (word.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleMastered,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (word.isMastered) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                            contentDescription = "标熟",
                            tint = if (word.isMastered) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "折叠" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 展开后在单词下面一行显示音标 (只显示两个音标)
            if (isExpanded) {
                val formattedPhonetic = formatTwoPhonetics(word.phonetic)
                if (formattedPhonetic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedPhonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }

                if (showChineseFirst) {
                    // 中文模式展开后：显示英文单词与中文释义
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = word.meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 正常模式下（未选中中文优先模式）：显示中文释义
            if (!showChineseFirst) {
                val shouldDisplayMeaning = showMeaning || isExpanded || revealMeaningPrivately
                if (shouldDisplayMeaning && word.meaning.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = word.meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 展开状态下的复习/错词统计面板
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "复习次数: ${word.reviewCount} 次 · 错词: ${word.mistakeCount} 次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val nextReviewText = if (word.isMastered) {
                            "已永久掌握"
                        } else if (word.nextReviewTime == 0L) {
                            "尚未开始"
                        } else {
                            val diffMinutes = (word.nextReviewTime - System.currentTimeMillis()) / (1000 * 60)
                            if (diffMinutes <= 0) "已到期待复习"
                            else if (diffMinutes < 60) "${diffMinutes} 分钟后"
                            else if (diffMinutes < 1440) "${diffMinutes / 60} 小时后"
                            else "${diffMinutes / 1440} 天后"
                        }
                        Text(
                            text = "下次复习: $nextReviewText",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
