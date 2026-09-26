package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.MainViewModel

/**
 * 艾宾浩斯复习界面：
 * 与学习模式共用高品质交互界面（包含7大学习方式：盲听选义、看词选义、看义说词、听音选词、听句选词、英文释义选词、盲听拼写），
 * 但独立使用复习模式的设置数据：
 * - 默认只开启 3 种题型（盲听选义、看词选义、看义说词）
 * - 默认开启【答对自动进入下一个】极速刷词，仅打错后显示详情页
 */
@Composable
fun ReviewScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    LearnScreen(
        viewModel = viewModel,
        modifier = modifier
    )
}
