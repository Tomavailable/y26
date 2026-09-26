package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode(val title: String) {
    SYSTEM("跟随系统"),
    LIGHT("日间模式"),
    DARK("夜间模式");

    val label: String get() = title
}

// 主题配色方案枚举
enum class AppDayTheme(
    val title: String,
    val subtitle: String
) {
    DEFAULT(
        title = "默认经典",
        subtitle = "日间清爽青绿，夜间沉稳暗夜默认配色"
    ),
    CRIMSON_OBSIDIAN(
        title = "经典黑红",
        subtitle = "大厂高对比黑曜绯红美学，深邃黑字/纯白底色/鲜艳绯红"
    ),
    CUSTOM(
        title = "全自由自定义",
        subtitle = "自由调整主背景、卡片、主色、选中色、文字以及透明度"
    )
}

// 自定义各部位颜色配置 (支持任意 RGB / Hex 颜色及透明度)
data class CustomColorConfig(
    val isCustomEnabled: Boolean = false,
    val customBackground: Color? = null,
    val customSurface: Color? = null,
    val customSurfaceVariant: Color? = null,
    val customPrimary: Color? = null,
    val customPrimaryContainer: Color? = null,
    val customOnSurface: Color? = null,
    val customOnSurfaceVariant: Color? = null
)

fun getCustomLightColorScheme(
    theme: AppDayTheme = AppDayTheme.DEFAULT,
    customConfig: CustomColorConfig = CustomColorConfig()
): ColorScheme {
    val baseScheme = when (theme) {
        AppDayTheme.CRIMSON_OBSIDIAN -> lightColorScheme(
            primary = CrimsonPrimary,
            onPrimary = CrimsonOnPrimary,
            primaryContainer = CrimsonPrimaryContainer,
            onPrimaryContainer = CrimsonOnPrimaryContainer,
            secondary = ObsidianSecondary,
            onSecondary = ObsidianOnSecondary,
            secondaryContainer = ObsidianSecondaryContainer,
            onSecondaryContainer = ObsidianOnSecondaryContainer,
            tertiary = CrimsonPrimary,
            onTertiary = CrimsonOnPrimary,
            tertiaryContainer = CrimsonPrimaryContainer,
            onTertiaryContainer = CrimsonOnPrimaryContainer,
            background = LightCanvasBackground,
            onBackground = LightCanvasOnBackground,
            surface = LightCanvasSurface,
            onSurface = LightCanvasOnSurface,
            surfaceVariant = LightCanvasSurfaceVariant,
            onSurfaceVariant = LightCanvasOnSurfaceVariant,
            outline = LightCanvasOutline,
            outlineVariant = LightCanvasOutlineVariant
        )
        else -> lightColorScheme(
            primary = OriginalPrimaryLight,
            onPrimary = OriginalOnPrimaryLight,
            primaryContainer = OriginalPrimaryContainerLight,
            onPrimaryContainer = OriginalOnPrimaryContainerLight,
            secondary = OriginalSecondaryLight,
            onSecondary = OriginalOnSecondaryLight,
            secondaryContainer = OriginalSecondaryContainerLight,
            onSecondaryContainer = OriginalOnSecondaryContainerLight,
            tertiary = OriginalPrimaryLight,
            onTertiary = OriginalOnPrimaryLight,
            tertiaryContainer = OriginalPrimaryContainerLight,
            onTertiaryContainer = OriginalOnPrimaryContainerLight,
            background = LightCanvasBackground,
            onBackground = LightCanvasOnBackground,
            surface = LightCanvasSurface,
            onSurface = LightCanvasOnSurface,
            surfaceVariant = LightCanvasSurfaceVariant,
            onSurfaceVariant = LightCanvasOnSurfaceVariant,
            outline = LightCanvasOutline,
            outlineVariant = LightCanvasOutlineVariant
        )
    }

    if (theme == AppDayTheme.CUSTOM || customConfig.isCustomEnabled) {
        val bg = customConfig.customBackground ?: baseScheme.background
        val surf = customConfig.customSurface ?: baseScheme.surface
        val surfVar = customConfig.customSurfaceVariant ?: baseScheme.surfaceVariant
        val primary = customConfig.customPrimary ?: baseScheme.primary
        val primaryContainer = customConfig.customPrimaryContainer ?: primary.copy(alpha = 0.15f)
        val onSurf = customConfig.customOnSurface ?: baseScheme.onSurface
        val onSurfVar = customConfig.customOnSurfaceVariant ?: baseScheme.onSurfaceVariant

        return baseScheme.copy(
            primary = primary,
            onPrimary = if (primary.red * 0.299 + primary.green * 0.587 + primary.blue * 0.114 > 0.6) Color.Black else Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = primary,
            background = bg,
            onBackground = onSurf,
            surface = surf,
            onSurface = onSurf,
            surfaceVariant = surfVar,
            onSurfaceVariant = onSurfVar
        )
    }

    return baseScheme
}

