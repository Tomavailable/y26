package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EbbinghausDayPlan
import com.example.data.model.EbbinghausPlanGenerator
import com.example.data.model.GroupPlanStatus
import com.example.data.model.WordFixedGroup
import com.example.ui.navigation.AppScreen
import com.example.ui.theme.StatusInProgressBgDark
import com.example.ui.theme.StatusInProgressBgLight
import com.example.ui.theme.StatusInProgressTextDark
import com.example.ui.theme.StatusInProgressTextLight
import com.example.ui.theme.StatusLearnedBgDark
import com.example.ui.theme.StatusLearnedBgLight
import com.example.ui.theme.StatusLearnedTextDark
import com.example.ui.theme.StatusLearnedTextLight
import com.example.ui.theme.StatusUnlearnedBgDark
import com.example.ui.theme.StatusUnlearnedBgLight
import com.example.ui.theme.StatusUnlearnedTextDark
import com.example.ui.theme.StatusUnlearnedTextLight
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPlanScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeBookId by viewModel.activeBookId.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val activeBook = allBooks.find { it.id == activeBookId }

    val fixedGroups by viewModel.fixedWordGroups.collectAsStateWithLifecycle()
    val wordsPerGroup by viewModel.dailyWordsPerGroup.collectAsStateWithLifecycle()

    val totalGroups = fixedGroups.size
    val ebbinghausPlans = remember(totalGroups) {
        EbbinghausPlanGenerator.generatePlan(totalGroups)
    }

    var selectedGroupForAction by remember { mutableStateOf<WordFixedGroup?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showGroupSizeDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showGroupSizeDialog) {
        showGroupSizeDialog = false
    }

    BackHandler(enabled = showMenu) {
        showMenu = false
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val horizontalScrollState = rememberScrollState()
    val verticalListState = rememberLazyListState()

    // 统计各状态组数
    val learnedGroupsCount = fixedGroups.count { it.unlearnedCount == 0 && it.words.isNotEmpty() }
    val inProgressGroups = remember(fixedGroups) { fixedGroups.filter { it.isTaskGroup } }
    val unlearnedGroupsCount = fixedGroups.count { it.learnedCount == 0 && it.unlearnedCount > 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部导航栏（对齐截图样式：< 标题 ...）
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.WORD_LIST) },
                        modifier = Modifier.testTag("all_plan_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = activeBook?.title ?: "全部计划",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "共 $totalGroups 组 · 每组 $wordsPerGroup 词 · 每天最多复习 5 组",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("all_plan_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多设置",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("设置每组单词数 ($wordsPerGroup 词)") },
                                onClick = {
                                    showMenu = false
                                    showGroupSizeDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Tune, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("前往单词表") },
                                onClick = {
                                    showMenu = false
                                    viewModel.navigateTo(AppScreen.WORD_LIST)
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("打开随身听") },
                                onClick = {
                                    showMenu = false
                                    viewModel.navigateTo(AppScreen.PORTABLE_PLAYER)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Headphones, contentDescription = null)
                                }
                            )
                        }
                    }
                }

                // 图例说明 (用不同的颜色标注：已学习 / 正学习 / 未学习)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 已学习
                    StatusLegendItem(
                        label = "已学习 ($learnedGroupsCount 组)",
                        bgColor = StatusLearnedBgLight,
                        textColor = StatusLearnedTextLight,
                        hasCheck = true
                    )

                    // 正学习
                    val inProgressTagsText = if (inProgressGroups.isNotEmpty()) inProgressGroups.joinToString(",") { it.tag } else "完成"
                    StatusLegendItem(
                        label = "正学习 ($inProgressTagsText)",
                        bgColor = StatusInProgressBgLight,
                        textColor = StatusInProgressTextLight,
                        hasTarget = true
                    )

                    // 未学习
                    StatusLegendItem(
                        label = "未学习 ($unlearnedGroupsCount 组)",
                        bgColor = StatusUnlearnedBgLight,
                        textColor = StatusUnlearnedTextLight,
                        hasCheck = false
                    )
                }
            }
        }

        if (totalGroups == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当前单词书暂无单词",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // 艾宾浩斯总计划表格（横向 + 纵向滚动）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                val dayColWidth = 72.dp
                val contentColWidth = 84.dp
                val tableWidth = dayColWidth + (contentColWidth * 6) + 2.dp
                val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)

                Column(modifier = Modifier.width(tableWidth)) {
                    // 表头 Row: [空白] | 记忆 | 复习1 | 复习2 | 复习3 | 复习4 | 复习5
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Col 0: 空白 / Day
                            Box(
                                modifier = Modifier
                                    .width(dayColWidth)
                                    .fillMaxSize()
                                    .border(BorderStroke(0.5.dp, borderColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Col 1: 记忆
                            Box(
                                modifier = Modifier
                                    .width(contentColWidth)
                                    .fillMaxSize()
                                    .border(BorderStroke(0.5.dp, borderColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "记忆",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Col 2..6: 复习1 .. 复习5
                            for (r in 1..5) {
                                Box(
                                    modifier = Modifier
                                        .width(contentColWidth)
                                        .fillMaxSize()
                                        .border(BorderStroke(0.5.dp, borderColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "复习$r",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 表体 Rows
                    LazyColumn(
                        state = verticalListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(ebbinghausPlans, key = { it.dayNumber }) { dayPlan ->
                            EbbinghausTableRow(
                                dayPlan = dayPlan,
                                fixedGroups = fixedGroups,
                                dayColWidth = dayColWidth,
                                contentColWidth = contentColWidth,
                                borderColor = borderColor,
                                onSelectGroup = { groupIndex ->
                                    val group = fixedGroups.find { it.groupIndex == groupIndex }
                                    selectedGroupForAction = group
                                },
                                onSelectDay = { dayNum ->
                                    dayPlan.memorizeGroupIndex?.let { gIdx ->
                                        val group = fixedGroups.find { it.groupIndex == gIdx }
                                        selectedGroupForAction = group
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 单元格点击操作底板 (BottomSheet)
    if (selectedGroupForAction != null) {
        val group = selectedGroupForAction!!
        val status = viewModel.getGroupPlanStatus(group)

        ModalBottomSheet(
            onDismissRequest = { selectedGroupForAction = null },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "List ${group.groupIndex} (${group.tag})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            GroupStatusBadge(status = status)
                        }
                        Text(
                            text = "包含 ${group.words.size} 个单词",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(label = "未学新词", value = "${group.unlearnedCount}", color = MaterialTheme.colorScheme.primary)
                        StatItem(label = "待复习词", value = "${group.dueCount}", color = if (group.dueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        StatItem(label = "已学词数", value = "${group.learnedCount}", color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: 1. 学习本组 2. 艾宾浩斯复习 3. 随身听播放 4. 单词表中查看
                Button(
                    onClick = {
                        val g = selectedGroupForAction
                        selectedGroupForAction = null
                        if (g != null) {
                            viewModel.startLearningGroup(g)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("plan_action_learn_group_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("学习本组 (${group.unlearnedCount} 词待学)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val g = selectedGroupForAction
                        selectedGroupForAction = null
                        if (g != null) {
                            viewModel.startReviewGroup(g)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("plan_action_review_group_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("艾宾浩斯复习本组 (${group.dueCount} 词到期)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val g = selectedGroupForAction
                            selectedGroupForAction = null
                            if (g != null) {
                                viewModel.playGroupInPortablePlayer(g)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("plan_action_play_group_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("随身听播放")
                    }

                    OutlinedButton(
                        onClick = {
                            selectedGroupForAction = null
                            viewModel.navigateTo(AppScreen.WORD_LIST)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("plan_action_view_words_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("单词表")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // 设置每组词数弹窗
    if (showGroupSizeDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGroupSizeDialog = false },
            title = { Text("设定每日学习数量 (每组词数)") },
            text = {
                Column {
                    Text(
                        text = "根据艾宾浩斯学习规律，单词书将被严格划分为若干固定组 (List1, List2...)，每日以固定组为最小单位进行学习和复习。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "选择每组单词数量：", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val options = listOf(10, 15, 20, 25, 30, 40, 50)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.take(4).forEach { size ->
                            FilterChip(
                                selected = wordsPerGroup == size,
                                onClick = {
                                    viewModel.setDailyWordsPerGroup(size)
                                    showGroupSizeDialog = false
                                },
                                label = { Text("$size 词") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.drop(4).forEach { size ->
                            FilterChip(
                                selected = wordsPerGroup == size,
                                onClick = {
                                    viewModel.setDailyWordsPerGroup(size)
                                    showGroupSizeDialog = false
                                },
                                label = { Text("$size 词") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupSizeDialog = false }) {
                    Text("完成")
                }
            }
        )
    }
}

@Composable
fun EbbinghausTableRow(
    dayPlan: EbbinghausDayPlan,
    fixedGroups: List<WordFixedGroup>,
    dayColWidth: androidx.compose.ui.unit.Dp,
    contentColWidth: androidx.compose.ui.unit.Dp,
    borderColor: Color,
    onSelectGroup: (Int) -> Unit,
    onSelectDay: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day 标题列（可点击蓝色超链接样式）
            Box(
                modifier = Modifier
                    .width(dayColWidth)
                    .fillMaxSize()
                    .border(BorderStroke(0.5.dp, borderColor))
                    .clickable { onSelectDay(dayPlan.dayNumber) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Day ${dayPlan.dayNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 记忆列 (Col 1)
            Box(
                modifier = Modifier
                    .width(contentColWidth)
                    .fillMaxSize()
                    .border(BorderStroke(0.5.dp, borderColor)),
                contentAlignment = Alignment.Center
            ) {
                if (dayPlan.memorizeGroupIndex != null) {
                    val groupIndex = dayPlan.memorizeGroupIndex
                    val group = fixedGroups.find { it.groupIndex == groupIndex }
                    PlanCellItem(
                        groupIndex = groupIndex,
                        group = group,
                        onClick = { onSelectGroup(groupIndex) }
                    )
                }
            }

            // 复习列 (Col 2..6: 复习1 .. 复习5)
            for (colIdx in 0 until 5) {
                Box(
                    modifier = Modifier
                        .width(contentColWidth)
                        .fillMaxSize()
                        .border(BorderStroke(0.5.dp, borderColor)),
                    contentAlignment = Alignment.Center
                ) {
                    if (colIdx < dayPlan.reviewGroupIndices.size) {
                        val groupIndex = dayPlan.reviewGroupIndices[colIdx]
                        val group = fixedGroups.find { it.groupIndex == groupIndex }
                        PlanCellItem(
                            groupIndex = groupIndex,
                            group = group,
                            onClick = { onSelectGroup(groupIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanCellItem(
    groupIndex: Int,
    group: WordFixedGroup?,
    onClick: () -> Unit
) {
    val status = if (group == null) {
        GroupPlanStatus.UNLEARNED
    } else if (group.unlearnedCount == 0 && group.words.isNotEmpty()) {
        GroupPlanStatus.LEARNED
    } else if (group.isTaskGroup || (group.learnedCount > 0 && group.unlearnedCount > 0)) {
        GroupPlanStatus.IN_PROGRESS
    } else {
        GroupPlanStatus.UNLEARNED
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val (bgColor, textColor) = when (status) {
        GroupPlanStatus.LEARNED -> if (isDark) Pair(StatusLearnedBgDark, StatusLearnedTextDark) else Pair(StatusLearnedBgLight, StatusLearnedTextLight)
        GroupPlanStatus.IN_PROGRESS -> if (isDark) Pair(StatusInProgressBgDark, StatusInProgressTextDark) else Pair(StatusInProgressBgLight, StatusInProgressTextLight)
        GroupPlanStatus.UNLEARNED -> if (isDark) Pair(StatusUnlearnedBgDark, StatusUnlearnedTextDark) else Pair(StatusUnlearnedBgLight, StatusUnlearnedTextLight)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = if (status == GroupPlanStatus.IN_PROGRESS) BorderStroke(1.5.dp, textColor) else null,
        modifier = Modifier
            .padding(2.dp)
            .clickable { onClick() }
            .testTag("plan_cell_list_$groupIndex")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "List$groupIndex",
                fontSize = 12.sp,
                fontWeight = if (status == GroupPlanStatus.IN_PROGRESS) FontWeight.ExtraBold else FontWeight.Bold,
                color = textColor
            )

            if (status == GroupPlanStatus.LEARNED) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已完成",
                    tint = textColor,
                    modifier = Modifier.size(13.dp)
                )
            } else if (status == GroupPlanStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "●",
                    fontSize = 8.sp,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun StatusLegendItem(
    label: String,
    bgColor: Color,
    textColor: Color,
    hasCheck: Boolean = false,
    hasTarget: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = if (hasTarget) BorderStroke(1.dp, textColor) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            if (hasCheck) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun GroupStatusBadge(status: GroupPlanStatus) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val (bgColor, textColor) = when (status) {
        GroupPlanStatus.LEARNED -> if (isDark) Pair(StatusLearnedBgDark, StatusLearnedTextDark) else Pair(StatusLearnedBgLight, StatusLearnedTextLight)
        GroupPlanStatus.IN_PROGRESS -> if (isDark) Pair(StatusInProgressBgDark, StatusInProgressTextDark) else Pair(StatusInProgressBgLight, StatusInProgressTextLight)
        GroupPlanStatus.UNLEARNED -> if (isDark) Pair(StatusUnlearnedBgDark, StatusUnlearnedTextDark) else Pair(StatusUnlearnedBgLight, StatusUnlearnedTextLight)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = status.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
