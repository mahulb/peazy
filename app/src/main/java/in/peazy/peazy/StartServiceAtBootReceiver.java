package in.peazy.peazy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by MB on 5/20/2017.
 */
public class StartServiceAtBootReceiver extends BroadcastReceiver{
    PendingIntent pi;
    AlarmManager am;
    Context context;
    private static final String TAG="PeazyBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Boot complete received");
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            this.context = context;
            setAlarm();
        }
    }

    public void setAlarm() {
        // Retrieve a PendingIntent that will perform a broadcast
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        pi = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

        am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        int interval = 5000;
        int windowLength = 1000;

        if(android.os.Build.VERSION.SDK_INT < 19) {
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, pi);
        }
        else {
            am.setWindow(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, windowLength, pi);
        }
        //Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
    }
}
