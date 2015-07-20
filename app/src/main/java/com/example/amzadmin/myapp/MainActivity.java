package com.example.amzadmin.myapp;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private DragReOrderInsert dragReOrderInsert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dragReOrderInsert = (DragReOrderInsert) findViewById(R.id.drag);

        List<String> arrayList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            arrayList.add("top " + i);
        }
        BAdapter tAdapter = new BAdapter(this, arrayList);

        List<String> arrayList2 = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            arrayList2.add("bottom " + i);
        }
        BAdapter bAdapter = new BAdapter(this, arrayList2);

        dragReOrderInsert.setTopAdapter(tAdapter);
        dragReOrderInsert.setBottomAdapter(bAdapter);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dragReOrderInsert.finish();
    }

}
