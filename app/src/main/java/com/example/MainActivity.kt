package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.reminder.ReviewReminderManager
import com.example.ui.components.ImportReportDialog
import com.example.ui.components.WordBookImportDialog
import com.example.ui.navigation.AppScreen
import com.example.ui.screens.AllPlanScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.PortablePlayerScreen
import com.example.ui.screens.ReviewScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WordListScreen
import com.example.ui.screens.WordbooksScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel
        ReviewReminderManager.createNotificationChannel(this)

        // Check if launched from review notification
        handleReviewNotificationIntent(intent)

        // Check if launched from share menu or highlighted text selection
        handleIncomingTextIntent(intent)

        setContent {
            val themeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
            val dayTheme by viewModel.appDayTheme.collectAsStateWithLifecycle()
            val customConfig by viewModel.customColorConfig.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeMode = themeMode,
                dayTheme = dayTheme,
                customColorConfig = customConfig
            ) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleReviewNotificationIntent(intent)
        handleIncomingTextIntent(intent)
    }

    private fun handleReviewNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ReviewReminderManager.EXTRA_OPEN_REVIEW, false) == true) {
            viewModel.startReview(30)
        }
    }

    private fun handleIncomingTextIntent(intent: Intent?) {
        if (intent == null) return
        
        var sharedText: String? = null
        
        // 1. Handle SHARE intent (ACTION_SEND)
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        
        // 2. Handle PROCESS_TEXT intent (ACTION_PROCESS_TEXT)
        if (intent.action == Intent.ACTION_PROCESS_TEXT && intent.type == "text/plain") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sharedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
        }
        
        // If we extracted text successfully, clean and search it!
        if (!sharedText.isNullOrBlank()) {
            val query = sharedText.trim()
            if (query.isNotEmpty() && query.length < 100) { // Keep search queries within a reasonable length
                viewModel.updateSearchQuery(query)
                viewModel.navigateTo(com.example.ui.navigation.AppScreen.SEARCH)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val dueCount by viewModel.dueReviewCount.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val lastImportReport by viewModel.lastImportReport.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showImportDialog by remember { mutableStateOf(false) }

    // 处理全局返回键逻辑
    androidx.activity.compose.BackHandler(enabled = showImportDialog) {
        showImportDialog = false
    }

    androidx.activity.compose.BackHandler(enabled = !showImportDialog && lastImportReport != null) {
        viewModel.clearImportReport()
    }

    androidx.activity.compose.BackHandler(enabled = !showImportDialog && lastImportReport == null && currentScreen != AppScreen.DASHBOARD) {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    // 运行时存储与通知权限请求器 (Runtime Storage & Notification Permission Requester)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions result handled
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val job = launch {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Indefinite
                )
            }
            delay(1200L) // 缩短所有屏幕底部弹出提示的显示时间至1.2秒，轻巧灵动不遮挡
            job.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
            viewModel.clearUserMessage()
        }
    }

    if (lastImportReport != null) {
        ImportReportDialog(
            report = lastImportReport!!,
            onDismiss = { viewModel.clearImportReport() }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Hide bottom bar during active learning, review sessions, and full-screen plan view
            if (currentScreen != AppScreen.LEARN && currentScreen != AppScreen.REVIEW && currentScreen != AppScreen.ALL_PLAN) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    // Home (首页)
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.DASHBOARD,
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                        label = { Text("首页") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_dashboard")
                    )

                    // Word List (单词表)
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.WORD_LIST,
                        onClick = { viewModel.navigateTo(AppScreen.WORD_LIST) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "单词表") },
                        label = { Text("单词表") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_wordlist")
                    )

                    // Portable Player (随身听 - 居中放置)
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.PORTABLE_PLAYER,
                        onClick = { viewModel.navigateTo(AppScreen.PORTABLE_PLAYER) },
                        icon = { Icon(Icons.Default.Headphones, contentDescription = "随身听") },
                        label = { Text("随身听") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_portable_player")
                    )

                    // Wordbooks (单词书)
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.WORDBOOKS,
                        onClick = { viewModel.navigateTo(AppScreen.WORDBOOKS) },
                        icon = { Icon(Icons.Default.Book, contentDescription = "单词书") },
                        label = { Text("单词书") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_wordbooks")
                    )

                    // Settings (设置)
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.SETTINGS,
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                        label = { Text("设置") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> HomeScreen(
                    viewModel = viewModel,
                    onOpenImportDialog = { showImportDialog = true }
                )
                AppScreen.LEARN -> LearnScreen(
                    viewModel = viewModel
                )
                AppScreen.REVIEW -> ReviewScreen(
                    viewModel = viewModel
                )
                AppScreen.WORD_LIST -> WordListScreen(
                    viewModel = viewModel
                )
                AppScreen.ALL_PLAN -> AllPlanScreen(
                    viewModel = viewModel
                )
                AppScreen.WORDBOOKS -> WordbooksScreen(
                    viewModel = viewModel,
                    onOpenImportDialog = { showImportDialog = true }
                )
                AppScreen.PORTABLE_PLAYER -> PortablePlayerScreen(
                    viewModel = viewModel
                )
                AppScreen.SEARCH -> SearchScreen(
                    viewModel = viewModel
                )
                AppScreen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    if (showImportDialog) {
        WordBookImportDialog(
            onDismiss = { showImportDialog = false },
            onImportConfirm = { title, description, content ->
                viewModel.importCustomBook(
                    title = title,
                    description = description,
                    rawContent = content
                ) { success, _ ->
                    if (success) {
                        showImportDialog = false
                    }
                }
            }
        )
    }
}
