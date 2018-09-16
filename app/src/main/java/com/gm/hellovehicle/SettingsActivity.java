package com.gm.hellovehicle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.gm.android.vehicle.settings.activity.GmSettingsActivity;
import com.gm.android.vehicle.settings.adapter.GmSettingsAdapter;

import java.util.ArrayList;
import java.util.Collections;

public class SettingsActivity extends GmSettingsActivity {

    private ArrayList<String> mSettingsItems = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Collections.addAll(mSettingsItems, "Hello!", "We are settings items.", "Nice to meet you!");
    }

    @Override
    public String getHeaderTitle() {
        return getString(R.string.settings);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Toast.makeText(this, mSettingsItems.get(i), Toast.LENGTH_SHORT).show();
    }

    @Override
    public BaseAdapter getListAdapter() {
        return new GmSettingsAdapter(this, mSettingsItems);
    }
}
