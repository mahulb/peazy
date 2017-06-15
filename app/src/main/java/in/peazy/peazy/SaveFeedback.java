package in.peazy.peazy;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * Created by MB on 6/12/2017.
 */
public class SaveFeedback implements NetworkInterface{
    private String TAG = "PeazySaveFeedback";
    private Context context;
    private View icon;

    public SaveFeedback(Context context) {
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
        } catch (Exception ex) {
            Log.e(TAG, "Caught Exception: "+ex);
        }
    }
}
