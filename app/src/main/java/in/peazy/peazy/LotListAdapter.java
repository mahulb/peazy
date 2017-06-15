package in.peazy.peazy;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by MB on 4/29/2017.
 */
public class LotListAdapter extends ArrayAdapter<LotListObject> implements View.OnClickListener{

    private ArrayList<LotListObject> dataSet;
    Context mContext;
    String TAG="PeazyLotListAdapter";

    // View lookup cache
    private static class ViewHolder {
        TextView name;
        TextView stretch;
        TextView distance;
        TextView availability;
    }

    public LotListAdapter(ArrayList<LotListObject> data, Context context) {
        super(context, R.layout.row_item, data);
        this.dataSet = data;
        this.mContext=context;

    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        LotListObject lotList = (LotListObject)object;
        Log.d(TAG, "Lot Name " + lotList.getName());
    }

    private int lastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        LotListObject lotList = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_item, parent, false);
            viewHolder.name = (TextView) convertView.findViewById(R.id.name);
            viewHolder.stretch = (TextView) convertView.findViewById(R.id.stretch);
            viewHolder.distance = (TextView) convertView.findViewById(R.id.distance);
            viewHolder.availability = (TextView) convertView.findViewById(R.id.availability);

            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }

        viewHolder.name.setText(lotList.getName());
        viewHolder.stretch.setText(lotList.getStretch());
        viewHolder.distance.setText(lotList.getDistance());
        viewHolder.availability.setText(lotList.getAvailability());
        // Return the completed view to render on screen
        return convertView;
    }
}
