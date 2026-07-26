package com.screenguard.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val appName = intent.getStringExtra("app_name") ?: "该应用"
        val usedMinutes = intent.getLongExtra("used_minutes", 0)
        val limitMinutes = intent.getLongExtra("limit_minutes", 60)

        setContent {
            ReminderScreen(
                appName = appName,
                usedMinutes = usedMinutes,
                limitMinutes = limitMinutes,
                onContinueUse = { finish() },
                onViewReport = {
                    val mainIntent = Intent(this, MainActivity::class.java)
                    startActivity(mainIntent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun ReminderScreen(
    appName: String,
    usedMinutes: Long,
    limitMinutes: Long,
    onContinueUse: () -> Unit,
    onViewReport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⏰",
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "该休息了",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                Text(
                    text = "「${appName}」今日已使用",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Text(
                    text = "${usedMinutes} 分钟",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )

                Text(
                    text = "每日限额：${limitMinutes} 分钟",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onContinueUse,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("继续使用", fontSize = 16.sp, modifier = Modifier.padding(4.dp))
                }

                OutlinedButton(
                    onClick = onViewReport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("查看使用报告", fontSize = 16.sp)
                }
            }
        }
    }
}
