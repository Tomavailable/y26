package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Settings
import com.example.ui.components.DictionaryAndAudioSettingsCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.dict.EcdictFormatter
import com.example.data.model.FirstListenVoicePreference
import com.example.data.model.LearnItemStage
import com.example.data.model.LearnSessionItem
import com.example.data.model.LearningMode
import com.example.data.model.ReviewQuality
import com.example.data.model.SessionType
import com.example.ui.components.WordCard
import com.example.ui.navigation.AppScreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sessionItems by viewModel.sessionItems.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentSessionIndex.collectAsStateWithLifecycle()
    val isAudioPlaying by viewModel.isWordAudioPlaying.collectAsStateWithLifecycle()
    val audioListenCount by viewModel.currentAudioListenCount.collectAsStateWithLifecycle()
    val isWordRevealed by viewModel.isWordRevealedAfterAudio.collectAsStateWithLifecycle()
    val isFinished by viewModel.isLearningFinished.collectAsStateWithLifecycle()

    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val modeSwitches by viewModel.learningModeSwitches.collectAsStateWithLifecycle()
    val repeatTimes by viewModel.firstListenRepeatTimes.collectAsStateWithLifecycle()
    val voicePref by viewModel.firstListenVoicePref.collectAsStateWithLifecycle()

    val passedCount by viewModel.sessionPassedCount.collectAsStateWithLifecycle()
    val masteredCount by viewModel.sessionMasteredCount.collectAsStateWithLifecycle()
    val reinforcedCount by viewModel.sessionReinforcedCount.collectAsStateWithLifecycle()

    val voiceProfiles by viewModel.voiceProfiles.collectAsStateWithLifecycle()
    val activeSentenceVoice by viewModel.activeSentenceVoicePlaying.collectAsStateWithLifecycle()
    val customBooks by viewModel.allCustomBooks.collectAsStateWithLifecycle()
    val defaultExampleSource by viewModel.defaultExampleSource.collectAsStateWithLifecycle()
    val selectedBuiltInDictionary by viewModel.selectedBuiltInDictionary.collectAsStateWithLifecycle()
    val isMultiVoicePlay by viewModel.multiVoiceSequentialPlay.collectAsStateWithLifecycle()
    val autoAdvanceOnCorrectAnswer by viewModel.autoAdvanceOnCorrectAnswer.collectAsStateWithLifecycle()
    val currentSessionType by viewModel.currentSessionType.collectAsStateWithLifecycle()
    val reviewModeSwitches by viewModel.reviewLearningModeSwitches.collectAsStateWithLifecycle()
    val reviewAutoAdvanceOnCorrectAnswer by viewModel.reviewAutoAdvanceOnCorrectAnswer.collectAsStateWithLifecycle()

    val isReview = currentSessionType == com.example.data.model.SessionType.REVIEW
    val activeSwitches = if (isReview) reviewModeSwitches else modeSwitches
    val activeAutoAdvance = if (isReview) reviewAutoAdvanceOnCorrectAnswer else autoAdvanceOnCorrectAnswer

    var showSettingsSheet by remember { mutableStateOf(false) }
    val attachedMdxName by viewModel.attachedMdxName.collectAsStateWithLifecycle()
    val attachedMdxPath by viewModel.attachedMdxPath.collectAsStateWithLifecycle()
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }

    // 收起设置弹窗
    BackHandler(enabled = showSettingsSheet) {
        showSettingsSheet = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Nav & Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                modifier = Modifier.testTag("learn_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回首页"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val titlePrefix = if (isReview) "艾宾浩斯复习" else "背单词"
                val finishedTitle = if (isReview) "复习完成" else "学习通关"
                Text(
                    text = if (!isFinished && sessionItems.isNotEmpty()) "$titlePrefix (${(currentIndex + 1).coerceAtMost(sessionItems.size)}/${sessionItems.size})" else finishedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { showSettingsSheet = !showSettingsSheet },
                modifier = Modifier.testTag("learn_settings_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = if (isReview) "复习设置" else "学习设置",
                    tint = if (showSettingsSheet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Quick Setting ModalBottomSheet
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isReview) "复习模式设置 (高效复习)" else "学习模式设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${activeSwitches.enabledCount}/7 开启 (至少保留一种)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    if (isReview) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⚡ 复习模式注重高效：默认开启【答对自动切题】，仅打错后显示详情页；默认只开启盲听选义、看词选义、看义说词 3 种题型。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    LearnModeSwitchRow(
                        title = "1. 盲听选义",
                        desc = "只播发音不显示单词，从 4 个中文释义中选择",
                        checked = activeSwitches.enableBlindListenMeaning,
                        onCheckedChange = { checked ->
                            val updated = activeSwitches.copy(enableBlindListenMeaning = checked)
                            if (isReview) viewModel.updateReviewLearningModeSwitches(updated) else viewModel.updateLearningModeSwitches(updated)
                        }
                    )
                    LearnModeSwitchRow(
                        title = "2. 看词选义",
                        desc = "显示单词，从 4 个中文释义中快速匹配",
                        checked = activeSwitches.enableLookChooseMeaning,
                        onCheckedChange = { checked ->
                            val updated = activeSwitches.copy(enableLookChooseMeaning = checked)
                            if (isReview) viewModel.updateReviewLearningModeSwitches(updated) else viewModel.updateLearningModeSwitches(updated)
                        }
                    )
                    LearnModeSwitchRow(
                        title = "3. 看义说词",
                        desc = "看中文释义回忆发音与拼写，倒计时核对",
                        checked = activeSwitches.enableRecallByMeaning,
                        onCheckedChange = { checked ->
                            val updated = activeSwitches.copy(enableRecallByMeaning = checked)
                            if (isReview) viewModel.updateReviewLearningModeSwitches(updated) else viewModel.updateLearningModeSwitches(updated)
                        }
                    )
                    LearnModeSwitchRow(
                        title = "4. 听音选词",
                        desc = "听单词发音，从 4 个易混拼写选项中选择",
                        checked = activeSwitches.enableListenChooseWord,
                        onCheckedChange = { checked ->
                            val updated = activeSwitches.copy(enableListenChooseWord = checked)
                            if (isReview) viewModel.updateReviewLearningModeSwitches(updated) else viewModel.updateLearningModeSwitches(updated)
                        }
                    )
                    LearnModeSwitchRow(
                        title = "5. 听句选词",
                        desc = "听原声双语例句，结合语境选出挖空单词",
                        checked = activeSwitches.enableListenSentenceChooseWord,
                        onCheckedChange = { checked ->
                            val updated = activeSwitches.copy(enableListenSentenceChooseWord = checked)
                            if (isReview) viewModel.updateReviewLearningModeSwitches(updated) else viewModel.updateLearningModeSwitches(updated)
                        }
                    )
                    LearnModeSwitchRow(
                        title = "6. 英文释义选词",
                        desc = "播放 TTS 朗读英文释义，选出对应正确的单词",
                        checked = activeSwitches.enableEnglishMeaningChooseWord,
                        onCheckedChange = { checked ->
                            val updated = activeSwitches.copy(enableEnglishMeaningChooseWord = checked)
                            if (isReview) viewModel.updateReviewLearningModeSwitches(updated) else viewModel.updateLearningModeSwitches(updated)
                        }
                    )
                    LearnModeSwitchRow(
                        title = "7. 盲听拼写",
                        desc = "听发音盲打拼写出完整英文单词 (3次尝试)",
                        checked = activeSwitches.enableBlindSpelling,
                        onCheckedChange = { checked ->
                            val updated = activeSwitches.copy(enableBlindSpelling = checked)
                            if (isReview) viewModel.updateReviewLearningModeSwitches(updated) else viewModel.updateLearningModeSwitches(updated)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    val multiVoiceEnabled by viewModel.multiVoiceSequentialPlay.collectAsStateWithLifecycle()
                    LearnModeSwitchRow(
                        title = "👥 多人轮播发音",
                        desc = "开启后支持美音、英音等多个真人发音人轮流朗读",
                        checked = multiVoiceEnabled,
                        onCheckedChange = { checked ->
                            viewModel.setMultiVoiceSequentialPlay(checked)
                        }
                    )

                    LearnModeSwitchRow(
                        title = "⏩ 答对自动进入下一个",
                        desc = if (isReview) "复习模式默认开启（极速切题，仅打错后显示详情页）；关闭后答对也显示详情页" else "开启后答对直接切题；默认关闭时答对后也进入详情界面查看释义例句",
                        checked = activeAutoAdvance,
                        onCheckedChange = { checked ->
                            if (isReview) viewModel.setReviewAutoAdvanceOnCorrectAnswer(checked) else viewModel.setAutoAdvanceOnCorrectAnswer(checked)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showVoiceSettingsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("发音与词典精细设置", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

        if (showVoiceSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showVoiceSettingsDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("发音与词典设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showVoiceSettingsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                DictionaryAndAudioSettingsCard(
                                    viewModel = viewModel,
                                    attachedMdxName = attachedMdxName,
                                    attachedMdxPath = attachedMdxPath,
                                    voiceProfiles = voiceProfiles
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showVoiceSettingsDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确定")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (!isFinished && sessionItems.isNotEmpty()) {
            val progress = ((currentIndex + 1).toFloat() / sessionItems.size.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            val currentItem = sessionItems.getOrNull(currentIndex)
            if (currentItem != null) {
                AnimatedContent(
                    targetState = currentItem,
                    transitionSpec = {
                        val oldItem = initialState
                        val newItem = targetState

                        if (oldItem?.word?.id == newItem?.word?.id && oldItem?.stage == newItem?.stage) {
                            // 同一单词且同一题型内部状态更新（倒计时刷新、点击提示展开、键盘打字拼写、选项点击变色高亮）
                            // 保持卡片绝对稳定无晃动
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            // 学习模式中，从详情页进入下一个单词时，详情页从右往左平滑移动
                            (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(320)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it } + fadeOut(animationSpec = tween(220)))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "learn_card_transition"
                ) { targetItem ->
                    when (targetItem.stage) {
                        // 盲听识词兼容
                        LearnItemStage.FIRST_LISTEN -> {
                            FirstListenCard(
                                item = targetItem,
                                isAudioPlaying = isAudioPlaying,
                                audioListenCount = audioListenCount,
                                totalRepeat = repeatTimes,
                                isWordRevealed = isWordRevealed,
                                onReplayAudio = { viewModel.playCurrentLearnItemAudio() },
                                onRevealSpelling = { viewModel.forceRevealSpelling() }
                            )
                        }
                        // 释义学习卡片
                        LearnItemStage.FIRST_LISTEN_EXPLAIN,
                        LearnItemStage.CHOOSE_MEANING_EXPLAIN -> {
                            ExplanationCard(
                                item = targetItem,
                                voiceProfiles = voiceProfiles,
                                customBooks = customBooks,
                                defaultExampleSource = defaultExampleSource,
                                selectedBuiltInDictionary = selectedBuiltInDictionary,
                                getCustomSentences = { word -> viewModel.getSentencesForWord(word) },
                                mdxDetail = viewModel.lookupMdxDetail(targetItem.word.word),
                                activeSentenceVoice = activeSentenceVoice,
                                onPronounceWord = { viewModel.pronounceWord(targetItem.word.word) },
                                onPronounceProfile = { id -> viewModel.pronounceWord(targetItem.word.word, id) },
                                onPronounceSequence = { viewModel.pronounceSequence(targetItem.word.word) },
                                onPronounceSentence = { s, v -> viewModel.pronounceSentence(s, v) },
                                onToggleFavorite = { viewModel.toggleFavorite(targetItem.word) },
                                onMarkMastered = { viewModel.toggleMastered(targetItem.word) },
                                isMultiVoiceEnabled = isMultiVoicePlay,
                                onToggleMultiVoice = { viewModel.setMultiVoiceSequentialPlay(it) },
                                autoAdvanceOnCorrectAnswer = activeAutoAdvance,
                                onToggleAutoAdvance = {
                                    if (isReview) viewModel.setReviewAutoAdvanceOnCorrectAnswer(it) else viewModel.setAutoAdvanceOnCorrectAnswer(it)
                                },
                                onAutoPronounce = { viewModel.pronounceWordAndFirstSentence(targetItem.word) },
                                isReview = isReview,
                                onContinue = { viewModel.onContinueFromExplanation() }
                            )
                        }
                        // 1. 盲听选义
                        LearnItemStage.BLIND_LISTEN_MEANING,
                        LearnItemStage.LISTEN_CHOOSE_MEANING -> {
                            ListenChooseMeaningCard(
                                item = targetItem,
                                onPronounce = { viewModel.pronounceWord(targetItem.word.word) },
                                onSelectOption = { optIndex -> viewModel.onSelectChoiceOption(optIndex) },
                                onHint = { viewModel.onHintClicked() },
                                onSeeAnswer = { viewModel.onSeeAnswerClicked() },
                                onDontKnow = { viewModel.onDontKnowClicked() }
                            )
                        }
                        // 2. 看词选义
                        LearnItemStage.LOOK_CHOOSE_MEANING -> {
                            LookChooseMeaningCard(
                                item = targetItem,
                                onPronounce = { viewModel.pronounceWord(targetItem.word.word) },
                                onSelectOption = { optIndex -> viewModel.onSelectChoiceOption(optIndex) },
                                onFamiliar = {
                                    viewModel.stopPronunciation()
                                    viewModel.onActionMarkFamiliar()
                                },
                                onDontKnow = {
                                    viewModel.stopPronunciation()
                                    viewModel.onDontKnowClicked()
                                }
                            )
                        }
                        // 3. 看义说词 (倒计时核对)
                        LearnItemStage.RECALL_BY_MEANING -> {
                            RecallByMeaningCard(
                                item = targetItem,
                                isWordRevealed = isWordRevealed,
                                onDecision = { quality -> viewModel.onRecallDecision(quality) },
                                onPronounce = { viewModel.pronounceWord(targetItem.word.word) }
                            )
                        }
                        // 4. 听音选词
                        LearnItemStage.LISTEN_CHOOSE_WORD -> {
                            ListenChooseWordCard(
                                item = targetItem,
                                onPronounce = { viewModel.pronounceWord(targetItem.word.word) },
                                onSelectOption = { optIndex ->
                                    viewModel.stopPronunciation()
                                    viewModel.onSelectChoiceOption(optIndex)
                                },
                                onHint = {
                                    viewModel.stopPronunciation()
                                    viewModel.onHintClicked()
                                },
                                onSeeAnswer = {
                                    viewModel.stopPronunciation()
                                    viewModel.onSeeAnswerClicked()
                                },
                                onDontKnow = {
                                    viewModel.stopPronunciation()
                                    viewModel.onDontKnowClicked()
                                }
                            )
                        }
                        // 5. 听句选词
                        LearnItemStage.LISTEN_SENTENCE_CHOOSE_WORD,
                        LearnItemStage.CLOZE_SENTENCE -> {
                            ClozeSentenceCard(
                                item = targetItem,
                                onPronounceSentence = { sentence ->
                                    val targetSentence = sentence.ifBlank { targetItem.word.exampleSentence }
                                    viewModel.pronounceSentence(targetSentence, com.example.audio.SentenceVoiceType.TTS_1)
                                },
                                onSelectOption = { optIndex -> viewModel.onSelectChoiceOption(optIndex) },
                                onHint = { viewModel.onHintClicked() },
                                onSeeAnswer = { viewModel.onSeeAnswerClicked() },
                                onDontKnow = {
                                    viewModel.stopPronunciation()
                                    viewModel.onDontKnowClicked()
                                }
                            )
                        }
                        // 6. 英文释义选词
                        LearnItemStage.ENGLISH_MEANING_CHOOSE_WORD -> {
                            EnglishMeaningChooseWordCard(
                                item = targetItem,
                                getEnglishDefs = { w -> viewModel.getEnglishDefinitions(w) },
                                onPronounce = { viewModel.playCurrentLearnItemAudio() },
                                onPronounceDef = { def -> viewModel.pronounceSentence(def, com.example.audio.SentenceVoiceType.TTS_1) },
                                onSelectOption = { optIndex ->
                                    viewModel.stopPronunciation()
                                    viewModel.onSelectChoiceOption(optIndex)
                                },
                                onHint = {
                                    viewModel.stopPronunciation()
                                    viewModel.onHintClicked()
                                },
                                onSeeAnswer = {
                                    viewModel.stopPronunciation()
                                    viewModel.onSeeAnswerClicked()
                                },
                                onDontKnow = {
                                    viewModel.stopPronunciation()
                                    viewModel.onDontKnowClicked()
                                }
                            )
                        }
                        // 7. 盲听拼写 / 听写
                        LearnItemStage.BLIND_SPELLING,
                        LearnItemStage.DICTATION_SPELLING -> {
                            DictationSpellingCard(
                                item = targetItem,
                                onPronounce = { viewModel.pronounceWord(targetItem.word.word) },
                                onTypeLetter = { c -> viewModel.onTypeSpellingLetter(c) },
                                onBackspace = { viewModel.onBackspaceSpelling() },
                                onSkipSpelling = { viewModel.onSkipSpelling() }
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Menus for Mode 1 (盲听识词)
            val currentItemForButtons = sessionItems.getOrNull(currentIndex)
            if (currentItemForButtons?.stage == LearnItemStage.FIRST_LISTEN) {
                FirstListenBottomButtons(
                    isWordRevealed = isWordRevealed,
                    onFamiliar = { viewModel.onActionMarkFamiliar() },
                    onKnow = { viewModel.onActionKnow() },
                    onFuzzy = { viewModel.onActionFuzzy() },
                    onHint = { viewModel.onActionHint() }
                )
            }
        } else {
            // Victory Completion Screen
            SessionCompletedView(
                passedCount = passedCount,
                masteredCount = masteredCount,
                reinforcedCount = reinforcedCount,
                isReview = isReview,
                onRestart = { viewModel.startNextGroupLearning() },
                onBackHome = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        }
    }
}

@Composable
private fun FirstListenCard(
    item: LearnSessionItem,
    isAudioPlaying: Boolean,
    audioListenCount: Int,
    totalRepeat: Int,
    isWordRevealed: Boolean,
    onReplayAudio: () -> Unit,
    onRevealSpelling: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAudioPlaying) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Audio Status Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isAudioPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isAudioPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isAudioPlaying) "正在播放第 $audioListenCount/$totalRepeat 遍..." else "点击重听发音",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isAudioPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Animated Audio Speaker Pulse
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable { onReplayAudio() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "发音中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Revealed Word or Listening State Prompt
            if (isWordRevealed) {
                Text(
                    text = item.word.word,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (item.word.phonetic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.word.phonetic,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "请在下方选择你的熟悉度",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "🎧 盲听识词中",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "播放完毕后将自动显示单词拼写",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onRevealSpelling,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("直接显示单词", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FirstListenBottomButtons(
    isWordRevealed: Boolean,
    onFamiliar: () -> Unit,
    onKnow: () -> Unit,
    onFuzzy: () -> Unit,
    onHint: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. 熟悉 (标熟 - 斩词跳过)
            Button(
                onClick = onFamiliar,
                enabled = isWordRevealed,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("action_familiar_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("熟悉", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text("直接标熟·跳过", fontSize = 10.sp)
                }
            }

            // 2. 认识 (进入下一词，末尾听音选义确认)
            Button(
                onClick = onKnow,
                enabled = isWordRevealed,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("action_know_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("认识", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text("末尾选义确认", fontSize = 10.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. 模糊 (展示释义，顺延 3 个词后复习)
            Button(
                onClick = onFuzzy,
                enabled = isWordRevealed,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("action_fuzzy_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarningOrange.copy(alpha = 0.15f),
                    contentColor = WarningOrange
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("模糊", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("看释义·顺延3词", fontSize = 10.sp)
                }
            }

            // 4. 提示 (展示释义，顺延 4 个词后复习)
            Button(
                onClick = onHint,
                enabled = isWordRevealed,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("action_hint_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed.copy(alpha = 0.12f),
                    contentColor = DangerRed
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("提示", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text("看释义·顺延4词", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(
    item: LearnSessionItem,
    voiceProfiles: List<com.example.data.dict.VoiceProfile>,
    customBooks: List<com.example.data.model.BookEntity> = emptyList(),
    defaultExampleSource: String = "builtin",
    selectedBuiltInDictionary: com.example.data.dict.BuiltInDictionary = com.example.data.dict.BuiltInDictionary.DEFAULT,
    getCustomSentences: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.data.model.BookSentenceEntity>>,
    mdxDetail: com.example.data.dict.ExtractedDictItem?,
    activeSentenceVoice: com.example.audio.SentenceVoiceType?,
    onPronounceWord: () -> Unit,
    onPronounceProfile: (String) -> Unit,
    onPronounceSequence: () -> Unit = {},
    onPronounceSentence: (String, com.example.audio.SentenceVoiceType) -> Unit,
    onToggleFavorite: () -> Unit,
    onMarkMastered: () -> Unit,
    isMultiVoiceEnabled: Boolean = false,
    onToggleMultiVoice: ((Boolean) -> Unit)? = null,
    autoAdvanceOnCorrectAnswer: Boolean = false,
    onToggleAutoAdvance: ((Boolean) -> Unit)? = null,
    onAutoPronounce: (() -> Unit)? = null,
    isReview: Boolean = false,
    onContinue: () -> Unit
) {
    // 学习模式中每次进入详情页 单词和第一个例句自动发音
    LaunchedEffect(item.word.id, item.stage) {
        onAutoPronounce?.invoke()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                val customSentences by getCustomSentences(item.word.word).collectAsStateWithLifecycle(initialValue = emptyList())
                WordCard(
                    word = item.word,
                    isMeaningRevealed = true,
                    customBooks = customBooks,
                    customSentences = customSentences,
                    defaultExampleSource = defaultExampleSource,
                    selectedBuiltInDictionary = selectedBuiltInDictionary,
                    onPronounce = onPronounceWord,
                    onPronounceProfile = onPronounceProfile,
                    onPronounceSequence = onPronounceSequence,
                    voiceProfiles = voiceProfiles,
                    mdxDetail = mdxDetail,
                    onPronounceSentence = onPronounceSentence,
                    activeSentenceVoice = activeSentenceVoice,
                    onRevealMeaning = {},
                    onToggleFavorite = onToggleFavorite,
                    onMarkMastered = onMarkMastered,
                    isMultiVoiceEnabled = isMultiVoiceEnabled,
                    onToggleMultiVoice = onToggleMultiVoice,
                    autoAdvanceOnCorrectAnswer = autoAdvanceOnCorrectAnswer,
                    onToggleAutoAdvance = onToggleAutoAdvance,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("explain_continue_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isReview) "记住了，继续复习" else "记住了，继续学习", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// 中文释义按词性分行展示组件
@Composable
private fun MeaningByPosView(
    rawMeaning: String,
    pos: String = "",
    modifier: Modifier = Modifier,
    definitionStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    definitionColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    val parsedMeanings = remember(rawMeaning, pos) {
        val list = EcdictFormatter.parseMeanings(rawMeaning, pos)
        val grouped = mutableListOf<EcdictFormatter.FormattedMeaning>()
        for (item in list) {
            val last = grouped.lastOrNull()
            if (last != null && last.pos == item.pos && last.pos.isNotBlank()) {
                grouped[grouped.lastIndex] = last.copy(definition = "${last.definition}；${item.definition}")
            } else {
                grouped.add(item)
            }
        }
        if (grouped.isNotEmpty()) grouped else listOf(EcdictFormatter.FormattedMeaning(pos = pos, definition = rawMeaning))
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        parsedMeanings.forEach { m ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 1.dp)
            ) {
                if (m.pos.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = m.pos,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = m.definition,
                    style = definitionStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = definitionColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ChoiceOptionItem(
    index: Int,
    optionText: String,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    isAnswered: Boolean,
    isFlashing: Boolean,
    flashingIsCorrect: Boolean,
    isUserSelectionCorrect: Boolean?,
    prefixColor: Color = MaterialTheme.colorScheme.primary,
    optionTextStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    onSelectOption: () -> Unit
) {
    val isUserSelectedWrong = isSelected && isUserSelectionCorrect == false
    val isUserSelectedRight = isSelected && isUserSelectionCorrect == true
    val isCorrectHighlight = isAnswered && isCorrectOption && !isUserSelectedRight

    val targetContainerColor = when {
        isFlashing && flashingIsCorrect -> SuccessGreen.copy(alpha = 0.25f)
        isFlashing && !flashingIsCorrect -> DangerRed.copy(alpha = 0.25f)
        isUserSelectedRight -> SuccessGreen.copy(alpha = 0.22f)
        isUserSelectedWrong -> DangerRed.copy(alpha = 0.22f)
        isCorrectHighlight -> SuccessGreen.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val targetBorderColor = when {
        isFlashing && flashingIsCorrect -> SuccessGreen
        isFlashing && !flashingIsCorrect -> DangerRed
        isUserSelectedRight -> SuccessGreen
        isUserSelectedWrong -> DangerRed
        isCorrectHighlight -> SuccessGreen
        else -> Color.Transparent
    }

    val targetAlpha = when {
        !isAnswered -> 1f
        isUserSelectedRight || isUserSelectedWrong || isCorrectHighlight -> 1f
        else -> 0.45f
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 150),
        label = "choice_bg"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 150),
        label = "choice_border"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 150),
        label = "choice_alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isUserSelectedRight || isUserSelectedWrong) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "choice_scale"
    )

    val borderWidth = if (isFlashing) 2.5.dp else if (isUserSelectedRight || isUserSelectedWrong || isCorrectHighlight) 2.dp else 1.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = animatedAlpha
            }
            .clip(RoundedCornerShape(14.dp))
            .border(borderWidth, animatedBorderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = !isAnswered) { onSelectOption() },
        shape = RoundedCornerShape(14.dp),
        color = animatedContainerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${('A' + index)}. ",
                fontWeight = FontWeight.Bold,
                color = when {
                    isUserSelectedRight || isCorrectHighlight -> SuccessGreen
                    isUserSelectedWrong -> DangerRed
                    else -> prefixColor
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = optionText,
                style = optionTextStyle,
                fontWeight = if (isUserSelectedRight || isUserSelectedWrong || isCorrectHighlight) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isUserSelectedRight || isCorrectHighlight -> SuccessGreen
                    isUserSelectedWrong -> DangerRed
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            if (isUserSelectedRight || (isFlashing && flashingIsCorrect) || isCorrectHighlight) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "正确",
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isUserSelectedWrong || (isFlashing && !flashingIsCorrect)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "错误",
                    tint = DangerRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 1. 盲听选义 卡片 (提示后显示单词，看答案闪烁绿框2次跳转下词，错选闪烁红框2次展示释义例句)
@Composable
private fun ListenChooseMeaningCard(
    item: LearnSessionItem,
    onPronounce: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onHint: () -> Unit,
    onSeeAnswer: () -> Unit,
    onDontKnow: () -> Unit
) {
    var secondsLeft by remember(item.word.id) { mutableStateOf(5) }

    LaunchedEffect(item.word.id, item.isHintClicked, item.isWordRevealed, item.selectedOptionIndex) {
        if (!item.isHintClicked && !item.isWordRevealed && item.selectedOptionIndex == null) {
            secondsLeft = 5
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft -= 1
            }
            onHint()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("盲听选义", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sound button
            IconButton(
                onClick = onPronounce,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "播放发音", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 固定高度 60dp 提示区域，揭晓单词前后高度一致，彻底消除下方选项下移晃动及误触
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.isWordRevealed || item.isHintClicked) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = item.word.word,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        if (item.word.phonetic.isNotBlank()) {
                            Text(
                                text = item.word.phonetic,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "点击喇叭重听发音，${secondsLeft}秒后自动显示单词",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Choices with enhanced animation & dual highlight
            val isAnswered = item.selectedOptionIndex != null || item.flashingOptionIndex != null
            item.multipleChoiceOptions.forEachIndexed { index, optionText ->
                ChoiceOptionItem(
                    index = index,
                    optionText = optionText,
                    isSelected = item.selectedOptionIndex == index,
                    isCorrectOption = optionText == item.word.meaning,
                    isAnswered = isAnswered,
                    isFlashing = item.flashingOptionIndex == index,
                    flashingIsCorrect = item.flashingIsCorrect,
                    isUserSelectionCorrect = item.isAnswerCorrect,
                    prefixColor = MaterialTheme.colorScheme.primary,
                    onSelectOption = { onSelectOption(index) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 底部操作按钮：不认识 (DangerRed 48dp) 与 提示/看答案
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDontKnow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_dont_know_blind_listen")
                ) {
                    Text("不认识", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (!item.isHintClicked) {
                    Button(
                        onClick = onHint,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_hint_blind_listen")
                    ) {
                        CircularProgressIndicator(
                            progress = secondsLeft / 5f,
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${secondsLeft}s 提示", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onSeeAnswer,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_see_answer_blind_listen")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("看答案", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 2. 看词选义 卡片
@Composable
private fun LookChooseMeaningCard(
    item: LearnSessionItem,
    onPronounce: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onFamiliar: () -> Unit,
    onDontKnow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("看词选义", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = item.word.word,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            if (item.word.phonetic.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.word.phonetic,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onPronounce, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "播放发音", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Choices with enhanced animation & dual highlight
            val isAnswered = item.selectedOptionIndex != null || item.flashingOptionIndex != null
            item.multipleChoiceOptions.forEachIndexed { index, optionText ->
                ChoiceOptionItem(
                    index = index,
                    optionText = optionText,
                    isSelected = item.selectedOptionIndex == index,
                    isCorrectOption = optionText == item.word.meaning,
                    isAnswered = isAnswered,
                    isFlashing = item.flashingOptionIndex == index,
                    flashingIsCorrect = item.flashingIsCorrect,
                    isUserSelectionCorrect = item.isAnswerCorrect,
                    prefixColor = MaterialTheme.colorScheme.primary,
                    onSelectOption = { onSelectOption(index) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 底部操作按钮：固定两个按钮 不认识 (DangerRed) 与 已熟悉 (SuccessGreen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDontKnow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_dont_know_look_choose")
                ) {
                    Text("不认识", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Button(
                    onClick = onFamiliar,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_familiar_look_choose")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("已熟悉", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// 英文释义选词卡片
@Composable
private fun EnglishMeaningChooseWordCard(
    item: LearnSessionItem,
    getEnglishDefs: (com.example.data.model.WordEntity) -> List<String>,
    onPronounce: () -> Unit,
    onPronounceDef: (String) -> Unit = {},
    onSelectOption: (Int) -> Unit,
    onHint: () -> Unit,
    onSeeAnswer: () -> Unit,
    onDontKnow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .testTag("english_meaning_choose_word_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("英文释义选词", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val defs = getEnglishDefs(item.word)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                defs.forEach { def ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPronounceDef(def) }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = def,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 英文释义选词提示与答案展示区：固定高度 60dp 消除卡片选项晃动
            // 点击提示只显示中文释义，不显示单词；点击看答案才显示单词与释义
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.isWordRevealed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "正确答案: ${item.word.word}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        MeaningByPosView(
                            rawMeaning = item.word.meaning,
                            pos = item.word.pos,
                            definitionStyle = MaterialTheme.typography.bodyMedium,
                            definitionColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (item.isHintClicked) {
                    // 点击提示按钮只显示中文释义，不要显示单词
                    MeaningByPosView(
                        rawMeaning = item.word.meaning,
                        pos = item.word.pos,
                        definitionStyle = MaterialTheme.typography.titleMedium,
                        definitionColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text("结合上方英文释义，选出正确的英文单词", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isAnswered = item.selectedOptionIndex != null || item.flashingOptionIndex != null
            item.multipleChoiceOptions.forEachIndexed { index, optionText ->
                ChoiceOptionItem(
                    index = index,
                    optionText = optionText,
                    isSelected = item.selectedOptionIndex == index,
                    isCorrectOption = optionText.equals(item.word.word, ignoreCase = true),
                    isAnswered = isAnswered,
                    isFlashing = item.flashingOptionIndex == index,
                    flashingIsCorrect = item.flashingIsCorrect,
                    isUserSelectionCorrect = item.isAnswerCorrect,
                    prefixColor = MaterialTheme.colorScheme.secondary,
                    optionTextStyle = MaterialTheme.typography.titleMedium,
                    onSelectOption = { onSelectOption(index) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 底部操作按钮：不认识 (DangerRed 48dp) 与 提示(WarningOrange)/看答案(SuccessGreen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDontKnow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_dont_know_english_def")
                ) {
                    Text("不认识", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (!item.isHintClicked) {
                    Button(
                        onClick = onHint,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_hint_english_def")
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("提示", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onSeeAnswer,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_see_answer_english_def")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("看答案", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 3. 听音选词 卡片
@Composable
private fun ListenChooseWordCard(
    item: LearnSessionItem,
    onPronounce: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onHint: () -> Unit,
    onSeeAnswer: () -> Unit,
    onDontKnow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("听音选词 (拼写辨析)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            IconButton(
                onClick = onPronounce,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "播放发音", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 固定高度 60dp 提示区域，揭晓释义时固定高度，避免下方选项下移晃动及误触
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.isWordRevealed || item.isHintClicked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        MeaningByPosView(
                            rawMeaning = item.word.meaning,
                            pos = item.word.pos,
                            definitionStyle = MaterialTheme.typography.titleMedium,
                            definitionColor = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text("播放发音，从 4 个英文单词中选出正确拼写", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isAnswered = item.selectedOptionIndex != null || item.flashingOptionIndex != null
            item.multipleChoiceOptions.forEachIndexed { index, optionText ->
                ChoiceOptionItem(
                    index = index,
                    optionText = optionText,
                    isSelected = item.selectedOptionIndex == index,
                    isCorrectOption = optionText.equals(item.word.word, ignoreCase = true),
                    isAnswered = isAnswered,
                    isFlashing = item.flashingOptionIndex == index,
                    flashingIsCorrect = item.flashingIsCorrect,
                    isUserSelectionCorrect = item.isAnswerCorrect,
                    prefixColor = MaterialTheme.colorScheme.secondary,
                    optionTextStyle = MaterialTheme.typography.titleMedium,
                    onSelectOption = { onSelectOption(index) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 底部操作按钮：不认识 (DangerRed 48dp) 与 提示/看答案
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDontKnow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_dont_know_listen_choose_word")
                ) {
                    Text("不认识", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (!item.isHintClicked) {
                    Button(
                        onClick = onHint,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_hint_listen_choose_word")
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("提示", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onSeeAnswer,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_see_answer_listen_choose_word")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("看答案", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 7. 盲听拼写 / 听写 卡片
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictationSpellingCard(
    item: LearnSessionItem,
    onPronounce: () -> Unit,
    onTypeLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSkipSpelling: () -> Unit
) {
    val targetWord = item.word.word.lowercase()
    val typed = item.typedSpelling.lowercase()

    val keyboardLetters = remember(targetWord) {
        val targetChars = targetWord.filter { it in 'a'..'z' }.toList()
        val fixedTotal = if (targetChars.size > 10) 14 else 12
        val neededExtra = (fixedTotal - targetChars.size).coerceAtLeast(2)
        val extraChars = ('a'..'z').filterNot { it in targetChars }.shuffled().take(neededExtra)
        (targetChars + extraChars).shuffled()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("盲听拼写 / 听写", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "剩余尝试机会: ${item.remainingSpellingAttempts}/3",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.remainingSpellingAttempts <= 1) DangerRed else MaterialTheme.colorScheme.primary
            )

            // 拼写正确 1 秒提示
            if (item.spellingSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreen.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "拼写正确！${item.word.word}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            IconButton(
                onClick = onPronounce,
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "播放发音", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(item.word.meaning, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)

            Spacer(modifier = Modifier.height(16.dp))

            // Spelling Slots - 自适应长单词尺寸与换行，防止超出屏幕
            val wordLen = targetWord.length
            val slotWidth = when {
                wordLen <= 6 -> 38.dp
                wordLen <= 8 -> 32.dp
                wordLen <= 10 -> 28.dp
                wordLen <= 13 -> 24.dp
                else -> 20.dp
            }
            val slotHeight = when {
                wordLen <= 6 -> 44.dp
                wordLen <= 8 -> 40.dp
                wordLen <= 10 -> 36.dp
                wordLen <= 13 -> 32.dp
                else -> 28.dp
            }
            val slotFontSize = when {
                wordLen <= 6 -> 18.sp
                wordLen <= 8 -> 16.sp
                wordLen <= 10 -> 14.sp
                wordLen <= 13 -> 12.sp
                else -> 11.sp
            }
            val slotHorizontalPadding = when {
                wordLen <= 6 -> 3.dp
                wordLen <= 8 -> 2.5.dp
                wordLen <= 10 -> 2.dp
                else -> 1.5.dp
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                targetWord.forEachIndexed { index, targetChar ->
                    if (targetChar == ' ') {
                        Spacer(modifier = Modifier.width(slotWidth / 2))
                    } else {
                        val letter = if (item.spellingSuccess) targetChar.toString() else typed.getOrNull(index)?.toString()
                        val slotColor = when {
                            item.spellingSuccess -> SuccessGreen.copy(alpha = 0.2f)
                            letter != null -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val textColor = when {
                            item.spellingSuccess -> SuccessGreen
                            letter != null -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            modifier = Modifier
                                .padding(horizontal = slotHorizontalPadding)
                                .size(width = slotWidth, height = slotHeight),
                            shape = RoundedCornerShape(8.dp),
                            color = slotColor
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = letter ?: "",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = slotFontSize,
                                    fontFamily = FontFamily.Monospace,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 固定字母键盘 (按单词候选与干扰字生成，键位完全固定，点击不位移不变动)
            val half = (keyboardLetters.size + 1) / 2
            val row1Letters = remember(keyboardLetters) { keyboardLetters.take(half) }
            val row2Letters = remember(keyboardLetters) { keyboardLetters.drop(half) }

            val letterKeyWidth = if (keyboardLetters.size > 12) 36.dp else 42.dp
            val letterKeyHeight = 44.dp

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row1Letters.forEach { char ->
                        Surface(
                            modifier = Modifier
                                .size(width = letterKeyWidth, height = letterKeyHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onTypeLetter(char) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = char.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Row 2 + Backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row2Letters.forEach { char ->
                        Surface(
                            modifier = Modifier
                                .size(width = letterKeyWidth, height = letterKeyHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onTypeLetter(char) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = char.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Backspace Button
                    Surface(
                        modifier = Modifier
                            .height(letterKeyHeight)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onBackspace() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "退格", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("删除", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // 拼写错误 5 秒正确拼法提示
            if (item.showCorrectSpelling) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "拼写错误 (剩余 ${item.remainingSpellingAttempts} 次机会)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "正确拼法: ${item.word.word}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "5秒后自动隐藏，点击字母即可重新拼写",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 盲听拼写一键跳过橙色长按钮 (点击后直接跳过这个单词的拼写，并且不标记为失败)
            Button(
                onClick = onSkipSpelling,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_skip_spelling")
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("一键跳过", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// 3. 看义说词 卡片 (3秒倒计时 -> 揭晓 -> 正确/忘了/错误)
@Composable
private fun RecallByMeaningCard(
    item: LearnSessionItem,
    isWordRevealed: Boolean,
    onDecision: (ReviewQuality) -> Unit,
    onPronounce: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WarningOrange.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarningOrange)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("看义说词 (快速反应)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                MeaningByPosView(
                    rawMeaning = item.word.meaning,
                    pos = item.word.pos,
                    definitionStyle = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isWordRevealed && item.countdownSecondsLeft > 0) {
                // Countdown circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${item.countdownSecondsLeft}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("请尝试大声说出该英文单词...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Answer revealed
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.word.word,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.word.phonetic.isNotBlank()) {
                            Text(
                                text = item.word.phonetic,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons: 正确 / 忘了 / 错误
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onDecision(ReviewQuality.KNOW) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("正确", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onDecision(ReviewQuality.FUZZY) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                    ) {
                        Text("忘了", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onDecision(ReviewQuality.FORGOT) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("错误", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 5. 例句语境完形填空 / 听句选词 卡片
@Composable
private fun ClozeSentenceCard(
    item: LearnSessionItem,
    onPronounceSentence: (String) -> Unit,
    onSelectOption: (Int) -> Unit,
    onHint: () -> Unit,
    onSeeAnswer: () -> Unit,
    onDontKnow: () -> Unit
) {
    val sentence = item.word.exampleSentence.split(" ||| ").firstOrNull { it.isNotBlank() } ?: item.word.exampleSentence
    val sentenceTrans = item.word.exampleTranslation.split(" ||| ").firstOrNull { it.isNotBlank() } ?: item.word.exampleTranslation
    val maskedSentence = remember(sentence, item.word.word, item.isHintClicked, item.isWordRevealed) {
        if (sentence.isNotBlank()) {
            if (item.isHintClicked || item.isWordRevealed) {
                // If hint clicked, show the word in sentence
                sentence
            } else {
                sentence.replace(Regex("(?i)\\b${Regex.escape(item.word.word)}\\b"), "[ ________ ]")
            }
        } else {
            "结合释义选出对应单词:\n\"${item.word.meaning}\"\n[ ________ ]"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WarningOrange.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarningOrange)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("听句选词 / 例句强化", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isCorrect = item.isAnswerCorrect == true || item.flashingIsCorrect && item.flashingOptionIndex != null
            val isError = item.isAnswerCorrect == false || (!item.flashingIsCorrect && item.flashingOptionIndex != null)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPronounceSentence(sentence) }
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "例句语境 (点击朗读例句)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isError || isCorrect) sentence else maskedSentence,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        if (sentenceTrans.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sentenceTrans,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Spacer(modifier = Modifier.height(20.dp))

            // Options with enhanced animation & dual highlight
            val isAnswered = item.selectedOptionIndex != null || item.flashingOptionIndex != null
            item.multipleChoiceOptions.forEachIndexed { index, optionText ->
                ChoiceOptionItem(
                    index = index,
                    optionText = optionText,
                    isSelected = item.selectedOptionIndex == index,
                    isCorrectOption = optionText.equals(item.word.word, ignoreCase = true),
                    isAnswered = isAnswered,
                    isFlashing = item.flashingOptionIndex == index,
                    flashingIsCorrect = item.flashingIsCorrect,
                    isUserSelectionCorrect = item.isAnswerCorrect,
                    prefixColor = MaterialTheme.colorScheme.primary,
                    optionTextStyle = MaterialTheme.typography.bodyMedium,
                    onSelectOption = { onSelectOption(index) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 底部操作按钮：不认识 (DangerRed 48dp) 与 提示(WarningOrange)/看答案(SuccessGreen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDontKnow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_dont_know_cloze")
                ) {
                    Text("不认识", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (!item.isHintClicked) {
                    Button(
                        onClick = onHint,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_hint_cloze")
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("提示", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onSeeAnswer,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_see_answer_cloze")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("看答案", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnModeSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SessionCompletedView(
    passedCount: Int,
    masteredCount: Int,
    reinforcedCount: Int,
    isReview: Boolean = false,
    onRestart: () -> Unit,
    onBackHome: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isReview) "🎉 艾宾浩斯复习完成！" else "今日新词通关，击败遗忘！",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isReview) "本次复习单词已全部强化完成，艾宾浩斯记忆曲线已更新！" else "当前队列所有单词均已完成二次确认或直接标熟，已科学录入艾宾浩斯复习曲线！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Session Stats Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$passedCount", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    Text(if (isReview) "复习过关掌握" else "二次确认掌握", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$masteredCount", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.tertiary)
                    Text("直接标熟跳过", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$reinforcedCount", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WarningOrange)
                    Text("巩固强化次数", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isReview) {
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("session_next_group_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("再学1组新词", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onBackHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = if (isReview) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) {
                Text("返回首页", fontWeight = FontWeight.Bold)
            }
        }
    }
}
