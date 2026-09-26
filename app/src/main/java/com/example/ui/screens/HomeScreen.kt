package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WordEntity
import com.example.ui.components.WordDetailDialog
import com.example.ui.components.WordListItem
import com.example.ui.navigation.AppScreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenImportDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeBookId by viewModel.activeBookId.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val activeBook = allBooks.find { it.id == activeBookId }

    val activeWords by viewModel.wordsForActiveBook.collectAsStateWithLifecycle()
    val dueCount by viewModel.dueReviewCount.collectAsStateWithLifecycle()
    val totalWords by viewModel.totalWordsCount.collectAsStateWithLifecycle()
    val totalLearned by viewModel.totalLearnedCount.collectAsStateWithLifecycle()
    val totalMastered by viewModel.totalMasteredCount.collectAsStateWithLifecycle()

    val dailyWordsPerGroup by viewModel.dailyWordsPerGroup.collectAsStateWithLifecycle()
    val dailyTargetGroups by viewModel.dailyTargetGroups.collectAsStateWithLifecycle()
    val learnedDays by viewModel.learnedDaysCount.collectAsStateWithLifecycle()
    val fixedGroups by viewModel.fixedWordGroups.collectAsStateWithLifecycle()

    val totalInBook = if (activeWords.isNotEmpty()) activeWords.size else (activeBook?.totalWords ?: totalWords)
    val learnedInBook = activeWords.count { it.reviewStage > 0 || it.isMastered }
    val newInBook = if (activeWords.isNotEmpty()) {
        activeWords.count { it.reviewStage == 0 && !it.isMastered }
    } else {
        (totalInBook - totalLearned).coerceAtLeast(0)
    }
    val masteredInBook = activeWords.count { it.isMastered }

    val dailyNewWordsLimit = (dailyWordsPerGroup * dailyTargetGroups).coerceAtLeast(1)

    // 读取“全部计划”中的总组数（最后一个List编号）与已完成组数
    val totalGroups = fixedGroups.size
    val learnedGroupsCount = fixedGroups.count { it.unlearnedCount == 0 && it.words.isNotEmpty() }
    val remainingGroups = (totalGroups - learnedGroupsCount).coerceAtLeast(0)
    val targetGroupsPerDay = dailyTargetGroups.coerceAtLeast(1)

    // 剩余天数 = (总List数 - 已学习List数) / 每日计划学习组数，向上取整
    val daysRemaining = if (fixedGroups.isNotEmpty()) {
        if (remainingGroups > 0) {
            kotlin.math.ceil(remainingGroups.toDouble() / targetGroupsPerDay.toDouble()).toInt()
        } else {
            0
        }
    } else if (newInBook > 0) {
        kotlin.math.ceil(newInBook.toDouble() / dailyNewWordsLimit.toDouble()).toInt()
    } else {
        0
    }

    val progress = if (totalInBook > 0) (learnedInBook.toFloat() / totalInBook.toFloat()).coerceIn(0f, 1f) else 0f
    val progressPercent = (progress * 100).toInt()

    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val homeFocusRequester = remember { FocusRequester() }
    var activeDetailWord by remember { mutableStateOf<WordEntity?>(null) }

    val homeFilteredWords = remember(activeWords, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val query = searchQuery.trim().lowercase()
            activeWords.filter {
                it.word.lowercase().contains(query) ||
                it.meaning.lowercase().contains(query) || it.originalMeaning.lowercase().contains(query) ||
                it.phonetic.lowercase().contains(query) ||
                it.exampleSentence.lowercase().contains(query)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Search Bar (replaces all previous top buttons)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(homeFocusRequester)
                    .testTag("home_top_search_bar"),
                placeholder = { Text("搜索单词、释义或音标...", fontSize = 15.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }

        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "找到 ${homeFilteredWords.size} 个相关词汇",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(
                items = homeFilteredWords,
                key = { "home_search_${it.id}" }
            ) { word ->
                WordListItem(
                    word = word,
                    onPronounce = { viewModel.pronounceWord(word.word) },
                    onToggleFavorite = { viewModel.toggleFavorite(word) },
                    onClick = { activeDetailWord = word }
                )
            }
        } else {
            // Spaced Repetition (Ebbinghaus) Reminder Banner if due
            if (dueCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.startReview(30) }
                        .testTag("home_due_review_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "到期复习提醒",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "有 $dueCount 个单词已到期，点击即可复习巩固",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Today's Learning Target Centered Hero Card
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered Icon Header (Smaller)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Centered Title & Status
                    Text(
                        text = activeBook?.title ?: "今日学习目标",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "尚余 $newInBook 个新词 · 已学 $learnedInBook/$totalInBook 词 ($progressPercent%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Bar (Slimmer)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Centered Dual Action Buttons: 「学习」 and 「复习」
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 学习 Button
                        Button(
                            onClick = { viewModel.startLearning(15) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("start_learning_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "任务 ${dailyTargetGroups}×${dailyWordsPerGroup}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // 复习 Button
                        Button(
                            onClick = { viewModel.startReview(30) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("start_review_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (dueCount > 0) "复习 ($dueCount)" else "复习",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stats Row
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatCard(
                    title = "已学单词",
                    value = "$totalLearned",
                    subtitle = "当前单词书 $learnedInBook",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "剩余天数",
                    value = "$daysRemaining 天",
                    subtitle = "已学习 ${learnedDays} 天",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "标熟(已掌握)",
                    value = "$totalMastered",
                    subtitle = "当前单词书 $masteredInBook",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Word Matching Game Widget (Moved to bottom)
        item {
            WordMatchingGame(viewModel = viewModel)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    }

    if (activeDetailWord != null) {
        WordDetailDialog(
            word = activeDetailWord!!,
            viewModel = viewModel,
            onDismiss = { activeDetailWord = null }
        )
    }
}

@Composable
private fun WordMatchingGame(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val words by viewModel.matchingGameWords.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.isMatchingGameSoundEnabled.collectAsStateWithLifecycle()

    // Game logic states
    var selectedWord by remember { mutableStateOf<WordEntity?>(null) }
    var selectedMeaning by remember { mutableStateOf<WordEntity?>(null) }
    var matchedWords by remember { mutableStateOf(setOf<Long>()) }

    // Shuffled words and meanings
    val shuffledWords = remember(words) { words.shuffled() }
    val shuffledMeanings = remember(words) { words.shuffled() }

    // Reset when words change
    LaunchedEffect(words) {
        matchedWords = emptySet()
        selectedWord = null
        selectedMeaning = null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "单词消消乐 (5 词挑战)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleMatchingGameSound() }) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "声音切换",
                            modifier = Modifier.size(18.dp),
                            tint = if (soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.refreshMatchingGame() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (words.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("暂无足够数据，快去学习吧！", style = MaterialTheme.typography.bodySmall)
                }
            } else if (matchedWords.size == words.size) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("恭喜完成！🎉", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.refreshMatchingGame() }) {
                        Text("换一组")
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Left column: Words
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shuffledWords.forEach { word ->
                            val isMatched = matchedWords.contains(word.id)
                            val isSelected = selectedWord?.id == word.id

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isMatched) Color.Transparent
                                        else if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isMatched) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = !isMatched) {
                                        selectedWord = if (isSelected) null else word
                                        
                                        // Pronounce if sound enabled
                                        if (selectedWord != null && soundEnabled) {
                                            viewModel.pronounceWord(word.word)
                                        }

                                        // Check match
                                        if (selectedWord != null && selectedMeaning != null) {
                                            if (selectedWord!!.id == selectedMeaning!!.id) {
                                                val matched = selectedWord!!
                                                matchedWords = matchedWords + matched.id
                                                viewModel.onWordMatchedInMatchingGame(matched)
                                                selectedWord = null
                                                selectedMeaning = null
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMatched) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            else if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Right column: Meanings
                    Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shuffledMeanings.forEach { word ->
                            val isMatched = matchedWords.contains(word.id)
                            val isSelected = selectedMeaning?.id == word.id

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isMatched) Color.Transparent
                                        else if (isSelected) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isMatched) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = !isMatched) {
                                        selectedMeaning = if (isSelected) null else word
                                        // Check match
                                        if (selectedWord != null && selectedMeaning != null) {
                                            if (selectedWord!!.id == selectedMeaning!!.id) {
                                                val matched = selectedWord!!
                                                matchedWords = matchedWords + matched.id
                                                viewModel.onWordMatchedInMatchingGame(matched)
                                                selectedWord = null
                                                selectedMeaning = null
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val displayMeaning = word.meaning.split("；").firstOrNull()?.split("，")?.firstOrNull() ?: word.meaning
                                Text(
                                    text = displayMeaning,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    color = if (isMatched) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            else if (isSelected) MaterialTheme.colorScheme.onSecondary
                                            else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
