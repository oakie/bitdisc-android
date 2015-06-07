package nu.ekskog.bitdisc.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import com.cloudinary.Cloudinary;
import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ServerValue;
import com.firebase.client.ValueEventListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.models.Entity;
import nu.ekskog.bitdisc.R;

public class BitdiscService extends Service {
    private final IBinder mBinder = new ModelServiceBinder();

    private String mUser = "";

    private Set<IBitdiscListener> mListeners = new HashSet<>();
    private Firebase mRootRef;
    private Firebase mConnectionRef;
    private ChildEventListener mFirebaseDataListener;
    private ValueEventListener mFirebaseConnectionListener;

    private Cloudinary mCloudinary;

    private Map<String, Entity> mUsers = new HashMap<>();
    private Map<String, Entity> mCourses = new HashMap<>();
    private Map<String, Entity> mHoles = new HashMap<>();
    private Map<String, Entity> mGames = new HashMap<>();
    private Map<String, Entity> mSubgames = new HashMap<>();

    public BitdiscService() {
    }

    public class ModelServiceBinder extends Binder {
        public BitdiscService getService() {
            return BitdiscService.this;
        }
    }

    @Override
    public void onCreate(){
        super.onCreate();

        mRootRef = new Firebase(getResources().getString(R.string.firebase_url));
        mRootRef.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                setAuthStatus(authData);
            }
        });

        mFirebaseConnectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if(connected) {
                    Firebase me = mRootRef.child(C.TYPE_USER).child(mUser);

                    Firebase connectionsRef = me.child(C.FIELD_CONNECTIONS).push();
                    connectionsRef.setValue(Boolean.TRUE);
                    connectionsRef.onDisconnect().removeValue();

                    Firebase lastOnlineRef = me.child(C.FIELD_LAST_ONLINE);
                    lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        };
        mConnectionRef = mRootRef.child(".info/connected");
        mConnectionRef.addValueEventListener(mFirebaseConnectionListener);

        mFirebaseDataListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                handle(C.EVENT_ADD, dataSnapshot);
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                handle(C.EVENT_CHANGE, dataSnapshot);
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                handle(C.EVENT_REMOVE, dataSnapshot);
            }
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        };
        mRootRef.child(C.TYPE_USER).addChildEventListener(mFirebaseDataListener);
        mRootRef.child(C.TYPE_COURSE).addChildEventListener(mFirebaseDataListener);
        mRootRef.child(C.TYPE_HOLE).addChildEventListener(mFirebaseDataListener);
        mRootRef.child(C.TYPE_GAME).addChildEventListener(mFirebaseDataListener);
        mRootRef.child(C.TYPE_SUBGAME).addChildEventListener(mFirebaseDataListener);

        mCloudinary = new Cloudinary(getResources().getString(R.string.cloudinary_url));
    }

    @Override
    public void onDestroy() {
        Log.d(C.TAG, "destroying service");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void auth(String token) {
        if(!mUser.equals("")) return;
        Log.d(C.TAG, "service AUTH");
        mRootRef.authWithOAuthToken("facebook", token, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                Log.d(C.TAG, "User was authenticated with firebase.");
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.d(C.TAG, "User could not be authenticated with firebase.");
            }
        });
    }

    public void unauth() {
        if(mUser.equals("")) return;
        Log.d(C.TAG, "service UNAUTH");
        mRootRef.unauth();
    }

    private void setAuthStatus(AuthData authData) {
        if(authData != null) {
            Log.d(C.TAG, "setAuthStatus TRUE");
            Log.d(C.TAG, authData.toString());

            String id = (String)authData.getProviderData().get(C.FACEBOOK_ID);
            if(id != null) {
                mUser = id;
                Entity user = new Entity(C.TYPE_USER);
                user.put(C.FIELD_ID, id);
                user.put(C.FIELD_NAME, authData.getProviderData().get(C.FACEBOOK_NAME));
                user.put(C.FIELD_EMAIL, authData.getProviderData().get(C.FACEBOOK_EMAIL));

                updateEntity(user);

                for(IBitdiscListener l : mListeners)
                    l.newCloudData(C.TYPE_AUTH, user);

                String url = "https://graph.facebook.com/" + id + "/picture?width=60&height=60";
                new FetchAvatarTask(url).execute();
            } else {
                Log.e(C.TAG, "Auth data error!");
                mUser = "";
            }
        } else {
            Log.d(C.TAG, "setAuthStatus FALSE");
            mUser = "";
            for(IBitdiscListener l : mListeners)
                l.newCloudData(C.TYPE_UNAUTH, null);
        }
    }

    public void addListener(IBitdiscListener l) {
        mListeners.add(l);
    }

    public void removeListener(IBitdiscListener l) {
        mListeners.remove(l);
    }

    private void pushEntity(Entity entity) {
        Firebase f = mRootRef.child(entity.getType()).push();
        entity.put(C.FIELD_ID, f.getKey());
        entity.put(C.FIELD_TIMESTAMP, ServerValue.TIMESTAMP);
        f.setValue(entity.getProperties());
    }

    public void updateEntity(Entity entity) {
        Firebase f = mRootRef.child(entity.getType()).child((String) entity.get(C.FIELD_ID));
        entity.put(C.FIELD_TIMESTAMP, ServerValue.TIMESTAMP);
        f.updateChildren(entity.getProperties());
    }

    public void deleteEntity(Entity entity) {
        String id = (String) entity.get(C.FIELD_ID);
        mRootRef.child(entity.getType()).child(id).removeValue();
    }

    public void popFromEntity(Entity entity, String key) {
        String id = (String) entity.get(C.FIELD_ID);
        mRootRef.child(entity.getType()).child(id).child(key).removeValue();
    }

    public Entity getMe() {
        return mUsers.get(mUser);
    }

    public Map<String, Entity> getUsers() {
        return mUsers;
    }

    public Map<String, Entity> getGames() {
        return mGames;
    }

    public Map<String, Entity> getSubgames() {
        return mSubgames;
    }

    public Map<String, Entity> getCourses() {
        return mCourses;
    }

    public Map<String, Entity> getHoles() {
        return mHoles;
    }

    public Entity createHole() {
        Entity hole = new Entity(C.TYPE_HOLE);
        hole.put(C.FIELD_PAR, 0);
        hole.put(C.FIELD_DISTANCE, 0);
        pushEntity(hole);
        return hole;
    }

    public void setHoleImage(String h, Uri image) {
        Entity hole = getHoles().get(h);
        new UploadImageTask(hole, image).execute();
    }

    public void getHoleImage(String h, ImageView view) {
        Entity hole = getHoles().get(h);
        String url = (String) hole.get(C.FIELD_IMG_URL);
        new DownloadImageTask(view, url).execute();
    }

    public Entity createCourse() {
        Entity course = new Entity(C.TYPE_COURSE);
        pushEntity(course);
        return course;
    }

    public void setCourseImage(String c, Uri image) {
        Entity course = getCourses().get(c);
        new UploadImageTask(course, image).execute();
    }

    public void getCourseImage(String c, ImageView view) {
        Entity course = getCourses().get(c);
        String url = (String) course.get(C.FIELD_IMG_URL);
        new DownloadImageTask(view, url).execute();
    }

    public Entity createGame() {
        Entity game = new Entity(C.TYPE_GAME);
        pushEntity(game);
        return game;
    }

    public Entity createSubgame(String user) {
        Entity subgame = new Entity(C.TYPE_SUBGAME);
        subgame.put(C.FIELD_USER, user);
        pushEntity(subgame);
        return subgame;
    }

    public Entity createGuest(String name) {
        Entity guest = new Entity(C.TYPE_USER);
        guest.put(C.FIELD_NAME, name);
        guest.put(C.FIELD_IS_GUEST, true);
        pushEntity(guest);

        Entity oldUser = getMe();
        Entity newUser = new Entity(oldUser);

        ArrayList<String> newFriends = new ArrayList<>();
        if(oldUser.has(C.FIELD_FRIENDS)) {
            ArrayList<String> oldFriends = (ArrayList<String>) oldUser.get(C.FIELD_FRIENDS);
            for (String f : oldFriends)
                newFriends.add(f);
        }
        newFriends.add((String) guest.get(C.FIELD_ID));
        newUser.put(C.FIELD_FRIENDS, newFriends);
        updateEntity(newUser);

        return guest;
    }

    private void handle(String event, DataSnapshot data) {
        Map<String, Object> map = (Map<String, Object>) data.getValue();
        String type = data.getRef().getParent().getKey();
        Entity entity = new Entity(type);
        for(String key : map.keySet())
            entity.put(key, map.get(key));

        switch (event) {
            case C.EVENT_ADD:
                insert(type, entity);
                break;
            case C.EVENT_CHANGE:
                insert(type, entity);
                break;
            case C.EVENT_REMOVE:
                remove(type, entity);
                break;
        }
        for (IBitdiscListener l : mListeners)
            l.newCloudData(type, entity);
    }

    private void insert(String type, Entity entity) {
        switch (type) {
            case C.TYPE_USER:
                mUsers.put((String) entity.get(C.FIELD_ID), entity);
                break;
            case C.TYPE_COURSE:
                mCourses.put((String) entity.get(C.FIELD_ID), entity);
                break;
            case C.TYPE_HOLE:
                mHoles.put((String) entity.get(C.FIELD_ID), entity);
                break;
            case C.TYPE_GAME:
                mGames.put((String) entity.get(C.FIELD_ID), entity);
                break;
            case C.TYPE_SUBGAME:
                mSubgames.put((String) entity.get(C.FIELD_ID), entity);
                break;
        }
    }

    private void remove(String type, Entity entity) {
        String key = (String) entity.get(C.FIELD_ID);
        switch (type) {
            case C.TYPE_USER:
                mUsers.remove(key);
                break;
            case C.TYPE_COURSE:
                mCourses.remove(key);
                break;
            case C.TYPE_HOLE:
                mHoles.remove(key);
                break;
            case C.TYPE_GAME:
                mGames.remove(key);
                break;
            case C.TYPE_SUBGAME:
                mSubgames.remove(key);
                break;
        }
    }

    private class DownloadImageTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<ImageView> mView;
        private String mUrl;
        private Bitmap mBitmap;
        public DownloadImageTask(ImageView view, String url) {
            super();
            mView = new WeakReference<>(view);
            mUrl = url;
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                InputStream is = (InputStream) new URL(mUrl).getContent();
                mBitmap = BitmapFactory.decodeStream(is);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            if(mView != null && mBitmap != null) {
                final ImageView v = mView.get();
                if(v != null)
                    v.setImageBitmap(mBitmap);
            }
        }
    }

    private class UploadImageTask extends AsyncTask<Void, Void, Void> {
        private Entity mEntity;
        private Uri mImage;

        public UploadImageTask(Entity entity, Uri image) {
            super();
            mEntity = entity;
            mImage = image;
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bmp = BitmapFactory.decodeFile(mImage.getPath(), options);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
                Map result = mCloudinary.uploader().upload(is, null);

                Entity newEntity = new Entity(mEntity);

                newEntity.put(C.FIELD_IMG_URL, result.get("url"));
                updateEntity(newEntity);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class FetchAvatarTask extends AsyncTask<Void, Void, Void> {
        private String mUrl;
        public FetchAvatarTask(String url) { super(); mUrl = url; }
        @Override
        protected Void doInBackground(Void... params) {
            Entity oldUser = getMe();
            if(oldUser == null) return null;
            Log.d(C.TAG, "downloading avatar");

            int bytesRead;
            byte[] buffer = new byte[1024];
            try {
                InputStream is = (InputStream) new URL(mUrl).getContent();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                while((bytesRead = is.read(buffer)) != -1)
                    os.write(buffer, 0, bytesRead);
                Log.d(C.TAG, "img bytes: " + os.size());
                String imgData = Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);

                Entity newUser = new Entity(oldUser);

                newUser.put(C.FIELD_AVATAR, imgData); // TODO: upload to cloudinary instead?
                updateEntity(newUser);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
