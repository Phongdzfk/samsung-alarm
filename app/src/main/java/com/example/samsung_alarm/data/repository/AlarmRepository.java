package com.example.samsung_alarm.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.example.samsung_alarm.service.AlarmScheduler;
import com.example.samsung_alarm.service.UpcomingNotificationManager;
import com.example.samsung_alarm.data.AppExecutors;
import com.example.samsung_alarm.data.database.AlarmDao;
import com.example.samsung_alarm.data.database.AppDatabase;
import com.example.samsung_alarm.data.model.Alarm;
import java.util.List;
import java.util.function.Consumer;
import java.util.Calendar;

/** Single entry point for alarm persistence and scheduling. */
public final class AlarmRepository {
    private static volatile AlarmRepository instance;
    private final Context context;
    private final AlarmDao dao;

    private AlarmRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dao = AppDatabase.getInstance(context).alarmDao();
    }

    public static AlarmRepository get(Context context) {
        if (instance == null) synchronized (AlarmRepository.class) {
            if (instance == null) instance = new AlarmRepository(context);
        }
        return instance;
    }

    public LiveData<List<Alarm>> observeAll() { return dao.getAllAlarms(); }
    public Alarm getByIdSync(int id) { return dao.getById(id); }
    public List<Alarm> getActiveSync() { return dao.getActiveAlarms(); }
    public void getById(int id, Consumer<Alarm> callback) {
        AppExecutors.DB.execute(() -> callback.accept(dao.getById(id)));
    }
    public void save(Alarm alarm, Runnable done) {
        AppExecutors.DB.execute(() -> {
            if (alarm.id == 0) alarm.id = (int) dao.insert(alarm);
            else { AlarmScheduler.cancel(context, alarm.id); dao.update(alarm); }
            AlarmScheduler.schedule(context, alarm);
            if (done != null) done.run();
        });
    }
    public void setActive(Alarm alarm, boolean active) {
        AppExecutors.DB.execute(() -> {
            alarm.isActive = active; dao.update(alarm);
            if (active) AlarmScheduler.schedule(context, alarm); else AlarmScheduler.cancel(context, alarm.id);
        });
    }
    public void disableSync(int id){Alarm alarm=dao.getById(id);if(alarm==null)return;alarm.isActive=false;dao.update(alarm);AlarmScheduler.cancel(context,id);}
    public void delete(Alarm alarm) {
        AppExecutors.DB.execute(() -> { AlarmScheduler.cancel(context, alarm.id); dao.delete(alarm); });
    }
    public void toggleSkipNext(Alarm alarm) {
        AppExecutors.DB.execute(() -> {
            AlarmScheduler.cancel(context, alarm.id);
            alarm.skipUntilMillis = alarm.skipUntilMillis > System.currentTimeMillis()
                    ? 0 : AlarmScheduler.nextTriggerIgnoringSkip(alarm);
            dao.update(alarm);
            if (alarm.isActive) AlarmScheduler.schedule(context, alarm);
        });
    }
    public void createQuickAlarm(int minutes, String label, boolean mathDismiss, Runnable done) {
        Alarm alarm=new Alarm(); alarm.isQuickAlarm=true; alarm.isActive=true; alarm.keepAfterDismiss=false;
        alarm.triggerAtMillis=System.currentTimeMillis()+minutes*60_000L; alarm.label=label;alarm.isMathDismiss=mathDismiss;alarm.mathDifficulty=1;
        Calendar trigger=Calendar.getInstance(); trigger.setTimeInMillis(alarm.triggerAtMillis);
        alarm.hour=trigger.get(Calendar.HOUR_OF_DAY); alarm.minute=trigger.get(Calendar.MINUTE);
        save(alarm,done);
    }
    public void snoozeSync(int id, int minutes) {
        Alarm alarm=dao.getById(id);
        long snoozeAt=System.currentTimeMillis()+minutes*60_000L;
        if(alarm!=null&&alarm.isQuickAlarm){alarm.triggerAtMillis=snoozeAt;Calendar trigger=Calendar.getInstance();trigger.setTimeInMillis(alarm.triggerAtMillis);alarm.hour=trigger.get(Calendar.HOUR_OF_DAY);alarm.minute=trigger.get(Calendar.MINUTE);dao.update(alarm);}
        AlarmScheduler.scheduleSnooze(context,id,minutes);
        if(alarm!=null)UpcomingNotificationManager.showSnoozedAt(context,alarm,snoozeAt);
    }
    public void rescheduleAll() {
        AppExecutors.DB.execute(() -> {
            long now=System.currentTimeMillis();
            for (Alarm alarm : dao.getActiveAlarms()) {
                if(alarm.isQuickAlarm&&alarm.triggerAtMillis<=now){alarm.isActive=false;dao.update(alarm);AlarmScheduler.cancel(context,alarm.id);}
                else AlarmScheduler.schedule(context, alarm);
            }
        });
    }
    public void finishOneTimeSync(int id) {
        Alarm alarm = dao.getById(id);
        if (alarm == null) return;
        alarm.skipUntilMillis = 0;
        if (!alarm.repeats() && !alarm.keepAfterDismiss) { alarm.isActive = false; dao.update(alarm); }
        else if (!alarm.repeats()) AlarmScheduler.schedule(context, alarm);
        else dao.update(alarm);
    }
    public void onTriggeredSync(int id) {
        Alarm alarm=dao.getById(id); if(alarm==null)return;
        alarm.skipUntilMillis=0; dao.update(alarm);
        if(alarm.isActive&&alarm.repeats())AlarmScheduler.schedule(context,alarm);
    }
}
