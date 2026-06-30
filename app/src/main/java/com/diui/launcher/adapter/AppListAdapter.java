package com.diui.launcher.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.diui.launcher.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private final Context context;
    private final List<AppInfo> apps = new ArrayList<>();
    private final PackageManager pm;

    public AppListAdapter(Context context) {
        this.context = context;
        this.pm = context.getPackageManager();
        loadApps();
    }

    private void loadApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

        String myPackage = context.getPackageName();
        for (ResolveInfo ri : activities) {
            if (ri.activityInfo.packageName.equals(myPackage)) continue;
            AppInfo info = new AppInfo();
            info.label = ri.loadLabel(pm).toString();
            info.packageName = ri.activityInfo.packageName;
            info.activityName = ri.activityInfo.name;
            info.icon = ri.loadIcon(pm);
            apps.add(info);
        }

        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    public void refresh() {
        apps.clear();
        loadApps();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo info = apps.get(position);
        holder.icon.setImageDrawable(info.icon);
        holder.name.setText(info.label);
        holder.itemView.setOnClickListener(v -> launchApp(info));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    private void launchApp(AppInfo info) {
        Intent intent = pm.getLaunchIntentForPackage(info.packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            name = itemView.findViewById(R.id.app_name);
        }
    }

    static class AppInfo {
        String label;
        String packageName;
        String activityName;
        Drawable icon;
    }
}
