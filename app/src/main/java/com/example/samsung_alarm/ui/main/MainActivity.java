package com.example.samsung_alarm.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.app.NotificationManager;
import android.provider.Settings;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.service.AlarmScheduler;
import com.example.samsung_alarm.data.model.Alarm;
import com.example.samsung_alarm.data.repository.AlarmRepository;
import com.example.samsung_alarm.settings.AppPreferences;
import com.example.samsung_alarm.ui.edit.EditAlarmActivity;
import com.example.samsung_alarm.ui.common.LocalizedActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class MainActivity extends LocalizedActivity implements AlarmAdapter.Listener {
    private AlarmRepository repository;
    private View alarmsPane, quickPane, timerPane, stopwatchPane, addButton;
    private TextView title, subtitle, upcoming, empty;
    private AlarmAdapter adapter;
    private QuickAlarmAdapter quickAdapter;
    private boolean fullScreenDialogShown;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()); view.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        repository=AlarmRepository.get(this); bindViews(); setupAlarmList(); setupNavigation(); setupQuick();
        new TimerController(this, this::ensureExactPermission); new StopwatchController(this);
        addButton.setOnClickListener(v -> startActivity(new Intent(this, EditAlarmActivity.class)));
        findViewById(R.id.settingsButton).setOnClickListener(v -> showSettings()); requestNotificationPermission();
    }

    private void bindViews() {
        alarmsPane=findViewById(R.id.paneAlarms); quickPane=findViewById(R.id.paneQuick); timerPane=findViewById(R.id.paneTimer);
        stopwatchPane=findViewById(R.id.paneStopwatch); addButton=findViewById(R.id.addAlarm); title=findViewById(R.id.title);
        subtitle=findViewById(R.id.subtitle); upcoming=findViewById(R.id.upcomingText); empty=findViewById(R.id.emptyText);
    }
    private void setupAlarmList() {
        RecyclerView list=findViewById(R.id.alarmList); adapter=new AlarmAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter);
        RecyclerView quickList=findViewById(R.id.quickAlarmList); quickAdapter=new QuickAlarmAdapter(this::confirmDeleteQuick);
        quickList.setLayoutManager(new LinearLayoutManager(this)); quickList.setAdapter(quickAdapter);
        repository.observeAll().observe(this, alarms -> {
            List<Alarm> regular=new ArrayList<>(), quick=new ArrayList<>();
            if(alarms!=null)for(Alarm alarm:alarms){if(alarm.isQuickAlarm&&alarm.isActive)quick.add(alarm);else if(!alarm.isQuickAlarm)regular.add(alarm);}
            adapter.submit(regular);quickAdapter.submit(quick);empty.setVisibility(regular.isEmpty()?View.VISIBLE:View.GONE);
            findViewById(R.id.quickEmptyText).setVisibility(quick.isEmpty()?View.VISIBLE:View.GONE);updateUpcoming(alarms);
        });
    }
    private void updateUpcoming(List<Alarm> alarms) {
        Alarm best=null; long time=Long.MAX_VALUE;
        if (alarms!=null) for (Alarm alarm:alarms) if (alarm.isActive) { long candidate=AlarmScheduler.nextTrigger(alarm); if(candidate<time){best=alarm;time=candidate;} }
        if(best==null) upcoming.setText(R.string.no_upcoming);
        else upcoming.setText(getString(R.string.upcoming_format, new SimpleDateFormat("EEE, HH:mm",Locale.getDefault()).format(new Date(time)),best.label));
    }
    private void setupNavigation() {
        findViewById(R.id.navAlarm).setOnClickListener(v->showPage(0)); findViewById(R.id.navQuick).setOnClickListener(v->showPage(1));
        findViewById(R.id.navTimer).setOnClickListener(v->showPage(2)); findViewById(R.id.navStopwatch).setOnClickListener(v->showPage(3));
    }
    private void showPage(int page) {
        alarmsPane.setVisibility(page==0?View.VISIBLE:View.GONE); quickPane.setVisibility(page==1?View.VISIBLE:View.GONE);
        timerPane.setVisibility(page==2?View.VISIBLE:View.GONE); stopwatchPane.setVisibility(page==3?View.VISIBLE:View.GONE); addButton.setVisibility(page==0?View.VISIBLE:View.GONE);
        int[] names={R.string.alarm,R.string.quick_alarm,R.string.timer,R.string.stopwatch};
        int[] subtitles={R.string.alarm_subtitle,R.string.quick_subtitle,R.string.timer_subtitle,R.string.stopwatch_subtitle};
        title.setText(names[page]); subtitle.setText(subtitles[page]);
    }
    private void setupQuick() {
        findViewById(R.id.quick5).setOnClickListener(v->scheduleQuick(5)); findViewById(R.id.quick10).setOnClickListener(v->scheduleQuick(10));
        findViewById(R.id.quick15).setOnClickListener(v->scheduleQuick(15)); findViewById(R.id.quick30).setOnClickListener(v->scheduleQuick(30));
        findViewById(R.id.quickStart).setOnClickListener(v->{
            String value=((EditText)findViewById(R.id.quickCustom)).getText().toString().trim();
            try { scheduleQuick(Math.max(1,Integer.parseInt(value))); } catch (NumberFormatException e) { Toast.makeText(this,R.string.enter_minutes,Toast.LENGTH_SHORT).show(); }
        });
    }
    private void scheduleQuick(int minutes) {
        if(!ensureExactPermission())return;
        if(!canUseFullScreenAlarms()){requestFullScreenPermissionIfNeeded();return;}
        boolean math=((MaterialSwitch)findViewById(R.id.quickMathSwitch)).isChecked();repository.createQuickAlarm(minutes,getString(R.string.quick_alarm),math,null);Toast.makeText(this,getString(R.string.quick_scheduled,minutes),Toast.LENGTH_LONG).show();
    }
    private void showSettings() {
        int padding=(int)(20*getResources().getDisplayMetrics().density);
        LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(padding,0,padding,0);
        TextView themeLabel=new TextView(this); themeLabel.setText(R.string.theme); content.addView(themeLabel);
        Spinner theme=new Spinner(this); theme.setAdapter(ArrayAdapter.createFromResource(this,R.array.theme_options,android.R.layout.simple_spinner_dropdown_item)); theme.setSelection(AppPreferences.theme(this)); content.addView(theme);
        TextView languageLabel=new TextView(this); languageLabel.setText(R.string.language); languageLabel.setPadding(0,padding/2,0,0); content.addView(languageLabel);
        Spinner language=new Spinner(this); language.setAdapter(ArrayAdapter.createFromResource(this,R.array.language_options,android.R.layout.simple_spinner_dropdown_item)); language.setSelection("en".equals(AppPreferences.language(this))?1:0); content.addView(language);
        new MaterialAlertDialogBuilder(this).setTitle(R.string.appearance_language).setView(content).setNegativeButton(R.string.cancel,null)
                .setPositiveButton(R.string.done,(dialog,which)->AppPreferences.save(this,theme.getSelectedItemPosition(),language.getSelectedItemPosition()==1?"en":"vi")).show();
    }

    @Override public void edit(Alarm alarm) { startActivity(new Intent(this,EditAlarmActivity.class).putExtra("alarm_id",alarm.id)); }
    @Override public void toggle(Alarm alarm,boolean active) { repository.setActive(alarm,active); if(active&&!ensureExactPermission())Toast.makeText(this,R.string.exact_alarm_permission,Toast.LENGTH_LONG).show(); }
    @Override public void delete(Alarm alarm) { new MaterialAlertDialogBuilder(this).setTitle(R.string.delete_alarm_title).setMessage(getString(R.string.delete_alarm_message,alarm.hour,alarm.minute,alarm.label)).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.delete,(d,w)->repository.delete(alarm)).show(); }
    private void confirmDeleteQuick(Alarm alarm){new MaterialAlertDialogBuilder(this).setTitle(R.string.delete_quick_alarm_title).setMessage(getString(R.string.delete_alarm_message,alarm.hour,alarm.minute,alarm.label)).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.delete,(d,w)->repository.delete(alarm)).show();}
    @Override public void skipNext(Alarm alarm) { repository.toggleSkipNext(alarm); }
    private boolean ensureExactPermission() { if(AlarmScheduler.canScheduleExact(this))return true; if(Build.VERSION.SDK_INT>=31)startActivity(AlarmScheduler.exactAlarmPermissionIntent(this)); return false; }
    private void requestNotificationPermission() { if(Build.VERSION.SDK_INT>=33&&ActivityCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},200); }
    @Override protected void onResume() { super.onResume(); if(repository!=null&&AlarmScheduler.canScheduleExact(this))repository.rescheduleAll(); requestFullScreenPermissionIfNeeded(); }
    private boolean canUseFullScreenAlarms(){if(Build.VERSION.SDK_INT<34)return true;NotificationManager manager=getSystemService(NotificationManager.class);return manager!=null&&manager.canUseFullScreenIntent();}
    private void requestFullScreenPermissionIfNeeded(){
        if(canUseFullScreenAlarms()||fullScreenDialogShown)return;
        fullScreenDialogShown=true;
        AlertDialog dialog=new MaterialAlertDialogBuilder(this).setTitle(R.string.full_screen_permission_title).setMessage(R.string.full_screen_permission_message).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.open_settings,(value,which)->{Intent intent=new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName()));startActivity(intent);}).create();
        dialog.setOnDismissListener(value->fullScreenDialogShown=false);dialog.show();
    }
}