fun getCustomDarkColorScheme(
    theme: AppDayTheme = AppDayTheme.DEFAULT,
    customConfig: CustomColorConfig = CustomColorConfig()
): ColorScheme {
    // 恢复原版夜间模式默认配色 (经典纯正暗夜与青碧)
    val baseDark = when (theme) {
        AppDayTheme.CRIMSON_OBSIDIAN -> darkColorScheme(
            primary = CrimsonPrimaryDark,
            onPrimary = CrimsonOnPrimaryDark,
            primaryContainer = CrimsonPrimaryContainerDark,
            onPrimaryContainer = CrimsonOnPrimaryContainerDark,
            secondary = ObsidianSecondary,
            onSecondary = ObsidianOnSecondary,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            outline = DarkOutline,
            outlineVariant = DarkOutlineVariant
        )
        else -> darkColorScheme(
            primary = OriginalPrimaryDark,
            onPrimary = OriginalOnPrimaryDark,
            primaryContainer = OriginalPrimaryContainerDark,
            onPrimaryContainer = OriginalOnPrimaryContainerDark,
            secondary = OriginalSecondaryDark,
            onSecondary = OriginalOnSecondaryDark,
            background = OriginalDarkBackground,
            onBackground = OriginalDarkOnBackground,
            surface = OriginalDarkSurface,
            onSurface = OriginalDarkOnSurface,
            surfaceVariant = OriginalDarkSurfaceVariant,
            onSurfaceVariant = OriginalDarkOnSurfaceVariant,
            outline = OriginalDarkOutline,
            outlineVariant = OriginalDarkOutlineVariant
        )
    }

    if (customConfig.isCustomEnabled) {
        val bg = customConfig.customBackground ?: baseDark.background
        val surf = customConfig.customSurface ?: baseDark.surface
        val surfVar = customConfig.customSurfaceVariant ?: baseDark.surfaceVariant
        val primary = customConfig.customPrimary ?: baseDark.primary
        val primaryContainer = customConfig.customPrimaryContainer ?: baseDark.primaryContainer
        val onSurf = customConfig.customOnSurface ?: baseDark.onSurface
        val onSurfVar = customConfig.customOnSurfaceVariant ?: baseDark.onSurfaceVariant

        return baseDark.copy(
            primary = primary,
            onPrimary = if (primary.red * 0.299 + primary.green * 0.587 + primary.blue * 0.114 > 0.6) Color.Black else Color.White,
            primaryContainer = primaryContainer,
            background = bg,
            onBackground = onSurf,
            surface = surf,
            onSurface = onSurf,
            surfaceVariant = surfVar,
            onSurfaceVariant = onSurfVar
        )
    }

    return baseDark
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dayTheme: AppDayTheme = AppDayTheme.DEFAULT,
    customColorConfig: CustomColorConfig = CustomColorConfig(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> getCustomDarkColorScheme(dayTheme, customColorConfig)
        else -> getCustomLightColorScheme(dayTheme, customColorConfig)
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
