package com.example.dotkebi.myapp;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private DragInsertReorder dragInsertReorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dragInsertReorder = (DragInsertReorder) findViewById(R.id.drag);

        List<Object> arrayList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            arrayList.add("top " + i);
        }
        DragInsertReorderAdapter tAdapter = new DragInsertReorderAdapter(this, arrayList);

        List<Object> arrayList2 = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            arrayList2.add("bottom " + i);
        }
        DragInsertReorderAdapter dragInsertReorderBaseAdapter = new DragInsertReorderAdapter(this, arrayList2);

        dragInsertReorder.setTopAdapter(tAdapter);
        dragInsertReorder.setBottomAdapter(dragInsertReorderBaseAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dragInsertReorder.finish();
    }

}
