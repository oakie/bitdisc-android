package nu.ekskog.bitdisc.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import nu.ekskog.bitdisc.C;
import nu.ekskog.bitdisc.lists.CourseArrayAdapter;
import nu.ekskog.bitdisc.models.Entity;
import nu.ekskog.bitdisc.lists.ItemListListener;
import nu.ekskog.bitdisc.R;


public class CourseActivity extends AbstractBitdiscActivity {
    private ArrayList<Entity> mCourses= new ArrayList<>();
    private HashMap<String, Entity> mHoles = new HashMap<>();
    private CourseArrayAdapter mCourseAdapter;
    private ItemListListener mListListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course);

        mCourseAdapter = new CourseArrayAdapter(this, R.layout.list_course_row, mCourses, mHoles);
        mListListener = new ItemListListener(this, new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_remove)
                    removeCourse(mListListener.getSelectedIndex());
                return false;
            }
        });

        ListView lv = (ListView)findViewById(R.id.course_list);
        registerForContextMenu(lv);
        lv.setAdapter(mCourseAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String course = ((TextView) view.findViewById(R.id.course_id)).getText().toString();
                Intent i = new Intent(CourseActivity.this, CourseDetailActivity.class);
                i.putExtra(C.FIELD_ID, course);
                startActivity(i);
            }
        });
        lv.setOnItemLongClickListener(mListListener);
        lv.setOnCreateContextMenuListener(mListListener);

        if(mServiceBound) {
            fetchCourses();
            fetchHoles();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_course, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case R.id.action_create_course:
                if(mServiceBound) {
                    Entity course = mDataStore.createCourse();
                    i = new Intent(CourseActivity.this, CourseEditActivity.class);
                    i.putExtra(C.FIELD_ID, (String) course.get(C.FIELD_ID));
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
        fetchCourses();
        fetchHoles();
    }

    @Override
    public void newCloudData(String type, Entity entity) {
        switch (type) {
            case C.TYPE_COURSE:
                fetchCourses();
                break;
            case C.TYPE_HOLE:
                fetchHoles();
                break;
            default:
                super.newCloudData(type, entity);
                break;
        }
    }

    private void removeCourse(int index) {
        mDataStore.deleteEntity(mCourses.get(index));
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
}
