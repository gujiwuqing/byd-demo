package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import com.bydlauncher.R;

public class PlaceholderPage {

    private final View rootView;
    private final TextView text;

    private static final String[] TAB_NAMES = {"Map", "Controls", "Apps", "Settings"};

    public PlaceholderPage(View rootView) {
        this.rootView = rootView;
        this.text = rootView.findViewById(R.id.placeholder_text);
    }

    public View getView() {
        return rootView;
    }

    public void showForTab(int tabIndex) {
        int nameIndex = tabIndex - 1;
        if (nameIndex >= 0 && nameIndex < TAB_NAMES.length) {
            text.setText(TAB_NAMES[nameIndex] + "\n" + rootView.getContext().getString(R.string.coming_soon));
        }
        rootView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        rootView.setVisibility(View.GONE);
    }
}
