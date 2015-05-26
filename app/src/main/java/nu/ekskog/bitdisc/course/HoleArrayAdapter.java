package nu.ekskog.bitdisc.course;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.R;

public class HoleArrayAdapter extends ArrayAdapter<Entity> {
    private Context mContext;
    private int mRes;
    private ArrayList<Entity> mHoles;

    public HoleArrayAdapter(Context context, int textViewResourceId, ArrayList<Entity> list) {
        super(context, textViewResourceId);
        mContext = context;
        mRes = textViewResourceId;
        mHoles = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        HoleHolder holder;

        if(row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(mRes, null);
            holder = new HoleHolder();
            holder.id = (TextView)row.findViewById(R.id.hole_id);
            holder.enumeration = (TextView)row.findViewById(R.id.hole_enum);
            holder.par = (TextView)row.findViewById(R.id.hole_par);
            holder.distance = (TextView)row.findViewById(R.id.hole_distance);
            row.setTag(holder);
        } else {
            holder = (HoleHolder)row.getTag();
        }

        Entity hole = mHoles.get(position);
        holder.id.setText((String)hole.get(C.FIELD_ID));
        holder.enumeration.setText(Integer.toString(position + 1));
        holder.par.setText("Par " + hole.get(C.FIELD_PAR) + " hole");
        holder.distance.setText(hole.get(C.FIELD_DISTANCE) + " meters");

        return row;
    }

    @Override
    public Entity getItem(int position)
    {
        return mHoles.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    public int getCount()
    {
        return mHoles.size();
    }

    public boolean isEnabled (int position)
    {
        return true;
    }

    static class HoleHolder {
        TextView id;
        TextView enumeration;
        TextView par;
        TextView distance;
    }
}