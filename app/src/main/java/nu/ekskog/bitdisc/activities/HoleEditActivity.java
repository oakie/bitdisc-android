package nu.ekskog.bitdisc.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.models.Entity;
import nu.ekskog.bitdisc.R;

public class HoleEditActivity extends AbstractBitdiscActivity {
    private String mHole;

    private EditText mEditPar;
    private EditText mEditDistance;
    private ImageView mImgMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hole_edit);

        mEditPar = (EditText)findViewById(R.id.edit_par);
        mEditDistance = (EditText)findViewById(R.id.edit_distance);
        mImgMap = (ImageView) findViewById(R.id.hole_map);

        Intent intent = getIntent();
        if(intent.hasExtra(C.FIELD_ID))
            mHole = intent.getStringExtra(C.FIELD_ID);
        else if(savedInstanceState != null)
            mHole = savedInstanceState.getString(C.FIELD_ID);
        else
            finish();

        if(mServiceBound) {
            loadData();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(C.FIELD_ID, mHole);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_hole_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case android.R.id.home:
                i = new Intent(HoleEditActivity.this, HoleDetailActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra(C.FIELD_ID, mHole);
                startActivity(i);
                return true;
            case R.id.action_save_hole:
                saveData();
                return true;
            case R.id.action_set_map:
                i = new Intent();
                i.setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(i, "Select image"), C.REQUEST_PICTURE);
                return true;
            case R.id.action_unset_map:
                removeMap();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == C.REQUEST_PICTURE && resultCode == RESULT_OK) {
            try {
                Uri uri = data.getData();
                Log.d(C.TAG, "uri: " + uri.toString());
                mDataStore.setHoleImage(mHole, uri);
            } catch(Exception e) {
                e.printStackTrace();
            }
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

        mEditPar.setText("");
        if(hole.has(C.FIELD_PAR))
            mEditPar.setText(hole.get(C.FIELD_PAR).toString());

        mEditDistance.setText("");
        if(hole.has(C.FIELD_DISTANCE))
            mEditDistance.setText(hole.get(C.FIELD_DISTANCE).toString());

        mImgMap.setImageBitmap(null);
        if(hole.has(C.FIELD_IMG_URL))
            mDataStore.getHoleImage(mHole, mImgMap);
    }

    private void saveData() {
        Log.d(C.TAG, "saveData");
        if(!checkService()) return;

        Entity oldHole = mDataStore.getHoles().get(mHole);
        Entity newHole = new Entity(oldHole);

        String par = mEditPar.getText().toString();
        String distance = mEditDistance.getText().toString();
        newHole.put(C.FIELD_PAR, 0);
        if(!par.equals(""))
            newHole.put(C.FIELD_PAR, Integer.parseInt(par));
        newHole.put(C.FIELD_DISTANCE, 0);
        if(!distance.equals(""))
            newHole.put(C.FIELD_DISTANCE, Integer.parseInt(distance));

        mDataStore.updateEntity(newHole);
    }

    private void removeMap() {
        Log.d(C.TAG, "removeMap");
        if(!checkService()) return;

        Entity hole = mDataStore.getHoles().get(mHole);
        mDataStore.popFromEntity(hole, C.FIELD_IMG_URL);
    }

}
