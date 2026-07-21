package com.example.samsung_alarm;

import static org.junit.Assert.assertTrue;
import com.example.samsung_alarm.service.AlarmScheduler;
import com.example.samsung_alarm.data.model.Alarm;
import org.junit.Test;
import java.util.Calendar;

public class AlarmSchedulerTest {
    @Test public void skipNextDailyAlarm_movesToFollowingDay() {
        Calendar soon=Calendar.getInstance(); soon.add(Calendar.MINUTE,5);
        Alarm alarm=new Alarm(); alarm.hour=soon.get(Calendar.HOUR_OF_DAY); alarm.minute=soon.get(Calendar.MINUTE);
        alarm.mon=alarm.tue=alarm.wed=alarm.thu=alarm.fri=alarm.sat=alarm.sun=true;

        long first=AlarmScheduler.nextTriggerIgnoringSkip(alarm);
        alarm.skipUntilMillis=first;
        long afterSkip=AlarmScheduler.nextTrigger(alarm);
        long difference=afterSkip-first;

        assertTrue("A daily skip should move roughly one day", difference>=20*60*60*1000L && difference<=28*60*60*1000L);
    }

    @Test public void inactiveSkipMarker_doesNotChangeNextTrigger() {
        Calendar soon=Calendar.getInstance(); soon.add(Calendar.MINUTE,5);
        Alarm alarm=new Alarm(); alarm.hour=soon.get(Calendar.HOUR_OF_DAY); alarm.minute=soon.get(Calendar.MINUTE);
        alarm.mon=alarm.tue=alarm.wed=alarm.thu=alarm.fri=alarm.sat=alarm.sun=true;
        alarm.skipUntilMillis=System.currentTimeMillis()-60_000L;
        assertTrue(Math.abs(AlarmScheduler.nextTrigger(alarm)-AlarmScheduler.nextTriggerIgnoringSkip(alarm))<1_000L);
    }
}
