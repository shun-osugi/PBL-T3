package io.github.shun.osugi.pblt3.android;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.PendingIntent;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1; // 任意のリクエストコード
    private static final String CHANNEL_ID = "Notification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // アラームスケジュールの権限があるかチェック
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(MainActivity.this, PermissionCheckActivity.class);
                startActivity(intent);
            }

            // アラーム権限の確認
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_ALARM)
                    != PackageManager.PERMISSION_GRANTED) {
                // 権限がない場合、リクエストする
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SET_ALARM}, REQUEST_PERMISSION_CODE);
            }
        }


        // 通知チャネルの作成
        createNotificationChannel();

        // ボタンのクリックイベントを設定
        Button notifyButton = findViewById(R.id.notifyButton);
        notifyButton.setOnClickListener(v -> showNotification());
    }

    // 通知を表示するメソッド
    private void showNotification() {
        // OKボタンのクリック処理
        Intent okIntent = new Intent(this, NotificationReceiver.class);
        okIntent.putExtra("button", "OK");
        PendingIntent okPendingIntent = PendingIntent.getBroadcast(this, 1, okIntent, PendingIntent.FLAG_IMMUTABLE);

        // NOボタンのクリック処理
        Intent noIntent = new Intent(this, NotificationReceiver.class);
        noIntent.putExtra("button", "NO");
        PendingIntent noPendingIntent = PendingIntent.getBroadcast(this, 2, noIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 通知アイコン
                .setContentTitle("Simple Notification")          // 通知タイトル
                .setContentText("This is a simple notification.") // 通知メッセージ
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_launcher_foreground, "OK", okPendingIntent) // OKボタン
                .addAction(R.drawable.ic_launcher_foreground, "NO", noPendingIntent); // NOボタン

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }


    // 通知チャネルを作成するメソッド（Android 8.0以降が対象）
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Simple Notification Channel";
            String description = "Channel for simple notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 権限が許可された場合の処理
                // 必要なアクションを実行
            } else {
                // 権限が拒否された場合の処理
                // ユーザーに説明を表示したり、代替手段を提供する
            }
        }
    }
}
