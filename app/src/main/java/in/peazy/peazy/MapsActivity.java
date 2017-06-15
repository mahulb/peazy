package in.peazy.peazy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private Toolbar toolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private String TAG="PeazyMapsActivity";
    public static final String PREFS_NAME = "PeazyPrefsFile";
    public static final String PREFS_LOC = "in.peazy.peazyLocationFile";
    private boolean isLotDrawn = false;

    private AutoCompleteTextView mAutocompleteTextView;
    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;
    private static final int GOOGLE_API_CLIENT_ID=0;
    private static final LatLngBounds BOUNDS_INDIA = new LatLngBounds(
            new LatLng(23.693920, 68.184697), new LatLng(28.204705, 97.301061));
    private static final LatLngBounds BOUNDS_HYDERABAD = new LatLngBounds(
            new LatLng(17.177759, 78.207016), new LatLng(17.614140, 78.648148));
    private List<Integer> filters = new ArrayList<Integer>();
    private AutocompleteFilter mAutocompleteFilter;

    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;

    LocationRequest mLocationRequest;
    private static final int REQUEST_CHECK_SETTINGS = 1000;
    private boolean isLocationSetup = false;
    private LatLng currentLocation = null;
    private LatLng lastSearchedLocation = null;
    private boolean shouldCenterOnCurrentLocation = false;
    private boolean shouldCallServer = true;
    Marker currentLocationMarker = null;

    private Tracker mTracker;
    private String uuid;

    ListView dialog_ListView;
    ArrayList<LotListObject> lotListObjects = new ArrayList<>();
    LotListAdapter lotListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        dbHelper = new MySQLiteHelper(this);
        database = dbHelper.getWritableDatabase();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        uuid = settings.getString("uuid", null);
        try {
            if (savedInstanceState == null) {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    currentLocation = new LatLng(extras.getDouble("latitude"), extras.getDouble("longitude"));
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Caught Exception: "+ex.toString());
        }
        setContentView(R.layout.activity_maps);

        mGoogleApiClient = new GoogleApiClient.Builder(MapsActivity.this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();

        ImageView moveToCurrentLocation = (ImageView) findViewById(R.id.myLoc);
        moveToCurrentLocation.bringToFront();
        moveToCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centerOnMap();
                shouldCenterOnCurrentLocation = true;
            }
        });

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //setupLocation();

        // Find our drawer view
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,toolbar,
                R.string.drawer_open,
                R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getActionBar().setTitle(mTitle);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getActionBar().setTitle(mDrawerTitle);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle.syncState();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        final TextView selection = (TextView) toolbar.findViewById(R.id.selection);
        selection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selection.setText("");
                selection.setVisibility(View.INVISIBLE);
                AutoCompleteTextView autocompleteTextView = (AutoCompleteTextView)toolbar.findViewById(R.id.destination);
                autocompleteTextView.setText("");
                autocompleteTextView.setVisibility(View.VISIBLE);

            }
        });

        mAutocompleteTextView = (AutoCompleteTextView)toolbar.findViewById(R.id.destination);
        mAutocompleteTextView.setThreshold(1);
        mAutocompleteTextView.setDropDownBackgroundDrawable(new ColorDrawable(this.getResources().getColor(R.color.black)));
        mAutocompleteTextView.setOnItemClickListener(mAutocompleteClickListener);
        //filters.add(Place.TYPE_LOCALITY);
        mAutocompleteFilter.create(filters);
        mPlaceArrayAdapter = new PlaceArrayAdapter(this, R.layout.dropdown_list_item,
                BOUNDS_INDIA, mAutocompleteFilter);
        mAutocompleteTextView.setAdapter(mPlaceArrayAdapter);

        try {
            displayLotList();
        } catch (Exception ex ){
            //No idea how it could crash here, but apparently it does
            Log.e(TAG, "Caught Exception: "+ex.toString());
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "Setting screen name: MapsActivity");
        mTracker.setScreenName("MapsActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        setupLocation();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (isLocationSetup && mGoogleApiClient.isConnected())
                LocationServices.FusedLocationApi.removeLocationUpdates(
                        mGoogleApiClient, this);
        } catch (Exception ex) {
            Log.e(TAG, "Exception Caught: "+ex.toString());
        }
    }

    @Override
    public void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Make sure this is the method with just `Bundle` as the signature
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        // Add a marker in kolkata and move the camera
        LatLng kolkata = new LatLng(22.549124, 88.357290);
        if (currentLocation == null)
            currentLocation = kolkata;

        centerOnMap();
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                Log.d(TAG, "Camera position changed");
                //drawParkingLots();
                shouldCenterOnCurrentLocation = false;
            }
        });
        drawParkingLots(currentLocation);
    }

    public void drawParkingLots(LatLng location){

        LatLngBounds curScreen = mMap.getProjection()
                .getVisibleRegion().latLngBounds;
        Boolean shouldDrawLot = true;

        Log.d(TAG, curScreen.toString());

        float zoom = mMap.getCameraPosition().zoom;
        Log.d(TAG, "Zoom is "+zoom);

        //if (!shouldDrawLot.equals(isLotDrawn)) {
        if (shouldDrawLot && shouldCallServer) {
            String url = "http://www.peazy.in/tatva/citySearch.php?lat=" +
                    location.latitude + "&lon=" +
                    location.longitude;

            if (lastSearchedLocation != null) {
                url = url + "&prevLat=" + lastSearchedLocation.latitude +
                        "&prevLon=" + lastSearchedLocation.longitude;
            }

            Log.d(TAG, "Url is "+ url);

            new NetworkManager(MapsActivity.this,
                    new GetParkingLots(mMap, shouldDrawLot, MapsActivity.this))
                    .execute(url);

            //Assuming no error happened
            isLotDrawn = shouldDrawLot;
            lastSearchedLocation = location;
            shouldCallServer = false;
        }
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                Log.i(TAG, "Selected: " + item.description);
                //Set to call server
                shouldCallServer = true;

                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Place search")
                        .setAction(item.description.toString())
                        .setLabel(uuid)
                        .setValue(1)
                        .build());

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
                Log.i(TAG, "Fetching details for ID: " + item.placeId);

                //try some ui
                TextView selection = (TextView) findViewById(R.id.selection);
                selection.setText(item.description);
                selection.setVisibility(View.VISIBLE);

                AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView)findViewById(R.id.destination);
                autoCompleteTextView.setVisibility(View.INVISIBLE);
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                RelativeLayout mainLayout = (RelativeLayout)findViewById(R.id.mapsLayout);
                inputManager.hideSoftInputFromWindow(mainLayout.getWindowToken(), 0);

            } catch (Exception ex) {
                Log.e(TAG, "Caught: " + ex.toString());
            }
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e(TAG, "Place query did not complete. Error: " +
                        places.getStatus().toString());
                return;
            }
            else if (places.getCount() > 0) {
                final Place place = places.get(0);
                shouldCenterOnCurrentLocation = false;

                //Update the map
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 18));
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(place.getLatLng())      // Sets the center of the map to Mountain View
                        .zoom(17)                   // Sets the zoom
                        .bearing(0)                // Sets the orientation of the camera to east
                        .tilt(70)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                drawParkingLots(place.getLatLng());
            }
        }
    };

    @Override
    public void onConnected(Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(TAG, "Google Places API connected.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());

        Toast.makeText(this,
                "Google Places API connection failed with error code:" +
                        connectionResult.getErrorCode(),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mPlaceArrayAdapter.setGoogleApiClient(null);
        Log.e(TAG, "Google Places API connection suspended.");
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {

        Log.d(TAG, "Marker has been clicked");
        LatLng markerLocation = marker.getPosition();

        try {
            //get the marker details
            String snippet = marker.getSnippet();
            StringTokenizer params = new StringTokenizer(snippet, "|");
            final String name = params.nextToken();
            final String stretch = params.nextToken();
            final String type = params.nextToken(); //full destination
            final String side = params.nextToken();
            final String startTime = params.nextToken();
            final String stopTime = params.nextToken();
            final String availability = params.nextToken();

            //Try to save he tried to search here
            ContentValues values = new ContentValues();
            String time = ((Long)System.currentTimeMillis()).toString();
            values.put(MySQLiteHelper.COLUMN_TIMESTAMP, time);
            values.put(MySQLiteHelper.COLUMN_LAT, Double.toString(markerLocation.latitude));
            values.put(MySQLiteHelper.COLUMN_LON, Double.toString(markerLocation.longitude));
            values.put(MySQLiteHelper.COLUMN_MARKER, name);
            long rowId = database.insert(MySQLiteHelper.TABLE_LOCATIONS, null, values);

            Log.d(TAG, "Inserted row Id: "+Long.toString(rowId));

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Marker Click")
                    .setAction(name)
                    .setLabel(uuid)
                    .setValue(1)
                    .build());

            //We will display the dialog
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog);

            TextView title = (TextView) dialog.findViewById(R.id.title);
            title.setText(name);

            TextView stretchText = (TextView) dialog.findViewById(R.id.stretchText);
            stretchText.setText(stretch);

            TextView sideText = (TextView) dialog.findViewById(R.id.directionText);
            sideText.setText(side);

            TextView typeText = (TextView) dialog.findViewById(R.id.typeText);
            typeText.setText(type);

            TextView timingText = (TextView) dialog.findViewById(R.id.timeText);
            timingText.setText(startTime + " to " + stopTime);

            TextView availabilityText = (TextView) dialog.findViewById(R.id.availabilityText);
            availabilityText.setText(availability);

            final TextView ok = (TextView) dialog.findViewById(R.id.ok);
            final View parent = (View)ok.getParent();
            parent.post( new Runnable() {
                // Post in the parent's message queue to make sure the parent
                // lays out its children before we call getHitRect()
                public void run() {
                    final Rect r = new Rect();
                    ok.getHitRect(r);
                    r.top -= 6;
                    r.bottom += 6;
                    r.bottom += 6;
                    parent.setTouchDelegate( new TouchDelegate( r , ok));
                }
            });
            ok.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "ok was clicked");
                    dialog.cancel();
                    return false;
                }
            });

            dialog.show();
        } catch (Exception ex) {
            Log.e(TAG, "Caught Exception: "+ex.toString());
        }
        return true;
    }

    protected void setupLocation() {

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
                                    MapsActivity.this,
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

    private void registerForLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        isLocationSetup = true;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location is " + location.toString());
        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        if (shouldCenterOnCurrentLocation) {
            centerOnMap();
        }
        ProcessLocationUpdates.processLocation(dbHelper,currentLocation, getApplicationContext());
    }

    private void centerOnMap(){
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(currentLocation)      // Sets the center of the map to current location
                .zoom(17)                   // Sets the zoom
                .bearing(0)                // Sets the orientation of the camera to east
                .tilt(70)
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_location_blue);
        if (currentLocationMarker != null)
            currentLocationMarker.remove();
        currentLocationMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("You").
                icon(icon));
        shouldCallServer = true;
        drawParkingLots(currentLocation);

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Map centered")
                .setAction(currentLocation.toString())
                .setLabel(uuid)
                .setValue(1)
                .build());
    }

    private void displayLotList() {
        String url = "http://www.peazy.in/tatva/listSearch.php?lat=" +
                currentLocation.latitude + "&lon=" +
                currentLocation.longitude;

        //url = "http://peazy.in/tatva/listSearch.php?lat=22.547835&lon=88.358084";

        Log.d(TAG, "Url is "+ url);

        new NetworkManager(MapsActivity.this,
                new GetLotList(MapsActivity.this))
                .execute(url);
    }
}
