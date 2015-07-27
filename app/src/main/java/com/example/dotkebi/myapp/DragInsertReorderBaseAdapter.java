package com.example.dotkebi.myapp;

import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dotkebi on 2015. 7. 9..
 */
public abstract class DragInsertReorderBaseAdapter extends BaseAdapter implements DragInsertReorder.OnPositionChanged, DragInsertReorder.OnObjectInserted {
    protected List<Object> arrayList = new ArrayList<>();

    public DragInsertReorderBaseAdapter(List<Object> arrayList) {
        this.arrayList = arrayList;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return arrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onPositionChanged(int oldPosition, int newPosition) {
        Object oldO = getItem(oldPosition);
        Object newO = getItem(newPosition);

        setItem(oldPosition, newO);
        setItem(newPosition, oldO);
    }

    private void setItem(int position, Object object) {
        arrayList.set(position, object);
    }

    @Override
    public void onObejctInserted(int position, Object object) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            if (i == position) {
                list.add(object);
            }
            list.add(arrayList.get(i));
        }
        arrayList = list;
    }

}
