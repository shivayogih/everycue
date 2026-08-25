package com.everycue.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.everycue.feature.renew.DueState
import com.everycue.feature.track.ExpiryState
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

const val EXTRA_DESTINATION = "com.everycue.app.extra.DESTINATION"
const val EXTRA_ITEM_ID = "com.everycue.app.extra.ITEM_ID"
const val DESTINATION_TRACK = "track"
const val DESTINATION_RENEW = "renew"

internal fun delayUntilTimeMillis(now: ZonedDateTime, hour: Int, minute: Int): Long {
    var next = now
        .withHour(hour.coerceIn(0, 23))
        .withMinute(minute.coerceIn(0, 59))
        .withSecond(0)
        .withNano(0)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return Duration.between(now, next).toMillis()
}

object ReminderScheduler {
    private const val UNIQUE_WORK = "everycue_daily_reminders"

    fun update(context: Context, settings: AppSettings) {
        val manager = WorkManager.getInstance(context)
        if (!settings.remindersEnabled) {
            manager.cancelUniqueWork(UNIQUE_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(
                delayUntilTimeMillis(ZonedDateTime.now(), settings.reminderHour, settings.reminderMinute),
                TimeUnit.MILLISECONDS,
            )
            .build()
        manager.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

class ReminderWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        if (!DeviceSecurityGuard.isAccessAllowed()) return Result.success()
        val app = applicationContext as EveryCueApplication
        val settings = app.settingsRepository.snapshot()
        if (!settings.remindersEnabled || !notificationsAllowed(applicationContext)) return Result.success()

        createReminderChannel(applicationContext)
        val urgentTrack = app.trackRepository.items.first().filter { it.expiryState() != ExpiryState.FRESH }
        val urgentRenewals = app.renewRepository.renewals.first().filter { it.dueState() != DueState.UPCOMING }
        urgentTrack.forEach { item ->
            notify(
                id = item.id.hashCode(),
                title = item.name,
                body = when (val days = item.daysRemaining()) {
                    in Long.MIN_VALUE until -1 -> applicationContext.getString(R.string.notification_expired_days, -days)
                    -1L -> applicationContext.getString(R.string.notification_expired_one_day)
                    0L -> applicationContext.getString(R.string.notification_expires_today)
                    1L -> applicationContext.getString(R.string.notification_expires_one_day)
                    else -> applicationContext.getString(R.string.notification_expires_days, days)
                },
                destination = DESTINATION_TRACK,
                itemId = item.id,
            )
        }
        urgentRenewals.forEach { renewal ->
            notify(
                id = renewal.id.hashCode(),
                title = renewal.title,
                body = when (val days = renewal.daysRemaining()) {
                    in Long.MIN_VALUE until -1 -> applicationContext.getString(R.string.notification_overdue_days, -days)
                    -1L -> applicationContext.getString(R.string.notification_overdue_one_day)
                    0L -> applicationContext.getString(R.string.notification_due_today)
                    1L -> applicationContext.getString(R.string.notification_due_one_day)
                    else -> applicationContext.getString(R.string.notification_due_days, days)
                },
                destination = DESTINATION_RENEW,
                itemId = renewal.id,
            )
        }
        EveryCueWidgetProvider.updateAll(applicationContext)
        return Result.success()
    }

    private fun notify(id: Int, title: String, body: String, destination: String, itemId: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, destination)
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between the explicit check and the framework call.
        }
    }
}

private const val REMINDER_CHANNEL = "everycue_reminders"

fun createReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            REMINDER_CHANNEL,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
