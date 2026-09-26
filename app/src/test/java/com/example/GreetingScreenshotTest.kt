package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.WordEntity
import com.example.ui.components.MemoryCurveCard
import com.example.ui.components.WordCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun word_card_screenshot() {
        val sampleWord = WordEntity(
            bookId = "cet4",
            word = "serendipity",
            phonetic = "/ˌserənˈdɪpəti/",
            meaning = "机缘巧合；意外发现珍宝的运气",
            pos = "n.",
            exampleSentence = "Finding this vintage book was pure serendipity.",
            exampleTranslation = "偶然淘到这本绝版旧书纯粹是机缘巧合。"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                WordCard(
                    word = sampleWord,
                    isMeaningRevealed = true,
                    onPronounce = {},
                    onRevealMeaning = {},
                    onToggleFavorite = {},
                    onMarkMastered = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/word_card.png")
    }
}
