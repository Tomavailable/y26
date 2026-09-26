package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SentenceVoiceType
import com.example.data.dict.EcdictFormatter
import com.example.data.dict.ExtractedDictItem
import com.example.data.dict.VoiceProfile
import com.example.data.model.BookEntity
import com.example.data.model.BookSentenceEntity
import com.example.data.model.WordEntity
import kotlinx.coroutines.launch

private data class ExampleSource(val id: String, val title: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordCard(
    word: WordEntity,
    isMeaningRevealed: Boolean,
    customBooks: List<BookEntity> = emptyList(),
    customSentences: List<BookSentenceEntity> = emptyList(),
    defaultExampleSource: String = "builtin",
    selectedBuiltInDictionary: com.example.data.dict.BuiltInDictionary = com.example.data.dict.BuiltInDictionary.DEFAULT,
    onPronounce: () -> Unit,
    onPronounceProfile: (String) -> Unit = {},
    onPronounceSequence: () -> Unit = {},
    voiceProfiles: List<VoiceProfile> = emptyList(),
    mdxDetail: ExtractedDictItem? = null,
    onPronounceSentence: (String, SentenceVoiceType) -> Unit = { _, _ -> },
    activeSentenceVoice: SentenceVoiceType? = null,
    onRevealMeaning: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMarkMastered: () -> Unit,
    isMultiVoiceEnabled: Boolean = false,
    onToggleMultiVoice: ((Boolean) -> Unit)? = null,
    autoAdvanceOnCorrectAnswer: Boolean = false,
    onToggleAutoAdvance: ((Boolean) -> Unit)? = null,
    isDetailView: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayWord = remember(word, selectedBuiltInDictionary) {
        com.example.data.dict.BuiltInDictionaryManager.formatForDictionary(word, selectedBuiltInDictionary)
    }
    var isMdxDetailExpanded by remember(word.id) { mutableStateOf(false) }
    var showVoicesDropdown by remember { mutableStateOf(false) }
    val enabledVoices = voiceProfiles.filter { it.isEnabled }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        // ================= WINDOW 1: Word Information Card =================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("word_card_${word.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Action Bar: Stage Badge + Master (斩) + Favorite + Voice Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val stageText = when {
                        word.isMastered -> "已掌握 (已斩)"
                        word.reviewStage == 0 -> "新词"
                        else -> "复习阶段 ${word.reviewStage}/8"
                    }
                    val stageColor = when {
                        word.isMastered -> MaterialTheme.colorScheme.tertiary
                        word.reviewStage == 0 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = stageColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = stageText,
                            color = stageColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Master "斩"
                        IconButton(
                            onClick = onMarkMastered,
                            modifier = Modifier.testTag("mark_mastered_btn")
                        ) {
                            Icon(
                                imageVector = if (word.isMastered) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = "标为已完全掌握",
                                tint = if (word.isMastered) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Favorite / Bookmark
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.testTag("toggle_favorite_btn")
                        ) {
                            Icon(
                                imageVector = if (word.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "收藏该单词",
                                tint = if (word.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Settings Dropdown (Auto Advance, Multi-Voice, Carousel)
                        Box {
                            IconButton(
                                onClick = { showVoicesDropdown = true },
                                modifier = Modifier.testTag("word_card_settings_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "卡片与学习设置",
                                    tint = if (autoAdvanceOnCorrectAnswer || isMultiVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showVoicesDropdown,
                                onDismissRequest = { showVoicesDropdown = false }
                            ) {
                                // 答对自动进入下一个 开关选项（默认关闭）
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (autoAdvanceOnCorrectAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "答对自动进入下一个",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (autoAdvanceOnCorrectAnswer) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (autoAdvanceOnCorrectAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = if (autoAdvanceOnCorrectAnswer) "已开启（直接切题）" else "默认关闭（答对后进入详情页）",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            Switch(
                                                checked = autoAdvanceOnCorrectAnswer,
                                                onCheckedChange = { checked ->
                                                    onToggleAutoAdvance?.invoke(checked)
                                                },
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onToggleAutoAdvance?.invoke(!autoAdvanceOnCorrectAnswer)
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // Multi-voice Carousel Toggle Switch
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (isMultiVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "多音源轮播发音",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isMultiVoiceEnabled) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isMultiVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Switch(
                                                checked = isMultiVoiceEnabled,
                                                onCheckedChange = { checked ->
                                                    onToggleMultiVoice?.invoke(checked)
                                                },
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onToggleMultiVoice?.invoke(!isMultiVoiceEnabled)
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = { Text("试听一次多音源轮播") },
                                    onClick = {
                                        showVoicesDropdown = false
                                        onPronounceSequence()
                                    },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp)) }
                                )

                                if (enabledVoices.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    enabledVoices.forEach { profile ->
                                        DropdownMenuItem(
                                            text = { Text("${profile.accent.flag} ${profile.name}") },
                                            onClick = {
                                                showVoicesDropdown = false
                                                onPronounceProfile(profile.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 1: Word Title + Frequency Rank Number (Only digits)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (word.frequencyRank != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "${word.frequencyRank}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: UK & US Phonetics with Click to Pronounce
                val (ukPhonetic, usPhonetic) = remember(word.phonetic) { parseUKAndUSPhonetics(word.phonetic) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // UK Phonetic
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPronounceProfile("uk") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "英",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (ukPhonetic.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = ukPhonetic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "英音发音",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // US Phonetic
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPronounceProfile("us") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "美",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (usPhonetic.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = usPhonetic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "美音发音",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                // Meaning / Active Recall Area inside Window 1
                if (!isMeaningRevealed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { onRevealMeaning() }
                            .testTag("reveal_meaning_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "轻触翻开释义与例句",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "主动唤醒记忆更有助于长效掌握",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    // 完整中文释义 (展示全量词性与丰富含义)
                    val fullMeaningRaw = displayWord.originalMeaning.ifBlank { word.originalMeaning.ifBlank { displayWord.meaning.ifBlank { word.meaning } } }
                    val parsedMeanings = EcdictFormatter.parseMeanings(fullMeaningRaw, word.pos)

                    if (parsedMeanings.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            parsedMeanings.forEach { m ->
                                Row(verticalAlignment = Alignment.Top) {
                                    if (m.pos.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = m.pos,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = m.definition.replace("\\n", "\n").replace("/", " ").trim(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = fullMeaningRaw.replace("\\n", "\n").replace("/", " ").trim().ifBlank { "暂无释义" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    }

                    // 时态与词形变化
                    val tensesText = remember(word.notes, displayWord.notes) {
                        val rawNotes = displayWord.notes.ifBlank { word.notes }
                        if (rawNotes.contains("【词形】")) {
                            rawNotes.substringAfter("【词形】").trim()
                        } else {
                            EcdictFormatter.formatExchange(word.word, rawNotes)
                        }
                    }
                    if (tensesText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tensesText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // 权威英英释义
                    val rawDef = displayWord.definition.ifBlank { word.definition.ifBlank { mdxDetail?.conciseMeaning ?: "" } }
                    val cleanDef = rawDef.replace(Regex("""^(Longman Contemporary\s*\[朗文当代\]|朗文当代·红星考点\s*\[Red Star Core\]|剑桥核心词典\s*\[Cambridge Essentials\]|\[朗文当代\]|\[Red Star Core\]|\[Cambridge Essentials\])\s*[:：]?\s*""", RegexOption.MULTILINE), "")
                        .replace("\\n", "\n").replace("\\r", "").trim()
                    if (cleanDef.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "英英释义 (${selectedBuiltInDictionary.displayName})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val defParas = cleanDef.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    defParas.forEach { p ->
                                        Text(
                                            text = "• " + p.replaceFirst(Regex("""^\d+[\s\.\、\-\:\/)]+"""), "").trim(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // MDX 离线大词典
                    if (isDetailView && mdxDetail != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isMdxDetailExpanded = !isMdxDetailExpanded }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "外部 MDX 词典释义",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isMdxDetailExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (isMdxDetailExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = mdxDetail.conciseMeaning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ================= WINDOW 2: Example Sentences Section =================
                if (isDetailView && isMeaningRevealed) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    var selectedExampleTab by remember(word.word) { mutableStateOf(0) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "权威双语例句",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (customSentences.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = selectedExampleTab == 0,
                                    onClick = { selectedExampleTab = 0 },
                                    label = { Text("权威例句", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = selectedExampleTab == 1,
                                    onClick = { selectedExampleTab = 1 },
                                    label = { Text("图书原句 (${customSentences.size})", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedExampleTab == 1 && customSentences.isNotEmpty()) {
                        CustomBookSentencesView(
                            word = word,
                            bookSentences = customSentences,
                            activeSentenceVoice = activeSentenceVoice,
                            onPronounceSentence = onPronounceSentence
                        )
                    } else {
                        BuiltInSentencesView(
                            word = displayWord,
                            selectedBuiltInDictionary = selectedBuiltInDictionary,
                            activeSentenceVoice = activeSentenceVoice,
                            onPronounceSentence = onPronounceSentence
                        )
                    }

                    // 词源速记 (Etymology)
                    val etymNotes = displayWord.notes.ifBlank { word.notes }
                    if (etymNotes.contains("【词根】") || etymNotes.contains("【构成】")) {
                        Spacer(modifier = Modifier.height(10.dp))
                        EtymologyView(word = displayWord)
                    }
                }
            }
        }
    }
}

@Composable
private fun BuiltInSentencesView(
    word: com.example.data.model.WordEntity,
    selectedBuiltInDictionary: com.example.data.dict.BuiltInDictionary = com.example.data.dict.BuiltInDictionary.DEFAULT,
    activeSentenceVoice: com.example.audio.SentenceVoiceType?,
    onPronounceSentence: (String, com.example.audio.SentenceVoiceType) -> Unit
) {
    if (word.exampleSentence.isNotBlank()) {
        val sents = word.exampleSentence.split(" ||| ").filter { it.isNotBlank() }
        val trans = word.exampleTranslation.split(" ||| ")

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selectedBuiltInDictionary != com.example.data.dict.BuiltInDictionary.DEFAULT) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "当前内置词典：${selectedBuiltInDictionary.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            sents.forEachIndexed { sIdx, rawSentence ->
                val rawTrans = trans.getOrNull(sIdx) ?: ""

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${sIdx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Simplified Speaker buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                com.example.audio.SentenceVoiceType.values().forEach { voiceType ->
                                    val isPlaying = (activeSentenceVoice == voiceType)
                                    IconButton(
                                        onClick = { onPronounceSentence(rawSentence, voiceType) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Sentence string with highlighted word
                        val target = word.word
                        val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                            val idx = rawSentence.indexOf(target, ignoreCase = true)
                            if (idx >= 0) {
                                append(rawSentence.substring(0, idx))
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(rawSentence.substring(idx, idx + target.length))
                                }
                                append(rawSentence.substring(idx + target.length))
                            } else {
                                append(rawSentence)
                            }
                        }

                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )

                        if (rawTrans.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rawTrans,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无内置例句",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomBookSentencesView(
    word: com.example.data.model.WordEntity,
    bookSentences: List<com.example.data.model.BookSentenceEntity>,
    activeSentenceVoice: com.example.audio.SentenceVoiceType?,
    onPronounceSentence: (String, com.example.audio.SentenceVoiceType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        bookSentences.forEachIndexed { index, bSent ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "图书例句 ${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            com.example.audio.SentenceVoiceType.values().forEach { voiceType ->
                                val isPlaying = (activeSentenceVoice == voiceType)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onPronounceSentence(bSent.sentence, voiceType) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = voiceType.flag, fontSize = 9.sp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = voiceType.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            color = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val sentence = bSent.sentence
                    val target = word.word
                    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                        val idx = sentence.indexOf(target, ignoreCase = true)
                        if (idx >= 0) {
                            append(sentence.substring(0, idx))
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(sentence.substring(idx, idx + target.length))
                            }
                            append(sentence.substring(idx + target.length))
                        } else {
                            append(sentence)
                        }
                    }

                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

private fun parseUKAndUSPhonetics(rawPhonetic: String): Pair<String, String> {
    if (rawPhonetic.isBlank()) return Pair("", "")
    
    // 1. Clean extra noise labels
    val clean = rawPhonetic
        .replace("🇬🇧", "")
        .replace("🇺🇸", "")
        .replace("英", "")
        .replace("美", "")
        .trim()
    
    // 2. Extract standard bracketed phonetics e.g. [ˈprɒspekt] or /ˈprɑːspekt/
    val regex = Regex("""[\[/][^\[\]/]+[\]/]""")
    val matches = regex.findAll(clean).map { it.value.trim() }.toList()
    
    if (matches.isNotEmpty()) {
        val uniqueMatches = matches.distinct()
        val uk = uniqueMatches.getOrNull(0) ?: ""
        val us = if (uniqueMatches.size > 1) uniqueMatches[1] else uk
        return Pair(uk, us)
    }
    
    // Fallback splitting and deduplication
    val parts = clean.split(Regex("""[\s,/|]+"""))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        
    val uk = parts.getOrNull(0) ?: clean
    val us = parts.getOrNull(1) ?: uk
    return Pair(uk, us)
}

private data class MorphologyElement(val part: String, val meaning: String)

private fun getMorphologyBreakdown(word: String): List<MorphologyElement> {
    val clean = word.trim().lowercase()
    val list = mutableListOf<MorphologyElement>()
    
    // Check prefixes
    if (clean.startsWith("un") && clean.length > 4) {
        list.add(MorphologyElement("un-", "前缀：表示“相反 / 否定 / 无”"))
    } else if (clean.startsWith("re") && clean.length > 4) {
        list.add(MorphologyElement("re-", "前缀：表示“往回 / 再次 / 重新”"))
    } else if ((clean.startsWith("in") || clean.startsWith("im") || clean.startsWith("il") || clean.startsWith("ir")) && clean.length > 4) {
        if (clean.startsWith("im")) {
            list.add(MorphologyElement("im-", "前缀：在m开头单词前表“不 / 向内”"))
        } else if (clean.startsWith("il")) {
            list.add(MorphologyElement("il-", "前缀：在l开头单词前表“不 / 否定”"))
        } else if (clean.startsWith("ir")) {
            list.add(MorphologyElement("ir-", "前缀：在r开头单词前表“相反 / 否定”"))
        } else {
            list.add(MorphologyElement("in-", "前缀：表示“不 / 否定 / 向内”"))
        }
    } else if (clean.startsWith("dis") && clean.length > 4) {
        list.add(MorphologyElement("dis-", "前缀：表示“分离 / 否定 / 夺去”"))
    } else if (clean.startsWith("con") && clean.length > 4) {
        list.add(MorphologyElement("con-", "前缀：表示“共同 / 加强语气”"))
    } else if (clean.startsWith("com") && clean.length > 4) {
        list.add(MorphologyElement("com-", "前缀：表示“共同 / 聚集”"))
    } else if (clean.startsWith("pro") && clean.length > 4) {
        list.add(MorphologyElement("pro-", "前缀：表示“向前 / 拥护 / 预先”"))
    } else if (clean.startsWith("sub") && clean.length > 4) {
        list.add(MorphologyElement("sub-", "前缀：表示“在...下面 / 次要 / 下级”"))
    } else if (clean.startsWith("trans") && clean.length > 5) {
        list.add(MorphologyElement("trans-", "前缀：表示“横过 / 穿过 / 转换”"))
    } else if (clean.startsWith("de") && clean.length > 4) {
        list.add(MorphologyElement("de-", "前缀：表示“向下 / 减少 / 否定”"))
    } else if (clean.startsWith("ex") && clean.length > 4) {
        list.add(MorphologyElement("ex-", "前缀：表示“出 / 向外 / 前任的”"))
    } else if (clean.startsWith("pre") && clean.length > 4) {
        list.add(MorphologyElement("pre-", "前缀：表示“在...之前 / 预先”"))
    } else if (clean.startsWith("ad") && clean.length > 4) {
        list.add(MorphologyElement("ad-", "前缀：表示“朝向 / 加强”"))
    } else if (clean.startsWith("per") && clean.length > 4) {
        list.add(MorphologyElement("per-", "前缀：表示“贯穿 / 彻底 / 始终”"))
    } else if (clean.startsWith("co") && clean.length > 4) {
        list.add(MorphologyElement("co-", "前缀：表示“共同 / 协同”"))
    } else if (clean.startsWith("bi") && clean.length > 4) {
        list.add(MorphologyElement("bi-", "前缀：表示“二 / 双”"))
    } else if (clean.startsWith("tri") && clean.length > 4) {
        list.add(MorphologyElement("tri-", "前缀：表示“三”"))
    } else if (clean.startsWith("multi") && clean.length > 5) {
        list.add(MorphologyElement("multi-", "前缀：表示“多 / 复合”"))
    } else if (clean.startsWith("inter") && clean.length > 5) {
        list.add(MorphologyElement("inter-", "前缀：表示“在...之间 / 相互”"))
    } else if (clean.startsWith("super") && clean.length > 5) {
        list.add(MorphologyElement("super-", "前缀：表示“在...之上 / 超级”"))
    } else if (clean.startsWith("anti") && clean.length > 5) {
        list.add(MorphologyElement("anti-", "前缀：表示“反对 / 相反”"))
    } else if (clean.startsWith("auto") && clean.length > 4) {
        list.add(MorphologyElement("auto-", "前缀：表示“自动 / 自己”"))
    } else if (clean.startsWith("tele") && clean.length > 4) {
        list.add(MorphologyElement("tele-", "前缀：表示“远处 / 远距离”"))
    } else if (clean.startsWith("micro") && clean.length > 5) {
        list.add(MorphologyElement("micro-", "前缀：表示“微小 / 细微”"))
    } else if (clean.startsWith("macro") && clean.length > 5) {
        list.add(MorphologyElement("macro-", "前缀：表示“宏大 / 巨大”"))
    }

    // Check suffixes
    if (clean.endsWith("able") || clean.endsWith("ible")) {
        list.add(MorphologyElement("-able/-ible", "后缀：表示“可...的 / 能够...的”"))
    } else if (clean.endsWith("ment")) {
        list.add(MorphologyElement("-ment", "后缀：表“行为 / 状态 / 结果”（名词）"))
    } else if (clean.endsWith("tion") || clean.endsWith("sion")) {
        list.add(MorphologyElement("-tion/-sion", "后缀：表“动作过程 / 状态 / 结果”"))
    } else if (clean.endsWith("ate") && clean.length > 4) {
        list.add(MorphologyElement("-ate", "后缀：多为动词表示“使做 / 造成”"))
    } else if (clean.endsWith("ify") && clean.length > 4) {
        list.add(MorphologyElement("-ify", "后缀：表示“使化 / 变成...”（动词）"))
    } else if (clean.endsWith("ize") || clean.endsWith("ise")) {
        list.add(MorphologyElement("-ize", "后缀：表示“使...化 / 变为...”（动词）"))
    } else if (clean.endsWith("ive") && clean.length > 4) {
        list.add(MorphologyElement("-ive", "后缀：表示“具有...性质的 / 倾向的”"))
    } else if (clean.endsWith("ous") || clean.endsWith("ious")) {
        list.add(MorphologyElement("-ous", "后缀：表示“充满...的 / 具有...特征的”"))
    } else if (clean.endsWith("al") && clean.length > 4) {
        list.add(MorphologyElement("-al", "后缀：表示“关于...的 / 具有...性质的”"))
    } else if (clean.endsWith("ent") || clean.endsWith("ant")) {
        list.add(MorphologyElement("-ent/-ant", "后缀：表示“形容词或名词表特定主体”"))
    } else if (clean.endsWith("ity") || clean.endsWith("ty")) {
        list.add(MorphologyElement("-ity/-ty", "后缀：表“性质 / 状态”（名词）"))
    } else if (clean.endsWith("ness")) {
        list.add(MorphologyElement("-ness", "后缀：表“某种状态或性质”（名词）"))
    } else if (clean.endsWith("ful")) {
        list.add(MorphologyElement("-ful", "后缀：表示“充满...的 / 具有...的”"))
    } else if (clean.endsWith("less")) {
        list.add(MorphologyElement("-less", "后缀：表示“没有...的 / 无...的”"))
    } else if (clean.endsWith("ism")) {
        list.add(MorphologyElement("-ism", "后缀：表示“...主义 / 论 / 学说”"))
    } else if (clean.endsWith("ist")) {
        list.add(MorphologyElement("-ist", "后缀：表示“做该事的人 / 专家”"))
    } else if (clean.endsWith("logy")) {
        list.add(MorphologyElement("-logy", "后缀：表示“...科学 / 学说”"))
    } else if (clean.endsWith("ship")) {
        list.add(MorphologyElement("-ship", "后缀：表示“身份 / 状态 / 关系”"))
    }

    return list
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EtymologyView(word: WordEntity) {
    val rawNotes = word.notes.trim()
    val localEntry = com.example.data.dict.LocalDictionary.lookup(word.word)
    val localNotes = localEntry?.notes?.trim() ?: ""

    val etymologyContent = when {
        rawNotes.contains("【词根】") || rawNotes.contains("【词源】") -> rawNotes
        localNotes.contains("【词根】") || localNotes.contains("【词源】") -> localNotes
        rawNotes.isNotBlank() -> rawNotes
        else -> localNotes
    }

    val displayEtymology = etymologyContent.ifBlank {
        "暂无词根词源信息。"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        if (displayEtymology.contains("->") || displayEtymology.contains("＋") || displayEtymology.contains("+")) {
            val parts = displayEtymology.split("->")
            val formula = parts.firstOrNull()?.replace("【词根】", "")?.replace("【词源】", "")?.trim() ?: ""
            val concept = parts.getOrNull(1)?.trim() ?: ""
            val typeLabel = if (displayEtymology.contains("【词源】")) "词源演变" else "词根拆解"

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = formula,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 22.sp
                    )
                    
                    if (concept.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "🎯 逻辑核：",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = concept,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        } else {
            val cleanDisplay = displayEtymology.replace("【词根】", "").replace("【词源】", "").trim()
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = cleanDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Suggestions removed by user request

        // 4. 词根同源派生词 (Derivatives) - 路径 A：内置轻量匹配，展示关联派生词
        val derivatives = remember(word.word) { getDerivatives(word.word) }
        if (derivatives.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories, // 使用 AutoStories 图书馆图标
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "👥 同根词族联想 (Derivatives)：",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            var selectedDerivativeForDialog by remember { mutableStateOf<DerivativeWord?>(null) }
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                derivatives.forEach { derivative ->
                    Surface(
                        shape = RoundedCornerShape(20.dp), // 胶囊型药丸
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .clickable { selectedDerivativeForDialog = derivative }
                            .padding(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = derivative.word,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = derivative.meaning.substringAfter(". ").take(10), // 截取核心简短含义
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 弹窗展示派生词简要详情，不打断用户做题！
            if (selectedDerivativeForDialog != null) {
                val d = selectedDerivativeForDialog!!
                AlertDialog(
                    onDismissRequest = { selectedDerivativeForDialog = null },
                    title = {
                        Text(
                            text = d.word,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "【同族释义】",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = d.meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "💡 词根记忆：与当前单词拥有相同的词根核心。背一得十，效率翻倍！",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedDerivativeForDialog = null }) {
                            Text("知道了")
                        }
                    }
                )
            }
        }
    }
}

// 派生词数据模型
private data class DerivativeWord(val word: String, val meaning: String)

// 派生词核心智能查找
private fun getDerivatives(word: String): List<DerivativeWord> {
    val clean = word.trim().lowercase()
    
    val rootsMap = mapOf(
        "spect" to listOf(
            DerivativeWord("prospect", "n. 前景, 期望"),
            DerivativeWord("inspect", "v. 检查, 视察"),
            DerivativeWord("suspect", "v. 怀疑 n. 嫌疑人"),
            DerivativeWord("respect", "v./n. 尊敬, 尊重"),
            DerivativeWord("retrospect", "n./v. 回顾, 回想"),
            DerivativeWord("perspective", "n. 视角, 远景")
        ),
        "port" to listOf(
            DerivativeWord("transport", "v./n. 运输, 传送"),
            DerivativeWord("export", "v./n. 出口"),
            DerivativeWord("import", "v./n. 进口"),
            DerivativeWord("portable", "adj. 便携的, 轻便的"),
            DerivativeWord("support", "v./n. 支持, 支撑"),
            DerivativeWord("report", "v./n. 报告, 报道")
        ),
        "cred" to listOf(
            DerivativeWord("incredible", "adj. 难以置信的, 极好的"),
            DerivativeWord("credible", "adj. 可信的, 可靠的"),
            DerivativeWord("credit", "n. 信用, 信贷 v. 相信"),
            DerivativeWord("credulous", "adj. 轻信的, 易受骗的")
        ),
        "path" to listOf(
            DerivativeWord("sympathy", "n. 同情, 共鸣"),
            DerivativeWord("empathy", "n. 共情, 神入"),
            DerivativeWord("antipathy", "n. 反感, 厌恶"),
            DerivativeWord("apathy", "n. 冷漠, 无动于衷")
        ),
        "tract" to listOf(
            DerivativeWord("attract", "v. 吸引, 招引"),
            DerivativeWord("contract", "n. 合同 v. 收缩"),
            DerivativeWord("distract", "v. 分散(注意力), 扰乱"),
            DerivativeWord("extract", "v. 拔出, 提取 n. 摘录"),
            DerivativeWord("abstract", "adj. 抽象的 n. 摘要")
        ),
        "scrib" to listOf(
            DerivativeWord("describe", "v. 描述, 形容"),
            DerivativeWord("subscribe", "v. 订阅, 签署"),
            DerivativeWord("prescribe", "v. 开处方, 规定"),
            DerivativeWord("inscribe", "v. 刻写, 雕刻"),
            DerivativeWord("transcript", "n. 抄本, 成绩单")
        ),
        "dict" to listOf(
            DerivativeWord("predict", "v. 预测, 预言"),
            DerivativeWord("contradict", "v. 反驳, 矛盾"),
            DerivativeWord("dictate", "v. 听写, 口授"),
            DerivativeWord("dictionary", "n. 词典, 字典"),
            DerivativeWord("addict", "v. 使沉溺, 使上瘾 n. 瘾君子")
        ),
        "vis" to listOf(
            DerivativeWord("visit", "v./n. 拜访, 参观"),
            DerivativeWord("advise", "v. 忠告, 建议"),
            DerivativeWord("revise", "v. 修订,复习"),
            DerivativeWord("supervise", "v. 监督, 管理"),
            DerivativeWord("visible", "adj. 可见的, 看得见的"),
            DerivativeWord("visual", "adj. 视觉的, 形象的")
        )
    )

    for ((root, derivatives) in rootsMap) {
        if (clean.contains(root)) {
            return derivatives.filter { it.word != clean }
        }
    }
    return emptyList()
}
