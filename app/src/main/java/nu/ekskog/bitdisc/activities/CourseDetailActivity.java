package nu.ekskog.bitdisc.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.lists.HoleArrayAdapter;
import nu.ekskog.bitdisc.models.Entity;
import nu.ekskog.bitdisc.R;


public class CourseDetailActivity extends AbstractBitdiscActivity {
    private String mCourse;

    private ArrayList<Entity> mHoles = new ArrayList<>();
    private HoleArrayAdapter mHoleAdapter;

    private TextView mTxtName;
    private TextView mTxtCity;
    private TextView mTxtPar;
    private TextView mTxtDistance;
    private ImageView mImgMap;
    private LinearLayout mHoleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        Intent intent = getIntent();
        if(intent.hasExtra(C.FIELD_ID))
            mCourse = intent.getStringExtra(C.FIELD_ID);
        else if(savedInstanceState != null)
            mCourse = savedInstanceState.getString(C.FIELD_ID);
        else
            finish();

        mTxtName = (TextView) findViewById(R.id.txt_name);
        mTxtCity = (TextView) findViewById(R.id.txt_city);
        mTxtPar = (TextView) findViewById(R.id.txt_par);
        mTxtDistance = (TextView) findViewById(R.id.txt_distance);
        mImgMap = (ImageView) findViewById(R.id.course_map);

        mHoleAdapter = new HoleArrayAdapter(this, R.layout.list_hole_row, mHoles);
        mHoleList = (LinearLayout) findViewById(R.id.list_holes);

        if(mServiceBound) {
            loadData();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(C.FIELD_ID, mCourse);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_course_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case android.R.id.home:
                i = new Intent(CourseDetailActivity.this, CourseActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            case R.id.action_edit_course:
                i = new Intent(CourseDetailActivity.this, CourseEditActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra(C.FIELD_ID, mCourse);
                startActivity(i);
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
        switch (type) {
            case C.TYPE_COURSE:
                if(entity == null) break;
                String course = (String) entity.get(C.FIELD_ID);
                if (course.equals(mCourse))
                    loadData();
                break;
            case C.TYPE_HOLE:
                if(entity == null) break;
                String hole = (String) entity.get(C.FIELD_ID);
                for(Entity h : mHoles)
                    if(h.get(C.FIELD_ID).equals(hole)) {
                        loadData();
                        break;
                    }
                break;
            default:
                super.newCloudData(type, entity);
                break;
        }
    }

    private void loadData() {
        Log.d(C.TAG, "loadData course: " + mCourse);
        Entity course = mDataStore.getCourses().get(mCourse);
        if(course == null) return;

        mTxtName.setText("");
        if(course.has(C.FIELD_NAME))
            mTxtName.setText((String) course.get(C.FIELD_NAME));

        mTxtCity.setText("");
        if(course.has(C.FIELD_CITY))
            mTxtCity.setText((String) course.get(C.FIELD_CITY));

        if(course.has(C.FIELD_HOLES)) {
            mHoles.clear();
            List<String> holes = (List<String>) course.get(C.FIELD_HOLES);
            for(String id : holes) {
                Entity hole = mDataStore.getHoles().get(id);
                if(hole != null)
                    mHoles.add(hole);
            }
            int parSum = 0;
            int distSum = 0;
            for(Entity hole : mHoles) {
                if(hole.has(C.FIELD_PAR))
                    parSum += ((Long) hole.get(C.FIELD_PAR)).intValue();
                if(hole.has(C.FIELD_DISTANCE))
                    distSum += ((Long) hole.get(C.FIELD_DISTANCE)).intValue();
            }
            mTxtPar.setText(String.valueOf(parSum));
            mTxtDistance.setText(String.valueOf(distSum) + " meters");

            makeHoleList();
            Log.d(C.TAG, "loaded holes: " + mHoles.size());
        }

        mImgMap.setImageBitmap(null);
        if(course.has(C.FIELD_IMG_URL))
            mDataStore.getCourseImage(mCourse, mImgMap);
    }

    private void makeHoleList() {
        final int length = mHoleAdapter.getCount();
        mHoleList.removeAllViews();
        for(int i = 0; i < length; ++i) {
            View v = mHoleAdapter.getView(i, null, null);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String hole = ((TextView) v.findViewById(R.id.hole_id)).getText().toString();
                    Intent i = new Intent(CourseDetailActivity.this, HoleDetailActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra(C.FIELD_ID, hole);
                    startActivity(i);
                }
            });
            mHoleList.addView(v);
        }
    }
}
