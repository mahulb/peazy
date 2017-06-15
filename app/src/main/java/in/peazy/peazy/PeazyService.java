package in.peazy.peazy;

/**
 * Created by MB on 4/4/2017.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;


public class PeazyService extends Service implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        com.google.android.gms.location.LocationListener {

    private static final String TAG = "PeazyService";
    private WindowManager windowManager;
    private View icon;
    private View iconClose;

    LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private boolean isLocationSetup = false;
    private boolean isLaunched = false;
    private Location currentLocation = null;
    private int currentCount;
    private MySQLiteHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new MySQLiteHelper(this);
        mGoogleApiClient = new GoogleApiClient.Builder(PeazyService.this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
        setupLocation();
    }

    private void launchHead() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay()
                .getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        Log.d(TAG, "height is "+Integer.toString(height)+ " & width is "+Integer.toString(width));

        int count = 0;
        //Now call the webservice to get the count
        if (currentLocation != null) {
            //count = getListCountFromWebService();
            currentCount = count;
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        icon = inflater.inflate(R.layout.popup, null);
        TextView popCount = (TextView)icon.findViewById(R.id.count);
        if (count <= 0) {
            popCount.setText("?");
        }
        else if (count < 10) {
            popCount.setText(Integer.toString(count));
        }

        else {
            popCount.setText("10+");
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 10;
        params.y = 200;

        //this code is for dragging the chat head
        icon.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private float finalTouchX;
            private float finalTouchY;
            private float diff;
            private boolean closeFlag = false;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        displayCloseView();
                        return true;
                    case MotionEvent.ACTION_UP:
                        finalTouchX = event.getRawX();
                        finalTouchY = event.getRawY();
                        if (closeFlag) {
                            removeCloseView();
                            windowManager.removeView(icon);
                            icon = null;
                            return true;
                        }
                        else {
                            removeCloseView();
                        }
                        diff = finalTouchY - initialTouchY;
                        //Log.d(TAG, "Diff is " + Float.toString(diff));
                        if (diff < 6 && diff > -6) {
                            // Now launch the maps, if we can
                            Intent intent = null;
                            if (currentLocation != null) {
                                intent = new Intent(v.getContext(), MapsActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("latitude", currentLocation.getLatitude());
                                intent.putExtra("longitude", currentLocation.getLongitude());
                            } else {
                                intent = new Intent(v.getContext(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }
                            startActivity(intent);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX
                                + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY
                                + (int) (event.getRawY() - initialTouchY);

                        //Log.d(TAG, "PY is "+params.y + " & Y is "+event.getRawY());
                        //Log.d(TAG, "PX is "+params.x + " & X is "+event.getRawX());

                        //is he trying to close?
                        if (params.y > height*0.6) {
                            params.x = (width - 65) / 2 + initialX - (int) initialTouchX;
                            params.y = height - 250;
                            closeFlag = true;
                        }

                        else {
                            closeFlag = false;
                        }

                        windowManager.updateViewLayout(icon, params);
                        return true;
                }
                return false;
            }
        });

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Now launch the maps, if we can
                Log.d(TAG, "Starting maps");

            }
        });

        windowManager.addView(icon, params);
    }

    public void displayCloseView(){

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        if (iconClose != null) {
            //Already displayed
            return;
        }

        iconClose = inflater.inflate(R.layout.killpopup, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.x = 0;
        params.y = 50;
        windowManager.addView(iconClose, params);
    }

    public void removeCloseView() {
        if (iconClose != null && iconClose.getParent() != null) {
            windowManager.removeView(iconClose);
            iconClose = null;
        }
        else {
            return;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying Service");
        super.onDestroy();
        if (isLocationSetup)
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        if (icon != null && icon.getParent() != null)
            windowManager.removeView(icon);
        if (iconClose != null && iconClose.getParent() != null)
            windowManager.removeView(iconClose);
    }

    protected void setupLocation() {
        Log.d(TAG, "Setting up location");
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
                    default:
                        // Location settings are not satisfied. Launch main activity
                        Log.e(TAG, "Location not enabled");
                        launchHead();
                        break;
                }
            }
        });
    }

    private void registerForLocationUpdates() {
        //Setup a timeout
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        isLocationSetup = true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Google Places API connected.");
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

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location is " + location.toString());
        LatLng curLocation = new LatLng(location.getLatitude(), location.getLongitude());
        ProcessLocationUpdates.processLocation(dbHelper, curLocation, getApplicationContext());
        currentLocation = location;
        if (!isLaunched) {
            launchHead();
            isLaunched = true;
        }
        else {
            updateCount();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    private void updateCount() {
        if (icon == null) {
            //The view has been removed; just return
            return;
        }
        String url = "http://www.peazy.in/tatva/listCount.php?lat=" +
                currentLocation.getLatitude() + "&lon=" +
                currentLocation.getLongitude();
        //url = "http://peazy.in/tatva/listCount.php?lat=22.547835&lon=88.358084";

        new NetworkManager(PeazyService.this,
                new GetLotCount(PeazyService.this, icon))
                .execute(url);
    }
}
