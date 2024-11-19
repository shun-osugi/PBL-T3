package io.github.shun.osugi.pblt3.android;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

public class FooterUtils {
    public static void setupFooter(Activity activity) {
        Button buttonAttendance = activity.findViewById(R.id.buttonAttendance);
        Button buttonSettings = activity.findViewById(R.id.buttonSettings);
        Button buttonMain = activity.findViewById(R.id.buttonMain);

        buttonAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(activity, AttendanceActivity.class);
            activity.startActivity(intent);
        });

        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(activity, SettingsActivity.class);
            activity.startActivity(intent);
        });

        buttonMain.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);
        });
    }
}
