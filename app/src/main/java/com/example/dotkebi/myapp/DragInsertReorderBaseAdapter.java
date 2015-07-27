package com.example.dotkebi.myapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dotkebi on 2015. 7. 9..
 */
public class DragInsertReorderBaseAdapter extends BaseAdapter implements DragInsertReorder.OnPositionChanged, DragInsertReorder.OnObjectInserted {
    private Context context;
    private List<Object> arrayList = new ArrayList<>();

    public DragInsertReorderBaseAdapter(Context context, List<Object> arrayList) {
        this.context = context;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView button;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.child, null);
            button = (TextView) convertView.findViewById(R.id.button);
            convertView.setTag(R.id.button, button);
        } else {
            button = (TextView) convertView.getTag(R.id.button);
        }

        String item = (String) arrayList.get(position);
        button.setText(item);
/*        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("button", item);
            }
        });*/

        return convertView;
    }

    public void setItem(int position, Object object) {
        String str = (object == null) ? "" : String.valueOf(object);
        arrayList.set(position, str);
    }

    @Override
    public void onPositionChanged(int oldPosition, int newPosition) {
        Object oldO = getItem(oldPosition);
        Object newO = getItem(newPosition);

        setItem(oldPosition, newO);
        setItem(newPosition, oldO);
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
