package com.example.samsung_alarm.ui.main;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;
import com.example.samsung_alarm.R;
import java.util.Locale;

final class StopwatchController {
    private final TextView display;
    private final Button start;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private long startedAt, accumulated;

    StopwatchController(Activity activity) {
        display = activity.findViewById(R.id.stopwatchDisplay); start = activity.findViewById(R.id.stopwatchStart);
        start.setOnClickListener(v -> toggle());
        activity.findViewById(R.id.stopwatchReset).setOnClickListener(v -> reset());
    }
    private void toggle() {
        if (running) { accumulated += SystemClock.elapsedRealtime()-startedAt; running=false; start.setText(R.string.resume); }
        else { startedAt=SystemClock.elapsedRealtime(); running=true; start.setText(R.string.pause); handler.post(tick); }
    }
    private void reset() { running=false; accumulated=0; handler.removeCallbacks(tick); display.setText(R.string.stopwatch_zero); start.setText(R.string.start); }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long value=accumulated+SystemClock.elapsedRealtime()-startedAt;
            display.setText(String.format(Locale.getDefault(), "%02d:%02d.%02d", value/60_000, (value/1_000)%60, (value/10)%100));
            handler.postDelayed(this, 30);
        }
    };
}
