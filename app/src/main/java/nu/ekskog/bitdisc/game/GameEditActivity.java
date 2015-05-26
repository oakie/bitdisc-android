package nu.ekskog.bitdisc.game;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import nu.ekskog.bitdisc.AbstractBitdiscActivity;
import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.ItemListListener;
import nu.ekskog.bitdisc.R;
import nu.ekskog.bitdisc.user.UserArrayAdapter;
import nu.ekskog.bitdisc.course.CourseArrayAdapter;

public class GameEditActivity extends AbstractBitdiscActivity {
    private String mGame;

    private ArrayList<Entity> mCourses = new ArrayList<>();
    private HashMap<String, Entity> mHoles = new HashMap<>();
    private ArrayList<Entity> mUsers = new ArrayList<>();
    private ArrayList<Entity> mGuests = new ArrayList<>();
    private CourseArrayAdapter mCourseAdapter;
    private UserArrayAdapter mUserAdapter;
    private UserArrayAdapter mGuestAdapter;

    private Spinner mCourseSpinner;
    private Dialog mUserDialog;
    private Dialog mGuestDialog;
    private LinearLayout mPlayerList;

    private Entity mCourse;
    private ArrayList<Entity> mSubgames = new ArrayList<>();
    private UserArrayAdapter mSubgameAdapter;
    private ItemListListener mListListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_edit);

        mCourseSpinner = (Spinner) findViewById(R.id.spinner);
        Button btnAddPlayer = (Button) findViewById(R.id.btn_add_player);
        Button btnAddGuest = (Button) findViewById(R.id.btn_add_guest);
        mPlayerList = (LinearLayout) findViewById(R.id.player_list);

        Intent intent = getIntent();
        if(intent.hasExtra(C.FIELD_ID))
            mGame = intent.getStringExtra(C.FIELD_ID);
        else if(savedInstanceState != null)
            mGame = savedInstanceState.getString(C.FIELD_ID);
        else
            finish();

        mCourseAdapter = new CourseArrayAdapter(this, R.layout.list_course_row, mCourses, mHoles);
        mUserAdapter = new UserArrayAdapter(this, R.layout.list_user_row, mUsers);
        mUserAdapter.setShowAvatar(true);
        mGuestAdapter = new UserArrayAdapter(this, R.layout.list_user_row, mGuests);
        mGuestAdapter.setShowAvatar(true);
        mSubgameAdapter = new UserArrayAdapter(this, R.layout.list_user_row, mSubgames);
        mSubgameAdapter.setShowAvatar(true);

        mCourseSpinner.setAdapter(mCourseAdapter);
        mCourseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String course = ((TextView) view.findViewById(R.id.course_id)).getText().toString();
                setCourse(course);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnAddPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUserDialog.show();
            }
        });
        btnAddGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGuestDialog.show();
            }
        });

        mListListener = new ItemListListener(this, new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_remove)
                    removeSubgame(mListListener.getSelectedIndex());
                return true;
            }
        });

        makeUserDialog();
        makeGuestDialog();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(C.FIELD_ID, mGame);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_game_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case android.R.id.home:
                i = new Intent(GameEditActivity.this, GameActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            case R.id.action_start_game:
                startGame();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        fetchCourses();
        fetchHoles();
        fetchUsers();
        loadData();
    }

    @Override
    public void newCloudData(String type, Entity entity) {
        switch (type) {
            case C.TYPE_COURSE:
                fetchCourses();
                loadData();
                break;
            case C.TYPE_HOLE:
                fetchHoles();
                loadData();
                break;
            case C.TYPE_USER:
                fetchUsers();
                loadData();
                break;
            case C.TYPE_GAME:
                if(entity == null) break;
                String game = (String) entity.get(C.FIELD_ID);
                if(game.equals(mGame))
                    loadData();
                break;
            case C.TYPE_SUBGAME:
                if(entity == null) break;
                String subgame = (String) entity.get(C.FIELD_ID);
                for(Entity s : mSubgames)
                    if(s.get(C.FIELD_ID).equals(subgame)) {
                        loadData();
                        break;
                    }
                break;
            default:
                super.newCloudData(type, entity);
                break;
        }
    }

    public void startGame() {
        Log.d(C.TAG, "startGame");
        if(!checkService()) return;

        Entity oldGame = mDataStore.getGames().get(mGame);
        Entity newGame = new Entity(oldGame);

        long ts = new Date().getTime();
        newGame.put(C.FIELD_START, ts);

        mDataStore.updateEntity(newGame);

        Intent i = new Intent(GameEditActivity.this, GamePlayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(C.FIELD_ID, mGame);
        startActivity(i);
    }

    private void fetchCourses() {
        mCourses.clear();
        mCourses.addAll(mDataStore.getCourses().values());
        mCourseAdapter.notifyDataSetChanged();
    }

    private void fetchHoles() {
        mHoles.clear();
        mHoles.putAll(mDataStore.getHoles());
        mCourseAdapter.notifyDataSetChanged();
    }

    private void fetchUsers() {
        mUsers.clear();
        mGuests.clear();

        for(Entity user : mDataStore.getUsers().values()) {
            if(user.has(C.FIELD_IS_GUEST) && (Boolean) user.get(C.FIELD_IS_GUEST))
                mGuests.add(user);
            else
                mUsers.add(user);
        }

        mUserAdapter.notifyDataSetChanged();
        mGuestAdapter.notifyDataSetChanged();
    }

    private void setCourse(String course) {
        Log.d(C.TAG, "setCourse");
        if(!checkService()) return;

        Entity oldGame = mDataStore.getGames().get(mGame);
        Entity newGame = new Entity(oldGame);

        newGame.put(C.FIELD_COURSE, course);

        mDataStore.updateEntity(newGame);
    }

    private String addSubgame(String player) {
        Log.d(C.TAG, "addSubgame");
        if(!checkService()) return "";

        Entity oldGame = mDataStore.getGames().get(mGame);
        Entity newGame = new Entity(oldGame);

        ArrayList<String> subgames = new ArrayList<>();
        for(Entity s : mSubgames)
            subgames.add((String) s.get(C.FIELD_ID));
        Entity subgame = mDataStore.createSubgame(player);
        subgames.add((String) subgame.get(C.FIELD_ID));
        newGame.put(C.FIELD_SUBGAMES, subgames);

        mDataStore.updateEntity(newGame);
        return (String) subgame.get(C.FIELD_ID);
    }

    private void removeSubgame(int index) {
        Log.d(C.TAG, "removeSubgame");
        if(!checkService()) return;

        Entity oldGame = mDataStore.getGames().get(mGame);
        Entity newGame = new Entity(oldGame);

        ArrayList<String> subgames = new ArrayList<>();
        for(Entity s : mSubgames)
            subgames.add((String) s.get(C.FIELD_ID));
        String subgame = subgames.get(index);
        subgames.remove(index);
        newGame.put(C.FIELD_SUBGAMES, subgames);

        mDataStore.updateEntity(newGame);
        mDataStore.deleteEntity(mDataStore.getSubgames().get(subgame));
    }

    private void loadData() {
        Log.d(C.TAG, "loadData");
        Entity game = mDataStore.getGames().get(mGame);
        if(game == null) return;

        mCourse = null;
        if(game.has(C.FIELD_COURSE)) {
            String c = (String) game.get(C.FIELD_COURSE);
            mCourse = mDataStore.getCourses().get(c);
            if(mCourse != null) {
                for(int i = 0; i < mCourses.size(); ++i) {
                    Entity course = mCourses.get(i);
                    if(course.get(C.FIELD_ID).equals(c)) {
                        mCourseSpinner.setSelection(i);
                        break;
                    }
                }
            }
        }

        mSubgames.clear();
        if(game.has(C.FIELD_SUBGAMES)) {
            ArrayList<String> subs = (ArrayList<String>) game.get(C.FIELD_SUBGAMES);
            for(String s : subs) {
                Entity subgame = mDataStore.getSubgames().get(s);
                String u = (String) subgame.get(C.FIELD_USER);
                Entity user = mDataStore.getUsers().get(u);
                subgame.put(C.FIELD_NAME, user.get(C.FIELD_NAME));
                mSubgames.add(subgame);
            }
            Log.d(C.TAG, "loaded subgames: " + mSubgames.size());
        }
        makePlayerList();
    }

    private void makeUserDialog() {
        ListView lvUsers = new ListView(this);
        lvUsers.setAdapter(mUserAdapter);
        lvUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String user = ((TextView) view.findViewById(R.id.user_id)).getText().toString();
                addSubgame(user);
                mUserDialog.dismiss();
            }
        });
        mUserDialog = new Dialog(this);
        mUserDialog.setContentView(lvUsers);
        mUserDialog.setTitle("Add player");
    }

    private void makeGuestDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView txtAdd = new TextView(this);
        txtAdd.setText("Existing guest");
        txtAdd.setTextSize(30);
        root.addView(txtAdd);

        final Spinner spinner = new Spinner(this);
        spinner.setAdapter(mGuestAdapter);
        root.addView(spinner);

        Button btnAdd = new Button(this);
        btnAdd.setText("Add");
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) spinner.getSelectedView().findViewById(R.id.user_id);
                String user = tv.getText().toString();
                addSubgame(user);
                mGuestDialog.dismiss();
            }
        });
        root.addView(btnAdd);

        Space space = new Space(this);
        space.setMinimumHeight(200);
        root.addView(space);

        TextView txtNew = new TextView(this);
        txtNew.setText("New guest");
        txtNew.setTextSize(30);
        root.addView(txtNew);

        final EditText editName = new EditText(this);
        root.addView(editName);

        Button btnNew = new Button(this);
        btnNew.setText("Add");
        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText().toString();
                if(name.equals("")) return;
                Entity guest = mDataStore.createGuest(name);
                addSubgame((String) guest.get(C.FIELD_ID));
                mGuestDialog.dismiss();
            }
        });
        root.addView(btnNew);

        mGuestDialog = new Dialog(this);
        mGuestDialog.setContentView(root);
        mGuestDialog.setTitle("Add guest player");
    }

    private void makePlayerList() {
        mPlayerList.removeAllViews();
        final int count = mSubgameAdapter.getCount();
        for(int i = 0; i < count; ++i) {
            View v = mSubgameAdapter.getView(i, null, null);
            v.setBackgroundResource(R.drawable.clickable_background);
            v.setLongClickable(true);
            v.setOnLongClickListener(mListListener);
            v.setOnCreateContextMenuListener(mListListener);
            mPlayerList.addView(v);
        }
    }
}
