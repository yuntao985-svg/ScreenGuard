package com.screenguard.widget

import android.content.Context
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.screenguard.ScreenGuardApp

class ScreenGuardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScreenGuardWidget()
}

class ScreenGuardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = ScreenGuardApp.instance
        val totalMs = app.usageStatsHelper.getTotalScreenTime()
        val totalMinutes = totalMs / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "📱 今日屏幕时间",
                    style = TextStyle(color = ColorProvider(android.graphics.Color.GRAY))
                )
                Text(
                    text = "${hours}h ${minutes}m",
                    style = TextStyle(color = ColorProvider(android.graphics.Color.BLACK))
                )
                Text(
                    text = "点击查看详情 →",
                    style = TextStyle(color = ColorProvider(android.graphics.Color.BLUE))
                )
            }
        }
    }
}
