package com.example.samsung_alarm.ui.ring;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.service.AlarmReceiver;
import com.example.samsung_alarm.service.AlarmScheduler;
import com.example.samsung_alarm.data.model.Alarm;
import com.example.samsung_alarm.data.repository.AlarmRepository;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class RingActivity extends AppCompatActivity {
    private int alarmId, answer;
    private Alarm alarm;
    private TextView question,error,snooze;
    private EditText input;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);setContentView(R.layout.activity_ring);
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){}});
        alarmId=getIntent().getIntExtra(AlarmScheduler.EXTRA_ALARM_ID,-1);question=findViewById(R.id.mathQuestion);error=findViewById(R.id.mathError);input=findViewById(R.id.mathAnswer);snooze=findViewById(R.id.snoozeButton);
        Calendar now=Calendar.getInstance();((TextView)findViewById(R.id.ringTime)).setText(String.format(Locale.getDefault(),"%02d:%02d",now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE)));
        findViewById(R.id.dismissButton).setOnClickListener(v->dismiss());snooze.setOnClickListener(v->snooze());
        if(alarmId>0)AlarmRepository.get(this).getById(alarmId,value->runOnUiThread(()->apply(value)));else((TextView)findViewById(R.id.ringLabel)).setText(R.string.quick_alarm);
    }
    private void apply(Alarm value) {
        alarm=value;if(alarm==null)return;((TextView)findViewById(R.id.ringTime)).setText(String.format(Locale.getDefault(),"%02d:%02d",alarm.hour,alarm.minute));((TextView)findViewById(R.id.ringLabel)).setText(alarm.label);
        snooze.setText(getString(R.string.snooze_button,alarm.snoozeMinutes));if(alarm.isMathDismiss){findViewById(R.id.mathPanel).setVisibility(View.VISIBLE);newProblem();}
    }
    private void newProblem() {
        Random random=new Random();int level=alarm==null?1:alarm.mathDifficulty;
        if(level==1){int a=random.nextInt(40)+5,b=random.nextInt(30)+1;boolean minus=random.nextBoolean();if(minus&&b>a){int t=a;a=b;b=t;}answer=minus?a-b:a+b;question.setText(getString(minus?R.string.math_subtract:R.string.math_add,a,b));}
        else if(level==2){int a=random.nextInt(12)+2,b=random.nextInt(12)+2;answer=a*b;question.setText(getString(R.string.math_multiply,a,b));}
        else{int a=random.nextInt(15)+2,b=random.nextInt(10)+2,c=random.nextInt(8)+2;answer=a+b*c;question.setText(getString(R.string.math_hard,a,b,c));}input.setText("");
    }
    private void dismiss() {
        if(alarm!=null&&alarm.isMathDismiss){int entered;try{entered=Integer.parseInt(input.getText().toString().trim());}catch(NumberFormatException e){entered=Integer.MIN_VALUE;}if(entered!=answer){error.setText(R.string.wrong_answer);newProblem();return;}}
        send(AlarmReceiver.ACTION_DISMISS,0);
    }
    private void snooze(){send(AlarmReceiver.ACTION_SNOOZE,alarm==null?5:alarm.snoozeMinutes);}
    private void send(String action,int minutes){sendBroadcast(new Intent(this,AlarmReceiver.class).setAction(action).putExtra(AlarmScheduler.EXTRA_ALARM_ID,alarmId).putExtra("minutes",minutes));finishAndRemoveTask();}
}
