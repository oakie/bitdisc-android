package nu.ekskog.bitdisc.course;

import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudinary.Cloudinary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nu.ekskog.bitdisc.AbstractBitdiscActivity;
import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.Entity;
import nu.ekskog.bitdisc.ItemListListener;
import nu.ekskog.bitdisc.R;


public class CourseEditActivity extends AbstractBitdiscActivity {
    private String mCourse;

    private EditText mEditName;
    private EditText mEditCity;
    private ImageView mImgMap;
    private LinearLayout mHoleList;

    private ArrayList<Entity> mHoles = new ArrayList<>();
    private HoleArrayAdapter mHoleAdapter;
    private ItemListListener mListListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_edit);

        mEditName = (EditText) findViewById(R.id.input_name);
        mEditCity = (EditText) findViewById(R.id.input_city);
        mImgMap = (ImageView) findViewById(R.id.course_map);
        mHoleList = (LinearLayout) findViewById(R.id.list_holes);

        Intent intent = getIntent();
        if(intent.hasExtra(C.FIELD_ID))
            mCourse = intent.getStringExtra(C.FIELD_ID);
        else if(savedInstanceState != null)
            mCourse = savedInstanceState.getString(C.FIELD_ID);
        else
            finish();

        mHoleAdapter = new HoleArrayAdapter(this, R.layout.list_hole_row, mHoles);
        mListListener = new ItemListListener(this, new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_remove)
                    removeHole(mListListener.getSelectedIndex());
                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(C.FIELD_ID, mCourse);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_course_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case android.R.id.home:
                i = new Intent(CourseEditActivity.this, CourseDetailActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra(C.FIELD_ID, mCourse);
                startActivity(i);
                return true;
            case R.id.action_create_hole:
                String hole = addHole();
                if(!hole.equals("")) {
                    i = new Intent(CourseEditActivity.this, HoleEditActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra(C.FIELD_ID, hole);
                    startActivity(i);
                }
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
            case R.id.action_save_course:
                saveData();
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
                mDataStore.setCourseImage(mCourse, uri);
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
        Log.d(C.TAG, "newCloudData");
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
        Log.d(C.TAG, "loadData");
        Entity course = mDataStore.getCourses().get(mCourse);
        if(course == null) return;

        mEditName.setText("");
        if(course.has(C.FIELD_NAME))
            mEditName.setText((String) course.get(C.FIELD_NAME));

        mEditCity.setText("");
        if(course.has(C.FIELD_CITY))
            mEditCity.setText((String) course.get(C.FIELD_CITY));

        mHoles.clear();
        if(course.has(C.FIELD_HOLES)) {
            List<String> holes = (List<String>) course.get(C.FIELD_HOLES);
            for(String id : holes) {
                Entity hole = mDataStore.getHoles().get(id);
                if(hole != null) mHoles.add(hole);
                else Log.d(C.TAG, "unknown hole: " + id);
            }
            Log.d(C.TAG, "loaded holes: " + mHoles.size());
        }
        makeHoleList();

        mImgMap.setImageBitmap(null);
        if(course.has(C.FIELD_IMG_URL))
            mDataStore.getCourseImage(mCourse, mImgMap);
    }

    private void saveData() {
        Log.d(C.TAG, "saveData");
        if(!checkService()) return;

        Entity oldCourse = mDataStore.getCourses().get(mCourse);
        Entity newCourse = new Entity(oldCourse);

        String name = mEditName.getText().toString();
        String city = mEditCity.getText().toString();
        newCourse.put(C.FIELD_NAME, name);
        newCourse.put(C.FIELD_CITY, city);

        mDataStore.updateEntity(newCourse);
    }

    private void removeMap() {
        Log.d(C.TAG, "removeMap");
        if(!checkService()) return;

        Entity course = mDataStore.getCourses().get(mCourse);
        mDataStore.popFromEntity(course, C.FIELD_IMG_URL);
    }

    private String addHole() {
        Log.d(C.TAG, "addHole");
        if(!checkService()) return "";

        Entity oldCourse = mDataStore.getCourses().get(mCourse);
        Entity newCourse = new Entity(oldCourse);

        ArrayList<String> holes = new ArrayList<>();
        for(Entity h : mHoles)
            holes.add((String) h.get(C.FIELD_ID));
        Entity hole = mDataStore.createHole();
        holes.add((String) hole.get(C.FIELD_ID));
        newCourse.put(C.FIELD_HOLES, holes);

        mDataStore.updateEntity(newCourse);
        return (String) hole.get(C.FIELD_ID);
    }

    private void removeHole(int index) {
        Log.d(C.TAG, "removeHole");
        if(!checkService()) return;

        Entity oldCourse = mDataStore.getCourses().get(mCourse);
        Entity newCourse = new Entity(oldCourse);

        ArrayList<String> holes = new ArrayList<>();
        for(Entity h : mHoles)
            holes.add((String) h.get(C.FIELD_ID));
        String hole = holes.get(index);
        holes.remove(index);
        newCourse.put(C.FIELD_HOLES, holes);

        mDataStore.updateEntity(newCourse);
        mDataStore.deleteEntity(mDataStore.getHoles().get(hole));
    }

    private void makeHoleList() {
        final int length = mHoleAdapter.getCount();
        mHoleList.removeAllViews();
        for(int i = 0; i < length; ++i) {
            View v = mHoleAdapter.getView(i, null, null);
            v.setBackgroundResource(R.drawable.clickable_background);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String hole = ((TextView) v.findViewById(R.id.hole_id)).getText().toString();
                    Intent i = new Intent(CourseEditActivity.this, HoleEditActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra(C.FIELD_ID, hole);
                    startActivity(i);
                }
            });
            v.setLongClickable(true);
            v.setOnLongClickListener(mListListener);
            v.setOnCreateContextMenuListener(mListListener);
            mHoleList.addView(v);
        }
    }
}
