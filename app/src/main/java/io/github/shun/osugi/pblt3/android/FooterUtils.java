package io.github.shun.osugi.pblt3.android;

import android.app.Activity;
import android.content.Intent;
import android.widget.Button;

public class FooterUtils {
    public static void setupFooter(Activity activity) {
        Button buttonAttendance = activity.findViewById(R.id.buttonAttendance);
        Button buttonSettings = activity.findViewById(R.id.buttonSettings);
        Button buttonMain = activity.findViewById(R.id.buttonMain);

        // 右側のボタンをクリックしたときの遷移処理
        Button navigateButton = activity.findViewById(R.id.navigateButton);
        navigateButton.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);
            // アニメーションを消す
            activity.overridePendingTransition(0, 0);        });

        buttonAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(activity, AttendanceActivity.class);
            activity.startActivity(intent);
            // アニメーションを消す
            activity.overridePendingTransition(0, 0);
        });

        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(activity, SettingsActivity.class);
            activity.startActivity(intent);
            // アニメーションを消す
            activity.overridePendingTransition(0, 0);
        });

        buttonMain.setOnClickListener(v -> {
            Intent intent = new Intent(activity, TimetableActivity.class);
            activity.startActivity(intent);
            // アニメーションを消す
            activity.overridePendingTransition(0, 0);
        });
    }
}
