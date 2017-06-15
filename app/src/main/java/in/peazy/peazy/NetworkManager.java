package in.peazy.peazy;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by MB on 12/3/2015.
 */
public class NetworkManager extends AsyncTask<String, Void, String> {
    private final String TAG = "CobollNetworkManager";
    NetworkInterface caller;
    Context context;

    public NetworkManager(Context context, NetworkInterface caller) {
        this.caller = caller;
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        Integer result = 0;
        String response = "";
        try {
            URL url = new URL(params[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("GET");
            int statusCode = urlConnection.getResponseCode();

                // 200 represents HTTP OK
            if (statusCode == 200) {
                inputStream = new BufferedInputStream(urlConnection.getInputStream());
                response = convertInputStreamToString(inputStream);
                result = 1; // Successful
            } else {
                result = 0; //"Failed to fetch data!";
            }
            Log.d(TAG, "Result is " + result);
        } catch (Exception e) {
            Log.d(TAG, "Exception caught " + e.getLocalizedMessage());
        }
        return response; //"Failed to fetch data!";
    }

    @Override
    protected void onPostExecute(String data) {
        try {
            JSONObject res = new JSONObject(data);
            Integer response = res.getInt("response");
            if (response != 0) {
                Log.e(TAG, "Failed:" + res.getString("message"));
                Toast.makeText(context, "Failed:" + res.getString("message"), Toast.LENGTH_SHORT).show();
            }
        }catch (Exception ex){
            Log.e(TAG, "Caught Exception: " + ex.toString());
        }
        /* Download complete. Lets update UI */
        caller.updateUI(data);
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {

        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null){
            result += line;
        }
            /* Close Stream */
        if(null!=inputStream){
            inputStream.close();
        }
        return result;
    }
}
