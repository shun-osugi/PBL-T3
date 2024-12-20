package io.github.shun.osugi.pblt3.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private FirebaseFirestore db = FirebaseFirestore.getInstance(); // Firestoreインスタンス

    @Override
    public void onReceive(Context context, Intent intent) {
        // ユーザーID（適切な場所から取得するように変更してください）
        String userID = "sample（ユーザーID）";
        String day = intent.getStringExtra("day"); // 曜日
        int period = intent.getIntExtra("period", -1); // 時限
        String buttonType = intent.getStringExtra("button"); // ボタンの種類（出席・欠席・休講）

        if ("出席".equals(buttonType) || "欠席".equals(buttonType)) {
            // **1日欠席通知を削除する処理**
            cancelNotification(context, 4); // 1日欠席通知のIDをキャンセル
        }

        if (period == -1 || day == null || buttonType == null) {
            Log.e(TAG, "不正なデータ: 必要な情報が不足しています");
            return;
        }

        if ("休講".equals(buttonType)) {
            Log.i(TAG, "休講が選択されたため、データを保存しません");
            // Advance_Notice アクティビティに遷移するIntentを作成
            Intent advanceNoticeIntent = new Intent(context, Advance_Notice.class);
            advanceNoticeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 新しいタスクとして起動
            context.startActivity(advanceNoticeIntent);

            // **通知をキャンセル**
            cancelNotification(context, 1); // 通知IDは送信時のものに合わせる
            return;
        }

        // 今日の日付を取得
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Firestoreから現在のデータを取得
        db.collection("timetable").document(userID).collection(day).document(String.valueOf(period)).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Map<String, Object> scheduleMap = (Map<String, Object>) task.getResult().get("授業日程");

                        int maxLessonNumber = 0;

                        // 現在の最大 "第n授業日" を計算
                        if (scheduleMap != null && !scheduleMap.isEmpty()) {
                            for (String key : scheduleMap.keySet()) {
                                if (key.startsWith("第")) {
                                    try {
                                        int number = Integer.parseInt(key.substring(1, key.length() - 3)); // "第n授業日"からnを取得
                                        maxLessonNumber = Math.max(maxLessonNumber, number);
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "授業日程キーの解析に失敗しました: " + key, e);
                                    }
                                }
                            }
                        }

                        // データがない場合は maxLessonNumber は0のまま → 第1授業日を作成
                        String newLessonKey = "第" + (maxLessonNumber + 1) + "授業日";
                        Map<String, Object> newLessonData = Map.of(
                                "授業日", todayDate,
                                "出欠", "出席".equals(buttonType), // 出席ならtrue, 欠席ならfalse
                                "遅刻", false // 初期値はfalse
                        );

                        // Firestoreにデータを保存
                        db.collection("timetable").document(userID).collection(day).document(String.valueOf(period))
                                .update("授業日程." + newLessonKey, newLessonData)
                                .addOnSuccessListener(aVoid -> Log.i(TAG, "新しい授業データが保存されました: " + newLessonKey))
                                .addOnFailureListener(e -> {
                                    // ドキュメントが存在しない場合は set() を使用して新しく作成
                                    Map<String, Object> initialData = Map.of(
                                            "授業日程", Map.of(newLessonKey, newLessonData) // 初期データとして授業日程を作成
                                    );

                                    db.collection("timetable").document(userID).collection(day).document(String.valueOf(period))
                                            .set(initialData)
                                            .addOnSuccessListener(aVoid2 -> Log.i(TAG, "ドキュメントが新規作成されました: " + newLessonKey))
                                            .addOnFailureListener(e2 -> Log.e(TAG, "新規データの保存に失敗しました", e2));
                                });
                    } else {
                        Log.e(TAG, "Firestoreからデータを取得できませんでした");
                    }
                });

        // **通知をキャンセル**
        cancelNotification(context, 1); // 通知IDは送信時のものに合わせる
    }

    // 通知をキャンセルするメソッド
    private void cancelNotification(Context context, int notificationId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(notificationId);
        Log.d(TAG, "通知が削除されました: ID=" + notificationId);
    }
}
