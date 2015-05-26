package nu.ekskog.bitdisc.lists;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.models.Entity;
import nu.ekskog.bitdisc.R;

public class UserArrayAdapter extends ArrayAdapter<Entity> {
    private Context mContext;
    private int mRes;
    private ArrayList<Entity> mUsers;
    private boolean mRankField = false;
    private boolean mValueField = false;
    private boolean mAvatarField = false;

    public UserArrayAdapter(Context context, int textViewResourceId, ArrayList<Entity> list)
    {
        super(context, textViewResourceId);
        mContext = context;
        mRes = textViewResourceId;
        mUsers = list;
    }

    public void setShowRank(boolean visible) {
        mRankField = visible;
    }

    public void setShowValue(boolean visible) {
        mValueField = visible;
    }

    public void setShowAvatar(boolean visible) {
        mAvatarField = visible;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        UserHolder holder;

        if(row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(mRes, null);
            holder = new UserHolder();
            holder.id = (TextView)row.findViewById(R.id.user_id);
            holder.rank = (TextView)row.findViewById(R.id.user_rank);
            holder.name = (TextView)row.findViewById(R.id.user_name);
            holder.value = (TextView)row.findViewById(R.id.user_value);
            holder.avatar = (ImageView)row.findViewById(R.id.user_avatar);

            row.setTag(holder);
        } else {
            holder = (UserHolder)row.getTag();
        }

        holder.id.setText("");
        holder.rank.setText(String.valueOf(position + 1));
        holder.name.setText("");
        holder.value.setText("");
        holder.avatar.setImageResource(R.drawable.user);

        holder.rank.setVisibility(mRankField ? View.VISIBLE : View.GONE);
        holder.value.setVisibility(mValueField ? View.VISIBLE : View.GONE);
        holder.avatar.setVisibility(mAvatarField ? View.VISIBLE : View.GONE);

        Entity user = mUsers.get(position);
        if(user == null)
            return row;
        if(user.has(C.FIELD_ID))
            holder.id.setText((String) user.get(C.FIELD_ID));
        if(user.has(C.FIELD_NAME))
            holder.name.setText((String) user.get(C.FIELD_NAME));
        if(user.has(C.FIELD_AVATAR)) {
            String imgStr = (String) user.get(C.FIELD_AVATAR);
            byte[] buffer = Base64.decode(imgStr, 0);
            Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
            holder.avatar.setImageBitmap(bmp);
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
        return mUsers.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    public int getCount()
    {
        return mUsers.size();
    }

    public boolean isEnabled (int position)
    {
        return true;
    }

    static class UserHolder
    {
        TextView id;
        TextView rank;
        TextView name;
        TextView value;
        ImageView avatar;
    }
}