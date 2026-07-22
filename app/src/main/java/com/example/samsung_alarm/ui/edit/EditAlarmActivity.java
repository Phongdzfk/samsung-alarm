package com.example.samsung_alarm.ui.edit;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.data.model.Alarm;
import com.example.samsung_alarm.data.repository.AlarmRepository;
import com.example.samsung_alarm.service.AlarmScheduler;
import com.example.samsung_alarm.ui.common.SimpleSeekBarListener;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

public class EditAlarmActivity extends AppCompatActivity {
    private AlarmRepository repository;
    private Alarm alarm;
    private TimePicker time;
    private TextInputEditText label;
    private MaterialButton mon,tue,wed,thu,fri,sat,sun;
    private SeekBar volume,snooze,autoAfter;
    private TextView volumeLabel,snoozeLabel,autoAfterLabel,ringtoneButton;
    private MaterialSwitch math,keep,gradual,vibrate;
    private Spinner difficulty,autoAction;
    private Uri ringtoneUri;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_edit_alarm); repository=AlarmRepository.get(this); bind(); setup();
        int id=getIntent().getIntExtra("alarm_id",0);
        if(id==0) { alarm=new Alarm(); alarm.label=getString(R.string.alarm); Calendar c=Calendar.getInstance(); c.add(Calendar.MINUTE,1); alarm.hour=c.get(Calendar.HOUR_OF_DAY); alarm.minute=c.get(Calendar.MINUTE); populate(); }
        else repository.getById(id, found->runOnUiThread(()->{ if(found==null){finish();return;} alarm=found; ((TextView)findViewById(R.id.editTitle)).setText(R.string.edit_alarm); populate(); }));
    }
    private void bind() {
        time=findViewById(R.id.timePicker); time.setIs24HourView(true); label=findViewById(R.id.labelInput);
        mon=findViewById(R.id.dayMon);tue=findViewById(R.id.dayTue);wed=findViewById(R.id.dayWed);thu=findViewById(R.id.dayThu);fri=findViewById(R.id.dayFri);sat=findViewById(R.id.daySat);sun=findViewById(R.id.daySun);
        volume=findViewById(R.id.volumeSeek);snooze=findViewById(R.id.snoozeSeek);autoAfter=findViewById(R.id.autoAfterSeek);
        volumeLabel=findViewById(R.id.volumeLabel);snoozeLabel=findViewById(R.id.snoozeLabel);autoAfterLabel=findViewById(R.id.autoAfterLabel);ringtoneButton=findViewById(R.id.ringtoneButton);
        math=findViewById(R.id.mathSwitch);keep=findViewById(R.id.keepSwitch);gradual=findViewById(R.id.gradualSwitch);vibrate=findViewById(R.id.vibrateSwitch);
        difficulty=findViewById(R.id.difficultySpinner);autoAction=findViewById(R.id.autoActionSpinner);
    }
    private void setup() {
        mon.setCheckable(true);tue.setCheckable(true);wed.setCheckable(true);thu.setCheckable(true);fri.setCheckable(true);sat.setCheckable(true);sun.setCheckable(true);
        difficulty.setAdapter(ArrayAdapter.createFromResource(this,R.array.math_difficulties,android.R.layout.simple_spinner_dropdown_item));
        autoAction.setAdapter(ArrayAdapter.createFromResource(this,R.array.auto_actions,android.R.layout.simple_spinner_dropdown_item));
        math.setOnCheckedChangeListener((button,checked)->difficulty.setVisibility(checked?View.VISIBLE:View.GONE));
        volume.setOnSeekBarChangeListener(new SimpleSeekBarListener(){@Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser){volumeLabel.setText(getString(R.string.volume_format,value));}});
        snooze.setOnSeekBarChangeListener(new SimpleSeekBarListener(){@Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser){snoozeLabel.setText(getString(R.string.snooze_duration,value+1));}});
        autoAfter.setOnSeekBarChangeListener(new SimpleSeekBarListener(){@Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser){autoAfterLabel.setText(getString(R.string.after_minutes,value+1));}});
        ringtoneButton.setOnClickListener(v->pickRingtone()); findViewById(R.id.backButton).setOnClickListener(v->finish()); findViewById(R.id.saveButton).setOnClickListener(v->save());
    }
    private void populate() {
        time.setHour(alarm.hour);time.setMinute(alarm.minute);label.setText(alarm.label);mon.setChecked(alarm.mon);tue.setChecked(alarm.tue);wed.setChecked(alarm.wed);thu.setChecked(alarm.thu);fri.setChecked(alarm.fri);sat.setChecked(alarm.sat);sun.setChecked(alarm.sun);
        volume.setProgress(alarm.volume);math.setChecked(alarm.isMathDismiss);difficulty.setSelection(Math.max(0,alarm.mathDifficulty-1));keep.setChecked(alarm.keepAfterDismiss);gradual.setChecked(alarm.gradualVolume);vibrate.setChecked(alarm.vibrate);
        autoAction.setSelection(alarm.autoAction);autoAfter.setProgress(Math.max(0,alarm.autoAfterMinutes-1));snooze.setProgress(Math.max(0,alarm.snoozeMinutes-1));
        if(alarm.ringtoneUri!=null){ringtoneUri=Uri.parse(alarm.ringtoneUri);showRingtoneName();}
    }
    private void pickRingtone() {
        RingtoneManager manager=new RingtoneManager(this);manager.setType(RingtoneManager.TYPE_ALARM);
        List<String> names=new ArrayList<>();List<Uri> uris=new ArrayList<>();names.add(getString(R.string.ringtone_selected_default));uris.add(null);int selected=0;
        android.database.Cursor cursor=manager.getCursor();
        for(int position=0;position<cursor.getCount();position++){cursor.moveToPosition(position);Uri uri=manager.getRingtoneUri(position);names.add(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));uris.add(uri);if(ringtoneUri!=null&&ringtoneUri.equals(uri))selected=position+1;}
        new MaterialAlertDialogBuilder(this).setTitle(R.string.choose_alarm_sound).setSingleChoiceItems(names.toArray(new String[0]),selected,(dialog,which)->{ringtoneUri=uris.get(which);showRingtoneName();dialog.dismiss();}).setNegativeButton(R.string.cancel,null).show();
    }
    private void showRingtoneName() {
        if(ringtoneUri==null){ringtoneButton.setText(R.string.ringtone_default);return;} Ringtone ringtone=RingtoneManager.getRingtone(this,ringtoneUri);
        ringtoneButton.setText(getString(R.string.ringtone_format,ringtone==null?getString(R.string.ringtone_selected):ringtone.getTitle(this)));
    }
    private void save() {
        if(!AlarmScheduler.canScheduleExact(this)){if(Build.VERSION.SDK_INT>=31)startActivity(AlarmScheduler.exactAlarmPermissionIntent(this));return;}
        if(Build.VERSION.SDK_INT>=34){NotificationManager manager=getSystemService(NotificationManager.class);if(manager==null||!manager.canUseFullScreenIntent()){new MaterialAlertDialogBuilder(this).setTitle(R.string.full_screen_permission_title).setMessage(R.string.full_screen_permission_message).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.open_settings,(dialog,which)->startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())))).show();return;}}
        if(alarm==null)return;alarm.hour=time.getHour();alarm.minute=time.getMinute();String value=label.getText()==null?"":label.getText().toString().trim();alarm.label=value.isEmpty()?getString(R.string.alarm):value;
        alarm.mon=mon.isChecked();alarm.tue=tue.isChecked();alarm.wed=wed.isChecked();alarm.thu=thu.isChecked();alarm.fri=fri.isChecked();alarm.sat=sat.isChecked();alarm.sun=sun.isChecked();alarm.volume=volume.getProgress();alarm.ringtoneUri=ringtoneUri==null?null:ringtoneUri.toString();
        alarm.isMathDismiss=math.isChecked();alarm.mathDifficulty=difficulty.getSelectedItemPosition()+1;alarm.keepAfterDismiss=keep.isChecked();alarm.gradualVolume=gradual.isChecked();alarm.vibrate=vibrate.isChecked();alarm.autoAction=autoAction.getSelectedItemPosition();alarm.autoAfterMinutes=autoAfter.getProgress()+1;alarm.snoozeMinutes=snooze.getProgress()+1;alarm.isActive=true;
        repository.save(alarm,()->runOnUiThread(()->{Toast.makeText(this,R.string.alarm_saved,Toast.LENGTH_SHORT).show();finish();}));
    }
}
