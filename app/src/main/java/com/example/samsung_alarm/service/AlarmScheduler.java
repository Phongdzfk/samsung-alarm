package com.example.samsung_alarm.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.example.samsung_alarm.data.model.Alarm;
import com.example.samsung_alarm.R;
import java.util.Calendar;

public final class AlarmScheduler {
    public static final String EXTRA_ALARM_ID = "alarm_id";
    public static final String EXTRA_PREVIEW = "preview";
    private static final int UPCOMING_OFFSET = 30 * 60 * 1000;

    private AlarmScheduler() {}

    public static long nextTrigger(Alarm alarm) {
        return calculateNext(alarm, true);
    }

    public static long nextTriggerIgnoringSkip(Alarm alarm) {
        return calculateNext(alarm, false);
    }

    private static long calculateNext(Alarm alarm, boolean respectSkip) {
        Calendar now = Calendar.getInstance();
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, alarm.hour);
        next.set(Calendar.MINUTE, alarm.minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!alarm.repeats()) {
            if (next.getTimeInMillis() <= now.getTimeInMillis()) next.add(Calendar.DAY_OF_YEAR, 1);
            if (respectSkip && next.getTimeInMillis() <= alarm.skipUntilMillis + 60_000L) next.add(Calendar.DAY_OF_YEAR, 1);
            return next.getTimeInMillis();
        }
        for (int add = 0; add < 15; add++) {
            Calendar candidate = (Calendar) next.clone();
            candidate.add(Calendar.DAY_OF_YEAR, add);
            boolean skipped = respectSkip && candidate.getTimeInMillis() <= alarm.skipUntilMillis + 60_000L;
            if (candidate.getTimeInMillis() > now.getTimeInMillis() && !skipped && enabled(alarm, candidate.get(Calendar.DAY_OF_WEEK))) {
                return candidate.getTimeInMillis();
            }
        }
        next.add(Calendar.DAY_OF_YEAR, 7);
        return next.getTimeInMillis();
    }

    private static boolean enabled(Alarm a, int day) {
        switch (day) {
            case Calendar.MONDAY: return a.mon;
            case Calendar.TUESDAY: return a.tue;
            case Calendar.WEDNESDAY: return a.wed;
            case Calendar.THURSDAY: return a.thu;
            case Calendar.FRIDAY: return a.fri;
            case Calendar.SATURDAY: return a.sat;
            case Calendar.SUNDAY: return a.sun;
            default: return false;
        }
    }

    public static boolean canScheduleExact(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms();
    }

    public static Intent exactAlarmPermissionIntent(Context context) {
        return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                android.net.Uri.parse("package:" + context.getPackageName()));
    }

    public static void schedule(Context context, Alarm alarm) {
        if (!alarm.isActive || !canScheduleExact(context)) return;
        long when = nextTrigger(alarm);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent ring = PendingIntent.getBroadcast(context, alarm.id,
                new Intent(context, AlarmReceiver.class).putExtra(EXTRA_ALARM_ID, alarm.id),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(when, ring), ring);

        if (when - System.currentTimeMillis() > UPCOMING_OFFSET) {
            PendingIntent upcoming = PendingIntent.getBroadcast(context, 100000 + alarm.id,
                    new Intent(context, UpcomingReceiver.class).putExtra(EXTRA_ALARM_ID, alarm.id),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when - UPCOMING_OFFSET, upcoming);
        }
    }

    public static void scheduleSnooze(Context context, int alarmId, int minutes) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent ring = PendingIntent.getBroadcast(context, alarmId,
                new Intent(context, AlarmReceiver.class).putExtra(EXTRA_ALARM_ID, alarmId),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(
                System.currentTimeMillis() + minutes * 60_000L, ring), ring);
    }

    public static void scheduleQuick(Context context, int minutes) {
        scheduleTemporary(context, 900001, minutes * 60_000L, context.getString(R.string.quick_alarm));
    }

    public static void scheduleTimer(Context context, long delayMillis) {
        scheduleTemporary(context, 900002, delayMillis, context.getString(R.string.timer_finished));
    }

    private static void scheduleTemporary(Context context, int request, long delayMillis, String label) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent ring = PendingIntent.getBroadcast(context, request,
                new Intent(context, AlarmReceiver.class).putExtra(EXTRA_ALARM_ID, -1)
                        .putExtra(EXTRA_PREVIEW, label),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(
                System.currentTimeMillis() + Math.max(1_000L, delayMillis), ring), ring);
    }

    public static void cancelQuick(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent ring = PendingIntent.getBroadcast(context, 900001,
                new Intent(context, AlarmReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (ring != null) manager.cancel(ring);
    }

    public static void cancelTimer(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent ring = PendingIntent.getBroadcast(context, 900002,
                new Intent(context, AlarmReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (ring != null) manager.cancel(ring);
    }

    public static void cancel(Context context, int alarmId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent ring = PendingIntent.getBroadcast(context, alarmId,
                new Intent(context, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (ring != null) manager.cancel(ring);
        PendingIntent upcoming = PendingIntent.getBroadcast(context, 100000 + alarmId,
                new Intent(context, UpcomingReceiver.class), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (upcoming != null) manager.cancel(upcoming);
    }
}
