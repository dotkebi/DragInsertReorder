package com.example.amzadmin.myapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * @author dotkebi on 2015. 7. 9..
 */
public class DragInsertReorderBaseAdapter extends BaseAdapter implements DragInsertReorder.OnPositionChanged, DragInsertReorder.OnObjectInserted, DragInsertReorder.OnReordering {
    private Context context;
    private List<String> arrayList;

    public DragInsertReorderBaseAdapter(Context context, List<String> arrayList) {
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
            button = (Button) convertView.getTag(R.id.button);
        }

        final String item = arrayList.get(position);
        button.setText(item);
/*        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("button", item);
            }
        });*/

        return convertView;
    }

    @Override
    public void onPositionChanged(int oldPosition, int newPosition) {

    }

    @Override
    public void onObejctInserted(Object object) {

    }

    @Override
    public void onReordering() {

    }
}
