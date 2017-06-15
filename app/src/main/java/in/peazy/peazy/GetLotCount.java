package in.peazy.peazy;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by MB on 5/5/2017.
 */
public class GetLotCount implements NetworkInterface{
    private String TAG = "PeazyListCount";
    private Context context;
    private View icon;

    public GetLotCount(Context context, View icon) {
        this.context = context;
        this.icon = icon;
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

            int count = result.getInt("value");
            Log.d(TAG, "Received count = "+Integer.toString(count));

            TextView popCount = (TextView) icon.findViewById(R.id.count);
            if (count <= 0) {
                popCount.setText("?");
            } else if (count < 10) {
                popCount.setText(Integer.toString(count));
            } else {
                popCount.setText("10+");
            }

        } catch (Exception ex) {
            Log.e(TAG, "Caught Exception: "+ ex.toString());
        }
    }
}