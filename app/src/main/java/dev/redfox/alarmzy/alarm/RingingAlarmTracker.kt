package dev.redfox.alarmzy.alarm

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The alarm currently sounding. */
data class RingingAlarm(
    val id: Long,
    val label: String,
    /** "12:30 PM" */
    val time: String,
)

/**
 * Single source of truth for "is an alarm ringing right now". [AlarmService] owns it (sets it when
 * it starts ringing, clears it when it stops); the full-screen activity, the notification and the
 * in-app "Ringing now" card all observe it, so stopping the alarm from any one of them closes the
 * others.
 */
@Singleton
class RingingAlarmTracker @Inject constructor() {
    private val _ringing = MutableStateFlow<RingingAlarm?>(null)
    val ringing: StateFlow<RingingAlarm?> = _ringing.asStateFlow()

    internal fun set(alarm: RingingAlarm?) {
        _ringing.value = alarm
    }

    companion object {
        /** Routes a Snooze/Dismiss from anywhere in the app to the ringing service. */
        fun send(context: Context, action: String, alarmId: Long) {
            context.startService(
                Intent(context, AlarmService::class.java)
                    .setAction(action)
                    .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            )
        }
    }
}
