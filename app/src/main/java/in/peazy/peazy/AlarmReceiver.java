package in.peazy.peazy;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.net.URLEncoder;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "PeazyAlarmReceiver";
    public static final String PREFS_NAME = "CobollPrefsFile";
    private PendingIntent pi;
    private AlarmManager am;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(TAG, "I am running");
        setAlarm(context);
        String task = printForegroundTask(context);
        if (task.contains("com.google.android.apps.maps") || task.contains("com.mmi.maps")) {
            //This is where magic happens
            //Check for internet connection
            if (!checkInternetConnection(context)) {
                return;
            }

            popHead(context);
        }
        else {
           // Log.d(TAG, "Time to Get out!");
            context.stopService(new Intent(context, PeazyService.class));
        }
    }

    public static void popHead(Context context){
        //pop the chat head
        //Log.d(TAG,"Time to Go!");
        context.startService(new Intent(context, PeazyService.class));
    }

    public String printForegroundTask(Context context) {
        String currentApp = "NULL";
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            //Log.d(TAG, "We are in android L");
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000*1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            Log.d(TAG, "We are not in android L");
            ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
        }

        Log.e(TAG, "Current App in foreground is: " + currentApp);
        return currentApp;
    }

    private boolean checkInternetConnection(Context context) {
        //Do nothing
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (isConnected) {
            Log.d(TAG, "Internet is connected");
        }
        return isConnected;
    }

    public void setAlarm(Context context) {
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