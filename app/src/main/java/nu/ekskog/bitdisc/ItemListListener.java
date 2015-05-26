package nu.ekskog.bitdisc;

import android.app.Activity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;

public class ItemListListener implements View.OnCreateContextMenuListener,
        View.OnLongClickListener, AdapterView.OnItemLongClickListener {
    private Activity mContext;
    private MenuItem.OnMenuItemClickListener mListener;
    private int mSelectedIndex = -1;

    public ItemListListener(Activity context, MenuItem.OnMenuItemClickListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        mContext.getMenuInflater().inflate(R.menu.menu_item_list, menu);
        final int length = menu.size();
        for (int index = 0; index < length; index++) {
            final MenuItem menuItem = menu.getItem(index);
            menuItem.setOnMenuItemClickListener(mListener);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        mSelectedIndex = ((LinearLayout)v.getParent()).indexOfChild(v);
        Log.d(C.TAG, "selected index: " + mSelectedIndex);
        v.showContextMenu();
        return false;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
        mSelectedIndex = position;
        Log.d(C.TAG, "selected index: " + mSelectedIndex);
        parent.showContextMenu();
        return false;
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }
}
