package nu.ekskog.bitdisc.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.models.Entity;
import nu.ekskog.bitdisc.R;
import nu.ekskog.bitdisc.Util;
import nu.ekskog.bitdisc.lists.UserArrayAdapter;


public class GameDetailActivity extends AbstractBitdiscActivity {
    private String mGame;

    private Entity mCourse;
    private ArrayList<Entity> mSubgames = new ArrayList<>();
    private ArrayList<Entity> mPlayers = new ArrayList<>();
    private UserArrayAdapter mPlayerAdapter;

    private TextView mTxtCourse;
    private TextView mTxtStart;
    private TextView mTxtDuration;
    private LinearLayout mRankList;
    private TableLayout mTableHeader;
    private TableLayout mTableBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        Intent intent = getIntent();
        if(intent.hasExtra(C.FIELD_ID))
            mGame = intent.getStringExtra(C.FIELD_ID);
        else if(savedInstanceState != null)
            mGame = savedInstanceState.getString(C.FIELD_ID);
        else
            finish();

        mTxtCourse = (TextView) findViewById(R.id.txt_course);
        mTxtStart = (TextView) findViewById(R.id.txt_start);
        mTxtDuration = (TextView) findViewById(R.id.txt_duration);
        mRankList = (LinearLayout) findViewById(R.id.rank_list);
        mTableHeader = (TableLayout) findViewById(R.id.table_header);
        mTableBody = (TableLayout) findViewById(R.id.table_body);

        mPlayerAdapter = new UserArrayAdapter(this, R.layout.list_user_row, mPlayers);
        mPlayerAdapter.setShowRank(true);
        mPlayerAdapter.setShowValue(true);
        mPlayerAdapter.setShowAvatar(true);

        if(mServiceBound) {
            loadData();
            makeRankList();
            makeGameTable();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(C.FIELD_ID, mGame);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_game_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case android.R.id.home:
                i = new Intent(GameDetailActivity.this, GameActivity.class);
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
        makeRankList();
        makeGameTable();
    }

    @Override
    public void newCloudData(String type, Entity entity) {
        switch (type) {
            case C.TYPE_COURSE:
                loadData();
                break;
            case C.TYPE_USER:
                loadData();
                break;
            case C.TYPE_GAME:
                loadData();
                makeRankList();
                makeGameTable();
                break;
            case C.TYPE_SUBGAME:
                loadData();
                makeRankList();
                makeGameTable();
                break;
            default:
                super.newCloudData(type, entity);
                break;
        }
    }

    private void loadData() {
        Log.d(C.TAG, "loadData");
        Entity game = mDataStore.getGames().get(mGame);
        if(game == null) return;

        mTxtCourse.setText("-");
        mCourse = null;
        if(game.get(C.FIELD_COURSE) != null) {
            String c = (String)game.get(C.FIELD_COURSE);
            mCourse = mDataStore.getCourses().get(c);
            if(mCourse != null && mCourse.get(C.FIELD_NAME) != null)
                mTxtCourse.setText((String) mCourse.get(C.FIELD_NAME));
        }

        mTxtStart.setText("-");
        mTxtDuration.setText("-");
        if(game.has(C.FIELD_START)) {
            long start = (Long) game.get(C.FIELD_START);
            SimpleDateFormat dt = new SimpleDateFormat(C.FORMAT_DATE_TIME);
            mTxtStart.setText(dt.format(new Date(start)));

            if(game.has(C.FIELD_END)) {
                long end = (Long) game.get(C.FIELD_END);
                long dur = end - start;
                long h = dur / (60 * 60 * 1000);
                long m = (dur - (h * (60 * 60 * 1000))) / (60 * 1000);
                mTxtDuration.setText(h + " hours " + m + " minutes");
            }
        }

        mPlayers.clear();
        mSubgames.clear();
        if(game.get(C.FIELD_SUBGAMES) != null) {
            ArrayList<String> subs = (ArrayList<String>) game.get(C.FIELD_SUBGAMES);
            for(String s : subs) {
                Entity subgame = mDataStore.getSubgames().get(s);
                String u = (String) subgame.get(C.FIELD_USER);
                Entity user = mDataStore.getUsers().get(u);
                mPlayers.add(user);
                mSubgames.add(subgame);
            }
            Log.d(C.TAG, "loaded subgames: " + mSubgames.size());
        }
        makeRankList();
    }

