package com.diui.launcher.ui;

import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.diui.launcher.R;
import com.diui.launcher.adapter.AppListAdapter;

public class AppsPage {

    private final View rootView;
    private final AppListAdapter adapter;

    public AppsPage(View rootView) {
        this.rootView = rootView;
        RecyclerView grid = rootView.findViewById(R.id.apps_grid);
        grid.setLayoutManager(new GridLayoutManager(rootView.getContext(), 6));
        adapter = new AppListAdapter(rootView.getContext());
        grid.setAdapter(adapter);
    }

    public void refresh() {
        adapter.refresh();
    }
}
