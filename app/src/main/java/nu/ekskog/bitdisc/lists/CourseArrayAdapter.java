package nu.ekskog.bitdisc.lists;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.models.Entity;
import nu.ekskog.bitdisc.R;

public class CourseArrayAdapter extends ArrayAdapter<Entity> {
    private Context mContext;
    private int mRes;
    private ArrayList<Entity> mCourses;
    private HashMap<String, Entity> mHoles;

    public CourseArrayAdapter(Context context, int resource, ArrayList<Entity> list,
                              HashMap<String, Entity> holes) {
        super(context, resource);
        mContext = context;
        mRes = resource;
        mCourses = list;
        mHoles = holes;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        CourseHolder holder;

        if(row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(mRes, null);
            holder = new CourseHolder();
            holder.id = (TextView) row.findViewById(R.id.course_id);
            holder.name = (TextView) row.findViewById(R.id.course_name);
            holder.holes = (TextView) row.findViewById(R.id.course_holes);
            holder.distance = (TextView) row.findViewById(R.id.course_distance);
            row.setTag(holder);
        } else {
            holder = (CourseHolder)row.getTag();
        }

        holder.id.setText("");
        holder.name.setText("");
        holder.holes.setText("0 holes");
        holder.distance.setText("0 meters");

        Entity course = mCourses.get(position);
        if(course == null)
            return row;
        if(course.has(C.FIELD_ID))
            holder.id.setText((String)course.get(C.FIELD_ID));
        if(course.has(C.FIELD_NAME))
            holder.name.setText((String)course.get(C.FIELD_NAME));
        if(course.has(C.FIELD_HOLES)) {
            int distSum = 0;
            ArrayList<String> holes = (ArrayList<String>) course.get(C.FIELD_HOLES);
            for(String h : holes) {
                if(mHoles.containsKey(h)) {
                    Entity hole = mHoles.get(h);
                    if(hole.has(C.FIELD_DISTANCE))
                        distSum += ((Long) hole.get(C.FIELD_DISTANCE)).intValue();
                }
            }
            holder.holes.setText(holes.size() + " holes");
            holder.distance.setText(String.valueOf(distSum) + " meters");
        }

        return row;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public Entity getItem(int position)
    {
        return mCourses.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    public int getCount()
    {
        return mCourses.size();
    }

    public boolean isEnabled (int position)
    {
        return true;
    }

    static class CourseHolder {
        TextView id;
        TextView name;
        TextView holes;
        TextView distance;
    }
}