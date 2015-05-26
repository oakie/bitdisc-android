package nu.ekskog.bitdisc.game;

import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import nu.ekskog.bitdisc.AbstractBitdiscActivity;
import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.ItemListListener;
import nu.ekskog.bitdisc.R;

public class GameActivity extends AbstractBitdiscActivity {
    private ArrayList<Entity> mGames = new ArrayList<>();
    private HashMap<String, Entity> mCourses = new HashMap<>();
    private GameArrayAdapter mGameAdapter;
    private ItemListListener mListListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mGameAdapter = new GameArrayAdapter(this, R.layout.list_game_row, mGames, mCourses);
        mListListener = new ItemListListener(this, new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_remove)
                    removeGame(mListListener.getSelectedIndex());
                return true;
            }
        });

        ListView lv = (ListView)findViewById(R.id.game_list);
        registerForContextMenu(lv);
        lv.setAdapter(mGameAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String g = ((TextView) view.findViewById(R.id.game_id)).getText().toString();
                Entity game = mDataStore.getGames().get(g);
                Intent i;
                if (game.has(C.FIELD_END))
                    i = new Intent(GameActivity.this, GameDetailActivity.class);
                else if (game.has(C.FIELD_START))
                    i = new Intent(GameActivity.this, GamePlayActivity.class);
                else
                    i = new Intent(GameActivity.this, GameEditActivity.class);
                i.putExtra(C.FIELD_ID, g);
                startActivity(i);
            }
        });
        lv.setOnItemLongClickListener(mListListener);
        lv.setOnCreateContextMenuListener(mListListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case R.id.action_create_game:
                if(mServiceBound) {
                    Entity game = mDataStore.createGame();
                    i = new Intent(GameActivity.this, GameEditActivity.class);
                    i.putExtra(C.FIELD_ID, (String) game.get(C.FIELD_ID));
                    startActivity(i);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        fetchGames();
        fetchCourses();
    }

    @Override
    public void newCloudData(String type, Entity entity) {
        if(type.equals(C.TYPE_GAME)) {
            fetchGames();
            return;
        } else if(type.equals(C.TYPE_COURSE)) {
            fetchCourses();
            return;
        }
        super.newCloudData(type, entity);
    }

    private void removeGame(int index) {
        Log.d(C.TAG, "removeGame");
        if(!checkService()) return;

        Entity oldGame = mGames.get(index);
        Entity newGame = new Entity(oldGame);

        newGame.put(C.FIELD_SUBGAMES, null);
        mDataStore.updateEntity(newGame);

        ArrayList<String> subgames = (ArrayList<String>) oldGame.get(C.FIELD_SUBGAMES);
        if(subgames != null) {
            for (String s : subgames)
                mDataStore.deleteEntity(mDataStore.getSubgames().get(s));
        }

        mDataStore.deleteEntity(mGames.get(index));
    }

    private void fetchGames() {
        mGames.clear();
        mGames.addAll(mDataStore.getGames().values());
        mGameAdapter.notifyDataSetChanged();
    }

    private void fetchCourses() {
        mCourses.clear();
        mCourses.putAll(mDataStore.getCourses());
        mGameAdapter.notifyDataSetChanged();
    }
}
