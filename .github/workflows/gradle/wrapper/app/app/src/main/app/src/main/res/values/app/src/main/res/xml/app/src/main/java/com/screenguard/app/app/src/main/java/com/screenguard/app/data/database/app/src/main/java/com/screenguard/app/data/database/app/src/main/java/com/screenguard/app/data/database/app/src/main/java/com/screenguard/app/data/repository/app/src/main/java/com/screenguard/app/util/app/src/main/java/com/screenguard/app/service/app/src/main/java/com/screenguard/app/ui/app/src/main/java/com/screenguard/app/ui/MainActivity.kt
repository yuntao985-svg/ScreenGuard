package com.screenguard.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenguard.ScreenGuardApp
import com.screenguard.data.database.AppLimitEntity
import com.screenguard.service.MonitorService
import com.screenguard.util.AppUsageInfo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionsAndStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScreenGuardTheme {
                MainScreen(
                    onRequestUsageStatsPermission = { openUsageStatsSettings() }
                )
            }
        }

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val app = ScreenGuardApp.instance
        if (app.usageStatsHelper.hasUsageStatsPermission()) {
            MonitorService.start(this)
        }
    }

    private fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        permissionLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStart()
    }
}

@Composable
fun MainScreen(onRequestUsageStatsPermission: () -> Unit) {
    val context = LocalContext.current
    val app = ScreenGuardApp.instance
    val scope = rememberCoroutineScope()

    var appUsageList by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }
    var limits by remember { mutableStateOf<List<AppLimitEntity>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf<AppUsageInfo?>(null) }
    var totalScreenTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        hasPermission = app.usageStatsHelper.hasUsageStatsPermission()
    }

    LaunchedEffect(Unit) {
        app.repository.getAll().collect { limitList ->
            limits = limitList
        }
    }

    fun refreshUsageData() {
        scope.launch {
            appUsageList = app.usageStatsHelper.getTodayUsageStats()
            totalScreenTime = appUsageList.sumOf { it.usageTimeMs }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            refreshUsageData()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("屏控", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (!hasPermission) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🔒", fontSize = 48.sp)
                        Text(
                            "需要开启使用情况访问权限",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "屏控需要此权限来读取应用使用时长，\n不会收集任何个人隐私数据。",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Button(onClick = onRequestUsageStatsPermission) {
                            Text("去开启权限")
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("今日屏幕使用时长", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        formatTime(totalScreenTime),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${appUsageList.size} 个应用被记录",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { refreshUsageData() }) {
                            Text("刷新数据")
                        }
                        Button(
                            onClick = {
                                MonitorService.stop(context)
                                MonitorService.start(context)
                            }
                        ) {
                            Text("重启监控")
                        }
                    }
                }
            }

            Text(
                "应用使用详情（点击设置限额）",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 14.sp,
                color = Color.Gray
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(appUsageList) { appInfo ->
                    val limit = limits.find { it.packageName == appInfo.packageName }
                    val usageMinutes = appInfo.usageTimeMs / 60_000
                    val limitMinutes = limit?.dailyLimitMinutes ?: 0
                    val isOverLimit = limit != null && limit.isEnabled && usageMinutes >= limitMinutes
                    val isNearLimit = limit != null && limit.isEnabled &&
                            usageMinutes >= limitMinutes * limit.warnAtPercent / 100 &&
                            usageMinutes < limitMinutes

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { showLimitDialog = appInfo },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isOverLimit -> Color(0xFFFFEBEE)
                                isNearLimit -> Color(0xFFFFF8E1)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    appInfo.appName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Text(
                                    formatTime(appInfo.usageTimeMs),
                                    fontSize = 14.sp,
                                    color = if (isOverLimit) Color(0xFFE53935) else Color.Gray
                                )
                                if (limit != null && limit.isEnabled) {
                                    Text(
                                        "限额：${limitMinutes}分钟/天",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            when {
                                isOverLimit -> Text("⚠️ 已超限", color = Color(0xFFE53935), fontSize = 12.sp)
                                isNearLimit -> Text("⚡ 接近限额", color = Color(0xFFFF9800), fontSize = 12.sp)
                                limit != null && limit.isEnabled -> Text("✅ 监控中", color = Color(0xFF4CAF50), fontSize = 12.sp)
                                else -> Text("—", color = Color.Gray, fontSize = 12.sp)
                            }

                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "设置限额",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    showLimitDialog?.let { appInfo ->
        val existingLimit = limits.find { it.packageName == appInfo.packageName }
        var sliderValue by remember {
            mutableFloatStateOf((existingLimit?.dailyLimitMinutes ?: 60).toFloat())
        }
        var enabled by remember {
            mutableStateOf(existingLimit?.isEnabled ?: true)
        }

        AlertDialog(
            onDismissRequest = { showLimitDialog = null },
            title = { Text("设置限额：${appInfo.appName}") },
            text = {
                Column {
                    Text("每日使用时长上限：${sliderValue.toInt()} 分钟")
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 5f..480f,
                        steps = 95
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (enabled) "启用监控" else "暂停监控")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        app.repository.saveLimit(
                            AppLimitEntity(
                                packageName = appInfo.packageName,
                                appName = appInfo.appName,
                                dailyLimitMinutes = sliderValue.toInt(),
                                isEnabled = enabled
                            )
                        )
                        showLimitDialog = null
                    }
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

fun formatTime(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}小时${minutes}分钟" else "${minutes}分钟"
}

@Composable
fun ScreenGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1565C0),
            primaryContainer = Color(0xFFE3F2FD),
            errorContainer = Color(0xFFFFEBEE)
        ),
        content = content
    )
}
