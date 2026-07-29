package com.example.samsung_alarm.ui.ring;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.service.AlarmReceiver;
import com.example.samsung_alarm.service.AlarmScheduler;
import com.example.samsung_alarm.data.model.Alarm;
import com.example.samsung_alarm.data.repository.AlarmRepository;
import com.example.samsung_alarm.ui.common.LocalizedActivity;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class RingActivity extends LocalizedActivity {
    private int alarmId, answer;
    private Alarm alarm;
    private TextView question,error,snooze,ringTime;
    private EditText input;
    private View dismissGesture,swipeThumb,swipeHint,swipeArrow;
    private float swipeStartY;
    private boolean alarmLoaded;
    private final Handler clockHandler=new Handler(Looper.getMainLooper());
    private final Runnable clockTick=new Runnable(){
        @Override public void run(){
            Calendar now=Calendar.getInstance();
            ringTime.setText(String.format(Locale.getDefault(),"%02d:%02d",now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE)));
            long delay=60_000L-(System.currentTimeMillis()%60_000L)+30L;
            clockHandler.postDelayed(this,delay);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);setContentView(R.layout.activity_ring);
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){}});
        alarmId=getIntent().getIntExtra(AlarmScheduler.EXTRA_ALARM_ID,-1);question=findViewById(R.id.mathQuestion);error=findViewById(R.id.mathError);input=findViewById(R.id.mathAnswer);snooze=findViewById(R.id.snoozeButton);ringTime=findViewById(R.id.ringTime);dismissGesture=findViewById(R.id.dismissGesture);swipeThumb=findViewById(R.id.swipeThumb);swipeHint=findViewById(R.id.swipeHint);swipeArrow=findViewById(R.id.swipeArrow);
        dismissGesture.setOnTouchListener(this::handleSwipe);snooze.setOnClickListener(v->snooze());findViewById(R.id.checkMathButton).setOnClickListener(v->dismiss());
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);input.setOnEditorActionListener((view,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){dismiss();return true;}return false;});
        setDismissEnabled(alarmId<=0);
        if(alarmId>0){applyIntentPreview(getIntent());AlarmRepository.get(this).getById(alarmId,value->runOnUiThread(()->apply(value)));}else{alarmLoaded=true;((TextView)findViewById(R.id.ringLabel)).setText(R.string.quick_alarm);}
    }
    private void apply(Alarm value) {
        boolean mathWasVisible=findViewById(R.id.mathPanel).getVisibility()==View.VISIBLE;alarm=value;if(alarm==null)return;alarmLoaded=true;setDismissEnabled(true);((TextView)findViewById(R.id.ringLabel)).setText(alarm.label);
        snooze.setText(getString(R.string.snooze_button,alarm.snoozeMinutes));snooze.setVisibility(alarm.isMathDismiss?View.GONE:View.VISIBLE);findViewById(R.id.mathPanel).setVisibility(alarm.isMathDismiss?View.VISIBLE:View.GONE);if(alarm.isMathDismiss&&!mathWasVisible)newProblem();
    }
    private void applyIntentPreview(Intent intent){if(!intent.hasExtra(AlarmScheduler.EXTRA_MATH_DISMISS))return;Alarm preview=new Alarm();preview.isMathDismiss=intent.getBooleanExtra(AlarmScheduler.EXTRA_MATH_DISMISS,false);preview.mathDifficulty=intent.getIntExtra(AlarmScheduler.EXTRA_MATH_DIFFICULTY,1);preview.label=intent.getStringExtra(AlarmScheduler.EXTRA_LABEL);preview.hour=intent.getIntExtra(AlarmScheduler.EXTRA_HOUR,0);preview.minute=intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE,0);preview.snoozeMinutes=intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES,5);apply(preview);}
    private void newProblem() {
        Random random=new Random();int level=alarm==null?1:alarm.mathDifficulty;
        if(level==1){int a=random.nextInt(40)+5,b=random.nextInt(30)+1;boolean minus=random.nextBoolean();if(minus&&b>a){int t=a;a=b;b=t;}answer=minus?a-b:a+b;question.setText(getString(minus?R.string.math_subtract:R.string.math_add,a,b));}
        else if(level==2){int a=random.nextInt(12)+2,b=random.nextInt(12)+2;answer=a*b;question.setText(getString(R.string.math_multiply,a,b));}
        else{int a=random.nextInt(15)+2,b=random.nextInt(10)+2,c=random.nextInt(8)+2;answer=a+b*c;question.setText(getString(R.string.math_hard,a,b,c));}input.setText("");
    }
    private void dismiss() {
        if(!alarmLoaded)return;
        if(alarm!=null&&alarm.isMathDismiss){int entered;try{entered=Integer.parseInt(input.getText().toString().trim());}catch(NumberFormatException e){entered=Integer.MIN_VALUE;}if(entered!=answer){error.setText(R.string.wrong_answer);newProblem();resetSwipe();return;}}
        send(AlarmReceiver.ACTION_DISMISS,0);
    }
    private boolean handleSwipe(View view, MotionEvent event){
        if(!alarmLoaded)return true;
        float density=getResources().getDisplayMetrics().density;
        float travel=72f*density;
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                swipeStartY=event.getRawY();view.getParent().requestDisallowInterceptTouchEvent(true);return true;
            case MotionEvent.ACTION_MOVE:
                float distance=Math.max(0,swipeStartY-event.getRawY());
                float progress=Math.min(1f,distance/travel);
                swipeThumb.setTranslationY(-travel*progress);
                swipeThumb.setScaleX(1f-.08f*progress);swipeThumb.setScaleY(1f-.08f*progress);
                swipeHint.setAlpha(1f-.65f*progress);swipeArrow.setAlpha(.72f+.28f*progress);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float releasedDistance=Math.max(0,swipeStartY-event.getRawY());
                if(releasedDistance>=travel*.72f){swipeThumb.animate().translationY(-travel).scaleX(.9f).scaleY(.9f).alpha(0f).setDuration(140).withEndAction(this::dismiss).start();}
                else resetSwipe();
                view.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:return false;
        }
    }
    private void resetSwipe(){swipeThumb.animate().translationY(0).scaleX(1f).scaleY(1f).alpha(1f).setDuration(180).start();swipeHint.animate().alpha(1f).setDuration(180).start();swipeArrow.animate().alpha(.72f).setDuration(180).start();}
    private void setDismissEnabled(boolean enabled){alarmLoaded=enabled;dismissGesture.setEnabled(enabled);dismissGesture.setAlpha(enabled?1f:.45f);}
    private void snooze(){send(AlarmReceiver.ACTION_SNOOZE,alarm==null?5:alarm.snoozeMinutes);}
    private void send(String action,int minutes){sendBroadcast(new Intent(this,AlarmReceiver.class).setAction(action).putExtra(AlarmScheduler.EXTRA_ALARM_ID,alarmId).putExtra("minutes",minutes));finishAndRemoveTask();}
    @Override protected void onResume(){super.onResume();clockHandler.removeCallbacks(clockTick);clockHandler.post(clockTick);}
    @Override protected void onPause(){clockHandler.removeCallbacks(clockTick);super.onPause();}
}
