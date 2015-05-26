package nu.ekskog.bitdisc.user;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Map;

import nu.ekskog.bitdisc.AbstractBitdiscActivity;
import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.R;


public class UserDetailActivity extends AbstractBitdiscActivity {
    private String mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        Intent intent = getIntent();
        if(intent.hasExtra(C.FIELD_ID))
            mUser = intent.getStringExtra(C.FIELD_ID);
        else if(savedInstanceState != null)
            mUser = savedInstanceState.getString(C.FIELD_ID);
        else
            finish();
        populateFields();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
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
        switch(item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        populateFields();
    }

    private void populateFields() {
        if(!mServiceBound)
            return;
        Map<String, Entity> users = mDataStore.getUsers();
        Entity user = users.get(mUser);

        ActionBar ab = getActionBar();
        if(ab != null)
            ab.setTitle((String)user.get(C.FIELD_NAME));
    }
}
