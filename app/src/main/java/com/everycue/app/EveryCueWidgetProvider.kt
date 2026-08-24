package com.everycue.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.everycue.feature.renew.DueState
import com.everycue.feature.track.ExpiryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EveryCueWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            if (!DeviceSecurityGuard.isAccessAllowed()) {
                clearSensitiveCounts(context)
                return
            }
            val app = context.applicationContext as EveryCueApplication
            val trackItems = app.trackRepository.items.first()
            val renewals = app.renewRepository.renewals.first()
            val pack = app.packRepository.snapshot()
            val urgentTrack = trackItems.count { it.expiryState() != ExpiryState.FRESH }
            val urgentRenew = renewals.count { it.dueState() != DueState.UPCOMING }
            val unpacked = pack.trips.sumOf { trip -> trip.items.count { !it.isPacked } }

            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, EveryCueWidgetProvider::class.java)
            val launchIntent = Intent(context, MainActivity::class.java)
            val clickIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.getAppWidgetIds(component).forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_everycue).apply {
                    setTextViewText(R.id.widget_track_count, urgentTrack.toString())
                    setTextViewText(R.id.widget_pack_count, unpacked.toString())
                    setTextViewText(R.id.widget_renew_count, urgentRenew.toString())
                    setOnClickPendingIntent(R.id.widget_root, clickIntent)
                }
                manager.updateAppWidget(widgetId, views)
            }
        }

        private fun clearSensitiveCounts(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, EveryCueWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { widgetId ->
                val blocked = context.getString(R.string.security_blocked_count)
                val views = RemoteViews(context.packageName, R.layout.widget_everycue).apply {
                    setTextViewText(R.id.widget_track_count, blocked)
                    setTextViewText(R.id.widget_pack_count, blocked)
                    setTextViewText(R.id.widget_renew_count, blocked)
                    setOnClickPendingIntent(R.id.widget_root, null)
                }
                manager.updateAppWidget(widgetId, views)
            }
        }
    }
}
