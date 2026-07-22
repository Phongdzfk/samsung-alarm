package com.example.samsung_alarm.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.samsung_alarm.R;
import com.example.samsung_alarm.ui.ring.RingActivity;
import com.example.samsung_alarm.data.AppExecutors;
import com.example.samsung_alarm.data.repository.AlarmRepository;
import com.example.samsung_alarm.data.model.Alarm;

public class AlarmRingingService extends Service {
    private MediaPlayer player;
    private Vibrator vibrator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoAction;
    private Runnable volumeFade;

    @Override public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        int id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        String preview = intent.getStringExtra(AlarmScheduler.EXTRA_PREVIEW);
        if (id < 0) startRinging(id, preview == null ? getString(R.string.quick_alarm) : preview, null);
        else AppExecutors.DB.execute(() -> {
            Alarm alarm = AlarmRepository.get(this).getByIdSync(id);
            if (alarm != null) handler.post(() -> startRinging(id, alarm.label, alarm));
            else stopSelf();
        });
        return START_NOT_STICKY;
    }

    private void startRinging(int id, String label, Alarm alarm) {
        Intent full = new Intent(this, RingActivity.class).putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
                .putExtra(AlarmScheduler.EXTRA_MATH_DISMISS, alarm != null && alarm.isMathDismiss)
                .putExtra(AlarmScheduler.EXTRA_MATH_DIFFICULTY, alarm == null ? 1 : alarm.mathDifficulty)
                .putExtra(AlarmScheduler.EXTRA_LABEL, label)
                .putExtra(AlarmScheduler.EXTRA_HOUR, alarm == null ? -1 : alarm.hour)
                .putExtra(AlarmScheduler.EXTRA_MINUTE, alarm == null ? -1 : alarm.minute)
                .putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, alarm == null ? 5 : alarm.snoozeMinutes)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreen = PendingIntent.getActivity(this, id, full,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent dismissIntent = new Intent(this, AlarmReceiver.class).setAction(AlarmReceiver.ACTION_DISMISS)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id);
        Intent snoozeIntent = new Intent(this, AlarmReceiver.class).setAction(AlarmReceiver.ACTION_SNOOZE)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id).putExtra("minutes", alarm == null ? 5 : alarm.snoozeMinutes);
        PendingIntent dismiss = PendingIntent.getBroadcast(this, 300000 + Math.max(id, 0), dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent snooze = PendingIntent.getBroadcast(this, 400000 + Math.max(id, 0), snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationHelper.ALARM_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(getString(R.string.ringing_title))
                .setContentText(label).setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX).setOngoing(true)
                .setContentIntent(fullScreen);
        builder.setFullScreenIntent(fullScreen,true);
        // Chế độ giải toán buộc người dùng mở màn hình báo thức, không cho tắt hoặc snooze từ notification.
        if (alarm == null || !alarm.isMathDismiss) {
            builder.addAction(android.R.drawable.ic_media_pause, getString(R.string.snooze), snooze);
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.dismiss), dismiss);
        }
        startForeground(42, builder.build());
        play(alarm);

        if (alarm != null && alarm.autoAction != 0) {
            autoAction = () -> {
                if (alarm.autoAction == 1) AlarmScheduler.scheduleSnooze(this, id, alarm.snoozeMinutes);
                else AppExecutors.DB.execute(() -> {
                    if (!alarm.repeats() && !alarm.keepAfterDismiss) AlarmRepository.get(this).finishOneTimeSync(id);
                });
                stopSelf();
            };
            handler.postDelayed(autoAction, Math.max(1, alarm.autoAfterMinutes) * 60_000L);
        }
    }

    private void play(Alarm alarm) {
        stopMedia();
        try {
            Uri uri = alarm != null && alarm.ringtoneUri != null ? Uri.parse(alarm.ringtoneUri)
                    : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            player = new MediaPlayer();
            player.setDataSource(this, uri);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setLooping(true);
            float volume = alarm == null ? 0.8f : Math.max(0, Math.min(100, alarm.volume)) / 100f;
            float initial = alarm != null && alarm.gradualVolume ? Math.min(volume, 0.1f) : volume;
            player.setVolume(initial, initial);
            player.prepare();
            player.start();
            if (alarm != null && alarm.gradualVolume && volume > initial) {
                final float target=volume; final float[] current={initial}; final float step=(target-initial)/20f;
                volumeFade=new Runnable(){@Override public void run(){if(player==null)return;current[0]=Math.min(target,current[0]+step);player.setVolume(current[0],current[0]);if(current[0]<target)handler.postDelayed(this,3_000L);}};
                handler.postDelayed(volumeFade,3_000L);
            }
        } catch (Exception ignored) { }
        if (alarm == null || alarm.vibrate) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 500}, 0));
            else vibrator.vibrate(new long[]{0, 500, 500}, 0);
        }
    }

    private void stopMedia() {
        if (player != null) { player.stop(); player.release(); player = null; }
        if (volumeFade != null) { handler.removeCallbacks(volumeFade); volumeFade=null; }
        if (vibrator != null) vibrator.cancel();
    }

    @Override public void onDestroy() {
        if (autoAction != null) handler.removeCallbacks(autoAction);
        stopMedia();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
