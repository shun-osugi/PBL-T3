package io.github.shun.osugi.pblt3.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.util.Log;

import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String buttonClicked = intent.getStringExtra("button");

        if ("OK".equals(buttonClicked)) {
            // OKボタンがクリックされた場合の処理
            Log.d("MainActivity", "OK button clicked");
        } else if ("NO".equals(buttonClicked)) {
            // NOボタンがクリックされた場合の処理
            Log.d("MainActivity", "NO button clicked");
        }
    }
}

