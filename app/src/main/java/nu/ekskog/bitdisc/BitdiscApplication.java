package nu.ekskog.bitdisc;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.firebase.client.Firebase;

public class BitdiscApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
        Firebase.getDefaultConfig().enablePersistence();
        FacebookSdk.sdkInitialize(getApplicationContext());
    }
}
