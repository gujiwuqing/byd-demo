package com.bydlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机自启动接收器。
 * BYD 车机的 AppAutoStartService 存在 INTERACT_ACROSS_USERS 权限问题，
 * 无法可靠启动第三方应用，因此需要自己监听 BOOT_COMPLETED 实现自启。
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Log.i(TAG, "Received BOOT_COMPLETED, starting MainActivity");

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.putExtra("from_boot", true);
        context.startActivity(launchIntent);
    }
}
