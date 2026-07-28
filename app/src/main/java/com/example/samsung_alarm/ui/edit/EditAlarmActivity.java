package com.example.samsung_alarm.ui.edit;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.app.NotificationManager;
import android.database.Cursor;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.data.model.Alarm;
import com.example.samsung_alarm.data.repository.AlarmRepository;
import com.example.samsung_alarm.service.AlarmScheduler;
import com.example.samsung_alarm.ui.common.SimpleSeekBarListener;
import com.example.samsung_alarm.ui.common.LocalizedActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

public class EditAlarmActivity extends LocalizedActivity {
    private AlarmRepository repository;
    private Alarm alarm;
    private TimePicker time;
    private TextInputEditText label;
    private MaterialButton everyDay,mon,tue,wed,thu,fri,sat,sun;
    private SeekBar volume,snooze,autoAfter;
    private TextView volumeLabel,snoozeLabel,autoAfterLabel,ringtoneButton,autoActionTitle;
    private MaterialButton previewRingtoneButton;
    private MaterialSwitch math,keep,gradual,vibrate;
    private Spinner difficulty,autoAction;
    private Uri ringtoneUri;
    private MediaPlayer previewPlayer;
    private final ActivityResultLauncher<String[]> audioPicker=registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),uri->{
                if(uri==null)return;
                try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
                catch(SecurityException ignored){}
                stopPreview();ringtoneUri=uri;showRingtoneName();
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_edit_alarm); repository=AlarmRepository.get(this); bind(); setup();
        int id=getIntent().getIntExtra("alarm_id",0);
        if(id==0) { alarm=new Alarm(); alarm.label=getString(R.string.alarm); Calendar c=Calendar.getInstance(); c.add(Calendar.MINUTE,1); alarm.hour=c.get(Calendar.HOUR_OF_DAY); alarm.minute=c.get(Calendar.MINUTE); populate(); }
        else repository.getById(id, found->runOnUiThread(()->{ if(found==null){finish();return;} alarm=found; ((TextView)findViewById(R.id.editTitle)).setText(R.string.edit_alarm); populate(); }));
    }
    private void bind() {
        time=findViewById(R.id.timePicker); time.setIs24HourView(true); label=findViewById(R.id.labelInput);
        everyDay=findViewById(R.id.dayEvery);mon=findViewById(R.id.dayMon);tue=findViewById(R.id.dayTue);wed=findViewById(R.id.dayWed);thu=findViewById(R.id.dayThu);fri=findViewById(R.id.dayFri);sat=findViewById(R.id.daySat);sun=findViewById(R.id.daySun);
        volume=findViewById(R.id.volumeSeek);snooze=findViewById(R.id.snoozeSeek);autoAfter=findViewById(R.id.autoAfterSeek);
        volumeLabel=findViewById(R.id.volumeLabel);snoozeLabel=findViewById(R.id.snoozeLabel);autoAfterLabel=findViewById(R.id.autoAfterLabel);ringtoneButton=findViewById(R.id.ringtoneButton);autoActionTitle=findViewById(R.id.autoActionTitle);previewRingtoneButton=findViewById(R.id.previewRingtoneButton);
        math=findViewById(R.id.mathSwitch);keep=findViewById(R.id.keepSwitch);gradual=findViewById(R.id.gradualSwitch);vibrate=findViewById(R.id.vibrateSwitch);
        difficulty=findViewById(R.id.difficultySpinner);autoAction=findViewById(R.id.autoActionSpinner);
    }
    private void setup() {
        everyDay.setCheckable(true);mon.setCheckable(true);tue.setCheckable(true);wed.setCheckable(true);thu.setCheckable(true);fri.setCheckable(true);sat.setCheckable(true);sun.setCheckable(true);
        everyDay.setOnClickListener(view->setAllDays(everyDay.isChecked()));
        MaterialButton.OnCheckedChangeListener dayListener=(button,checked)->syncEveryDayButton();
        mon.addOnCheckedChangeListener(dayListener);tue.addOnCheckedChangeListener(dayListener);wed.addOnCheckedChangeListener(dayListener);thu.addOnCheckedChangeListener(dayListener);fri.addOnCheckedChangeListener(dayListener);sat.addOnCheckedChangeListener(dayListener);sun.addOnCheckedChangeListener(dayListener);
        difficulty.setAdapter(ArrayAdapter.createFromResource(this,R.array.math_difficulties,android.R.layout.simple_spinner_dropdown_item));
        autoAction.setAdapter(ArrayAdapter.createFromResource(this,R.array.auto_actions,android.R.layout.simple_spinner_dropdown_item));
        math.setOnCheckedChangeListener((button,checked)->updateMathControls(checked));
        volume.setOnSeekBarChangeListener(new SimpleSeekBarListener(){@Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser){volumeLabel.setText(getString(R.string.volume_format,value));}});
        snooze.setOnSeekBarChangeListener(new SimpleSeekBarListener(){@Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser){snoozeLabel.setText(getString(R.string.snooze_duration,value+1));}});
        autoAfter.setOnSeekBarChangeListener(new SimpleSeekBarListener(){@Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser){autoAfterLabel.setText(getString(R.string.after_minutes,value+1));}});
        ringtoneButton.setOnClickListener(v->pickRingtone());previewRingtoneButton.setOnClickListener(v->togglePreview());
        findViewById(R.id.backButton).setOnClickListener(v->finish()); findViewById(R.id.saveButton).setOnClickListener(v->save());
    }
    private void populate() {
        time.setHour(alarm.hour);time.setMinute(alarm.minute);label.setText(alarm.label);mon.setChecked(alarm.mon);tue.setChecked(alarm.tue);wed.setChecked(alarm.wed);thu.setChecked(alarm.thu);fri.setChecked(alarm.fri);sat.setChecked(alarm.sat);sun.setChecked(alarm.sun);syncEveryDayButton();
        volume.setProgress(alarm.volume);difficulty.setSelection(Math.max(0,alarm.mathDifficulty-1));keep.setChecked(alarm.keepAfterDismiss);gradual.setChecked(alarm.gradualVolume);vibrate.setChecked(alarm.vibrate);
        autoAction.setSelection(alarm.autoAction);autoAfter.setProgress(Math.max(0,alarm.autoAfterMinutes-1));snooze.setProgress(Math.max(0,alarm.snoozeMinutes-1));
        math.setChecked(alarm.isMathDismiss);updateMathControls(alarm.isMathDismiss);
        if(alarm.ringtoneUri!=null){ringtoneUri=Uri.parse(alarm.ringtoneUri);showRingtoneName();}else showRingtoneName();
    }
    private void pickRingtone() {
        RingtoneManager manager=new RingtoneManager(this);manager.setType(RingtoneManager.TYPE_ALARM);
        List<String> names=new ArrayList<>();List<Uri> uris=new ArrayList<>();names.add(getString(R.string.ringtone_selected_default));uris.add(null);int selected=0;
        Cursor cursor=manager.getCursor();
        for(int position=0;position<cursor.getCount();position++){cursor.moveToPosition(position);Uri uri=manager.getRingtoneUri(position);names.add(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));uris.add(uri);if(ringtoneUri!=null&&ringtoneUri.equals(uri))selected=position+1;}
        int customIndex=names.size();names.add(getString(R.string.choose_music_from_device));uris.add(Uri.EMPTY);
        if(ringtoneUri!=null&&selected==0)selected=customIndex;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.choose_alarm_sound).setSingleChoiceItems(names.toArray(new String[0]),selected,(dialog,which)->{
            dialog.dismiss();
            if(Uri.EMPTY.equals(uris.get(which))){audioPicker.launch(new String[]{"audio/*"});return;}
            stopPreview();ringtoneUri=uris.get(which);showRingtoneName();
        }).setNegativeButton(R.string.cancel,null).show();
    }
    private void showRingtoneName() {
        if(ringtoneUri==null){ringtoneButton.setText(R.string.ringtone_default);return;}
        String name=displayName(ringtoneUri);
        ringtoneButton.setText(getString(R.string.ringtone_format,name));
    }
    private String displayName(Uri uri){
        if("content".equals(uri.getScheme())){
            try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                if(cursor!=null&&cursor.moveToFirst()){int column=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(column>=0){String value=cursor.getString(column);if(value!=null&&!value.isEmpty())return value;}}
            }catch(Exception ignored){}
        }
        Ringtone ringtone=RingtoneManager.getRingtone(this,uri);
        return ringtone==null?getString(R.string.ringtone_selected):ringtone.getTitle(this);
    }
    private void togglePreview(){
        if(previewPlayer!=null){stopPreview();return;}
        Uri uri=ringtoneUri==null?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):ringtoneUri;
        try{
            previewPlayer=new MediaPlayer();
            previewPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            previewPlayer.setDataSource(this,uri);float level=Math.max(0,Math.min(100,volume.getProgress()))/100f;previewPlayer.setVolume(level,level);
            previewPlayer.setOnCompletionListener(player->stopPreview());previewPlayer.prepare();previewPlayer.start();previewRingtoneButton.setText(R.string.stop_preview);
        }catch(Exception error){stopPreview();Toast.makeText(this,R.string.unable_to_play_sound,Toast.LENGTH_SHORT).show();}
    }
    private void stopPreview(){
        if(previewPlayer!=null){try{previewPlayer.stop();}catch(Exception ignored){}previewPlayer.release();previewPlayer=null;}
        if(previewRingtoneButton!=null)previewRingtoneButton.setText(R.string.preview_sound);
    }
    private void updateMathControls(boolean enabled){
        difficulty.setVisibility(enabled?View.VISIBLE:View.GONE);
        if(enabled)autoAction.setSelection(0);
        int optionalVisibility=enabled?View.GONE:View.VISIBLE;
        autoActionTitle.setVisibility(optionalVisibility);autoAction.setVisibility(optionalVisibility);
        autoAfterLabel.setVisibility(optionalVisibility);autoAfter.setVisibility(optionalVisibility);
        snoozeLabel.setVisibility(optionalVisibility);snooze.setVisibility(optionalVisibility);
    }
    private void setAllDays(boolean checked){
        mon.setChecked(checked);tue.setChecked(checked);wed.setChecked(checked);thu.setChecked(checked);fri.setChecked(checked);sat.setChecked(checked);sun.setChecked(checked);
        everyDay.setChecked(checked);
    }
    private void syncEveryDayButton(){
        everyDay.setChecked(mon.isChecked()&&tue.isChecked()&&wed.isChecked()&&thu.isChecked()&&fri.isChecked()&&sat.isChecked()&&sun.isChecked());
    }
    private void save() {
        if(!AlarmScheduler.canScheduleExact(this)){if(Build.VERSION.SDK_INT>=31)startActivity(AlarmScheduler.exactAlarmPermissionIntent(this));return;}
        if(Build.VERSION.SDK_INT>=34){NotificationManager manager=getSystemService(NotificationManager.class);if(manager==null||!manager.canUseFullScreenIntent()){new MaterialAlertDialogBuilder(this).setTitle(R.string.full_screen_permission_title).setMessage(R.string.full_screen_permission_message).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.open_settings,(dialog,which)->startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())))).show();return;}}
        if(alarm==null)return;alarm.hour=time.getHour();alarm.minute=time.getMinute();String value=label.getText()==null?"":label.getText().toString().trim();alarm.label=value.isEmpty()?getString(R.string.alarm):value;
        alarm.mon=mon.isChecked();alarm.tue=tue.isChecked();alarm.wed=wed.isChecked();alarm.thu=thu.isChecked();alarm.fri=fri.isChecked();alarm.sat=sat.isChecked();alarm.sun=sun.isChecked();alarm.volume=volume.getProgress();alarm.ringtoneUri=ringtoneUri==null?null:ringtoneUri.toString();
        alarm.isMathDismiss=math.isChecked();alarm.mathDifficulty=difficulty.getSelectedItemPosition()+1;alarm.keepAfterDismiss=keep.isChecked();alarm.gradualVolume=gradual.isChecked();alarm.vibrate=vibrate.isChecked();alarm.autoAction=math.isChecked()?0:autoAction.getSelectedItemPosition();alarm.autoAfterMinutes=autoAfter.getProgress()+1;alarm.snoozeMinutes=snooze.getProgress()+1;alarm.isActive=true;
        repository.save(alarm,()->runOnUiThread(()->{Toast.makeText(this,R.string.alarm_saved,Toast.LENGTH_SHORT).show();finish();}));
    }
    @Override protected void onStop(){stopPreview();super.onStop();}
}
