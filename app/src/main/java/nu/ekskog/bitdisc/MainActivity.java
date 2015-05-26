package nu.ekskog.bitdisc;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.facebook.login.LoginManager;

import java.util.Arrays;

import nu.ekskog.bitdisc.course.CourseActivity;
import nu.ekskog.bitdisc.game.GameActivity;
import nu.ekskog.bitdisc.user.UserActivity;

public class MainActivity extends AbstractBitdiscActivity {
    private TextView mTxtHeading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        setContentView(R.layout.activity_main);

        // Heading text
        mTxtHeading = (TextView) findViewById(R.id.txt_heading);

        // Buttons
        findViewById(R.id.btn_courses).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(C.TAG, "Courses");
                Intent i = new Intent(MainActivity.this, CourseActivity.class);
                startActivity(i);
            }
        });
        findViewById(R.id.btn_players).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(C.TAG, "Players");
                Intent i = new Intent(MainActivity.this, UserActivity.class);
                startActivity(i);
            }
        });
        findViewById(R.id.btn_games).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(C.TAG, "Games");
                Intent i = new Intent(MainActivity.this, GameActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_signout:
                LoginManager.getInstance().logOut();
                return true;
            case R.id.action_signin:
                LoginManager.getInstance().logInWithReadPermissions(MainActivity.this,
                        Arrays.asList("public_profile", "email"));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        setHeading();
    }

    @Override
    public void newCloudData(String type, Entity entity) {
        if(type.equals(C.TYPE_AUTH) || type.equals(C.TYPE_UNAUTH)) {
            setHeading();
            return;
        }
        super.newCloudData(type, entity);
    }

    private void setHeading() {
        if(!mServiceBound)
            return;
        Entity user = mDataStore.getMe();
        if(user != null)
            mTxtHeading.setText("Welcome, " + user.get("name") + "!");
        else
            mTxtHeading.setText("You are not signed in.");
    }
}
