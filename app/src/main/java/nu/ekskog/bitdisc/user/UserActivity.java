package nu.ekskog.bitdisc.user;

import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import nu.ekskog.bitdisc.AbstractBitdiscActivity;
import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.R;

public class UserActivity extends AbstractBitdiscActivity {
    private ArrayList<Entity> mUsers = new ArrayList<>();
    private UserArrayAdapter mUserAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        mUserAdapter = new UserArrayAdapter(this, R.layout.list_user_row, mUsers);
        mUserAdapter.setShowAvatar(true);

        ListView lv = (ListView)findViewById(R.id.user_list);
        registerForContextMenu(lv);
        lv.setAdapter(mUserAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String user = ((TextView)view.findViewById(R.id.user_id)).getText().toString();
                Intent i = new Intent(UserActivity.this, UserDetailActivity.class);
                i.putExtra(C.FIELD_ID, user);
                startActivity(i);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        fetchUsers();
    }

    @Override
    public void newCloudData(String type, Entity entity) {
        if(type.equals(C.TYPE_USER)) {
            fetchUsers();
            return;
        }
        super.newCloudData(type, entity);
    }

    private void fetchUsers() {
        mUsers.clear();
        mUsers.addAll(mDataStore.getUsers().values());
        mUserAdapter.notifyDataSetChanged();
    }
}
