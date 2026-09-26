package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MemoryCurveColor

@Composable
fun MemoryCurveCard(
    dueCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "艾宾浩斯记忆曲线",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "科学抗遗忘间隔复习机制",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                if (dueCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$dueCount 词待巩固",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Drawing: Natural Decay Curve vs Spaced Review Curve
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val errorColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                val w = size.width
                val h = size.height
                val paddingX = 16f

                // Draw horizontal grid lines
                for (i in 1..3) {
                    val y = h * (i / 4f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.12f),
                        start = Offset(paddingX, y),
                        end = Offset(w - paddingX, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 1. Natural forgetting curve (steep drop)
                val decayPath = Path().apply {
                    moveTo(paddingX, h * 0.15f)
                    cubicTo(
                        paddingX + w * 0.15f, h * 0.75f,
                        paddingX + w * 0.5f, h * 0.88f,
                        w - paddingX, h * 0.92f
                    )
                }
                drawPath(
                    path = decayPath,
                    color = errorColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // 2. Spaced review curve (bounces back repeatedly forming a high retention envelope)
                val reviewPath = Path().apply {
                    moveTo(paddingX, h * 0.15f)
                    // Drop to 1st review point (5min)
                    lineTo(paddingX + w * 0.12f, h * 0.50f)
                    // Review 1 bounce
                    lineTo(paddingX + w * 0.15f, h * 0.15f)
                    // Drop to 2nd review (30min)
                    lineTo(paddingX + w * 0.32f, h * 0.40f)
                    // Review 2 bounce
                    lineTo(paddingX + w * 0.35f, h * 0.15f)
                    // Drop to 3rd review (12h/1d)
                    lineTo(paddingX + w * 0.58f, h * 0.30f)
                    // Review 3 bounce
                    lineTo(paddingX + w * 0.60f, h * 0.15f)
                    // Gradual stabilized retention
                    cubicTo(
                        paddingX + w * 0.75f, h * 0.20f,
                        paddingX + w * 0.85f, h * 0.22f,
                        w - paddingX, h * 0.22f
                    )
                }

                drawPath(
                    path = reviewPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(primaryColor, secondaryColor, MemoryCurveColor)
                    ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Key review nodes (dots)
                val nodeX = floatArrayOf(
                    paddingX,
                    paddingX + w * 0.15f,
                    paddingX + w * 0.35f,
                    paddingX + w * 0.60f,
                    w - paddingX
                )
                val nodeY = floatArrayOf(
                    h * 0.15f,
                    h * 0.15f,
                    h * 0.15f,
                    h * 0.15f,
                    h * 0.22f
                )

                for (idx in nodeX.indices) {
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(nodeX[idx], nodeY[idx])
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(nodeX[idx], nodeY[idx])
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 3.dp)
                            .background(errorColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "自然遗忘(遗忘超70%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 3.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "科学复习(巩固长效记忆)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
