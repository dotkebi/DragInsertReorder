package com.example.dotkebi.myapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * @author dotkebi on 2015. 7. 9..
 */
public class DragInsertReorderAdapter extends DragInsertReorderBaseAdapter {
    private Context context;

    public DragInsertReorderAdapter(Context context, List<Object> arrayList) {
        super(arrayList);
        this.context = context;
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

    @Override
    public void setItem(int position, Object object) {
        arrayList.set(position, object);
    }

}
