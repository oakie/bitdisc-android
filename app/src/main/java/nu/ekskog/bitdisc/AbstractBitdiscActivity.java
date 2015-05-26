package nu.ekskog.bitdisc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;

import java.util.Arrays;

public class AbstractBitdiscActivity extends Activity implements IBitdiscListener, ServiceConnection {
    protected BitdiscService mDataStore;
    protected boolean mServiceBound = false;

    private CallbackManager mCallbackManager = CallbackManager.Factory.create();
    private AccessTokenTracker mFacebookTokenTracker;
    private String mFacebookToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Attach to background service
        Intent intent = new Intent(this, BitdiscService.class);
        getApplicationContext().startService(intent);
        getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE);

        // Authenticate to facebook
        if(AccessToken.getCurrentAccessToken() != null)
            mFacebookToken = AccessToken.getCurrentAccessToken().getToken();
        mFacebookTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldToken, AccessToken newToken) {
                if(newToken != null && !newToken.isExpired()) {
                    mFacebookToken = newToken.getToken();
                    if(mServiceBound) {
                        Log.d(C.TAG, "Access token will be used for firebase auth.");
                        mDataStore.auth(mFacebookToken);
                    }
                } else {
                    mFacebookToken = "";
                    if(mServiceBound) {
                        Log.d(C.TAG, "Access token has expired.");
                        mDataStore.unauth();
                    }
                }
            }
        };
        if(mFacebookToken.equals("")) {
            LoginManager.getInstance().logInWithReadPermissions(this,
                    Arrays.asList("public_profile", "email"));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFacebookTokenTracker.stopTracking();
        if(mServiceBound)
            unsubscribe();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        Log.d(C.TAG, "service connected");
        mDataStore = ((BitdiscService.ModelServiceBinder) binder).getService();
        mServiceBound = true;
        subscribe();
        if(!mFacebookToken.equals(""))
            mDataStore.auth(mFacebookToken);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(C.TAG, "service disconnected");
        mServiceBound = false;
    }

    @Override
    public void newCloudData(String type, Entity entity) {
    }

    protected void subscribe() {
        Log.d(C.TAG, "subscribe");
        mDataStore.addListener(this);
    }

    protected void unsubscribe() {
        Log.d(C.TAG, "unsubscribe");
        mDataStore.removeListener(this);
    }

    protected boolean checkService() {
        if (!mServiceBound)
            Toast.makeText(this, "No service connected!", Toast.LENGTH_SHORT).show();
        return mServiceBound;
    }
}
