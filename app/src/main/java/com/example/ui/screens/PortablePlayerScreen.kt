package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SentenceVoiceType
import com.example.data.model.PortableAudioMode
import com.example.data.model.PortableContentItem
import com.example.data.model.PortablePlayPhase
import com.example.data.model.PortableVoiceSelection
import com.example.data.model.WordEntity
import com.example.data.model.WordFixedGroup
import com.example.ui.navigation.AppScreen
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PortablePlayerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeWords by viewModel.wordsForActiveBook.collectAsStateWithLifecycle()
    val playlist by viewModel.portablePlaylist.collectAsStateWithLifecycle()
    val currentIndex by viewModel.portableCurrentIndex.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPortablePlaying.collectAsStateWithLifecycle()
    val statusText by viewModel.portablePlayStatusText.collectAsStateWithLifecycle()
    val playPhase by viewModel.portablePlayPhase.collectAsStateWithLifecycle()

    val currentCategory by viewModel.portablePlayingCategory.collectAsStateWithLifecycle()
    val audioMode by viewModel.portableAudioMode.collectAsStateWithLifecycle()
    val selectedContentItems by viewModel.portableSelectedContentItems.collectAsStateWithLifecycle()
    val voiceSelection by viewModel.portableVoiceSelection.collectAsStateWithLifecycle()
    val voiceProfiles by viewModel.voiceProfiles.collectAsStateWithLifecycle()
    val portableEnabledVoiceIds by viewModel.portableEnabledProfileIds.collectAsStateWithLifecycle()
    val portableEnabledSentenceVoiceTypes by viewModel.portableEnabledSentenceVoiceTypes.collectAsStateWithLifecycle()
    val pauseSeconds by viewModel.portablePauseSeconds.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()
    val allCount = activeWords.size
    val unlearnedCount = activeWords.count { it.reviewStage == 0 && !it.isMastered }
    val dueCount = activeWords.count { it.nextReviewTime in 1..now && !it.isMastered }
    val masteredCount = activeWords.count { it.isMastered }
    val favCount = activeWords.count { it.isFavorite }

    val fixedWordGroups by viewModel.fixedWordGroups.collectAsStateWithLifecycle()
    val taskGroups = remember(fixedWordGroups) { fixedWordGroups.filter { it.isTaskGroup } }
    val taskGroup = fixedWordGroups.find { it.isTaskGroup } ?: fixedWordGroups.firstOrNull()

    // 随身听当前列表按 Ebbinghaus 固定分组映射
    val playlistGroups = remember(playlist, fixedWordGroups, now) {
        fixedWordGroups.mapNotNull { group ->
            val wordsInPlaylist = group.words.filter { gw -> playlist.any { it.id == gw.id } }
            if (wordsInPlaylist.isNotEmpty()) {
                WordFixedGroup(
                    groupIndex = group.groupIndex,
                    tag = "L${group.groupIndex}",
                    words = wordsInPlaylist,
                    unlearnedCount = wordsInPlaylist.count { it.reviewStage == 0 },
                    dueCount = wordsInPlaylist.count { it.nextReviewTime in 1..now },
                    learnedCount = wordsInPlaylist.count { it.reviewStage > 0 },
                    isTaskGroup = group.isTaskGroup,
                    isReviewGroup = group.isReviewGroup
                )
            } else null
        }
    }

    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 播放时自动向上收起折叠顶部面板，释放最大展示空间；暂停或用户点击可随时展开
    var isHeaderExpanded by remember { mutableStateOf(!isPlaying) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            isHeaderExpanded = false
        }
    }

    BackHandler(enabled = showSettingsSheet) {
        showSettingsSheet = false
    }

    val listState = rememberLazyListState()

    // 自动居中滚动：当播放索引改变时，自动将当前正在播放的单词卡片保持在屏幕正中间
    LaunchedEffect(currentIndex, playlist.size, playlistGroups) {
        if (playlist.isNotEmpty() && currentIndex in playlist.indices) {
            // 计算当前正在播放的单词在 LazyColumn 中的准确索引位置
            var targetLazyIndex = 0
            val currentWordId = playlist[currentIndex].id
            for (g in playlistGroups) {
                val localIndex = g.words.indexOfFirst { it.id == currentWordId }
                if (localIndex != -1) {
                    targetLazyIndex += 1 + localIndex
                    break
                }
                targetLazyIndex += 1 + g.words.size
            }

            // 保持屏幕居中滚动
            listState.animateScrollToItem(
                index = targetLazyIndex.coerceAtLeast(0),
                scrollOffset = -260
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    viewModel.pausePortablePlayback()
                    viewModel.navigateTo(AppScreen.DASHBOARD)
                },
                modifier = Modifier.testTag("portable_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回首页"
                )
            }

            // Clickable header capsule - allows toggling full panel anytime
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (!isHeaderExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    if (!isHeaderExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isHeaderExpanded = !isHeaderExpanded }
                    .testTag("portable_header_toggle_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "随身听 (磨耳朵)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (playlist.isNotEmpty()) "${currentIndex + 1}/${playlist.size} 词 · $currentCategory" else "列表为空 · $currentCategory",
                            fontSize = 11.sp,
                            color = if (!isHeaderExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (!isHeaderExpanded) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isHeaderExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isHeaderExpanded) "收起面板" else "展开选组",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = { showSettingsSheet = true },
                modifier = Modifier.testTag("portable_settings_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "播放设置",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Animated collapsible header: 收起第 2 层与第 3 层，最大化释放内容展示空间
        AnimatedVisibility(
            visible = isHeaderExpanded,
            enter = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(280)),
            exit = shrinkVertically(animationSpec = tween(280)) + fadeOut(animationSpec = tween(280))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 一键收听快捷卡片 (一键收听任务组 / 一键收听复习组)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 一键收听任务组
                    val isTaskActive = currentCategory.contains("任务组") || (taskGroup != null && currentCategory == taskGroup.tag)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTaskActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isTaskActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.playTaskGroupInPortablePlayer() }
                            .testTag("portable_play_task_group_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "一键听任务组",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val taskTagsText = if (taskGroups.isNotEmpty()) taskGroups.joinToString(",") { it.tag } else (taskGroup?.tag ?: "1#")
                                val taskUnlearnedSum = if (taskGroups.isNotEmpty()) taskGroups.sumOf { it.unlearnedCount } else (taskGroup?.unlearnedCount ?: 0)
                                Text(
                                    text = "$taskTagsText · ${taskUnlearnedSum}新词",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 一键收听复习组
                    val isReviewActive = currentCategory == "复习组" || currentCategory == "待复习"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isReviewActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isReviewActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.playReviewGroupInPortablePlayer() }
                            .testTag("portable_play_review_group_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (dueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "一键听复习组",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (dueCount > 0) "到期 $dueCount 词" else "巩固已学",
                                    fontSize = 10.sp,
                                    color = if (dueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 自由选择某组单词进行听 (All groups 1#, 2#, 3#... + category chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 全部
                    FilterChip(
                        selected = currentCategory == "全部",
                        onClick = { viewModel.setPortableCategory("全部") },
                        label = { Text("全部 ($allCount)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("portable_filter_全部")
                    )

                    // 自由选择固定组 (1#, 2#, 3#...)
                    fixedWordGroups.forEach { group ->
                        val isSelected = currentCategory == group.tag || (currentCategory.contains("任务组") && group.isTaskGroup)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.playFixedGroupInPortablePlayer(group) },
                            label = {
                                Text(
                                    text = if (group.isTaskGroup) "${group.tag} 🎯" else group.tag,
                                    fontSize = 11.sp,
                                    fontWeight = if (group.isTaskGroup || isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("portable_filter_group_${group.tag}")
                        )
                    }

                    // 未学
                    FilterChip(
                        selected = currentCategory == "未学",
                        onClick = { viewModel.setPortableCategory("未学") },
                        label = { Text("未学 ($unlearnedCount)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 待复习
                    FilterChip(
                        selected = currentCategory == "待复习",
                        onClick = { viewModel.setPortableCategory("待复习") },
                        label = { Text("待复习 ($dueCount)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 已收藏
                    FilterChip(
                        selected = currentCategory == "已收藏",
                        onClick = { viewModel.setPortableCategory("已收藏") },
                        label = { Text("已收藏 ($favCount)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Word List / Lyrics Teleprompter Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (playlist.isEmpty()) {
                // Empty state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "当前【$currentCategory】分类暂无单词",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "请点击上方有词汇的组标签（如【L1】或【全部】），即可立即开始听音磨耳朵。",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.setPortableCategory("全部") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("切换到全部单词")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header with count
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "随身听列表 · ${playlist.size} 词 · ${playlistGroups.size} 组",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Groups: L1, L2 ...
                    playlistGroups.forEach { group ->
                        val firstWord = group.words.firstOrNull()
                        val groupStartIndex = if (firstWord != null) playlist.indexOfFirst { it.id == firstWord.id }.coerceAtLeast(0) else 0
                        val isPlayingInThisGroup = playlist.isNotEmpty() && currentIndex in playlist.indices && group.words.any { it.id == playlist[currentIndex].id }

                        // Group Header Card
                        item(key = "portable_group_header_${group.tag}") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("portable_group_header_${group.tag}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlayingInThisGroup) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPlayingInThisGroup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Tag badge
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isPlayingInThisGroup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(end = 10.dp)
                                        ) {
                                            Text(
                                                text = group.tag,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "第 ${group.groupIndex} 组 (${group.words.size} 词)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isPlayingInThisGroup) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primary
                                                    ) {
                                                        Text(
                                                            text = "播放中 🎵",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // 播放本组
                                        IconButton(
                                            onClick = {
                                                viewModel.seekPortableWord(groupStartIndex)
                                                if (!isPlaying) {
                                                    viewModel.startPortablePlayback()
                                                }
                                            },
                                            modifier = Modifier.size(34.dp).testTag("play_group_btn_${group.tag}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "播放此组",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // words inside group
                        itemsIndexed(
                            items = group.words,
                            key = { _, word -> "portable_${group.tag}_${word.id}" }
                        ) { _, word ->
                            val actualIndex = playlist.indexOfFirst { it.id == word.id }.coerceAtLeast(0)
                            val isCurrent = (actualIndex == currentIndex)

                            PortableWordCard(
                                word = word,
                                actualIndex = actualIndex,
                                isCurrent = isCurrent,
                                isPlaying = isPlaying,
                                playPhase = playPhase,
                                selectedContentItems = selectedContentItems,
                                onSelectWord = { viewModel.temporaryPlayWord(actualIndex) },
                                onToggleFavorite = { viewModel.toggleFavorite(word) },
                                onToggleMastered = { viewModel.toggleMastered(word) }
                            )
                        }
                    }

                    // Bottom spacer for floating control bar
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Docked Bottom Player Controls Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .testTag("portable_bottom_controls_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Control Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    IconButton(
                        onClick = { viewModel.prevPortableWord() },
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("portable_prev_btn")
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "上一词",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Large Play / Pause Button
                    IconButton(
                        onClick = { viewModel.togglePortablePlayPause() },
                        modifier = Modifier
                            .size(62.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("portable_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = { viewModel.nextPortableWord() },
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("portable_next_btn")
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "下一词",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Isolated Modal Bottom Sheet for Portable Settings - prevents freezes / layout crashes!
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "随身听播放配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showSettingsSheet = false }) {
                        Icon(Icons.Default.Check, contentDescription = "完成")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // 1. 播放内容模式 (多选：单词、中释、英释、例句1、例句2、例句3)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("播放内容模式 (多选):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "已选 ${selectedContentItems.size}/6 项",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PortableContentItem.values().forEach { item ->
                            val isSelected = selectedContentItems.contains(item)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.togglePortableContentItem(item) },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("portable_content_item_${item.id}")
                            )
                        }
                    }
                }

                // 2. 单词发音 (朗文, 有道美音, 有道英音)
                Text("单词发音:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val wordOptions = listOf(
                        "voice_longman_us" to "朗文",
                        "voice_youdao_us" to "有道美音",
                        "voice_youdao_uk" to "有道英音"
                    )
                    wordOptions.forEach { (id, name) ->
                        val isSelected = id in portableEnabledVoiceIds
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.togglePortableVoiceProfile(id) },
                            label = { Text(name, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // 3. 例句发音 (TTS1, TTS2, TTS3)
                Text("例句发音:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sentenceVoiceOptions = listOf(
                        SentenceVoiceType.TTS_1 to "TTS1",
                        SentenceVoiceType.TTS_2 to "TTS2",
                        SentenceVoiceType.TTS_3 to "TTS3"
                    )
                    sentenceVoiceOptions.forEach { (type, name) ->
                        val isSelected = type in portableEnabledSentenceVoiceTypes
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.togglePortableSentenceVoiceType(type) },
                            label = { Text(name, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // 4. 词间停顿秒数 (默认 0.5 秒，支持 0 ~ 5 秒自由滑动切换与快捷标签)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("词间停顿时间:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        val displaySecStr = if (pauseSeconds <= 0f) "0秒 (无停顿)" else "${if (pauseSeconds == pauseSeconds.toInt().toFloat()) "${pauseSeconds.toInt()}" else "$pauseSeconds"}秒"
                        Text(
                            text = displaySecStr,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    androidx.compose.material3.Slider(
                        value = pauseSeconds,
                        onValueChange = { rawVal ->
                            val stepped = (Math.round(rawVal * 2.0f) / 2.0f) // 0.5s step
                            viewModel.setPortablePauseSeconds(stepped)
                        },
                        valueRange = 0f..5f,
                        steps = 9,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("portable_pause_slider")
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0f, 0.5f, 1f, 1.5f, 2f, 3f, 5f).forEach { sec ->
                            val isSelected = Math.abs(pauseSeconds - sec) < 0.05f
                            val label = when (sec) {
                                0f -> "0秒"
                                0.5f -> "0.5秒(默认)"
                                else -> if (sec == sec.toInt().toFloat()) "${sec.toInt()}秒" else "${sec}秒"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setPortablePauseSeconds(sec) },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showSettingsSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("完成设置")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PortableWordCard(
    word: WordEntity,
    actualIndex: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    playPhase: PortablePlayPhase,
    selectedContentItems: Set<PortableContentItem> = PortableContentItem.DEFAULT_SELECTION,
    onSelectWord: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleMastered: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLocallyExpanded by remember(word.id) { mutableStateOf(false) }

    val shouldShowMeaning = isLocallyExpanded || selectedContentItems.contains(PortableContentItem.CHINESE_MEANING)
    val shouldShowEnglishDef = isLocallyExpanded || selectedContentItems.contains(PortableContentItem.ENGLISH_DEFINITION)
    val shouldShowSentence1 = isLocallyExpanded || selectedContentItems.contains(PortableContentItem.SENTENCE_1)
    val shouldShowSentence2 = isLocallyExpanded || selectedContentItems.contains(PortableContentItem.SENTENCE_2)
    val shouldShowSentence3 = isLocallyExpanded || selectedContentItems.contains(PortableContentItem.SENTENCE_3)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectWord() }
            .testTag("portable_word_item_$actualIndex"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrent) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top row: Index + Word + Phonetic + Frequency + Actions
            val isWordHighlight = isCurrent && (playPhase == PortablePlayPhase.WORD)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Digital index indicator
                    val displayRank = word.frequencyRank ?: (actualIndex + 1)
                    Text(
                        text = "$displayRank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isWordHighlight) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                    }

                    // Word text
                    Text(
                        text = word.word,
                        style = if (isCurrent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isWordHighlight || isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    // Phonetic
                    if (isLocallyExpanded && word.phonetic.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = word.phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }

                // Action buttons: Star, Mastered, Collapse
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Favorite
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (word.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (word.isFavorite) "取消收藏" else "收藏",
                            tint = if (word.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 标熟
                    IconButton(
                        onClick = onToggleMastered,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (word.isMastered) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                            contentDescription = if (word.isMastered) "取消标熟" else "标熟 (移出此组)",
                            tint = if (word.isMastered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 最右边 折叠/展开 按钮
                    IconButton(
                        onClick = { isLocallyExpanded = !isLocallyExpanded },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isLocallyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isLocallyExpanded) "折叠" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 1. 中文释义 (根据多选模式显示/隐藏)
            if (shouldShowMeaning && word.meaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val isMeaningHighlight = isCurrent && (playPhase == PortablePlayPhase.CHINESE_MEANING || playPhase == PortablePlayPhase.MEANING)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isMeaningHighlight) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(if (isMeaningHighlight) 6.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMeaningHighlight) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = word.meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isMeaningHighlight) FontWeight.Bold else FontWeight.Normal,
                            color = if (isMeaningHighlight) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = if (isCurrent) 4 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2. 英文释义 (根据多选模式显示/隐藏)
            if (shouldShowEnglishDef && word.definition.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val isEnDefHighlight = isCurrent && (playPhase == PortablePlayPhase.ENGLISH_DEFINITION)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEnDefHighlight) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (isEnDefHighlight) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(end = 4.dp, top = 2.dp)
                            )
                        }
                        Text(
                            text = "[英释] " + word.definition.replace("\n", "; ").trim(),
                            fontSize = 11.5.sp,
                            fontWeight = if (isEnDefHighlight) FontWeight.Bold else FontWeight.Normal,
                            color = if (isEnDefHighlight) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = if (isLocallyExpanded) 6 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. 例句展示 (例句1, 例句2, 例句3 按需展示)
            val sents = if (word.exampleSentence.isNotBlank()) {
                word.exampleSentence.split(" ||| ").filter { it.isNotBlank() }
            } else emptyList()
            val trans = if (word.exampleTranslation.isNotBlank()) {
                word.exampleTranslation.split(" ||| ")
            } else emptyList()

            val sentencesToShow = mutableListOf<Triple<Int, String, String>>()
            if (sents.isNotEmpty() && shouldShowSentence1) {
                sentencesToShow.add(Triple(0, sents[0], trans.getOrNull(0) ?: ""))
            }
            if (sents.size >= 2 && shouldShowSentence2) {
                sentencesToShow.add(Triple(1, sents[1], trans.getOrNull(1) ?: ""))
            }
            if (sents.size >= 3 && shouldShowSentence3) {
                sentencesToShow.add(Triple(2, sents[2], trans.getOrNull(2) ?: ""))
            }

            if (sentencesToShow.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sentencesToShow.forEach { (sIdx, sent, tr) ->
                        val isSentHighlight = isCurrent && when (sIdx) {
                            0 -> playPhase == PortablePlayPhase.SENTENCE_1 || playPhase == PortablePlayPhase.SENTENCE
                            1 -> playPhase == PortablePlayPhase.SENTENCE_2
                            2 -> playPhase == PortablePlayPhase.SENTENCE_3
                            else -> false
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSentHighlight) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = if (isSentHighlight) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSentHighlight) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .padding(end = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "例句${sIdx + 1}: $sent",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSentHighlight) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSentHighlight) {
                                            MaterialTheme.colorScheme.primary
                                        } else if (isCurrent) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                if (tr.isNotBlank()) {
                                    Text(
                                        text = tr,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(start = if (isSentHighlight) 18.dp else 0.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
