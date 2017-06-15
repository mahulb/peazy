package in.peazy.peazy;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.StringTokenizer;

public class FeedbackService extends Service {
    private static final String TAG="PeazyFeedbackService";
    public FeedbackService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "We are in feedback service");
        try {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Integer notificationId = extras.getInt("id");
                String intentAction = intent.getAction();
                StringTokenizer params = new StringTokenizer(intentAction, "|");
                String action = params.nextToken();
                String marker = extras.getString("marker");
                String date = extras.getString("time");
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Log.d(TAG, "Notification id is "+notificationId);
                Log.d(TAG, "Marker is "+marker);
                Log.d(TAG, "Time is "+date);
                notificationManager.cancel(notificationId);

                if (action.equals("NA")) {
                    Log.d(TAG, "The user wasn't looking for parking at all");
                }

                else if (action.equals("Yes")) {
                    Log.d(TAG, "Finding parking successful");
                    sendFeedback(marker, date, "Yes");
                }

                else if (action.equals("No")) {
                    Log.d(TAG, "Finding parking unsuccessful");
                    sendFeedback(marker, date, "No");
                }
            }

            else {
                Log.e(TAG, "No extras? How?");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Caught Exception: "+ex.toString());
        }
        return Service.START_NOT_STICKY;
    }

    public void sendFeedback (String marker, String time, String response){
        String url = "http://www.peazy.in/tatva/saveFeedback.php?marker=" +
                marker + "&time=" + time + "&response=" + response;

        new NetworkManager(FeedbackService.this,
                new SaveFeedback(FeedbackService.this))
                .execute(url);
    }
}
