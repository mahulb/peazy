package in.peazy.peazy;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Places;

import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        com.google.android.gms.location.LocationListener {

    LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private static final int GOOGLE_API_CLIENT_ID=0;
    private static final int REQUEST_CHECK_SETTINGS = 1000;
    private static final int LOCATION_WAIT =  15000; //ms

    private static final String TAG="PeazyMainActivity";
    public static final String PREFS_NAME = "PeazyPrefsFile";
    private static final String[] TOPICS = {"global"};

    private Location mLastLocation;

    private boolean isLocationSetup = false;
    private boolean isActivityFired = false;
    private boolean isEulaAccepted = true;

    private Tracker mTracker;

    private PendingIntent pi;
    private AlarmManager am;

    Handler handler = new Handler();
    Runnable proceedWithoutLocation = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "We got called");
            fireActivity(mLastLocation);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        validatePermissions();

        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        //Check if this is first time use
        String eulaAccepted = settings.getString("eula", null);
        if (eulaAccepted == null) {
            isEulaAccepted = false;
            //start a dialog
            showDialog();
        }

        setAlarm();

        String token = settings.getString("token", null);
        if (token == null) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        handler.removeCallbacks(proceedWithoutLocation);
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isLocationSetup)
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resume called");
        Log.i(TAG, "Setting screen name: MainActivity");
        mTracker.setScreenName("MainActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        setupLocation();
    }

    protected void setupLocation() {

        if (!isEulaAccepted)
            return;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        Log.d(TAG, "Location enabled");
                        registerForLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        Log.e(TAG, "Location not enabled");
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        Log.e(TAG, "Location not enabled");
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(intent);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        Log.d(TAG, "User hit okay");
                        registerForLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Log.d(TAG, "User hit cancel");
                        setupLocation();
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Google Places API connected.");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d(TAG, "Found a last location");
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());

        Toast.makeText(this,
                "Google Places API connection failed with error code:" +
                        connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Google Places API connection suspended.");
    }

    private void registerForLocationUpdates() {
        //Setup a timeout
        handler.postDelayed(proceedWithoutLocation, LOCATION_WAIT);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        isLocationSetup = true;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location is " + location.toString());
        fireActivity(location);
    }

    private void fireActivity(Location location) {

        if (!isActivityFired) {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (location != null) {
                intent.putExtra("latitude", location.getLatitude());
                intent.putExtra("longitude", location.getLongitude());
            }
            startActivity(intent);
            isActivityFired = true;
        }
        finish();
    }

    private void showDialog() {
        try {
            //We will display the dialog
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.eula_dialog);

            final TextView link = (TextView) dialog.findViewById(R.id.link);
            link.setMovementMethod(LinkMovementMethod.getInstance());
            final View linkParent = (View)link.getParent();
            linkParent.post(new Runnable() {
                // Post in the parent's message queue to make sure the parent
                // lays out its children before we call getHitRect()
                public void run() {
                    final Rect r = new Rect();
                    link.getHitRect(r);
                    r.top -= 16;
                    r.bottom += 16;
                    linkParent.setTouchDelegate(new TouchDelegate(r, link));
                }
            });

            final TextView ok = (TextView) dialog.findViewById(R.id.ok);
            final View parent = (View) ok.getParent();
            parent.post(new Runnable() {
                // Post in the parent's message queue to make sure the parent
                // lays out its children before we call getHitRect()
                public void run() {
                    final Rect r = new Rect();
                    ok.getHitRect(r);
                    r.top -= 16;
                    r.bottom += 16;
                    parent.setTouchDelegate(new TouchDelegate(r, ok));
                }
            });
            ok.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "ok was clicked");

                    //persist the usage
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("eula", "yes");
                    editor.commit();

                    isEulaAccepted = true;
                    dialog.cancel();
                    setupLocation();
                    return false;
                }
            });

            dialog.show();
        } catch (Exception ex) {
            Log.e(TAG, "Caught Exception: " + ex.toString());
        }
    }

    public void setAlarm() {
        // Retrieve a PendingIntent that will perform a broadcast
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        pi = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

        am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
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

    private void validatePermissions() {
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            //Usage stats permission check
            try {
                AppOpsManager appOps = (AppOpsManager) this
                        .getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                        android.os.Process.myUid(), this.getPackageName());
                boolean granted = mode == AppOpsManager.MODE_ALLOWED;
                if (!granted) {
                    //request a grant
                    Log.d(TAG, "We do not have usage stats permissions");
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                }
                else
                    Log.d(TAG, "Yahoo! We have usage stats permissions");
            } catch (Exception ex) {
                Log.e(TAG, "Caught exception : "+ex.toString());
            }
        }
    }
}
