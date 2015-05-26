package nu.ekskog.bitdisc.game;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import nu.ekskog.bitdisc.AbstractBitdiscActivity;
import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.R;
import nu.ekskog.bitdisc.Util;


public class GamePlayActivity extends AbstractBitdiscActivity {
    private String mGame;

    private Entity mCourse;
    private ArrayList<Entity> mPlayers = new ArrayList<>();
    private ArrayList<Entity> mSubgames = new ArrayList<>();

    private TextView mTxtCourse;
    private TextView mTxtStart;
    private TextView mTxtDuration;
    private TableLayout mTableHeader;
    private TableLayout mTableBody;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_play);

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
        mTableHeader = (TableLayout) findViewById(R.id.table_header);
        mTableBody = (TableLayout) findViewById(R.id.table_body);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(C.FIELD_ID, mGame);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_game_play, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case android.R.id.home:
                i = new Intent(GamePlayActivity.this, GameActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            case R.id.action_finish_game:
                finishGame();

                i = new Intent(GamePlayActivity.this, GameDetailActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra(C.FIELD_ID, mGame);
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
        makeGameTable();
    }

    @Override
    public void newCloudData(String type, Entity entity) {
        switch (type) {
            case C.TYPE_COURSE:
                loadData();
                break;
            case C.TYPE_HOLE:
                loadData();
                break;
            case C.TYPE_USER:
                loadData();
                break;
            case C.TYPE_GAME:
                loadData();
                makeGameTable();
                break;
            case C.TYPE_SUBGAME:
                loadData();
                makeGameTable();
                break;
            default:
                super.newCloudData(type, entity);
                break;
        }
    }

    private void finishGame() {
        Log.d(C.TAG, "finishGame");
        if(!checkService()) return;

        Entity oldGame = mDataStore.getGames().get(mGame);
        Entity newGame = new Entity(oldGame);

        long ts = new Date().getTime();
        newGame.put(C.FIELD_END, ts);

        mDataStore.updateEntity(newGame);
    }

    private void loadData() {
        Log.d(C.TAG, "loadData");

        mCourse = null;
        mPlayers.clear();
        mSubgames.clear();

        Entity game = mDataStore.getGames().get(mGame);

        mTxtCourse.setText("-");
        if(game.get(C.FIELD_COURSE) != null) {
            String c = (String)game.get(C.FIELD_COURSE);
            mCourse = mDataStore.getCourses().get(c);
            if(mCourse != null && mCourse.get(C.FIELD_NAME) != null)
                mTxtCourse.setText((String) mCourse.get(C.FIELD_NAME));
        }

        if(game.get(C.FIELD_START) != null) {
            long timestamp = (Long) game.get(C.FIELD_START);
            Date t = new Date(timestamp);
            SimpleDateFormat dt = new SimpleDateFormat(C.FORMAT_DATE_TIME);
            mTxtStart.setText(dt.format(t));

            long now = new Date().getTime();
            long dur = now - timestamp;
            long h = dur / (60*60*1000);
            long m = (dur-(h*(60*60*1000))) / (60*1000);
            mTxtDuration.setText(h + " hours " + m + " minutes");
        }

        if(game.get(C.FIELD_SUBGAMES) != null) {
            ArrayList<String> subs = (ArrayList<String>) game.get(C.FIELD_SUBGAMES);
            for(String s : subs) {
                Entity subgame = mDataStore.getSubgames().get(s);
                String u = (String) subgame.get(C.FIELD_USER);
                Entity user = mDataStore.getUsers().get(u);

                if(subgame.get(C.FIELD_SPLITS) == null) {
                    ArrayList<Entity> holes = (ArrayList<Entity>) mCourse.get(C.FIELD_HOLES);
                    ArrayList<Long> splits = new ArrayList<Long>();
                    for(int i = 0; i < holes.size(); ++i)
                        splits.add(0L);
                    subgame.put(C.FIELD_SPLITS, splits);
                    Log.d(C.TAG, "created splits: " + splits.size());
                }

                mPlayers.add(user);
                mSubgames.add(subgame);
            }
            Log.d(C.TAG, "loaded subgames: " + mSubgames.size());
        }
    }

    private void saveData() {
        Log.d(C.TAG, "saveData");
        if(!checkService()) return;

        for(Entity subgame : mSubgames) {
            Log.d(C.TAG, "saving " + subgame.toString());
            mDataStore.updateEntity(subgame);
        }
    }

    public void makeGameTable() {
        ArrayList<String> holes = (ArrayList<String>)mCourse.get(C.FIELD_HOLES);
        int cols = mPlayers.size();
        int rows = holes.size();

        int[] sums = new int[cols];
        int[][] splits = new int[rows][cols];
        for(int c = 0; c < cols; ++c) {
            sums[c] = 0;
            ArrayList<Long> s = (ArrayList<Long>) mSubgames.get(c).get(C.FIELD_SPLITS);
            for(int r = 0; r < s.size(); ++r) {
                splits[r][c] = s.get(r).intValue();
                sums[c] += splits[r][c];
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
            txtName.setGravity(Gravity.CENTER_HORIZONTAL);
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
            rowScore.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    AlertDialog.Builder playerBuilder = new AlertDialog.Builder(GamePlayActivity.this);
                    playerBuilder.setTitle("Hole " + String.valueOf(row + 1))
                            .setView(makeInputDialogView(row))
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    mDialog = playerBuilder.create();
                    mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            saveData();
                        }
                    });
                    mDialog.setCancelable(true);
                    mDialog.setCanceledOnTouchOutside(true);
                    mDialog.show();
                }
            });

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
                txtCell.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
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

        TextView txtSum = new TextView(this);
        txtSum.setText("\u03A3");
        txtSum.setTextSize(40);
        txtSum.setWidth(0);
        txtSum.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        rowSum.addView(txtSum);

        for(int c = 0; c < cols; ++c) {
            TextView txtCell = new TextView(this);
            txtCell.setText(String.valueOf(sums[c]));
            txtCell.setTextSize(30);
            txtCell.setWidth(0);
            txtCell.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            rowSum.addView(txtCell);
        }

        mTableBody.addView(rowSum);
    }

    private View makeInputDialogView(final int row) {
        ArrayList<String> holes = (ArrayList<String>)mCourse.get(C.FIELD_HOLES);
        final Entity hole = mDataStore.getHoles().get(holes.get(row));
        final int par = hole.has(C.FIELD_PAR) ? ((Long)hole.get(C.FIELD_PAR)).intValue() : 0;
        final int dist = hole.has(C.FIELD_DISTANCE) ? ((Long)hole.get(C.FIELD_DISTANCE)).intValue() : 0;

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView txtPar = new TextView(this);
        txtPar.setTextSize(30);
        txtPar.setText("Par: -");
        if(par != 0)
            txtPar.setText("Par: " + par);
        root.addView(txtPar);

        TextView txtDistance = new TextView(this);
        txtDistance.setTextSize(30);
        txtDistance.setText("Distance: -");
        if(dist != 0)
            txtDistance.setText("Distance: " + dist + " m");
        root.addView(txtDistance);

        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.HORIZONTAL);

        int cols = mPlayers.size();
        for(int c = 0; c < cols; c++) {
            final Entity subgame = mSubgames.get(c);
            final Entity player = mPlayers.get(c);
            final ArrayList<Long> splits = (ArrayList<Long>) subgame.get(C.FIELD_SPLITS);

            LinearLayout h = new LinearLayout(this);
            TextView txtName = new TextView(this);
            ImageButton btnUp = new ImageButton(this);
            final TextView txtVal = new TextView(this);
            ImageButton btnDown = new ImageButton(this);

            h.setOrientation(LinearLayout.VERTICAL);
            h.setGravity(Gravity.CENTER_HORIZONTAL);
            h.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            String name = Util.getPlayerTag((String) player.get(C.FIELD_NAME));
            txtName.setText(name);
            txtName.setTextSize(18);
            txtName.setGravity(Gravity.CENTER_HORIZONTAL);
            h.addView(txtName);

            btnUp.setImageResource(R.drawable.up);
            btnUp.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(splits.get(row) == 0 && par != 0)
                        splits.set(row, (long)par);
                    else
                        splits.set(row, splits.get(row) + 1);
                    txtVal.setText(String.valueOf(splits.get(row)));
                }
            });
            h.addView(btnUp);

            txtVal.setGravity(Gravity.CENTER);
            txtVal.setTextSize(30);
            txtVal.setText(String.valueOf(splits.get(row)));
            h.addView(txtVal);

            btnDown.setImageResource(R.drawable.down);
            btnDown.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (splits.get(row) == 0)
                        return;
                    splits.set(row, splits.get(row) - 1);
                    txtVal.setText(String.valueOf(splits.get(row)));
                }
            });
            h.addView(btnDown);
            s.addView(h);
        }
        root.addView(s);

        if(hole.has(C.FIELD_IMG_URL)) {
            ImageView img = new ImageView(this);
            img.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            img.setAdjustViewBounds(true);
            mDataStore.getHoleImage((String) hole.get(C.FIELD_ID), img);
            root.addView(img);
        }
        scroll.addView(root);
        return scroll;
    }
}
