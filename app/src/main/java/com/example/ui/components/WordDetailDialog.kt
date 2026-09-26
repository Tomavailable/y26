package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WordEntity
import com.example.ui.viewmodel.MainViewModel

@Composable
fun WordDetailDialog(
    word: WordEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val customBooks by viewModel.allCustomBooks.collectAsStateWithLifecycle()
    val defaultExampleSource by viewModel.defaultExampleSource.collectAsStateWithLifecycle()
    val selectedBuiltInDictionary by viewModel.selectedBuiltInDictionary.collectAsStateWithLifecycle()
    val voiceProfiles by viewModel.voiceProfiles.collectAsStateWithLifecycle()
    val isMultiVoiceEnabled by viewModel.multiVoiceSequentialPlay.collectAsStateWithLifecycle()
    val autoAdvanceOnCorrectAnswer by viewModel.autoAdvanceOnCorrectAnswer.collectAsStateWithLifecycle()
    val activeSentenceVoice by viewModel.activeSentenceVoicePlaying.collectAsStateWithLifecycle()
    val customSentences by viewModel.getSentencesForWord(word.word).collectAsStateWithLifecycle(initialValue = emptyList())
    val mdxDetail = remember(word.word) { viewModel.lookupMdxDetail(word.word) }

    var showHeaderSettingsMenu by remember { mutableStateOf(false) }

    // 每次进入详情页 单词和第一个例句自动发音
    LaunchedEffect(word.id) {
        viewModel.pronounceWordAndFirstSentence(word)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("word_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "单词详情",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 右上角设置按钮
                        Box {
                            IconButton(
                                onClick = { showHeaderSettingsMenu = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("word_detail_settings_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "详情页设置",
                                    tint = if (autoAdvanceOnCorrectAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showHeaderSettingsMenu,
                                onDismissRequest = { showHeaderSettingsMenu = false }
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
                                                    viewModel.setAutoAdvanceOnCorrectAnswer(checked)
                                                },
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setAutoAdvanceOnCorrectAnswer(!autoAdvanceOnCorrectAnswer)
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // Multi-voice toggle
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "多人轮播发音",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isMultiVoiceEnabled) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "多个真人发音人轮流播放",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Switch(
                                                checked = isMultiVoiceEnabled,
                                                onCheckedChange = { checked ->
                                                    viewModel.setMultiVoiceSequentialPlay(checked)
                                                },
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setMultiVoiceSequentialPlay(!isMultiVoiceEnabled)
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    WordCard(
                        word = word,
                        isMeaningRevealed = true,
                        customBooks = customBooks,
                        customSentences = customSentences,
                        defaultExampleSource = defaultExampleSource,
                        selectedBuiltInDictionary = selectedBuiltInDictionary,
                        onPronounce = { viewModel.pronounceWord(word.word) },
                        onPronounceProfile = { profileId -> viewModel.pronounceWord(word.word, profileId) },
                        onPronounceSequence = { viewModel.pronounceSequence(word.word) },
                        voiceProfiles = voiceProfiles,
                        mdxDetail = mdxDetail,
                        onPronounceSentence = { sentence, voiceType ->
                            viewModel.pronounceSentence(sentence, voiceType)
                        },
                        activeSentenceVoice = activeSentenceVoice,
                        onRevealMeaning = {},
                        onToggleFavorite = { viewModel.toggleFavorite(word) },
                        onMarkMastered = { viewModel.toggleMastered(word) },
                        isMultiVoiceEnabled = isMultiVoiceEnabled,
                        onToggleMultiVoice = { viewModel.setMultiVoiceSequentialPlay(it) },
                        autoAdvanceOnCorrectAnswer = autoAdvanceOnCorrectAnswer,
                        onToggleAutoAdvance = { viewModel.setAutoAdvanceOnCorrectAnswer(it) },
                        isDetailView = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
