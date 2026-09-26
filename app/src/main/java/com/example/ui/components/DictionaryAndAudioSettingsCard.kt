package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dict.VoiceAccent
import com.example.data.dict.VoiceProfile
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryAndAudioSettingsCard(
    viewModel: MainViewModel,
    attachedMdxName: String,
    attachedMdxPath: String?,
    voiceProfiles: List<VoiceProfile>,
    modifier: Modifier = Modifier
) {
    var showAddVoiceDialog by remember { mutableStateOf(false) }
    var newVoiceName by remember { mutableStateOf("") }
    var newVoiceAccent by remember { mutableStateOf(VoiceAccent.US) }
    var newTtsVoiceName by remember { mutableStateOf<String?>(null) }
    var newTtsPitch by remember { mutableStateOf(1.0f) }
    var newTtsSpeechRate by remember { mutableStateOf(0.92f) }

    var editingTtsProfile by remember { mutableStateOf<VoiceProfile?>(null) }

    var auditionWord by remember { mutableStateOf("serendipity") }

    val multiVoiceSequentialPlay by viewModel.multiVoiceSequentialPlay.collectAsState()
    val sentenceVoiceRotationEnabled by viewModel.sentenceVoiceRotationEnabled.collectAsState()
    val downloadState by viewModel.voiceDownloadState.collectAsState()

    // Add Custom Voice Profile Dialog (TTS)
    if (showAddVoiceDialog) {
        val availableTtsVoices = remember { viewModel.getAvailableTtsVoices() }
        AlertDialog(
            onDismissRequest = { showAddVoiceDialog = false },
            title = {
                Text(
                    text = "添加自定义本地 TTS 音源",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newVoiceName,
                        onValueChange = { newVoiceName = it },
                        label = { Text("音源名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VoiceAccent.values().forEach { accent ->
                            FilterChip(
                                selected = (newVoiceAccent == accent),
                                onClick = { newVoiceAccent = accent },
                                label = { Text("${accent.flag} ${accent.displayName}") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "选择系统 TTS 发音人:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (availableTtsVoices.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "系统默认发音人",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        var expandedVoiceDropdown by remember { mutableStateOf(false) }
                        val selectedVoiceObj = availableTtsVoices.find { it.name == newTtsVoiceName }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedVoiceDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = selectedVoiceObj?.displayName ?: "系统默认发音人",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = expandedVoiceDropdown,
                                onDismissRequest = { expandedVoiceDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("系统默认发音人") },
                                    onClick = {
                                        newTtsVoiceName = null
                                        expandedVoiceDropdown = false
                                    }
                                )
                                availableTtsVoices.forEach { v ->
                                    DropdownMenuItem(
                                        text = { Text("${v.displayName} (${v.localeStr})") },
                                        onClick = {
                                            newTtsVoiceName = v.name
                                            expandedVoiceDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("语速: ${Math.round(newTtsSpeechRate * 100)}% (${String.format("%.2f", newTtsSpeechRate)}x)", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = newTtsSpeechRate,
                        onValueChange = { newTtsSpeechRate = (Math.round(it * 100f) / 100f) },
                        valueRange = 0.5f..1.5f
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("音量: ${Math.round(newTtsPitch * 100)}%", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = newTtsPitch,
                        onValueChange = { newTtsPitch = (Math.round(it * 10f) / 10f) },
                        valueRange = 0.0f..1.5f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newVoiceName.ifBlank { "自定义 TTS" }
                        viewModel.addLocalTtsVoice(name, newVoiceAccent, newTtsVoiceName, newTtsPitch, newTtsSpeechRate)
                        showAddVoiceDialog = false
                        newVoiceName = ""
                    }
                ) {
                    Text("添加音源")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVoiceDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 3. Edit Existing TTS Profile Dialog
    editingTtsProfile?.let { profile ->
        var editName by remember(profile) { mutableStateOf(profile.name) }
        var editAccent by remember(profile) { mutableStateOf(profile.accent) }
        var editTtsVoiceName by remember(profile) { mutableStateOf(profile.ttsVoiceName) }
        var editPitch by remember(profile) { mutableStateOf(profile.pitch) }
        var editSpeechRate by remember(profile) { mutableStateOf(profile.speechRate) }
        val availableTtsVoices = remember { viewModel.getAvailableTtsVoices() }

        AlertDialog(
            onDismissRequest = { editingTtsProfile = null },
            title = { Text("配置 TTS 发音人参数", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("音源显示名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VoiceAccent.values().forEach { accent ->
                            FilterChip(
                                selected = (editAccent == accent),
                                onClick = { editAccent = accent },
                                label = { Text("${accent.flag} ${accent.displayName}") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("选择 TTS 发音人角色:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    var expandedVoiceDropdown by remember { mutableStateOf(false) }
                    val selectedVoiceObj = availableTtsVoices.find { it.name == editTtsVoiceName }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedVoiceDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedVoiceObj?.displayName ?: "系统默认发音人",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = expandedVoiceDropdown,
                            onDismissRequest = { expandedVoiceDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("系统默认发音人") },
                                onClick = {
                                    editTtsVoiceName = null
                                    expandedVoiceDropdown = false
                                }
                            )
                            availableTtsVoices.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.displayName} (${v.localeStr})") },
                                    onClick = {
                                        editTtsVoiceName = v.name
                                        expandedVoiceDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("语速: ${Math.round(editSpeechRate * 100)}% (${String.format("%.2f", editSpeechRate)}x)", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = editSpeechRate,
                        onValueChange = { editSpeechRate = (Math.round(it * 100f) / 100f) },
                        valueRange = 0.5f..1.5f
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("音量: ${Math.round(editPitch * 100)}%", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = editPitch,
                        onValueChange = { editPitch = (Math.round(it * 10f) / 10f) },
                        valueRange = 0.0f..1.5f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTtsVoiceProfile(
                            profileId = profile.id,
                            name = editName.ifBlank { "TTS 音源" },
                            accent = editAccent,
                            ttsVoiceName = editTtsVoiceName,
                            pitch = editPitch,
                            speechRate = editSpeechRate
                        )
                        editingTtsProfile = null
                    }
                ) {
                    Text("保存配置")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTtsProfile = null }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. 试听对比测试台
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "发音试听测试",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = { viewModel.pronounceSequence(auditionWord) },
                        modifier = Modifier.height(24.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("轮播已启音源", fontSize = 10.sp)
                    }
                }

                val quickWords = listOf("serendipity", "fascinating", "aesthetic", "resilience")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickWords.forEach { w ->
                        val isSelected = (auditionWord == w)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    auditionWord = w
                                    viewModel.pronounceWord(w)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 3.dp)) {
                                Text(
                                    text = w,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global Download Status Indicator Banner (if downloading)
        AnimatedVisibility(visible = downloadState.isDownloading || downloadState.totalCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (downloadState.isDownloading) Icons.Default.Sync else Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (downloadState.isDownloading) "正在下载【${downloadState.profileName}】..." else "【${downloadState.profileName}】已就绪",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (downloadState.isDownloading) {
                            IconButton(
                                onClick = { viewModel.cancelBatchAudioDownload() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "取消下载", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    if (downloadState.isDownloading) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "进度: ${downloadState.completedCount}/${downloadState.totalCount} (成功 ${downloadState.successCount}, 失败 ${downloadState.failCount})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                        if (downloadState.statusMessage.isNotBlank()) {
                            Text(
                                text = downloadState.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // SECTION 1: 单词发音设置卡片
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (multiVoiceSequentialPlay) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = if (multiVoiceSequentialPlay) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "单词轮播开关",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (multiVoiceSequentialPlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (multiVoiceSequentialPlay) "已开启：仅使用朗文美音/有道美音/有道英音轮播，失败时由对应的 1美音/2英音/3澳音 TTS 替补" else "已关闭：单词发音优先使用默认发音方案（朗文美音）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = multiVoiceSequentialPlay,
                    onCheckedChange = { viewModel.setMultiVoiceSequentialPlay(it) },
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "单词发音方案",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { showAddVoiceDialog = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加自定义音源", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }

        // List of Word Voice Profiles (朗文美音, 有道美音, 有道英音)
        val wordProfiles = voiceProfiles.filter { it.sourceType != "LOCAL_TTS" }
        wordProfiles.forEach { profile ->
            VoiceProfileCardItem(
                profile = profile,
                viewModel = viewModel,
                downloadState = downloadState,
                auditionWord = auditionWord,
                onEditTtsProfile = { editingTtsProfile = it }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

        // SECTION 2: 例句与英文释义发音设置卡片
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (sentenceVoiceRotationEnabled) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = if (sentenceVoiceRotationEnabled) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "例句轮播开关",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sentenceVoiceRotationEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (sentenceVoiceRotationEnabled) "已开启：主页例句和英文释义发音在【1美音 tts】、【2英音 tts】、【3澳音 tts】中循环播放" else "已关闭：例句与英文释义默认使用 1美音 tts 朗读",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = sentenceVoiceRotationEnabled,
                    onCheckedChange = { viewModel.setSentenceVoiceRotationEnabled(it) },
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Text(
            text = "例句与英文释义发音方案 (TTS)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // List of Sentence TTS Profiles (1美音 tts, 2英音 tts, 3澳音 tts)
        val sentenceProfiles = voiceProfiles.filter { it.sourceType == "LOCAL_TTS" }
        sentenceProfiles.forEach { profile ->
            VoiceProfileCardItem(
                profile = profile,
                viewModel = viewModel,
                downloadState = downloadState,
                auditionWord = auditionWord,
                onEditTtsProfile = { editingTtsProfile = it }
            )
        }
        // Clear All Audio Caches Button
        OutlinedButton(
            onClick = { viewModel.clearAllAudioCaches() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("清空全部已下载离线发音包", fontSize = 11.sp)
        }
    }
}

@Composable
fun VoiceProfileCardItem(
    profile: VoiceProfile,
    viewModel: MainViewModel,
    downloadState: com.example.data.dict.VoiceBatchDownloadState,
    auditionWord: String,
    onEditTtsProfile: (VoiceProfile) -> Unit
) {
    val stats = viewModel.voiceManager.getVoiceCacheStats(profile)
    val isCurrentDownloading = (downloadState.isDownloading && downloadState.profileId == profile.id)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (profile.isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = if (profile.isDefault) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            // Row 1: Flag + Name + Badges + Stats + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = profile.accent.flag, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (profile.isDefault) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "默认",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (profile.sourceType == "LOCAL_TTS") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "TTS例句",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    if (profile.sourceType == "LOCAL_TTS") {
                        Text(
                            text = "(${profile.ttsVoiceName?.take(12) ?: "系统默认"})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 9.sp
                        )
                    } else {
                        Text(
                            text = "(${stats.fileCount}词)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stats.fileCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                }

                Switch(
                    checked = profile.isEnabled,
                    onCheckedChange = { isChecked ->
                        viewModel.toggleVoiceProfile(profile.id, isChecked)
                    },
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Set Default + Audition Play Button + Batch Download + Clear Cache
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (profile.sourceType != "LOCAL_TTS") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.setDefaultVoiceProfile(profile.id) }
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (profile.isDefault) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (profile.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (profile.isDefault) "默认单词发音" else "设为默认",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (profile.isDefault) FontWeight.Bold else FontWeight.Normal,
                            color = if (profile.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { viewModel.pronounceWord(auditionWord, profile.id) },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(24.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("试听", fontSize = 10.sp)
                    }

                    if (profile.sourceType == "LOCAL_TTS") {
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = { onEditTtsProfile(profile) },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(24.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("发音人", fontSize = 10.sp)
                        }
                    }

                    if (profile.sourceType != "LOCAL_TTS") {
                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedButton(
                            onClick = {
                                if (isCurrentDownloading) {
                                    viewModel.cancelBatchAudioDownload()
                                } else {
                                    viewModel.startBatchAudioDownload(profile.id)
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(24.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = if (isCurrentDownloading) Icons.Default.Close else Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = if (isCurrentDownloading) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isCurrentDownloading) "取消" else "下载离线包",
                                fontSize = 10.sp,
                                color = if (isCurrentDownloading) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        IconButton(
                            onClick = { viewModel.clearVoiceCache(profile) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "清理缓存",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuiltInDictionarySettingsCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedBuiltInDict by viewModel.selectedBuiltInDictionary.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("builtin_dict_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "内置词典方案",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "背单词与详情页提取英文释义与例句（红星/剑桥仅保留3例句，其余项目继承自默认词典）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                com.example.data.dict.BuiltInDictionary.values().forEach { dictOption ->
                    val isSelected = (selectedBuiltInDict == dictOption)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setBuiltInDictionary(dictOption) }
                            .testTag("dict_option_${dictOption.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dictOption.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = dictOption.tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dictOption.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

