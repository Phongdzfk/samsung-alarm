package com.example.samsung_alarm.ui.main;

import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.service.AlarmScheduler;
import com.example.samsung_alarm.ui.common.SimpleSeekBarListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Locale;

final class TimerController {
    private static final int MAX_MINUTES = 10_080;
    interface PermissionGate { boolean canSchedule(); }
    private final Activity activity;
    private final PermissionGate permissionGate;
    private final TextView display;
    private final SeekBar seek;
    private final Button start;
    private final TextInputLayout inputLayout;
    private final TextInputEditText minuteInput;
    private CountDownTimer timer;
    private int minutes = 5;
    private long remaining;
    private boolean syncing;

    TimerController(Activity activity, PermissionGate permissionGate) {
        this.activity = activity; this.permissionGate = permissionGate;
        display = activity.findViewById(R.id.timerDisplay); seek = activity.findViewById(R.id.timerSeek);
        start = activity.findViewById(R.id.timerStart);inputLayout=activity.findViewById(R.id.timerMinutesLayout);
        minuteInput=activity.findViewById(R.id.timerMinutesInput);
        seek.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if(syncing)return;
                minutes = Math.max(1, progress);
                if(fromUser){syncing=true;minuteInput.setText(String.valueOf(minutes));minuteInput.setSelection(minuteInput.length());syncing=false;inputLayout.setError(null);}
                if (timer == null && remaining == 0) display.setText(format(minutes*60_000L));
            }
        });
        minuteInput.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence value,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence value,int start,int before,int count){}
            @Override public void afterTextChanged(Editable value){if(!syncing&&timer==null&&remaining==0)readInput(false);}
        });
        minuteInput.setOnEditorActionListener((view,action,event)->{
            if(action!=EditorInfo.IME_ACTION_DONE)return false;
            readInput(true);minuteInput.clearFocus();
            InputMethodManager keyboard=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(minuteInput.getWindowToken(),0);
            return true;
        });
        syncing=true;minuteInput.setText(String.valueOf(minutes));minuteInput.setSelection(minuteInput.length());syncing=false;
        display.setText(format(minutes*60_000L));
        start.setOnClickListener(v -> toggle());
        activity.findViewById(R.id.timerReset).setOnClickListener(v -> reset());
    }

    private void toggle() {
        if (timer != null) {
            timer.cancel(); timer = null; AlarmScheduler.cancelTimer(activity);
            start.setText(R.string.resume); return;
        }
        if (!permissionGate.canSchedule()) return;
        if(remaining==0&&!readInput(true))return;
        long millis = remaining > 0 ? remaining : minutes * 60_000L;
        remaining = millis; AlarmScheduler.scheduleTimer(activity, millis);
        setEditingEnabled(false);
        timer = new CountDownTimer(millis, 1_000L) {
            @Override public void onTick(long value) { remaining = value; display.setText(format(value)); }
            @Override public void onFinish() { timer = null; remaining = 0; display.setText(format(0));setEditingEnabled(true);start.setText(R.string.start); }
        }.start();
        start.setText(R.string.pause);
    }

    private void reset() {
        if (timer != null) timer.cancel();
        timer = null; remaining = 0; AlarmScheduler.cancelTimer(activity);
        if(!readInput(false)){minutes=5;syncing=true;minuteInput.setText(String.valueOf(minutes));seek.setProgress(minutes);syncing=false;inputLayout.setError(null);}
        display.setText(format(minutes*60_000L));setEditingEnabled(true);
        start.setText(R.string.start);
    }
    private boolean readInput(boolean showEmptyError){
        String text=minuteInput.getText()==null?"":minuteInput.getText().toString().trim();
        if(text.isEmpty()){inputLayout.setError(showEmptyError?activity.getString(R.string.timer_minutes_invalid):null);return false;}
        long value;
        try{value=Long.parseLong(text);}catch(NumberFormatException error){inputLayout.setError(activity.getString(R.string.timer_minutes_invalid));return false;}
        if(value<1||value>MAX_MINUTES){inputLayout.setError(activity.getString(R.string.timer_minutes_invalid));return false;}
        inputLayout.setError(null);minutes=(int)value;
        syncing=true;seek.setProgress(Math.min(120,minutes));syncing=false;
        display.setText(format(minutes*60_000L));return true;
    }
    private void setEditingEnabled(boolean enabled){seek.setEnabled(enabled);inputLayout.setEnabled(enabled);minuteInput.setEnabled(enabled);}
    private String format(long millis) {
        long seconds=(millis+999)/1000,hours=seconds/3600,minutesPart=(seconds%3600)/60,secondsPart=seconds%60;
        return hours>0?String.format(Locale.getDefault(),"%02d:%02d:%02d",hours,minutesPart,secondsPart)
                :String.format(Locale.getDefault(),"%02d:%02d",minutesPart,secondsPart);
    }
}
