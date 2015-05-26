package nu.ekskog.bitdisc.game;

import android.app.Activity;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.R;

public class GameArrayAdapter extends ArrayAdapter<Entity> {
    private Context context;
    private int res;
    private ArrayList<Entity> games;
    private HashMap<String, Entity> courses;

    public GameArrayAdapter(Context context, int textViewResourceId, ArrayList<Entity> games, HashMap<String, Entity> courses)
    {
        super(context, textViewResourceId);
        this.context = context;
        this.res = textViewResourceId;
        this.games = games;
        this.courses = courses;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        GameHolder holder;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(res, null);
            holder = new GameHolder();
            holder.id = (TextView)row.findViewById(R.id.game_id);
            holder.course = (TextView)row.findViewById(R.id.game_course);
            holder.timestamp = (TextView)row.findViewById(R.id.game_time);
            holder.players = (TextView)row.findViewById(R.id.game_players);
            row.setTag(holder);
        } else {
            holder = (GameHolder)row.getTag();
        }

        Entity game = games.get(position);
        holder.id.setText((String)game.get(C.FIELD_ID));

        holder.course.setText("-");
        String c = (String)game.get(C.FIELD_COURSE);
        Entity course = courses.get(c);
        if(course != null)
            holder.course.setText((String)course.get(C.FIELD_NAME));

        SimpleDateFormat dt = new SimpleDateFormat(C.FORMAT_DATE_TIME);
        if(game.get(C.FIELD_END) != null)
            holder.timestamp.setText(dt.format(new Date((Long)game.get(C.FIELD_END))));
        else if(game.get(C.FIELD_START) != null)
            holder.timestamp.setText("IN PROGRESS");
        else
            holder.timestamp.setText("NOT STARTED");

        holder.players.setText("no players");
        if(game.get(C.FIELD_SUBGAMES) != null) {
            ArrayList<String> subs = (ArrayList<String>) game.get(C.FIELD_SUBGAMES);
            holder.players.setText(subs.size() + " players");
            if(subs.size() == 1)
                holder.players.setText("1 player");
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
        return games.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    public int getCount()
    {
        return games.size();
    }

    public boolean isEnabled (int position)
    {
        return true;
    }

    static class GameHolder
    {
        TextView id;
        TextView course;
        TextView timestamp;
        TextView players;
    }
}