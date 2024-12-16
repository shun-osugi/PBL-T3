package io.github.shun.osugi.pblt3.android;

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


public class AttendanceActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String userID;
    final String[] daysOfWeek = {"月", "火", "水", "木", "金", "土", "日"};
    private LinearLayout parentLayout;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        // Firebase初期化
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        userID = "sample（ユーザーID）";

        // ヘッダーのタイトルを動的に変更
        TextView headerTitle = findViewById(R.id.headerTitle);
        headerTitle.setText("出席確認");

        // フッターのクリックイベントを設定
        FooterUtils.setupFooter(this);

        progressBar = findViewById(R.id.progress_bar); // ProgressBarの参照を取得


        // 親レイアウトの取得
        parentLayout = findViewById(R.id.parentLayout); // ここに適切なIDが設定されていることを確認してください。

        // 全曜日とコマを探索して表示
        loadAttendanceData();
    }

    private void loadAttendanceData() {
        // まず、月曜日のデータをロードする
        db.collection("timetable").document(userID).collection("月").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<DocumentSnapshot> periods = task.getResult().getDocuments();
                        periods.sort((doc1, doc2) -> {
                            int period1 = Integer.parseInt(doc1.getId().replaceAll("\\D", ""));
                            int period2 = Integer.parseInt(doc2.getId().replaceAll("\\D", ""));
                            return Integer.compare(period1, period2);
                        });
                        // 月曜日のデータを表示
                        addDayLayout("月", periods);
                    }

                    // 月曜日のデータがロードされたら、次に他の曜日を順番通りにロード
                    List<String> completedDays = new ArrayList<>();
                    completedDays.add("月"); // 月曜日が最初なので先に追加

                    // 火曜日から日曜日までの曜日データを順番に処理
                    for (int i = 1; i < daysOfWeek.length; i++) {
                        String day = daysOfWeek[i];
                        db.collection("timetable").document(userID).collection(day).get()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful() && !task2.getResult().isEmpty()) {
                                        List<DocumentSnapshot> periods = task2.getResult().getDocuments();
                                        periods.sort((doc1, doc2) -> {
                                            int period1 = Integer.parseInt(doc1.getId().replaceAll("\\D", ""));
                                            int period2 = Integer.parseInt(doc2.getId().replaceAll("\\D", ""));
                                            return Integer.compare(period1, period2);
                                        });
                                        // 各曜日のデータを表示
                                        addDayLayout(day, periods);
                                    }
                                    // 完了した曜日のデータがあればcompletedDaysに追加
                                    completedDays.add(day);

                                    // すべての曜日のデータがロードされたら、表示順序を更新
                                    if (completedDays.size() == daysOfWeek.length) {
                                        sortAndDisplayDays(completedDays);
                                        progressBar.setVisibility(View.GONE); // プログレスバーを非表示
                                    }
                                });
                    }
                });
    }

    private void sortAndDisplayDays(List<String> completedDays) {
        // 完了した曜日を月曜日から順番に並べ替えて、親レイアウトに追加
        List<String> orderedDays = new ArrayList<>();
        for (String day : daysOfWeek) {
            if (completedDays.contains(day)) {
                orderedDays.add(day); // 曜日を正しい順番で追加
            }
        }

        // 正しい順番で親レイアウトに追加
        for (String day : orderedDays) {
            for (int i = 0; i < parentLayout.getChildCount(); i++) {
                LinearLayout dayLayout = (LinearLayout) parentLayout.getChildAt(i);
                TextView dayTitle = (TextView) dayLayout.getChildAt(0); // 曜日タイトルのTextView
                if (dayTitle.getText().toString().contains(day)) {
                    parentLayout.removeView(dayLayout);
                    parentLayout.addView(dayLayout);
                    break;
                }
            }
        }
    }


    private boolean isAllDataLoaded() {
        // すべての曜日のデータが読み込まれたかどうかを判定するロジックを追加
        // 例えば、各曜日が処理されたかどうかのフラグを立てて判定する
        return true;
    }


    private void addDayLayout(String day, List<DocumentSnapshot> periods) {
        // 曜日とコマを表示するコンテナ
        LinearLayout dayLayout = new LinearLayout(this);
        dayLayout.setOrientation(LinearLayout.VERTICAL);
        dayLayout.setPadding(0, 16, 0, 16);

        // 曜日名
        TextView dayTitle = new TextView(this);
        dayTitle.setText(day + "曜日");
        dayTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        dayTitle.setGravity(Gravity.CENTER);
        dayTitle.setTextColor(Color.BLACK);

        // コンテナに曜日名を追加
        dayLayout.addView(dayTitle);

        // 各コマ（授業）ごとに表を生成
        for (DocumentSnapshot periodDoc : periods) {
            String period = periodDoc.getId();
            Map<String, Object> scheduleMap = (Map<String, Object>) periodDoc.get("授業日程");
            String className = periodDoc.getString("教科名");

            // 出席、遅刻、欠席のカウントを初期化
            int attendanceCount = 0;
            int lateCount = 0;
            int absentCount = 0;

            // 「n限」を表示する TextView を追加
            TextView periodTitle = new TextView(this);
            periodTitle.setText(period + "限");
            periodTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            periodTitle.setGravity(Gravity.START);
            periodTitle.setPadding(16, 8, 0, 8);
            periodTitle.setTextColor(Color.DKGRAY);

            // コンテナに「n限」を追加
            dayLayout.addView(periodTitle);

            // 授業名を表示
            TextView classTitle = new TextView(this);
            classTitle.setText(className != null ? className : "不明");
            classTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            classTitle.setGravity(Gravity.START);
            classTitle.setPadding(16, 8, 0, 8);
            classTitle.setTextColor(Color.DKGRAY);

            // 出席、遅刻、欠席のカウント
            for (int i = 1; i <= 15; i++) {
                String lessonKey = "第" + i + "授業日";
                if (scheduleMap != null && scheduleMap.containsKey(lessonKey)) {
                    Map<String, Object> lesson = (Map<String, Object>) scheduleMap.get(lessonKey);

                    Boolean attendance = (Boolean) lesson.get("出欠");
                    Boolean late = (Boolean) lesson.get("遅刻");

                    if (attendance != null && attendance) {
                        attendanceCount++;
                    }
                    if (late != null && late) {
                        lateCount++;
                    }
                    if (attendance != null && !attendance) {
                        absentCount++;
                    }
                }
            }

            // 出席、遅刻、欠席のカウントを表示
            TextView attendanceSummary = new TextView(this);
            attendanceSummary.setText("出席 " + attendanceCount + " 遅刻 " + lateCount + " 欠席 " + absentCount);
            attendanceSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            attendanceSummary.setGravity(Gravity.START);
            attendanceSummary.setPadding(16, 8, 0, 8);
            attendanceSummary.setTextColor(Color.DKGRAY);

            // 表の作成
            TableLayout tableLayout = new TableLayout(this);
            tableLayout.setStretchAllColumns(true);

            // 3行5列の表を作成
            int maxLessons = 15;
            int columnCount = 5;

            for (int row = 0; row < 3; row++) {
                TableRow tableRow = new TableRow(this);
                for (int col = 0; col < columnCount; col++) {
                    int lessonNumber = row * columnCount + col + 1;

                    if (lessonNumber <= maxLessons) {
                        String displayText = "未";
                        int backgroundColor = Color.LTGRAY;

                        if (scheduleMap != null && scheduleMap.containsKey("第" + lessonNumber + "授業日")) {
                            Map<String, Object> lesson = (Map<String, Object>) scheduleMap.get("第" + lessonNumber + "授業日");

                            Boolean attendance = (Boolean) lesson.get("出欠");
                            Boolean late = (Boolean) lesson.get("遅刻");

                            if (attendance != null) {
                                if (attendance) {
                                    displayText = "出";
                                    backgroundColor = Color.BLUE;
                                } else {
                                    displayText = "欠";
                                    backgroundColor = Color.RED;
                                }
                            }
                            if (late != null && late) {
                                displayText = "遅";
                                backgroundColor = Color.parseColor("#FFA500");  // オレンジ色
                            }
                        }

                        // セルのレイアウト
                        LinearLayout cellLayout = new LinearLayout(this);
                        cellLayout.setOrientation(LinearLayout.VERTICAL);
                        cellLayout.setGravity(Gravity.CENTER);
                        cellLayout.setBackgroundColor(Color.TRANSPARENT);

                        TextView title = new TextView(this);
                        title.setText(String.valueOf(lessonNumber));
                        title.setGravity(Gravity.CENTER);
                        title.setBackgroundColor(Color.GRAY);
                        title.setTextColor(Color.WHITE);

                        TextView status = new TextView(this);
                        status.setText(displayText);
                        status.setGravity(Gravity.CENTER);
                        status.setBackgroundColor(backgroundColor);
                        status.setTextColor(Color.WHITE);

                        // クリックイベントを追加して出欠を変更
                        final String currentDay = day; // 曜日
                        final String currentPeriod = period; // コマ数
                        final Map<String, Object> currentScheduleMap = scheduleMap; // 授業スケジュール
                        final int currentLessonNumber = lessonNumber; // 授業番号

                        status.setOnClickListener(v -> toggleAttendanceStatus(status, currentLessonNumber, currentPeriod, currentDay, periodDoc, currentScheduleMap));
                        cellLayout.addView(title);
                        cellLayout.addView(status);
                        tableRow.addView(cellLayout);
                    }
                }
                tableLayout.addView(tableRow);
            }
            // 曜日コンテナに授業名、出席情報、表を追加
            dayLayout.addView(classTitle);
            dayLayout.addView(attendanceSummary); // 出席、遅刻、欠席のカウントを追加
            dayLayout.addView(tableLayout);
        }
        // 親レイアウトに曜日コンテナを追加
        parentLayout.addView(dayLayout);
    }


    private void toggleAttendanceStatus(TextView statusView, int lessonNumber, String period, String day, DocumentSnapshot periodDoc,Map<String, Object> scheduleMap) {
        // 現在の状態を取得
        String currentStatus = statusView.getText().toString();

        // 選択肢を作成
        CharSequence[] options = {"出席", "欠席", "遅刻"};

        // ダイアログを作成
        new AlertDialog.Builder(this)
                .setTitle("出席状況の編集")
                .setItems(options, (dialog, which) -> {
                    String newStatus = "未";
                    int newBackgroundColor = Color.LTGRAY;

                    // 選択されたオプションに基づいて状態を設定
                    switch (which) {
                        case 0: // 出席
                            newStatus = "出";
                            newBackgroundColor = Color.BLUE;
                            break;
                        case 1: // 欠席
                            newStatus = "欠";
                            newBackgroundColor = Color.RED;
                            break;
                        case 2: // 遅刻
                            newStatus = "遅";
                            newBackgroundColor = Color.parseColor("#FFA500");  // オレンジ色
                            break;
                    }

                    // 「未」の場合は新しいデータをFirestoreに保存
                    if (currentStatus.equals("未")) {
                        // 今日の日付を取得
                        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                        // Firestoreから現在のデータを取得
                        String finalNewStatus = newStatus;
                        db.collection("timetable").document(userID).collection(day).document(String.valueOf(period))
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && task.getResult().exists()) {
                                        Map<String, Object> scheduleMap1 = (Map<String, Object>) task.getResult().get("授業日程");

                                        int maxLessonNumber = 0;

                                        // 現在の最大 "第n授業日" を計算
                                        if (scheduleMap1 != null && !scheduleMap1.isEmpty()) {
                                            for (String key : scheduleMap1.keySet()) {
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

                                        // 新しい授業日データを作成
                                        String newLessonKey = "第" + (maxLessonNumber + 1) + "授業日";
                                        final Map<String, Object> newLessonData = new HashMap<>();
                                        newLessonData.put("授業日", todayDate);
                                        newLessonData.put("出欠", finalNewStatus.equals("出")); // 出席ならtrue, 欠席ならfalse
                                        newLessonData.put("遅刻", finalNewStatus.equals("遅")); // 遅刻ならtrue, 他はfalse

                                        // Firestoreにデータを保存
                                        db.collection("timetable").document(userID).collection(day).document(String.valueOf(period))
                                                .update("授業日程." + newLessonKey, newLessonData)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.i(TAG, "新しい授業データが保存されました: " + newLessonKey);
                                                    // 親レイアウトを再描画して、UI全体を更新
                                                    runOnUiThread(() -> {
                                                        updateAllAttendanceViews();
                                                    });
                                                })
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
                } else {
                        // 「未」以外の状態（出席、欠席、遅刻）については既存データを更新
                        Map<String, Object> lesson = (Map<String, Object>) scheduleMap.get("第" + lessonNumber + "授業日");
                        lesson.put("出欠", newStatus.equals("出"));
                        lesson.put("遅刻", newStatus.equals("遅"));
                        // Firestoreに更新した情報を反映
                        periodDoc.getReference().update("授業日程", scheduleMap)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // 親レイアウトを再描画して、UI全体を更新
                                        runOnUiThread(() -> {
                                            updateAllAttendanceViews(); // 新しいメソッドを追加して、レイアウトを再描画する
                                        });
                                    }
                                });
                    }
                })
                .setCancelable(true)
                .show();
    }
    // すべての出席情報を再描画するメソッド
    private void updateAllAttendanceViews() {
        parentLayout.removeAllViews();  // 親レイアウトを一度クリアしてから再描画

        // 再度、全曜日とコマを探索して表示
        loadAttendanceData();  // 出席情報を再度ロードしてUIに反映
    }
}
