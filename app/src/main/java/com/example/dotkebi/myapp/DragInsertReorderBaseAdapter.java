package com.example.dotkebi.myapp;

import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dotkebi on 2015. 7. 9..
 */
public abstract class DragInsertReorderBaseAdapter<T> extends BaseAdapter implements DragInsertReorder.OnPositionChanged, DragInsertReorder.OnObjectInserted<T> {
    protected List<T> arrayList = new ArrayList<>();

    public DragInsertReorderBaseAdapter(List<T> arrayList) {
        this.arrayList = arrayList;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public T getItem(int position) {
        return arrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onPositionChanged(int oldPosition, int newPosition) {
        T oldO = getItem(oldPosition);
        T newO = getItem(newPosition);

        setItem(oldPosition, newO);
        setItem(newPosition, oldO);
    }

    private void setItem(int position, T object) {
        arrayList.set(position, object);
    }

    @Override
    public void onObejctInserted(int position, T object) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            if (i == position) {
                list.add(object);
            }
            list.add(arrayList.get(i));
        }
        arrayList = list;
    }

}
