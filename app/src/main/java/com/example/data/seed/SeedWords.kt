package com.example.data.seed

import com.example.data.model.WordBookEntity

object SeedWords {
    val BUILT_IN_BOOKS = listOf(
        WordBookEntity(
            id = "coca_1_3500",
            title = "COCA 1-3500",
            description = "COCA当代美语语料库高频核心基础词汇（序号 1 ~ 3500）",
            isCustom = false,
            totalWords = 3500
        ),
        WordBookEntity(
            id = "coca_3500_7000",
            title = "COCA 3500-7000",
            description = "COCA进阶中高频词汇与学术表达（序号 3501 ~ 7000）",
            isCustom = false,
            totalWords = 3500
        ),
        WordBookEntity(
            id = "coca_7000_10000",
            title = "COCA 7000-10000",
            description = "COCA高阶扩展词汇与地道考点难词（序号 7001 ~ 10000）",
            isCustom = false,
            totalWords = 3000
        ),
        WordBookEntity(
            id = "coca_10000_15000",
            title = "COCA 10000-15000",
            description = "COCA巅峰高阶词汇与学术专业表达（序号 10001 ~ 15000）",
            isCustom = false,
            totalWords = 5000
        )
    )
}
