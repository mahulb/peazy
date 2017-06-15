package in.peazy.peazy;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by MB on 6/10/2017.
 */
public class ProcessLocationUpdates {
    private static Long lastProcess = Long.getLong("0");
    private static Long lastCleaned = Long.getLong("0");
    private static final String TAG="PeazyProcessLocation";
    private static int notificationId = 1;
    public static void processLocation (MySQLiteHelper dbHelper,  LatLng latLng, Context context) {
        if (dbHelper == null) {
            return;
        }

        if (lastProcess != null && System.currentTimeMillis()-lastProcess < 300000) { //if we have already processed less than 5 mins ago
            Log.d(TAG, "Processed a location less than 5 mins ago");
            return;
        }
        lastProcess = System.currentTimeMillis();
        Log.d(TAG, "processing location");

        //Open up the database
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                MySQLiteHelper.COLUMN_TIMESTAMP,
                MySQLiteHelper.COLUMN_LAT,
                MySQLiteHelper.COLUMN_LON,
                MySQLiteHelper.COLUMN_MARKER
        };

        // Filter results WHERE "timestamp" > 'current_timestamp - 2 hours'
        String selection = MySQLiteHelper.COLUMN_TIMESTAMP + " > ?";
        String[] selectionArgs = { Long.toString(System.currentTimeMillis() - 3600000) };
        Log.d(TAG, selectionArgs.toString());

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                MySQLiteHelper.COLUMN_TIMESTAMP + " DESC";

        Cursor cursor = database.query(
                MySQLiteHelper.TABLE_LOCATIONS,           // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        while(cursor.moveToNext()) {
            String markerName = cursor.getString(cursor.getColumnIndexOrThrow(MySQLiteHelper.COLUMN_MARKER));
            Double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(MySQLiteHelper.COLUMN_LAT));
            Double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(MySQLiteHelper.COLUMN_LON));
            Long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(MySQLiteHelper.COLUMN_TIMESTAMP));
            float [] dist = new float[1];
            Location.distanceBetween(latitude, longitude, latLng.latitude, latLng.longitude, dist);
            Float distance = dist[0] * 0.000621371192f;
            Log.d(TAG, markerName + " : dist- " + Float.toString(distance));
            if (distance < 0.5) {
                //raise a notification
                sendNotification(context, markerName);
                //delete the entry (so we don't keep notifying)
                selection = MySQLiteHelper.COLUMN_MARKER + " = ?";
                String[] deletionArgs = { markerName };
                int count = database.delete(MySQLiteHelper.TABLE_LOCATIONS, selection, deletionArgs);
                Log.d(TAG, count + " rows deleted");
            }
        }
        cursor.close();

        if (lastCleaned == null || System.currentTimeMillis()-lastCleaned > 36000000) {
            //Cleanup old entries
            selection = MySQLiteHelper.COLUMN_TIMESTAMP + " < ?";
            selectionArgs = new String[]{ Long.toString(System.currentTimeMillis() - 3600000) };
            int count = database.delete(MySQLiteHelper.TABLE_LOCATIONS, selection, selectionArgs);
            Log.d(TAG, count + " rows deleted");
        }
    }

    public static void sendNotification(Context context, String marker) {

        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.notification);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        // Sets up the Snooze and Dismiss action buttons that will appear in the
        // big view of the notification.
        Intent successIntent = new Intent(context, FeedbackService.class);
        successIntent.setAction("Yes|" + notificationId);
        successIntent.putExtra("marker", marker);
        successIntent.putExtra("id", notificationId);
        successIntent.putExtra("time", formattedDate);
        PendingIntent piYes = PendingIntent.getService(context, 0, successIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent failureIntent = new Intent(context, FeedbackService.class);
        failureIntent.setAction("No|" + notificationId);
        failureIntent.putExtra("marker", marker);
        failureIntent.putExtra("id", notificationId);
        failureIntent.putExtra("time", formattedDate);
        PendingIntent piNo = PendingIntent.getService(context, 0, failureIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent naIntent = new Intent(context, FeedbackService.class);
        naIntent.setAction("NA|" + notificationId);
        naIntent.putExtra("marker", marker);
        naIntent.putExtra("id", notificationId);
        naIntent.putExtra("time", formattedDate);
        PendingIntent piNA = PendingIntent.getService(context, 0, naIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification));
        builder.setContentTitle("Were you able to park at " + marker + "?");
        builder.setContentText("You seem to have been looking for parking at " + marker + ". Did you find parking?");
        builder.setSubText("Help improve peazy predictions.");
        builder.setDefaults(Notification.DEFAULT_ALL);
        builder.setAutoCancel(true);
        // Moves the expanded layout object into the notification object.
        builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText("You seem to have been looking for parking at " + marker + ". Did you find parking?"))
                .addAction (R.drawable.action_search,
                        "Yes", piYes)
                .addAction(R.drawable.ic_close,
                        "No", piNo)
                .addAction(R.drawable.ic_close,
                        "Wasn't Looking", piNA);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Will display the notification in the notification bar
        notificationManager.notify(notificationId++, builder.build());
    }

}
