package com.example.samsung_alarm.ui.main;

import android.app.Activity;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.service.AlarmScheduler;
import com.example.samsung_alarm.ui.common.SimpleSeekBarListener;
import java.util.Locale;

final class TimerController {
    interface PermissionGate { boolean canSchedule(); }
    private final Activity activity;
    private final PermissionGate permissionGate;
    private final TextView display;
    private final SeekBar seek;
    private final Button start;
    private CountDownTimer timer;
    private int minutes = 5;
    private long remaining;

    TimerController(Activity activity, PermissionGate permissionGate) {
        this.activity = activity; this.permissionGate = permissionGate;
        display = activity.findViewById(R.id.timerDisplay); seek = activity.findViewById(R.id.timerSeek);
        start = activity.findViewById(R.id.timerStart);
        seek.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                minutes = Math.max(1, progress);
                if (timer == null && remaining == 0) display.setText(String.format(Locale.getDefault(), "%02d:00", minutes));
            }
        });
        start.setOnClickListener(v -> toggle());
        activity.findViewById(R.id.timerReset).setOnClickListener(v -> reset());
    }

    private void toggle() {
        if (timer != null) {
            timer.cancel(); timer = null; AlarmScheduler.cancelTimer(activity);
            start.setText(R.string.resume); return;
        }
        if (!permissionGate.canSchedule()) return;
        long millis = remaining > 0 ? remaining : minutes * 60_000L;
        remaining = millis; AlarmScheduler.scheduleTimer(activity, millis);
        timer = new CountDownTimer(millis, 1_000L) {
            @Override public void onTick(long value) { remaining = value; display.setText(format(value)); }
            @Override public void onFinish() { timer = null; remaining = 0; start.setText(R.string.start); }
        }.start();
        start.setText(R.string.pause);
    }

    private void reset() {
        if (timer != null) timer.cancel();
        timer = null; remaining = 0; AlarmScheduler.cancelTimer(activity);
        minutes = Math.max(1, seek.getProgress()); display.setText(String.format(Locale.getDefault(), "%02d:00", minutes));
        start.setText(R.string.start);
    }
    private String format(long millis) { long seconds=(millis+999)/1000; return String.format(Locale.getDefault(), "%02d:%02d", seconds/60, seconds%60); }
}
