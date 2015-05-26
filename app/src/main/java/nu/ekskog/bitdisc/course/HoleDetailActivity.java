package nu.ekskog.bitdisc.course;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import nu.ekskog.bitdisc.AbstractBitdiscActivity;
import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.R;


public class HoleDetailActivity extends AbstractBitdiscActivity {
    private String mHole;

    private TextView mTxtPar;
    private TextView mTxtDistance;
    private ImageView mImgMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hole_detail);

        mTxtPar = (TextView) findViewById(R.id.hole_par);
        mTxtDistance = (TextView) findViewById(R.id.hole_distance);
        mImgMap = (ImageView) findViewById(R.id.hole_map);

        Intent intent = getIntent();
        if(intent.hasExtra(C.FIELD_ID))
            mHole = intent.getStringExtra(C.FIELD_ID);
        else if(savedInstanceState != null)
            mHole = savedInstanceState.getString(C.FIELD_ID);
        else
            finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(C.FIELD_ID, mHole);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_hole_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case android.R.id.home:
                i = new Intent(HoleDetailActivity.this, CourseDetailActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            case R.id.action_edit_hole:
                i = new Intent(HoleDetailActivity.this, HoleEditActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra(C.FIELD_ID, mHole);
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
    }

    @Override
    public void newCloudData(String type, Entity entity) {
        switch(type) {
            case C.TYPE_HOLE:
                if(entity == null) break;
                String hole = (String) entity.get(C.FIELD_ID);
                if(hole.equals(mHole))
                    loadData();
                break;
            default:
                super.newCloudData(type, entity);
                break;
        }
    }

    private void loadData() {
        Log.d(C.TAG, "loadData");
        Entity hole = mDataStore.getHoles().get(mHole);
        if(hole == null) return;

        mTxtPar.setText("");
        if(hole.has(C.FIELD_PAR))
            mTxtPar.setText(hole.get(C.FIELD_PAR).toString());

        mTxtDistance.setText("");
        if(hole.has(C.FIELD_DISTANCE))
            mTxtDistance.setText(hole.get(C.FIELD_DISTANCE).toString());

        mImgMap.setImageBitmap(null);
        if(hole.has(C.FIELD_IMG_URL))
            mDataStore.getHoleImage(mHole, mImgMap);
    }

}
