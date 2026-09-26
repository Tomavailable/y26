package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.example.ui.theme.AppDayTheme
import com.example.ui.theme.CustomColorConfig
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SentenceVoiceType
import com.example.data.model.FirstListenVoicePreference
import com.example.data.model.LearningIntensity
import com.example.data.model.LearningMode
import com.example.data.model.PortableAudioMode
import com.example.data.model.PortableContentItem
import com.example.ui.components.DictionaryAndAudioSettingsCard
import com.example.ui.components.ReviewTimePickerDialog
import com.example.ui.navigation.AppScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isReminderEnabled by viewModel.isReminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()

    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val learningIntensity by viewModel.learningIntensity.collectAsStateWithLifecycle()
    val modeSwitches by viewModel.learningModeSwitches.collectAsStateWithLifecycle()
    val repeatTimes by viewModel.firstListenRepeatTimes.collectAsStateWithLifecycle()
    val voicePref by viewModel.firstListenVoicePref.collectAsStateWithLifecycle()

    val portableMode by viewModel.portableAudioMode.collectAsStateWithLifecycle()
    val portableSelectedContentItems by viewModel.portableSelectedContentItems.collectAsStateWithLifecycle()
    val portableEnabledVoiceIds by viewModel.portableEnabledProfileIds.collectAsStateWithLifecycle()
    val portableEnabledSentenceVoiceTypes by viewModel.portableEnabledSentenceVoiceTypes.collectAsStateWithLifecycle()

    val attachedMdxName by viewModel.attachedMdxName.collectAsStateWithLifecycle()
    val attachedMdxPath by viewModel.attachedMdxPath.collectAsStateWithLifecycle()
    val voiceProfiles by viewModel.voiceProfiles.collectAsStateWithLifecycle()
    val customBooks by viewModel.allCustomBooks.collectAsStateWithLifecycle()
    val defaultExampleSource by viewModel.defaultExampleSource.collectAsStateWithLifecycle()

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showCustomGroupSizeDialog by remember { mutableStateOf(false) }
    var showCustomTargetGroupsDialog by remember { mutableStateOf(false) }
    var showCustomReviewTargetDialog by remember { mutableStateOf(false) }
    var showBookImportDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showBookImportDialog) {
        showBookImportDialog = false
    }

    BackHandler(enabled = showCustomGroupSizeDialog) {
        showCustomGroupSizeDialog = false
    }

    BackHandler(enabled = showCustomTargetGroupsDialog) {
        showCustomTargetGroupsDialog = false
    }

    BackHandler(enabled = showCustomReviewTargetDialog) {
        showCustomReviewTargetDialog = false
    }

    BackHandler(enabled = showResetConfirmation) {
        showResetConfirmation = false
    }

    BackHandler(enabled = showTimePickerDialog) {
        showTimePickerDialog = false
    }

    // Notification Permission Launcher (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showMessage("通知权限已开启！")
            viewModel.triggerTestNotification()
        } else {
            viewModel.showMessage("未授予通知权限，请在系统设置中允许本应用发送通知")
        }
    }

    val themeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val dayTheme by viewModel.appDayTheme.collectAsStateWithLifecycle()
    val customColorConfig by viewModel.customColorConfig.collectAsStateWithLifecycle()
    val dailyGroupSize by viewModel.dailyWordsPerGroup.collectAsStateWithLifecycle()
    val dailyTargetGroups by viewModel.dailyTargetGroups.collectAsStateWithLifecycle()
    val dailyReviewTargetGroups by viewModel.dailyReviewTargetGroups.collectAsStateWithLifecycle()
    val selectedBuiltInDict by viewModel.selectedBuiltInDictionary.collectAsStateWithLifecycle()

    val reviewModeSwitches by viewModel.reviewLearningModeSwitches.collectAsStateWithLifecycle()
    val autoAdvanceOnCorrect by viewModel.autoAdvanceOnCorrectAnswer.collectAsStateWithLifecycle()
    val reviewAutoAdvance by viewModel.reviewAutoAdvanceOnCorrectAnswer.collectAsStateWithLifecycle()

    // Section fold/unfold states (all custom option cards default to collapsed as requested)
    var isThemeExpanded by remember { mutableStateOf(false) }
    var isAudioExpanded by remember { mutableStateOf(false) }
    var isBuiltInDictExpanded by remember { mutableStateOf(false) }
    var isPlanExpanded by remember { mutableStateOf(false) }
    var isLearnModeExpanded by remember { mutableStateOf(false) }
    var isModeSwitchesExpanded by remember { mutableStateOf(false) }
    var isReviewModeSwitchesExpanded by remember { mutableStateOf(false) }
    var isPortableSettingsExpanded by remember { mutableStateOf(false) }
    var isReminderExpanded by remember { mutableStateOf(false) }
    var isEbbinghausExpanded by remember { mutableStateOf(false) }
    var isCacheExpanded by remember { mutableStateOf(false) }
    var isAboutExpanded by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("确认重置所有数据？") },
            text = { Text("这将删除所有学习进度、收藏夹和自定义导入的单词书，并恢复到初始状态。此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAppData()
                        showResetConfirmation = false
                    }
                ) {
                    Text("确认重置", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showTimePickerDialog) {
        ReviewTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { hour, minute ->
                viewModel.setReminderTime(hour, minute)
                showTimePickerDialog = false
            }
        )
    }

    if (showCustomGroupSizeDialog) {
        CustomInputDialog(
            title = "自定义固定分组词数",
            label = "每组词数 (5 - 100)",
            initialValue = dailyGroupSize.toString(),
            onDismiss = { showCustomGroupSizeDialog = false },
            onConfirm = { value ->
                value.toIntOrNull()?.let { viewModel.setDailyWordsPerGroup(it) }
                showCustomGroupSizeDialog = false
            }
        )
    }

    if (showCustomTargetGroupsDialog) {
        CustomInputDialog(
            title = "自定义每日目标组数",
            label = "每日组数 (1 - 100)",
            initialValue = dailyTargetGroups.toString(),
            onDismiss = { showCustomTargetGroupsDialog = false },
            onConfirm = { value ->
                value.toIntOrNull()?.let { viewModel.setDailyTargetGroups(it) }
                showCustomTargetGroupsDialog = false
            }
        )
    }

    if (showCustomReviewTargetDialog) {
        CustomInputDialog(
            title = "自定义每日复习组数",
            label = "每日复习组数 (1 - 100)",
            initialValue = dailyReviewTargetGroups.toString(),
            onDismiss = { showCustomReviewTargetDialog = false },
            onConfirm = { value ->
                value.toIntOrNull()?.let { viewModel.setDailyReviewTargetGroups(it) }
                showCustomReviewTargetDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                modifier = Modifier.testTag("settings_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回首页"
                )
            }

            Text(
                text = "设置与偏好",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Box(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section 0A: Theme & Color Scheme Settings (经典黑红美学与全自由自定义调色盘)
        FoldableCard(
            title = "主题与色彩方案",
            subtitle = "${themeMode.label} · ${dayTheme.title}",
            icon = Icons.Default.Palette,
            isExpanded = isThemeExpanded,
            onToggle = { isThemeExpanded = !isThemeExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 1. 色彩模式 (日间/夜间/跟随系统)
                Text(
                    text = "色彩模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.values().forEach { mode ->
                        val isSelected = themeMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setAppThemeMode(mode) },
                            label = { Text(mode.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 主题方案选择卡片 (默认经典 / 经典黑红 / 全自由自定义)
                Text(
                    text = "主题配色方案切换",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppDayTheme.values().forEach { theme ->
                        val isSelected = dayTheme == theme
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setAppDayTheme(theme) }
                                .testTag("app_day_theme_${theme.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini preview palette with circles
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when (theme) {
                                        AppDayTheme.DEFAULT -> {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFF8FAFC),
                                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                                                modifier = Modifier.size(24.dp)
                                            ) {}
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF0F766E),
                                                border = BorderStroke(1.dp, Color.White),
                                                modifier = Modifier.size(24.dp)
                                            ) {}
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF2DD4BF),
                                                border = BorderStroke(1.dp, Color.White),
                                                modifier = Modifier.size(24.dp)
                                            ) {}
                                        }
                                        AppDayTheme.CRIMSON_OBSIDIAN -> {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFF8FAFC),
                                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                                                modifier = Modifier.size(24.dp)
                                            ) {}
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF0F172A),
                                                border = BorderStroke(1.dp, Color.White),
                                                modifier = Modifier.size(24.dp)
                                            ) {}
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFE11D48),
                                                border = BorderStroke(1.dp, Color.White),
                                                modifier = Modifier.size(24.dp)
                                            ) {}
                                        }
                                        AppDayTheme.CUSTOM -> {
                                            Surface(
                                                shape = CircleShape,
                                                color = customColorConfig.customBackground ?: Color(0xFFF8FAFC),
                                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                                                modifier = Modifier.size(24.dp)
                                            ) {}
                                            Surface(
                                                shape = CircleShape,
                                                color = customColorConfig.customPrimary ?: Color(0xFFE11D48),
                                                border = BorderStroke(1.dp, Color.White),
                                                modifier = Modifier.size(24.dp)
                                            ) {}
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = theme.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = theme.subtitle,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // 全自由自定义各部位色卡与透明度编辑区域
                if (dayTheme == AppDayTheme.CUSTOM || customColorConfig.isCustomEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🎨 各部位色彩与透明度调色盘",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "支持 RGB / Hex 与 0%~100% 透明度自定义调节",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = { viewModel.resetCustomColors() }) {
                            Text("恢复默认", fontSize = 12.sp)
                        }
                    }

                    // 1. 主背景底色
                    CustomColorPartEditor(
                        title = "1. 主背景底色 (Canvas Background)",
                        description = "控制整个应用窗口最底层的整体画布背景色及透明度",
                        currentColor = customColorConfig.customBackground ?: Color(0xFFF8FAFC),
                        quickPresets = listOf(
                            Color(0xFFF8FAFC) to "极简冷白",
                            Color(0xFFFFFFFF) to "纯白",
                            Color(0xFF0F172A) to "黑曜深黑",
                            Color(0xFF121212) to "原生暗夜",
                            Color(0xFFF1F5F9) to "雅灰冷霜",
                            Color(0xFFFFF1F2) to "浅淡绯红"
                        ),
                        onSelectColor = { color -> viewModel.updateCustomColor("background", color) }
                    )

                    // 2. 卡片表面色
                    CustomColorPartEditor(
                        title = "2. 卡片/容器表面色 (Surface Card)",
                        description = "控制单词大卡片、折叠面板、主窗口表面底色及透明度",
                        currentColor = customColorConfig.customSurface ?: Color(0xFFFFFFFF),
                        quickPresets = listOf(
                            Color(0xFFFFFFFF) to "纯白卡片",
                            Color(0xFF1E293B) to "黑曜深卡",
                            Color(0xFF1E1E24) to "原生暗卡",
                            Color(0xFFF1F5F9) to "雅灰轻卡",
                            Color(0xFF000000) to "纯黑卡片",
                            Color(0xFFFFF1F2) to "玫瑰浅卡"
                        ),
                        onSelectColor = { color -> viewModel.updateCustomColor("surface", color) }
                    )

                    // 3. 二级表面/气泡底色
                    CustomColorPartEditor(
                        title = "3. 二级气泡/表面色 (Surface Variant)",
                        description = "控制音标气泡、例句底框、状态徽章等次级背景及透明度",
                        currentColor = customColorConfig.customSurfaceVariant ?: Color(0xFFF1F5F9),
                        quickPresets = listOf(
                            Color(0xFFF1F5F9) to "轻灰气泡",
                            Color(0xFFE2E8F0) to "冷霜浅灰",
                            Color(0xFF27272A) to "暗夜气泡",
                            Color(0xFF334155) to "深岩气泡",
                            Color(0xFFFFE4E6) to "粉红气泡",
                            Color(0xFFE0E7FF) to "淡蓝气泡"
                        ),
                        onSelectColor = { color -> viewModel.updateCustomColor("surfaceVariant", color) }
                    )

                    // 4. 核心强调主色
                    CustomColorPartEditor(
                        title = "4. 核心强调主色 (Primary Accent)",
                        description = "控制按钮、进度条、播放主键、高亮边框重点色及透明度",
                        currentColor = customColorConfig.customPrimary ?: Color(0xFF0F766E),
                        quickPresets = listOf(
                            Color(0xFF0F766E) to "默认青绿",
                            Color(0xFF2DD4BF) to "暗夜青碧",
                            Color(0xFFE11D48) to "鲜亮绯红",
                            Color(0xFF0F172A) to "黑曜黑",
                            Color(0xFF2563EB) to "皇家蓝",
                            Color(0xFF7C3AED) to "电光紫",
                            Color(0xFF10B981) to "翡翠绿",
                            Color(0xFFF59E0B) to "活力橙"
                        ),
                        onSelectColor = { color -> viewModel.updateCustomColor("primary", color) }
                    )

                    // 5. 选中/播放高亮容器色
                    CustomColorPartEditor(
                        title = "5. 选中/高亮容器色 (Primary Container)",
                        description = "控制随身听当前行高亮底色、激活过滤标签底色及透明度",
                        currentColor = customColorConfig.customPrimaryContainer ?: Color(0xFFCCFBF1),
                        quickPresets = listOf(
                            Color(0xFFCCFBF1) to "清爽青绿",
                            Color(0xFFFFE4E6) to "浅淡绯红",
                            Color(0xFF134E48) to "深暗碧绿",
                            Color(0xFFE0E7FF) to "浅淡靛蓝",
                            Color(0xFFDCFCE7) to "浅淡翡翠",
                            Color(0xFFFEF3C7) to "浅淡暖橙"
                        ),
                        onSelectColor = { color -> viewModel.updateCustomColor("primaryContainer", color) }
                    )

                    // 6. 主要正文字迹颜色
                    CustomColorPartEditor(
                        title = "6. 正文字迹颜色 (On-Surface Text)",
                        description = "控制单词英文名、中文释义及卡片大标题文字色及透明度",
                        currentColor = customColorConfig.customOnSurface ?: Color(0xFF0F172A),
                        quickPresets = listOf(
                            Color(0xFF0F172A) to "黑曜深黑",
                            Color(0xFF000000) to "纯净极黑",
                            Color(0xFFF1F5F9) to "亮白纯白",
                            Color(0xFFFFFFFF) to "纯净亮白",
                            Color(0xFFE11D48) to "绯红字体",
                            Color(0xFF1E293B) to "深岩深灰"
                        ),
                        onSelectColor = { color -> viewModel.updateCustomColor("onSurface", color) }
                    )

                    // 7. 次级说明文字颜色
                    CustomColorPartEditor(
                        title = "7. 次级说明文字色 (On-Surface Variant)",
                        description = "控制音标、例句英文释义、副标题等次级文字色及透明度",
                        currentColor = customColorConfig.customOnSurfaceVariant ?: Color(0xFF475569),
                        quickPresets = listOf(
                            Color(0xFF475569) to "雅灰石墨",
                            Color(0xFF64748B) to "浅灰次字",
                            Color(0xFFA1A1AA) to "暗夜次字",
                            Color(0xFF94A3B8) to "冷灰次字",
                            Color(0xFF9F1239) to "深绯文字"
                        ),
                        onSelectColor = { color -> viewModel.updateCustomColor("onSurfaceVariant", color) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section 1: Word Pronunciation (单词发音 - 设置界面第二个选项)
        FoldableCard(
            title = "单词发音",
            subtitle = "7 大音源离线下载 · 独立本地缓存 · 实时 TTS",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            isExpanded = isAudioExpanded,
            onToggle = { isAudioExpanded = !isAudioExpanded }
        ) {
            DictionaryAndAudioSettingsCard(
                viewModel = viewModel,
                attachedMdxName = attachedMdxName,
                attachedMdxPath = attachedMdxPath,
                voiceProfiles = voiceProfiles
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section 2: Built-in Dictionary (内置词典 - 单独设置选项)
        FoldableCard(
            title = "内置词典",
            subtitle = "当前选择: ${selectedBuiltInDict.displayName} (${selectedBuiltInDict.tag})",
            icon = Icons.Default.AutoStories,
            isExpanded = isBuiltInDictExpanded,
            onToggle = { isBuiltInDictExpanded = !isBuiltInDictExpanded }
        ) {
            com.example.ui.components.BuiltInDictionarySettingsCard(
                viewModel = viewModel
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section 3: Learning Plan (包含固定分组、每日学习数量、每日复习数量)
        FoldableCard(
            title = "学习计划",
            subtitle = "新学 $dailyTargetGroups 组 (${dailyTargetGroups * dailyGroupSize}词) | 复习 $dailyReviewTargetGroups 组 (${dailyReviewTargetGroups * dailyGroupSize}词) | 每组 $dailyGroupSize 词",
            icon = Icons.Default.Flag,
            isExpanded = isPlanExpanded,
            onToggle = { isPlanExpanded = !isPlanExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. 固定分组词数
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "固定分组词数",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "每组 $dailyGroupSize 词",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "系统将单词书切分为固定组，作为学习与复习的基本单位：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 20, 30).forEach { size ->
                            val isSelected = dailyGroupSize == size
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setDailyWordsPerGroup(size) },
                                label = { Text("$size 词", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("group_size_$size")
                            )
                        }
                        val isCustomSelected = dailyGroupSize !in listOf(10, 20, 30)
                        FilterChip(
                            selected = isCustomSelected,
                            onClick = { showCustomGroupSizeDialog = true },
                            label = { Text(if (isCustomSelected) "$dailyGroupSize 词" else "自定义", fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("group_size_custom")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. 每日新学数量
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "每日新学组数",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$dailyTargetGroups 组 (${dailyTargetGroups * dailyGroupSize} 词)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "设定每日计划学习的新词组数：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3).forEach { size ->
                            val isSelected = dailyTargetGroups == size
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setDailyTargetGroups(size) },
                                label = { Text("$size 组", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("target_group_$size")
                            )
                        }
                        val isCustomSelected = dailyTargetGroups !in listOf(1, 2, 3)
                        FilterChip(
                            selected = isCustomSelected,
                            onClick = { showCustomTargetGroupsDialog = true },
                            label = { Text(if (isCustomSelected) "$dailyTargetGroups 组" else "自定义", fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("target_group_custom")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 3. 每日复习数量 (以组为单位)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "每日复习组数",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$dailyReviewTargetGroups 组 (${dailyReviewTargetGroups * dailyGroupSize} 词)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "设定每日计划复习的组数（以固定分组为单位）：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15).forEach { count ->
                            val isSelected = dailyReviewTargetGroups == count
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setDailyReviewTargetGroups(count) },
                                label = { Text("$count 组", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("review_target_$count")
                            )
                        }
                        val isCustomSelected = dailyReviewTargetGroups !in listOf(5, 10, 15)
                        FilterChip(
                            selected = isCustomSelected,
                            onClick = { showCustomReviewTargetDialog = true },
                            label = { Text(if (isCustomSelected) "$dailyReviewTargetGroups 组" else "自定义", fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("review_target_custom")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section: Learning Intensity (学习强度: 轻松 / 中等 / 强者)
        FoldableCard(
            title = "学习强度",
            subtitle = "${learningIntensity.title} (${learningIntensity.subtitle}) · ${learningIntensity.description}",
            icon = Icons.Default.Psychology,
            isExpanded = isLearnModeExpanded,
            onToggle = { isLearnModeExpanded = !isLearnModeExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "设定每个单词的目标考核轮次与过关标准：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LearningIntensity.values().forEach { intensity ->
                    val isSelected = learningIntensity == intensity
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setLearningIntensity(intensity) }
                            .testTag("learning_intensity_${intensity.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = intensity.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = intensity.subtitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = intensity.description,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section: Learning Mode Switches (包含全部7种模式开关，且至少开启一种模式)
        FoldableCard(
            title = "学习模式设置",
            subtitle = "${modeSwitches.enabledCount}/7 种模式开启 | ${if (autoAdvanceOnCorrect) "答对自动切题" else "答对看详情"}",
            icon = Icons.Default.ToggleOn,
            isExpanded = isModeSwitchesExpanded,
            onToggle = { isModeSwitchesExpanded = !isModeSwitchesExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 答对自动进入下一个
                SwitchRow(
                    title = "⏩ 答对自动进入下一个",
                    desc = "学习模式默认关闭（答对后也进入详情界面查看释义例句）；开启后答对直接切题",
                    checked = autoAdvanceOnCorrect,
                    onCheckedChange = { checked ->
                        viewModel.setAutoAdvanceOnCorrectAnswer(checked)
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Text(
                    text = "学习题型功能开关（支持自由组合，至少保留开启 1 种）：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mode 1: 盲听选义
                SwitchRow(
                    title = "1. 盲听选义",
                    desc = "只播发音不显示单词，从 4 个中文释义中快速选择",
                    checked = modeSwitches.enableBlindListenMeaning,
                    onCheckedChange = { checked ->
                        viewModel.updateLearningModeSwitches(modeSwitches.copy(enableBlindListenMeaning = checked))
                    }
                )

                // Mode 2: 看词选义
                SwitchRow(
                    title = "2. 看词选义",
                    desc = "展示英文单词，从 4 个中文释义中选择匹配项",
                    checked = modeSwitches.enableLookChooseMeaning,
                    onCheckedChange = { checked ->
                        viewModel.updateLearningModeSwitches(modeSwitches.copy(enableLookChooseMeaning = checked))
                    }
                )

                // Mode 3: 看义说词
                SwitchRow(
                    title = "3. 看义说词",
                    desc = "根据中文释义回忆发音与拼写，倒计时核对",
                    checked = modeSwitches.enableRecallByMeaning,
                    onCheckedChange = { checked ->
                        viewModel.updateLearningModeSwitches(modeSwitches.copy(enableRecallByMeaning = checked))
                    }
                )

                // Mode 4: 听音选词
                SwitchRow(
                    title = "4. 听音选词",
                    desc = "听单词发音，从 4 个易混拼写选项中选择正确英文单词",
                    checked = modeSwitches.enableListenChooseWord,
                    onCheckedChange = { checked ->
                        viewModel.updateLearningModeSwitches(modeSwitches.copy(enableListenChooseWord = checked))
                    }
                )

                // Mode 5: 听句选词
                SwitchRow(
                    title = "5. 听句选词",
                    desc = "听原声双语例句，结合例句语境选出挖空处的正确单词",
                    checked = modeSwitches.enableListenSentenceChooseWord,
                    onCheckedChange = { checked ->
                        viewModel.updateLearningModeSwitches(modeSwitches.copy(enableListenSentenceChooseWord = checked))
                    }
                )

                // Mode 6: 英文释义选词
                SwitchRow(
                    title = "6. 英文释义选词",
                    desc = "看英文柯林斯/朗文简明释义，选出对应正确英文单词",
                    checked = modeSwitches.enableEnglishMeaningChooseWord,
                    onCheckedChange = { checked ->
                        viewModel.updateLearningModeSwitches(modeSwitches.copy(enableEnglishMeaningChooseWord = checked))
                    }
                )

                // Mode 7: 盲听拼写
                SwitchRow(
                    title = "7. 盲听拼写",
                    desc = "听发音盲打拼写出完整英文单词 (共3次尝试机会)",
                    checked = modeSwitches.enableBlindSpelling,
                    onCheckedChange = { checked ->
                        viewModel.updateLearningModeSwitches(modeSwitches.copy(enableBlindSpelling = checked))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section: Review Mode Settings (复习模式设置 - 高效复习)
        FoldableCard(
            title = "复习模式设置 (高效复习)",
            subtitle = "${reviewModeSwitches.enabledCount}/7 种模式开启 (默认开启3种) | ${if (reviewAutoAdvance) "答对自动切题" else "答对看详情"}",
            icon = Icons.Default.Restore,
            isExpanded = isReviewModeSwitchesExpanded,
            onToggle = { isReviewModeSwitchesExpanded = !isReviewModeSwitchesExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "⚡ 复习模式注重高效：系统默认开启【答对自动进入下一个】，仅在打错时显示详情页；7 大题型默认只开启【盲听选义】、【看词选义】、【看义说词】3 种，支持任意组合配置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // 答对自动进入下一个
                SwitchRow(
                    title = "⏩ 答对自动进入下一个",
                    desc = "复习模式默认开启（极速切题，仅打错后显示详情页）；关闭后答对也显示详情页",
                    checked = reviewAutoAdvance,
                    onCheckedChange = { checked ->
                        viewModel.setReviewAutoAdvanceOnCorrectAnswer(checked)
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Text(
                    text = "复习题型功能开关（默认开启前3种，支持任意组合，至少保留开启 1 种）：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mode 1: 盲听选义
                SwitchRow(
                    title = "1. 盲听选义",
                    desc = "只播发音不显示单词，从 4 个中文释义中快速选择 (默认开启)",
                    checked = reviewModeSwitches.enableBlindListenMeaning,
                    onCheckedChange = { checked ->
                        viewModel.updateReviewLearningModeSwitches(reviewModeSwitches.copy(enableBlindListenMeaning = checked))
                    }
                )

                // Mode 2: 看词选义
                SwitchRow(
                    title = "2. 看词选义",
                    desc = "展示英文单词，从 4 个中文释义中选择匹配项 (默认开启)",
                    checked = reviewModeSwitches.enableLookChooseMeaning,
                    onCheckedChange = { checked ->
                        viewModel.updateReviewLearningModeSwitches(reviewModeSwitches.copy(enableLookChooseMeaning = checked))
                    }
                )

                // Mode 3: 看义说词
                SwitchRow(
                    title = "3. 看义说词",
                    desc = "根据中文释义回忆发音与拼写，倒计时核对 (默认开启)",
                    checked = reviewModeSwitches.enableRecallByMeaning,
                    onCheckedChange = { checked ->
                        viewModel.updateReviewLearningModeSwitches(reviewModeSwitches.copy(enableRecallByMeaning = checked))
                    }
                )

                // Mode 4: 听音选词
                SwitchRow(
                    title = "4. 听音选词",
                    desc = "听单词发音，从 4 个易混拼写选项中选择正确英文单词",
                    checked = reviewModeSwitches.enableListenChooseWord,
                    onCheckedChange = { checked ->
                        viewModel.updateReviewLearningModeSwitches(reviewModeSwitches.copy(enableListenChooseWord = checked))
                    }
                )

                // Mode 5: 听句选词
                SwitchRow(
                    title = "5. 听句选词",
                    desc = "听原声双语例句，结合例句语境选出挖空处的正确单词",
                    checked = reviewModeSwitches.enableListenSentenceChooseWord,
                    onCheckedChange = { checked ->
                        viewModel.updateReviewLearningModeSwitches(reviewModeSwitches.copy(enableListenSentenceChooseWord = checked))
                    }
                )

                // Mode 6: 英文释义选词
                SwitchRow(
                    title = "6. 英文释义选词",
                    desc = "看英文柯林斯/朗文简明释义，选出对应正确英文单词",
                    checked = reviewModeSwitches.enableEnglishMeaningChooseWord,
                    onCheckedChange = { checked ->
                        viewModel.updateReviewLearningModeSwitches(reviewModeSwitches.copy(enableEnglishMeaningChooseWord = checked))
                    }
                )

                // Mode 7: 盲听拼写
                SwitchRow(
                    title = "7. 盲听拼写",
                    desc = "听发音盲打拼写出完整英文单词 (共3次尝试机会)",
                    checked = reviewModeSwitches.enableBlindSpelling,
                    onCheckedChange = { checked ->
                        viewModel.updateReviewLearningModeSwitches(reviewModeSwitches.copy(enableBlindSpelling = checked))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section 3: Portable Player Settings (随身听设置)
        val selectedContentLabels = portableSelectedContentItems.joinToString("·") { it.label }
        FoldableCard(
            title = "随身听设置 (后台磨耳朵)",
            subtitle = if (portableSelectedContentItems.isNotEmpty()) "$selectedContentLabels · 多选发音" else "无内容 · 多选发音",
            icon = Icons.Default.Headphones,
            isExpanded = isPortableSettingsExpanded,
            onToggle = { isPortableSettingsExpanded = !isPortableSettingsExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "播放内容模式 (多选)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "已选 ${portableSelectedContentItems.size}/6 项",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PortableContentItem.values().forEach { item ->
                        val isSelected = item in portableSelectedContentItems
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
                            )
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text(
                    text = "单词发音",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text(
                    text = "例句发音",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section 4: Daily SRS Review Reminder
        FoldableCard(
            title = "每日复习提醒通知",
            subtitle = if (isReminderEnabled) "已开启 · 每日 %02d:%02d".format(reminderHour, reminderMinute) else "已关闭",
            icon = Icons.Default.NotificationsActive,
            isExpanded = isReminderExpanded,
            onToggle = { isReminderExpanded = !isReminderExpanded }
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "开启每日艾宾浩斯复习提醒",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "到期提醒复习，保持大脑记忆长效稳定",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setReminderEnabled(enabled)
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                        modifier = Modifier.testTag("reminder_toggle_switch")
                    )
                }

                if (isReminderEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Picker Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showTimePickerDialog = true }
                            .padding(vertical = 8.dp)
                            .testTag("change_reminder_time_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "每日提醒时间",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "点击修改提醒时间点",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "%02d:%02d".format(reminderHour, reminderMinute),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Notification Button
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.triggerTestNotification()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_notification_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("立即发送一条测试提醒通知")
                    }
                }
            }
        }



        Spacer(modifier = Modifier.height(14.dp))

        com.example.ui.components.ExampleSentenceSettingsCard(
            defaultSource = defaultExampleSource,
            customBooks = customBooks,
            onSetDefaultSource = { viewModel.setDefaultExampleSource(it) },
            onImportBookClick = { showBookImportDialog = true },
            onDeleteBook = { viewModel.deleteCustomBook(it) }
        )

        if (showBookImportDialog) {
            com.example.ui.components.BookExampleImportDialog(
                onDismiss = { showBookImportDialog = false },
                onImportBook = { title, fileName, rawText, onComplete ->
                    viewModel.importCustomBookText(title, fileName, rawText, onComplete)
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section 6: Ebbinghaus SRS Cycle Table
        FoldableCard(
            title = "艾宾浩斯 6 阶段复习周期表",
            subtitle = "8小时(次日) / 2天 / 4天 / 7天 / 15天 / 30天",
            icon = Icons.Default.Timeline,
            isExpanded = isEbbinghausExpanded,
            onToggle = { isEbbinghausExpanded = !isEbbinghausExpanded }
        ) {
            Column {
                Text(
                    text = "系统依据艾宾浩斯遗忘曲线，按天级跨度智能安排复习，助力高效转化为永久长时记忆：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    Pair("阶段 1", "8 小时后 (或第二天) · 次日早晚回忆强化"),
                    Pair("阶段 2", "2 天后 · 双日间隔巩固"),
                    Pair("阶段 3", "4 天后 · 中期记忆强化"),
                    Pair("阶段 4", "7 天后 · 周循环记忆固定"),
                    Pair("阶段 5", "15 天后 · 半月长效巩固"),
                    Pair("阶段 6", "30 天后 (1个月) · 永久长效记忆牢固标熟")
                ).forEach { (stage, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stage,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foldable Section 7: Cache & Data Management
        FoldableCard(
            title = "缓存与数据管理",
            subtitle = "清除音频缓存 · 重置学习数据",
            icon = Icons.Default.DeleteSweep,
            isExpanded = isCacheExpanded,
            onToggle = { isCacheExpanded = !isCacheExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsActionRow(
                    title = "清除音频缓存",
                    desc = "删除已下载的离线发音文件以节省空间",
                    actionLabel = "清除缓存",
                    onClick = { viewModel.clearAudioCache() }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                SettingsActionRow(
                    title = "重置学习数据",
                    desc = "清空所有进度并恢复初始词库 (慎用)",
                    actionLabel = "立即重置",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { showResetConfirmation = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))



        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    desc: String,
    actionLabel: String,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(text = actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun FoldableCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "折叠" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }
            }
        }
    }
}

private fun colorToHex(color: Color): String {
    val a = (color.alpha * 255f).toInt().coerceIn(0, 255)
    val r = (color.red * 255f).toInt().coerceIn(0, 255)
    val g = (color.green * 255f).toInt().coerceIn(0, 255)
    val b = (color.blue * 255f).toInt().coerceIn(0, 255)
    return if (a == 255) {
        String.format("#%02X%02X%02X", r, g, b)
    } else {
        String.format("#%02X%02X%02X%02X", a, r, g, b)
    }
}

@Composable
fun CustomColorPartEditor(
    title: String,
    description: String,
    currentColor: Color,
    quickPresets: List<Pair<Color, String>>,
    onSelectColor: (Color) -> Unit
) {
    var showPickerDialog by remember { mutableStateOf(false) }

    if (showPickerDialog) {
        UniversalColorPickerDialog(
            title = "自定义 $title",
            initialColor = currentColor,
            onDismiss = { showPickerDialog = false },
            onColorSelected = { color ->
                onSelectColor(color)
            }
        )
    }

    val currentAlphaPercent = (currentColor.alpha * 100f).roundToInt().coerceIn(0, 100)
    val hexCode = colorToHex(currentColor)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with title, description and current color badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = currentColor,
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.size(16.dp)
                        ) {}
                        Text(
                            text = hexCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row with Quick Preset Swatches + "Custom Color Picker" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quick swatches
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickPresets.forEach { (color, _) ->
                        val isSelected = (currentColor.toArgb() == color.toArgb())
                        Surface(
                            shape = CircleShape,
                            color = color,
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Color.LightGray),
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable {
                                    // Preserve current transparency when picking quick color
                                    onSelectColor(color.copy(alpha = currentColor.alpha))
                                }
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (color == Color.White || color.value == 0xFFFFFFFFUL) Color.Black else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Color Button
                Button(
                    onClick = { showPickerDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Colorize, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("拾色器", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inline Transparency / Opacity (Alpha) Slider for this custom part
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "透明度调节 (Alpha)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${currentAlphaPercent}% ${if (currentAlphaPercent == 100) "(不透明)" else ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = currentColor.alpha,
                    onValueChange = { newAlpha ->
                        onSelectColor(currentColor.copy(alpha = newAlpha))
                    },
                    valueRange = 0.05f..1.0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun UniversalColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    // Initial RGBA
    val initA = (initialColor.alpha * 255f).toInt().coerceIn(0, 255)
    val initR = (initialColor.red * 255f).toInt().coerceIn(0, 255)
    val initG = (initialColor.green * 255f).toInt().coerceIn(0, 255)
    val initB = (initialColor.blue * 255f).toInt().coerceIn(0, 255)

    var alphaValue by remember { mutableStateOf(initA.toFloat()) }
    var redValue by remember { mutableStateOf(initR.toFloat()) }
    var greenValue by remember { mutableStateOf(initG.toFloat()) }
    var blueValue by remember { mutableStateOf(initB.toFloat()) }

    fun buildHexText(a: Int, r: Int, g: Int, b: Int): String {
        return if (a == 255) {
            String.format("%02X%02X%02X", r, g, b)
        } else {
            String.format("%02X%02X%02X%02X", a, r, g, b)
        }
    }

    var hexText by remember {
        mutableStateOf(buildHexText(initA, initR, initG, initB))
    }

    val currentColor = Color(
        red = redValue.toInt().coerceIn(0, 255),
        green = greenValue.toInt().coerceIn(0, 255),
        blue = blueValue.toInt().coerceIn(0, 255),
        alpha = alphaValue.toInt().coerceIn(0, 255)
    )

    fun updateFromHex(hex: String) {
        val clean = hex.removePrefix("#").trim()
        if (clean.length == 6) {
            try {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                redValue = r.toFloat()
                greenValue = g.toFloat()
                blueValue = b.toFloat()
                alphaValue = 255f
            } catch (_: Exception) {}
        } else if (clean.length == 8) {
            try {
                val a = clean.substring(0, 2).toInt(16)
                val r = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val b = clean.substring(6, 8).toInt(16)
                alphaValue = a.toFloat()
                redValue = r.toFloat()
                greenValue = g.toFloat()
                blueValue = b.toFloat()
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Color comparison preview card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("当前原色", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = initialColor,
                                border = BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.size(36.dp)
                            ) {}
                            Text(
                                text = "${(initialColor.alpha * 100).toInt()}%",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("即时新色", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = currentColor,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(42.dp)
                            ) {}
                            Text(
                                text = "${(currentColor.alpha * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Hex input field (supports 6-digit #RRGGBB or 8-digit #AARRGGBB)
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isLetterOrDigit() }.take(8).uppercase()
                        hexText = filtered
                        updateFromHex(filtered)
                    },
                    label = { Text("十六进制颜色代码 (Hex #RGB 或 #ARGB)") },
                    prefix = { Text("#", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick spectrum palette chips
                Text("高频鲜艳色卡 (点击快速选取)：", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val presets = listOf(
                        Color(0xFFE11D48) to "绯红",
                        Color(0xFF0F172A) to "黑曜",
                        Color(0xFF0F766E) to "青绿",
                        Color(0xFFFFFFFF) to "纯白",
                        Color(0xFF2563EB) to "皇家蓝",
                        Color(0xFF10B981) to "翡翠绿",
                        Color(0xFFF59E0B) to "活力橙",
                        Color(0xFF7C3AED) to "电光紫"
                    )
                    presets.forEach { (color, _) ->
                        Surface(
                            shape = CircleShape,
                            color = color,
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    val r = (color.red * 255f).toInt().coerceIn(0, 255)
                                    val g = (color.green * 255f).toInt().coerceIn(0, 255)
                                    val b = (color.blue * 255f).toInt().coerceIn(0, 255)
                                    redValue = r.toFloat()
                                    greenValue = g.toFloat()
                                    blueValue = b.toFloat()
                                    hexText = buildHexText(alphaValue.toInt(), r, g, b)
                                }
                        ) {}
                    }
                }

                // Quick Alpha chips
                Text("快速透明度预设：", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        1.0f to "100%",
                        0.85f to "85%",
                        0.70f to "70%",
                        0.50f to "50%",
                        0.25f to "25%"
                    ).forEach { (alphaRatio, label) ->
                        val targetAlpha = (alphaRatio * 255f)
                        val isAlphaSelected = (alphaValue.toInt() in (targetAlpha.toInt() - 5)..(targetAlpha.toInt() + 5))
                        FilterChip(
                            selected = isAlphaSelected,
                            onClick = {
                                alphaValue = targetAlpha
                                hexText = buildHexText(alphaValue.toInt(), redValue.toInt(), greenValue.toInt(), blueValue.toInt())
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Alpha (Transparency) Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("不透明度 (A - Alpha / ${(alphaValue / 255f * 100).roundToInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        Text("${alphaValue.toInt()} / 255", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = alphaValue,
                        onValueChange = {
                            alphaValue = it
                            hexText = buildHexText(alphaValue.toInt(), redValue.toInt(), greenValue.toInt(), blueValue.toInt())
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Red Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("红色分量 (R - Red)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                        Text("${redValue.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = redValue,
                        onValueChange = {
                            redValue = it
                            hexText = buildHexText(alphaValue.toInt(), redValue.toInt(), greenValue.toInt(), blueValue.toInt())
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFDC2626), activeTrackColor = Color(0xFFDC2626))
                    )
                }

                // Green Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("绿色分量 (G - Green)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF16A34A))
                        Text("${greenValue.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = greenValue,
                        onValueChange = {
                            greenValue = it
                            hexText = buildHexText(alphaValue.toInt(), redValue.toInt(), greenValue.toInt(), blueValue.toInt())
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF16A34A), activeTrackColor = Color(0xFF16A34A))
                    )
                }

                // Blue Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("蓝色分量 (B - Blue)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2563EB))
                        Text("${blueValue.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = blueValue,
                        onValueChange = {
                            blueValue = it
                            hexText = buildHexText(alphaValue.toInt(), redValue.toInt(), greenValue.toInt(), blueValue.toInt())
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF2563EB), activeTrackColor = Color(0xFF2563EB))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentColor)
                    onDismiss()
                }
            ) {
                Text("应用此颜色")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CustomInputDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
