package io.github.shun.osugi.pblt3.android;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.PendingIntent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FirestoreExample";
    private FirebaseFirestore db;
    private String userID;
    private ProgressBar progressBar;
    private TableLayout tableLayout;
    final String[] daysOfWeek = {"月", "火", "水", "木", "金", "土", "日"};
    private static final int REQUEST_PERMISSION_CODE = 1; // 任意のリクエストコード
    private static final String CHANNEL_ID = "Notification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Firestoreのインスタンスを取得
        db = FirebaseFirestore.getInstance();
        userID = "sample（ユーザーID）";
        // ヘッダーのタイトルを動的に変更
        TextView headerTitle = findViewById(R.id.headerTitle);
        headerTitle.setText("通知");

        // フッターのクリックイベントを設定
        FooterUtils.setupFooter(this);

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

        Spinner subjectSpinner = findViewById(R.id.subjectSpinner);
        List<String> subjectList = new ArrayList<>();

// 曜日リスト
        String[] days = {"月", "火", "水", "木", "金"};
// 時間割（1~7限）
        int[] periods = {1, 2, 3, 4, 5, 6, 7};

// Firestoreからデータを取得
        for (String day : days) {
            for (int period : periods) {
                db.collection("timetable").document(userID).collection(day).document(String.valueOf(period)).get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot snapshot = task.getResult();
                                if (snapshot.exists()) {
                                    // 教科名を取得してリストに追加
                                    String subjectName = snapshot.contains("教科名") ? snapshot.getString("教科名") : "不明";
                                    if (!subjectList.contains(subjectName)) { // 重複を防ぐ
                                        subjectList.add(subjectName);
                                    }

                                    // スピナーにアダプターを設定（毎回更新）
                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectList);
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    subjectSpinner.setAdapter(adapter);
                                } else {
                                    Log.d("Database", "ドキュメントが存在しません: " + day + " " + period);
                                }
                            } else {
                                Log.e("Database", "データの取得に失敗しました: " + day + " " + period, task.getException());
                            }
                        });
            }
        }

        // 通知ボタンのクリックイベント
        Button notifyButton = findViewById(R.id.notifyButton);
        notifyButton.setOnClickListener(v -> {
            // スピナーで選択された教科名を取得
            String selectedSubject = (String) subjectSpinner.getSelectedItem();
            if (selectedSubject != null) {
                // 通知を表示
                showNotification(selectedSubject);
            } else {
                Log.e("Notification", "スピナーから教科名が選択されていません");
            }
        });

        // 通知チャネルの作成
        createNotificationChannel();
    }

    private void showNotification(String selectedSubject) {
        // Firestoreから曜日と時限を取得する
        String[] days = {"月", "火", "水", "木", "金"};
        int[] periods = {1, 2, 3, 4, 5, 6, 7};

        for (String day : days) {
            for (int period : periods) {
                db.collection("timetable").document(userID).collection(day).document(String.valueOf(period)).get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult().exists()) {
                                DocumentSnapshot snapshot = task.getResult();
                                if (snapshot.contains("教科名") && selectedSubject.equals(snapshot.getString("教科名"))) {
                                    // 該当する曜日と時限が見つかった場合
                                    sendNotificationWithDayPeriod(day, period, selectedSubject);
                                }
                            } else {
                                Log.d("Database", "教科データが見つかりません: " + day + " " + period);
                            }
                        });
            }
        }
    }

    // 通知を作成して送信する
    private void sendNotificationWithDayPeriod(String day, int period, String selectedSubject) {
        // 出席ボタンのクリック処理
        Intent attendanceIntent = new Intent(this, NotificationReceiver.class);
        attendanceIntent.putExtra("button", "出席");
        attendanceIntent.putExtra("day", day);
        attendanceIntent.putExtra("period", period);
        PendingIntent attendancePendingIntent = PendingIntent.getBroadcast(this, 1, attendanceIntent, PendingIntent.FLAG_IMMUTABLE);

        // 欠席ボタンのクリック処理
        Intent absenceIntent = new Intent(this, NotificationReceiver.class);
        absenceIntent.putExtra("button", "欠席");
        absenceIntent.putExtra("day", day);
        absenceIntent.putExtra("period", period);
        PendingIntent absencePendingIntent = PendingIntent.getBroadcast(this, 2, absenceIntent, PendingIntent.FLAG_IMMUTABLE);

        // 休講ボタンのクリック処理
        Intent cancelIntent = new Intent(this, NotificationReceiver.class);
        cancelIntent.putExtra("button", "休講");
        cancelIntent.putExtra("day", day);
        cancelIntent.putExtra("period", period);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, 3, cancelIntent, PendingIntent.FLAG_IMMUTABLE);

        // 通知を作成
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground) // 通知アイコン
                .setContentTitle(selectedSubject + " の出欠確認") // 通知タイトル
                .setContentText("出席、欠席、休講のいずれかを選択してください") // 通知メッセージ
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.mipmap.ic_launcher_foreground, "出席", attendancePendingIntent) // 出席ボタン
                .addAction(R.mipmap.ic_launcher_foreground, "欠席", absencePendingIntent)    // 欠席ボタン
                .addAction(R.mipmap.ic_launcher_foreground, "休講", cancelPendingIntent);    // 休講ボタン

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
