package in.peazy.peazy;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.JsPromptResult;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import in.peazy.peazy.NetworkInterface;

/**
 * Created by MB on 9/3/2015.
 */
public class GetParkingLots implements NetworkInterface {
    private String TAG = "PeazyGetParkingLots";
    private GoogleMap mMap;
    private Context context;
    private boolean shouldDrawLot;
    private ArrayList<LatLng> parkingLots = new ArrayList<LatLng>();
    int[] colors = {
            Color.rgb(150, 150, 150), //light grey
            Color.rgb(100, 100, 100), //light grey
            Color.rgb(50, 50, 50), //dark grey
            Color.rgb(25, 25, 25),
            Color.rgb(0, 0, 0) //black
    };

    float[] startPoints = {
            0.2f, 0.4f, 0.6f, 0.8f, 1f
    };

    public GetParkingLots(GoogleMap aMap, boolean shouldDrawLot, Context context){
        this.mMap = aMap;
        this.context = context;
        this.shouldDrawLot = shouldDrawLot;
    }

    @Override
    public void updateUI(String data) {
        try {
            JSONObject result = new JSONObject(data);
            //Check the response code
            int responseCode = result.getInt("response");
            if (responseCode != 0) {
                //Something went wrong
                Log.d(TAG, "Server returned "+result.getString("message"));
                return;
            }

            if (result.getInt("count") == 0) {
                //We have no lots nearby
                try {
                    Log.d(TAG, "No more parking lots nearby");
                    //Toast.makeText(context, result.getString("msg"), Toast.LENGTH_SHORT).show();
                    return;
                }catch (Exception ex){
                    Log.e(TAG, "Caught Exception: "+ex.toString());
                }
            }

            //Clear the map
            parkingLots.clear();
            mMap.clear();

            //Now start adding markers if any
            JSONArray arr = result.getJSONArray("data");

            for (int i=0; i < arr.length(); i++){
                JSONObject rider = arr.getJSONObject(i);
                String name = rider.getString("roadName");
                String stretch = rider.getString("stretch");
                String type = rider.getString("type");
                String side = rider.getString("side");
                Double startLatitude = Double.parseDouble(rider.getString("startLat"));
                Double startLongitude = Double.parseDouble(rider.getString("startLon"));
                Double stopLatitude = Double.parseDouble(rider.getString("stopLat"));
                Double stopLongitude = Double.parseDouble(rider.getString("stopLon"));
                String startTime = rider.getString("startTime");
                String stopTime = rider.getString("stopTime");
                String availability = rider.getString("availability");
                LatLng pos = new LatLng((startLatitude + stopLatitude) / 2,
                        (startLongitude + stopLongitude) / 2);

                parkingLots.add(new LatLng(startLatitude, startLongitude));
                if (shouldDrawLot) {
                    //Draw the lot here itself
                    PolylineOptions lineOptions = new PolylineOptions()
                            .add(new LatLng(startLatitude, startLongitude))
                            .add(new LatLng(stopLatitude, stopLongitude))
                            .color(Color.argb(150, 145, 145, 145));
                    Polyline polyline = mMap.addPolyline(lineOptions);
                    String snippet = name + "|" + stretch + "|" + type + "|" + side + "|" + startTime + "|" + stopTime + "|" + availability;
                    BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_available);
                    if (availability.equals("NA"))
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_unknown);
                    else if (availability.equals("0-2%") || availability.equals("0-5%") || availability.equals("0-10%"))
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_full);
                    else if (availability.equals("10-20%"))
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_warning);
                    else
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_available);

                    mMap.addMarker(new MarkerOptions().position(pos).title(name).
                            icon(icon).
                            snippet(snippet));
                }
            }

            if (!shouldDrawLot) {
                //Draw a heat map
                Gradient gradient = new Gradient(colors, startPoints);

                // Create a heat map tile provider, passing it the latlngs of the police stations.
                HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                        .data(parkingLots)
                        .gradient(gradient)
                        .build();
                // Add a tile overlay to the map, using the heat map tile provider.
                TileOverlay mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
                mProvider.setOpacity(0.9);
                mOverlay.clearTileCache();
            }

        }catch (Exception e) {
            Log.d(TAG, "Caught : "+e.toString());
        }
    }
}
