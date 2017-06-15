package in.peazy.peazy;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by MB on 5/4/2017.
 */
public class GetLotList implements NetworkInterface {
    private String TAG = "PeazyListSearch";
    private Context context;
    ListView dialog_ListView;
    ArrayList<LotListObject> lotListObjects = new ArrayList<>();
    LotListAdapter lotListAdapter;

    public GetLotList(Context context){
        this.context = context;
    }

    @Override
    public void updateUI(String data) {
        try {
            JSONObject result = new JSONObject(data);
            //Check the response code
            int responseCode = result.getInt("response");
            if (responseCode != 0) {
                //Something went wrong
                Log.d(TAG, "Server returned " + result.getString("message"));
                return;
            }

            if (result.getInt("count") == 0) {
                //We have no lots nearby
                try {
                    Log.d(TAG, "No parking lots nearby");
                    Toast.makeText(context, result.getString("No parking lots nearby"), Toast.LENGTH_SHORT).show();
                    return;
                }catch (Exception ex){
                    Log.e(TAG, "Caught Exception: "+ex.toString());
                }
            }

            int count = result.getInt("count");

            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.lot_list);

            TextView title = (TextView)dialog.findViewById(R.id.title);
            title.setText(Integer.toString(count) + " lots found");

            dialog_ListView = (ListView)dialog.findViewById(R.id.dialoglist);
            lotListObjects = new ArrayList<>();

            ImageView close = (ImageView)dialog.findViewById(R.id.close);
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.cancel();
                }
            });
            dialog_ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    LotListObject listObject = lotListObjects.get(position);
                }
            });

            //Now start adding markers if any
            JSONArray arr = result.getJSONArray("data");

            for (int i=0; i < arr.length(); i++){
                JSONObject lot = arr.getJSONObject(i);
                String name = lot.getString("roadName");
                String stretch = lot.getString("stretch");
                Double startLatitude = Double.parseDouble(lot.getString("startLat"));
                Double startLongitude = Double.parseDouble(lot.getString("startLon"));
                Double stopLatitude = Double.parseDouble(lot.getString("stopLat"));
                Double stopLongitude = Double.parseDouble(lot.getString("stopLon"));
                LatLng pos = new LatLng((startLatitude + stopLatitude) / 2,
                        (startLongitude + stopLongitude) / 2);
                Double distance = Double.parseDouble(lot.getString("distance"));

                lotListObjects.add(new LotListObject(name, stretch, distance + " kms away","Unknown"));
                lotListAdapter = new LotListAdapter(lotListObjects,context);
                dialog_ListView.setAdapter(lotListAdapter);
                dialog_ListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        LotListObject lotListObject = lotListObjects.get(position);
                        Snackbar.make(view, lotListObject.getName(), Snackbar.LENGTH_LONG)
                                .setAction("No action", null).show();
                    }
                });
            }
            dialog.show();

        }catch (Exception e) {
            Log.d(TAG, "Caught : "+e.toString());
        }
    }
}
