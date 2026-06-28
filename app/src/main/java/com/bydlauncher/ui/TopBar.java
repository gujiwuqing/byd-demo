package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import com.bydlauncher.R;

public class TopBar {

    private final TextView mediaSource;

    public TopBar(View rootView) {
        mediaSource = rootView.findViewById(R.id.top_media_source);
    }

    public void updateMediaSource(String sourceName) {
        if (sourceName != null && !sourceName.isEmpty()) {
            mediaSource.setText("♫ " + sourceName);
        } else {
            mediaSource.setText("♫");
        }
    }
}