    private void makeRankList() {
        ArrayList<String> holes = (ArrayList<String>)mCourse.get(C.FIELD_HOLES);
        final int rows = holes.size();
        final int cols = mPlayers.size();

        final ArrayList<Integer> sums = new ArrayList<>();
        for(int c = 0; c < cols; ++c) {
            sums.add(0);
            ArrayList<Long> s = (ArrayList<Long>) mSubgames.get(c).get(C.FIELD_SPLITS);
            if(s != null) {
                for (int r = 0; r < rows; ++r) {
                    sums.set(c, sums.get(c) + s.get(r).intValue());
                }
            }
        }

        ArrayList<Integer> indexes = new ArrayList<>();
        for(int c = 0; c < cols; ++c)
            indexes.add(c);
        Collections.sort(indexes, new Comparator<Integer>() {
            @Override
            public int compare(Integer lhs, Integer rhs) {
                return sums.get(lhs) - sums.get(rhs);
            }
        });

        ArrayList<Integer> sorted = new ArrayList<>(sums);
        Collections.sort(sorted);
        ArrayList<Integer> ranks = new ArrayList<>();
        for(int c = 0; c < cols; ++c)
            ranks.add(sorted.indexOf(sums.get(c)) + 1);

        mRankList.removeAllViews();
        for(int c = 0; c < cols; ++c) {
            int player = indexes.get(c);
            View v = mPlayerAdapter.getView(player, null, null);
            TextView txtRank = (TextView)v.findViewById(R.id.user_rank);
            txtRank.setText(String.valueOf(ranks.get(player)));
            TextView txtValue = (TextView)v.findViewById(R.id.user_value);
            txtValue.setText(String.valueOf(sums.get(player)));
            mRankList.addView(v);
        }
    }

    private void makeGameTable() {
        ArrayList<String> holes = (ArrayList<String>)mCourse.get(C.FIELD_HOLES);
        int cols = mPlayers.size();
        int rows = holes.size();

        int[] sums = new int[cols];
        int[][] splits = new int[rows][cols];
        for(int c = 0; c < cols; ++c) {
            sums[c] = 0;
            ArrayList<Long> s = (ArrayList<Long>) mSubgames.get(c).get(C.FIELD_SPLITS);
            if(s != null) {
                for (int r = 0; r < s.size(); ++r) {
                    splits[r][c] = s.get(r).intValue();
                    sums[c] += splits[r][c];
                }
            }
        }

        mTableHeader.removeAllViews();
        mTableBody.removeAllViews();

        TableRow rowHeader = new TableRow(this);
        rowHeader.setGravity(Gravity.CENTER);

        TextView padding = new TextView(this);
        padding.setWidth(0);
        rowHeader.addView(padding);

        for(int c = 0; c < cols; ++c) {
            Entity player = mPlayers.get(c);
            String name = Util.getPlayerTag((String) player.get(C.FIELD_NAME));
            TextView txtName = new TextView(this);
            txtName.setText(name);
            txtName.setTextSize(18);
            txtName.setWidth(0);
            txtName.setGravity(Gravity.END);
            rowHeader.addView(txtName);
        }
        mTableHeader.addView(rowHeader);

        TableRow rowDivider = new TableRow(this);
        rowDivider.setMinimumHeight(5);
        rowDivider.setBackgroundColor(padding.getCurrentTextColor());
        mTableHeader.addView(rowDivider);

        for(int r = 0; r < rows; ++r) {
            final int row = r;
            TableRow rowScore = new TableRow(this);
            rowScore.setGravity(Gravity.CENTER);
            rowScore.setBackgroundResource(R.drawable.clickable_background);
            rowScore.setClickable(true);

            TextView txtHole = new TextView(this);
            txtHole.setText(Integer.toString(r + 1) + ".");
            txtHole.setSingleLine(true);
            txtHole.setTextSize(40);
            txtHole.setWidth(0);
            txtHole.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            rowScore.addView(txtHole);

            for(int c = 0; c < cols; ++c) {
                TextView txtCell = new TextView(this);
                txtCell.setText(String.valueOf(splits[r][c]));
                txtCell.setTextSize(30);
                txtCell.setWidth(0);
                txtCell.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                rowScore.addView(txtCell);
            }
            mTableBody.addView(rowScore);
        }
        rowDivider = new TableRow(this);
        rowDivider.setMinimumHeight(5);
        rowDivider.setBackgroundColor(padding.getCurrentTextColor());
        mTableBody.addView(rowDivider);

        TableRow rowSum = new TableRow(this);
        rowSum.setGravity(Gravity.CENTER);

        TextView txtHole = new TextView(this);
        txtHole.setText("\u03A3");
        txtHole.setTextSize(40);
        txtHole.setWidth(0);
        txtHole.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        rowSum.addView(txtHole);

        for(int c = 0; c < cols; ++c) {
            TextView txtCell = new TextView(this);
            txtCell.setText(String.valueOf(sums[c]));
            txtCell.setTextSize(30);
            txtCell.setWidth(0);
            txtCell.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            rowSum.addView(txtCell);
        }

        mTableBody.addView(rowSum);
    }
}
