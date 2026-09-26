package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReviewReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!ReviewReminderManager.isReminderEnabled(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val now = System.currentTimeMillis()
                val dueCount = db.wordDao().getDueReviewCountSync(now)

                val title: String
                val content: String
                if (dueCount > 0) {
                    title = "艾宾浩斯复习提醒 📚"
                    content = "今日有 $dueCount 个单词到达复习周期！及时复习能抵抗 80% 的记忆衰减，点击立即巩固。"
                } else {
                    title = "背单词打卡提醒 🌿"
                    content = "保持每日背词的好习惯！今天有新的单词等待探索。"
                }

                ReviewReminderManager.showNotification(context, title, content)

                // Reschedule for next day
                val (hour, minute) = ReviewReminderManager.getReminderTime(context)
                ReviewReminderManager.scheduleDailyReminder(context, hour, minute)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
