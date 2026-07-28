package com.example.samsung_alarm.ui.main;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.samsung_alarm.R;
import java.util.Locale;

final class StopwatchController {
    private final Activity activity;
    private final TextView display;
    private final Button start,flag;
    private final LinearLayout flagList;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private long startedAt, accumulated;
    private int flagCount;

    StopwatchController(Activity activity) {
        this.activity=activity;display = activity.findViewById(R.id.stopwatchDisplay); start = activity.findViewById(R.id.stopwatchStart);
        flag=activity.findViewById(R.id.stopwatchFlag);flagList=activity.findViewById(R.id.stopwatchFlagList);
        start.setOnClickListener(v -> toggle());
        flag.setOnClickListener(v->addFlag());
        activity.findViewById(R.id.stopwatchReset).setOnClickListener(v -> reset());
    }
    private void toggle() {
        if (running) { accumulated += SystemClock.elapsedRealtime()-startedAt; running=false;flag.setEnabled(false);start.setText(R.string.resume); }
        else { startedAt=SystemClock.elapsedRealtime(); running=true;flag.setEnabled(true);start.setText(R.string.pause); handler.post(tick); }
    }
    private void addFlag(){
        if(!running)return;
        long value=accumulated+SystemClock.elapsedRealtime()-startedAt;
        View item=LayoutInflater.from(activity).inflate(R.layout.item_stopwatch_flag,flagList,false);
        ((TextView)item.findViewById(R.id.stopwatchFlagNumber)).setText(activity.getString(R.string.flag_number,++flagCount));
        ((TextView)item.findViewById(R.id.stopwatchFlagTime)).setText(format(value));
        flagList.addView(item,0);
    }
    private void reset() { running=false; accumulated=0;flagCount=0;handler.removeCallbacks(tick);flagList.removeAllViews();flag.setEnabled(false);display.setText(R.string.stopwatch_zero); start.setText(R.string.start); }
    private String format(long value){return String.format(Locale.getDefault(), "%02d:%02d.%02d", value/60_000, (value/1_000)%60, (value/10)%100);}
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long value=accumulated+SystemClock.elapsedRealtime()-startedAt;
            display.setText(format(value));
            handler.postDelayed(this, 30);
        }
    };
}
