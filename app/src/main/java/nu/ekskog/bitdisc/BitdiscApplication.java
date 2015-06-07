package nu.ekskog.bitdisc;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.firebase.client.Firebase;

public class BitdiscApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Firebase.setAndroidContext(this);
        Firebase.getDefaultConfig().setPersistenceEnabled(true);
        String url = getResources().getString(R.string.firebase_url);
        new Firebase(url).child(C.TYPE_COURSE).keepSynced(true);
        new Firebase(url).child(C.TYPE_HOLE).keepSynced(true);
        new Firebase(url).child(C.TYPE_USER).keepSynced(true);

        FacebookSdk.sdkInitialize(getApplicationContext());
    }
}
