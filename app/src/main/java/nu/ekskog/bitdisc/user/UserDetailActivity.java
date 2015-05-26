package nu.ekskog.bitdisc.user;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import nu.ekskog.bitdisc.AbstractBitdiscActivity;
import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.R;
import nu.ekskog.bitdisc.course.CourseActivity;
import nu.ekskog.bitdisc.course.CourseEditActivity;


public class UserDetailActivity extends AbstractBitdiscActivity {
    private String mUser;

    private ArrayList<Entity> mFriends = new ArrayList<>();
    private UserArrayAdapter mFriendAdapter;

    private LinearLayout mStatsList;
    private LinearLayout mFriendList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        mStatsList = (LinearLayout) findViewById(R.id.stats_list);
        mFriendList = (LinearLayout) findViewById(R.id.friend_list);

        Intent intent = getIntent();
        if(intent.hasExtra(C.FIELD_ID))
            mUser = intent.getStringExtra(C.FIELD_ID);
        else if(savedInstanceState != null)
            mUser = savedInstanceState.getString(C.FIELD_ID);
        else
            finish();

        mFriendAdapter = new UserArrayAdapter(this, R.layout.list_user_row, mFriends);
        mFriendAdapter.setShowAvatar(true);

        if(mServiceBound)
            loadData();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(C.FIELD_ID, mUser);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case android.R.id.home:
                i = new Intent(UserDetailActivity.this, UserActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        loadData();
    }

    @Override
    public void newCloudData(String type, Entity entity) {
        switch (type) {
            case C.TYPE_USER:
                if(entity == null) break;
                String u = (String) entity.get(C.FIELD_ID);
                if (u.equals(mUser)) {
                    loadData();
                    break;
                }
                Entity user = mDataStore.getUsers().get(mUser);
                if(user.has(C.FIELD_FRIENDS)) {
                    ArrayList<String> friends = (ArrayList<String>) user.get(C.FIELD_FRIENDS);
                    for(String f : friends)
                        if(f.equals(u)) {
                            loadData();
                            break;
                        }
                }
                break;
            default:
                super.newCloudData(type, entity);
                break;
        }
    }

    private void loadData() {
        Log.d(C.TAG, "loadData");
        if(!checkService()) return;

        Entity user = mDataStore.getUsers().get(mUser);
        if(user == null) return;

        ActionBar ab = getActionBar();
        if(ab != null)
            ab.setTitle((String) user.get(C.FIELD_NAME));

        mFriends.clear();
        if(user.has(C.FIELD_FRIENDS)) {
            ArrayList<String> friends = (ArrayList<String>) user.get(C.FIELD_FRIENDS);
            for(String f : friends)
                mFriends.add(mDataStore.getUsers().get(f));
            Log.d(C.TAG, "loaded friends: " + mFriends.size());
        }
        makeFriendList();
        makeStatsList();
    }

    private void makeStatsList() {
        int numGames = 0;
        int numThrows = 0;
        ArrayList<Entity> subgames = new ArrayList<>(mDataStore.getSubgames().values());
        for(Entity subgame : subgames) {
            if(mUser.equals(subgame.get(C.FIELD_USER))) {
                numGames++;
                ArrayList<Long> splits = (ArrayList<Long>) subgame.get(C.FIELD_SPLITS);
                for(Long s : splits)
                    numThrows += s.intValue();
            }
        }

        mStatsList.removeAllViews();

        TextView txtGames = new TextView(this);
        txtGames.setTextSize(24);
        txtGames.setText(numGames + " games");
        mStatsList.addView(txtGames);

        TextView txtThrows = new TextView(this);
        txtThrows.setTextSize(24);
        txtThrows.setText(numThrows + " throws");
        mStatsList.addView(txtThrows);
    }

    private void makeFriendList() {
        final int length = mFriendAdapter.getCount();
        mFriendList.removeAllViews();
        for(int i = 0; i < length; ++i) {
            View v = mFriendAdapter.getView(i, null, null);
            mFriendList.addView(v);
        }
    }
}
